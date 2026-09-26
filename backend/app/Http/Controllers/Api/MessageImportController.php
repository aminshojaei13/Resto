<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Customer;
use App\Models\ImportedMessage;
use App\Models\Product;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class MessageImportController extends Controller
{
    public function index(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';
        $messages = ImportedMessage::where('organization_id', $orgId)
            ->with(['customer', 'order'])
            ->orderBy('created_at', 'desc')
            ->get();

        return response()->json($messages);
    }

    public function parse(Request $request)
    {
        $orgId = $request->get('org_id') ?? $request->input('org_id') ?? 'org_apex';

        $request->validate([
            'raw_text' => 'required|string',
            'source' => 'nullable|string',
        ]);

        $rawText = $request->raw_text;
        $source = $request->source ?? 'manual_paste';

        // Deterministic Key-Value Line Parsing
        $lines = explode("\n", str_replace("\r", "", $rawText));

        $customerName = 'Social Customer';
        $phone = '';
        $address = '';
        $paymentMethod = 'CASH';
        $extractedItems = [];

        $currentSku = '';
        $currentProductName = '';
        $currentQty = 1;

        foreach ($lines as $line) {
            $line = trim($line);
            if (empty($line)) continue;

            if (preg_match('/^(Customer|مشتری|نام):\s*(.+)$/i', $line, $matches)) {
                $customerName = trim($matches[2]);
            } elseif (preg_match('/^(Phone|تلفن|موبایل|همراه):\s*(.+)$/i', $line, $matches)) {
                $phone = trim($matches[2]);
            } elseif (preg_match('/^(Address|آدرس|نشانی):\s*(.+)$/i', $line, $matches)) {
                $address = trim($matches[2]);
            } elseif (preg_match('/^(Payment|پرداخت|روش پرداخت):\s*(.+)$/i', $line, $matches)) {
                $payStr = strtoupper(trim($matches[2]));
                if (str_contains($payStr, 'CARD') || str_contains($payStr, 'کارت')) {
                    $paymentMethod = 'CARD';
                } elseif (str_contains($payStr, 'TRANSFER') || str_contains($payStr, 'حواله')) {
                    $paymentMethod = 'BANK_TRANSFER';
                } else {
                    $paymentMethod = 'CASH';
                }
            } elseif (preg_match('/^(SKU|کد کالا|کد):\s*(.+)$/i', $line, $matches)) {
                $currentSku = trim($matches[2]);
            } elseif (preg_match('/^(Product|کالا|محصول):\s*(.+)$/i', $line, $matches)) {
                $currentProductName = trim($matches[2]);
            } elseif (preg_match('/^(Quantity|تعداد|تعداد سفارش):\s*(\d+)$/i', $line, $matches)) {
                $currentQty = (int) $matches[2];

                if ($currentSku || $currentProductName) {
                    $extractedItems[] = [
                        'sku' => $currentSku,
                        'name' => $currentProductName,
                        'quantity' => $currentQty,
                    ];
                    $currentSku = '';
                    $currentProductName = '';
                    $currentQty = 1;
                }
            }
        }

        // If items were parsed without explicit quantity line trigger
        if ($currentSku || $currentProductName) {
            $extractedItems[] = [
                'sku' => $currentSku,
                'name' => $currentProductName,
                'quantity' => $currentQty,
            ];
        }

        // Product Catalog Matching
        $matchedItems = [];
        $subtotal = 0.0;

        foreach ($extractedItems as $extracted) {
            $product = null;

            if (!empty($extracted['sku'])) {
                $product = Product::where('organization_id', $orgId)->where('sku', $extracted['sku'])->first();
            }

            if (!$product && !empty($extracted['name'])) {
                $product = Product::where('organization_id', $orgId)
                    ->where('name', 'LIKE', "%{$extracted['name']}%")
                    ->first();
            }

            // Fallback to first catalog product if not explicitly matched
            if (!$product) {
                $product = Product::where('organization_id', $orgId)->first();
            }

            if ($product) {
                $unitPrice = (float) $product->price;
                $qty = max(1, $extracted['quantity']);
                $lineSubtotal = round($unitPrice * $qty, 2);
                $subtotal += $lineSubtotal;

                $matchedItems[] = [
                    'product_id' => $product->id,
                    'product_name' => $product->name,
                    'sku' => $product->sku,
                    'price' => $unitPrice,
                    'quantity' => $qty,
                    'subtotal' => $lineSubtotal,
                    'stock_available' => $product->stock()->sum('quantity'),
                ];
            }
        }

        $taxAmount = round($subtotal * 0.08, 2);
        $grandTotal = round($subtotal + $taxAmount, 2);

        // Customer Matching
        $customer = null;
        if (!empty($phone)) {
            $customer = Customer::where('organization_id', $orgId)->where('phone', $phone)->first();
        }

        if (!$customer && !empty($customerName)) {
            $customer = Customer::where('organization_id', $orgId)->where('name', $customerName)->first();
        }

        $customerId = $customer?->id;
        $isNewCustomer = false;

        if (!$customer && !empty($customerName)) {
            $customer = Customer::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $orgId,
                'name' => $customerName,
                'phone' => $phone,
                'address' => $address,
            ]);
            $customerId = $customer->id;
            $isNewCustomer = true;
        }

        // Persist Imported Message record
        $importedMessage = ImportedMessage::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $orgId,
            'source' => $source,
            'raw_text' => $rawText,
            'status' => 'PARSED',
            'customer_id' => $customerId,
        ]);

        return response()->json([
            'id' => $importedMessage->id,
            'source' => $source,
            'raw_text' => $rawText,
            'customer' => [
                'id' => $customerId,
                'name' => $customerName,
                'phone' => $phone,
                'address' => $address,
                'is_new' => $isNewCustomer,
            ],
            'items' => $matchedItems,
            'subtotal' => $subtotal,
            'tax_amount' => $taxAmount,
            'grand_total' => $grandTotal,
            'payment_method' => $paymentMethod,
            'idempotency_key' => 'IDEM-IMPORT-' . Str::uuid(),
        ]);
    }
}
