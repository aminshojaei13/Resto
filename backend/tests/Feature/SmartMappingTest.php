<?php

namespace Tests\Feature;

use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\User;
use App\Services\Import\Mapping\ImportMappingService;
use App\Support\Import\FileFormat;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Tests\TestCase;

class SmartMappingTest extends TestCase
{
    use RefreshDatabase;

    private ImportMappingService $service;
    protected User $userApex;
    protected User $userOther;
    protected string $orgApexId = 'org_apex';
    protected string $orgOtherId = 'org_foreign';

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        Storage::fake('local');

        $this->service = app(ImportMappingService::class);
        $this->userApex = User::find('usr_admin_1');

        $this->userOther = User::create([
            'id' => 'usr_other_tenant',
            'name' => 'Foreign User',
            'email' => 'other@tenant.com',
            'password' => bcrypt('password123'),
        ]);

        $orgOther = Organization::create([
            'id' => $this->orgOtherId,
            'name' => 'Foreign Org',
            'code' => 'FOR',
            'currency_symbol' => '﷼',
        ]);

        OrganizationMembership::create([
            'id' => 'mem_foreign',
            'user_id' => $this->userOther->id,
            'organization_id' => $orgOther->id,
            'role' => OrganizationMembership::ROLE_OWNER,
            'status' => OrganizationMembership::STATUS_ACTIVE,
            'joined_at' => now(),
        ]);
    }

    private function apexHeaders(): array
    {
        return [
            'X-Tenant-ID' => $this->orgApexId,
            'X-Store-ID' => 'store_apex_1',
        ];
    }

    public function test_exact_canonical_matching_across_all_seven_import_types(): void
    {
        $types = [
            ImportType::PRODUCTS => ['name', 'sku', 'price'],
            ImportType::CUSTOMERS => ['name'],
            ImportType::INVENTORY => ['sku', 'quantity'],
            ImportType::EXPENSES => ['title', 'category', 'amount', 'date'],
            ImportType::ORDERS => ['order_id', 'sku', 'quantity', 'unit_price', 'date'],
            ImportType::SUPPLIERS => ['name'],
            ImportType::PURCHASES => ['purchase_id', 'sku', 'quantity', 'unit_cost', 'date'],
        ];

        foreach ($types as $type => $requiredFields) {
            $detailed = $this->service->suggestMappingsDetailed($requiredFields, $type);
            $suggestions = $detailed['suggestions'];

            $this->assertCount(count($requiredFields), $suggestions);
            foreach ($suggestions as $index => $sug) {
                $expectedField = $requiredFields[$index];
                $this->assertSame($expectedField, $sug['target_field'], "Failed on {$type} -> {$expectedField}");
                $this->assertSame(1.0, $sug['confidence'], "Confidence must be 1.0 for {$type} -> {$expectedField}");
                $this->assertSame(ImportMappingService::MATCH_EXACT_CANONICAL, $sug['match_category']);
                $this->assertFalse($sug['is_ambiguous']);
                $this->assertFalse($sug['has_conflict']);
                $this->assertNotEmpty($sug['reason_fa']);
                $this->assertNotEmpty($sug['reason_en']);
            }
        }
    }

    public function test_exact_alias_matching_persian_and_english(): void
    {
        // Products
        $headers = ['نام محصول', 'کد فنی', 'فی فروش', 'uom'];
        $detailed = $this->service->suggestMappingsDetailed($headers, ImportType::PRODUCTS);
        $s = $detailed['suggestions'];

        $this->assertSame('name', $s[0]['target_field']);
        $this->assertSame(ImportMappingService::MATCH_EXACT_ALIAS, $s[0]['match_category']);
        $this->assertSame(0.95, $s[0]['confidence']);

        $this->assertSame('sku', $s[1]['target_field']);
        $this->assertSame(ImportMappingService::MATCH_EXACT_ALIAS, $s[1]['match_category']);
        $this->assertSame(0.95, $s[1]['confidence']);

        $this->assertSame('price', $s[2]['target_field']);
        $this->assertSame(ImportMappingService::MATCH_EXACT_ALIAS, $s[2]['match_category']);
        $this->assertSame(0.95, $s[2]['confidence']);

        $this->assertSame('unit', $s[3]['target_field']);
        $this->assertSame(ImportMappingService::MATCH_EXACT_ALIAS, $s[3]['match_category']);
        $this->assertSame(0.95, $s[3]['confidence']);
    }

    public function test_normalized_matching_handles_diacritics_arabic_chars_and_zwnj(): void
    {
        // Headers with diacritics (harakat), Arabic Ye/Kaf, ZWNJ, and compound words without spaces
        $candidate1 = $this->service->suggestMappingsDetailed(['قِیمَت'], ImportType::PRODUCTS)['suggestions'][0];
        $this->assertSame('price', $candidate1['target_field']);
        $this->assertSame(ImportMappingService::MATCH_NORMALIZED_ALIAS, $candidate1['match_category']);
        $this->assertSame(0.90, $candidate1['confidence']);

        $candidate2 = $this->service->suggestMappingsDetailed(['كُد كالا'], ImportType::PRODUCTS)['suggestions'][0];
        $this->assertSame('sku', $candidate2['target_field']);
        $this->assertSame(ImportMappingService::MATCH_NORMALIZED_ALIAS, $candidate2['match_category']);
        $this->assertSame(0.90, $candidate2['confidence']);

        $candidate3 = $this->service->suggestMappingsDetailed(["نام\u{200C}محصول"], ImportType::PRODUCTS)['suggestions'][0];
        $this->assertSame('name', $candidate3['target_field']);
        $this->assertSame(ImportMappingService::MATCH_NORMALIZED_ALIAS, $candidate3['match_category']);
        $this->assertSame(0.90, $candidate3['confidence']);

        $candidate4 = $this->service->suggestMappingsDetailed(['کدکالا'], ImportType::PRODUCTS)['suggestions'][0];
        $this->assertSame('sku', $candidate4['target_field']);
        $this->assertSame(ImportMappingService::MATCH_NORMALIZED_ALIAS, $candidate4['match_category']);
        $this->assertSame(0.90, $candidate4['confidence']);
    }

    public function test_fuzzy_high_matching(): void
    {
        // Header with prefix matching an alias of description ('توضیحات')
        $headers = ['توضیحات تکمیلی'];
        $detailed = $this->service->suggestMappingsDetailed($headers, ImportType::PRODUCTS);
        $sug = $detailed['suggestions'][0];

        $this->assertSame('description', $sug['target_field']);
        $this->assertSame(ImportMappingService::MATCH_FUZZY_HIGH, $sug['match_category']);
        $this->assertSame(0.80, $sug['confidence']);
    }

    public function test_ambiguous_header_matching_multiple_fields_is_left_unmapped(): void
    {
        // 'توضیحات تکمیلی کالا' starts with 'توضیحات' (description alias) and ends with 'کالا' (name alias)
        // Both match with fuzzy 0.80, triggering ambiguity!
        $headers = ['توضیحات تکمیلی کالا'];
        $detailed = $this->service->suggestMappingsDetailed($headers, ImportType::PRODUCTS);
        $sug = $detailed['suggestions'][0];

        $this->assertNull($sug['target_field']);
        $this->assertNull($sug['confidence']);
        $this->assertTrue($sug['is_ambiguous']);
        $this->assertSame(ImportMappingService::MATCH_AMBIGUOUS, $sug['match_category']);
        $this->assertContains('name', $sug['candidate_fields']);
        $this->assertContains('description', $sug['candidate_fields']);

        // Check ambiguous_headers collection
        $this->assertNotEmpty($detailed['ambiguous_headers']);
        $this->assertSame('توضیحات تکمیلی کالا', $detailed['ambiguous_headers'][0]['source_column']);
    }

    public function test_competing_source_columns_with_equal_confidence_flags_conflict_and_leaves_unmapped(): void
    {
        // Two columns competing for SKU with equal alias confidence (0.95)
        $headers = ['کد کالا', 'کد محصول'];
        $detailed = $this->service->suggestMappingsDetailed($headers, ImportType::PRODUCTS);

        $sug1 = $detailed['suggestions'][0];
        $sug2 = $detailed['suggestions'][1];

        // Neither silently wins
        $this->assertNull($sug1['target_field']);
        $this->assertNull($sug1['confidence']);
        $this->assertTrue($sug1['has_conflict']);
        $this->assertSame(ImportMappingService::MATCH_CONFLICT, $sug1['match_category']);
        $this->assertContains('کد محصول', $sug1['competing_columns']);

        $this->assertNull($sug2['target_field']);
        $this->assertNull($sug2['confidence']);
        $this->assertTrue($sug2['has_conflict']);
        $this->assertSame(ImportMappingService::MATCH_CONFLICT, $sug2['match_category']);
        $this->assertContains('کد کالا', $sug2['competing_columns']);

        // Check conflicts array
        $this->assertNotEmpty($detailed['conflicts']);
        $conflict = $detailed['conflicts'][0];
        $this->assertSame('sku', $conflict['target_field']);
        $this->assertNull($conflict['dominant_column']);
        $this->assertEquals(['کد کالا', 'کد محصول'], $conflict['competing_columns']);
    }

    public function test_competing_source_columns_with_dominant_column_assigns_winner_and_flags_loser(): void
    {
        // 'sku' (exact canonical 1.0) vs 'کد فنی' (exact alias 0.95)
        $headers = ['sku', 'کد فنی'];
        $detailed = $this->service->suggestMappingsDetailed($headers, ImportType::PRODUCTS);

        $winner = $detailed['suggestions'][0];
        $loser = $detailed['suggestions'][1];

        // Winner assigned
        $this->assertSame('sku', $winner['target_field']);
        $this->assertSame(1.0, $winner['confidence']);
        $this->assertSame(ImportMappingService::MATCH_EXACT_CANONICAL, $winner['match_category']);
        $this->assertFalse($winner['has_conflict']);

        // Loser flagged as conflict and left unmapped
        $this->assertNull($loser['target_field']);
        $this->assertNull($loser['confidence']);
        $this->assertTrue($loser['has_conflict']);
        $this->assertSame(ImportMappingService::MATCH_CONFLICT, $loser['match_category']);
        $this->assertEquals(['sku'], $loser['competing_columns']);

        // Conflicts array records dominance
        $this->assertNotEmpty($detailed['conflicts']);
        $this->assertSame('sku', $detailed['conflicts'][0]['dominant_column']);
    }

    public function test_unmapped_required_fields_list(): void
    {
        // Only provide non-required or partial headers
        $headers = ['بارکد', 'توضیحات'];
        $detailed = $this->service->suggestMappingsDetailed($headers, ImportType::PRODUCTS);

        $unmappedReq = $detailed['unmapped_required_fields'];
        $unmappedReqNames = array_column($unmappedReq, 'name');

        // Required fields for products are name, sku, price
        $this->assertContains('name', $unmappedReqNames);
        $this->assertContains('sku', $unmappedReqNames);
        $this->assertContains('price', $unmappedReqNames);
    }

    public function test_unmapped_unknown_header_is_flagged_cleanly(): void
    {
        $headers = ['ستون_کاملا_نامربوط_و_ناشناخته'];
        $detailed = $this->service->suggestMappingsDetailed($headers, ImportType::PRODUCTS);
        $sug = $detailed['suggestions'][0];

        $this->assertNull($sug['target_field']);
        $this->assertNull($sug['confidence']);
        $this->assertSame(ImportMappingService::MATCH_UNMAPPED, $sug['match_category']);
        $this->assertFalse($sug['is_ambiguous']);
        $this->assertFalse($sug['has_conflict']);
    }

    public function test_get_mapping_api_endpoint_returns_rich_smart_mapping_metadata(): void
    {
        $jobId = (string) Str::uuid();
        $sourceId = (string) Str::uuid();
        $csvContent = "نام کالا,کد کالا,قیمت فروش,ستون نامعلوم\nقهوه اسپرسو,PIZZA-01,250000,تست";
        $storagePath = "imports/{$this->orgApexId}/{$jobId}/test.csv";
        Storage::disk('local')->put($storagePath, $csvContent);

        $job = ImportJob::create([
            'id' => $jobId,
            'organization_id' => $this->orgApexId,
            'user_id' => $this->userApex->id,
            'import_type' => ImportType::PRODUCTS,
            'status' => ImportStatus::MAPPING_REQUIRED,
            'original_filename' => 'products_catalog.csv',
            'detected_format' => FileFormat::CSV,
            'file_size' => strlen($csvContent),
            'total_rows' => 1,
        ]);

        ImportSource::create([
            'id' => $sourceId,
            'import_job_id' => $jobId,
            'organization_id' => $this->orgApexId,
            'original_filename' => 'products_catalog.csv',
            'storage_path' => $storagePath,
            'file_format' => FileFormat::CSV,
            'file_size' => strlen($csvContent),
            'checksum' => hash('sha256', $csvContent),
            'upload_status' => 'uploaded',
        ]);

        ImportDataset::create([
            'id' => (string) Str::uuid(),
            'import_job_id' => $job->id,
            'import_source_id' => $sourceId,
            'organization_id' => $this->orgApexId,
            'sheet_index' => 0,
            'name' => 'Sheet1',
            'row_count' => 1,
            'headers' => ['نام کالا', 'کد کالا', 'قیمت فروش', 'ستون نامعلوم'],
            'sample_rows' => [
                ['قهوه اسپرسو', 'PIZZA-01', '250000', 'تست'],
            ],
            'status' => 'inspected',
        ]);

        $response = $this->actingAs($this->userApex)
            ->getJson("/api/v1/import/jobs/{$job->id}/mapping", $this->apexHeaders());

        $response->assertOk()
            ->assertJsonStructure([
                'job_id',
                'import_type',
                'status',
                'target_fields',
                'headers',
                'suggested_mappings' => [
                    '*' => [
                        'source_column',
                        'source_index',
                        'target_field',
                        'confidence',
                        'match_category',
                        'is_ambiguous',
                        'has_conflict',
                        'reason_fa',
                        'reason_en',
                    ],
                ],
                'conflicts',
                'ambiguous_headers',
                'unmapped_required_fields',
            ]);

        $json = $response->json();
        $this->assertSame('products', $json['import_type']);
        $this->assertCount(4, $json['suggested_mappings']);

        // First 3 headers mapped
        $this->assertSame('name', $json['suggested_mappings'][0]['target_field']);
        $this->assertSame('sku', $json['suggested_mappings'][1]['target_field']);
        $this->assertSame('price', $json['suggested_mappings'][2]['target_field']);
        // 4th header unmapped
        $this->assertNull($json['suggested_mappings'][3]['target_field']);
        $this->assertSame('unmapped', $json['suggested_mappings'][3]['match_category']);

        // All required fields were mapped, so unmapped_required_fields should be empty
        $this->assertEmpty($json['unmapped_required_fields']);
    }

    public function test_tenant_isolation_on_mapping_endpoint(): void
    {
        $jobA = ImportJob::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'user_id' => $this->userApex->id,
            'import_type' => ImportType::CUSTOMERS,
            'status' => ImportStatus::MAPPING_REQUIRED,
            'original_filename' => 'customers.csv',
            'detected_format' => FileFormat::CSV,
            'file_size' => 500,
            'total_rows' => 10,
        ]);

        // User from Foreign Org cannot access Apex's job mapping
        $response = $this->actingAs($this->userOther)
            ->getJson("/api/v1/import/jobs/{$jobA->id}/mapping", [
                'X-Tenant-ID' => $this->orgOtherId,
            ]);

        $response->assertStatus(404);
    }
}
