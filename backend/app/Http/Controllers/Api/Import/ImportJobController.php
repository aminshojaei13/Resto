<?php

namespace App\Http\Controllers\Api\Import;

use App\Http\Controllers\Controller;
use App\Models\ImportJob;
use App\Models\ImportValidationResult;
use App\Services\Import\Execution\ImportExecutionService;
use App\Services\Import\ImportJobService;
use App\Services\Import\ImportStateMachine;
use App\Services\Import\ImportStorageService;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Validation\ImportValidationService;
use App\Support\Import\ImportStatus;
use App\Support\MembershipContext;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\StreamedResponse;

class ImportJobController extends Controller
{
    public function __construct(
        private ImportJobService $jobService,
        private ImportStorageService $storageService,
        private ImportMappingService $mappingService,
        private ImportValidationService $validationService,
        private ImportExecutionService $executionService,
        private ImportStateMachine $stateMachine,
    ) {
    }

    /**
     * Create an import job and upload source file.
     */
    public function store(Request $request): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $user = MembershipContext::user($request);

        if (!$user) {
            return response()->json(['message' => 'کاربر احراز هویت نشده است.'], 401);
        }

        $request->validate([
            'file' => 'required|file|max:20480', // 20 MB max
            'import_type' => 'required|string|in:products,customers,inventory,expenses,orders,suppliers,purchases',
            'idempotency_key' => 'nullable|string|max:120',
        ], [
            'file.required' => 'فایل ورودی الزامی است.',
            'file.max' => 'حجم فایل نباید بیش از ۲۰ مگابایت باشد.',
            'import_type.required' => 'نوع داده برای درون‌ریزی الزامی است.',
            'import_type.in' => 'نوع داده انتخاب‌شده معتبر نیست.',
        ]);

        $file = $request->file('file');
        $importType = $request->input('import_type');
        $idempotencyKey = $request->input('idempotency_key')
            ?? $request->header('X-Idempotency-Key');

        try {
            $job = $this->jobService->createJob(
                $user,
                $organizationId,
                $importType,
                $file,
                $idempotencyKey
            );

            return response()->json([
                'job' => $job,
                'summary' => $this->jobService->getSummary($job),
            ], 201);
        } catch (\InvalidArgumentException $e) {
            return response()->json([
                'message' => $e->getMessage(),
            ], 422);
        } catch (\Throwable $e) {
            return response()->json([
                'message' => 'خطا در ثبت و بارگذاری فایل: ' . $e->getMessage(),
            ], 500);
        }
    }

    /**
     * List import jobs for current tenant.
     */
    public function index(Request $request): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $query = ImportJob::where('organization_id', $organizationId)
            ->with(['sources', 'datasets'])
            ->orderByDesc('created_at');

        if ($request->filled('status')) {
            $query->where('status', $request->query('status'));
        }

        if ($request->filled('import_type')) {
            $query->where('import_type', $request->query('import_type'));
        }

        $perPage = min(max((int) $request->query('per_page', 15), 1), 100);
        $paginated = $query->paginate($perPage);

        return response()->json($paginated);
    }

    /**
     * Retrieve import job details.
     */
    public function show(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->with(['sources', 'datasets'])
            ->first();

        if (!$job) {
            return response()->json([
                'message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.',
            ], 404);
        }

        return response()->json([
            'job' => $job,
            'summary' => $this->jobService->getSummary($job),
        ]);
    }

    /**
     * Retrieve progress counters and status.
     */
    public function progress(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json([
                'message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.',
            ], 404);
        }

        return response()->json([
            'status' => $job->status,
            'progress' => [
                'total_rows' => $job->total_rows,
                'processed_rows' => $job->processed_rows,
                'valid_rows' => $job->valid_rows,
                'invalid_rows' => $job->invalid_rows,
                'imported_rows' => $job->imported_rows,
                'skipped_rows' => $job->skipped_rows,
                'failed_rows' => $job->failed_rows,
            ],
            'summary' => $this->jobService->getSummary($job),
        ]);
    }

    /**
     * Cancel a cancellable import job.
     */
    public function cancel(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $user = MembershipContext::user($request);

        if (!$user) {
            return response()->json(['message' => 'کاربر احراز هویت نشده است.'], 401);
        }

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json([
                'message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.',
            ], 404);
        }

        try {
            $reason = $request->input('reason', 'لغو توسط کاربر');
            $cancelled = $this->jobService->cancelJob($job, $user, $reason);

            return response()->json([
                'message' => 'عملیات با موفقیت لغو شد.',
                'job' => $cancelled,
                'summary' => $this->jobService->getSummary($cancelled),
            ]);
        } catch (\InvalidArgumentException $e) {
            return response()->json([
                'message' => $e->getMessage(),
            ], 422);
        }
    }

    /**
     * Retrieve validation errors and warnings for job.
     */
    public function errors(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json([
                'message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.',
            ], 404);
        }

        $query = $job->validationResults()->orderBy('row_number');

        if ($request->filled('severity')) {
            $query->where('severity', $request->query('severity'));
        }

        $perPage = min(max((int) $request->query('per_page', 50), 1), 200);

        return response()->json($query->paginate($perPage));
    }

    /**
     * Securely download original uploaded source file.
     */
    public function download(Request $request, string $id): StreamedResponse|JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json([
                'message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.',
            ], 404);
        }

        $source = $job->sources()->first();
        if (!$source) {
            return response()->json([
                'message' => 'فایل منبع یافت نشد.',
            ], 404);
        }

        try {
            $stream = $this->storageService->getStream($source->storage_path);
            $filename = $source->original_filename;

            return response()->stream(
                function () use ($stream) {
                    fpassthru($stream);
                    if (is_resource($stream)) {
                        fclose($stream);
                    }
                },
                200,
                [
                    'Content-Type' => 'application/octet-stream',
                    'Content-Disposition' => 'attachment; filename="' . rawurlencode($filename) . '"',
                    'Content-Length' => (string) $source->file_size,
                ]
            );
        } catch (\Throwable $e) {
            return response()->json([
                'message' => 'خطا در بارگیری فایل: ' . $e->getMessage(),
            ], 404);
        }
    }

    /**
     * Get target fields, datasets, and mapping suggestions for the job.
     */
    public function getMapping(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->with(['datasets'])
            ->first();

        if (!$job) {
            return response()->json(['message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.'], 404);
        }

        $targetFields = $this->mappingService->getTargetFields($job->import_type);

        $datasets = $job->datasets;
        $sheetIndex = (int) $request->query('sheet_index', 0);
        $dataset = $datasets->firstWhere('sheet_index', $sheetIndex) ?? $datasets->first();

        $headers = $dataset ? ($dataset->headers ?? []) : [];
        $suggestions = $this->mappingService->suggestMappings($headers, $job->import_type);

        return response()->json([
            'job_id' => $job->id,
            'import_type' => $job->import_type,
            'status' => $job->status,
            'target_fields' => array_values($targetFields),
            'datasets' => $datasets,
            'active_sheet_index' => $dataset ? $dataset->sheet_index : 0,
            'headers' => $headers,
            'sample_rows' => $dataset ? ($dataset->sample_rows ?? []) : [],
            'suggested_mappings' => $suggestions,
            'current_mapping' => $job->mapping_config,
        ]);
    }

    /**
     * Save column mapping configuration for the job.
     */
    public function saveMapping(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json(['message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.'], 404);
        }

        $request->validate([
            'fields' => 'required|array',
            'sheet_index' => 'nullable|integer',
        ], [
            'fields.required' => 'تطبیق فیلدها الزامی است.',
            'fields.array' => 'فرمت تطبیق فیلدها معتبر نیست.',
        ]);

        $fields = $request->input('fields');
        $sheetIndex = (int) $request->input('sheet_index', 0);

        // Validate mapping logic
        $valResult = $this->mappingService->validateMapping($fields, $job->import_type);
        if (!$valResult['valid']) {
            return response()->json([
                'message' => 'خطا در اعتبارسنجی تطبیق ستون‌ها',
                'errors' => $valResult['errors'],
            ], 422);
        }

        // Clear any previous validation results and reset progress counters when mapping changes
        ImportValidationResult::where('import_job_id', $job->id)->delete();
        $meta = $job->metadata ?? [];
        unset($meta['preview_rows'], $meta['matched_existing_rows']);

        // Save mapping config
        $job->update([
            'valid_rows' => 0,
            'invalid_rows' => 0,
            'processed_rows' => 0,
            'metadata' => $meta,
            'mapping_config' => [
                'sheet_index' => $sheetIndex,
                'fields' => $fields,
                'saved_at' => now()->toIso8601String(),
            ],
        ]);

        // If status is mapping_required or preview_ready, advance to ready_to_validate
        if (in_array($job->status, [ImportStatus::MAPPING_REQUIRED, ImportStatus::PREVIEW_READY], true)) {
            $this->stateMachine->transition($job, ImportStatus::READY_TO_VALIDATE, 'Mapping configuration saved/updated');
        }

        return response()->json([
            'message' => 'پیکربندی تطبیق ستون‌ها با موفقیت ذخیره شد.',
            'job' => $job->fresh(['datasets', 'sources']),
        ]);
    }

    /**
     * Run row-level and field-level validation on the mapped file.
     */
    public function validateData(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json(['message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.'], 404);
        }

        try {
            $summary = $this->validationService->validateJob($job);

            return response()->json([
                'message' => 'اعتبارسنجی داده‌ها با موفقیت انجام شد.',
                'summary' => $summary,
                'job' => $job->fresh(),
            ]);
        } catch (\InvalidArgumentException $e) {
            return response()->json(['message' => $e->getMessage()], 422);
        } catch (\Throwable $e) {
            return response()->json(['message' => 'خطا در اعتبارسنجی: ' . $e->getMessage()], 500);
        }
    }

    /**
     * Retrieve preview data and summary.
     */
    public function preview(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json(['message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.'], 404);
        }

        $meta = $job->metadata ?? [];
        $previewRows = $meta['preview_rows'] ?? [];

        return response()->json([
            'job_id' => $job->id,
            'status' => $job->status,
            'total_rows' => $job->total_rows,
            'valid_rows' => $job->valid_rows,
            'invalid_rows' => $job->invalid_rows,
            'matched_existing_rows' => $meta['matched_existing_rows'] ?? 0,
            'preview_rows' => $previewRows,
            'can_execute' => in_array($job->status, [ImportStatus::PREVIEW_READY, ImportStatus::READY_TO_IMPORT], true) && $job->valid_rows > 0,
        ]);
    }

    /**
     * Execute user-confirmed import.
     */
    public function executeImport(Request $request, string $id): JsonResponse
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $user = MembershipContext::user($request);

        if (!$user) {
            return response()->json(['message' => 'کاربر احراز هویت نشده است.'], 401);
        }

        $job = ImportJob::where('organization_id', $organizationId)
            ->where('id', $id)
            ->first();

        if (!$job) {
            return response()->json(['message' => 'عملیات درون‌ریزی مورد نظر یافت نشد.'], 404);
        }

        $strategy = $request->input('duplicate_strategy', ImportExecutionService::STRATEGY_SKIP);

        try {
            $result = $this->executionService->executeJob($job, $user, $strategy);

            return response()->json([
                'message' => 'عملیات درون‌ریزی با موفقیت به پایان رسید.',
                'result' => $result,
                'job' => $job->fresh(),
            ]);
        } catch (\InvalidArgumentException $e) {
            return response()->json(['message' => $e->getMessage()], 422);
        } catch (\Throwable $e) {
            return response()->json(['message' => 'خطا در اجرای درون‌ریزی: ' . $e->getMessage()], 500);
        }
    }
}

