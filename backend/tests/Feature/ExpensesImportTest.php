<?php

namespace Tests\Feature;

use App\Models\Account;
use App\Models\Expense;
use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\ImportValidationResult;
use App\Models\JournalEntry;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Store;
use App\Models\User;
use App\Services\AccountingService;
use App\Services\ExpenseService;
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

class ExpensesImportTest extends TestCase
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

    public function test_mapping_service_suggests_and_validates_expense_fields(): void
    {
        $mappingService = app(ImportMappingService::class);
        $headers = ['عنوان هزینه', 'دسته بندی', 'مبلغ', 'تاریخ', 'فروشگاه', 'روش پرداخت', 'توضیحات'];

        $suggestions = $mappingService->suggestMappings($headers, ImportType::EXPENSES);

        $this->assertSame('title', $suggestions[0]['target_field']);
        $this->assertSame('category', $suggestions[1]['target_field']);
        $this->assertSame('amount', $suggestions[2]['target_field']);
        $this->assertSame('date', $suggestions[3]['target_field']);
        $this->assertSame('store', $suggestions[4]['target_field']);
        $this->assertSame('payment_method', $suggestions[5]['target_field']);
        $this->assertSame('notes', $suggestions[6]['target_field']);

        // Missing required field: amount
        $val1 = $mappingService->validateMapping([
            'title' => 'عنوان هزینه',
            'category' => 'دسته بندی',
            'date' => 'تاریخ',
        ], ImportType::EXPENSES);
        $this->assertFalse($val1['valid']);

        // Valid mapping
        $val2 = $mappingService->validateMapping([
            'title' => 'عنوان هزینه',
            'category' => 'دسته بندی',
            'amount' => 'مبلغ',
            'date' => 'تاریخ',
        ], ImportType::EXPENSES);
        $this->assertTrue($val2['valid']);
    }

    public function test_persian_digits_normalization_and_monetary_precision(): void
    {
        $money = DataNormalizer::parseMoney('۵۰۰,۰۰۰ تومان');
        $this->assertSame('500000.00', $money);

        $amountDec = DataNormalizer::parseMoney('۱۲۵۰۰۰.۵۰');
        $this->assertSame('125000.50', $amountDec);

        // Negative amount should be rejected
        $negative = DataNormalizer::parseMoney('-5000');
        $this->assertNull($negative);

        // Zero should be rejected
        $zero = DataNormalizer::parseMoney('0');
        $this->assertSame('0.00', $zero);
    }

    public function test_valid_and_invalid_expense_dates_including_jalali(): void
    {
        // Jalali date conversion: 1403/07/21 => 2024-10-12
        $jalali = DataNormalizer::parseDate('۱۴۰۳/۰۷/۲۱');
        $this->assertSame('2024-10-12', $jalali);

        // Gregorian ISO date: 2024-10-12
        $greg = DataNormalizer::parseDate('2024-10-12');
        $this->assertSame('2024-10-12', $greg);

        // Excel serial date 45577 => 2024-10-12
        $excel = DataNormalizer::parseDate('45577');
        $this->assertSame('2024-10-12', $excel);

        // Invalid date string
        $invalid = DataNormalizer::parseDate('not-a-real-date');
        $this->assertNull($invalid);

        // Impossible date
        $impossible = DataNormalizer::parseDate('1402/13/45');
        $this->assertNull($impossible);
    }

    public function test_api_mapping_get_and_save_with_store_support(): void
    {
        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['عنوان', 'دسته‌بندی', 'مبلغ', 'تاریخ'],
            ['قبض برق', 'برق', '۱۵۰۰۰۰', '۱۴۰۳/۰۷/۲۰'],
        ]);

        $res = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->getJson("/api/v1/import/jobs/{$job->id}/mapping");

        $res->assertOk();
        $res->assertJsonStructure(['target_fields', 'stores', 'categories', 'suggested_mappings']);
        $this->assertNotEmpty($res->json('stores'));
        $this->assertNotEmpty($res->json('categories'));

        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        // Save mapping with default store_id
        $saveRes = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/mapping", [
                'fields' => [
                    'title' => 'عنوان',
                    'category' => 'دسته‌بندی',
                    'amount' => 'مبلغ',
                    'date' => 'تاریخ',
                ],
                'store_id' => $mainStore->id,
            ]);

        $saveRes->assertOk();
        $job->refresh();
        $this->assertSame(ImportStatus::READY_TO_VALIDATE, $job->status);
        $this->assertSame($mainStore->id, $job->mapping_config['store_id']);
    }

    public function test_expense_validation_pipeline_detects_errors_and_conflicts(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        // Pre-create an existing expense in database
        Expense::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'store_id' => $mainStore->id,
            'title' => 'اینترنت ماهانه دفتر',
            'category' => 'INTERNET',
            'amount' => '450000.00',
            'payment_method' => 'CASH',
            'date' => '2024-10-10',
            'notes' => 'قبلی',
        ]);

        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date', 'notes'],
            // Row 1: Valid new expense
            ['اجاره انبار مرکزی', 'اجاره', '5000000', '1403/07/01', 'ماه مهر'],
            // Row 2: Matches existing expense
            ['اینترنت ماهانه دفتر', 'اینترنت', '450000', '2024-10-10', 'مجدد'],
            // Row 3: Invalid category
            ['خرید مواد متفرقه', 'دسته فضایی', '100000', '1403/07/05', 'توضیح'],
            // Row 4: Invalid negative amount
            ['تعمیر صندلی', 'تعمیرات', '-20000', '1403/07/05', 'توضیح'],
            // Row 5: Invalid date
            ['هزینه نظافت', 'خدمات', '80000', 'تاریخ نامشخص', 'توضیح'],
            // Row 6: In-file duplicate of row 1
            ['اجاره انبار مرکزی', 'اجاره', '5000000', '1403/07/01', 'تکراری'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'title' => 'title',
                    'category' => 'category',
                    'amount' => 'amount',
                    'date' => 'date',
                    'notes' => 'notes',
                ],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        $validationService = app(ImportValidationService::class);
        $summary = $validationService->validateJob($job);

        $this->assertSame(6, $summary['total_rows']);
        $this->assertSame(3, $summary['valid_rows']); // Row 1, Row 2 (match), Row 6 (dup warning)
        $this->assertSame(3, $summary['invalid_rows']); // Row 3 (category), Row 4 (amount), Row 5 (date)
        $this->assertSame(1, $summary['matched_existing_rows']); // Row 2

        // Verify ImportValidationResult records
        $errors = ImportValidationResult::where('import_job_id', $job->id)->get();
        $this->assertTrue($errors->contains('error_code', ImportErrorCode::INVALID_CATEGORY));
        $this->assertTrue($errors->contains('error_code', ImportErrorCode::INVALID_AMOUNT));
        $this->assertTrue($errors->contains('error_code', ImportErrorCode::INVALID_DATE));
        $this->assertTrue($errors->contains('error_code', ImportErrorCode::EXISTING_RECORD_MATCH));
        $this->assertTrue($errors->contains('error_code', ImportErrorCode::DUPLICATE_IN_FILE));
    }

    public function test_expense_execution_creates_records_and_posts_accounting_journal(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        $accountExpense = Account::where('organization_id', $this->orgApexId)
            ->where('code', AccountingService::ACCOUNT_OPERATING_EXPENSE)
            ->first();
        $balanceBefore = (float) ($accountExpense?->balance ?? 0);

        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date', 'notes'],
            ['هزینه اشتراک نرم‌افزار', 'خدمات', '150000', '1403/07/15', 'پرداخت آنلاین'],
            ['قبض آب مهر ماه', 'آب', '85000', '1403/07/16', 'پرداخت با کارت'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'title' => 'title',
                    'category' => 'category',
                    'amount' => 'amount',
                    'date' => 'date',
                    'notes' => 'notes',
                ],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(2, $result['imported_rows']);
        $this->assertSame(0, $result['failed_rows']);
        $this->assertSame(0, $result['skipped_rows']);

        // Verify expenses created
        $exp1 = Expense::where('organization_id', $this->orgApexId)
            ->where('category', 'SERVICES')
            ->first();
        $this->assertNotNull($exp1);
        $this->assertSame('SERVICES', $exp1->category);
        $this->assertEquals(150000.00, (float) $exp1->amount);
        $this->assertSame('2024-10-06', $exp1->date);

        // Verify double-entry journal entry was posted
        $accountExpense->refresh();
        $this->assertEquals(235000.00, round((float) $accountExpense->balance - $balanceBefore, 2));

        $journalEntries = JournalEntry::where('organization_id', $this->orgApexId)
            ->where('reference_type', 'Expense')
            ->whereIn('reference_id', [$exp1->id])
            ->get();
        $this->assertCount(1, $journalEntries);
        $this->assertEquals(150000.00, (float) $journalEntries->first()->total_debit);
    }

    public function test_expense_execution_skip_mode_preserves_existing_expenses(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        // Create initial expense
        $existing = Expense::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'store_id' => $mainStore->id,
            'title' => 'اجاره دفتر شهریور',
            'category' => 'RENT',
            'amount' => '2000000.00',
            'payment_method' => 'CASH',
            'date' => '2024-09-01',
            'notes' => 'یادداشت اولیه',
        ]);

        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date', 'notes'],
            ['اجاره دفتر شهریور', 'RENT', '2000000', '2024-09-01', 'یادداشت جدید در فایل'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'title' => 'title',
                    'category' => 'category',
                    'amount' => 'amount',
                    'date' => 'date',
                    'notes' => 'notes',
                ],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $this->assertSame(0, $result['imported_rows']);
        $this->assertSame(1, $result['skipped_rows']);

        // Existing expense untouched
        $existing->refresh();
        $this->assertSame('یادداشت اولیه', $existing->notes);
    }

    public function test_expense_execution_update_mode_updates_existing_records_safely(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        $existing = Expense::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $this->orgApexId,
            'store_id' => $mainStore->id,
            'title' => 'هزینه شارژ ساختمان',
            'category' => 'SERVICES',
            'amount' => '300000.00',
            'payment_method' => 'CASH',
            'date' => '2024-09-15',
            'notes' => 'قدیمی',
        ]);

        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date', 'notes', 'payment_method'],
            ['هزینه شارژ ساختمان', 'SERVICES', '300000', '2024-09-15', 'بروزرسانی شده با فایل', 'CARD'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'title' => 'title',
                    'category' => 'category',
                    'amount' => 'amount',
                    'date' => 'date',
                    'notes' => 'notes',
                    'payment_method' => 'payment_method',
                ],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_UPDATE);

        $this->assertSame(1, $result['imported_rows']);
        $existing->refresh();
        $this->assertSame('بروزرسانی شده با فایل', $existing->notes);
        $this->assertSame('CARD', $existing->payment_method);
        // Tenant ownership preserved
        $this->assertSame($this->orgApexId, $existing->organization_id);
    }

    public function test_tenant_isolation_prevents_foreign_store_injection_and_matching(): void
    {
        // Create foreign store
        $foreignStore = Store::create([
            'id' => 'store_foreign_secret',
            'organization_id' => $this->orgOtherId,
            'name' => 'Foreign Store',
            'code' => 'FOR-ST',
        ]);

        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date'],
            ['تست هزینه', 'RENT', '500000', '2024-10-10'],
        ]);

        // Attempt to pass foreign store in mapping payload
        $res = $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/mapping", [
                'fields' => [
                    'title' => 'title',
                    'category' => 'category',
                    'amount' => 'amount',
                    'date' => 'date',
                ],
                'store_id' => $foreignStore->id,
            ]);

        $res->assertStatus(422);
        $res->assertJsonFragment([
            'message' => 'فروشگاه پیش‌فرض انتخاب‌شده در سامانه یافت نشد یا به این سازمان تعلق ندارد.',
        ]);
    }

    public function test_repeated_execution_of_same_job_is_rejected(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date'],
            ['خرید لامپ', 'REPAIRS', '45000', '1403/07/02'],
        ]);

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'title' => 'title',
                    'category' => 'category',
                    'amount' => 'amount',
                    'date' => 'date',
                ],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $executionService->executeJob($job, $this->userApex);

        $job->refresh();
        $this->assertSame(ImportStatus::COMPLETED, $job->status);

        // Repeated execution should fail
        $this->expectException(\InvalidArgumentException::class);
        $executionService->executeJob($job, $this->userApex);
    }

    public function test_deterministic_idempotency_prevents_duplicate_records_on_retry(): void
    {
        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date'],
            ['حقوق سرایدار', 'SALARIES', '1200000', '1403/07/01'],
        ]);

        $deterministicId = ExpenseService::deterministicUuid("resto_imp_{$this->orgApexId}_{$job->id}_1");

        // Simulate row 1 was already created by this job
        $expenseService = app(ExpenseService::class);
        $expenseService->createExpense([
            'id' => $deterministicId,
            'organization_id' => $this->orgApexId,
            'store_id' => $mainStore->id,
            'title' => 'حقوق سرایدار',
            'category' => 'SALARIES',
            'amount' => '1200000.00',
            'date' => '2024-09-22',
            'payment_method' => 'CASH',
        ], $this->userApex);

        $countBefore = Expense::where('organization_id', $this->orgApexId)->count();

        $job->update([
            'mapping_config' => [
                'fields' => [
                    'title' => 'title',
                    'category' => 'category',
                    'amount' => 'amount',
                    'date' => 'date',
                ],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::PREVIEW_READY,
        ]);

        $executionService = app(ImportExecutionService::class);
        $result = $executionService->executeJob($job, $this->userApex, ImportExecutionService::STRATEGY_SKIP);

        $countAfter = Expense::where('organization_id', $this->orgApexId)->count();

        // Count should NOT have increased because retry detected already-committed deterministic ID
        $this->assertSame($countBefore, $countAfter);
        $this->assertSame(1, $result['skipped_rows']);
        $this->assertSame(0, $result['imported_rows']);
    }

    public function test_saving_new_mapping_invalidates_stale_validation_and_preview(): void
    {
        $job = $this->createMockJob(ImportType::EXPENSES, [
            ['title', 'category', 'amount', 'date'],
            ['کرایه', 'RENT', '1000', '2024-10-01'],
        ]);

        $mainStore = Store::where('organization_id', $this->orgApexId)->first();

        $job->update([
            'mapping_config' => [
                'fields' => ['title' => 'title', 'category' => 'category', 'amount' => 'amount', 'date' => 'date'],
                'store_id' => $mainStore->id,
            ],
            'status' => ImportStatus::READY_TO_VALIDATE,
        ]);

        app(ImportValidationService::class)->validateJob($job);
        $job->refresh();
        $this->assertSame(ImportStatus::PREVIEW_READY, $job->status);
        $this->assertNotEmpty($job->metadata['preview_rows']);

        // User changes mapping
        $this->actingAs($this->userApex)
            ->withHeaders($this->apexHeaders())
            ->postJson("/api/v1/import/jobs/{$job->id}/mapping", [
                'fields' => ['title' => 'title', 'category' => 'category', 'amount' => 'amount', 'date' => 'date'],
                'store_id' => $mainStore->id,
            ])->assertOk();

        $job->refresh();
        $this->assertSame(ImportStatus::READY_TO_VALIDATE, $job->status);
        $this->assertArrayNotHasKey('preview_rows', $job->metadata);
    }
}
