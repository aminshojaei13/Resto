<?php

namespace Tests\Feature;

use Database\Seeders\DatabaseSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MessageImportTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(DatabaseSeeder::class);
    }

    public function test_deterministic_social_message_parsing(): void
    {
        $rawText = "CALCUAPP_ORDER\nCustomer: Ali Rezaei\nPhone: +1 (555) 888-9999\nSKU: APX-LAP-001\nQuantity: 2\nAddress: Tehran, Freedom Square\nPayment: Cash";

        $response = $this->postJson('/api/v1/messages/parse', [
            'raw_text' => $rawText,
            'source' => 'manual_paste',
        ], ['X-Tenant-ID' => 'org_apex']);

        $response->assertStatus(200)
                 ->assertJsonFragment(['name' => 'Ali Rezaei'])
                 ->assertJsonFragment(['sku' => 'APX-LAP-001']);

        $parsed = $response->json();
        $this::assertEquals('Ali Rezaei', $parsed['customer']['name']);
        $this::assertEquals('+1 (555) 888-9999', $parsed['customer']['phone']);
        $this::assertEquals(1, count($parsed['items']));
        $this::assertEquals('APX-LAP-001', $parsed['items'][0]['sku']);
        $this::assertEquals(2, $parsed['items'][0]['quantity']);

        // Test checkout order creation from parsed message items
        $checkoutPayload = [
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_id' => $parsed['customer']['id'],
            'customer_name' => $parsed['customer']['name'],
            'payment_method' => $parsed['payment_method'],
            'items' => array_map(function($i) {
                return [
                    'product_id' => $i['product_id'],
                    'product_name' => $i['product_name'],
                    'sku' => $i['sku'],
                    'price' => $i['price'],
                    'quantity' => $i['quantity'],
                ];
            }, $parsed['items'])
        ];

        $checkoutRes = $this->postJson('/api/v1/orders/checkout', $checkoutPayload, [
            'X-Tenant-ID' => 'org_apex',
            'X-Idempotency-Key' => $parsed['idempotency_key']
        ]);

        $checkoutRes->assertStatus(201)
                   ->assertJsonFragment(['customer_name' => 'Ali Rezaei']);
    }
}
