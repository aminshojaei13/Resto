<?php

namespace App\Jobs;

use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Services\Import\ImportStateMachine;
use App\Services\Import\SpreadsheetInspectorService;
use App\Support\Import\ImportStatus;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Throwable;

class InspectImportJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public int $tries = 3;
    public int $timeout = 120;

    public function __construct(
        public string $importJobId
    ) {
    }

    public function handle(
        SpreadsheetInspectorService $inspector,
        ImportStateMachine $stateMachine
    ): void {
        /** @var ImportJob|null $job */
        $job = ImportJob::find($this->importJobId);
        if (!$job) {
            Log::warning("InspectImportJob: Import job {$this->importJobId} not found.");
            return;
        }

        // Only inspect if in 'uploaded' state
        if ($job->status !== ImportStatus::UPLOADED) {
            Log::info("InspectImportJob: Job {$this->importJobId} status is {$job->status}, skipping inspection.");
            return;
        }

        // Transition to inspecting
        $stateMachine->transition($job, ImportStatus::INSPECTING, 'Inspection started');

        try {
            /** @var ImportSource|null $source */
            $source = $job->sources()->first();
            if (!$source) {
                throw new \RuntimeException('No source file record found for import job.');
            }

            $storageDisk = Storage::disk('local');
            $absolutePath = $storageDisk->path($source->storage_path);

            $datasetsInfo = $inspector->inspect($absolutePath, $source->file_format);

            DB::transaction(function () use ($job, $source, $datasetsInfo) {
                $totalRows = 0;

                foreach ($datasetsInfo as $dataset) {
                    ImportDataset::create([
                        'id' => (string) Str::uuid(),
                        'import_job_id' => $job->id,
                        'import_source_id' => $source->id,
                        'organization_id' => $job->organization_id,
                        'name' => $dataset['name'],
                        'sheet_index' => $dataset['sheet_index'],
                        'row_count' => $dataset['row_count'],
                        'headers' => $dataset['headers'],
                        'sample_rows' => $dataset['sample_rows'],
                        'status' => 'inspected',
                        'metadata' => $dataset['metadata'] ?? [],
                    ]);

                    $totalRows += $dataset['row_count'];
                }

                $job->update([
                    'total_rows' => $totalRows,
                ]);
            });

            // Transition to mapping_required
            $stateMachine->transition(
                $job,
                ImportStatus::MAPPING_REQUIRED,
                'File inspection completed successfully.'
            );

        } catch (Throwable $e) {
            Log::error("InspectImportJob failed for job {$job->id}: {$e->getMessage()}");

            $safeError = 'خطا در بررسی ساختار فایل: ' . $e->getMessage();

            $stateMachine->transition(
                $job,
                ImportStatus::FAILED,
                $safeError
            );
        }
    }
}
