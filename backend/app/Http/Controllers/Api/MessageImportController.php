<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Customer;
use App\Models\ImportedMessage;
use App\Models\Product;
use App\Services\BusinessSettingsService;
use App\Services\OrderPricingService;
use App\Support\MembershipContext;
use App\Support\Money;
use App\Support\UnitCatalog;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

/**
 * Order from a customer message.
 *
 * A shopkeeper pastes what a customer wrote on Instagram, Telegram or WhatsApp
 * and gets a draft order to correct before checkout.
 *
 * This is deterministic text parsing — a documented set of patterns — not a
 * language model. It is described as such everywhere. The draft is never
 * silently completed: a line that could not be matched to the catalog is
 * returned unresolved, so the person registers a real order or corrects the
 * line, rather than the system quietly substituting whichever product happened
 * to be first in the table.
 */
class MessageImportController extends Controller
{
    /** Lines that carry no order content. */
    private const NOISE = [
        'سلام', 'درود', 'سپاس', 'تشکر', 'لطفا', 'لطفاً', 'ببخشید', 'ممنون',
        'میخوام', 'می‌خوام', 'خواستم', 'میشه', 'می‌شه', 'بفرستید', 'ارسال',
        'hello', 'hi', 'please', 'thanks', 'thank you', 'regards',
    ];

    public function __construct(
        private BusinessSettingsService $settings,
        private OrderPricingService $pricing,
    ) {
    }

    public function index(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $messages = ImportedMessage::where('organization_id', $organizationId)
            ->with(['customer', 'order'])
            ->orderByDesc('created_at')
            ->get();

        return response()->json($messages);
    }

    /**
     * Read a pasted message and return a draft order for review.
     */
    public function parse(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'raw_text' => 'required|string|max:8000',
            'source' => 'nullable|string|max:40',
            'locale' => 'nullable|string|in:fa,en',
        ]);

        $rawText = $validated['raw_text'];
        $locale = $validated['locale'] ?? 'fa';
        $source = $validated['source'] ?? 'manual_paste';

        $customer = $this->extractCustomer($rawText, $organizationId);
        $paymentMethod = $this->extractPaymentMethod($rawText);
        $requestedLines = $this->extractLines($rawText);

        $matched = [];
        $unmatched = [];

        foreach ($requestedLines as $line) {
            $product = $this->matchProduct($line['query'], $organizationId);

            if ($product) {
                $matched[] = $this->buildLine($product, $line, $locale);
            } else {
                $unmatched[] = [
                    'raw_text' => $line['raw'],
                    'product_name' => $line['query'],
                    'quantity' => $line['quantity'],
                    'reason' => 'no_catalog_match',
                ];
            }
        }

        $taxRate = $this->settings->defaultTaxRate($organizationId);

        $totals = $this->totals($matched, $taxRate);

        $message = ImportedMessage::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $organizationId,
            'source' => $source,
            'raw_text' => $rawText,
            'status' => 'PARSED',
            'customer_id' => $customer['id'] ?? null,
        ]);

        return response()->json([
            'id' => $message->id,
            'source' => $source,
            'raw_text' => $rawText,
            'customer' => $customer,
            'items' => $matched,
            'unmatched_items' => $unmatched,
            'tax_rate' => $taxRate,
            'tax_rate_source' => 'business_default',
            'payment_method' => $paymentMethod,
            ...$totals,
            // The order is not registered by parsing. The person reviews and
            // corrects the draft, then checks it out through the order flow.
            'requires_review' => true,
        ]);
    }

    /**
     * Pricing preview for a draft. Uses exactly the same service as checkout,
     * so what is shown and what is charged cannot drift apart.
     */
    private function totals(array $lines, float $taxRate): array
    {
        $priced = $this->pricing->price(array_map(fn (array $line) => [
            'product_id' => $line['product_id'],
            'quantity' => $line['quantity'],
            'unit_price' => $line['price'],
            'discount_percent' => $line['discount_percent'],
        ], $lines), $taxRate);

        return [
            'subtotal' => $priced['subtotal'],
            'discount_amount' => $priced['discount'],
            'tax_amount' => $priced['tax'],
            'grand_total' => $priced['total'],
        ];
    }

    /**
     * Name, phone and address, when the message contains them.
     *
     * An existing customer is matched; an unknown name is returned as a
     * proposal rather than written to the customer table by a parser.
     */
    private function extractCustomer(string $rawText, string $organizationId): array
    {
        $lines = $this->normalisedLines($rawText);

        $name = null;
        $phone = null;
        $address = null;

        foreach ($lines as $line) {
            if (preg_match('/^(?:مشتری|نام|name|customer)\s*[:：\-]\s*(.+)$/iu', $line, $m)) {
                $name = $this->tidy($m[1]);
            } elseif (preg_match('/^(?:تلفن|موبایل|همراه|شماره|phone|mobile)\s*[:：\-]\s*(.+)$/iu', $line, $m)) {
                $phone = $this->digits($m[1]);
            } elseif (preg_match('/^(?:آدرس|نشانی|address)\s*[:：\-]\s*(.+)$/iu', $line, $m)) {
                $address = $this->tidy($m[1]);
            }
        }

        // Conversational customer name patterns if name is still null
        if (!$name) {
            foreach ($lines as $line) {
                if (preg_match('/(?:سلام\s+)?(?:من\s+)?([\p{L}\s]{2,30}?)\s+(?:هستم|هستتم|باشم)/u', $line, $m)) {
                    $cand = $this->tidy($m[1]);
                    $cand = preg_replace('/^(?:سلام|درود)\s+/u', '', $cand);
                    if (mb_strlen($cand) >= 2) {
                        $name = $cand;
                        break;
                    }
                } elseif (preg_match('/(?:از\s+طرف|مشتری|سفارش)\s+([\p{L}\s]{2,30}?)(?:\s+باشم|\s+هستم|\s+هستتم|\n|$)/u', $line, $m)) {
                    $cand = $this->tidy($m[1]);
                    if (mb_strlen($cand) >= 2) {
                        $name = $cand;
                        break;
                    }
                }
            }
        }

        // A phone number written on its own line is still a phone number.
        if (!$phone) {
            foreach ($lines as $line) {
                if (preg_match('/^(\+?\d[\d\s\-]{7,})$/u', trim($line), $m)) {
                    $phone = $this->digits($m[1]);
                    break;
                }
            }
        }

        $existing = null;

        if ($phone) {
            $existing = Customer::where('organization_id', $organizationId)->where('phone', $phone)->first();
        }

        if (!$existing && $name) {
            $existing = Customer::where('organization_id', $organizationId)->where('name', $name)->first();
        }

        return [
            'id' => $existing?->id,
            'name' => $existing?->name ?? $name,
            'phone' => $existing?->phone ?? $phone,
            'email' => $existing?->email,
            'address' => $existing?->address ?? $address,
            'is_new' => $existing === null,
        ];
    }

    private function extractPaymentMethod(string $rawText): string
    {
        foreach ($this->normalisedLines($rawText) as $line) {
            if (preg_match('/^(?:پرداخت|روش پرداخت|payment)\s*[:：\-]\s*(.+)$/iu', $line, $m)) {
                return $this->mapPaymentMethod($m[1]);
            }
        }

        $text = $this->normalise($rawText);

        if (str_contains($text, 'کارت') || str_contains($text, 'card') || str_contains($text, 'pos')) {
            return 'CARD';
        }

        if (str_contains($text, 'حواله') || str_contains($text, 'کارت به کارت') || str_contains($text, 'transfer')) {
            return 'BANK_TRANSFER';
        }

        return 'CASH';
    }

    private function mapPaymentMethod(string $value): string
    {
        $value = $this->normalise($value);

        return match (true) {
            str_contains($value, 'کارت'), str_contains($value, 'card'), str_contains($value, 'pos') => 'CARD',
            str_contains($value, 'حواله'), str_contains($value, 'انتقال'), str_contains($value, 'transfer') => 'BANK_TRANSFER',
            default => 'CASH',
        };
    }

    /**
     * Clean product query strings from noise, greeting, customer names, and closing verbs.
     */
    private function cleanProductQuery(string $query): string
    {
        $query = $this->tidy($query);

        // Strip customer introduction phrases like "حسینی هستتم", "سلام من علی هستم"
        $query = preg_replace('/(?:سلام\s+)?(?:من\s+)?[\p{L}\s]{2,30}?\s+(?:هستم|هستتم)/u', '', $query);

        // Strip leading greetings/fillers
        $query = preg_replace('/^(?:سلام|درود|سپاس|تشکر|لطفا|لطفاً|ممنون|ببخشید|روز بخیر|وقت بخیر|نام|مشتری)\s+/iu', '', $query);

        // Strip trailing closing verbs / fillers
        $query = preg_replace('/\s+(?:میخوام|می‌خوام|میخام|می‌خام|خواستم|میخواستم|می‌خواستم|بفرستید|ارسال|کنید|لطفا|لطفاً|ممنون|تشکر|بشه|لازم\s+دارم|نیاز\s+دارم|خرید\s+دارم|ثبت\s+کنید|ثبت\s+بفرمایید)$/iu', '', $query);

        return $this->tidy($query);
    }

    /**
     * Pull order lines out of free text.
     *
     * Recognised shapes, for example:
     *   ۲ عدد قهوه اسپرسو
     *   حسینی هستتم ۱۰ عدد کابل usb میخوام
     *   2 عدد بسته قهوه ۲۵۰ گرمی
     *   3 x Espresso
     *   قهوه اسپرسو - 2
     */
    private function extractLines(string $rawText): array
    {
        $lines = [];
        $unitPattern = '(?:عدد|عددی|تا|بسته|جعبه|بطری|دست|سرو|سفارش|کیلو|کیلوگرم|گرم|گرمی|متر|شاخه|حلقه|جفت|کارتن|پک|رول|طغری|دستگاه|حعدد|PCS?|ITEMS?|BOX(?:ES)?|PACK(?:S)?|BOTTLE(?:S)?|SET|SERVING(?:S)?|KG|G|M)';
        $closingPattern = '(?:میخوام|می‌خوام|میخام|می‌خام|خواستم|میخواستم|می‌خواستم|بفرستید|ارسال|لطفا|لطفاً|ممنون|تشکر|لازم|نیاز|خرید|ثبت)';

        foreach ($this->normalisedLines($rawText) as $raw) {
            if ($this->isNoise($raw) || $this->isMetadataLine($raw)) {
                continue;
            }

            // A labelled line: "کالا: قهوه" / "Quantity: 2" / "تعداد: 2"
            if (preg_match('/^(?:کالا|محصول|product|item)\s*[:：\-]\s*(.+)$/iu', $raw, $m)) {
                $query = $this->cleanProductQuery($m[1]);
                $quantity = $this->peekQuantity($raw);

                if ($query !== '') {
                    $lines[] = ['raw' => $raw, 'query' => $query, 'quantity' => $quantity ?: 1];
                }

                continue;
            }

            $quantity = null;
            $query = null;

            // Pattern 1: "<count> <unit> <name>" anywhere in line
            if (preg_match('/(?:^|\s)([\d٠-٩۰-۹]+)\s*' . $unitPattern . '\s+(.+?)(?=\s*' . $closingPattern . '|$)/iu', $raw, $m)) {
                $quantity = $this->toInt($m[1]);
                $query = $this->cleanProductQuery($m[2]);
            }
            // Pattern 2: "<count> x <name>"
            elseif (preg_match('/(?:^|\s)([\d٠-٩۰-۹]+)\s*[x×*]\s*(.+?)(?=\s*' . $closingPattern . '|$)/iu', $raw, $m)) {
                $quantity = $this->toInt($m[1]);
                $query = $this->cleanProductQuery($m[2]);
            }
            // Pattern 3: "<name> - <count>"
            elseif (preg_match('/^(.+?)\s*[-–—]\s*([\d٠-٩۰-۹]+)$/u', $raw, $m)) {
                $query = $this->cleanProductQuery($m[1]);
                $quantity = $this->toInt($m[2]);
            }
            // Pattern 4: "<name> <count> <unit>"
            elseif (preg_match('/^(.+?)\s+([\d٠-٩۰-۹]+)\s*' . $unitPattern . '$/iu', $raw, $m)) {
                $query = $this->cleanProductQuery($m[1]);
                $quantity = $this->toInt($m[2]);
            }
            // Pattern 5: "<count> <name>" without explicit unit word
            elseif (preg_match('/(?:^|\s)([\d٠-٩۰-۹]+)\s+(.+?)(?=\s*' . $closingPattern . '|$)/iu', $raw, $m)) {
                $quantity = $this->toInt($m[1]);
                $query = $this->cleanProductQuery($m[2]);
            }
            // Pattern 6: Just product name
            else {
                $query = $this->cleanProductQuery($raw);
                $quantity = $this->peekQuantity($raw) ?: 1;
            }

            if ($query === null || $query === '') {
                continue;
            }

            $quantity ??= $this->peekQuantity($raw) ?: 1;

            $lines[] = ['raw' => $raw, 'query' => $query, 'quantity' => max(1, $quantity)];
        }

        return $lines;
    }

    /**
     * A count written at the end of the line, e.g. "تعداد: ۲".
     */
    private function peekQuantity(string $line): ?int
    {
        if (preg_match('/(?:تعداد|qty|quantity)\s*[:：\-]?\s*([\p{N}٠-٩۰-۹]+)/iu', $line, $m)) {
            return $this->toInt($m[1]);
        }

        return null;
    }

    /**
     * Find the catalog product a line refers to using smart multi-tier token matching.
     */
    private function matchProduct(string $query, string $organizationId): ?Product
    {
        $query = $this->cleanProductQuery($query);
        $normQuery = $this->normalise($query);

        if ($normQuery === '') {
            return null;
        }

        $products = Product::where('organization_id', $organizationId)->get();

        if ($products->isEmpty()) {
            return null;
        }

        $queryTokens = array_values(array_filter(explode(' ', $normQuery), fn ($t) => strlen($t) > 0));

        $bestProduct = null;
        $bestScore = 0;

        foreach ($products as $product) {
            $normName = $this->normalise($product->name);
            $normSku = $this->normalise($product->sku);

            $score = 0;

            if ($normName === $normQuery || $normSku === $normQuery) {
                $score = 1000;
            } elseif (!empty($normSku) && (str_contains($normSku, $normQuery) || str_contains($normQuery, $normSku))) {
                $score = 900;
            } elseif (str_replace(' ', '', $normName) === str_replace(' ', '', $normQuery)) {
                $score = 850;
            } elseif (str_starts_with($normName, $normQuery)) {
                $score = 800;
            } elseif (str_starts_with($normQuery, $normName)) {
                $score = 750;
            } elseif (str_contains($normName, $normQuery)) {
                $score = 700;
            } else {
                $matchedTokens = 0;
                foreach ($queryTokens as $token) {
                    if (str_contains($normName, $token) || str_contains($normSku, $token)) {
                        $matchedTokens++;
                    }
                }

                if ($matchedTokens > 0) {
                    $ratio = $matchedTokens / max(count($queryTokens), 1);
                    if ($ratio >= 0.5) {
                        $score = (int)(200 + ($ratio * 400));
                    }
                }
            }

            if ($score > $bestScore) {
                $bestScore = $score;
                $bestProduct = $product;
            }
        }

        if ($bestScore >= 200) {
            return $bestProduct;
        }

        return null;
    }

    /**
     * Build the editable draft line for a matched product.
     */
    private function buildLine(Product $product, array $line, string $locale): array
    {
        $price = (float) $product->price;

        return [
            'product_id' => $product->id,
            'product_name' => $product->name,
            'sku' => (string) $product->sku,
            'unit' => UnitCatalog::normalize($product->unit) ?? UnitCatalog::DEFAULT_UNIT,
            'unit_label' => UnitCatalog::label($product->unit, $locale),
            'price' => $price,
            'quantity' => $line['quantity'],
            'discount_percent' => 0.0,
            'subtotal' => Money::fromMinor(Money::toMinor($price) * $line['quantity']),
            'stock_available' => (int) $product->stock()->sum('quantity'),
        ];
    }

    private function isNoise(string $line): bool
    {
        $normalised = $this->normalise($line);

        foreach (self::NOISE as $word) {
            if ($normalised === $this->normalise($word)) {
                return true;
            }
        }

        return false;
    }

    private function isMetadataLine(string $line): bool
    {
        return (bool) preg_match('/^(?:پرداخت|روش پرداخت|مشتری|نام|تلفن|موبایل|همراه|شماره|آدرس|نشانی|توضیحات|payment|customer|name|phone|mobile|address)\s*[:：\-]/iu', trim($line));
    }

    /** @return array<int, string> */
    private function normalisedLines(string $text): array
    {
        return array_values(array_filter(
            array_map('trim', preg_split('/\R/u', str_replace("\r", "\n", $text)) ?: []),
            fn (string $line) => $line !== ''
        ));
    }

    /**
     * Persian and Arabic digits are converted so quantities and phone numbers
     * are read the way they were written.
     */
    private function toInt(string $value): int
    {
        return (int) $this->latinise(trim($value));
    }

    private function digits(string $value): string
    {
        return preg_replace('/\D/', '', $this->latinise($value)) ?? '';
    }

    private function latinise(string $value): string
    {
        return strtr($value, [
            '۰' => '0', '۱' => '1', '۲' => '2', '۳' => '3', '۴' => '4',
            '۵' => '5', '۶' => '6', '۷' => '7', '۸' => '8', '۹' => '9',
            '٠' => '0', '١' => '1', '٢' => '2', '٣' => '3', '٤' => '4',
            '٥' => '5', '٦' => '6', '٧' => '7', '٨' => '8', '٩' => '9',
        ]);
    }

    /** Arabic kaf and yeh to their Persian forms, so matching is consistent. */
    private function normalise(string $value): string
    {
        $value = $this->latinise($value);

        $value = strtr($value, ['ك' => 'ک', 'ﻙ' => 'ک', 'ي' => 'ی', 'ى' => 'ی']);

        return mb_strtolower(trim($value), 'UTF-8');
    }

    /** Collapse the whitespace and zero-width noise real messages contain. */
    private function tidy(string $value): string
    {
        $value = str_replace(["\u{200B}", "\u{200C}", "\u{200D}", "\u{FEFF}"], '', $value);
        $value = preg_replace('/\s+/u', ' ', $value) ?? $value;

        return trim($value, " \t\n\r\0\x0B:：-");
    }
}