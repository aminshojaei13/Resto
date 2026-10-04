<?php

namespace Tests\Feature;

use App\Models\Customer;
use App\Models\JournalEntry;
use App\Models\Order;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Product;
use App\Models\Store;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\AccountingService;
use App\Services\InventoryService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Tests\TestCase;

class EndToEndBusinessCycleTest extends TestCase
{
    use RefreshDatabase;

    public function test_full_business_cycle_inventory_checkout_double_entry(): void
    {
        // 0. A real authenticated owner of the business under test.
        $owner = User::create([
            'id' => 'usr_nexus_owner',
            'name' => 'Nexus Owner',
            'first_name' => 'Nexus',
            'last_name' => 'Owner',
            'email' => 'owner@nexus.example',
            'password' => Hash::make('Str0ng-Passphrase!'),
            'role' => 'Owner',
            'status' => User::STATUS_ACTIVE,
        ]);

        // 1. Create Organization, Store & Warehouse
        $org = Organization::create([
            'id' => 'org_nexus',
            'name' => 'Nexus Global Retail',
            'code' => 'NEXUS',
            'currency_symbol' => '$',
            'currency_code' => 'USD',
        ]);

        $store = Store::create([
            'id' => 'store_nexus_1',
            'organization_id' => 'org_nexus',
            'name' => 'Nexus Main Store',
            'code' => 'NEX-MAIN',
            'address' => '500 Tech Parkway',
        ]);

        $warehouse = Warehouse::create([
            'id' => 'wh_nexus_1a',
            'store_id' => 'store_nexus_1',
            'organization_id' => 'org_nexus',
            'name' => 'Nexus Central Storage',
            'code' => 'WH-NEX-1',
        ]);

        // 2. Create Product
        $product = Product::create([
            'id' => 'prod_nexus_laptop',
            'organization_id' => 'org_nexus',
            'sku' => 'NEX-LAP-001',
            'barcode' => '998877665544',
            'name' => 'SuperBook Pro 16',
            'price' => 2000.00,
            'cost_price' => 1200.00,
            'category' => 'Laptops',
        ]);

        OrganizationMembership::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_nexus',
            'user_id' => $owner->id,
            'role' => 'OWNER',
            'status' => 'ACTIVE',
            'joined_at' => now(),
        ]);

        $this->actingAs($owner);

        // 3. Receive Stock (Initial Purchase / Restock of 15 units)
        $inventoryService = new InventoryService();
        $stock = $inventoryService->adjustStock(
            'org_nexus',
            'wh_nexus_1a',
            'prod_nexus_laptop',
            null,
            15,
            'Initial Purchase Order Receive'
        );

        $this->assertEquals(15, $stock->quantity);

        // 4. Create Customer
        $customer = Customer::create([
            'id' => 'cust_nexus_1',
            'organization_id' => 'org_nexus',
            'name' => 'Michael Scott',
            'email' => 'michael@dundermifflin.com',
            'phone' => '+1 (555) 901-2211',
        ]);

        // 5. Perform POS Checkout (2 units of SuperBook Pro 16)
        $checkoutPayload = [
            'org_id' => 'org_nexus',
            'store_id' => 'store_nexus_1',
            'warehouse_id' => 'wh_nexus_1a',
            'customer_id' => 'cust_nexus_1',
            'customer_name' => 'Michael Scott',
            'payment_method' => 'CARD',
            'items' => [
                [
                    'product_id' => 'prod_nexus_laptop',
                    'product_name' => 'SuperBook Pro 16',
                    'sku' => 'NEX-LAP-001',
                    'price' => 2000.00,
                    'quantity' => 2,
                    'discount_percent' => 0,
                    'tax_rate' => 0.08,
                ]
            ]
        ];

        $response = $this->postJson('/api/v1/orders/checkout', $checkoutPayload, [
            'X-Tenant-ID' => 'org_nexus',
        ]);
        $response->assertStatus(201);
        // 6. Verify Warehouse Stock Decreased by 2 (15 -> 13)
        $updatedStock = WarehouseStock::where('warehouse_id', 'wh_nexus_1a')
            ->where('product_id', 'prod_nexus_laptop')
            ->first();

        $this->assertEquals(13, $updatedStock->quantity);

        // 7. Verify Order Recorded ($4,000 subtotal + $320 tax = $4,320 total)
        $order = Order::where('organization_id', 'org_nexus')->first();
        $this->assertNotNull($order);
        $this->assertEquals(4320.00, $order->total_amount);

        // 8. Verify Double-Entry General Ledger Balance (Sum of Debits == Sum of Credits)
        $journalEntries = JournalEntry::where('organization_id', 'org_nexus')->get();
        $this->assertTrue($journalEntries->count() >= 1);

        foreach ($journalEntries as $entry) {
            $this->assertEquals(
                $entry->total_debit,
                $entry->total_credit,
                "Double-entry equation violated on Journal Entry {$entry->entry_number}: Debits ({$entry->total_debit}) != Credits ({$entry->total_credit})"
            );
        }
    }
}
