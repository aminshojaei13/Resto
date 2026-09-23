<?php

namespace Tests\Feature;

use App\Models\Organization;
use App\Models\Product;
use App\Models\Store;
use App\Models\Warehouse;
use App\Services\AccountingService;
use App\Services\InventoryService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class InventoryAndAccountingTest extends TestCase
{
    use RefreshDatabase;

    public function test_inventory_stock_adjustment_records_movement(): void
    {
        $org = Organization::create([
            'id' => 'org_test',
            'name' => 'Test Org',
            'code' => 'TEST',
        ]);

        $store = Store::create([
            'id' => 'store_test',
            'organization_id' => 'org_test',
            'name' => 'Test Store',
            'code' => 'STR-1',
        ]);

        $wh = Warehouse::create([
            'id' => 'wh_test',
            'store_id' => 'store_test',
            'organization_id' => 'org_test',
            'name' => 'Test WH',
            'code' => 'WH-1',
        ]);

        $product = Product::create([
            'id' => 'prod_test',
            'organization_id' => 'org_test',
            'sku' => 'SKU-001',
            'barcode' => '1122334455',
            'name' => 'Test Product',
            'price' => 100.00,
            'cost_price' => 60.00,
            'category' => 'General',
        ]);

        $inventoryService = new InventoryService();
        $stock = $inventoryService->adjustStock('org_test', 'wh_test', 'prod_test', null, 10, 'Initial Stock Restock');

        $this->assertEquals(10, $stock->quantity);

        // Deduct 3
        $updatedStock = $inventoryService->adjustStock('org_test', 'wh_test', 'prod_test', null, -3, 'Sale Deduction');
        $this->assertEquals(7, $updatedStock->quantity);
    }

    public function test_accounting_service_enforces_debit_equals_credit(): void
    {
        $org = Organization::create([
            'id' => 'org_test',
            'name' => 'Test Org',
            'code' => 'TEST',
        ]);

        $store = Store::create([
            'id' => 'store_test',
            'organization_id' => 'org_test',
            'name' => 'Test Store',
            'code' => 'STR-1',
        ]);

        $accountingService = new AccountingService();

        $lines = [
            ['account_id' => 'acc_1', 'type' => 'DEBIT', 'amount' => 500.00],
            ['account_id' => 'acc_2', 'type' => 'CREDIT', 'amount' => 500.00],
        ];

        $entry = $accountingService->postJournalEntry('org_test', 'store_test', 'Test Journal Entry', $lines);

        $this->assertNotNull($entry);
        $this->assertEquals(500.00, $entry->total_debit);
        $this->assertEquals(500.00, $entry->total_credit);
    }
}
