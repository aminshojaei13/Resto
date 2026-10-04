<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class OrganizationMembership extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    public const ROLE_OWNER = 'OWNER';
    public const ROLE_MANAGER = 'MANAGER';
    public const ROLE_STAFF = 'STAFF';

    public const STATUS_ACTIVE = 'ACTIVE';
    public const STATUS_DEACTIVATED = 'DEACTIVATED';

    protected $fillable = [
        'id', 'organization_id', 'user_id', 'role', 'status', 'invited_by', 'joined_at',
    ];

    protected $casts = [
        'joined_at' => 'datetime',
    ];

    public function organization()
    {
        return $this->belongsTo(Organization::class);
    }

    public function user()
    {
        return $this->belongsTo(User::class);
    }

    public function isActive(): bool
    {
        return ($this->status ?? self::STATUS_ACTIVE) === self::STATUS_ACTIVE;
    }
}
