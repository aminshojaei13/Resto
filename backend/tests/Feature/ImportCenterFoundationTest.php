<?php

namespace Tests\Feature;

use App\Jobs\InspectImportJob;
use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\User;
use App\Services\Import\ImportStateMachine;
use App\Services\Import\SpreadsheetInspectorService;
use App\Support\Import\FileFormat;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Queue;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;
use ZipArchive;

class ImportCenterFoundationTest extends TestCase
{
    use RefreshDatabase;

    protected User $userApex;
    protected User $userOther;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        Storage::fake('local');

        $this->userApex = User::find('usr_admin_1');

        // Create an isolated user for a different organization
        $this->userOther = User::create([
            'id' => 'usr_other_tenant',
            'name' => 'Foreign User',
            'email' => 'other@tenant.com',
            'password' => bcrypt('password123'),
        ]);

        $orgOther = Organization::create([
            'id' => 'org_foreign',
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
        ]);
    }

    private function apexHeaders(): array
    {
        return [
            'X-Tenant-ID' => 'org_apex',
            'X-Store-ID' => 'store_apex_1',
        ];
    }

    private function foreignHeaders(): array
    {
        return [
            'X-Tenant-ID' => 'org_foreign',
        ];
    }

    /* ---------------------------------------------------- 1. Authorization & Tenancy */

    public function test_unauthenticated_requests_are_rejected(): void
    {
        $response = $this->getJson('/api/v1/import/jobs', $this->apexHeaders());
        $response->assertStatus(401);

        $response = $this->postJson('/api/v1/import/jobs', [], $this->apexHeaders());
        $response->assertStatus(401);
    }

    public function test_authorized_tenant_user_can_access_own_import_jobs(): void
    {
        $this->actingAs($this->userApex);

        $response = $this->getJson('/api/v1/import/jobs', $this->apexHeaders());
        $response->assertStatus(200);
        $response->assertJsonStructure(['data', 'current_page', 'total']);
    }

    public function test_tenant_user_cannot_access_another_tenants_import_job(): void
    {
        $this->actingAs($this->userApex);

        // Create job for apex
        $csvContent = "نام کالا,کد,قیمت\nکالای نمونه,SKU-1,15000\n";
        $file = UploadedFile::fake()->createWithContent('products.csv', $csvContent);

        $createRes = $this->postJson('/api/v1/import/jobs', [
            'file' => $file,
            'import_type' => ImportType::PRODUCTS,
        ], $this->apexHeaders());

        $createRes->assertStatus(201);
        $jobId = $createRes->json('job.id');

        // Now foreign user attempts to read apex's job
        $this->actingAs($this->userOther);

        $foreignRes = $this->getJson("/api/v1/import/jobs/{$jobId}", $this->foreignHeaders());
        $foreignRes->assertStatus(404);

        $downloadRes = $this->getJson("/api/v1/import/jobs/{$jobId}/download", $this->foreignHeaders());
        $downloadRes->assertStatus(404);

        $progressRes = $this->getJson("/api/v1/import/jobs/{$jobId}/progress", $this->foreignHeaders());
        $progressRes->assertStatus(404);

        $cancelRes = $this->postJson("/api/v1/import/jobs/{$jobId}/cancel", [], $this->foreignHeaders());
        $cancelRes->assertStatus(404);
    }

    public function test_supplied_tenant_id_without_membership_is_forbidden(): void
    {
        $this->actingAs($this->userOther);

        // userOther attempts to pass apex's tenant ID
        $response = $this->getJson('/api/v1/import/jobs', [
            'X-Tenant-ID' => 'org_apex',
        ]);

        $response->assertStatus(403);
    }

    /* ---------------------------------------------------- 2. Upload Security */

    public function test_unsupported_file_formats_are_rejected(): void
    {
        $this->actingAs($this->userApex);

        $fakePdf = UploadedFile::fake()->create('malicious.pdf', 50, 'application/pdf');

        $response = $this->postJson('/api/v1/import/jobs', [
            'file' => $fakePdf,
            'import_type' => ImportType::PRODUCTS,
        ], $this->apexHeaders());

        $response->assertStatus(422);
    }

    public function test_suspicious_executable_script_content_is_rejected(): void
    {
        $this->actingAs($this->userApex);

        $maliciousPhp = "<?php echo 'hacked'; ?>\nCol1,Col2\n1,2";
        $file = UploadedFile::fake()->createWithContent('exploit.csv', $maliciousPhp);

        $response = $this->postJson('/api/v1/import/jobs', [
            'file' => $file,
            'import_type' => ImportType::PRODUCTS,
        ], $this->apexHeaders());

        $response->assertStatus(422);
        $this->assertStringContainsString('مشکوک', $response->json('message'));
    }

    public function test_macro_enabled_xlsx_workbook_is_rejected(): void
    {
        $this->actingAs($this->userApex);

        // Build a mock zip with vbaProject.bin
        $tempPath = tempnam(sys_get_temp_dir(), 'macro_') . '.xlsx';
        $zip = new ZipArchive();
        $zip->open($tempPath, ZipArchive::CREATE);
        $zip->addFromString('xl/workbook.xml', '<workbook><sheets><sheet name="Sheet1" sheetId="1"/></sheets></workbook>');
        $zip->addFromString('xl/vbaProject.bin', 'DUMMY_VBA_CODE');
        $zip->close();

        $file = new UploadedFile($tempPath, 'macro.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', null, true);

        $response = $this->postJson('/api/v1/import/jobs', [
            'file' => $file,
            'import_type' => ImportType::PRODUCTS,
        ], $this->apexHeaders());

        $response->assertStatus(422);
        $this->assertStringContainsString('ماکرو', $response->json('message'));
        @unlink($tempPath);
    }

    public function test_original_filename_does_not_control_storage_path(): void
    {
        $this->actingAs($this->userApex);

        $csv = "ColA,ColB\nValA,ValB\n";
        $file = UploadedFile::fake()->createWithContent('../../../etc/passwd.csv', $csv);

        $response = $this->postJson('/api/v1/import/jobs', [
            'file' => $file,
            'import_type' => ImportType::CUSTOMERS,
        ], $this->apexHeaders());

        $response->assertStatus(201);
        $jobId = $response->json('job.id');

        $source = ImportSource::where('import_job_id', $jobId)->first();
        $this->assertNotNull($source);

        // Path must be inside imports/org_apex/UUID.csv, not containing path traversal
        $this->assertStringStartsWith('imports/org_apex/', $source->storage_path);
        $this->assertStringNotContainsString('../', $source->storage_path);
        $this->assertStringNotContainsString('passwd', $source->storage_path);
    }

    /* ---------------------------------------------------- 3. Idempotency */

    public function test_repeated_request_with_same_idempotency_key_returns_existing_job(): void
    {
        $this->actingAs($this->userApex);

        $csv = "کد,نام\n1,مشتری ۱\n";
        $file1 = UploadedFile::fake()->createWithContent('cust.csv', $csv);
        $idempotencyKey = 'import_batch_' . uniqid();

        $res1 = $this->postJson('/api/v1/import/jobs', [
            'file' => $file1,
            'import_type' => ImportType::CUSTOMERS,
            'idempotency_key' => $idempotencyKey,
        ], $this->apexHeaders());

        $res1->assertStatus(201);
        $firstId = $res1->json('job.id');

        // Submit again with same idempotency key
        $file2 = UploadedFile::fake()->createWithContent('cust.csv', $csv);
        $res2 = $this->postJson('/api/v1/import/jobs', [
            'file' => $file2,
            'import_type' => ImportType::CUSTOMERS,
            'idempotency_key' => $idempotencyKey,
        ], $this->apexHeaders());

        $res2->assertStatus(201);
        $secondId = $res2->json('job.id');

        $this->assertEquals($firstId, $secondId);
        $this->assertEquals(1, ImportJob::where('idempotency_key', $idempotencyKey)->count());
    }

    /* ---------------------------------------------------- 4. Lifecycle & Cancellation */

    public function test_job_state_machine_valid_and_invalid_transitions(): void
    {
        $stateMachine = new ImportStateMachine();

        $job = ImportJob::create([
            'id' => 'job_sm_test',
            'organization_id' => 'org_apex',
            'user_id' => $this->userApex->id,
            'import_type' => ImportType::PRODUCTS,
            'status' => ImportStatus::UPLOADED,
            'original_filename' => 'test.csv',
            'detected_format' => FileFormat::CSV,
            'file_size' => 100,
        ]);

        // Valid transition: uploaded -> inspecting
        $stateMachine->transition($job, ImportStatus::INSPECTING, 'Started');
        $this->assertEquals(ImportStatus::INSPECTING, $job->fresh()->status);
        $this->assertNotNull($job->fresh()->started_at);

        // Invalid transition: inspecting -> completed directly is illegal
        $this->expectException(\InvalidArgumentException::class);
        $stateMachine->transition($job, ImportStatus::COMPLETED, 'Skipped steps');
    }

    public function test_cancellable_job_can_be_cancelled_and_terminal_job_cannot(): void
    {
        $this->actingAs($this->userApex);

        $csv = "نام,کد\nکالا ۱,123\n";
        $file = UploadedFile::fake()->createWithContent('prod.csv', $csv);

        $res = $this->postJson('/api/v1/import/jobs', [
            'file' => $file,
            'import_type' => ImportType::PRODUCTS,
        ], $this->apexHeaders());

        $jobId = $res->json('job.id');

        // Cancel job in uploaded state
        $cancelRes = $this->postJson("/api/v1/import/jobs/{$jobId}/cancel", [
            'reason' => 'کاربر انصراف داد',
        ], $this->apexHeaders());

        $cancelRes->assertStatus(200);
        $this->assertEquals(ImportStatus::CANCELLED, $cancelRes->json('job.status'));

        // Trying to cancel again on terminal state fails
        $secondCancel = $this->postJson("/api/v1/import/jobs/{$jobId}/cancel", [], $this->apexHeaders());
        $secondCancel->assertStatus(422);
    }

    /* ---------------------------------------------------- 5. Queue Inspection & Data Extraction */

    public function test_csv_inspection_extracts_headers_and_row_count(): void
    {
        $csvContent = "\xEF\xBB\xBFنام کالا;بارکد;قیمت;توضیحات\nسیب سرخ;1001;50000;کیفیت درجه یک\nموز اکوادور;1002;95000;وارداتی\n\nپرتقال تامسون;1003;40000;\n";
        $tempPath = tempnam(sys_get_temp_dir(), 'csv_inspect_') . '.csv';
        file_put_contents($tempPath, $csvContent);

        $inspector = new SpreadsheetInspectorService();
        $result = $inspector->inspect($tempPath, FileFormat::CSV);

        $this->assertCount(1, $result);
        $sheet = $result[0];

        $this->assertEquals('Sheet1', $sheet['name']);
        $this->assertEquals(3, $sheet['row_count']);
        $this->assertEquals(['نام کالا', 'بارکد', 'قیمت', 'توضیحات'], $sheet['headers']);
        $this->assertCount(3, $sheet['sample_rows']);
        $this->assertEquals(';', $sheet['metadata']['delimiter']);
        $this->assertTrue($sheet['metadata']['has_bom']);

        @unlink($tempPath);
    }

    public function test_xlsx_inspection_extracts_worksheets_and_headers(): void
    {
        // Build valid minimal XLSX file
        $tempPath = tempnam(sys_get_temp_dir(), 'xlsx_inspect_') . '.xlsx';
        $zip = new ZipArchive();
        $zip->open($tempPath, ZipArchive::CREATE);

        $workbookXml = '<workbook><sheets><sheet name="لیست مشتریان" sheetId="1"/></sheets></workbook>';
        $sharedStringsXml = '<sst count="3" uniqueCount="3"><si><t>نام مشتری</t></si><si><t>شماره تماس</t></si><si><t>علی رضایی</t></si></sst>';
        $sheetXml = '<worksheet><sheetData>' .
            '<row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>' .
            '<row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2"><v>09123456789</v></c></row>' .
            '</sheetData></worksheet>';

        $zip->addFromString('xl/workbook.xml', $workbookXml);
        $zip->addFromString('xl/sharedStrings.xml', $sharedStringsXml);
        $zip->addFromString('xl/worksheets/sheet1.xml', $sheetXml);
        $zip->close();

        $inspector = new SpreadsheetInspectorService();
        $sheets = $inspector->inspect($tempPath, FileFormat::XLSX);

        $this->assertCount(1, $sheets);
        $sheet = $sheets[0];
        $this->assertEquals('لیست مشتریان', $sheet['name']);
        $this->assertEquals(1, $sheet['row_count']);
        $this->assertEquals(['نام مشتری', 'شماره تماس'], $sheet['headers']);
        $this->assertEquals([['علی رضایی', '09123456789']], $sheet['sample_rows']);

        @unlink($tempPath);
    }

    public function test_inspect_import_job_queue_handler_updates_database_records(): void
    {
        $csvContent = "عنوان هزینه,مبلغ,تاریخ\nاجاره مغازه,25000000,1403/07/01\nقبض برق,1200000,1403/07/05\n";

        // Store file in test storage
        $storagePath = 'imports/org_apex/test_expense.csv';
        Storage::disk('local')->put($storagePath, $csvContent);

        $job = ImportJob::create([
            'id' => 'job_queue_test',
            'organization_id' => 'org_apex',
            'user_id' => $this->userApex->id,
            'import_type' => ImportType::EXPENSES,
            'status' => ImportStatus::UPLOADED,
            'original_filename' => 'expenses.csv',
            'detected_format' => FileFormat::CSV,
            'file_size' => strlen($csvContent),
        ]);

        ImportSource::create([
            'id' => 'source_queue_test',
            'import_job_id' => $job->id,
            'organization_id' => 'org_apex',
            'original_filename' => 'expenses.csv',
            'storage_path' => $storagePath,
            'file_format' => FileFormat::CSV,
            'file_size' => strlen($csvContent),
            'checksum' => hash('sha256', $csvContent),
        ]);

        $jobHandler = new InspectImportJob($job->id);
        $jobHandler->handle(new SpreadsheetInspectorService(), new ImportStateMachine());

        $freshJob = $job->fresh();
        $this->assertEquals(ImportStatus::MAPPING_REQUIRED, $freshJob->status);
        $this->assertEquals(2, $freshJob->total_rows);

        $dataset = ImportDataset::where('import_job_id', $job->id)->first();
        $this->assertNotNull($dataset);
        $this->assertEquals(['عنوان هزینه', 'مبلغ', 'تاریخ'], $dataset->headers);
        $this->assertEquals(2, $dataset->row_count);
    }

    public function test_authorized_download_returns_file_stream(): void
    {
        $this->actingAs($this->userApex);

        $csv = "Header1,Header2\nVal1,Val2\n";
        $file = UploadedFile::fake()->createWithContent('download_me.csv', $csv);

        $res = $this->postJson('/api/v1/import/jobs', [
            'file' => $file,
            'import_type' => ImportType::SUPPLIERS,
        ], $this->apexHeaders());

        $jobId = $res->json('job.id');

        $downloadRes = $this->get("/api/v1/import/jobs/{$jobId}/download", $this->apexHeaders());
        $downloadRes->assertStatus(200);
        $downloadRes->assertHeader('Content-Disposition', 'attachment; filename="download_me.csv"');
    }
}
