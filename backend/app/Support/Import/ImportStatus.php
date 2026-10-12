<?php

namespace App\Support\Import;

class ImportStatus
{
    public const UPLOADED = 'uploaded';
    public const INSPECTING = 'inspecting';
    public const MAPPING_REQUIRED = 'mapping_required';
    public const READY_TO_VALIDATE = 'ready_to_validate';
    public const VALIDATING = 'validating';
    public const PREVIEW_READY = 'preview_ready';
    public const READY_TO_IMPORT = 'ready_to_import';
    public const IMPORTING = 'importing';
    public const COMPLETED = 'completed';
    public const COMPLETED_WITH_ERRORS = 'completed_with_errors';
    public const FAILED = 'failed';
    public const CANCELLED = 'cancelled';

    public const ALL = [
        self::UPLOADED,
        self::INSPECTING,
        self::MAPPING_REQUIRED,
        self::READY_TO_VALIDATE,
        self::VALIDATING,
        self::PREVIEW_READY,
        self::READY_TO_IMPORT,
        self::IMPORTING,
        self::COMPLETED,
        self::COMPLETED_WITH_ERRORS,
        self::FAILED,
        self::CANCELLED,
    ];

    public const CANCELLABLE_STATES = [
        self::UPLOADED,
        self::INSPECTING,
        self::MAPPING_REQUIRED,
        self::READY_TO_VALIDATE,
        self::VALIDATING,
        self::PREVIEW_READY,
        self::READY_TO_IMPORT,
    ];

    public const TERMINAL_STATES = [
        self::COMPLETED,
        self::COMPLETED_WITH_ERRORS,
        self::CANCELLED,
    ];

    /**
     * Map of valid state transitions.
     *
     * @var array<string, string[]>
     */
    public const ALLOWED_TRANSITIONS = [
        self::UPLOADED => [self::INSPECTING, self::CANCELLED, self::FAILED],
        self::INSPECTING => [self::MAPPING_REQUIRED, self::READY_TO_VALIDATE, self::FAILED, self::CANCELLED],
        self::MAPPING_REQUIRED => [self::READY_TO_VALIDATE, self::FAILED, self::CANCELLED],
        self::READY_TO_VALIDATE => [self::VALIDATING, self::FAILED, self::CANCELLED],
        self::VALIDATING => [self::PREVIEW_READY, self::FAILED, self::CANCELLED],
        self::PREVIEW_READY => [self::READY_TO_IMPORT, self::MAPPING_REQUIRED, self::READY_TO_VALIDATE, self::FAILED, self::CANCELLED],
        self::READY_TO_IMPORT => [self::IMPORTING, self::FAILED, self::CANCELLED],
        self::IMPORTING => [self::COMPLETED, self::COMPLETED_WITH_ERRORS, self::FAILED],
        self::FAILED => [self::UPLOADED, self::INSPECTING, self::READY_TO_VALIDATE],
        self::COMPLETED => [],
        self::COMPLETED_WITH_ERRORS => [],
        self::CANCELLED => [],
    ];

    public static function canTransition(string $from, string $to): bool
    {
        $allowed = self::ALLOWED_TRANSITIONS[$from] ?? [];
        return in_array($to, $allowed, true);
    }

    public static function isCancellable(string $status): bool
    {
        return in_array($status, self::CANCELLABLE_STATES, true);
    }

    public static function isTerminal(string $status): bool
    {
        return in_array($status, self::TERMINAL_STATES, true);
    }
}
