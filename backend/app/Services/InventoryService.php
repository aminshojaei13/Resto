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
        ?string $refId = null
    ): WarehouseStock {
        return DB::transaction(function () use ($orgId, $warehouseId, $productId, $variantId, $delta, $reason, $refId) {
            $product = Product::findOrFail($productId);

            $stock = WarehouseStock::where('organization_id', $orgId)
                ->where('warehouse_id', $warehouseId)
                ->where('product_id', $productId)
                ->where('product_variant_id', $variantId)
                ->lockForUpdate()
                ->first();

            if (!$stock) {
                $stock = WarehouseStock::create([
                    'id' => (string) Str::uuid(),
                    'organization_id' => $orgId,
                    'warehouse_id' => $warehouseId,
                    'product_id' => $productId,
                    'product_variant_id' => $variantId,
                    'quantity' => 0,
                    'reserved_quantity' => 0,
                ]);
            }

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
                'unit_cost' => $product->cost_price,
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
