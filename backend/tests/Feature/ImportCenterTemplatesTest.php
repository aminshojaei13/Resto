<?php

namespace Tests\Feature;

use App\Models\User;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Template\ImportTemplateService;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ImportCenterTemplatesTest extends TestCase
{
    use RefreshDatabase;

    protected User $userApex;
    protected string $orgApexId = 'org_apex';

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        $this->userApex = User::find('usr_admin_1');
    }

    private function headers(): array
    {
        return [
            'X-Tenant-ID' => $this->orgApexId,
            'X-Store-ID' => 'store_apex_1',
            'Accept' => 'application/json',
        ];
    }

    public function test_unauthenticated_request_is_rejected(): void
    {
        $response = $this->getJson('/api/v1/import/templates', [
            'X-Tenant-ID' => $this->orgApexId,
        ]);
        $response->assertStatus(401);

        $downloadResponse = $this->get('/api/v1/import/templates/products', [
            'X-Tenant-ID' => $this->orgApexId,
            'Accept' => 'application/json',
        ]);
        $downloadResponse->assertStatus(401);
    }

    public function test_catalog_lists_all_seven_supported_import_templates(): void
    {
        $response = $this->actingAs($this->userApex)
            ->getJson('/api/v1/import/templates', $this->headers());

        $response->assertStatus(200);

        $templates = $response->json('templates');
        $this->assertIsArray($templates);
        $this->assertCount(7, $templates);

        $expectedTypes = [
            'products',
            'customers',
            'inventory',
            'expenses',
            'orders',
            'suppliers',
            'purchases',
        ];

        $returnedTypes = array_column($templates, 'type');
        $this->assertEquals($expectedTypes, $returnedTypes);

        foreach ($templates as $tpl) {
            $this->assertNotEmpty($tpl['title_fa']);
            $this->assertNotEmpty($tpl['title_en']);
            $this->assertNotEmpty($tpl['description_fa']);
            $this->assertNotEmpty($tpl['description_en']);
            $this->assertNotEmpty($tpl['filename']);
            $this->assertNotEmpty($tpl['headers']);
            $this->assertNotEmpty($tpl['field_keys']);
            $this->assertArrayHasKey('is_multiline', $tpl);
            $this->assertArrayHasKey('domain_notes_fa', $tpl);
            $this->assertArrayHasKey('domain_notes_en', $tpl);
        }
    }

    public function test_download_template_csv_for_all_seven_types_contains_utf8_bom(): void
    {
        $types = [
            'products',
            'customers',
            'inventory',
            'expenses',
            'orders',
            'suppliers',
            'purchases',
        ];

        $mappingService = app(ImportMappingService::class);
        $templateService = app(ImportTemplateService::class);

        foreach ($types as $type) {
            $tpl = $templateService->getTemplate($type);
            $expectedFilename = $tpl['filename'];

            $response = $this->actingAs($this->userApex)
                ->get("/api/v1/import/templates/{$type}", [
                    'X-Tenant-ID' => $this->orgApexId,
                    'X-Store-ID' => 'store_apex_1',
                ]);

            $response->assertStatus(200);
            $this->assertStringContainsString('text/csv', $response->headers->get('content-type'));
            $this->assertStringContainsString("attachment; filename=\"{$expectedFilename}\"", $response->headers->get('content-disposition'));

            $content = $response->streamedContent();

            // Check UTF-8 BOM
            $this->assertTrue(str_starts_with($content, "\xEF\xBB\xBF"), "Template for {$type} must start with UTF-8 BOM");

            // Strip BOM and parse CSV lines
            $cleanContent = substr($content, 3);
            $lines = explode("\n", trim($cleanContent));
            $this->assertGreaterThanOrEqual(2, count($lines), "Template for {$type} must contain header and sample rows");

            $headerLine = array_shift($lines);
            $headers = str_getcsv($headerLine);

            $this->assertNotEmpty($headers, "Headers for {$type} should not be empty");

            // Verify headers map cleanly with suggestMappings
            $suggested = $mappingService->suggestMappings($headers, $type);
            $this->assertNotEmpty($suggested, "Suggested mappings for {$type} should not be empty");

            // Check that at least the required target fields are matched
            $targetFields = $mappingService->getTargetFields($type);
            $suggestedTargets = array_filter(array_column($suggested, 'target_field'));

            foreach ($targetFields as $tf) {
                if ($tf['required'] ?? false) {
                    $this->assertContains(
                        $tf['name'],
                        $suggestedTargets,
                        "Required field '{$tf['name']}' in '{$type}' should be mapped by the template headers"
                    );
                }
            }
        }
    }

    public function test_orders_template_demonstrates_multiline_order_items(): void
    {
        $response = $this->actingAs($this->userApex)
            ->get('/api/v1/import/templates/orders', [
                'X-Tenant-ID' => $this->orgApexId,
                'X-Store-ID' => 'store_apex_1',
            ]);
        $response->assertStatus(200);

        $content = substr($response->streamedContent(), 3);
        $lines = array_map('str_getcsv', explode("\n", trim($content)));
        $headers = array_shift($lines);

        $orderIdIndex = array_search('شناسه سفارش', $headers);
        $this->assertNotFalse($orderIdIndex, "Orders template must contain 'شناسه سفارش' header");

        $orderIds = array_column($lines, $orderIdIndex);
        $counts = array_count_values($orderIds);
        $hasGroupedOrder = false;
        foreach ($counts as $num => $cnt) {
            if ($cnt > 1) {
                $hasGroupedOrder = true;
                break;
            }
        }
        $this->assertTrue($hasGroupedOrder, "Orders template must demonstrate multi-line item grouping under the same order id");
    }

    public function test_purchases_template_demonstrates_multiline_purchase_items(): void
    {
        $response = $this->actingAs($this->userApex)
            ->get('/api/v1/import/templates/purchases', [
                'X-Tenant-ID' => $this->orgApexId,
                'X-Store-ID' => 'store_apex_1',
            ]);
        $response->assertStatus(200);

        $content = substr($response->streamedContent(), 3);
        $lines = array_map('str_getcsv', explode("\n", trim($content)));
        $headers = array_shift($lines);

        $invoiceIdIndex = array_search('شناسه خرید', $headers);
        $this->assertNotFalse($invoiceIdIndex, "Purchases template must contain 'شناسه خرید' header");

        $invoiceIds = array_column($lines, $invoiceIdIndex);
        $counts = array_count_values($invoiceIds);
        $hasGroupedPurchase = false;
        foreach ($counts as $num => $cnt) {
            if ($cnt > 1) {
                $hasGroupedPurchase = true;
                break;
            }
        }
        $this->assertTrue($hasGroupedPurchase, "Purchases template must demonstrate multi-line item grouping under the same invoice id");
    }

    public function test_requesting_non_existent_template_type_returns_404(): void
    {
        $response = $this->actingAs($this->userApex)
            ->getJson('/api/v1/import/templates/invalid_non_existent_type', $this->headers());
        $response->assertStatus(404);
    }
}
