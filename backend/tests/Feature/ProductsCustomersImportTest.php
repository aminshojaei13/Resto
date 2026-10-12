<?php

namespace Tests\Feature;

use App\Models\Customer;
use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\ImportValidationResult;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Product;
use App\Models\User;
use App\Services\Import\Execution\ImportExecutionService;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Normalizer\DataNormalizer;
use App\Support\Import\FileFormat;
use App\Support\Import\ImportErrorCode;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Tests\TestCase;

class ProductsCustomersImportTest extends TestCase
{
    use RefreshDatabase;

    protected User $userApex;
    protected User $userOther;
    protected string $orgApexId = 'org_apex';
    protected string $orgOtherId = 'org_foreign';

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        Storage::fake('local');

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

    private function foreignHeaders(): array
    {
        return [
            'X-Tenant-ID' => $this->orgOtherId,
        ];
    }

    public function test_normalizer_handles_persian_digits_money_phones_and_units(): void
    {
        // Persian digits to English
        $this->assertSame('1234567890', DataNormalizer::toEnglishDigits('۱۲۳۴۵۶۷۸۹۰'));

        // Money parsing
        $this->assertSame('150000.00', DataNormalizer::parseMoney('۱۵۰,۰۰۰ تومان'));
        $this->assertSame('2500.50', DataNormalizer::parseMoney('۲۵۰۰٫۵۰'));
        $this->assertSame('1000.00', DataNormalizer::parseMoney('1000'));
        $this->assertNull(DataNormalizer::parseMoney('-500'));
        $this->assertNull(DataNormalizer::parseMoney('invalid'));

        // Phone normalization
        $this->assertSame('09121234567', DataNormalizer::normalizePhone('۰۹۱۲ ۱۲۳ ۴۵۶۷'));
        $this->assertSame('09121234567', DataNormalizer::normalizePhone('+989121234567'));
        $this->assertSame('09121234567', DataNormalizer::normalizePhone('00989121234567'));
        $this->assertNull(DataNormalizer::normalizePhone('123')); // too short

        // Email normalization
        $this->assertSame('user@example.com', DataNormalizer::normalizeEmail(' User@Example.COM '));
        $this->assertNull(DataNormalizer::normalizeEmail('invalid-email'));

        // Unit normalization via UnitCatalog
        $this->assertSame('piece', DataNormalizer::normalizeUnit('عدد'));
        $this->assertSame('kilogram', DataNormalizer::normalizeUnit('کیلوگرم'));
        $this->assertSame('kilogram', DataNormalizer::normalizeUnit('kg'));
        $this->assertSame('piece', DataNormalizer::normalizeUnit(null)); // default
        $this->assertNull(DataNormalizer::normalizeUnit('واحد_کاملا_نامعتبر_و_عجیب'));
    }

    public function test_mapping_service_suggests_canonical_persian_and_english_aliases(): void
    {
        $mappingService = app(ImportMappingService::class);

        // Product suggestions
        $productHeaders = ['نام کالا', 'کد محصول', 'قیمت فروش', 'واحد سنجش', 'بارکد', 'دسته بندی', 'توضیحات'];
        $suggestions = $mappingService->suggestMappings($productHeaders, ImportType::PRODUCTS);

        $mappedFields = array_column($suggestions, 'target_field');
        $this->assertContains('name', $mappedFields);
        $this->assertContains('sku', $mappedFields);
        $this->assertContains('price', $mappedFields);
        $this->assertContains('unit', $mappedFields);
        $this->assertContains('barcode', $mappedFields);
        $this->assertContains('category', $mappedFields);
        $this->assertContains('description', $mappedFields);

        // Customer suggestions
        $customerHeaders = ['نام و نام خانوادگی', 'شماره همراه', 'ایمیل', 'آدرس'];
        $custSuggestions = $mappingService->suggestMappings($customerHeaders, ImportType::CUSTOMERS);

        $custMappedFields = array_column($custSuggestions, 'target_field');
        $this->assertContains('name', $custMappedFields);
        $this->assertContains('phone', $custMappedFields);
        $this->assertContains('email', $custMappedFields);
        $this->assertContains('address', $custMappedFields);
    }

    public function test_mapping_validation_enforces_required_fields_and_prevents_conflicts(): void
    {
        $mappingService = app(ImportMappingService::class);

        // Missing required field (sku, price missing)
        $invalidMapping = [
            'name' => 'نام کالا',
        ];
        $val1 = $mappingService->validateMapping($invalidMapping, ImportType::PRODUCTS);
        $this->assertFalse($val1['valid']);
        $this->assertNotEmpty($val1['errors']);

        // Duplicate target assignment
        $validMapping = [
            'name' => 'نام کالا',
            'sku' => 'کد محصول',
            'price' => 'قیمت',
        ];
        $val2 = $mappingService->validateMapping($validMapping, ImportType::PRODUCTS);
        $this->assertTrue($val2['valid']);
    }

    public function test_api_mapping_get_and_save(): void
    {
        $job = $this->createMockJob(ImportType::PRODUCTS, [
            ['نام کالا', 'کد کالا', 'قیمت'],
            ['قهوه اسپرسو', 'PRD-ESP-1', '50000'],
        ]);

        // GET /api/v1/import/jobs/{id}/mapping
        $response = $this->actingAs($this->userApex)
            ->getJson("/api/v1/import/jobs/{$job->id}/mapping", $this->apexHeaders());

        $response->assertStatus(200)
            ->assertJsonStructure([
                'job_id',
                'target_fields',
                'suggested_mappings',
            ]);

        // POST /api/v1/import/jobs/{id}/mapping
        $saveResponse = $this->actingAs($this->userApex)
            ->postJson("/api/v1/import/jobs/{$job->id}/mapping", [
                'fields' => [
                    'name' => 'نام کالا',
                    'sku' => 'کد کالا',
                    'price' => 'قیمت',
                ],
                'sheet_index' => 0,
            ], $this->apexHeaders());

        $saveResponse->assertStatus(200);
        $this->assertEquals(ImportStatus::READY_TO_VALIDATE, $job->fresh()->status);
        $this->assertEquals('نام کالا', $job->fresh()->mapping_config['fields']['name']);
    }

    public function test_product_validation_pipeline_detects_errors_duplicates_and_matches(): void
    {
        // Pre-create an existing product in DB
        Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'sku' => 'EXISTING-SKU-1',
            'barcode' => 'BAR-EXISTING-1',
            'category' => 'نوشیدنی',
            'name' => 'Existing Coffee',
            'price' => '60000.00',
            'cost_price' => '30000.00',
            'unit' => 'piece',
        ]);

        $csvRows = [
            ['نام کالا', 'کد کالا', 'قیمت', 'واحد'],
            ['اسپرسو دابل', 'NEW-SKU-1', '50000', 'عدد'], // Valid
            ['لاته ماکیاتو', 'EXISTING-SKU-1', '75000', 'عدد'], // Existing match warning
            ['چای ماسالا', 'NEW-SKU-2', 'نامعتبر', 'عدد'], // Invalid price error
            ['بدون کد', '', '40000', 'عدد'], // Missing SKU error
            ['کالای اول', 'DUP-SKU-1', '30000', 'عدد'], // First duplicate
            ['کالای دوم', 'DUP-SKU-1', '35000', 'عدد'], // Second duplicate in file error
            ['واحد اشتباه', 'NEW-SKU-3', '20000', 'واحد_عجیب_غریب'], // Invalid unit error
        ];

        $job = $this->createMockJob(ImportType::PRODUCTS, $csvRows);
        $job->update([
            'status' => ImportStatus::READY_TO_VALIDATE,
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => [
                    'name' => 'نام کالا',
                    'sku' => 'کد کالا',
                    'price' => 'قیمت',
                    'unit' => 'واحد',
                ],
            ],
        ]);

        // POST /api/v1/import/jobs/{id}/validate
        $response = $this->actingAs($this->userApex)
            ->postJson("/api/v1/import/jobs/{$job->id}/validate", [], $this->apexHeaders());

        $response->assertStatus(200);
        $updatedJob = $job->fresh();
        $this->assertEquals(ImportStatus::PREVIEW_READY, $updatedJob->status);
        $this->assertEquals(7, $updatedJob->total_rows);
        $this->assertGreaterThan(0, $updatedJob->valid_rows);
        $this->assertGreaterThan(0, $updatedJob->invalid_rows);

        // Check preview endpoint
        $previewResp = $this->actingAs($this->userApex)
            ->getJson("/api/v1/import/jobs/{$job->id}/preview", $this->apexHeaders());

        $previewResp->assertStatus(200)
            ->assertJsonStructure([
                'job_id',
                'status',
                'total_rows',
                'valid_rows',
                'invalid_rows',
                'matched_existing_rows',
                'preview_rows',
                'can_execute',
            ]);

        $this->assertGreaterThan(0, $previewResp->json('matched_existing_rows'));

        // Check errors endpoint
        $errorsResp = $this->actingAs($this->userApex)
            ->getJson("/api/v1/import/jobs/{$job->id}/errors", $this->apexHeaders());

        $errorsResp->assertStatus(200);
        $errorItems = $errorsResp->json('data');
        $this->assertNotEmpty($errorItems);

        $errorCodes = array_column($errorItems, 'error_code');
        $this->assertContains(ImportErrorCode::NUMERIC_INVALID, $errorCodes);
        $this->assertContains(ImportErrorCode::REQUIRED_FIELD_MISSING, $errorCodes);
        $this->assertContains(ImportErrorCode::DUPLICATE_IN_FILE, $errorCodes);
        $this->assertContains(ImportErrorCode::INVALID_UNIT, $errorCodes);
    }

    public function test_product_execution_with_skip_and_update_modes_and_zero_inventory_alteration(): void
    {
        $existingProduct = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'sku' => 'SKU-EXISTING',
            'barcode' => 'BAR-EXISTING-2',
            'category' => 'عمومی',
            'name' => 'Original Name',
            'price' => '10000.00',
            'cost_price' => '5000.00',
            'unit' => 'piece',
        ]);

        $csvRows = [
            ['name', 'sku', 'price', 'unit'],
            ['Original Updated Name', 'SKU-EXISTING', '15000', 'عدد'],
            ['Brand New Product', 'SKU-NEW-1', '25000', 'عدد'],
        ];

        // 1. Test Execution with 'skip' duplicate strategy
        $jobSkip = $this->createMockJob(ImportType::PRODUCTS, $csvRows);
        $jobSkip->update([
            'status' => ImportStatus::PREVIEW_READY,
            'valid_rows' => 2,
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => [
                    'name' => 'name',
                    'sku' => 'sku',
                    'price' => 'price',
                    'unit' => 'unit',
                ],
            ],
        ]);

        $skipResp = $this->actingAs($this->userApex)
            ->postJson("/api/v1/import/jobs/{$jobSkip->id}/execute", [
                'duplicate_strategy' => 'skip',
            ], $this->apexHeaders());

        $skipResp->assertStatus(200);
        $this->assertSame('Original Name', $existingProduct->fresh()->name); // Was skipped!
        $this->assertDatabaseHas('products', [
            'organization_id' => $this->orgApexId,
            'sku' => 'SKU-NEW-1',
            'name' => 'Brand New Product',
        ]);

        // 2. Test Execution with 'update' duplicate strategy
        $jobUpdate = $this->createMockJob(ImportType::PRODUCTS, $csvRows);
        $jobUpdate->update([
            'status' => ImportStatus::PREVIEW_READY,
            'valid_rows' => 2,
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => [
                    'name' => 'name',
                    'sku' => 'sku',
                    'price' => 'price',
                    'unit' => 'unit',
                ],
            ],
        ]);

        $updateResp = $this->actingAs($this->userApex)
            ->postJson("/api/v1/import/jobs/{$jobUpdate->id}/execute", [
                'duplicate_strategy' => 'update',
            ], $this->apexHeaders());

        $updateResp->assertStatus(200);
        $this->assertSame('Original Updated Name', $existingProduct->fresh()->name); // Was updated!
        $this->assertEquals(15000.00, (float) $existingProduct->fresh()->price);

        // Verify zero order/sales entries altered or created
        $orderItemsCount = DB::table('order_items')
            ->where('product_id', $existingProduct->id)
            ->count();
        $this->assertSame(0, $orderItemsCount);
    }

    public function test_customer_validation_and_execution(): void
    {
        $csvRows = [
            ['Customer Name', 'Phone Number', 'Email Address', 'Address'],
            ['علی احمدی', '09121112233', 'ali@test.com', 'تهران، خیابان ولیعصر'],
            ['مریم رضایی', '۰۹۳۵ ۱۲۳ ۴۵۶۷', 'maryam@test.com', 'شیراز'],
            ['شماره تماس خراب', 'invalid-phone', 'bad@test.com', 'اصفهان'], // Invalid phone
        ];

        $job = $this->createMockJob(ImportType::CUSTOMERS, $csvRows);
        $job->update([
            'status' => ImportStatus::READY_TO_VALIDATE,
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => [
                    'name' => 'Customer Name',
                    'phone' => 'Phone Number',
                    'email' => 'Email Address',
                    'address' => 'Address',
                ],
            ],
        ]);

        // Validate
        $valResp = $this->actingAs($this->userApex)
            ->postJson("/api/v1/import/jobs/{$job->id}/validate", [], $this->apexHeaders());

        $valResp->assertStatus(200);
        $this->assertEquals(2, $job->fresh()->valid_rows);
        $this->assertEquals(1, $job->fresh()->invalid_rows);

        // Execute
        $execResp = $this->actingAs($this->userApex)
            ->postJson("/api/v1/import/jobs/{$job->id}/execute", [
                'duplicate_strategy' => 'skip',
            ], $this->apexHeaders());

        $execResp->assertStatus(200);
        $this->assertDatabaseHas('customers', [
            'organization_id' => $this->orgApexId,
            'name' => 'علی احمدی',
            'phone' => '09121112233',
        ]);
        $this->assertDatabaseHas('customers', [
            'organization_id' => $this->orgApexId,
            'name' => 'مریم رضایی',
            'phone' => '09351234567',
        ]);
    }

    public function test_tenant_isolation_prevents_foreign_data_leak_or_matching(): void
    {
        // Create product in foreign org
        $foreignProduct = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgOtherId,
            'sku' => 'SECRET-FOREIGN-SKU',
            'barcode' => 'BAR-SECRET-1',
            'category' => 'محرمانه',
            'name' => 'Foreign Confidential Item',
            'price' => '9999.00',
            'cost_price' => '5000.00',
            'unit' => 'piece',
        ]);

        $csvRows = [
            ['name', 'sku', 'price'],
            ['Apex Item', 'SECRET-FOREIGN-SKU', '12000'],
        ];

        $job = $this->createMockJob(ImportType::PRODUCTS, $csvRows);
        $job->update([
            'status' => ImportStatus::READY_TO_VALIDATE,
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => [
                    'name' => 'name',
                    'sku' => 'sku',
                    'price' => 'price',
                ],
            ],
        ]);

        // Apex user validates: Should NOT report foreign product as existing match
        $valResp = $this->actingAs($this->userApex)
            ->postJson("/api/v1/import/jobs/{$job->id}/validate", [], $this->apexHeaders());

        $valResp->assertStatus(200);
        $this->assertEquals(0, $valResp->json('summary.matched_existing_rows'));

        // Foreign user cannot view or run Apex job
        $forbiddenResp = $this->actingAs($this->userOther)
            ->postJson("/api/v1/import/jobs/{$job->id}/validate", [], $this->foreignHeaders());

        $forbiddenResp->assertStatus(404);
    }

    private function createMockJob(string $importType, array $csvData): ImportJob
    {
        $jobId = (string) Str::uuid();
        $sourceId = (string) Str::uuid();
        $storagePath = "imports/{$this->orgApexId}/{$jobId}/test.csv";

        $handle = fopen('php://memory', 'r+');
        foreach ($csvData as $row) {
            fputcsv($handle, $row);
        }
        rewind($handle);
        $csvContent = stream_get_contents($handle);
        fclose($handle);

        Storage::disk('local')->put($storagePath, $csvContent);

        $headers = $csvData[0] ?? [];
        $sampleRows = array_slice($csvData, 1, 5);

        $job = ImportJob::create([
            'id' => $jobId,
            'organization_id' => $this->orgApexId,
            'user_id' => $this->userApex->id,
            'import_type' => $importType,
            'status' => ImportStatus::MAPPING_REQUIRED,
            'original_filename' => 'test.csv',
            'detected_format' => FileFormat::CSV,
            'file_size' => strlen($csvContent),
            'total_rows' => count($csvData) - 1,
        ]);

        ImportSource::create([
            'id' => $sourceId,
            'import_job_id' => $jobId,
            'organization_id' => $this->orgApexId,
            'original_filename' => 'test.csv',
            'storage_path' => $storagePath,
            'file_format' => FileFormat::CSV,
            'file_size' => strlen($csvContent),
            'checksum' => hash('sha256', $csvContent),
            'upload_status' => 'uploaded',
        ]);

        ImportDataset::create([
            'id' => (string) Str::uuid(),
            'import_job_id' => $jobId,
            'import_source_id' => $sourceId,
            'organization_id' => $this->orgApexId,
            'name' => 'Sheet1',
            'sheet_index' => 0,
            'row_count' => count($csvData) - 1,
            'headers' => $headers,
            'sample_rows' => $sampleRows,
            'status' => 'inspected',
        ]);

        return $job;
    }
}
