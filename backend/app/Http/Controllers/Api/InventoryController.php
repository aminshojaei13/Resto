<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\InventoryService;
use App\Support\MembershipContext;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;

/**
 * Stock reads and movements.
 *
 * The organization is always the caller's own business (never a value taken
 * from the request body), and every warehouse referenced must actually belong
 * to that business. A failed movement is reported as a failure; nothing is
 * silently invented and no default warehouse is ever substituted.
 */
class InventoryController extends Controller
{
    public function __construct(protected InventoryService $inventoryService)
    {
    }

    public function stock(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $query = WarehouseStock::where('organization_id', $organizationId)
            ->with(['product', 'warehouse']);

        $warehouseId = $request->query('warehouse_id');

        if (is_string($warehouseId) && $warehouseId !== '') {
            $this->assertWarehouseBelongsTo($warehouseId, $organizationId);
            $query->where('warehouse_id', $warehouseId);
        }

        return response()->json($query->get());
    }

    public function adjust(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $request->validate([
            'warehouse_id' => 'required|string',
            'product_id' => 'required|string',
            'variant_id' => 'nullable|string',
            'delta' => 'required|integer',
            'reason' => 'required|string|max:255',
        ]);

        $this->assertWarehouseBelongsTo($request->warehouse_id, $organizationId);

        $updatedStock = $this->inventoryService->adjustStock(
            $organizationId,
            $request->warehouse_id,
            $request->product_id,
            $request->variant_id,
            $request->delta,
            $request->reason,
            $request->reference_id
        );

        return response()->json([
            'message' => 'موجودی با موفقیت اصلاح شد.',
            'stock' => $updatedStock,
        ]);
    }

    public function transfer(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $request->validate([
            'source_warehouse_id' => 'required|string',
            'destination_warehouse_id' => 'required|string',
            'product_id' => 'required|string',
            'quantity' => 'required|integer|min:1',
            'reason' => 'nullable|string|max:255',
        ]);

        if ($request->source_warehouse_id === $request->destination_warehouse_id) {
            throw ValidationException::withMessages([
                'destination_warehouse_id' => ['انبار مبدأ و مقصد نمی‌توانند یکسان باشند.'],
            ]);
        }

        $this->assertWarehouseBelongsTo($request->source_warehouse_id, $organizationId);
        $this->assertWarehouseBelongsTo($request->destination_warehouse_id, $organizationId);

        $this->inventoryService->transferStock(
            $organizationId,
            $request->source_warehouse_id,
            $request->destination_warehouse_id,
            $request->product_id,
            $request->quantity,
            $request->reason ?? 'انتقال بین انبارها'
        );

        return response()->json(['message' => 'انتقال موجودی با موفقیت انجام شد.']);
    }

    /**
     * Refuse to touch a warehouse that belongs to a different business.
     */
    private function assertWarehouseBelongsTo(string $warehouseId, string $organizationId): Warehouse
    {
        $warehouse = Warehouse::where('id', $warehouseId)->first();

        if (!$warehouse || $warehouse->organization_id !== $organizationId) {
            throw ValidationException::withMessages([
                'warehouse_id' => ['انبار انتخاب‌شده برای این کسب‌وکار معتبر نیست.'],
            ]);
        }

        return $warehouse;
    }
}
