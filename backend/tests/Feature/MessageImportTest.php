<?php

namespace Tests\Feature;

use App\Models\User;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Order from a customer message.
 *
 * The parser reads the way customers actually write — Persian digits,
 * "۲ عدد قهوه" — and returns a draft the operator corrects before checkout.
 * It never invents a product, and it is not described as AI anywhere.
 */
class MessageImportTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        $this->actingAs(User::find('usr_admin_1'));
    }

    private function parse(string $rawText)
    {
        return $this->postJson('/api/v1/messages/parse', [
            'raw_text' => $rawText,
            'source' => 'manual_paste',
        ], ['X-Tenant-ID' => 'org_apex']);
    }

    public function test_parses_a_real_customer_message(): void
    {
        // A product whose name is written in Persian, as a shopkeeper would
        // actually list it.
        $this->postJson('/api/v1/products', [
            'sku' => 'APX-COF-001',
            'barcode' => '990000000001',
            'name' => 'قهوه اسپرسو',
            'price' => 25000,
            'cost_price' => 12000,
            'category' => 'قهوه',
            'unit' => 'عدد',
        ], ['X-Tenant-ID' => 'org_apex'])->assertStatus(201);

        $response = $this->parse(<<<'TEXT'
        سلام
        ۲ عدد قهوه اسپرسو
        ۱ بسته NoiseCancel Studio Headphones
        پرداخت: کارت
        مشتری: علی رضایی
        تلفن: 09121234567
        TEXT);

        $response->assertStatus(200);

        $parsed = $response->json();

        $this::assertEquals('علی رضایی', $parsed['customer']['name']);
        $this::assertEquals('09121234567', $parsed['customer']['phone']);
        $this::assertEquals('CARD', $parsed['payment_method']);

        $this::assertCount(2, $parsed['items']);
        $this::assertCount(0, $parsed['unmatched_items']);

        // Persian digits are read as numbers, not as text.
        $first = $parsed['items'][0];
        $this::assertEquals(2, $first['quantity']);
        $this::assertEquals('قهوه اسپرسو', $first['product_name']);
        $this::assertEquals('APX-COF-001', $first['sku']);

        $second = $parsed['items'][1];
        $this::assertEquals(1, $second['quantity']);
        $this::assertEquals('APX-AUD-002', $second['sku']);

        // Every quantity is stated in the product's own unit, not a raw number.
        $this::assertEquals('piece', $first['unit']);
        $this::assertEquals('عدد', $first['unit_label']);
    }

    public function test_an_unrecognised_line_is_reported_rather_than_invented(): void
    {
        $response = $this->parse('۳ عدد کالایی که اصلا در فروشگاه ما وجود ندارد');

        $response->assertStatus(200);

        $parsed = $response->json();

        // No product is silently substituted for a line that could not be read.
        $this::assertCount(0, $parsed['items']);
        $this::assertCount(1, $parsed['unmatched_items']);
        $this::assertEquals('no_catalog_match', $parsed['unmatched_items'][0]['reason']);

        // Parsing never registers a sale on its own.
        $this::assertTrue($parsed['requires_review']);
    }

    public function test_parse_uses_the_business_tax_rate_not_a_hardcoded_eight(): void
    {
        $first = $this->parse('۱ عدد ProBook Ultra 15 M3')->json();

        $this::assertEquals(8.0, $first['tax_rate']);
        $this::assertEquals('business_default', $first['tax_rate_source']);
        $this::assertEquals(round(1299.99 * 0.08, 2), $first['tax_amount']);

        // Change the configured rate; the next parse must follow it.
        $this->putJson('/api/v1/business/settings', [
            'default_tax_rate' => 10,
        ], ['X-Tenant-ID' => 'org_apex'])->assertStatus(200);

        $second = $this->parse('۱ عدد ProBook Ultra 15 M3')->json();

        $this::assertEquals(10.0, $second['tax_rate']);
        $this::assertEquals(round(1299.99 * 0.10, 2), $second['tax_amount']);
        $this::assertEquals(round($second['subtotal'] + $second['tax_amount'], 2), $second['grand_total']);
    }

    public function test_draft_can_be_corrected_then_checked_out(): void
    {
        $parsed = $this->parse('۲ عدد ProBook Ultra 15 M3')->json();

        $this::assertCount(1, $parsed['items']);
        $this::assertEquals(2, $parsed['items'][0]['quantity']);

        // The operator corrects the quantity before checkout. The order is
        // priced from the catalog, never from the draft or the client.
        $checkout = $this->postJson('/api/v1/orders/checkout', [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_id' => $parsed['customer']['id'],
            'customer_name' => $parsed['customer']['name'],
            'payment_method' => $parsed['payment_method'],
            'source' => 'MESSAGE',
            'items' => [[
                'product_id' => $parsed['items'][0]['product_id'],
                'quantity' => 1,
                // A tampered price is ignored in favour of the catalog price.
                'price' => 1,
            ]],
        ], ['X-Tenant-ID' => 'org_apex']);

        $checkout->assertStatus(201)
            ->assertJsonFragment(['source' => 'MESSAGE']);

        $order = $checkout->json();

        $this::assertEquals(1299.99, $order['subtotal']);
        $this::assertEquals(round(1299.99 * 0.08, 2), $order['tax_amount']);
        $this::assertEquals(8.0, $order['tax_rate']);
        $this::assertEquals(round($order['subtotal'] + $order['tax_amount'], 2), $order['total_amount']);
    }

    public function test_parses_conversational_message_with_customer_intro_and_verbs(): void
    {
        $this->postJson('/api/v1/products', [
            'sku' => 'APX-CBL-001',
            'barcode' => '990000000002',
            'name' => 'کابل USB',
            'price' => 50000,
            'cost_price' => 20000,
            'category' => 'لوازم جانبی',
            'unit' => 'عدد',
        ], ['X-Tenant-ID' => 'org_apex'])->assertStatus(201);

        $response = $this->parse('حسینی هستتم ۱۰ عدد کابل usb میخوام');

        $response->assertStatus(200);

        $parsed = $response->json();

        $this::assertEquals('حسینی', $parsed['customer']['name']);
        $this::assertCount(1, $parsed['items']);
        $this::assertCount(0, $parsed['unmatched_items']);

        $item = $parsed['items'][0];
        $this::assertEquals(10, $item['quantity']);
        $this::assertEquals('کابل USB', $item['product_name']);
        $this::assertNotEmpty($item['product_id']);

        // Check checkout with parsed item
        $checkout = $this->postJson('/api/v1/orders/checkout', [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_name' => $parsed['customer']['name'],
            'payment_method' => 'CASH',
            'source' => 'MESSAGE',
            'items' => [[
                'product_id' => $item['product_id'],
                'quantity' => $item['quantity'],
            ]],
        ], ['X-Tenant-ID' => 'org_apex']);

        $checkout->assertStatus(201);
    }
}