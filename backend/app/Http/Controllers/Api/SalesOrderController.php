<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
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
        $orgId = $request->get('org_id') ?? 'org_apex';
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
        $request->validate([
            'org_id' => 'required',
            'store_id' => 'required',
            'warehouse_id' => 'required',
            'items' => 'required|array|min:1',
            'payment_method' => 'required',
        ]);

        return DB::transaction(function () use ($request) {
            $orderNumber = 'ORD-' . date('Y') . '-' . rand(1000, 9999);
            $subtotal = 0.0;
            $discountTotal = 0.0;
            $taxTotal = 0.0;
            $grandTotal = 0.0;

            $order = Order::create([
                'id' => (string) Str::uuid(),
                'order_number' => $orderNumber,
                'organization_id' => $request->org_id,
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
                $itemSubtotal = $item['price'] * $item['quantity'];
                $itemDiscount = $itemSubtotal * (($item['discount_percent'] ?? 0) / 100.0);
                $itemTaxable = $itemSubtotal - $itemDiscount;
                $itemTax = $itemTaxable * ($item['tax_rate'] ?? 0.08);
                $itemTotal = $itemTaxable + $itemTax;

                $subtotal += $itemSubtotal;
                $discountTotal += $itemDiscount;
                $taxTotal += $itemTax;
                $grandTotal += $itemTotal;

                OrderItem::create([
                    'id' => (string) Str::uuid(),
                    'order_id' => $order->id,
                    'product_id' => $item['product_id'],
                    'product_variant_id' => $item['variant_id'] ?? null,
                    'product_name' => $item['product_name'],
                    'sku' => $item['sku'],
                    'unit_price' => $item['price'],
                    'quantity' => $item['quantity'],
                    'discount_percent' => $item['discount_percent'] ?? 0,
                    'tax_amount' => $itemTax,
                    'total_price' => $itemTotal,
                ]);

                // Deduct stock in warehouse
                $this->inventoryService->adjustStock(
                    $request->org_id,
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
                'organization_id' => $request->org_id,
                'order_id' => $order->id,
                'amount' => $grandTotal,
                'payment_method' => $request->payment_method,
                'status' => 'SUCCESS',
            ]);

            // Double-entry accounting entry
            $this->accountingService->recordSaleJournal($request->org_id, $request->store_id, $order);

            return response()->json($order->load('items'), 201);
        });
    }
}
