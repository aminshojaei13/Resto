<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Account;
use App\Models\Customer;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Payment;
use App\Models\Product;
use App\Models\Warehouse;
use App\Services\AccountingService;
use App\Services\BusinessSettingsService;
use App\Services\InventoryService;
use App\Services\OrderPricingService;
use App\Support\MembershipContext;
use App\Support\Money;
use App\Support\RolePermission;
use App\Support\UnitCatalog;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * Sales orders.
 *
 * The organization is always the caller's own business, resolved from their
 * authenticated membership. Prices, discounts and tax are computed here from
 * catalog prices and the business tax setting — a client-supplied total is
 * never stored.
 */
class SalesOrderController extends Controller
{
    /** Payment states this backend actually stores. */
    public const PAYMENT_STATUSES = ['PAID', 'PENDING', 'REFUNDED'];

    /** Fulfilment states this backend actually stores. */
    public const FULFILLMENT_STATUSES = ['NEW', 'PREPARING', 'COMPLETED', 'CANCELLED'];

    public function __construct(
        private InventoryService $inventoryService,
        private AccountingService $accountingService,
        private OrderPricingService $pricing,
        private BusinessSettingsService $settings,
    ) {
    }

    /**
     * The operational order list: searchable, filterable, paginated, scoped.
     */
    public function index(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $builder = Order::where('organization_id', $organizationId)
            ->with('items')
            ->withCount('items');

        $this->applyFilters($builder, $request);

        $perPage = min(100, max(10, (int) $request->query('per_page', 25)));

        $orders = $builder->orderByDesc('created_at')->paginate($perPage);

        return response()->json($orders);
    }

    /**
     * Search and status filters, all server side.
     */
    private function applyFilters(Builder $builder, Request $request): void
    {
        $search = trim((string) $request->query('q', ''));

        if ($search !== '') {
            $builder->where(function (Builder $query) use ($search) {
                $query->where('order_number', 'like', "%{$search}%")
                    ->orWhere('customer_name', 'like', "%{$search}%")
                    // A phone or email may only be on the customer record, so
                    // matching it means joining the customer in.
                    ->orWhereHas('customer', function (Builder $customer) use ($search) {
                        $customer->where('phone', 'like', "%{$search}%")
                            ->orWhere('email', 'like', "%{$search}%")
                            ->orWhere('name', 'like', "%{$search}%");
                    });
            });
        }

        $status = $request->query('status');

        if (is_string($status) && $status !== '') {
            match ($status) {
                // The tabs the UI offers map onto the two real state columns.
                'PENDING_PAYMENT' => $builder->where('payment_status', 'PENDING'),
                'PAID' => $builder->where('payment_status', 'PAID'),
                'REFUNDED' => $builder->where('payment_status', 'REFUNDED'),
                'NEW' => $builder->where('fulfillment_status', 'NEW'),
                'PREPARING' => $builder->where('fulfillment_status', 'PREPARING'),
                'COMPLETED' => $builder->where('fulfillment_status', 'COMPLETED'),
                'CANCELLED' => $builder->where('fulfillment_status', 'CANCELLED'),
                default => throw ValidationException::withMessages([
                    'status' => ['وضعیت سفارش انتخاب‌شده معتبر نیست.'],
                ]),
            };
        }

        $paymentStatus = $request->query('payment_status');

        if (is_string($paymentStatus) && $paymentStatus !== '' && in_array($paymentStatus, self::PAYMENT_STATUSES, true)) {
            $builder->where('payment_status', $paymentStatus);
        }

        $source = $request->query('source');

        if (is_string($source) && $source !== '') {
            $builder->where('source', $source);
        }
    }

    /**
     * Order detail, with the pieces an operator needs: customer, items with
     * units, financials, payment, status and a timeline.
     */
    public function show(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $order = Order::where('organization_id', $organizationId)
            ->with(['items', 'payments', 'customer'])
            ->findOrFail($id);

        $payload = $order->toArray();
        $payload['tax_rate'] = (float) $order->tax_rate;
        $payload['timeline'] = $this->timeline($order);
        $payload['available_actions'] = $this->availableActions($request, $organizationId, $order);

        return response()->json($payload);
    }

    /**
     * The order's history, built from the records that actually exist.
     */
    private function timeline(Order $order): array
    {
        $events = [[
            'status' => 'NEW',
            'at' => $order->created_at,
            'label' => 'ثبت سفارش',
        ]];

        foreach ($order->payments as $payment) {
            if (strtoupper((string) $payment->status) === 'SUCCESS') {
                $events[] = ['status' => 'PAID', 'at' => $payment->created_at, 'label' => 'پرداخت'];
            }
        }

        if ($order->fulfillment_status === 'PREPARING') {
            $events[] = ['status' => 'PREPARING', 'at' => $order->updated_at, 'label' => 'آماده‌سازی'];
        }

        if ($order->fulfillment_status === 'COMPLETED') {
            $events[] = ['status' => 'COMPLETED', 'at' => $order->updated_at, 'label' => 'تکمیل'];
        }

        if ($order->fulfillment_status === 'CANCELLED') {
            $events[] = [
                'status' => 'CANCELLED',
                'at' => $order->cancelled_at ?? $order->updated_at,
                'label' => 'لغو',
            ];
        }

        return $events;
    }

    /**
     * Actions the current person may take on this order, given its state.
     */
    private function availableActions(Request $request, string $organizationId, Order $order): array
    {
        $role = MembershipContext::roleFor($request, $organizationId);
        $canCreate = RolePermission::allows($role, RolePermission::SALES_CREATE);
        $canRefund = RolePermission::allows($role, RolePermission::SALES_REFUND);

        $actions = ['view'];

        if ($order->fulfillment_status !== 'CANCELLED') {
            $actions[] = 'prepare';
        }

        if ($order->payment_status === 'PENDING' && $order->fulfillment_status !== 'CANCELLED' && $canCreate) {
            $actions[] = 'pay';
        }

        if ($canCreate) {
            $actions[] = 'cancel';
        }

        if ($canRefund && $order->payment_status === 'PAID') {
            $actions[] = 'refund';
        }

        return $actions;
    }

    /**
     * Register a sale.
     *
     * `items[].price` is accepted for compatibility but re-read from the
     * catalog, so a tampered client cannot set its own price. Totals and tax
     * are computed here.
     */
    public function checkout(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $storeId = MembershipContext::activeStoreId($request, $organizationId);

        $validated = $request->validate([
            'store_id' => 'nullable|string',
            'warehouse_id' => 'required|string',
            'customer_id' => 'nullable|string',
            'customer_name' => 'nullable|string|max:191',
            'payment_method' => 'required|string|max:40',
            'payment_status' => 'nullable|string|in:' . implode(',', self::PAYMENT_STATUSES),
            'fulfillment_status' => 'nullable|string|in:' . implode(',', self::FULFILLMENT_STATUSES),
            'tax_rate' => 'nullable|numeric',
            'notes' => 'nullable|string|max:1000',
            'source' => 'nullable|string|max:40',
            'items' => 'required|array|min:1',
            'items.*.product_id' => 'required|string|filled',
            'items.*.quantity' => 'required|integer|min:1',
            'items.*.price' => 'nullable|numeric|min:0',
            'items.*.discount_percent' => 'nullable|numeric|min:0|max:100',
        ], [
            'items.required' => 'اقلام سفارش نمی‌تواند خالی باشد.',
            'items.min' => 'حداقل یک کالا برای ثبت سفارش لازم است.',
            'items.*.product_id.required' => 'شناسه کالا در اقلام سفارش مشخص نشده است.',
            'items.*.product_id.filled' => 'شناسه کالا در اقلام سفارش نامعتبر یا خالی است.',
        ]);

        $storeId = $validated['store_id'] ?? $storeId;

        if (!$storeId) {
            throw ValidationException::withMessages([
                'store_id' => ['برای ثبت سفارش باید فروشگاه مشخص باشد.'],
            ]);
        }

        $warehouse = Warehouse::where('organization_id', $organizationId)
            ->where('id', $validated['warehouse_id'])
            ->first();

        if (!$warehouse) {
            throw ValidationException::withMessages([
                'warehouse_id' => ['انبار انتخاب‌شده برای این کسب‌وکار معتبر نیست.'],
            ]);
        }

        $taxRate = $this->settings->resolveTaxRate($request, $organizationId);

        // Server-side pricing: product names, SKUs, prices and units all come
        // from this business's own catalog.
        $lines = [];
        $seen = [];

        foreach ($validated['items'] as $index => $item) {
            $product = Product::where('organization_id', $organizationId)
                ->where('id', $item['product_id'])
                ->first();

            if (!$product) {
                throw ValidationException::withMessages([
                    "items.{$index}.product_id" => ['کالای انتخاب‌شده در این کسب‌وکار یافت نشد.'],
                ]);
            }

            if (isset($seen[$product->id])) {
                throw ValidationException::withMessages([
                    "items.{$index}.product_id" => ['همان کالا چند بار در سفارش آمده است؛ تعداد را در یک ردیف وارد کنید.'],
                ]);
            }

            $seen[$product->id] = true;

            $lines[] = [
                'product_id' => $product->id,
                'product_variant_id' => null,
                'product_name' => $product->name,
                'sku' => (string) $product->sku,
                'unit' => UnitCatalog::normalize($product->unit) ?? UnitCatalog::DEFAULT_UNIT,
                'unit_price' => (float) $product->price,
                'quantity' => (int) $item['quantity'],
                'discount_percent' => (float) ($item['discount_percent'] ?? 0),
            ];
        }

        $priced = $this->pricing->price($lines, $taxRate);

        return DB::transaction(function () use ($request, $organizationId, $storeId, $warehouse, $priced, $taxRate, $validated) {
            $order = Order::create([
                'id' => (string) Str::uuid(),
                'order_number' => $this->nextOrderNumber($organizationId),
                'organization_id' => $organizationId,
                'store_id' => $storeId,
                'warehouse_id' => $warehouse->id,
                'customer_id' => $validated['customer_id'] ?? null,
                'customer_name' => $validated['customer_name'] ?? 'مشتری حضوری',
                'subtotal' => 0,
                'discount_amount' => 0,
                'tax_amount' => 0,
                'tax_rate' => 0,
                'total_amount' => 0,
                'payment_method' => $validated['payment_method'],
                'payment_status' => $validated['payment_status'] ?? 'PAID',
                'fulfillment_status' => $validated['fulfillment_status'] ?? 'COMPLETED',
                'notes' => $validated['notes'] ?? '',
                'source' => $validated['source'] ?? 'POS',
                'created_by_user_id' => $request->user()?->id,
            ]);

            foreach ($priced['lines'] as $line) {
                OrderItem::create(array_merge([
                    'id' => (string) Str::uuid(),
                    'order_id' => $order->id,
                ], $line));

                // Stock is moved by the ledger, never by arithmetic in the UI.
                $this->inventoryService->adjustStock(
                    $organizationId,
                    $warehouse->id,
                    $line['product_id'],
                    $line['product_variant_id'],
                    -$line['quantity'],
                    "فروش سفارش #{$order->order_number}",
                    $order->id
                );
            }

            $this->pricing->applyToOrder($order, $priced, $taxRate);
            $order->save();

            if ($order->payment_status === 'PAID') {
                Payment::create([
                    'id' => (string) Str::uuid(),
                    'organization_id' => $organizationId,
                    'order_id' => $order->id,
                    'amount' => $order->total_amount,
                    'payment_method' => $order->payment_method,
                    'status' => 'SUCCESS',
                ]);

                $this->accountingService->recordSaleJournal($organizationId, $storeId, $order);
            }

            return response()->json(
                $order->load('items')->toArray() + ['tax_rate' => (float) $order->tax_rate],
                201
            );
        });
    }

    /**
     * A collision-free order number: respects the global unique constraint on orders.order_number.
     */
    private function nextOrderNumber(string $organizationId): string
    {
        $year = date('Y');

        $last = Order::where('order_number', 'like', "ORD-{$year}-%")
            ->orderByDesc('order_number')
            ->value('order_number');

        $sequence = 1;
        if ($last && preg_match('/ORD-\d{4}-(\d+)/', $last, $m)) {
            $sequence = ((int) $m[1]) + 1;
        }

        do {
            $candidate = sprintf('ORD-%s-%06d', $year, $sequence);
            $exists = Order::where('order_number', $candidate)->exists();
            if ($exists) {
                $sequence++;
            }
        } while ($exists);

        return $candidate;
    }

    /**
     * Move a paid or pending order into preparation.
     */
    public function prepare(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $order = Order::where('organization_id', $organizationId)->findOrFail($id);

        if ($order->fulfillment_status === 'CANCELLED') {
            throw ValidationException::withMessages([
                'status' => ['سفارش لغو شده قابل آماده‌سازی نیست.'],
            ]);
        }

        if ($order->fulfillment_status === 'COMPLETED') {
            throw ValidationException::withMessages([
                'status' => ['این سفارش قبلاً تکمیل شده است.'],
            ]);
        }

        $order->fulfillment_status = 'PREPARING';
        $order->save();

        return response()->json([
            'message' => 'سفارش در حال آماده‌سازی قرار گرفت.',
            'order' => $order->fresh(),
        ]);
    }

    /**
     * Settle an order that was registered but not yet paid.
     */
    public function pay(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $order = Order::where('organization_id', $organizationId)->findOrFail($id);

        if ($order->payment_status === 'PAID') {
            throw ValidationException::withMessages([
                'payment_status' => ['این سفارش قبلاً پرداخت شده است.'],
            ]);
        }

        if ($order->fulfillment_status === 'CANCELLED') {
            throw ValidationException::withMessages([
                'payment_status' => ['سفارش لغو شده قابل پرداخت نیست.'],
            ]);
        }

        $validated = $request->validate([
            'payment_method' => 'nullable|string|max:40',
        ]);

        return DB::transaction(function () use ($order, $organizationId, $validated) {
            $method = $validated['payment_method'] ?? $order->payment_method;

            Payment::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organizationId,
                'order_id' => $order->id,
                'amount' => $order->total_amount,
                'payment_method' => $method,
                'status' => 'SUCCESS',
            ]);

            $order->payment_method = $method;
            $order->payment_status = 'PAID';
            $order->save();

            $this->accountingService->recordSaleJournal($organizationId, $order->store_id, $order);

            return response()->json([
                'message' => 'پرداخت سفارش ثبت شد.',
                'order' => $order->fresh(),
            ]);
        });
    }

    /**
     * Cancel an order: stock returns to the warehouse and the sale journal is
     * reversed. A cancelled order keeps its record; nothing is deleted.
     */
    public function cancel(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $order = Order::where('organization_id', $organizationId)->with('items')->findOrFail($id);

        if ($order->fulfillment_status === 'CANCELLED') {
            throw ValidationException::withMessages([
                'status' => ['این سفارش قبلاً لغو شده است.'],
            ]);
        }

        $role = MembershipContext::roleFor($request, $organizationId);

        if (!RolePermission::allows($role, RolePermission::SALES_CREATE)) {
            throw ValidationException::withMessages([
                'status' => ['اجازه لغو سفارش را ندارید.'],
            ]);
        }

        return DB::transaction(function () use ($order, $request) {
            foreach ($order->items as $item) {
                $this->inventoryService->adjustStock(
                    $order->organization_id,
                    $order->warehouse_id,
                    $item->product_id,
                    $item->product_variant_id,
                    $item->quantity,
                    "لغو سفارش #{$order->order_number} و بازگشت کالا به انبار",
                    $order->id
                );
            }

            $order->fulfillment_status = 'CANCELLED';
            $order->cancelled_at = now();
            $order->cancelled_by_user_id = $request->user()?->id;
            $order->save();

            if ($order->payment_status === 'PAID') {
                $this->reverseSaleJournal($order);
            }

            return response()->json([
                'message' => 'سفارش لغو شد و موجودی انبار بازگردانده شد.',
                'order' => $order->fresh(),
            ]);
        });
    }

    /**
     * Refund a paid order. This reverses the sale journal and returns stock;
     * it is not the same operation as cancelling an unpaid order.
     */
    public function refund(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $order = Order::where('organization_id', $organizationId)->with('items')->findOrFail($id);

        if ($order->payment_status !== 'PAID') {
            throw ValidationException::withMessages([
                'payment_status' => ['فقط سفارش پرداخت‌شده قابل استرداد است.'],
            ]);
        }

        return DB::transaction(function () use ($order) {
            foreach ($order->items as $item) {
                $this->inventoryService->adjustStock(
                    $order->organization_id,
                    $order->warehouse_id,
                    $item->product_id,
                    $item->product_variant_id,
                    $item->quantity,
                    "استرداد سفارش #{$order->order_number} و بازگشت کالا به انبار",
                    $order->id
                );
            }

            $order->payment_status = 'REFUNDED';
            $order->fulfillment_status = 'CANCELLED';
            $order->cancelled_at = now();
            $order->save();

            $this->reverseSaleJournal($order);

            return response()->json([
                'message' => 'سفارش مسترد شد و سند حسابداری آن برگشت خورد.',
                'order' => $order->fresh(),
            ]);
        });
    }

    /**
     * Mirror image of the sale entry: credit cash, debit revenue, and debit the
     * tax that had been collected. Same accounts, opposite direction.
     */
    private function reverseSaleJournal(Order $order): void
    {
        $grossMinor = Money::toMinor($order->total_amount);
        $taxMinor = Money::toMinor($order->tax_amount);
        $netMinor = $grossMinor - $taxMinor;

        $cashAccount = $this->accountingService->account(
            $order->organization_id,
            AccountingService::ACCOUNT_CASH,
            'Cash / POS Drawer',
            'coa_asset'
        );

        $salesAccount = $this->accountingService->account(
            $order->organization_id,
            AccountingService::ACCOUNT_SALES_REVENUE,
            'Sales Revenue',
            'coa_revenue'
        );

        $lines = [
            [
                'account_id' => $salesAccount->id,
                'type' => 'DEBIT',
                'amount' => Money::fromMinor($netMinor),
                'description' => "برگشت درآمد فروش سفارش #{$order->order_number}",
            ],
            [
                'account_id' => $cashAccount->id,
                'type' => 'CREDIT',
                'amount' => Money::fromMinor($grossMinor),
                'description' => "برگشت وجه نقد سفارش #{$order->order_number}",
            ],
        ];

        if ($taxMinor > 0) {
            $taxAccount = $this->accountingService->account(
                $order->organization_id,
                AccountingService::ACCOUNT_TAX_PAYABLE,
                'Sales Tax Payable',
                'coa_liability'
            );

            $lines[] = [
                'account_id' => $taxAccount->id,
                'type' => 'DEBIT',
                'amount' => Money::fromMinor($taxMinor),
                'description' => "برگشت مالیات فروش سفارش #{$order->order_number}",
            ];
        }

        $this->accountingService->postJournalEntry(
            $order->organization_id,
            $order->store_id,
            "برگشت سند فروش سفارش #{$order->order_number}",
            $lines,
            'SalesOrderReversal',
            $order->id
        );
    }

    /**
     * The customer the order belongs to, resolved server side for the detail
     * view. Only ever returns a customer of this business.
     */
    private function resolveCustomer(?string $customerId, string $organizationId): ?Customer
    {
        if (!$customerId) {
            return null;
        }

        return Customer::where('organization_id', $organizationId)->where('id', $customerId)->first();
    }
}