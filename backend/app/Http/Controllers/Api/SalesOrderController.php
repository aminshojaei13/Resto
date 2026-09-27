<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Account;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Payment;
use App\Services\AccountingService;
use App\Services\InventoryService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class SalesOrderController extends Controller
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
        $storeId = $request->query('store_id');

        $builder = Order::where('organization_id', $orgId)->with('items');

        if ($storeId) {
            $builder->where('store_id', $storeId);
        }

        return response()->json($builder->orderBy('created_at', 'desc')->get());
    }

    public function show(string $id)
    {
        $order = Order::with(['items', 'payments', 'customer'])->findOrFail($id);
        return response()->json($order);
    }

    public function checkout(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';

        $request->validate([
            'store_id' => 'required',
            'warehouse_id' => 'required',
            'items' => 'required|array|min:1',
            'payment_method' => 'required',
        ]);

        return DB::transaction(function () use ($request, $orgId) {
            $orderNumber = 'ORD-' . date('Y') . '-' . rand(1000, 9999);
            $subtotal = 0.0;
            $discountTotal = 0.0;
            $taxTotal = 0.0;
            $grandTotal = 0.0;

            $order = Order::create([
                'id' => (string) Str::uuid(),
                'order_number' => $orderNumber,
                'organization_id' => $orgId,
                'store_id' => $request->store_id,
                'warehouse_id' => $request->warehouse_id,
                'customer_id' => $request->customer_id,
                'customer_name' => $request->customer_name ?? 'Walk-in Customer',
                'subtotal' => 0,
                'discount_amount' => 0,
                'tax_amount' => 0,
                'total_amount' => 0,
                'payment_method' => $request->payment_method,
                'payment_status' => 'PAID',
                'fulfillment_status' => 'COMPLETED',
                'notes' => $request->notes ?? '',
            ]);

            foreach ($request->items as $item) {
                $itemSubtotal = round($item['price'] * $item['quantity'], 2);
                $itemDiscount = round($itemSubtotal * (($item['discount_percent'] ?? 0) / 100.0), 2);
                $itemTaxable = round($itemSubtotal - $itemDiscount, 2);
                $itemTax = round($itemTaxable * ($item['tax_rate'] ?? 0.08), 2);
                $itemTotal = round($itemTaxable + $itemTax, 2);

                $subtotal = round($subtotal + $itemSubtotal, 2);
                $discountTotal = round($discountTotal + $itemDiscount, 2);
                $taxTotal = round($taxTotal + $itemTax, 2);
                $grandTotal = round($grandTotal + $itemTotal, 2);

                OrderItem::create([
                    'id' => (string) Str::uuid(),
                    'order_id' => $order->id,
                    'product_id' => $item['product_id'],
                    'product_variant_id' => $item['variant_id'] ?? null,
                    'product_name' => $item['product_name'],
                    'sku' => $item['sku'],
                    'unit_price' => round($item['price'], 2),
                    'quantity' => $item['quantity'],
                    'discount_percent' => $item['discount_percent'] ?? 0,
                    'tax_amount' => $itemTax,
                    'total_price' => $itemTotal,
                ]);

                // Deduct stock in warehouse
                $this->inventoryService->adjustStock(
                    $orgId,
                    $request->warehouse_id,
                    $item['product_id'],
                    $item['variant_id'] ?? null,
                    -$item['quantity'],
                    "Sales Order #{$orderNumber} checkout",
                    $order->id
                );
            }

            $order->subtotal = $subtotal;
            $order->discount_amount = $discountTotal;
            $order->tax_amount = $taxTotal;
            $order->total_amount = $grandTotal;
            $order->save();

            // Record Payment
            Payment::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $orgId,
                'order_id' => $order->id,
                'amount' => $grandTotal,
                'payment_method' => $request->payment_method,
                'status' => 'SUCCESS',
            ]);

            // Double-entry accounting entry
            $this->accountingService->recordSaleJournal($orgId, $request->store_id, $order);

            return response()->json($order->load('items'), 201);
        });
    }

    public function cancel(Request $request, string $id)
    {
        $order = Order::with('items')->findOrFail($id);

        if ($order->fulfillment_status === 'CANCELLED') {
            return response()->json(['message' => 'Order is already cancelled'], 422);
        }

        return DB::transaction(function () use ($order) {
            foreach ($order->items as $item) {
                // Restore stock
                $this->inventoryService->adjustStock(
                    $order->organization_id,
                    $order->warehouse_id,
                    $item->product_id,
                    $item->product_variant_id,
                    $item->quantity,
                    "Order #{$order->order_number} cancellation restock",
                    $order->id
                );
            }

            $order->fulfillment_status = 'CANCELLED';
            $order->save();

            return response()->json(['message' => 'Order cancelled and stock restored successfully', 'order' => $order]);
        });
    }

    public function refund(Request $request, string $id)
    {
        $order = Order::with('items')->findOrFail($id);

        if ($order->payment_status === 'REFUNDED') {
            return response()->json(['message' => 'Order is already refunded'], 422);
        }

        return DB::transaction(function () use ($order) {
            foreach ($order->items as $item) {
                // Restore stock
                $this->inventoryService->adjustStock(
                    $order->organization_id,
                    $order->warehouse_id,
                    $item->product_id,
                    $item->product_variant_id,
                    $item->quantity,
                    "Order #{$order->order_number} refund restock",
                    $order->id
                );
            }

            $order->payment_status = 'REFUNDED';
            $order->fulfillment_status = 'CANCELLED';
            $order->save();

            // Reversal journal entry: Dr Sales Revenue (4010), Cr Cash (1010)
            $salesAccount = Account::firstOrCreate(
                ['organization_id' => $order->organization_id, 'code' => '4010'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_revenue', 'name' => 'Sales Revenue', 'balance' => 0]
            );

            $cashAccount = Account::firstOrCreate(
                ['organization_id' => $order->organization_id, 'code' => '1010'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_asset', 'name' => 'Cash / POS Drawer', 'balance' => 0]
            );

            $this->accountingService->postJournalEntry(
                $order->organization_id,
                $order->store_id,
                "Sales Refund for Order #{$order->order_number}",
                [
                    [
                        'account_id' => $salesAccount->id,
                        'type' => 'DEBIT',
                        'amount' => $order->total_amount,
                        'description' => "Revenue reversal for Order #{$order->order_number}"
                    ],
                    [
                        'account_id' => $cashAccount->id,
                        'type' => 'CREDIT',
                        'amount' => $order->total_amount,
                        'description' => "Cash refund paid out for Order #{$order->order_number}"
                    ]
                ],
                'SalesRefund',
                $order->id
            );

            return response()->json(['message' => 'Order refunded, stock restored, and accounting entry reversed successfully', 'order' => $order]);
        });
    }
}
