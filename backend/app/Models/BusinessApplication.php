<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class BusinessApplication extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id',
        'business_name',
        'owner_name',
        'email',
        'phone',
        'business_type',
        'country',
        'city',
        'address',
        'notes',
        'status',
        'rejection_reason',
        'organization_id',
        'reviewed_by',
        'reviewed_at',
    ];

    protected $casts = [
        'reviewed_at' => 'datetime',
    ];

    public function organization()
    {
        return $this->belongsTo(Organization::class);
    }

    public function reviewer()
    {
        return $this->belongsTo(User::class, 'reviewed_by');
    }
}
