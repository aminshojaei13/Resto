<?php

namespace App\Models;

use App\Support\Import\ImportStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class ImportJob extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id',
        'organization_id',
        'user_id',
        'import_type',
        'status',
        'original_filename',
        'detected_format',
        'file_size',
        'total_rows',
        'processed_rows',
        'valid_rows',
        'invalid_rows',
        'imported_rows',
        'skipped_rows',
        'failed_rows',
        'error_summary',
        'idempotency_key',
        'mapping_config',
        'metadata',
        'started_at',
        'completed_at',
    ];

    protected $casts = [
        'file_size' => 'integer',
        'total_rows' => 'integer',
        'processed_rows' => 'integer',
        'valid_rows' => 'integer',
        'invalid_rows' => 'integer',
        'imported_rows' => 'integer',
        'skipped_rows' => 'integer',
        'failed_rows' => 'integer',
        'mapping_config' => 'array',
        'metadata' => 'array',
        'started_at' => 'datetime',
        'completed_at' => 'datetime',
    ];

    public function organization(): BelongsTo
    {
        return $this->belongsTo(Organization::class);
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function sources(): HasMany
    {
        return $this->hasMany(ImportSource::class);
    }

    public function datasets(): HasMany
    {
        return $this->hasMany(ImportDataset::class);
    }

    public function validationResults(): HasMany
    {
        return $this->hasMany(ImportValidationResult::class);
    }

    public function isCancellable(): bool
    {
        return ImportStatus::isCancellable($this->status);
    }

    public function isTerminal(): bool
    {
        return ImportStatus::isTerminal($this->status);
    }

    public function canTransitionTo(string $targetStatus): bool
    {
        return ImportStatus::canTransition($this->status, $targetStatus);
    }
}
