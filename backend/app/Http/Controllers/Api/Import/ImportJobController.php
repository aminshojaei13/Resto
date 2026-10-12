<?php

namespace App\Http\Controllers\Api\Import;

use App\Http\Controllers\Controller;
use App\Models\ImportJob;
use App\Services\Import\ImportJobService;
use App\Services\Import\ImportStorageService;
use App\Support\MembershipContext;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\StreamedResponse;

class ImportJobController extends Controller
{
    public function __construct(
        private ImportJobService $jobService,
        private ImportStorageService $storageService,
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
}
