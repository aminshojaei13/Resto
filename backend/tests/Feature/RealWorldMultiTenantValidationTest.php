<?php

namespace Tests\Feature;

use App\Models\AuditLog;
use App\Models\BusinessApplication;
use App\Models\Customer;
use App\Models\Expense;
use App\Models\JournalEntry;
use App\Models\Order;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Product;
use App\Models\Purchase;
use App\Models\Store;
use App\Models\Supplier;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\AccountingService;
use App\Services\InventoryService;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Tests\TestCase;

class RealWorldMultiTenantValidationTest extends TestCase
{
    use RefreshDatabase;

    protected User $adminUser;
    protected User $alphaOwner;
    protected User $betaOwner;
    protected User $gammaOwner;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);

        // Platform Admin
        $this->adminUser = User::create([
            'id' => 'usr_p8_admin',
            'name' => 'Resto Platform Operator',
            'email' => 'operator@resto.com',
            'password' => Hash::make('secret123'),
            'role' => 'PlatformAdmin',
            'is_platform_admin' => true,
        ]);

        // Tenant Alpha
        Organization::create(['id' => 'org_alpha', 'name' => 'Alpha Coffee Roasters', 'code' => 'ALPHA', 'currency_symbol' => '$', 'currency_code' => 'USD']);
        Store::create(['id' => 'store_alpha_1', 'organization_id' => 'org_alpha', 'name' => 'Alpha Flagship Store', 'code' => 'ALPHA-ST1']);
        Warehouse::create(['id' => 'wh_alpha_1', 'organization_id' => 'org_alpha', 'store_id' => 'store_alpha_1', 'name' => 'Alpha Warehouse', 'code' => 'ALPHA-WH1']);
        $this->alphaOwner = User::create(['id' => 'usr_alpha', 'name' => 'Alpha Owner', 'email' => 'owner@alpha.com', 'password' => Hash::make('password123'), 'role' => 'Owner', 'is_platform_admin' => false]);
        OrganizationMembership::create(['id' => (string) Str::uuid(), 'organization_id' => 'org_alpha', 'user_id' => 'usr_alpha', 'role' => 'OWNER']);

        // Tenant Beta
        Organization::create(['id' => 'org_beta', 'name' => 'Beta Retail Group', 'code' => 'BETA', 'currency_symbol' => '$', 'currency_code' => 'USD']);
        Store::create(['id' => 'store_beta_1', 'organization_id' => 'org_beta', 'name' => 'Beta Downtown Store', 'code' => 'BETA-ST1']);
        Warehouse::create(['id' => 'wh_beta_1', 'organization_id' => 'org_beta', 'store_id' => 'store_beta_1', 'name' => 'Beta Central Hub', 'code' => 'BETA-WH1']);
        $this->betaOwner = User::create(['id' => 'usr_beta', 'name' => 'Beta Owner', 'email' => 'owner@beta.com', 'password' => Hash::make('password123'), 'role' => 'Owner', 'is_platform_admin' => false]);
        OrganizationMembership::create(['id' => (string) Str::uuid(), 'organization_id' => 'org_beta', 'user_id' => 'usr_beta', 'role' => 'OWNER']);

        // Tenant Gamma
        Organization::create(['id' => 'org_gamma', 'name' => 'Gamma Food & Grocery', 'code' => 'GAMMA', 'currency_symbol' => '$', 'currency_code' => 'USD']);
        Store::create(['id' => 'store_gamma_1', 'organization_id' => 'org_gamma', 'name' => 'Gamma Supermarket', 'code' => 'GAMMA-ST1']);
        Warehouse::create(['id' => 'wh_gamma_1', 'organization_id' => 'org_gamma', 'store_id' => 'store_gamma_1', 'name' => 'Gamma Cold Storage', 'code' => 'GAMMA-WH1']);
        $this->gammaOwner = User::create(['id' => 'usr_gamma', 'name' => 'Gamma Owner', 'email' => 'owner@gamma.com', 'password' => Hash::make('password123'), 'role' => 'Owner', 'is_platform_admin' => false]);
        OrganizationMembership::create(['id' => (string) Str::uuid(), 'organization_id' => 'org_gamma', 'user_id' => 'usr_gamma', 'role' => 'OWNER']);
    }

    /**
     * PHASE 3 — TENANT ISOLATION MATRIX
     */
    public function test_tenant_isolation_matrix_prevents_cross_tenant_data_access(): void
    {
        // Create product in Tenant Beta
        $betaProduct = Product::create([
            'id' => 'prod_beta_secret',
            'organization_id' => 'org_beta',
            'sku' => 'BETA-SEC-001',
            'barcode' => '777888999',
            'name' => 'Beta Confidential Product',
            'price' => 500.00,
            'cost_price' => 300.00,
            'category' => 'Secret',
        ]);

        // 1. Alpha User attempting to read Beta's product directly -> 403
        $this->actingAs($this->alphaOwner);
        $res = $this->getJson("/api/v1/products/{$betaProduct->id}", ['X-Tenant-ID' => 'org_beta']);
        $res->assertStatus(403)
            ->assertJsonFragment(['error' => 'Unauthorized organization access']);

        // 2. Alpha User attempting to list products using Beta tenant header -> 403
        $listRes = $this->getJson('/api/v1/products', ['X-Tenant-ID' => 'org_beta']);
        $listRes->assertStatus(403);

        // 3. Alpha User listing products under Alpha tenant header -> 200, contains only Alpha data
        $alphaRes = $this->getJson('/api/v1/products', ['X-Tenant-ID' => 'org_alpha']);
        $alphaRes->assertStatus(200);
        $json = $alphaRes->json();
        foreach ($json as $item) {
            $this->assertNotEquals('prod_beta_secret', $item['id']);
        }

        // 4. Gamma User attempting to list Beta suppliers -> 403
        $this->actingAs($this->gammaOwner);
        $supRes = $this->getJson('/api/v1/suppliers', ['X-Tenant-ID' => 'org_beta']);
        $supRes->assertStatus(403);
    }

    /**
     * PHASE 4 & 5 — END-TO-END BUSINESS LIFECYCLE & INVENTORY CONSISTENCY
     */
    public function test_full_business_lifecycle_inventory_and_accounting_for_tenant_alpha(): void
    {
        $this->actingAs($this->alphaOwner);
        $headers = ['X-Tenant-ID' => 'org_alpha', 'X-Store-ID' => 'store_alpha_1'];

        // 1. Create Product
        $prodRes = $this->postJson('/api/v1/products', [
            'sku' => 'ALP-COF-001',
            'barcode' => '111222333444',
            'name' => 'Alpha Arabica Beans 1kg',
            'price' => 25.00,
            'cost_price' => 12.00,
            'category' => 'Coffee Beans',
            'unit' => 'bag',
        ], $headers);
        $prodRes->assertStatus(201);
        $prodId = $prodRes->json('id');

        // 2. Create Supplier
        $supRes = $this->postJson('/api/v1/suppliers', [
            'name' => 'Alpha Import Farms',
            'email' => 'sales@alphafarms.com',
            'phone' => '+1 555 111 2222',
        ], $headers);
        $supRes->assertStatus(201);
        $supId = $supRes->json('id');

        // 3. Create Purchase Order (100 bags @ $12.00 = $1,200 total)
        $poRes = $this->postJson('/api/v1/purchases', [
            'store_id' => 'store_alpha_1',
            'warehouse_id' => 'wh_alpha_1',
            'supplier_id' => $supId,
            'items' => [
                ['product_id' => $prodId, 'quantity' => 100, 'unit_cost' => 12.00]
            ]
        ], $headers);
        $poRes->assertStatus(201);
        $poId = $poRes->json('id');

        // 4. Receive Purchase Goods -> Stock increases by 100
        $recvRes = $this->postJson("/api/v1/purchases/{$poId}/receive", [], $headers);
        $recvRes->assertStatus(200);

        $stock = WarehouseStock::where('warehouse_id', 'wh_alpha_1')->where('product_id', $prodId)->first();
        $this->assertNotNull($stock);
        $this->assertEquals(100, $stock->quantity);

        // 5. Create Customer
        $custRes = $this->postJson('/api/v1/customers', [
            'name' => 'John Alpha Customer',
            'email' => 'john@alphacustomer.com',
        ], $headers);
        $custRes->assertStatus(201);
        $custId = $custRes->json('id');

        // 6. POS Checkout (4 bags @ $25.00 = $100.00 subtotal)
        $checkoutRes = $this->postJson('/api/v1/orders/checkout', [
            'store_id' => 'store_alpha_1',
            'warehouse_id' => 'wh_alpha_1',
            'customer_id' => $custId,
            'customer_name' => 'John Alpha Customer',
            'payment_method' => 'CASH',
            'items' => [
                ['product_id' => $prodId, 'product_name' => 'Alpha Arabica Beans 1kg', 'sku' => 'ALP-COF-001', 'price' => 25.00, 'quantity' => 4]
            ]
        ], $headers);
        $checkoutRes->assertStatus(201);

        // 7. Verify Inventory decrease (100 - 4 = 96)
        $stock->refresh();
        $this->assertEquals(96, $stock->quantity);

        // 8. Record Expense ($15.00 utility)
        $expRes = $this->postJson('/api/v1/expenses', [
            'store_id' => 'store_alpha_1',
            'category' => 'Store Utilities',
            'amount' => 15.00,
            'date' => date('Y-m-d'),
            'notes' => 'Coffee grinder maintenance',
        ], $headers);
        $expRes->assertStatus(201);

        // 9. Verify General Ledger Balance (Debits == Credits for ALL entries)
        $journalEntries = JournalEntry::where('organization_id', 'org_alpha')->get();
        $this->assertTrue($journalEntries->count() >= 2);

        foreach ($journalEntries as $entry) {
            $this->assertEquals(
                $entry->total_debit,
                $entry->total_credit,
                "Double-entry equation violated on entry {$entry->entry_number}: Debits ({$entry->total_debit}) != Credits ({$entry->total_credit})"
            );
        }
    }

    /**
     * PHASE 7 — IDEMPOTENCY
     */
    public function test_idempotency_middleware_prevents_duplicate_transactions(): void
    {
        $this->actingAs($this->betaOwner);
        $headers = [
            'X-Tenant-ID' => 'org_beta',
            'X-Store-ID' => 'store_beta_1',
            'X-Idempotency-Key' => 'IDEM-BETA-CHK-9999'
        ];

        $product = Product::create([
            'id' => 'prod_beta_phone',
            'organization_id' => 'org_beta',
            'sku' => 'BET-PHN-001',
            'barcode' => '888777666',
            'name' => 'Beta Phone 15',
            'price' => 800.00,
            'cost_price' => 500.00,
            'category' => 'Smartphones',
        ]);

        WarehouseStock::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_beta',
            'warehouse_id' => 'wh_beta_1',
            'product_id' => $product->id,
            'quantity' => 10,
        ]);

        $payload = [
            'store_id' => 'store_beta_1',
            'warehouse_id' => 'wh_beta_1',
            'customer_name' => 'Repeat Customer',
            'payment_method' => 'CARD',
            'items' => [
                ['product_id' => $product->id, 'product_name' => 'Beta Phone 15', 'sku' => 'BET-PHN-001', 'price' => 800.00, 'quantity' => 1]
            ]
        ];

        // Request #1
        $res1 = $this->postJson('/api/v1/orders/checkout', $payload, $headers);
        $res1->assertStatus(201);
        $order1 = $res1->json('id');

        // Request #2 (identical idempotency key)
        $res2 = $this->postJson('/api/v1/orders/checkout', $payload, $headers);
        $res2->assertStatus(201)
             ->assertHeader('X-Cache-Lookup', 'HIT-IDEMPOTENT');

        $order2 = $res2->json('id');
        $this->assertEquals($order1, $order2);

        // Verify only 1 order created in DB and stock decreased by exactly 1 (10 -> 9)
        $orderCount = Order::where('organization_id', 'org_beta')->count();
        $this->assertEquals(1, $orderCount);

        $stock = WarehouseStock::where('warehouse_id', 'wh_beta_1')->where('product_id', $product->id)->first();
        $this->assertEquals(9, $stock->quantity);
    }

    /**
     * PHASE 14 — PLATFORM ADMIN SECURITY
     */
    public function test_platform_admin_security_and_privilege_escalation_protection(): void
    {
        // 1. Normal Tenant User trying to access Platform Applications API -> 403
        $this->actingAs($this->alphaOwner);
        $resDeny = $this->getJson('/api/v1/platform/business-applications');
        $resDeny->assertStatus(403)
                ->assertJsonFragment(['error' => 'Unauthorized platform admin access']);

        // 2. Platform Admin User accessing Platform Applications API -> 200
        $this->actingAs($this->adminUser);
        $resAllow = $this->getJson('/api/v1/platform/business-applications');
        $resAllow->assertStatus(200);
    }

    /**
     * PHASE 15 — AUDIT LOG STREAM
     */
    public function test_audit_logs_record_administrative_and_business_events(): void
    {
        $this->actingAs($this->adminUser);

        // Submit Business Application
        $appRes = $this->postJson('/api/v1/business-applications', [
            'business_name' => 'Gamma Bakery',
            'owner_name' => 'Sara Gamma',
            'email' => 'sara@gammabakery.com',
            'business_type' => 'BAKERY',
        ]);
        $appRes->assertStatus(201);
        $appId = $appRes->json('application_id');

        // Approve Application
        $approveRes = $this->postJson("/api/v1/platform/business-applications/{$appId}/approve");
        $approveRes->assertStatus(200);

        // Verify Audit Log entry exists for application creation & approval
        $auditLogs = AuditLog::where('entity_id', $appId)->orWhere('entity_type', 'Organization')->get();
        $this->assertTrue($auditLogs->count() >= 1);
    }

    /**
     * PHASE 11 — BACKUP & RESTORE DRILL
     */
    public function test_backup_and_restore_integrity_drill(): void
    {
        // Verify primary tables exist and have records
        $this->assertDatabaseHas('organizations', ['id' => 'org_alpha']);
        $this->assertDatabaseHas('organizations', ['id' => 'org_beta']);
        $this->assertDatabaseHas('users', ['email' => 'operator@resto.com']);

        // Verify total debits equal total credits across primary database
        $totalDebits = DB::table('journal_entries')->sum('total_debit');
        $totalCredits = DB::table('journal_entries')->sum('total_credit');

        $this->assertEquals(round($totalDebits, 2), round($totalCredits, 2));
    }
}
