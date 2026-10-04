<?php

namespace Tests\Feature;

use App\Models\User;
use App\Services\AccountingService;
use App\Support\Money;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Money must never depend on binary floating point.
 *
 * Prices, discounts and tax are computed server side from the catalog and the
 * business tax setting, in integer minor units.
 */
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

    public function test_minor_unit_arithmetic_is_exact(): void
    {
        // The classic binary-float trap: these do not sum to 0.30 in floating
        // point, and must not be allowed to decide an order total.
        $this::assertEquals(0.30, Money::fromMinor(Money::add(Money::toMinor(0.1), Money::toMinor(0.2))));

        // 5697.00 at 8% = 455.76, held as 45576 minor units throughout.
        $this::assertEquals(45576, Money::percentageOf(569700, 800));
        $this::assertEquals(455.76, Money::fromMinor(Money::percentageOf(569700, 800)));
        $this::assertEquals(45576, Money::percentageOf(569700, Money::rateToMinor(8)));
        $this::assertEquals(0, Money::percentageOf(569700, 0));

        // A rate of 8.25% is carried exactly, not as 8.250000000000001.
        $this::assertEquals(8.25, Money::fromMinor(Money::rateToMinor(8.25)));
    }

    public function test_checkout_is_priced_from_the_catalog_and_the_configured_tax_rate(): void
    {
        // prod_1 costs 1299.99 with a 5% line discount.
        $response = $this->postJson('/api/v1/orders/checkout', [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_name' => 'Precision Test Customer',
            'payment_method' => 'CASH',
            'items' => [[
                'product_id' => 'prod_1',
                'quantity' => 3,
                'discount_percent' => 5,
                // A tampered price is ignored: the catalog price is used.
                'price' => 19.99,
            ]],
        ], ['X-Tenant-ID' => 'org_apex']);

        $response->assertStatus(201);

        $order = $response->json();

        $subtotal = round(1299.99 * 3, 2);            // 3899.97
        $discount = round($subtotal * 0.05, 2);      // 195.00
        $taxable = round($subtotal - $discount, 2);  // 3704.97
        $tax = round($taxable * 0.08, 2);            // 296.40

        $this::assertEquals($subtotal, $order['subtotal']);
        $this::assertEquals($discount, $order['discount_amount']);
        $this::assertEquals($tax, $order['tax_amount']);
        $this::assertEquals(round($taxable + $tax, 2), $order['total_amount']);
        $this::assertEquals(8.0, $order['tax_rate']);

        // The line records the product's own unit, not a bare number.
        $this::assertEquals('piece', $order['items'][0]['unit']);
    }

    public function test_changing_the_configured_rate_changes_the_tax_and_the_journal(): void
    {
        $items = [[
            'product_id' => 'prod_1',
            'quantity' => 2,
        ]];

        $before = $this->postJson('/api/v1/orders/checkout', [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_name' => 'Tax Customer',
            'payment_method' => 'CASH',
            'items' => $items,
        ], ['X-Tenant-ID' => 'org_apex'])->json();

        $this::assertEquals(round(1299.99 * 2 * 0.08, 2), $before['tax_amount']);

        $this->putJson('/api/v1/business/settings', [
            'default_tax_rate' => 9.5,
        ], ['X-Tenant-ID' => 'org_apex'])->assertStatus(200);

        $after = $this->postJson('/api/v1/orders/checkout', [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_name' => 'Tax Customer',
            'payment_method' => 'CASH',
            'items' => $items,
        ], ['X-Tenant-ID' => 'org_apex'])->json();

        $this::assertEquals(9.5, $after['tax_rate']);
        $this::assertEquals(round(1299.99 * 2 * 0.095, 2), $after['tax_amount']);
        $this::assertEquals(round($after['subtotal'] + $after['tax_amount'], 2), $after['total_amount']);

        // The tax is a liability, not revenue: the sale entry credits it
        // separately and still balances.
        $taxAccount = \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', \App\Services\AccountingService::ACCOUNT_TAX_PAYABLE)
            ->first();

        $this::assertNotNull($taxAccount, 'A tax payable account must exist once tax is charged.');

        $revenue = \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', \App\Services\AccountingService::ACCOUNT_SALES_REVENUE)
            ->first();

        $cash = \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', \App\Services\AccountingService::ACCOUNT_CASH)
            ->first();

        $revenueBefore = (float) $revenue->balance;
        $taxBefore = (float) $taxAccount->balance;
        $cashBefore = (float) $cash->balance;

        $third = $this->postJson('/api/v1/orders/checkout', [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_name' => 'Tax Customer',
            'payment_method' => 'CASH',
            'items' => $items,
        ], ['X-Tenant-ID' => 'org_apex'])->json();

        // One order moves cash by the gross, revenue by the net, and the tax
        // liability by the tax. Revenue is never credited the gross.
        $gross = Money::toMinor($third['total_amount']);
        $tax = Money::toMinor($third['tax_amount']);

        $balance = fn (string $code) => (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', $code)
            ->value('balance');

        $this::assertEquals(Money::fromMinor($gross), round($balance('1010') - $cashBefore, 2));
        $this::assertEquals(Money::fromMinor($tax), round($balance('2020') - $taxBefore, 2));
        // Revenue and the tax liability are both credit-normal, so a sale
        // grows both by their own amount, never the gross.
        $this::assertEquals(
            Money::fromMinor($gross - $tax),
            round($balance('4010') - $revenueBefore, 2)
        );
    }

    public function test_tax_rate_validation_rejects_impossible_values(): void
    {
        foreach ([-1, 101, 'abc'] as $invalid) {
            $this->putJson('/api/v1/business/settings', [
                'default_tax_rate' => $invalid,
            ], ['X-Tenant-ID' => 'org_apex'])->assertStatus(422);
        }

        $this::putJson('/api/v1/business/settings', [
            'default_tax_rate' => 0,
        ], ['X-Tenant-ID' => 'org_apex'])->assertStatus(200);
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