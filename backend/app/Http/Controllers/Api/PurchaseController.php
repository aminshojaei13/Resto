<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Account;
use App\Models\Purchase;
use App\Models\PurchaseItem;
use App\Models\SupplierPayment;
use App\Services\AccountingService;
use App\Services\InventoryService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class PurchaseController extends Controller
{
    protected InventoryService $inventoryService;
    protected AccountingService $accountingService;

    public function __construct(InventoryService $inventoryService, AccountingService $accountingService)
    {
        $this->inventoryService = $inventoryService;
        $this->accountingService = $accountingService;
    }

    public function index(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';
        $purchases = Purchase::where('organization_id', $orgId)->with(['supplier', 'items'])->orderBy('created_at', 'desc')->get();
        return response()->json($purchases);
    }

    public function show(string $id)
    {
        $purchase = Purchase::with(['supplier', 'items.product', 'payments'])->findOrFail($id);
        return response()->json($purchase);
    }

    public function store(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';

        $request->validate([
            'store_id' => 'required|string',
            'warehouse_id' => 'required|string',
            'supplier_id' => 'required|string',
            'items' => 'required|array|min:1',
            'items.*.product_id' => 'required|string',
            'items.*.quantity' => 'required|integer|min:1',
            'items.*.unit_cost' => 'required|numeric|min:0',
        ]);

        return DB::transaction(function () use ($request, $orgId) {
            $purchaseNumber = 'PO-' . date('Y') . '-' . rand(1000, 9999);
            $totalAmount = 0.0;

            $purchase = Purchase::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $orgId,
                'store_id' => $request->store_id,
                'warehouse_id' => $request->warehouse_id,
                'supplier_id' => $request->supplier_id,
                'purchase_number' => $purchaseNumber,
                'total_amount' => 0,
                'status' => 'ORDERED',
                'payment_status' => 'UNPAID',
            ]);

            foreach ($request->items as $item) {
                $totalCost = $item['quantity'] * $item['unit_cost'];
                $totalAmount += $totalCost;

                PurchaseItem::create([
                    'id' => (string) Str::uuid(),
                    'purchase_id' => $purchase->id,
                    'product_id' => $item['product_id'],
                    'quantity' => $item['quantity'],
                    'unit_cost' => $item['unit_cost'],
                    'total_cost' => $totalCost,
                ]);
            }

            $purchase->total_amount = $totalAmount;
            $purchase->save();

            return response()->json($purchase->load('items'), 201);
        });
    }

    public function receive(Request $request, string $id)
    {
        $purchase = Purchase::with('items')->findOrFail($id);

        if ($purchase->status === 'RECEIVED') {
            return response()->json(['message' => 'Purchase order has already been received'], 422);
        }

        return DB::transaction(function () use ($purchase) {
            foreach ($purchase->items as $item) {
                $this->inventoryService->adjustStock(
                    $purchase->organization_id,
                    $purchase->warehouse_id,
                    $item->product_id,
                    null,
                    $item->quantity,
                    "Purchase Receiving #{$purchase->purchase_number}",
                    $purchase->id
                );
            }

            $purchase->status = 'RECEIVED';
            $purchase->save();

            // Journal Entry: Dr Inventory (2010), Cr Accounts Payable (2010)
            $inventoryAccount = Account::firstOrCreate(
                ['organization_id' => $purchase->organization_id, 'code' => '1200'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_asset', 'name' => 'Inventory Asset', 'balance' => 0]
            );

            $payableAccount = Account::firstOrCreate(
                ['organization_id' => $purchase->organization_id, 'code' => '2010'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_liability', 'name' => 'Accounts Payable', 'balance' => 0]
            );

            $this->accountingService->postJournalEntry(
                $purchase->organization_id,
                $purchase->store_id,
                "Purchase #{$purchase->purchase_number} received into stock",
                [
                    [
                        'account_id' => $inventoryAccount->id,
                        'type' => 'DEBIT',
                        'amount' => $purchase->total_amount,
                        'description' => "Inventory received for PO #{$purchase->purchase_number}"
                    ],
                    [
                        'account_id' => $payableAccount->id,
                        'type' => 'CREDIT',
                        'amount' => $purchase->total_amount,
                        'description' => "Accounts Payable for PO #{$purchase->purchase_number}"
                    ]
                ],
                'Purchase',
                $purchase->id
            );

            return response()->json(['message' => 'Purchase goods received into inventory successfully', 'purchase' => $purchase]);
        });
    }

    public function pay(Request $request, string $id)
    {
        $purchase = Purchase::findOrFail($id);

        $request->validate([
            'amount' => 'required|numeric|min:0.01',
            'payment_method' => 'required|string',
        ]);

        return DB::transaction(function () use ($request, $purchase) {
            $payment = SupplierPayment::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $purchase->organization_id,
                'purchase_id' => $purchase->id,
                'amount' => $request->amount,
                'payment_method' => $request->payment_method,
            ]);

            $totalPaid = SupplierPayment::where('purchase_id', $purchase->id)->sum('amount');
            if ($totalPaid >= $purchase->total_amount) {
                $purchase->payment_status = 'PAID';
            } else {
                $purchase->payment_status = 'PARTIAL';
            }
            $purchase->save();

            // Accounting entry: Dr Accounts Payable (2010), Cr Cash/Bank (1010)
            $payableAccount = Account::firstOrCreate(
                ['organization_id' => $purchase->organization_id, 'code' => '2010'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_liability', 'name' => 'Accounts Payable', 'balance' => 0]
            );

            $cashAccount = Account::firstOrCreate(
                ['organization_id' => $purchase->organization_id, 'code' => '1010'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_asset', 'name' => 'Cash / POS Drawer', 'balance' => 0]
            );

            $this->accountingService->postJournalEntry(
                $purchase->organization_id,
                $purchase->store_id,
                "Supplier payment for Purchase #{$purchase->purchase_number}",
                [
                    [
                        'account_id' => $payableAccount->id,
                        'type' => 'DEBIT',
                        'amount' => $request->amount,
                        'description' => "Settling Accounts Payable PO #{$purchase->purchase_number}"
                    ],
                    [
                        'account_id' => $cashAccount->id,
                        'type' => 'CREDIT',
                        'amount' => $request->amount,
                        'description' => "Cash payment to supplier for PO #{$purchase->purchase_number}"
                    ]
                ],
                'PurchasePayment',
                $payment->id
            );

            return response()->json(['message' => 'Supplier payment recorded successfully', 'payment' => $payment, 'purchase' => $purchase]);
        });
    }
}
