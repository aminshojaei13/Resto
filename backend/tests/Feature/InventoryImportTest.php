<?php

namespace Tests\Feature;

use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\ImportValidationResult;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Product;
use App\Models\StockMovement;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\Import\Execution\ImportExecutionService;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Validation\ImportValidationService;
use App\Support\Import\FileFormat;
use App\Support\Import\ImportErrorCode;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Tests\TestCase;

class InventoryImportTest extends TestCase
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

    private function createMockJob(string $importType, array $csvData, string $orgId = 'org_apex'): ImportJob
    {
        $jobId = (string) Str::uuid();
        $sourceId = (string) Str::uuid();
        $storagePath = "imports/{$orgId}/{$jobId}/test.csv";

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
            'organization_id' => $orgId,
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
            'organization_id' => $orgId,
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
            'organization_id' => $orgId,
            'name' => 'Sheet1',
            'sheet_index' => 0,
            'headers' => $headers,
            'sample_rows' => $sampleRows,
            'row_count' => count($csvData) - 1,
        ]);

        return $job;
    }

    public function test_mapping_service_suggests_and_validates_inventory_fields(): void
    {
        $mappingService = app(ImportMappingService::class);
        $headers = ['کد کالا', 'تعداد', 'انبار', 'قیمت خرید', 'توضیحات'];

        $suggestions = $mappingService->suggestMappings($headers, ImportType::INVENTORY);

        $this->assertSame('sku', $suggestions[0]['target_field']);
        $this->assertSame('quantity', $suggestions[1]['target_field']);
        $this->assertSame('warehouse', $suggestions[2]['target_field']);
        $this->assertSame('unit_cost', $suggestions[3]['target_field']);
        $this->assertSame('notes', $suggestions[4]['target_field']);

        // Required field validation: missing quantity
        $val1 = $mappingService->validateMapping([
            'sku' => 'کد کالا',
        ], ImportType::INVENTORY);
        $this->assertFalse($val1['valid']);
        $this->assertStringContainsString('موجودی اولیه', $val1['errors'][0]);

        // Valid mapping
        $val2 = $mappingService->validateMapping([
            'sku' => 'کد کالا',
            'quantity' => 'تعداد',
        ], ImportType::INVENTORY);
        $this->assertTrue($val2['valid']);
    }

    public function test_api_mapping_get_and_save_with_warehouse_support(): void
    {
        $job = $this->createMockJob(ImportType::INVENTORY, [
            ['کد کالا', 'موجودی اولیه'],
            ['SKU-001', '50'],
        ]);

        // GET mapping
        $res = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->getJson("/api/v1/import/jobs/{$job->id}/mapping");

        $res->assertOk();
        $res->assertJsonStructure(['target_fields', 'warehouses', 'suggested_mappings']);
        $this->assertNotEmpty($res->json('warehouses'));

        $mainWh = Warehouse::where('organization_id', $this->orgApexId)->first();

        // SAVE mapping with default warehouse_id
        $saveRes = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/mapping", [
                'fields' => [
                    'sku' => 'کد کالا',
                    'quantity' => 'موجودی اولیه',
                ],
                'warehouse_id' => $mainWh->id,
            ]);

        $saveRes->assertOk();
        $job->refresh();
        $this->assertSame(ImportStatus::READY_TO_VALIDATE, $job->status);
        $this->assertSame($mainWh->id, $job->mapping_config['warehouse_id']);
    }

    public function test_inventory_validation_pipeline_detects_errors_and_conflicts(): void
    {
        // Pre-create product in Apex
        $product = Product::firstOrCreate(
            ['id' => 'prod_test_inv_1', 'organization_id' => $this->orgApexId],
            [
                'sku' => 'INV-SKU-100',
                'barcode' => 'BAR-INV-100',
                'name' => 'Existing Coffee Beans',
                'category' => 'عمومی',
                'price' => '250000.00',
                'cost_price' => '180000.00',
                'unit' => 'piece',
            ]
        );

        $mainWh = Warehouse::where('organization_id', $this->orgApexId)->first();

        // Create pre-existing stock of 20 units
        WarehouseStock::updateOrCreate(
            [
                'organization_id' => $this->orgApexId,
                'warehouse_id' => $mainWh->id,
                'product_id' => $product->id,
                'product_variant_id' => null,
            ],
            [
                'id' => (string) Str::uuid(),
                'quantity' => 20,
                'reserved_quantity' => 0,
            ]
        );

        $product2 = Product::firstOrCreate(
            ['id' => 'prod_test_inv_2', 'organization_id' => $this->orgApexId],
            [
                'sku' => 'INV-SKU-200',
                'barcode' => 'BAR-INV-200',
                'name' => 'Tea Leaves',
                'category' => 'عمومی',
                'price' => '150000.00',
                'cost_price' => '100000.00',
                'unit' => 'piece',
            ]
        );

        $product3 = Product::firstOrCreate(
            ['id' => 'prod_test_inv_3', 'organization_id' => $this->orgApexId],
            [
                'sku' => 'INV-SKU-300',
                'barcode' => 'BAR-INV-300',
                'name' => 'Green Tea',
                'category' => 'عمومی',
                'price' => '120000.00',
                'cost_price' => '80000.00',
                'unit' => 'piece',
            ]
        );

        // Rows:
        // Row 1: Valid matched product with pre-existing stock (triggers warning)
        // Row 2: Unknown product SKU (triggers ERR_PRODUCT_NOT_FOUND)
        // Row 3: Invalid quantity (negative) (triggers ERR_INVALID_QUANTITY)
        // Row 4: Duplicate product in same file for same warehouse (triggers ERR_DUPLICATE_IN_FILE)
        // Row 5: Persian digits for quantity (valid)
        $csvData = [
            ['کد کالا', 'تعداد', 'انبار'],
            ['INV-SKU-100', '50', $mainWh->name],
            ['NON-EXISTENT-SKU', '10', $mainWh->name],
            ['INV-SKU-300', '-5', $mainWh->name],
            ['INV-SKU-100', '15', $mainWh->name],
            ['INV-SKU-200', '۳۰', $mainWh->name],
        ];

        $job = $this->createMockJob(ImportType::INVENTORY, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_VALIDATE,
            'mapping_config' => [
                'fields' => [
                    'sku' => 'کد کالا',
                    'quantity' => 'تعداد',
                    'warehouse' => 'انبار',
                ],
            ],
        ]);

        $validationService = app(ImportValidationService::class);
        $summary = $validationService->validateJob($job);

        $this->assertSame(5, $summary['total_rows']);
        $this->assertSame(2, $summary['valid_rows']); // Row 1 (warning only) and Row 5
        $this->assertSame(3, $summary['invalid_rows']); // Rows 2, 3, 4
        $this->assertSame(2, $summary['matched_existing_rows']); // Rows 1 and 4 matched existing stock

        // Check validation error codes in DB
        $errors = ImportValidationResult::where('import_job_id', $job->id)->get();
        $errorCodes = $errors->pluck('error_code')->all();

        $this->assertContains(ImportErrorCode::PRODUCT_NOT_FOUND, $errorCodes);
        $this->assertContains(ImportErrorCode::INVALID_QUANTITY, $errorCodes);
        $this->assertContains(ImportErrorCode::DUPLICATE_IN_FILE, $errorCodes);
        $this->assertContains(ImportErrorCode::EXISTING_BALANCE_CONFLICT, $errorCodes);
    }

    public function test_inventory_execution_skip_mode_preserves_existing_balances(): void
    {
        $product = Product::firstOrCreate(
            ['id' => 'prod_test_exec_1', 'organization_id' => $this->orgApexId],
            [
                'sku' => 'EXEC-SKU-1',
                'barcode' => 'BAR-EXEC-1',
                'name' => 'Product Exec 1',
                'category' => 'عمومی',
                'price' => '100.00',
                'cost_price' => '60.00',
                'unit' => 'piece',
            ]
        );

        $productNew = Product::firstOrCreate(
            ['id' => 'prod_test_exec_2', 'organization_id' => $this->orgApexId],
            [
                'sku' => 'EXEC-SKU-2',
                'barcode' => 'BAR-EXEC-2',
                'name' => 'Product Exec 2',
                'category' => 'عمومی',
                'price' => '200.00',
                'cost_price' => '120.00',
                'unit' => 'piece',
            ]
        );

        $mainWh = Warehouse::where('organization_id', $this->orgApexId)->first();

        // Seed stock for product 1 = 15 units
        WarehouseStock::updateOrCreate(
            [
                'organization_id' => $this->orgApexId,
                'warehouse_id' => $mainWh->id,
                'product_id' => $product->id,
                'product_variant_id' => null,
            ],
            [
                'id' => (string) Str::uuid(),
                'quantity' => 15,
                'reserved_quantity' => 0,
            ]
        );

        $csvData = [
            ['sku', 'quantity'],
            ['EXEC-SKU-1', '100'], // Has existing balance 15 -> should be SKIPPED
            ['EXEC-SKU-2', '40'],  // Has no existing balance -> should be IMPORTED
        ];

        $job = $this->createMockJob(ImportType::INVENTORY, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                ],
                'warehouse_id' => $mainWh->id,
            ],
        ]);

        $executionService = app(ImportExecutionService::class);
        $res = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(1, $res['imported_rows']);
        $this->assertSame(1, $res['skipped_rows']);
        $this->assertSame(0, $res['failed_rows']);

        // Stock 1 must remain 15!
        $stock1 = WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $mainWh->id)
            ->where('product_id', $product->id)
            ->first();
        $this->assertSame(15, (int) $stock1->quantity);

        // Stock 2 must be 40!
        $stock2 = WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $mainWh->id)
            ->where('product_id', $productNew->id)
            ->first();
        $this->assertSame(40, (int) $stock2->quantity);

        // Assert movement created for product 2 only
        $movement2 = StockMovement::where('organization_id', $this->orgApexId)
            ->where('product_id', $productNew->id)
            ->first();
        $this->assertNotNull($movement2);
        $this->assertSame(40, (int) $movement2->quantity);
        $this->assertSame($job->id, $movement2->reference_id);
    }

    public function test_inventory_execution_update_mode_adjusts_balances_atomically(): void
    {
        $product = Product::firstOrCreate(
            ['id' => 'prod_test_update_1', 'organization_id' => $this->orgApexId],
            [
                'sku' => 'UPDATE-SKU-1',
                'barcode' => 'BAR-UPDATE-1',
                'name' => 'Product Update 1',
                'category' => 'عمومی',
                'price' => '100.00',
                'cost_price' => '60.00',
                'unit' => 'piece',
            ]
        );

        $mainWh = Warehouse::where('organization_id', $this->orgApexId)->first();

        // Seed initial balance of 10
        WarehouseStock::updateOrCreate(
            [
                'organization_id' => $this->orgApexId,
                'warehouse_id' => $mainWh->id,
                'product_id' => $product->id,
                'product_variant_id' => null,
            ],
            [
                'id' => (string) Str::uuid(),
                'quantity' => 10,
                'reserved_quantity' => 0,
            ]
        );

        $csvData = [
            ['sku', 'quantity', 'unit_cost', 'notes'],
            ['UPDATE-SKU-1', '85', '65.00', 'انبارگردانی افتتاحیه'],
        ];

        $job = $this->createMockJob(ImportType::INVENTORY, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_cost' => 'unit_cost',
                    'notes' => 'notes',
                ],
                'warehouse_id' => $mainWh->id,
            ],
        ]);

        $executionService = app(ImportExecutionService::class);
        $res = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);

        $this->assertSame(1, $res['imported_rows']);
        $this->assertSame(0, $res['skipped_rows']);

        // Final balance should be exactly 85
        $stock = WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $mainWh->id)
            ->where('product_id', $product->id)
            ->first();
        $this->assertSame(85, (int) $stock->quantity);

        // Movement delta should be 85 - 10 = +75
        $movement = StockMovement::where('organization_id', $this->orgApexId)
            ->where('product_id', $product->id)
            ->where('reference_id', $job->id)
            ->first();
        $this->assertNotNull($movement);
        $this->assertSame(75, (int) $movement->quantity);
        $this->assertSame('IN', $movement->type);
        $this->assertEquals(65.00, (float) $movement->unit_cost);
        $this->assertStringContainsString('انبارگردانی افتتاحیه', $movement->reason);
    }

    public function test_tenant_isolation_prevents_matching_or_modifying_foreign_inventory(): void
    {
        // Product belongs to foreign org
        $foreignProduct = Product::create([
            'id' => 'prod_foreign_iso',
            'organization_id' => $this->orgOtherId,
            'sku' => 'FOREIGN-SKU',
            'barcode' => 'BAR-FOREIGN-ISO',
            'name' => 'Foreign Secret Product',
            'category' => 'عمومی',
            'price' => '500.00',
            'cost_price' => '300.00',
            'unit' => 'piece',
        ]);

        $mainWh = Warehouse::where('organization_id', $this->orgApexId)->first();

        // Apex user tries to import inventory pointing to foreign product
        $csvData = [
            ['sku', 'quantity'],
            ['FOREIGN-SKU', '50'],
        ];

        $job = $this->createMockJob(ImportType::INVENTORY, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_VALIDATE,
            'mapping_config' => [
                'fields' => [
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                ],
                'warehouse_id' => $mainWh->id,
            ],
        ]);

        $validationService = app(ImportValidationService::class);
        $summary = $validationService->validateJob($job);

        // Must fail with 0 valid rows because foreign product cannot be found
        $this->assertSame(0, $summary['valid_rows']);
        $this->assertSame(1, $summary['invalid_rows']);

        $err = ImportValidationResult::where('import_job_id', $job->id)->first();
        $this->assertSame(ImportErrorCode::PRODUCT_NOT_FOUND, $err->error_code);

        // Ensure foreign stock was untouched
        $foreignStock = WarehouseStock::where('product_id', $foreignProduct->id)->count();
        $this->assertSame(0, $foreignStock);
    }
}
