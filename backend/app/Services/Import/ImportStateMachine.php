<?php

namespace App\Services\Import;

use App\Models\ImportJob;
use App\Support\Import\ImportStatus;
use InvalidArgumentException;

class ImportStateMachine
{
    /**
     * Transition import job to a new status with validation and history tracking.
     */
    public function transition(ImportJob $job, string $targetStatus, ?string $reason = null): ImportJob
    {
        $currentStatus = $job->status;

        if ($currentStatus === $targetStatus) {
            return $job;
        }

        if (!$job->canTransitionTo($targetStatus)) {
            throw new InvalidArgumentException(
                "تغییر وضعیت از {$currentStatus} به {$targetStatus} مجاز نیست."
            );
        }

        $metadata = $job->metadata ?? [];
        $history = $metadata['transition_history'] ?? [];

        $history[] = [
            'from' => $currentStatus,
            'to' => $targetStatus,
            'reason' => $reason,
            'timestamp' => now()->toIso8601String(),
        ];

        $metadata['transition_history'] = $history;
        $job->metadata = $metadata;
        $job->status = $targetStatus;

        if (in_array($targetStatus, [ImportStatus::INSPECTING, ImportStatus::VALIDATING, ImportStatus::IMPORTING], true)) {
            if (!$job->started_at) {
                $job->started_at = now();
            }
        }

        if ($job->isTerminal() || $targetStatus === ImportStatus::FAILED) {
            $job->completed_at = now();
            if ($targetStatus === ImportStatus::FAILED && $reason) {
                $job->error_summary = $reason;
            }
        }

        $job->save();

        return $job;
    }
}
