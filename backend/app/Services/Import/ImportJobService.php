<?php

namespace App\Services\Import;

use App\Jobs\InspectImportJob;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\User;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use InvalidArgumentException;

class ImportJobService
{
    public function __construct(
        private ImportStorageService $storageService,
        private ImportStateMachine $stateMachine,
    ) {
    }

    /**
     * Create a new import job and store its source file.
     * Enforces tenant isolation and idempotency.
     */
    public function createJob(
        User $user,
        string $organizationId,
        string $importType,
        UploadedFile $file,
        ?string $idempotencyKey = null
    ): ImportJob {
        if (!ImportType::isValid($importType)) {
            throw new InvalidArgumentException("نوع درون‌ریزی '{$importType}' نامعتبر است.");
        }

        // Idempotency check scoped to organization
        if ($idempotencyKey) {
            $existing = ImportJob::where('organization_id', $organizationId)
                ->where('idempotency_key', $idempotencyKey)
                ->with(['sources', 'datasets'])
                ->first();

            if ($existing) {
                return $existing;
            }
        }

        // Validate and store file safely in private storage
        $stored = $this->storageService->storeUploadedFile($file, $organizationId);

        $jobId = (string) Str::uuid();
        $sourceId = (string) Str::uuid();

        /** @var ImportJob $job */
        $job = DB::transaction(function () use (
            $jobId,
            $sourceId,
            $user,
            $organizationId,
            $importType,
            $stored,
            $idempotencyKey
        ) {
            $job = ImportJob::create([
                'id' => $jobId,
                'organization_id' => $organizationId,
                'user_id' => $user->id,
                'import_type' => $importType,
                'status' => ImportStatus::UPLOADED,
                'original_filename' => $stored['original_filename'],
                'detected_format' => $stored['file_format'],
                'file_size' => $stored['file_size'],
                'idempotency_key' => $idempotencyKey,
                'metadata' => [
                    'client_original_name' => $stored['original_filename'],
                    'storage_disk' => ImportStorageService::DISK,
                ],
            ]);

            ImportSource::create([
                'id' => $sourceId,
                'import_job_id' => $jobId,
                'organization_id' => $organizationId,
                'original_filename' => $stored['original_filename'],
                'storage_path' => $stored['storage_path'],
                'file_format' => $stored['file_format'],
                'file_size' => $stored['file_size'],
                'checksum' => $stored['checksum'],
                'upload_status' => 'uploaded',
            ]);

            return $job;
        });

        // Dispatch background inspection
        InspectImportJob::dispatch($job->id);

        return $job->fresh(['sources', 'datasets']);
    }

    /**
     * Cancel a job if in a cancellable state.
     */
    public function cancelJob(ImportJob $job, User $user, string $reason = 'لغو توسط کاربر'): ImportJob
    {
        if (!$job->isCancellable()) {
            throw new InvalidArgumentException(
                "عملیات درون‌ریزی در وضعیت '{$job->status}' قابل لغو نیست."
            );
        }

        return $this->stateMachine->transition($job, ImportStatus::CANCELLED, $reason);
    }

    /**
     * Format summary representation.
     *
     * @return array<string, mixed>
     */
    public function getSummary(ImportJob $job): array
    {
        return [
            'id' => $job->id,
            'status' => $job->status,
            'import_type' => $job->import_type,
            'total_rows' => $job->total_rows,
            'processed_rows' => $job->processed_rows,
            'valid_rows' => $job->valid_rows,
            'invalid_rows' => $job->invalid_rows,
            'imported_rows' => $job->imported_rows,
            'skipped_rows' => $job->skipped_rows,
            'failed_rows' => $job->failed_rows,
            'datasets_count' => $job->datasets()->count(),
            'validation_errors_count' => $job->validationResults()->where('severity', 'error')->count(),
            'validation_warnings_count' => $job->validationResults()->where('severity', 'warning')->count(),
            'is_cancellable' => $job->isCancellable(),
            'is_terminal' => $job->isTerminal(),
            'started_at' => $job->started_at?->toIso8601String(),
            'completed_at' => $job->completed_at?->toIso8601String(),
            'created_at' => $job->created_at?->toIso8601String(),
        ];
    }
}
