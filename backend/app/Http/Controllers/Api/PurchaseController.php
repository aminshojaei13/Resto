<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Purchase;
use App\Models\PurchaseItem;
use App\Models\SupplierPayment;
use App\Models\Warehouse;
use App\Services\AccountingService;
use App\Services\InventoryService;
use App\Support\MembershipContext;
use App\Support\RolePermission;
use App\Support\UnitCatalog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * Purchasing.
 *
 * Three genuinely different things live here, and the API keeps them apart:
 *
 *  - a purchase order records what the business agreed to buy from a supplier;
 *  - receiving records the goods physically arriving into a warehouse, and may
 *    happen in more than one delivery and for less than the ordered quantity;
 *  - a supplier payment settles what is owed.
 *
 * Ordering does not move stock. Only receiving does, and only through the
 * inventory ledger.
 */
class PurchaseController extends Controller
{
    public const STATUS_ORDERED = 'ORDERED';
    public const STATUS_PARTIALLY_RECEIVED = 'PARTIALLY_RECEIVED';
    public const STATUS_RECEIVED = 'RECEIVED';
    public const STATUS_CANCELLED = 'CANCELLED';

    public function __construct(
        private InventoryService $inventoryService,
        private AccountingService $accountingService,
    ) {
    }

    public function index(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $query = trim((string) $request->query('q', ''));

        $builder = Purchase::where('organization_id', $organizationId)
            ->with(['supplier', 'items.product']);

        if ($query !== '') {
            $builder->where(function ($sub) use ($query) {
                $sub->where('purchase_number', 'like', "%{$query}%")
                    ->orWhereHas('supplier', fn ($supplier) => $supplier->where('name', 'like', "%{$query}%"));
            });
        }

        $status = $request->query('status');

        if (is_string($status) && $status !== '') {
            $builder->where('status', $status);
        }

        return response()->json($builder->orderByDesc('created_at')->get());
    }

    /**
     * Purchases awaiting goods to arrive — the receiving worklist.
     */
    public function receivingQueue(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $purchases = Purchase::where('organization_id', $organizationId)
            ->whereIn('status', [self::STATUS_ORDERED, self::STATUS_PARTIALLY_RECEIVED])
            ->with(['supplier', 'items.product'])
            ->orderBy('created_at')
            ->get();

        return response()->json($purchases);
    }

    public function show(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $purchase = Purchase::where('organization_id', $organizationId)
            ->with(['supplier', 'items.product', 'payments'])
            ->findOrFail($id);

        return response()->json($purchase);
    }

    /**
     * Raise a purchase order. This is a commitment to a supplier; no stock
     * moves here.
     */
    public function store(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $storeId = MembershipContext::activeStoreId($request, $organizationId);

        $validated = $request->validate([
            'store_id' => 'nullable|string',
            'warehouse_id' => 'nullable|string',
            'supplier_id' => 'required|string',
            'purchase_date' => 'nullable|date',
            'items' => 'required|array|min:1',
            'items.*.product_id' => 'required|string',
            'items.*.quantity' => 'required|integer|min:1',
            'items.*.unit_cost' => 'required|numeric|min:0',
        ]);

        $storeId = $validated['store_id'] ?? $storeId;

        if (!$storeId) {
            throw ValidationException::withMessages([
                'store_id' => ['برای ثبت سفارش خرید باید فروشگاه مشخص باشد.'],
            ]);
        }

        // A warehouse may be chosen now as the intended destination, or left
        // for the receiving step. It is never a hardcoded fallback.
        $warehouseId = $validated['warehouse_id'] ?? null;

        if ($warehouseId && !$this->warehouseOf($organizationId, $warehouseId)) {
            throw ValidationException::withMessages([
                'warehouse_id' => ['انبار انتخاب‌شده برای این کسب‌وکار معتبر نیست.'],
            ]);
        }

        return DB::transaction(function () use ($validated, $organizationId, $storeId, $warehouseId) {
            $purchase = Purchase::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organizationId,
                'store_id' => $storeId,
                'warehouse_id' => $warehouseId,
                'supplier_id' => $validated['supplier_id'],
                'purchase_number' => $this->nextPurchaseNumber($organizationId),
                'purchase_date' => $validated['purchase_date'] ?? date('Y-m-d'),
                'total_amount' => 0,
                'status' => self::STATUS_ORDERED,
                'payment_status' => 'UNPAID',
            ]);

            $totalMinor = 0;

            foreach ($validated['items'] as $item) {
                $unitCostMinor = \App\Support\Money::toMinor($item['unit_cost']);
                $lineTotalMinor = \App\Support\Money::multiply($unitCostMinor, (int) $item['quantity']);

                $totalMinor += $lineTotalMinor;

                $product = \App\Models\Product::where('organization_id', $organizationId)
                    ->where('id', $item['product_id'])
                    ->first();

                PurchaseItem::create([
                    'id' => (string) Str::uuid(),
                    'purchase_id' => $purchase->id,
                    'product_id' => $item['product_id'],
                    'unit' => UnitCatalog::normalize($product?->unit) ?? UnitCatalog::DEFAULT_UNIT,
                    'quantity' => (int) $item['quantity'],
                    'received_quantity' => 0,
                    'unit_cost' => \App\Support\Money::fromMinor($unitCostMinor),
                    'total_cost' => \App\Support\Money::fromMinor($lineTotalMinor),
                ]);
            }

            $purchase->total_amount = \App\Support\Money::fromMinor($totalMinor);
            $purchase->save();

            return response()->json($purchase->load(['supplier', 'items.product']), 201);
        });
    }

    /**
     * Record goods arriving into a warehouse.
     *
     * Receiving is partial by nature: a supplier may deliver 20 of 30 ordered.
     * Each call records only what physically arrived, and stock is moved by
     * the ledger for exactly that amount.
     */
    public function receive(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $purchase = Purchase::where('organization_id', $organizationId)
            ->with('items')
            ->findOrFail($id);

        if ($purchase->status === self::STATUS_RECEIVED) {
            throw ValidationException::withMessages([
                'status' => ['این سفارش خرید قبلاً به‌طور کامل دریافت شده است.'],
            ]);
        }

        if ($purchase->status === self::STATUS_CANCELLED) {
            throw ValidationException::withMessages([
                'status' => ['سفارش خرید لغو شده دریافت نمی‌شود.'],
            ]);
        }

        $validated = $request->validate([
            // Optional here: a purchase that already names its destination
            // warehouse does not need it repeated. A purchase without one must
            // state it at receiving time, which is checked below.
            'warehouse_id' => 'nullable|string',
            'items' => 'sometimes|array',
            'items.*.purchase_item_id' => 'required|string',
            'items.*.received_quantity' => 'required|integer|min:0',
            'notes' => 'nullable|string|max:1000',
        ]);

        $warehouseId = $validated['warehouse_id'] ?? $purchase->warehouse_id;

        if (!$warehouseId) {
            throw ValidationException::withMessages([
                'warehouse_id' => ['برای دریافت کالا باید انبار مقصد انتخاب شود.'],
            ]);
        }

        $warehouse = $this->warehouseOf($organizationId, (string) $warehouseId);

        if (!$warehouse) {
            throw ValidationException::withMessages([
                'warehouse_id' => ['برای دریافت کالا باید یک انبار معتبر انتخاب شود.'],
            ]);
        }

        // A delivery names what arrived. Lines that are not named are left
        // untouched; only a request with no line list at all means "receive
        // everything still outstanding".
        $requested = [];

        foreach ($validated['items'] ?? [] as $line) {
            $requested[$line['purchase_item_id']] = (int) $line['received_quantity'];
        }

        $receiveEverything = !array_key_exists('items', $validated);

        return DB::transaction(function () use ($purchase, $warehouse, $requested, $receiveEverything, $validated) {
            $receivedValueMinor = 0;
            $fullyReceived = true;
            $anythingReceived = false;

            foreach ($purchase->items as $item) {
                $outstanding = $item->quantity - $item->received_quantity;
                $amount = array_key_exists($item->id, $requested)
                    ? $requested[$item->id]
                    : ($receiveEverything ? $outstanding : 0);

                if ($amount < 0 || $amount > $outstanding) {
                    throw ValidationException::withMessages([
                        'items' => ["مقدار دریافتی برای «{$item->product?->name}» نمی‌تواند بیشتر از مقدار سفارش‌داده‌شده باشد."],
                    ]);
                }

                if ($amount === 0) {
                    $fullyReceived = false;
                    continue;
                }

                $anythingReceived = true;

                $item->received_quantity = $item->received_quantity + $amount;
                $item->save();

                // The ledger is the only thing that changes stock.
                $this->inventoryService->adjustStock(
                    $purchase->organization_id,
                    $warehouse->id,
                    $item->product_id,
                    null,
                    $amount,
                    "دریافت کالای سفارش خرید #{$purchase->purchase_number}",
                    $purchase->id
                );

                $receivedValueMinor += \App\Support\Money::toMinor($item->unit_cost) * $amount;
            }

            if (!$anythingReceived) {
                throw ValidationException::withMessages([
                    'items' => ['حداقل برای یک قلم باید مقدار دریافتی بیشتر از صفر ثبت شود.'],
                ]);
            }

            // Receiving status is derived from what actually arrived, never set
            // by hand.
            $purchase->refresh()->load('items');

            $outstanding = $purchase->items->sum(fn (PurchaseItem $i) => $i->quantity - $i->received_quantity);

            $purchase->status = $outstanding <= 0 ? self::STATUS_RECEIVED : self::STATUS_PARTIALLY_RECEIVED;
            $purchase->warehouse_id = $warehouse->id;
            $purchase->last_received_at = now();
            $purchase->received_at = $purchase->status === self::STATUS_RECEIVED ? now() : null;
            $purchase->save();

            // Inventory asset and the payable are recognised for what arrived.
            $this->postReceivingEntry($purchase, $warehouse, \App\Support\Money::fromMinor($receivedValueMinor));

            return response()->json([
                'message' => $purchase->status === self::STATUS_RECEIVED
                    ? 'کالا به‌طور کامل دریافت و به انبار موجود اضافه شد.'
                    : 'دریافت جزئی کالا ثبت شد.',
                'purchase' => $purchase->fresh(['supplier', 'items.product', 'payments']),
            ]);
        });
    }

    /**
     * Dr Inventory asset, Cr Accounts payable — for the value that arrived.
     */
    private function postReceivingEntry(Purchase $purchase, Warehouse $warehouse, float $value): void
    {
        if ($value <= 0) {
            return;
        }

        $inventoryAccount = $this->accountingService->account(
            $purchase->organization_id,
            AccountingService::ACCOUNT_INVENTORY,
            'Inventory Asset',
            'coa_asset'
        );

        $payableAccount = $this->accountingService->account(
            $purchase->organization_id,
            AccountingService::ACCOUNT_PAYABLE,
            'Accounts Payable',
            'coa_liability'
        );

        $this->accountingService->postJournalEntry(
            $purchase->organization_id,
            $purchase->store_id,
            "دریافت کالای سفارش خرید #{$purchase->purchase_number} در انبار {$warehouse->name}",
            [
                [
                    'account_id' => $inventoryAccount->id,
                    'type' => 'DEBIT',
                    'amount' => $value,
                    'description' => "ارزش کالای دریافت‌شده در انبار {$warehouse->name}",
                ],
                [
                    'account_id' => $payableAccount->id,
                    'type' => 'CREDIT',
                    'amount' => $value,
                    'description' => "بدهی به تأمین‌کننده بابت سفارش خرید #{$purchase->purchase_number}",
                ],
            ],
            'PurchaseReceiving',
            $purchase->id
        );
    }

    /**
     * Settle what the business owes the supplier.
     */
    public function pay(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $purchase = Purchase::where('organization_id', $organizationId)->findOrFail($id);

        $validated = $request->validate([
            'amount' => 'required|numeric|min:0.01',
            'payment_method' => 'required|string|max:40',
        ]);

        $amount = \App\Support\Money::fromMinor(\App\Support\Money::toMinor($validated['amount']));

        $outstanding = \App\Support\Money::toMinor($purchase->total_amount)
            - (int) \App\Support\Money::fromMinor(
                \App\Support\Money::toMinor(
                    SupplierPayment::where('purchase_id', $purchase->id)->sum('amount')
                )
            );

        if (\App\Support\Money::toMinor($amount) > $outstanding) {
            throw ValidationException::withMessages([
                'amount' => ['مبلغ پرداختی نمی‌تواند از مانده بدهی این سفارش خرید بیشتر باشد.'],
            ]);
        }

        return DB::transaction(function () use ($purchase, $validated, $amount) {
            $payment = SupplierPayment::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $purchase->organization_id,
                'purchase_id' => $purchase->id,
                'amount' => $amount,
                'payment_method' => $validated['payment_method'],
            ]);

            $totalPaid = \App\Support\Money::toMinor(SupplierPayment::where('purchase_id', $purchase->id)->sum('amount'));

            $purchase->payment_status = $totalPaid >= \App\Support\Money::toMinor($purchase->total_amount) ? 'PAID' : 'PARTIAL';
            $purchase->save();

            $payableAccount = $this->accountingService->account(
                $purchase->organization_id,
                AccountingService::ACCOUNT_PAYABLE,
                'Accounts Payable',
                'coa_liability'
            );

            $cashAccount = $this->accountingService->account(
                $purchase->organization_id,
                AccountingService::ACCOUNT_CASH,
                'Cash / POS Drawer',
                'coa_asset'
            );

            $this->accountingService->postJournalEntry(
                $purchase->organization_id,
                $purchase->store_id,
                "پرداخت به تأمین‌کننده بابت سفارش خرید #{$purchase->purchase_number}",
                [
                    [
                        'account_id' => $payableAccount->id,
                        'type' => 'DEBIT',
                        'amount' => $amount,
                        'description' => "تسویه بدهی تأمین‌کننده برای #{$purchase->purchase_number}",
                    ],
                    [
                        'account_id' => $cashAccount->id,
                        'type' => 'CREDIT',
                        'amount' => $amount,
                        'description' => "پرداخت نقدی به تأمین‌کننده برای #{$purchase->purchase_number}",
                    ],
                ],
                'PurchasePayment',
                $payment->id
            );

            return response()->json([
                'message' => 'پرداخت به تأمین‌کننده ثبت شد.',
                'payment' => $payment,
                'purchase' => $purchase->fresh(['supplier', 'items.product', 'payments']),
            ]);
        });
    }

    private function nextPurchaseNumber(string $organizationId): string
    {
        $year = date('Y');

        $last = Purchase::where('purchase_number', 'like', "PO-{$year}-%")
            ->orderByDesc('purchase_number')
            ->value('purchase_number');

        $sequence = 1;
        if ($last && preg_match('/PO-\d{4}-(\d+)/', $last, $m)) {
            $sequence = ((int) $m[1]) + 1;
        }

        do {
            $candidate = sprintf('PO-%s-%06d', $year, $sequence);
            $exists = Purchase::where('purchase_number', $candidate)->exists();
            if ($exists) {
                $sequence++;
            }
        } while ($exists);

        return $candidate;
    }

    private function warehouseOf(string $organizationId, string $warehouseId): ?Warehouse
    {
        return Warehouse::where('organization_id', $organizationId)->where('id', $warehouseId)->first();
    }
}