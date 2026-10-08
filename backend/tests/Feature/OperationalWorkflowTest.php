<?php

namespace Tests\Feature;

use App\Models\Organization;
use App\Models\Purchase;
use App\Models\User;
use App\Models\Warehouse;
use App\Services\AccountingService;
use App\Support\UnitCatalog;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * P10 — operational workflow corrections.
 *
 * Covers the seven reported problems at the API boundary: one unit vocabulary,
 * explicit warehouse choice, a configured tax rate, a usable message template,
 * a real order list, three separate purchasing concepts, and operating
 * expenses that are unmistakably not purchases.
 */
class OperationalWorkflowTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        $this->actingAs(User::find('usr_admin_1'));
    }

    private function headers(): array
    {
        return ['X-Tenant-ID' => 'org_apex', 'X-Store-ID' => 'store_apex_1'];
    }

    /* ------------------------------------------------ 1. unit of measurement */

    public function test_units_are_a_controlled_vocabulary_with_both_languages(): void
    {
        $response = $this->getJson('/api/v1/units?locale=fa', $this->headers());
        $response->assertStatus(200);

        $codes = array_column($response->json(), 'code');

        foreach (['piece', 'kilogram', 'gram', 'liter', 'meter', 'pack', 'box', 'bottle', 'set', 'serving', 'order'] as $expected) {
            $this::assertContains($expected, $codes);
        }

        $response = $this->getJson('/api/v1/units?locale=en', $this->headers());
        $english = collect($response->json())->firstWhere('code', 'piece');

        $this::assertEquals('Piece', $english['label']);

        // Technical storage terms are never what a person sees.
        $this::assertStringNotContainsString('unit_code', json_encode($english, JSON_UNESCAPED_UNICODE));
    }

    public function test_a_product_unit_must_be_a_known_unit(): void
    {
        $this->postJson('/api/v1/products', [
            'sku' => 'ALP-X-1',
            'barcode' => '111',
            'name' => 'Test product',
            'price' => 10,
            'cost_price' => 5,
            'category' => 'Test',
            'unit' => 'furlong',
        ], $this->headers())->assertStatus(422);

        // Familiar spellings are accepted and stored canonically.
        foreach (['pcs' => 'piece', 'kg' => 'kilogram', 'عدد' => 'piece', 'Pack' => 'pack'] as $input => $expected) {
            $created = $this->postJson('/api/v1/products', [
                'sku' => "SKU-$expected-" . crc32($input),
                'barcode' => '1' . crc32($input),
                'name' => "Product $input",
                'price' => 10,
                'cost_price' => 5,
                'category' => 'Test',
                'unit' => $input,
            ], $this->headers());

            $created->assertStatus(201);
            $this::assertEquals($expected, $created->json('unit'));
        }
    }

    public function test_the_same_unit_appears_on_catalog_stock_order_and_purchase(): void
    {
        // Stock
        $this->postJson('/api/v1/inventory/stock-in', [
            'warehouse_id' => 'wh_apex_1a',
            'product_id' => 'prod_1',
            'quantity' => 4,
        ], $this->headers())->assertStatus(200);

        $stock = $this->getJson('/api/v1/inventory/stock', $this->headers())->json();
        $row = collect($stock)->firstWhere('product_id', 'prod_1');

        $this::assertEquals('piece', $row['unit']);
        $this::assertEquals('عدد', $row['unit_label']);

        // Order
        $order = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Unit Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->assertStatus(201)->json();

        $this::assertEquals('piece', $order['items'][0]['unit']);

        // Purchase
        $purchase = $this->postJson('/api/v1/purchases', [
            'supplier_id' => 'sup_1',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 5, 'unit_cost' => 900]],
        ], $this->headers())->assertStatus(201)->json();

        $this::assertEquals('piece', $purchase['items'][0]['unit']);
    }

    public function test_existing_product_data_is_preserved(): void
    {
        // The seeded catalog still holds the same products and quantities.
        $products = $this->getJson('/api/v1/products', $this->headers())->json();

        $this::assertCount(3, $products);
        $this::assertEquals(1299.99, (float) collect($products)->firstWhere('id', 'prod_1')['price']);
    }

    /* ------------------------------------- 2. inventory increase / warehouse */

    public function test_stock_in_requires_a_warehouse_that_belongs_to_the_business(): void
    {
        $missing = $this->postJson('/api/v1/inventory/stock-in', [
            'product_id' => 'prod_1',
            'quantity' => 5,
        ], $this->headers());

        $missing->assertStatus(422);
        $this::assertArrayHasKey('warehouse_id', $missing->json('errors'));

        $foreign = $this->postJson('/api/v1/inventory/stock-in', [
            'warehouse_id' => 'wh_braveboy_1',
            'product_id' => 'prod_1',
            'quantity' => 5,
        ], $this->headers());

        $foreign->assertStatus(422);
        $this::assertArrayHasKey('warehouse_id', $foreign->json('errors'));

        $zero = $this->postJson('/api/v1/inventory/stock-in', [
            'warehouse_id' => 'wh_apex_1a',
            'product_id' => 'prod_1',
            'quantity' => 0,
        ], $this->headers());

        $zero->assertStatus(422);
    }

    public function test_warehouses_come_from_the_callers_own_business(): void
    {
        $warehouses = $this->getJson('/api/v1/inventory/warehouses', $this->headers())->json();

        $ids = array_column($warehouses, 'id');

        $this::assertContains('wh_apex_1a', $ids);
        $this::assertNotContains('wh_braveboy_1', $ids);
    }

    public function test_a_business_without_a_warehouse_is_told_to_create_one(): void
    {
        Warehouse::where('organization_id', 'org_apex')->delete();

        $this::getJson('/api/v1/inventory/warehouses', $this->headers())
            ->assertStatus(200)
            ->assertExactJson([]);

        $this->postJson('/api/v1/inventory/stock-in', [
            'product_id' => 'prod_1',
            'quantity' => 5,
        ], $this->headers())->assertStatus(422);
    }

    public function test_stock_in_creates_a_movement_record(): void
    {
        $this->postJson('/api/v1/inventory/stock-in', [
            'warehouse_id' => 'wh_apex_1a',
            'product_id' => 'prod_2',
            'quantity' => 7,
            'reason' => 'شمارش دستی',
        ], $this->headers())->assertStatus(200);

        $movements = $this->getJson('/api/v1/inventory/movements?product_id=prod_2', $this->headers())->json();

        $this::assertGreaterThanOrEqual(1, $movements['total']);
        $this::assertEquals(7, (int) $movements['data'][0]['quantity']);
        $this::assertEquals('IN', $movements['data'][0]['type']);
    }

    /* ------------------------------------------------------- 3. tax authority */

    public function test_the_tax_rate_comes_from_business_settings_only(): void
    {
        $settings = $this->getJson('/api/v1/business/settings', $this->headers())->json();

        $this::assertEquals(8.0, $settings['default_tax_rate']);
        $this::assertTrue($settings['can_override_tax_per_order']);

        $this->putJson('/api/v1/business/settings', ['default_tax_rate' => 7.25], $this->headers())
            ->assertStatus(200);

        $order = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Tax Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->assertStatus(201)->json();

        $this::assertEquals(7.25, $order['tax_rate']);
        $this::assertEquals(round(1299.99 * 0.0725, 2), $order['tax_amount']);
    }

    public function test_a_client_cannot_invent_its_own_tax_rate_without_permission(): void
    {
        // The permission matrix is the gate: an ordinary staff member does
        // not hold sales.tax_override.
        $this::assertFalse(\App\Support\RolePermission::allows('STAFF', \App\Support\RolePermission::SALES_TAX_OVERRIDE));
        $this::assertTrue(\App\Support\RolePermission::allows('MANAGER', \App\Support\RolePermission::SALES_TAX_OVERRIDE));
        $this::assertTrue(\App\Support\RolePermission::allows('OWNER', \App\Support\RolePermission::SALES_TAX_OVERRIDE));

        // The rule itself: a request arriving with an STAFF role gets the
        // business rate, whatever the body asked for.
        $request = \Illuminate\Http\Request::create('/api/v1/orders/checkout', 'POST', ['tax_rate' => 50]);
        $request->setUserResolver(fn () => User::find('usr_cashier_1'));

        $settings = app(\App\Services\BusinessSettingsService::class);

        $this::assertEquals(
            8.0,
            $settings->resolveTaxRate($request, 'org_apex'),
            'A caller without sales.tax_override gets the business rate, not the requested one.'
        );

        // The same request as an OWNER is honoured, because they may override.
        $ownerRequest = \Illuminate\Http\Request::create('/api/v1/orders/checkout', 'POST', ['tax_rate' => 50]);
        $ownerRequest->setUserResolver(fn () => User::find('usr_admin_1'));

        $this::assertEquals(50.0, $settings->resolveTaxRate($ownerRequest, 'org_apex'));
    }

    public function test_an_authorised_user_may_override_the_rate_for_one_order(): void
    {
        $order = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Manager Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'tax_rate' => 5,
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->assertStatus(201)->json();

        $this::assertEquals(5.0, $order['tax_rate']);
        $this::assertEquals(round(1299.99 * 0.05, 2), $order['tax_amount']);

        // The next order is back on the business default: an override is for
        // one order, not a silent change of policy.
        $next = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Manager Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->json();

        $this::assertEquals(8.0, $next['tax_rate']);
    }

    /* ---------------------------------------- 5. order list, filters and search */

    public function test_orders_can_be_searched_and_filtered_server_side(): void
    {
        $checkout = fn (string $name, string $phone) => $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => $name,
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'customer_id' => $this->postJson('/api/v1/customers', [
                'name' => $name,
                'phone' => $phone,
            ], $this->headers())->json('id'),
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->assertStatus(201)->json();

        $checkout('Searchable Person', '09120000001');
        $checkout('Someone Else', '09120000002');

        $byName = $this->getJson('/api/v1/orders?q=Searchable', $this->headers())->json();
        $this::assertEquals(1, $byName['total']);

        // Phone lives on the customer record, so the search must join it in.
        $byPhone = $this->getJson('/api/v1/orders?q=09120000002', $this->headers())->json();
        $this::assertEquals(1, $byPhone['total']);

        $byNumber = $this->getJson('/api/v1/orders?q=' . $byName['data'][0]['order_number'], $this->headers())->json();
        $this::assertEquals(1, $byNumber['total']);

        $pending = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Unpaid Customer',
            'payment_method' => 'CASH',
            'payment_status' => 'PENDING',
            'fulfillment_status' => 'NEW',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->assertStatus(201)->json();

        $this::assertEquals(0, $this->getJson('/api/v1/orders?status=PENDING_PAYMENT', $this->headers())->json()['total'] - 1);

        $this::getJson('/api/v1/orders?status=PENDING_PAYMENT', $this->headers())
            ->assertStatus(200)
            ->assertJsonFragment(['order_number' => $pending['order_number']]);

        // A status the backend does not store is refused, not invented.
        $this->getJson('/api/v1/orders?status=TELEPORTED', $this->headers())->assertStatus(422);
    }

    public function test_order_detail_carries_customer_items_financials_and_a_timeline(): void
    {
        $order = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Detail Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 2]],
        ], $this->headers())->assertStatus(201)->json();

        $detail = $this->getJson("/api/v1/orders/{$order['id']}", $this->headers())->json();

        $this::assertEquals($order['order_number'], $detail['order_number']);
        $this::assertEquals('piece', $detail['items'][0]['unit']);
        $this::assertEquals('Detail Customer', $detail['customer_name']);
        $this::assertEquals(8.0, $detail['tax_rate']);
        $this::assertNotEmpty($detail['timeline']);
        $this::assertContains('view', $detail['available_actions']);
        $this::assertContains('refund', $detail['available_actions']);
    }

    public function test_orders_from_another_business_are_not_readable(): void
    {
        $order = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Scoped Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->assertStatus(201)->json();

        // A person who belongs only to another business can neither list nor
        // read an order that is not theirs.
        $outsider = \App\Models\User::create([
            'id' => 'usr_outsider_1',
            'name' => 'Outsider',
            'email' => 'outsider@braveboy.com',
            'password' => \Illuminate\Support\Facades\Hash::make('password123'),
        ]);

        \App\Models\OrganizationMembership::create([
            'id' => (string) \Illuminate\Support\Str::uuid(),
            'organization_id' => 'org_braveboy',
            'user_id' => $outsider->id,
            'role' => 'Owner',
        ]);

        $this->withFreshAuth();
        $this->actingAs($outsider);

        $this->getJson('/api/v1/orders', ['X-Tenant-ID' => 'org_apex'])->assertStatus(403);
        $this->getJson("/api/v1/orders/{$order['id']}", ['X-Tenant-ID' => 'org_braveboy'])->assertStatus(404);
    }

    /* ------------------------- 6. purchase, receiving and inventory are distinct */

    public function test_ordering_a_purchase_moves_no_stock(): void
    {
        $before = collect($this->getJson('/api/v1/inventory/stock?product_id=prod_1', $this->headers())->json())
            ->firstWhere('product_id', 'prod_1');

        $purchase = $this->postJson('/api/v1/purchases', [
            'supplier_id' => 'sup_1',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 10, 'unit_cost' => 900]],
        ], $this->headers())->assertStatus(201)->json();

        $this::assertEquals('ORDERED', $purchase['status']);
        $this::assertEquals(0, $purchase['items'][0]['received_quantity']);

        $after = collect($this->getJson('/api/v1/inventory/stock?product_id=prod_1', $this->headers())->json())
            ->firstWhere('product_id', 'prod_1');

        $this::assertEquals(
            (int) ($before['quantity'] ?? 0),
            (int) ($after['quantity'] ?? 0),
            'Ordering a purchase must not change stock.'
        );
    }

    public function test_receiving_is_partial_and_shows_ordered_against_received(): void
    {
        $purchase = $this->postJson('/api/v1/purchases', [
            'supplier_id' => 'sup_1',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [
                ['product_id' => 'prod_1', 'quantity' => 10, 'unit_cost' => 900],
                ['product_id' => 'prod_2', 'quantity' => 4, 'unit_cost' => 100],
            ],
        ], $this->headers())->assertStatus(201)->json();

        $firstLine = $purchase['items'][0]['id'];
        $secondLine = $purchase['items'][1]['id'];

        // Only the first line is booked; the second stays outstanding.
        $partial = $this->postJson("/api/v1/purchases/{$purchase['id']}/receive", [
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['purchase_item_id' => $firstLine, 'received_quantity' => 6]],
        ], $this->headers())->assertStatus(200)->json();

        $this::assertEquals('PARTIALLY_RECEIVED', $partial['purchase']['status']);
        $this::assertEquals(6, $partial['purchase']['items'][0]['received_quantity']);
        $this::assertEquals(10, $partial['purchase']['items'][0]['quantity']);
        $this::assertEquals(0, $partial['purchase']['items'][1]['received_quantity']);

        // Receiving more than was ordered is refused.
        $this->postJson("/api/v1/purchases/{$purchase['id']}/receive", [
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['purchase_item_id' => $firstLine, 'received_quantity' => 99]],
        ], $this->headers())->assertStatus(422);

        // A line already closed cannot be received into again.
        $this->postJson("/api/v1/purchases/{$purchase['id']}/receive", [
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['purchase_item_id' => $firstLine, 'received_quantity' => 1]],
        ], $this->headers())->assertStatus(200);

        // The purchase closes only once every line is fully received.
        $rest = $this->postJson("/api/v1/purchases/{$purchase['id']}/receive", [
            'warehouse_id' => 'wh_apex_1a',
            'items' => [
                ['purchase_item_id' => $firstLine, 'received_quantity' => 3],
                ['purchase_item_id' => $secondLine, 'received_quantity' => 4],
            ],
        ], $this->headers())->assertStatus(200)->json();

        $this::assertEquals('RECEIVED', $rest['purchase']['status']);
        $this::assertEquals(10, $rest['purchase']['items'][0]['received_quantity']);
        $this::assertEquals(4, $rest['purchase']['items'][1]['received_quantity']);
    }

    public function test_a_purchase_can_be_received_into_a_different_warehouse(): void
    {
        $this->postJson('/api/v1/warehouses', [
            'organization_id' => 'org_apex',
            'store_id' => 'store_apex_1',
            'name' => 'Cold Store',
            'code' => 'APEX-WH2',
        ], $this->headers())->assertStatus(201);

        $coldStore = Warehouse::where('organization_id', 'org_apex')->where('code', 'APEX-WH2')->first();

        $purchase = $this->postJson('/api/v1/purchases', [
            'supplier_id' => 'sup_1',
            'items' => [['product_id' => 'prod_1', 'quantity' => 3, 'unit_cost' => 900]],
        ], $this->headers())->assertStatus(201)->json();

        $this->postJson("/api/v1/purchases/{$purchase['id']}/receive", [
            'warehouse_id' => $coldStore->id,
        ], $this->headers())->assertStatus(200);

        $stock = collect($this->getJson("/api/v1/inventory/stock?warehouse_id={$coldStore->id}", $this->headers())->json())
            ->firstWhere('product_id', 'prod_1');

        $this::assertEquals(3, (int) $stock['quantity']);
        $this::assertEquals('Cold Store', $stock['warehouse_name']);
    }

    public function test_receiving_queue_lists_only_purchases_still_awaiting_goods(): void
    {
        $waiting = $this->postJson('/api/v1/purchases', [
            'supplier_id' => 'sup_1',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 2, 'unit_cost' => 900]],
        ], $this->headers())->assertStatus(201)->json();

        $done = $this->postJson('/api/v1/purchases', [
            'supplier_id' => 'sup_1',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 2, 'unit_cost' => 900]],
        ], $this->headers())->assertStatus(201)->json();

        $this->postJson("/api/v1/purchases/{$done['id']}/receive", ['warehouse_id' => 'wh_apex_1a'], $this->headers())
            ->assertStatus(200);

        $queue = collect($this->getJson('/api/v1/purchases/receiving-queue', $this->headers())->json())
            ->pluck('id')
            ->all();

        $this::assertContains($waiting['id'], $queue);
        $this::assertNotContains($done['id'], $queue);
    }

    /* ------------------------------------------------------ 7. operating expense */

    public function test_an_expense_needs_a_title_and_a_known_category(): void
    {
        $this->postJson('/api/v1/expenses', [
            'category' => 'RENT',
            'amount' => 100,
            'date' => date('Y-m-d'),
        ], $this->headers())->assertStatus(422);

        $this->postJson('/api/v1/expenses', [
            'title' => 'Something',
            'category' => 'Alien Category',
            'amount' => 100,
            'date' => date('Y-m-d'),
        ], $this->headers())->assertStatus(422);

        $this->postJson('/api/v1/expenses', [
            'title' => 'Monthly internet',
            'category' => 'INTERNET',
            'amount' => 500000,
            'date' => date('Y-m-d'),
        ], $this->headers())->assertStatus(201);
    }

    public function test_expense_categories_cover_the_business_costs_a_shopkeeper_recognises(): void
    {
        $codes = array_column($this->getJson('/api/v1/expenses/categories?locale=fa', $this->headers())->json(), 'code');

        foreach (['RENT', 'INTERNET', 'ELECTRICITY', 'WATER', 'TRANSPORT', 'ADVERTISING', 'SALARIES', 'REPAIRS', 'SERVICES', 'FEES', 'OTHER'] as $expected) {
            $this::assertContains($expected, $codes);
        }

        $labels = collect($this->getJson('/api/v1/expenses/categories?locale=fa', $this->headers())->json())
            ->pluck('label')
            ->all();

        foreach (['اجاره', 'اینترنت', 'برق', 'حقوق'] as $word) {
            $this::assertContains($word, $labels);
        }
    }

    public function test_an_expense_posts_to_accounting_and_a_correction_reverses_it(): void
    {
        $expenseBefore = (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', AccountingService::ACCOUNT_OPERATING_EXPENSE)->value('balance');

        $expense = $this->postJson('/api/v1/expenses', [
            'title' => 'Monthly internet',
            'category' => 'INTERNET',
            'amount' => 500000,
            'date' => date('Y-m-d'),
        ], $this->headers())->assertStatus(201)->json();

        $expenseAfter = (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', AccountingService::ACCOUNT_OPERATING_EXPENSE)->value('balance');

        $this::assertEquals(500000, round($expenseAfter - $expenseBefore, 2));

        // Correcting the amount must leave the ledger matching the screen.
        $this->putJson("/api/v1/expenses/{$expense['id']}", [
            'amount' => 600000,
        ], $this->headers())->assertStatus(200);

        $corrected = (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', AccountingService::ACCOUNT_OPERATING_EXPENSE)->value('balance');

        $this::assertEquals(600000, round($corrected - $expenseBefore, 2));
    }

    public function test_deleting_an_expense_reverses_rather_than_erases_its_entry(): void
    {
        $balanceBefore = (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', AccountingService::ACCOUNT_OPERATING_EXPENSE)->value('balance');

        $expense = $this->postJson('/api/v1/expenses', [
            'title' => 'Temporary cost',
            'category' => 'SERVICES',
            'amount' => 1000,
            'date' => date('Y-m-d'),
        ], $this->headers())->assertStatus(201)->json();

        $this->assertEquals(
            1000,
            round((float) \App\Models\Account::where('organization_id', 'org_apex')
                ->where('code', AccountingService::ACCOUNT_OPERATING_EXPENSE)->value('balance') - $balanceBefore, 2)
        );

        $this->deleteJson("/api/v1/expenses/{$expense['id']}", [], $this->headers())->assertStatus(200);

        $after = (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', AccountingService::ACCOUNT_OPERATING_EXPENSE)->value('balance');

        $this::assertEquals(0, round($after - $balanceBefore, 2));

        // A reversal entry exists, so the trail is intact.
        $reversals = \App\Models\JournalEntry::where('organization_id', 'org_apex')
            ->where('reference_type', 'ExpenseReversal')
            ->count();

        $this::assertGreaterThanOrEqual(1, $reversals);
    }

    public function test_expense_dashboard_reports_this_month_today_categories_and_trend(): void
    {
        $this->postJson('/api/v1/expenses', [
            'title' => 'Rent for the month',
            'category' => 'RENT',
            'amount' => 1000,
            'date' => date('Y-m-d'),
        ], $this->headers())->assertStatus(201);

        $summary = $this->getJson('/api/v1/expenses/summary', $this->headers())->json();

        $this::assertGreaterThanOrEqual(1000, $summary['month_total']);
        $this::assertGreaterThanOrEqual(1000, $summary['today_total']);
        $this::assertCount(6, $summary['trend']);
        $this::assertEquals('RENT', $summary['by_category'][0]['category']);
    }

    public function test_an_expense_is_not_a_purchase(): void
    {
        // Recording an expense creates no inventory and no payable.
        $stockBefore = collect($this->getJson('/api/v1/inventory/stock?product_id=prod_1', $this->headers())->json())
            ->sum('quantity');

        $payableBefore = (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', AccountingService::ACCOUNT_PAYABLE)->value('balance');

        $this->postJson('/api/v1/expenses', [
            'title' => 'Electricity bill',
            'category' => 'ELECTRICITY',
            'amount' => 2500,
            'date' => date('Y-m-d'),
        ], $this->headers())->assertStatus(201);

        $stockAfter = collect($this->getJson('/api/v1/inventory/stock?product_id=prod_1', $this->headers())->json())
            ->sum('quantity');

        $payableAfter = (float) \App\Models\Account::where('organization_id', 'org_apex')
            ->where('code', AccountingService::ACCOUNT_PAYABLE)->value('balance');

        $this::assertEquals($stockBefore, $stockAfter);
        $this::assertEquals($payableBefore, $payableAfter);
    }

    /* ------------------------------------------------------- cancellation semantics */

    public function test_cancelling_a_paid_order_reverses_the_sale_and_returns_stock(): void
    {
        $this->postJson('/api/v1/inventory/stock-in', [
            'warehouse_id' => 'wh_apex_1a',
            'product_id' => 'prod_2',
            'quantity' => 10,
        ], $this->headers());

        $before = collect($this->getJson('/api/v1/inventory/stock?product_id=prod_2', $this->headers())->json())
            ->firstWhere('product_id', 'prod_2');

        $order = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Cancel Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_2', 'quantity' => 3]],
        ], $this->headers())->assertStatus(201)->json();

        $afterSale = collect($this->getJson('/api/v1/inventory/stock?product_id=prod_2', $this->headers())->json())
            ->firstWhere('product_id', 'prod_2');

        $this::assertEquals((int) $before['quantity'] - 3, (int) $afterSale['quantity']);

        $this->postJson("/api/v1/orders/{$order['id']}/cancel", [], $this->headers())
            ->assertStatus(200)
            ->assertJsonFragment(['fulfillment_status' => 'CANCELLED']);

        $afterCancel = collect($this->getJson('/api/v1/inventory/stock?product_id=prod_2', $this->headers())->json())
            ->firstWhere('product_id', 'prod_2');

        $this::assertEquals((int) $before['quantity'], (int) $afterCancel['quantity']);

        // Cancelling twice is a refusal, not a second reversal.
        $this->postJson("/api/v1/orders/{$order['id']}/cancel", [], $this->headers())->assertStatus(422);
    }

    public function test_tax_rate_stored_on_an_order_survives_a_settings_change(): void
    {
        $order = $this->postJson('/api/v1/orders/checkout', [
            'customer_name' => 'Historic Customer',
            'payment_method' => 'CASH',
            'warehouse_id' => 'wh_apex_1a',
            'items' => [['product_id' => 'prod_1', 'quantity' => 1]],
        ], $this->headers())->assertStatus(201)->json();

        $this::putJson('/api/v1/business/settings', ['default_tax_rate' => 20], $this->headers())->assertStatus(200);

        $detail = $this->getJson("/api/v1/orders/{$order['id']}", $this->headers())->json();

        $this::assertEquals(8.0, $detail['tax_rate']);
        $this::assertEquals($order['tax_amount'], $detail['tax_amount']);
    }
}