<?php

namespace Tests\Feature;

use App\Models\Account;
use App\Models\Customer;
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
use App\Models\StockMovement;
use App\Models\Store;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\AccountingService;
use App\Services\Import\Execution\ImportExecutionService;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Normalizer\DataNormalizer;
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

class HistoricalOrdersImportTest extends TestCase
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

    public function test_mapping_service_suggests_and_validates_order_fields(): void
    {
        $mappingService = app(ImportMappingService::class);
        $targetFields = $mappingService->getTargetFields(ImportType::ORDERS);

        $this->assertArrayHasKey('order_id', $targetFields);
        $this->assertArrayHasKey('sku', $targetFields);
        $this->assertArrayHasKey('quantity', $targetFields);
        $this->assertArrayHasKey('unit_price', $targetFields);
        $this->assertArrayHasKey('date', $targetFields);

        $headers = ['شماره فاکتور', 'کد کالا', 'تعداد', 'قیمت واحد', 'تاریخ سفارش', 'تخفیف', 'نام مشتری'];
        $suggestions = $mappingService->suggestMappings($headers, ImportType::ORDERS);

        $this->assertNotEmpty($suggestions);
        $matchedTargets = array_filter(array_column($suggestions, 'target_field'));
        $this->assertContains('order_id', $matchedTargets);
        $this->assertContains('sku', $matchedTargets);
        $this->assertContains('quantity', $matchedTargets);
        $this->assertContains('unit_price', $matchedTargets);
        $this->assertContains('date', $matchedTargets);
    }

    public function test_orders_validation_groups_multiline_orders_and_detects_errors(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        $job = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'customer', 'status'],
            ['ORD-101', 'APX-LAP-001', '2', '45000', '1402/05/15', 'علی محمدی', 'COMPLETED'],
            ['ORD-101', 'APX-AUD-002', '1', '55000', '1402/05/15', 'علی محمدی', 'COMPLETED'], // Same order, line 2
            ['ORD-102', 'NON-EXISTENT-SKU', '1', '10000', '1402/05/16', 'مشتری ناشناس', 'COMPLETED'], // Invalid SKU
            ['', 'APX-LAP-001', '1', '45000', '1402/05/17', 'بدون شناسه', 'COMPLETED'], // Missing order_id
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                    'customer' => 'customer',
                    'status' => 'status',
                ],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        $validationService = app(ImportValidationService::class);
        $result = $validationService->validateJob($job);

        $this->assertSame(4, $result['total_rows']);
        $this->assertSame(2, $result['valid_rows']); // 2 lines of ORD-101
        $this->assertSame(2, $result['invalid_rows']); // 1 invalid SKU, 1 missing order_id
        $this->assertNotEmpty($job->metadata['preview_rows']);

        $errors = ImportValidationResult::where('import_job_id', $job->id)->get();
        $this->assertTrue($errors->contains('error_code', ImportErrorCode::PRODUCT_NOT_FOUND));
        $this->assertTrue($errors->contains('error_code', ImportErrorCode::ORDER_ID_REQUIRED));
    }

    public function test_execution_creates_orders_without_inventory_or_accounting_side_effects(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $product = Product::where('organization_id', $this->orgApexId)->first();

        // 1. Establish initial stock and initial accounting balances
        $initialStock = 50;
        WarehouseStock::updateOrCreate(
            ['organization_id' => $this->orgApexId, 'warehouse_id' => $wh->id, 'product_id' => $product->id],
            ['quantity' => $initialStock]
        );

        $cashBefore = (float) Account::where('organization_id', $this->orgApexId)
            ->where('code', AccountingService::ACCOUNT_CASH)
            ->value('balance');
        $revenueBefore = (float) Account::where('organization_id', $this->orgApexId)
            ->where('code', AccountingService::ACCOUNT_SALES_REVENUE)
            ->value('balance');

        $stockMovementsCountBefore = StockMovement::where('organization_id', $this->orgApexId)->count();
        $journalEntriesCountBefore = JournalEntry::where('organization_id', $this->orgApexId)->count();
        $paymentsCountBefore = Payment::where('organization_id', $this->orgApexId)->count();

        // 2. Import a multi-line historical order
        $job = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'customer', 'status', 'total_amount'],
            ['HIST-INV-777', (string) $product->sku, '5', '100000', '1401/10/20', 'خریدار قدیمی', 'COMPLETED', '500000'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                    'customer' => 'customer',
                    'status' => 'status',
                    'total_amount' => 'total_amount',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex);

        $this->assertSame(1, $result['imported_rows']);
        $this->assertSame(0, $result['failed_rows']);

        // 3. VERIFY STRICT INVARIANTS:
        // A. Inventory MUST NOT BE TOUCHED
        $currentStock = (int) WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $wh->id)
            ->where('product_id', $product->id)
            ->value('quantity');
        $this->assertSame($initialStock, $currentStock, 'CRITICAL: Historical order import altered WarehouseStock!');

        $stockMovementsCountAfter = StockMovement::where('organization_id', $this->orgApexId)->count();
        $this->assertSame($stockMovementsCountBefore, $stockMovementsCountAfter, 'CRITICAL: Stock movements created for historical order!');

        // B. Accounting General Ledger MUST NOT BE TOUCHED
        $cashAfter = (float) Account::where('organization_id', $this->orgApexId)
            ->where('code', AccountingService::ACCOUNT_CASH)
            ->value('balance');
        $revenueAfter = (float) Account::where('organization_id', $this->orgApexId)
            ->where('code', AccountingService::ACCOUNT_SALES_REVENUE)
            ->value('balance');
        $this->assertSame($cashBefore, $cashAfter, 'CRITICAL: Historical order modified live Cash on Hand balance!');
        $this->assertSame($revenueBefore, $revenueAfter, 'CRITICAL: Historical order modified live Sales Revenue balance!');

        $journalEntriesCountAfter = JournalEntry::where('organization_id', $this->orgApexId)->count();
        $this->assertSame($journalEntriesCountBefore, $journalEntriesCountAfter, 'CRITICAL: General ledger journal entry created for historical order!');

        // C. Live Payments MUST NOT BE CREATED
        $paymentsCountAfter = Payment::where('organization_id', $this->orgApexId)->count();
        $this->assertSame($paymentsCountBefore, $paymentsCountAfter, 'CRITICAL: Payment record created for historical order!');

        // D. Order correctly persisted with HISTORICAL metadata
        $importedOrder = Order::where('organization_id', $this->orgApexId)
            ->where('external_order_id', 'HIST-INV-777')
            ->first();

        $this->assertNotNull($importedOrder);
        $this->assertTrue($importedOrder->is_historical);
        $this->assertSame('IMPORT_HISTORICAL', $importedOrder->source);
        $this->assertSame('PAID', $importedOrder->payment_status);
        $this->assertSame('COMPLETED', $importedOrder->fulfillment_status);
        $this->assertSame('2023-01-10 12:00:00', $importedOrder->order_date->format('Y-m-d H:i:s'));
        $this->assertSame(500000.0, (float) $importedOrder->total_amount);

        // Verify items
        $this->assertCount(1, $importedOrder->items);
        $this->assertSame(5, (int) $importedOrder->items[0]->quantity);
        $this->assertSame(100000.0, (float) $importedOrder->items[0]->unit_price);
    }

    public function test_accounting_summary_excludes_historical_orders_from_daily_and_pl_metrics(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $product = Product::where('organization_id', $this->orgApexId)->first();

        // Snapshot accounting summary before import
        $resBefore = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->getJson('/api/v1/accounting/summary');
        $resBefore->assertOk();
        $totalRevenueBefore = (float) $resBefore->json('total_revenue');
        $salesCountBefore = (int) $resBefore->json('total_sales_count');

        // Import historical order
        $job = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date'],
            ['HIST-EXCL-01', (string) $product->sku, '1', '990000', '1401/01/15'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        app(ImportExecutionService::class)->executeJob($job, $this->userApex);

        // Query accounting summary after import
        $resAfter = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->getJson('/api/v1/accounting/summary');
        $resAfter->assertOk();
        $totalRevenueAfter = (float) $resAfter->json('total_revenue');
        $salesCountAfter = (int) $resAfter->json('total_sales_count');

        // Operational revenue and sales count MUST NOT INCLUDE historical orders
        $this->assertSame($totalRevenueBefore, $totalRevenueAfter);
        $this->assertSame($salesCountBefore, $salesCountAfter);
    }

    public function test_duplicate_strategy_skip_and_update_behavior(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $product = Product::where('organization_id', $this->orgApexId)->first();

        // 1. Create initial historical order
        $order = Order::create([
            'id' => (string) Str::uuid(),
            'order_number' => 'HIST-DUP-01',
            'external_order_id' => 'EXT-DUP-01',
            'organization_id' => $this->orgApexId,
            'store_id' => $mainStore->id,
            'warehouse_id' => $wh->id,
            'subtotal' => 1000,
            'discount_amount' => 0,
            'tax_amount' => 0,
            'total_amount' => 1000,
            'payment_method' => 'CASH',
            'payment_status' => 'PAID',
            'fulfillment_status' => 'COMPLETED',
            'notes' => 'یادداشت اولیه',
            'source' => 'IMPORT_HISTORICAL',
            'is_historical' => true,
        ]);

        // 2. File with same external_order_id
        $job = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'notes', 'payment_method'],
            ['EXT-DUP-01', (string) $product->sku, '1', '1000', '1402/01/01', 'یادداشت جدید', 'CARD'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                    'notes' => 'notes',
                    'payment_method' => 'payment_method',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);

        // A. Test SKIP strategy
        $skipResult = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);
        $this->assertSame(1, $skipResult['skipped_rows']);
        $this->assertSame(0, $skipResult['imported_rows']);
        $order->refresh();
        $this->assertSame('یادداشت اولیه', $order->notes);

        // B. Test UPDATE strategy (re-test on fresh job)
        $job2 = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'notes', 'payment_method'],
            ['EXT-DUP-01', (string) $product->sku, '1', '1000', '1402/01/01', 'یادداشت جدید', 'CARD'],
        ]);
        $job2->update([
            'mapping_config' => $job->mapping_config,
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $updateResult = $executionService->executeJob($job2, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);
        $this->assertSame(1, $updateResult['imported_rows']);
        $order->refresh();
        $this->assertSame('یادداشت جدید', $order->notes);
        $this->assertSame('CARD', $order->payment_method);
        $this->assertSame(1000.0, (float) $order->total_amount); // Financials preserved
    }

    public function test_tenant_isolation_prevents_foreign_store_or_cross_tenant_access(): void
    {
        $foreignStore = Store::create([
            'id' => 'store_foreign_private',
            'organization_id' => $this->orgOtherId,
            'name' => 'Foreign Store',
            'code' => 'STR-FOR-01',
        ]);

        $job = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date'],
            ['ORD-SEC-01', 'SKU-ESP-001', '1', '10000', '1402/01/01'],
        ]);

        // Attempt to pass foreign store in mapping payload
        $res = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/mapping", [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                ],
                'store_id' => $foreignStore->id,
            ]);

        $res->assertStatus(422);
        $res->assertJsonFragment([
            'message' => 'فروشگاه پیش‌فرض انتخاب‌شده در سامانه یافت نشد یا به این سازمان تعلق ندارد.',
        ]);
    }

    public function test_normal_pos_checkout_pipeline_remains_fully_unaffected(): void
    {
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $product = Product::where('organization_id', $this->orgApexId)->first();

        // Ensure sufficient stock
        WarehouseStock::updateOrCreate(
            ['organization_id' => $this->orgApexId, 'warehouse_id' => $wh->id, 'product_id' => $product->id],
            ['quantity' => 20]
        );

        $res = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson('/api/v1/orders/checkout', [
                'customer_name' => 'Live POS Customer',
                'payment_method' => 'CASH',
                'warehouse_id' => $wh->id,
                'items' => [
                    [
                        'product_id' => $product->id,
                        'quantity' => 2,
                    ]
                ],
            ]);

        $res->assertStatus(201);
        $order = $res->json();

        // Verify normal POS orders are NOT marked as historical
        $savedOrder = Order::find($order['id']);
        $this->assertNotNull($savedOrder);
        $this->assertFalse($savedOrder->is_historical);
        $this->assertSame('POS', $savedOrder->source);

        // Verify stock was decremented normally for live POS order
        $currentStock = (int) WarehouseStock::where('organization_id', $this->orgApexId)
            ->where('warehouse_id', $wh->id)
            ->where('product_id', $product->id)
            ->value('quantity');
        $this->assertSame(18, $currentStock);
    }

    public function test_financial_details_exact_minor_math_and_reload(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $product = Product::where('organization_id', $this->orgApexId)->first();

        // 3 items with specific prices, discounts, and taxes
        $job = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'discount_amount', 'tax_amount', 'total_amount'],
            ['ORD-FIN-01', (string) $product->sku, '3', '150000.50', '1402/08/10', '10000.00', '12600.00', '452601.50'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                    'discount_amount' => 'discount_amount',
                    'tax_amount' => 'tax_amount',
                    'total_amount' => 'total_amount',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex);
        $this->assertSame(1, $result['imported_rows']);

        // Fresh reload from DB to prove exact persistence without float distortions
        $order = Order::where('organization_id', $this->orgApexId)
            ->where('external_order_id', 'ORD-FIN-01')
            ->with('items')
            ->firstOrFail();

        $this->assertSame('450001.50', number_format((float) $order->subtotal, 2, '.', ''));
        $this->assertSame('10000.00', number_format((float) $order->discount_amount, 2, '.', ''));
        $this->assertSame('12600.00', number_format((float) $order->tax_amount, 2, '.', ''));
        $this->assertSame('452601.50', number_format((float) $order->total_amount, 2, '.', ''));

        $this->assertCount(1, $order->items);
        $item = $order->items[0];
        $this->assertSame(3, (int) $item->quantity);
        $this->assertSame('150000.50', number_format((float) $item->unit_price, 2, '.', ''));
        $this->assertSame('450001.50', number_format((float) $item->subtotal, 2, '.', ''));
        $this->assertSame('10000.00', number_format((float) $item->discount_amount, 2, '.', ''));
        $this->assertSame('12600.00', number_format((float) $item->tax_amount, 2, '.', ''));
        $this->assertSame('452601.50', number_format((float) $item->total_price, 2, '.', ''));
    }

    public function test_reexecuting_same_job_is_idempotent(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $product = Product::where('organization_id', $this->orgApexId)->first();

        $job = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date'],
            ['ORD-IDEMP-01', (string) $product->sku, '2', '50000', '1402/06/01'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $run1 = $executionService->executeJob($job, $this->userApex);
        $this->assertSame(1, $run1['imported_rows']);

        // Reset status to simulate replay/retry
        $job->update(['status' => ImportStatus::PREVIEW_READY]);
        $run2 = $executionService->executeJob($job, $this->userApex);

        // All rows skipped because deterministic order already exists for this job
        $this->assertSame(1, $run2['skipped_rows']);
        $this->assertSame(0, $run2['imported_rows']);

        // Total orders in DB remains exactly 1
        $count = Order::where('organization_id', $this->orgApexId)
            ->where('external_order_id', 'ORD-IDEMP-01')
            ->count();
        $this->assertSame(1, $count);
    }

    public function test_two_different_jobs_importing_same_order_respect_duplicate_strategy(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();
        $wh = Warehouse::where('organization_id', $this->orgApexId)->first();
        $product = Product::where('organization_id', $this->orgApexId)->first();

        $job1 = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'notes'],
            ['ORD-MULTI-JOB', (string) $product->sku, '1', '75000', '1402/07/01', 'اولین ایمپورت'],
        ]);
        $job1->update([
            'mapping_config' => [
                'fields' => [
                    'order_id' => 'order_id',
                    'sku' => 'sku',
                    'quantity' => 'quantity',
                    'unit_price' => 'unit_price',
                    'date' => 'date',
                    'notes' => 'notes',
                ],
                'store_id' => $mainStore->id,
                'warehouse_id' => $wh->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $executionService->executeJob($job1, $this->userApex);

        // Job 2 has different job ID, same external_order_id
        $job2 = $this->createMockJob(ImportType::ORDERS, [
            ['order_id', 'sku', 'quantity', 'unit_price', 'date', 'notes'],
            ['ORD-MULTI-JOB', (string) $product->sku, '1', '75000', '1402/07/01', 'دومین ایمپورت'],
        ]);
        $job2->update([
            'mapping_config' => $job1->mapping_config,
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        // Strategy SKIP: does not create duplicate and does not overwrite notes
        $resSkip = $executionService->executeJob($job2, $this->userApex, ImportExecutionService::STRATEGY_SKIP);
        $this->assertSame(1, $resSkip['skipped_rows']);
        $this->assertSame(0, $resSkip['imported_rows']);

        $orderCount = Order::where('organization_id', $this->orgApexId)
            ->where('external_order_id', 'ORD-MULTI-JOB')
            ->count();
        $this->assertSame(1, $orderCount);

        $order = Order::where('organization_id', $this->orgApexId)
            ->where('external_order_id', 'ORD-MULTI-JOB')
            ->first();
        $this->assertSame('اولین ایمپورت', $order->notes);
    }
}
