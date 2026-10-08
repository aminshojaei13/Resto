<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * A pending invitation for a person to join a business.
 *
 * The raw token only ever exists in the invitation link that was emailed.
 * What is stored is a SHA-256 hash, so a database leak cannot be replayed.
 */
class StaffInvitation extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    public const STATUS_PENDING = 'PENDING';
    public const STATUS_ACCEPTED = 'ACCEPTED';
    public const STATUS_REVOKED = 'REVOKED';
    public const STATUS_EXPIRED = 'EXPIRED';

    public const VALID_FOR_HOURS = 72;

    protected $fillable = [
        'id', 'organization_id', 'email', 'first_name', 'last_name',
        'role', 'token', 'invited_by', 'status', 'expires_at', 'accepted_at',
    ];

    protected $hidden = [
        'token',
    ];

    protected $casts = [
        'expires_at' => 'datetime',
        'accepted_at' => 'datetime',
    ];

    public function organization()
    {
        return $this->belongsTo(Organization::class);
    }

    public function inviter()
    {
        return $this->belongsTo(User::class, 'invited_by');
    }

    public function isPending(): bool
    {
        return $this->status === self::STATUS_PENDING && !$this->isExpired();
    }

    public function isExpired(): bool
    {
        return $this->expires_at !== null && $this->expires_at->isPast();
    }

    /** Public preview of an invitation — never exposes the token hash. */
    public function toPreviewArray(): array
    {
        return [
            'email' => $this->email,
            'first_name' => $this->first_name,
            'last_name' => $this->last_name,
            'role' => \App\Support\RolePermission::canonical($this->role),
            'organization_name' => $this->organization?->name,
            'expires_at' => optional($this->expires_at)->toIso8601String(),
            'status' => $this->isExpired() && $this->status === self::STATUS_PENDING
                ? self::STATUS_EXPIRED
                : $this->status,
        ];
    }

    public static function expiryFromNow(): Carbon
    {
        return now()->addHours(self::VALID_FOR_HOURS);
    }
}
