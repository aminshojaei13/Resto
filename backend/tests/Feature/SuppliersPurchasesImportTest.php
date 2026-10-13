<?php

namespace Tests\Feature;

use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\ImportValidationResult;
use App\Models\JournalEntry;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Product;
use App\Models\Purchase;
use App\Models\PurchaseItem;
use App\Models\StockMovement;
use App\Models\Store;
use App\Models\Supplier;
use App\Models\SupplierPayment;
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
use App\Support\Money;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Tests\TestCase;

class SuppliersPurchasesImportTest extends TestCase
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
            'processed_rows' => 0,
            'valid_rows' => 0,
            'invalid_rows' => 0,
            'imported_rows' => 0,
            'skipped_rows' => 0,
            'failed_rows' => 0,
            'metadata' => [
                'original_filename' => 'test.csv',
            ],
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
            'headers' => $csvData[0] ?? [],
            'sample_rows' => array_slice($csvData, 1, 5),
            'row_count' => count($csvData) - 1,
        ]);

        return $job;
    }

    // =========================================================================
    // 1. MAPPING ENGINE TESTS
    // =========================================================================

    public function test_mapping_service_provides_target_fields_for_suppliers(): void
    {
        /** @var ImportMappingService $mappingService */
        $mappingService = app(ImportMappingService::class);
        $fields = $mappingService->getTargetFields(ImportType::SUPPLIERS);

        $this->assertNotEmpty($fields);
        $fieldNames = array_column($fields, 'name');
        $this->assertContains('name', $fieldNames);
        $this->assertContains('code', $fieldNames);
        $this->assertContains('phone', $fieldNames);
        $this->assertContains('email', $fieldNames);
        $this->assertContains('address', $fieldNames);
        $this->assertContains('tax_id', $fieldNames);
        $this->assertContains('notes', $fieldNames);
    }

    public function test_mapping_service_provides_target_fields_for_purchases(): void
    {
        /** @var ImportMappingService $mappingService */
        $mappingService = app(ImportMappingService::class);
        $fields = $mappingService->getTargetFields(ImportType::PURCHASES);

        $this->assertNotEmpty($fields);
        $fieldNames = array_column($fields, 'name');
        $this->assertContains('purchase_id', $fieldNames);
        $this->assertContains('sku', $fieldNames);
        $this->assertContains('quantity', $fieldNames);
        $this->assertContains('unit_cost', $fieldNames);
        $this->assertContains('date', $fieldNames);
        $this->assertContains('total_amount', $fieldNames);
        $this->assertContains('supplier', $fieldNames);
        $this->assertContains('warehouse', $fieldNames);
        $this->assertContains('store', $fieldNames);
        $this->assertContains('status', $fieldNames);
    }

    public function test_mapping_endpoint_returns_suppliers_warehouses_and_stores_for_purchases(): void
    {
        $csv = [
            ['شماره فاکتور', 'کد کالا', 'تعداد', 'قیمت خرید'],
            ['PO-1001', 'SKU-COF-001', '5', '120000'],
        ];
        $job = $this->createMockJob(ImportType::PURCHASES, $csv);

        $response = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->getJson("/api/v1/import/jobs/{$job->id}/mapping");

        $response->assertStatus(200)
            ->assertJsonStructure([
                'job_id',
                'import_type',
                'target_fields',
                'warehouses',
                'stores',
                'suppliers',
            ]);

        $this->assertNotEmpty($response->json('warehouses'));
        $this->assertNotEmpty($response->json('stores'));
        $this->assertNotEmpty($response->json('suppliers'));
    }

    // =========================================================================
    // 2. SUPPLIERS IMPORT TESTS
    // =========================================================================

    public function test_suppliers_validation_and_execution_success(): void
    {
        $csv = [
            ['نام تأمین‌کننده', 'کد شناسایی', 'شماره تماس', 'ایمیل', 'شناسه ملی / کد اقتصادی', 'آدرس', 'یادداشت'],
            ['تأمین قهوه البرز', 'SUP-101', '09121112233', 'alborz@coffee.ir', '14001234567', 'تهران، خیابان ولیعصر', 'تأمین‌کننده اصلی دان قهوه'],
            ['بسته‌بندی پارس', 'SUP-102', '09124445566', 'pars@pack.ir', '14009876543', 'کرج، شهرک صنعتی', 'جعبه و لیوان کاغذی'],
        ];
        $job = $this->createMockJob(ImportType::SUPPLIERS, $csv);

        $mapping = [
            'name' => 'نام تأمین‌کننده',
            'code' => 'کد شناسایی',
            'phone' => 'شماره تماس',
            'email' => 'ایمیل',
            'tax_id' => 'شناسه ملی / کد اقتصادی',
            'address' => 'آدرس',
            'notes' => 'یادداشت',
        ];

        $job->update([
            'mapping_config' => ['sheet_index' => 0, 'fields' => $mapping],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        /** @var ImportValidationService $valService */
        $valService = app(ImportValidationService::class);
        $valSummary = $valService->validateJob($job);

        $this->assertEquals(2, $valSummary['valid_rows']);
        $this->assertEquals(0, $valSummary['invalid_rows']);

        /** @var ImportExecutionService $execService */
        $execService = app(ImportExecutionService::class);
        $execSummary = $execService->executeJob($job, $this->userApex);

        $this->assertEquals(2, $execSummary['imported_rows']);
        $this->assertEquals(0, $execSummary['failed_rows']);

        $sup1 = Supplier::where('organization_id', $this->orgApexId)->where('code', 'SUP-101')->first();
        $this->assertNotNull($sup1);
        $this->assertEquals('تامین قهوه البرز', $sup1->name);
        $this->assertEquals('09121112233', $sup1->phone);
        $this->assertEquals('alborz@coffee.ir', $sup1->email);
        $this->assertEquals('14001234567', $sup1->tax_id);
    }

    public function test_suppliers_do_not_merge_solely_on_similar_name(): void
    {
        // Existing supplier with code SUP-OLD and name "شرکت پخش آریا"
        Supplier::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'name' => 'شرکت پخش آریا',
            'code' => 'SUP-OLD',
            'phone' => '09120000001',
        ]);

        // Imported supplier with identical name but distinct code SUP-NEW and distinct phone
        $csv = [
            ['نام تأمین‌کننده', 'کد شناسایی', 'شماره تماس'],
            ['شرکت پخش آریا', 'SUP-NEW', '09129999999'],
        ];
        $job = $this->createMockJob(ImportType::SUPPLIERS, $csv);
        $mapping = [
            'name' => 'نام تأمین‌کننده',
            'code' => 'کد شناسایی',
            'phone' => 'شماره تماس',
        ];
        $job->update([
            'mapping_config' => ['sheet_index' => 0, 'fields' => $mapping],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        $valService = app(ImportValidationService::class);
        $valSummary = $valService->validateJob($job);
        $this->assertEquals(0, $valSummary['matched_existing_rows'], 'Must NOT match existing supplier solely by name!');

        $execService = app(ImportExecutionService::class);
        $execSummary = $execService->executeJob($job, $this->userApex);
        $this->assertEquals(1, $execSummary['imported_rows']);

        // Both suppliers must exist as separate records
        $supOld = Supplier::where('organization_id', $this->orgApexId)->where('code', 'SUP-OLD')->first();
        $supNew = Supplier::where('organization_id', $this->orgApexId)->where('code', 'SUP-NEW')->first();
        $this->assertNotNull($supOld);
        $this->assertNotNull($supNew);
        $this->assertNotEquals($supOld->id, $supNew->id);
    }

    public function test_suppliers_duplicate_skip_and_update_strategies(): void
    {
        $existing = Supplier::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'name' => 'تامین لبنیات پاک',
            'code' => 'SUP-DAIRY',
            'phone' => '09125556677',
            'address' => 'ادرس قدیمی',
        ]);

        $csv = [
            ['نام', 'کد', 'تلفن', 'آدرس'],
            ['تأمین لبنیات پاک تهران', 'SUP-DAIRY', '09125556677', 'آدرس جدید کارخانه'],
        ];

        // 1. Test SKIP
        $jobSkip = $this->createMockJob(ImportType::SUPPLIERS, $csv);
        $mapping = ['name' => 'نام', 'code' => 'کد', 'phone' => 'تلفن', 'address' => 'آدرس'];
        $jobSkip->update([
            'mapping_config' => ['sheet_index' => 0, 'fields' => $mapping],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        app(ImportValidationService::class)->validateJob($jobSkip);
        $execResultSkip = app(ImportExecutionService::class)->executeJob($jobSkip, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertEquals(1, $execResultSkip['skipped_rows']);
        $this->assertEquals(0, $execResultSkip['imported_rows']);
        $existing->refresh();
        $this->assertEquals('ادرس قدیمی', $existing->address);

        // 2. Test UPDATE
        $jobUpdate = $this->createMockJob(ImportType::SUPPLIERS, $csv);
        $jobUpdate->update([
            'mapping_config' => ['sheet_index' => 0, 'fields' => $mapping],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        app(ImportValidationService::class)->validateJob($jobUpdate);
        $execResultUpdate = app(ImportExecutionService::class)->executeJob($jobUpdate, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);

        $this->assertEquals(1, $execResultUpdate['imported_rows']);
        $existing->refresh();
        $this->assertEquals('ادرس جدید کارخانه', $existing->address);
        $this->assertEquals('تامین لبنیات پاک تهران', $existing->name);
    }

    // =========================================================================
    // 3. PURCHASES IMPORT & HISTORICAL_RECORD_ONLY DOMAIN TESTS
    // =========================================================================

    public function test_purchases_multiline_grouping_and_exact_math(): void
    {
        $product1 = Product::where('organization_id', $this->orgApexId)->first();
        $product2 = Product::where('organization_id', $this->orgApexId)->skip(1)->first();

        // 2 items for the same invoice PO-HIST-8888
        $csv = [
            ['شناسه خرید', 'کد کالا', 'تعداد', 'قیمت واحد', 'تاریخ', 'تخفیف', 'مالیات', 'مبلغ کل فاکتور'],
            ['PO-HIST-8888', $product1->sku, '10', '150000', '1402/08/15', '50000', '10000', '3560000'],
            ['PO-HIST-8888', $product2->sku, '20', '100000', '1402/08/15', '0', '0', '3560000'],
        ];
        // Line 1: (10 * 150000) - 50000 + 10000 = 1460000
        // Line 2: (20 * 100000) = 2000000
        // Total: 1460000 + 2000000 = 3460000. But CSV declared 3560000 -> Should detect total mismatch!

        $job = $this->createMockJob(ImportType::PURCHASES, $csv);
        $mapping = [
            'purchase_id' => 'شناسه خرید',
            'sku' => 'کد کالا',
            'quantity' => 'تعداد',
            'unit_cost' => 'قیمت واحد',
            'date' => 'تاریخ',
            'discount_amount' => 'تخفیف',
            'tax_amount' => 'مالیات',
            'total_amount' => 'مبلغ کل فاکتور',
        ];
        $job->update([
            'mapping_config' => ['sheet_index' => 0, 'fields' => $mapping],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        /** @var ImportValidationService $valService */
        $valService = app(ImportValidationService::class);
        $valSummary = $valService->validateJob($job);

        // Validation should register a total mismatch error
        $mismatchError = ImportValidationResult::where('import_job_id', $job->id)
            ->where('error_code', ImportErrorCode::PURCHASE_TOTAL_MISMATCH)
            ->first();
        $this->assertNotNull($mismatchError);
    }

    public function test_purchases_zero_side_effects_on_inventory_and_accounting(): void
    {
        $product = Product::where('organization_id', $this->orgApexId)->first();
        $warehouse = Warehouse::where('organization_id', $this->orgApexId)->first();
        $store = Store::where('organization_id', $this->orgApexId)->first();
        $supplier = Supplier::where('organization_id', $this->orgApexId)->first();

        // Check stock quantity before import
        $stockBefore = WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $warehouse->id)
            ->where('product_id', $product->id)
            ->value('quantity') ?? 0;

        $movementsCountBefore = StockMovement::where('organization_id', $this->orgApexId)->count();
        $journalEntriesCountBefore = JournalEntry::where('organization_id', $this->orgApexId)->count();
        $supplierPaymentsCountBefore = SupplierPayment::where('organization_id', $this->orgApexId)->count();

        $csv = [
            ['شناسه خرید', 'کد کالا', 'تعداد', 'قیمت واحد', 'تاریخ', 'مبلغ کل'],
            ['PO-ZERO-EFFECTS', $product->sku, '50', '250000', '1402/05/10', '12500000'],
        ];

        $job = $this->createMockJob(ImportType::PURCHASES, $csv);
        $mapping = [
            'purchase_id' => 'شناسه خرید',
            'sku' => 'کد کالا',
            'quantity' => 'تعداد',
            'unit_cost' => 'قیمت واحد',
            'date' => 'تاریخ',
            'total_amount' => 'مبلغ کل',
        ];
        $job->update([
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => $mapping,
                'warehouse_id' => $warehouse->id,
                'store_id' => $store->id,
                'supplier_id' => $supplier->id,
            ],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        $valService = app(ImportValidationService::class);
        $valSummary = $valService->validateJob($job);
        $this->assertEquals(1, $valSummary['valid_rows']);

        $execService = app(ImportExecutionService::class);
        $execSummary = $execService->executeJob($job, $this->userApex);
        $this->assertEquals(1, $execSummary['imported_rows']);

        // 1. Verify Purchase was created with is_historical = true and source = IMPORT_HISTORICAL
        $purchase = Purchase::where('organization_id', $this->orgApexId)
            ->where('external_purchase_id', 'PO-ZERO-EFFECTS')
            ->first();

        $this->assertNotNull($purchase);
        $this->assertTrue($purchase->is_historical);
        $this->assertEquals('IMPORT_HISTORICAL', $purchase->source);
        $this->assertEquals('12500000.00', $purchase->total_amount);

        // 2. Verify PurchaseItem was created
        $item = PurchaseItem::where('purchase_id', $purchase->id)->first();
        $this->assertNotNull($item);
        $this->assertEquals(50, $item->quantity);
        $this->assertEquals('250000.00', $item->unit_cost);

        // 3. ZERO side effects check: Stock quantity MUST NOT CHANGE
        $stockAfter = WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $warehouse->id)
            ->where('product_id', $product->id)
            ->value('quantity') ?? 0;
        $this->assertEquals($stockBefore, $stockAfter, 'WarehouseStock must remain 100% untouched by historical purchase import!');

        // 4. ZERO Stock Movements
        $movementsCountAfter = StockMovement::where('organization_id', $this->orgApexId)->count();
        $this->assertEquals($movementsCountBefore, $movementsCountAfter, 'StockMovement must not be created!');

        // 5. ZERO General Ledger Journal Entries
        $journalEntriesCountAfter = JournalEntry::where('organization_id', $this->orgApexId)->count();
        $this->assertEquals($journalEntriesCountBefore, $journalEntriesCountAfter, 'JournalEntry must not be created!');

        // 6. ZERO Supplier Payments
        $supplierPaymentsCountAfter = SupplierPayment::where('organization_id', $this->orgApexId)->count();
        $this->assertEquals($supplierPaymentsCountBefore, $supplierPaymentsCountAfter, 'SupplierPayment must not be created!');
    }

    public function test_purchases_workflow_guards_block_receive_and_pay(): void
    {
        $supplier = Supplier::where('organization_id', $this->orgApexId)->first();

        $purchase = Purchase::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1',
            'supplier_id' => $supplier->id,
            'purchase_number' => 'HIST-PO-GUARD-TEST',
            'external_purchase_id' => 'GUARD-TEST',
            'total_amount' => 500000,
            'status' => 'RECEIVED',
            'payment_status' => 'PAID',
            'source' => 'IMPORT_HISTORICAL',
            'is_historical' => true,
        ]);

        // 1. Receiving Queue must NOT include historical purchase
        $queueResponse = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->getJson('/api/v1/purchases/receiving-queue');

        $queueResponse->assertStatus(200);
        $queueIds = collect($queueResponse->json('data'))->pluck('id')->toArray();
        $this->assertNotContains($purchase->id, $queueIds);

        // 2. Receive endpoint must block historical purchase
        $receiveResponse = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/purchases/{$purchase->id}/receive", [
                'items' => [],
            ]);

        $receiveResponse->assertStatus(422);

        // 3. Pay endpoint must block historical purchase
        $payResponse = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/purchases/{$purchase->id}/pay", [
                'amount' => 100000,
                'payment_method' => 'CASH',
            ]);

        $payResponse->assertStatus(422);
    }

    public function test_purchases_idempotency_and_duplicate_skip_update(): void
    {
        $product = Product::where('organization_id', $this->orgApexId)->first();
        $warehouse = Warehouse::where('organization_id', $this->orgApexId)->first();
        $store = Store::where('organization_id', $this->orgApexId)->first();
        $supplier = Supplier::where('organization_id', $this->orgApexId)->first();

        $csv = [
            ['شناسه خرید', 'کد کالا', 'تعداد', 'قیمت واحد', 'تاریخ', 'یادداشت'],
            ['PO-IDEM-001', $product->sku, '10', '100000', '1402/06/01', 'یادداشت اولیه'],
        ];

        $job = $this->createMockJob(ImportType::PURCHASES, $csv);
        $mapping = [
            'purchase_id' => 'شناسه خرید',
            'sku' => 'کد کالا',
            'quantity' => 'تعداد',
            'unit_cost' => 'قیمت واحد',
            'date' => 'تاریخ',
            'notes' => 'یادداشت',
        ];
        $job->update([
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => $mapping,
                'warehouse_id' => $warehouse->id,
                'store_id' => $store->id,
                'supplier_id' => $supplier->id,
            ],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        app(ImportValidationService::class)->validateJob($job);
        $execResult1 = app(ImportExecutionService::class)->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);
        $this->assertEquals(1, $execResult1['imported_rows']);

        // Re-executing same job must be skipped due to deterministic UUID
        $job->update(['status' => ImportStatus::READY_TO_IMPORT]);
        $execResultRetry = app(ImportExecutionService::class)->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);
        $this->assertEquals(1, $execResultRetry['skipped_rows']);
        $this->assertEquals(0, $execResultRetry['imported_rows']);

        // Second job with same purchase_id: test UPDATE strategy
        $csvUpdate = [
            ['شناسه خرید', 'کد کالا', 'تعداد', 'قیمت واحد', 'تاریخ', 'یادداشت'],
            ['PO-IDEM-001', $product->sku, '10', '100000', '1402/06/01', 'یادداشت بروزرسانی شده'],
        ];
        $job2 = $this->createMockJob(ImportType::PURCHASES, $csvUpdate);
        $job2->update([
            'mapping_config' => [
                'sheet_index' => 0,
                'fields' => $mapping,
                'warehouse_id' => $warehouse->id,
                'store_id' => $store->id,
                'supplier_id' => $supplier->id,
            ],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        app(ImportValidationService::class)->validateJob($job2);
        $execResultUpdate = app(ImportExecutionService::class)->executeJob($job2, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);

        $this->assertEquals(1, $execResultUpdate['imported_rows']);

        $persisted = Purchase::where('organization_id', $this->orgApexId)
            ->where('external_purchase_id', 'PO-IDEM-001')
            ->first();
        $this->assertEquals('یادداشت بروزرسانی شده', $persisted->notes);
    }

    public function test_tenant_isolation_prevents_importing_cross_tenant_products(): void
    {
        // Product in foreign tenant
        $foreignProduct = Product::create([
            'id' => 'prod_foreign_99',
            'organization_id' => $this->orgOtherId,
            'name' => 'Foreign Product',
            'sku' => 'SKU-FOREIGN-99',
            'barcode' => 'BAR-FOREIGN-99',
            'category' => 'coffee',
            'price' => 50000,
            'cost_price' => 30000,
            'status' => 'active',
        ]);

        $csv = [
            ['شناسه خرید', 'کد کالا', 'تعداد', 'قیمت واحد', 'تاریخ'],
            ['PO-CROSS-01', 'SKU-FOREIGN-99', '5', '30000', '1402/01/01'],
        ];

        $job = $this->createMockJob(ImportType::PURCHASES, $csv, $this->orgApexId);
        $mapping = [
            'purchase_id' => 'شناسه خرید',
            'sku' => 'کد کالا',
            'quantity' => 'تعداد',
            'unit_cost' => 'قیمت واحد',
            'date' => 'تاریخ',
        ];
        $job->update([
            'mapping_config' => ['sheet_index' => 0, 'fields' => $mapping],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        $valService = app(ImportValidationService::class);
        $valSummary = $valService->validateJob($job);

        $this->assertEquals(0, $valSummary['valid_rows']);
        $this->assertEquals(1, $valSummary['invalid_rows']);

        $err = ImportValidationResult::where('import_job_id', $job->id)
            ->where('error_code', ImportErrorCode::PRODUCT_NOT_FOUND)
            ->first();
        $this->assertNotNull($err);
    }
}
