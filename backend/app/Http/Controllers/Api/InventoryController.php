<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Product;
use App\Models\StockMovement;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\InventoryService;
use App\Support\MembershipContext;
use App\Support\UnitCatalog;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;

/**
 * Stock reads and movements.
 *
 * The organization is always the caller's own business (never a value taken
 * from the request body), and every warehouse referenced must actually belong
 * to that business. A failed movement is reported as a failure; nothing is
 * silently invented and no default warehouse is ever substituted.
 *
 * Stock is only ever changed by the inventory ledger. There is no path in this
 * controller that adds a quantity to a row directly.
 */
class InventoryController extends Controller
{
    public function __construct(private InventoryService $inventoryService)
    {
    }

    /**
     * The warehouses this business actually has.
     *
     * This is where the warehouse picker gets its options. A business with one
     * warehouse still gets to see it named; a business with none gets an empty
     * list and is told to create one, instead of being handed a warehouse it
     * does not have.
     */
    public function warehouses(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $warehouses = Warehouse::where('organization_id', $organizationId)
            ->with('store')
            ->orderBy('name')
            ->get()
            ->map(fn (Warehouse $warehouse) => [
                'id' => $warehouse->id,
                'name' => $warehouse->name,
                'code' => $warehouse->code,
                'store_id' => $warehouse->store_id,
                'store_name' => $warehouse->store?->name ?? '',
            ]);

        return response()->json($warehouses);
    }

    /**
     * What is on hand, per warehouse, with the product's unit.
     */
    public function stock(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $locale = $request->query('locale') === 'en' ? 'en' : 'fa';

        $query = WarehouseStock::where('organization_id', $organizationId)
            ->with(['product', 'warehouse']);

        $warehouseId = $request->query('warehouse_id');

        if (is_string($warehouseId) && $warehouseId !== '') {
            $this->assertWarehouseBelongsTo($warehouseId, $organizationId);
            $query->where('warehouse_id', $warehouseId);
        }

        $productId = $request->query('product_id');

        if (is_string($productId) && $productId !== '') {
            $query->where('product_id', $productId);
        }

        $rows = $query->get()->map(fn (WarehouseStock $stock) => [
            'id' => $stock->id,
            'warehouse_id' => $stock->warehouse_id,
            'warehouse_name' => $stock->warehouse?->name ?? '',
            'product_id' => $stock->product_id,
            'product_name' => $stock->product?->name ?? '',
            'sku' => $stock->product?->sku ?? '',
            'quantity' => (int) $stock->quantity,
            'reserved_quantity' => (int) $stock->reserved_quantity,
            'available_quantity' => max(0, (int) $stock->quantity - (int) $stock->reserved_quantity),
            'unit' => UnitCatalog::normalize($stock->product?->unit) ?? UnitCatalog::DEFAULT_UNIT,
            'unit_label' => UnitCatalog::label($stock->product?->unit, $locale),
        ]);

        return response()->json($rows);
    }

    /**
     * Movement history for a product or a whole warehouse.
     */
    public function movements(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $query = StockMovement::where('organization_id', $organizationId);

        $warehouseId = $request->query('warehouse_id');

        if (is_string($warehouseId) && $warehouseId !== '') {
            $this->assertWarehouseBelongsTo($warehouseId, $organizationId);
            $query->where('warehouse_id', $warehouseId);
        }

        $productId = $request->query('product_id');

        if (is_string($productId) && $productId !== '') {
            $query->where('product_id', $productId);
        }

        $perPage = min(200, max(10, (int) $request->query('per_page', 50)));

        return response()->json($query->orderByDesc('created_at')->paginate($perPage));
    }

    /**
     * Register stock coming into a warehouse.
     *
     * This is the "increase inventory" operation. It is a movement through the
     * ledger, not an arithmetic edit: the warehouse row is only ever changed
     * by InventoryService.
     */
    public function stockIn(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'warehouse_id' => 'required|string',
            'product_id' => 'required|string',
            'variant_id' => 'nullable|string',
            'quantity' => 'required|integer|min:1',
            'reason' => 'nullable|string|max:255',
        ]);

        $this->assertWarehouseBelongsTo($validated['warehouse_id'], $organizationId);

        $product = Product::where('organization_id', $organizationId)
            ->where('id', $validated['product_id'])
            ->first();

        if (!$product) {
            throw ValidationException::withMessages([
                'product_id' => ['کالای انتخاب‌شده در این کسب‌وکار یافت نشد.'],
            ]);
        }

        // The unit is the product's own unit. It is never chosen separately
        // and never converted.
        $reason = ($validated['reason'] ?? '') ?: 'ورود کالا به انبار';

        $stock = $this->inventoryService->adjustStock(
            $organizationId,
            $validated['warehouse_id'],
            $validated['product_id'],
            $validated['variant_id'] ?? null,
            (int) $validated['quantity'],
            $reason
        );

        return response()->json([
            'message' => 'موجودی با موفقیت افزایش یافت.',
            'stock' => [
                'warehouse_id' => $stock->warehouse_id,
                'product_id' => $stock->product_id,
                'quantity' => (int) $stock->quantity,
                'unit' => UnitCatalog::normalize($product->unit) ?? UnitCatalog::DEFAULT_UNIT,
                'unit_label' => UnitCatalog::label($product->unit, $request->query('locale') === 'en' ? 'en' : 'fa'),
            ],
        ]);
    }

    /**
     * Correct stock: a counting discrepancy, damage, or loss.
     */
    public function adjust(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'warehouse_id' => 'required|string',
            'product_id' => 'required|string',
            'variant_id' => 'nullable|string',
            'delta' => 'required|integer',
            'reason' => 'required|string|max:255',
        ]);

        if ((int) $validated['delta'] === 0) {
            throw ValidationException::withMessages([
                'delta' => ['مقدار اصلاح باید بزرگ‌تر یا کوچک‌تر از صفر باشد.'],
            ]);
        }

        $this->assertWarehouseBelongsTo($validated['warehouse_id'], $organizationId);

        $updatedStock = $this->inventoryService->adjustStock(
            $organizationId,
            $validated['warehouse_id'],
            $validated['product_id'],
            $validated['variant_id'] ?? null,
            (int) $validated['delta'],
            $validated['reason'],
            $request->input('reference_id')
        );

        return response()->json([
            'message' => 'موجودی با موفقیت اصلاح شد.',
            'stock' => $updatedStock,
        ]);
    }

    public function transfer(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'source_warehouse_id' => 'required|string',
            'destination_warehouse_id' => 'required|string',
            'product_id' => 'required|string',
            'quantity' => 'required|integer|min:1',
            'reason' => 'nullable|string|max:255',
        ]);

        if ($validated['source_warehouse_id'] === $validated['destination_warehouse_id']) {
            throw ValidationException::withMessages([
                'destination_warehouse_id' => ['انبار مبدأ و مقصد نمی‌توانند یکسان باشند.'],
            ]);
        }

        $this->assertWarehouseBelongsTo($validated['source_warehouse_id'], $organizationId);
        $this->assertWarehouseBelongsTo($validated['destination_warehouse_id'], $organizationId);

        $this->inventoryService->transferStock(
            $organizationId,
            $validated['source_warehouse_id'],
            $validated['destination_warehouse_id'],
            $validated['product_id'],
            (int) $validated['quantity'],
            $validated['reason'] ?? 'انتقال بین انبارها'
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