<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ApiRoutesTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);

        // Every business endpoint now requires a real authenticated user.
        $this->actingAs(User::find('usr_admin_1'));
    }

    public function test_business_endpoints_reject_unauthenticated_callers(): void
    {
        auth()->logout();

        $this->getJson('/api/v1/organizations')->assertStatus(401);
        $this->getJson('/api/v1/products')->assertStatus(401);
    }

    public function test_health_endpoint(): void
    {
        $response = $this->getJson('/api/v1/health');
        $response->assertStatus(200)
                 ->assertJsonFragment(['status' => 'healthy']);
    }

    public function test_organizations_endpoint(): void
    {
        $response = $this->getJson('/api/v1/organizations');
        $response->assertStatus(200)
                 ->assertJsonFragment(['name' => 'Apex Retail Group']);
    }

    public function test_products_endpoint(): void
    {
        $response = $this->getJson('/api/v1/products?org_id=org_apex');
        $response->assertStatus(200)
                 ->assertJsonFragment(['name' => 'ProBook Ultra 15 M3']);
    }

    public function test_barcode_lookup_endpoint(): void
    {
        $response = $this->getJson('/api/v1/products/barcode/880609123401?org_id=org_apex');
        $response->assertStatus(200)
                 ->assertJsonFragment(['sku' => 'APX-LAP-001']);
    }

    public function test_checkout_endpoint(): void
    {
        $payload = [
            'org_id' => 'org_apex',
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_name' => 'John Smith',
            'payment_method' => 'CASH',
            'items' => [
                [
                    'product_id' => 'prod_2',
                    'product_name' => 'NoiseCancel Studio Headphones',
                    'sku' => 'APX-AUD-002',
                    'price' => 249.99,
                    'quantity' => 1,
                    'discount_percent' => 0,
                    'tax_rate' => 0.08
                ]
            ]
        ];

        $response = $this->postJson('/api/v1/orders/checkout', $payload);
        $response->assertStatus(201)
                 ->assertJsonFragment(['customer_name' => 'John Smith']);
    }

    public function test_accounting_summary_endpoint(): void
    {
        $response = $this->getJson('/api/v1/accounting/summary?org_id=org_apex');
        $response->assertStatus(200)
                 ->assertJsonStructure(['total_revenue', 'today_revenue', 'total_sales_count']);
    }
}
