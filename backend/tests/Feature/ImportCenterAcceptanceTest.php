<?php

namespace Tests\Feature;

use App\Models\Account;
use App\Models\Customer;
use App\Models\Expense;
use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\ImportValidationResult;
use App\Models\JournalEntry;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Payment;
use App\Models\Product;
use App\Models\Purchase;
use App\Models\PurchaseItem;
use App\Models\StockMovement;
use App\Models\Store;
use App\Models\Supplier;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\Import\Execution\ImportExecutionService;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Normalizer\DataNormalizer;
use App\Services\Import\Reader\SpreadsheetReaderService;
use App\Services\Import\Template\ImportTemplateService;
use App\Services\Import\Validation\ImportValidationService;
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

/**
 * End-to-end acceptance testing and regression verification for Resto Import Center (P14.1 - P14.8).
 */
class ImportCenterAcceptanceTest extends TestCase
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
            'Accept' => 'application/json',
        ];
    }

    private function foreignHeaders(): array
    {
        return [
            'X-Tenant-ID' => $this->orgOtherId,
            'Accept' => 'application/json',
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
            'total_rows' => max(0, count($csvData) - 1),
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
            'row_count' => max(0, count($csvData) - 1),
        ]);

        return $job;
    }

    // =========================================================================
    // 1. TEMPLATE DOWNLOAD & BOM ACROSS ALL 7 IMPORT TYPES
    // =========================================================================

    public function test_acceptance_matrix_template_download_all_seven_types_utf8_bom(): void
    {
        $templateService = app(ImportTemplateService::class);

        foreach (ImportType::ALL as $type) {
            $response = $this->actingAs($this->userApex)
                ->get("/api/v1/import/templates/{$type}", $this->apexHeaders());

            $response->assertStatus(200);
            $response->assertHeader('Content-Type', 'text/csv; charset=UTF-8');
            $this->assertStringContainsString('attachment; filename=', (string) $response->headers->get('Content-Disposition'));

            $content = $response->streamedContent();
            $this->assertNotEmpty($content);

            // Assert UTF-8 BOM is present
            $this->assertStringStartsWith("\xEF\xBB\xBF", $content, "Template {$type} must start with UTF-8 BOM");

            // Assert headers in downloaded CSV match template definition
            $templateMeta = $templateService->getTemplate($type);
            $csvRows = str_getcsv(substr($content, 3), "\n");
            $headerLine = str_getcsv($csvRows[0]);

            $this->assertSame($templateMeta['headers'], $headerLine, "Headers mismatch for type {$type}");
        }

        // 404 on invalid template type
        $invalidResponse = $this->actingAs($this->userApex)
            ->get('/api/v1/import/templates/unknown_domain_type', $this->apexHeaders());
        $invalidResponse->assertStatus(404);
    }

    // =========================================================================
    // 2. DATA AND FILE SAFETY: FORMULA PROTECTION & VALUE NORMALIZATION
    // =========================================================================

    public function test_acceptance_matrix_data_and_file_safety_formula_and_normalization(): void
    {
        // 1. Formula Injection attempts: =CMD, @SUM, +123, -500 in CSV cells
        $csvWithFormulas = [
            ['name', 'sku', 'price'],
            ["=cmd|' /C calc'!A0", 'SKU-SAFE-01', '50000'],
            ['@SUM(100+200)', 'SKU-SAFE-02', '+120000'],
            ['-500 Negative', 'SKU-SAFE-03', '85000'],
        ];

        $job = $this->createMockJob(ImportType::PRODUCTS, $csvWithFormulas);
        $source = $job->sources()->first();
        $reader = app(SpreadsheetReaderService::class);
        $absolutePath = Storage::disk('local')->path($source->storage_path);

        $readRows = iterator_to_array($reader->streamRows($absolutePath, FileFormat::CSV));
        $this->assertCount(3, $readRows);

        // Verify formulas were treated as plain strings and not evaluated
        $this->assertSame("=cmd|' /C calc'!A0", $readRows[0]['assoc']['name']);
        $this->assertSame('@SUM(100+200)', $readRows[1]['assoc']['name']);

        // 2. Persian and Arabic digits normalization
        $this->assertSame('1234567890', DataNormalizer::toEnglishDigits('۱۲۳۴۵۶۷۸۹۰'));
        $this->assertSame('1234567890', DataNormalizer::toEnglishDigits('١٢٣٤٥٦٧٨٩٠'));

        // 3. Exact 2-decimal money normalization without float drift
        $this->assertSame('1250000.00', DataNormalizer::parseMoney('۱,۲۵۰,۰۰۰ تومان'));
        $this->assertSame('9999.50', DataNormalizer::parseMoney('۹۹۹۹٫۵۰'));
        $this->assertSame('100.00', DataNormalizer::parseMoney('100'));
        $this->assertNull(DataNormalizer::parseMoney('-15000')); // Negative rejected

        // 4. Persian typography & character cleanup
        $this->assertSame('کالای نمونه', DataNormalizer::cleanString('كالاي نمونه'));
        $this->assertSame('تست فارسی', DataNormalizer::cleanString("تست\u{200C}فارسی"));
    }

    // =========================================================================
    // 3. PRODUCTS: E2E SKIP & UPDATE WITH PERSISTED-STATE INVARIANTS
    // =========================================================================

    public function test_acceptance_matrix_products_e2e_skip_and_update_with_persisted_state(): void
    {
        // Pre-create existing product
        $existingProduct = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'name' => 'Original Product Name',
            'sku' => 'PROD-ACCEPT-01',
            'barcode' => 'BAR-001',
            'price' => 100000.00,
            'cost_price' => 70000.00,
            'unit' => 'piece',
            'category' => 'عمومی',
        ]);

        $initialStockCount = WarehouseStock::count();
        $initialMovementsCount = StockMovement::count();
        $initialJournalsCount = JournalEntry::count();

        $csvData = [
            ['name', 'sku', 'price', 'cost_price', 'category', 'unit'],
            ['Updated Product Name', 'PROD-ACCEPT-01', '150000', '90000', 'قهوه', 'piece'],
            ['Brand New Croissant', 'PROD-ACCEPT-02', '45000', '25000', 'شیرینی', 'piece'],
        ];

        $job = $this->createMockJob(ImportType::PRODUCTS, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'name' => 'name',
                    'sku' => 'sku',
                    'price' => 'price',
                    'cost_price' => 'cost_price',
                    'category' => 'category',
                    'unit' => 'unit',
                ],
            ],
        ]);

        $executionService = app(ImportExecutionService::class);

        // A. Test SKIP strategy: existing record is preserved untouched
        $skipResult = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);
        $this->assertSame(1, $skipResult['imported_rows']);
        $this->assertSame(1, $skipResult['skipped_rows']);

        $existingProduct->refresh();
        $this->assertSame('Original Product Name', $existingProduct->name);
        $this->assertSame(100000.0, (float) $existingProduct->price);

        // Assert new product created
        $newProduct = Product::where('organization_id', $this->orgApexId)->where('sku', 'PROD-ACCEPT-02')->first();
        $this->assertNotNull($newProduct);
        $this->assertSame('Brand New Croissant', $newProduct->name);

        // Invariant check: zero stock / accounting side effects
        $this->assertSame($initialStockCount, WarehouseStock::count());
        $this->assertSame($initialMovementsCount, StockMovement::count());
        $this->assertSame($initialJournalsCount, JournalEntry::count());

        // B. Test UPDATE strategy on new job
        $job2 = $this->createMockJob(ImportType::PRODUCTS, $csvData);
        $job2->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => $job->mapping_config,
        ]);

        $updateResult = $executionService->executeJob($job2, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);
        $this->assertSame(2, $updateResult['imported_rows']);

        $existingProduct->refresh();
        $this->assertSame('Updated Product Name', $existingProduct->name);
        $this->assertSame(150000.0, (float) $existingProduct->price);
    }

    // =========================================================================
    // 4. CUSTOMERS: E2E PHONE NORMALIZATION & ZERO SIDE EFFECTS
    // =========================================================================

    public function test_acceptance_matrix_customers_e2e_phone_normalization_and_zero_side_effects(): void
    {
        $csvData = [
            ['name', 'phone', 'address'],
            ['علیرضا نادری', '۰۹۱۲-۹۸۷-۶۵۴۳', 'تهران، میدان ونک'],
            ['سارا کریمی', '+989351234567', 'اصفهان، خیابان چهارباغ'],
        ];

        $job = $this->createMockJob(ImportType::CUSTOMERS, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'name' => 'name',
                    'phone' => 'phone',
                    'address' => 'address',
                ],
            ],
        ]);

        $initialInvoices = Order::count();
        $initialJournals = JournalEntry::count();

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(2, $result['imported_rows']);

        $c1 = Customer::where('organization_id', $this->orgApexId)->where('name', 'علیرضا نادری')->first();
        $this->assertNotNull($c1);
        $this->assertSame('09129876543', $c1->phone); // Normalized
        $this->assertSame('تهران، میدان ونک', $c1->address);

        $c2 = Customer::where('organization_id', $this->orgApexId)->where('name', 'سارا کریمی')->first();
        $this->assertNotNull($c2);
        $this->assertSame('09351234567', $c2->phone);
        $this->assertSame('اصفهان، خیابان چهارباغ', $c2->address);

        // Invariant check: customer import creates zero transactions or orders
        $this->assertSame($initialInvoices, Order::count());
        $this->assertSame($initialJournals, JournalEntry::count());
    }

    // =========================================================================
    // 5. OPENING INVENTORY: CONCURRENCY, LEDGER & IDEMPOTENCY
    // =========================================================================

    public function test_acceptance_matrix_opening_inventory_concurrency_and_adjustment_ledger_invariants(): void
    {
        $productExisting = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'name' => 'Inventory Product 1',
            'sku' => 'INV-ACC-01',
            'barcode' => 'BAR-INV-ACC-01',
            'price' => 50000.00,
            'cost_price' => 35000.00,
            'category' => 'عمومی',
            'unit' => 'piece',
        ]);

        $productNewStock = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'name' => 'Inventory Product 2',
            'sku' => 'INV-ACC-02',
            'barcode' => 'BAR-INV-ACC-02',
            'price' => 75000.00,
            'cost_price' => 50000.00,
            'category' => 'عمومی',
            'unit' => 'piece',
        ]);

        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();

        // Seed initial balance for product 1 only
        WarehouseStock::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'warehouse_id' => $wh->id,
            'product_id' => $productExisting->id,
            'quantity' => 20,
            'reserved_quantity' => 0,
        ]);

        $csvData = [
            ['sku', 'quantity'],
            ['INV-ACC-01', '100'], // counted balance: 100 (delta: +80)
            ['INV-ACC-02', '50'],  // new row: 50 (delta: +50)
        ];

        $job = $this->createMockJob(ImportType::INVENTORY, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                ],
                'warehouse_id' => $wh->id,
            ],
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);

        $this->assertSame(2, $result['imported_rows']);

        // Persisted stock invariants
        $stock1 = WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $wh->id)
            ->where('product_id', $productExisting->id)
            ->first();
        $this->assertSame(100, (int) $stock1->quantity);

        $stock2 = WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $wh->id)
            ->where('product_id', $productNewStock->id)
            ->first();
        $this->assertSame(50, (int) $stock2->quantity);

        // Persisted stock movements created
        $movements = StockMovement::where('organization_id', $this->orgApexId)
            ->where('reference_id', $job->id)
            ->get();
        $this->assertCount(2, $movements);
        $this->assertTrue($movements->every(fn ($m) => in_array($m->type, ['IN', 'OUT'], true)));
        $this->assertStringContainsString('موجودی اولیه', $movements->first()->reason);

        // Idempotency: Re-executing a completed job MUST be rejected and cannot double-adjust
        $job->refresh();
        $this->assertSame(ImportStatus::COMPLETED, $job->status);

        $this->expectException(\InvalidArgumentException::class);
        $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);

        // Stock remains unchanged at 100 and 50
        $this->assertSame(100, (int) $stock1->fresh()->quantity);
        $this->assertSame(50, (int) $stock2->fresh()->quantity);
    }

    // =========================================================================
    // 6. OPERATING EXPENSES: CANONICAL DOUBLE-ENTRY & DECIMAL PRECISION
    // =========================================================================

    public function test_acceptance_matrix_operating_expenses_canonical_double_entry_and_decimal_precision(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        $csvData = [
            ['title', 'amount', 'category', 'date', 'notes'],
            ['قبض برق کارگاه', '۲,۷۵۰,۰۰۰.۰۰', 'برق', '1403/05/10', 'برق ماه مرداد'],
        ];

        $job = $this->createMockJob(ImportType::EXPENSES, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'title' => 'title',
                    'amount' => 'amount',
                    'category' => 'category',
                    'date' => 'date',
                    'notes' => 'notes',
                ],
                'store_id' => $mainStore->id,
                'payment_method' => 'cash',
            ],
        ]);

        $initialJournalsCount = JournalEntry::count();

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(1, $result['imported_rows']);

        // Persisted expense assertions
        $expense = Expense::where('organization_id', $this->orgApexId)->where('title', 'قبض برق کارگاه')->first();
        $this->assertNotNull($expense);
        $this->assertSame(2750000.0, (float) $expense->amount);
        $this->assertSame('ELECTRICITY', $expense->category);
        $this->assertSame('CASH', $expense->payment_method);

        // Double-entry accounting entry assertion
        $this->assertSame($initialJournalsCount + 1, JournalEntry::count());
        $journal = JournalEntry::where('organization_id', $this->orgApexId)
            ->where('reference_type', 'Expense')
            ->where('reference_id', $expense->id)
            ->with('lines')
            ->first();

        $this->assertNotNull($journal);
        $this->assertCount(2, $journal->lines);
        $this->assertEquals(2750000.0, (float) $journal->total_debit);

        $debitLine = $journal->lines->firstWhere('type', 'DEBIT');
        $creditLine = $journal->lines->firstWhere('type', 'CREDIT');

        $this->assertNotNull($debitLine);
        $this->assertNotNull($creditLine);
        $this->assertEquals(2750000.0, (float) $debitLine->amount);
        $this->assertEquals(2750000.0, (float) $creditLine->amount);
    }

    // =========================================================================
    // 7. HISTORICAL ORDERS: STRICT HISTORICAL_RECORD_ONLY CONTRACT
    // =========================================================================

    public function test_acceptance_matrix_historical_orders_strict_historical_record_only_contract(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();

        $product = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'name' => 'Historical Pizza',
            'sku' => 'HIST-SKU-PIZZA',
            'barcode' => 'BAR-HIST-PIZZA',
            'price' => 120000.00,
            'cost_price' => 80000.00,
            'category' => 'عمومی',
            'unit' => 'piece',
        ]);

        // Pre-create stock of 50 units
        $initialStock = WarehouseStock::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'warehouse_id' => $wh->id,
            'product_id' => $product->id,
            'quantity' => 50,
            'reserved_quantity' => 0,
        ]);

        $initialMovementsCount = StockMovement::count();
        $initialJournalsCount = JournalEntry::count();
        $initialPaymentsCount = Payment::count();

        // Multiline order
        $csvData = [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'payment_method'],
            ['ORD-HIST-ACCEPT-01', 'HIST-SKU-PIZZA', '2', '120000', '1402/08/20', 'CASH'],
        ];

        $job = $this->createMockJob(ImportType::ORDERS, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                    'payment_method' => 'payment_method',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
            ],
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(1, $result['imported_rows']);

        // Persisted Order assertions
        $order = Order::where('organization_id', $this->orgApexId)
            ->where('external_order_id', 'ORD-HIST-ACCEPT-01')
            ->first();
        $this->assertNotNull($order);
        $this->assertTrue((bool) $order->is_historical);
        $this->assertSame('IMPORT_HISTORICAL', $order->source);
        $this->assertSame(240000.0, (float) $order->total_amount);

        // CRITICAL INVARIANT: Zero change to live warehouse stock!
        $this->assertSame(50, (int) $initialStock->fresh()->quantity);

        // CRITICAL INVARIANT: Zero stock movements!
        $this->assertSame($initialMovementsCount, StockMovement::count());

        // CRITICAL INVARIANT: Zero live journal entries!
        $this->assertSame($initialJournalsCount, JournalEntry::count());

        // CRITICAL INVARIANT: Zero live payments!
        $this->assertSame($initialPaymentsCount, Payment::count());

        // CRITICAL INVARIANT: Excluded from operational queries
        $operationalOrders = Order::where('organization_id', $this->orgApexId)
            ->where('is_historical', false)
            ->where('external_order_id', 'ORD-HIST-ACCEPT-01')
            ->count();
        $this->assertSame(0, $operationalOrders);
    }

    // =========================================================================
    // 8. SUPPLIERS: TENANT-SCOPED & ZERO FINANCIAL EFFECTS
    // =========================================================================

    public function test_acceptance_matrix_suppliers_e2e_tenant_scoped_and_zero_financial_effects(): void
    {
        $csvData = [
            ['name', 'code', 'phone', 'address', 'tax_id'],
            ['بازرگانی قهوه پایتخت', 'SUP-PAY-01', '02188776655', 'تهران، خیابان آزادی', '411223344'],
        ];

        $job = $this->createMockJob(ImportType::SUPPLIERS, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'name' => 'name',
                    'code' => 'code',
                    'phone' => 'phone',
                    'address' => 'address',
                    'tax_id' => 'tax_id',
                ],
            ],
        ]);

        $initialPurchasesCount = Purchase::count();
        $initialJournalsCount = JournalEntry::count();

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(1, $result['imported_rows']);

        $supplier = Supplier::where('organization_id', $this->orgApexId)
            ->where('code', 'SUP-PAY-01')
            ->first();
        $this->assertNotNull($supplier);
        $this->assertSame('بازرگانی قهوه پایتخت', $supplier->name);

        // Invariants: Supplier import does not create purchases or accounting side effects
        $this->assertSame($initialPurchasesCount, Purchase::count());
        $this->assertSame($initialJournalsCount, JournalEntry::count());
    }

    // =========================================================================
    // 9. HISTORICAL PURCHASES: STRICT HISTORICAL_RECORD_ONLY CONTRACT
    // =========================================================================

    public function test_acceptance_matrix_historical_purchases_strict_historical_record_only_contract(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $supplier = Supplier::where('organization_id', $this->orgApexId)->first();

        $product = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'name' => 'Tea Supply',
            'sku' => 'PUR-TEA-SKU-01',
            'barcode' => 'BAR-PUR-TEA-01',
            'price' => 80000.00,
            'cost_price' => 40000.00,
            'category' => 'عمومی',
            'unit' => 'piece',
        ]);

        $stock = WarehouseStock::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'warehouse_id' => $wh->id,
            'product_id' => $product->id,
            'quantity' => 15,
            'reserved_quantity' => 0,
        ]);

        $initialMovementsCount = StockMovement::count();
        $initialJournalsCount = JournalEntry::count();

        $csvData = [
            ['purchase_number', 'sku', 'quantity', 'unit_cost', 'date'],
            ['PUR-HIST-ACC-01', 'PUR-TEA-SKU-01', '10', '40000', '1402/10/15'],
        ];

        $job = $this->createMockJob(ImportType::PURCHASES, $csvData);
        $job->update([
            'status' => ImportStatus::READY_TO_IMPORT,
            'mapping_config' => [
                'fields' => [
                    'purchase_id' => 'purchase_number',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_cost' => 'unit_cost',
                    'date' => 'date',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
                'supplier_id' => $supplier->id,
            ],
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(1, $result['imported_rows']);

        $purchase = Purchase::where('organization_id', $this->orgApexId)
            ->where('external_purchase_id', 'PUR-HIST-ACC-01')
            ->first();
        $this->assertNotNull($purchase);
        $this->assertTrue((bool) $purchase->is_historical);
        $this->assertSame(400000.0, (float) $purchase->total_amount);

        // CRITICAL INVARIANT: Zero change to warehouse stock!
        $this->assertSame(15, (int) $stock->fresh()->quantity);

        // CRITICAL INVARIANT: Zero stock movements!
        $this->assertSame($initialMovementsCount, StockMovement::count());

        // CRITICAL INVARIANT: Zero journal entries!
        $this->assertSame($initialJournalsCount, JournalEntry::count());
    }

    // =========================================================================
    // 10. VALIDATION PREVIEW & ACTIONABLE ERROR REPORTING
    // =========================================================================

    public function test_acceptance_matrix_validation_preview_and_actionable_error_reporting(): void
    {
        $csvData = [
            ['name', 'sku', 'price'],
            ['Valid Product', 'SKU-VAL-01', '50000'],
            ['', 'SKU-VAL-02', '30000'], // Missing required name
            ['Invalid Price Product', 'SKU-VAL-03', '-500'], // Negative price
        ];

        $job = $this->createMockJob(ImportType::PRODUCTS, $csvData);

        // Save mapping
        $mappingRes = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/mapping", [
                'fields' => [
                    'name' => 'name',
                    'sku' => 'sku',
                    'price' => 'price',
                ],
            ]);
        $mappingRes->assertOk();

        // Run validation
        $validateRes = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/validate");
        $validateRes->assertOk();

        $job->refresh();
        $this->assertSame(ImportStatus::PREVIEW_READY, $job->status);
        $this->assertSame(1, $job->valid_rows);
        $this->assertSame(2, $job->invalid_rows);

        // Fetch paginated errors
        $errorsRes = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->getJson("/api/v1/import/jobs/{$job->id}/errors");
        $errorsRes->assertOk();

        $errorList = $errorsRes->json('data');
        $this->assertCount(2, $errorList);

        $errorCodes = array_column($errorList, 'error_code');
        $this->assertContains(ImportErrorCode::MISSING_REQUIRED_FIELD, $errorCodes);
        $this->assertContains(ImportErrorCode::NUMERIC_INVALID, $errorCodes);

        // Verify actionable suggestion
        $this->assertNotEmpty($errorList[0]['suggested_action']);
    }

    // =========================================================================
    // 11. CONCURRENCY LOCKING & IDEMPOTENT EXECUTION PROTECTION
    // =========================================================================

    public function test_acceptance_matrix_concurrency_locking_and_idempotent_execution_protection(): void
    {
        $csvData = [
            ['name', 'sku', 'price'],
            ['Coffee Cup', 'SKU-LOCK-01', '12000'],
        ];

        $job = $this->createMockJob(ImportType::PRODUCTS, $csvData);
        $job->update([
            'status' => ImportStatus::COMPLETED,
            'mapping_config' => ['fields' => ['name' => 'name', 'sku' => 'sku', 'price' => 'price']],
        ]);

        // Executing a completed job via API must return 422
        $response = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/execute", [
                'duplicate_strategy' => 'skip',
            ]);

        $response->assertStatus(422);
        $this->assertStringContainsString('امکان اجرا ندارد', $response->json('message'));
    }

    // =========================================================================
    // 12. MULTI-TENANT ISOLATION BOUNDARY ENFORCEMENT
    // =========================================================================

    public function test_acceptance_matrix_cross_tenant_isolation_boundary_enforcement(): void
    {
        $csvData = [
            ['name', 'sku', 'price'],
            ['Private Apex Item', 'SKU-APEX-PRIV-01', '250000'],
        ];

        $apexJob = $this->createMockJob(ImportType::PRODUCTS, $csvData, $this->orgApexId);

        // Foreign user cannot view Apex import job
        $getRes = $this->actingAs($this->userOther)
            ->withHeaders($this->foreignHeaders())
            ->getJson("/api/v1/import/jobs/{$apexJob->id}");
        $getRes->assertStatus(404);

        // Foreign user cannot fetch mapping
        $mappingRes = $this->actingAs($this->userOther)
            ->withHeaders($this->foreignHeaders())
            ->getJson("/api/v1/import/jobs/{$apexJob->id}/mapping");
        $mappingRes->assertStatus(404);

        // Foreign user cannot save mapping
        $saveRes = $this->actingAs($this->userOther)
            ->withHeaders($this->foreignHeaders())
            ->postJson("/api/v1/import/jobs/{$apexJob->id}/mapping", [
                'fields' => ['name' => 'name', 'sku' => 'sku', 'price' => 'price'],
            ]);
        $saveRes->assertStatus(404);

        // Foreign user cannot execute Apex job
        $execRes = $this->actingAs($this->userOther)
            ->withHeaders($this->foreignHeaders())
            ->postJson("/api/v1/import/jobs/{$apexJob->id}/execute", [
                'duplicate_strategy' => 'skip',
            ]);
        $execRes->assertStatus(404);

        // Foreign user cannot download Apex source file
        $downRes = $this->actingAs($this->userOther)
            ->withHeaders($this->foreignHeaders())
            ->get("/api/v1/import/jobs/{$apexJob->id}/download");
        $downRes->assertStatus(404);

        // Create foreign store first so foreign warehouse has valid store_id
        $foreignStore = Store::create([
            'id' => 'store_foreign_private',
            'organization_id' => $this->orgOtherId,
            'name' => 'Foreign Private Store',
            'code' => 'STR-FOR-01',
        ]);

        // Apex user cannot reference foreign warehouse in mapping payload
        $foreignWarehouse = Warehouse::create([
            'id' => 'wh_foreign_private',
            'organization_id' => $this->orgOtherId,
            'store_id' => $foreignStore->id,
            'name' => 'Foreign Private Warehouse',
            'code' => 'WH-FOR-01',
        ]);

        $invJob = $this->createMockJob(ImportType::INVENTORY, [['sku', 'quantity'], ['SKU-1', '10']], $this->orgApexId);
        $badMappingRes = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$invJob->id}/mapping", [
                'fields' => ['sku' => 'sku', 'quantity' => 'quantity'],
                'warehouse_id' => $foreignWarehouse->id,
            ]);
        $badMappingRes->assertStatus(422);
    }
}
