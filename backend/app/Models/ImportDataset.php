<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ImportDataset extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id',
        'import_job_id',
        'import_source_id',
        'organization_id',
        'name',
        'sheet_index',
        'row_count',
        'headers',
        'sample_rows',
        'status',
        'metadata',
    ];

    protected $casts = [
        'sheet_index' => 'integer',
        'row_count' => 'integer',
        'headers' => 'array',
        'sample_rows' => 'array',
        'metadata' => 'array',
    ];

    public function job(): BelongsTo
    {
        return $this->belongsTo(ImportJob::class, 'import_job_id');
    }

    public function source(): BelongsTo
    {
        return $this->belongsTo(ImportSource::class, 'import_source_id');
    }

    public function organization(): BelongsTo
    {
        return $this->belongsTo(Organization::class);
    }
}
