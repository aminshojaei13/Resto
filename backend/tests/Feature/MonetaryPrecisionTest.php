<?php

namespace Tests\Feature;

use App\Models\User;
use App\Services\AccountingService;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MonetaryPrecisionTest extends TestCase
{
    use RefreshDatabase;

    protected AccountingService $accountingService;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        $this->actingAs(User::find('usr_admin_1'));
        $this->accountingService = app(AccountingService::class);
    }

    public function test_floating_point_addition_rounding(): void
    {
        $val1 = 0.1;
        $val2 = 0.2;
        $sum = round($val1 + $val2, 2);

        $this::assertEquals(0.3, $sum);
    }

    public function test_checkout_pricing_and_tax_precision(): void
    {
        $payload = [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_name' => 'Precision Test Customer',
            'payment_method' => 'CASH',
            'items' => [
                [
                    'product_id' => 'prod_1',
                    'product_name' => 'ProBook Ultra 15 M3',
                    'sku' => 'APX-LAP-001',
                    'price' => 19.99,
                    'quantity' => 3,
                    'discount_percent' => 5.0, // 59.97 * 0.05 = 2.9985 -> round to 3.00
                    'tax_rate' => 0.08 // (59.97 - 3.00) * 0.08 = 56.97 * 0.08 = 4.5576 -> round to 4.56
                ]
            ]
        ];

        $response = $this->postJson('/api/v1/orders/checkout', $payload, ['X-Tenant-ID' => 'org_apex']);
        $response->assertStatus(201);

        $order = $response->json();
        $this::assertEquals(59.97, $order['subtotal']);
        $this::assertEquals(3.00, $order['discount_amount']);
        $this::assertEquals(4.56, $order['tax_amount']);
        $this::assertEquals(61.53, $order['total_amount']);
    }

    public function test_double_entry_accounting_precision_invariant(): void
    {
        $lines = [
            [
                'account_id' => 'acc_1010',
                'type' => 'DEBIT',
                'amount' => 12.35,
            ],
            [
                'account_id' => 'acc_4010',
                'type' => 'CREDIT',
                'amount' => 12.35,
            ]
        ];

        $entry = $this->accountingService->postJournalEntry(
            'org_apex',
            'store_apex_1',
            'Monetary precision verification entry',
            $lines
        );

        $this::assertEquals(12.35, $entry->total_debit);
        $this::assertEquals(12.35, $entry->total_credit);
    }
}
