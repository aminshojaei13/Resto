<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\WarehouseStock;
use App\Services\InventoryService;
use Illuminate\Http\Request;

class InventoryController extends Controller
{
    protected InventoryService $inventoryService;

    public function __construct(InventoryService $inventoryService)
    {
        $this->inventoryService = $inventoryService;
    }

    public function stock(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';
        $stock = WarehouseStock::where('organization_id', $orgId)->with(['product', 'warehouse'])->get();
        return response()->json($stock);
    }

    public function adjust(Request $request)
    {
        $request->validate([
            'org_id' => 'required',
            'warehouse_id' => 'required',
            'product_id' => 'required',
            'delta' => 'required|integer',
            'reason' => 'required|string',
        ]);

        $updatedStock = $this->inventoryService->adjustStock(
            $request->org_id,
            $request->warehouse_id,
            $request->product_id,
            $request->variant_id,
            $request->delta,
            $request->reason,
            $request->reference_id
        );

        return response()->json([
            'message' => 'Stock adjusted successfully',
            'stock' => $updatedStock
        ]);
    }

    public function transfer(Request $request)
    {
        $request->validate([
            'org_id' => 'required',
            'source_warehouse_id' => 'required',
            'destination_warehouse_id' => 'required',
            'product_id' => 'required',
            'quantity' => 'required|integer|min:1',
        ]);

        $this->inventoryService->transferStock(
            $request->org_id,
            $request->source_warehouse_id,
            $request->destination_warehouse_id,
            $request->product_id,
            $request->quantity,
            $request->reason ?? 'Warehouse Transfer'
        );

        return response()->json(['message' => 'Stock transfer completed successfully']);
    }
}
