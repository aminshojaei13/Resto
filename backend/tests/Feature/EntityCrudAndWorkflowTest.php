<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class EntityCrudAndWorkflowTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
    }

    public function test_product_crud_lifecycle(): void
    {
        // 1. Create Product
        $createPayload = [
            'org_id' => 'org_apex',
            'sku' => 'TEST-SKU-100',
            'barcode' => '999111222',
            'name' => 'Smart Watch Pro',
            'description' => 'Fitness and health tracking watch',
            'price' => 199.99,
            'cost_price' => 100.00,
            'category' => 'Wearables',
            'unit' => 'pcs',
        ];

        $createRes = $this->postJson('/api/v1/products', $createPayload, ['X-Tenant-ID' => 'org_apex']);
        $createRes->assertStatus(201)
                  ->assertJsonFragment(['name' => 'Smart Watch Pro']);

        $productId = $createRes->json('id');

        // 2. Read Product Details
        $readRes = $this->getJson("/api/v1/products/{$productId}", ['X-Tenant-ID' => 'org_apex']);
        $readRes->assertStatus(200)
                ->assertJsonFragment(['sku' => 'TEST-SKU-100']);

        // 3. Update Product
        $updatePayload = [
            'name' => 'Smart Watch Pro v2',
            'price' => 229.99,
            'cost_price' => 110.00,
        ];

        $updateRes = $this->putJson("/api/v1/products/{$productId}", $updatePayload, ['X-Tenant-ID' => 'org_apex']);
        $updateRes->assertStatus(200)
                  ->assertJsonFragment(['name' => 'Smart Watch Pro v2']);

        // 4. Delete Product
        $deleteRes = $this->deleteJson("/api/v1/products/{$productId}", [], ['X-Tenant-ID' => 'org_apex']);
        $deleteRes->assertStatus(200);
    }

    public function test_supplier_and_purchasing_receiving_flow(): void
    {
        // 1. Create Supplier
        $supplierRes = $this->postJson('/api/v1/suppliers', [
            'org_id' => 'org_apex',
            'name' => 'Apex Components Wholesale',
            'email' => 'contact@apexcomponents.com',
            'phone' => '+1 (555) 777-8899',
            'address' => '100 Industrial Parkway',
        ], ['X-Tenant-ID' => 'org_apex']);
        $supplierRes->assertStatus(201);
        $supplierId = $supplierRes->json('id');

        // 2. Create Purchase Order
        $purchaseRes = $this->postJson('/api/v1/purchases', [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'supplier_id' => $supplierId,
            'items' => [
                [
                    'product_id' => 'prod_1',
                    'quantity' => 5,
                    'unit_cost' => 850.00,
                ]
            ]
        ], ['X-Tenant-ID' => 'org_apex']);
        $purchaseRes->assertStatus(201);
        $purchaseId = $purchaseRes->json('id');

        // 3. Receive Goods into Inventory
        $receiveRes = $this->postJson("/api/v1/purchases/{$purchaseId}/receive", [], ['X-Tenant-ID' => 'org_apex']);
        $receiveRes->assertStatus(200)
                   ->assertJsonFragment(['message' => 'Purchase goods received into inventory successfully']);

        // 4. Pay Supplier
        $payRes = $this->postJson("/api/v1/purchases/{$purchaseId}/pay", [
            'amount' => 4250.00,
            'payment_method' => 'BANK_TRANSFER'
        ], ['X-Tenant-ID' => 'org_apex']);
        $payRes->assertStatus(200);
    }

    public function test_tenant_authorization_enforcement(): void
    {
        // User 'usr_cashier_1' is a member of 'org_apex', but NOT 'org_braveboy'
        $cashier = User::find('usr_cashier_1');

        $this->actingAs($cashier);

        // Accessing org_apex (authorized)
        $authorizedRes = $this->getJson('/api/v1/products', ['X-Tenant-ID' => 'org_apex']);
        $authorizedRes->assertStatus(200);

        // Accessing org_braveboy (unauthorized for cashier)
        $unauthorizedRes = $this->getJson('/api/v1/products', ['X-Tenant-ID' => 'org_braveboy']);
        $unauthorizedRes->assertStatus(403)
                        ->assertJsonFragment(['error' => 'Unauthorized organization access']);
    }

    public function test_idempotency_key_middleware(): void
    {
        $idempotencyKey = 'IDEM-TEST-123456';

        $payload = [
            'store_id' => 'store_apex_1',
            'category' => 'Store Supplies',
            'amount' => 150.00,
            'date' => date('Y-m-d'),
            'notes' => 'Paper and printer ink',
        ];

        // First Request
        $res1 = $this->postJson('/api/v1/expenses', $payload, [
            'X-Tenant-ID' => 'org_apex',
            'X-Idempotency-Key' => $idempotencyKey,
        ]);
        $res1->assertStatus(201);

        // Second Request with identical key
        $res2 = $this->postJson('/api/v1/expenses', $payload, [
            'X-Tenant-ID' => 'org_apex',
            'X-Idempotency-Key' => $idempotencyKey,
        ]);
        $res2->assertStatus(201);
        $res2->assertHeader('X-Cache-Lookup', 'HIT-IDEMPOTENT');

        // Verify expense was created only ONCE in database
        $count = \App\Models\Expense::where('notes', 'Paper and printer ink')->count();
        $this->assertEquals(1, $count);
    }
}
