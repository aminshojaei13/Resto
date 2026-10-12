<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ImportValidationResult extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id',
        'import_job_id',
        'organization_id',
        'source_file_id',
        'worksheet_name',
        'row_number',
        'field_name',
        'original_value',
        'error_code',
        'message',
        'suggested_action',
        'severity',
        'is_eligible',
    ];

    protected $casts = [
        'row_number' => 'integer',
        'is_eligible' => 'boolean',
    ];

    public function job(): BelongsTo
    {
        return $this->belongsTo(ImportJob::class, 'import_job_id');
    }

    public function organization(): BelongsTo
    {
        return $this->belongsTo(Organization::class);
    }
}
