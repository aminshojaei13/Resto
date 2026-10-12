<?php

namespace App\Services;

use App\Models\Product;
use App\Models\StockMovement;
use App\Models\WarehouseStock;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Exception;

class InventoryService
{
    /**
     * Ledger-backed stock adjustment with pessimistic locking and DB transaction.
     */
    public function adjustStock(
        string $orgId,
        string $warehouseId,
        string $productId,
        ?string $variantId,
        int $delta,
        string $reason,
        ?string $refId = null,
        ?string $unitCost = null
    ): WarehouseStock {
        return DB::transaction(function () use ($orgId, $warehouseId, $productId, $variantId, $delta, $reason, $refId, $unitCost) {
            $product = Product::findOrFail($productId);

            // Concurrency-safe initial stock record resolution:
            // If the row does not yet exist, firstOrCreate under unique constraint prevents race conditions
            $stock = WarehouseStock::firstOrCreate(
                [
                    'organization_id' => $orgId,
                    'warehouse_id' => $warehouseId,
                    'product_id' => $productId,
                    'product_variant_id' => $variantId,
                ],
                [
                    'id' => (string) Str::uuid(),
                    'quantity' => 0,
                    'reserved_quantity' => 0,
                ]
            );

            // Re-fetch with pessimistic row-level lock for safe mutation
            $stock = WarehouseStock::where('id', $stock->id)
                ->lockForUpdate()
                ->first();

            $newQty = max(0, $stock->quantity + $delta);
            $stock->quantity = $newQty;
            $stock->save();

            // Record movement ledger
            StockMovement::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $orgId,
                'warehouse_id' => $warehouseId,
                'product_id' => $productId,
                'product_variant_id' => $variantId,
                'type' => $delta >= 0 ? 'IN' : 'OUT',
                'quantity' => $delta,
                'unit_cost' => $unitCost ?? $product->cost_price,
                'reason' => $reason,
                'reference_id' => $refId,
            ]);

            return $stock;
        });
    }

    /**
     * Transfer stock between warehouses.
     */
    public function transferStock(
        string $orgId,
        string $sourceWhId,
        string $destWhId,
        string $productId,
        int $qty,
        string $reason = 'Warehouse Transfer'
    ): void {
        if ($qty <= 0) {
            throw new Exception("Transfer quantity must be positive");
        }

        DB::transaction(function () use ($orgId, $sourceWhId, $destWhId, $productId, $qty, $reason) {
            // Deduct from source
            $this->adjustStock($orgId, $sourceWhId, $productId, null, -$qty, "Transfer Out: $reason");
            // Add to destination
            $this->adjustStock($orgId, $destWhId, $productId, null, $qty, "Transfer In: $reason");
        });
    }
}
