<?php

namespace App\Models;

use App\Notifications\ResetPasswordNotification;
use Illuminate\Auth\Passwords\CanResetPassword;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Illuminate\Support\Str;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use CanResetPassword, HasApiTokens, HasFactory, Notifiable;

    public $incrementing = false;
    protected $keyType = 'string';

    public const STATUS_ACTIVE = 'ACTIVE';
    public const STATUS_BANNED = 'BANNED';

    /**
     * The account exists but the person has not yet accepted their
     * invitation and therefore has no usable password. Such an account
     * can never sign in.
     */
    public const STATUS_PENDING_INVITE = 'PENDING_INVITE';

    protected $fillable = [
        'id', 'name', 'first_name', 'last_name', 'email', 'phone', 'password',
        'role', 'status', 'is_platform_admin', 'preferred_locale',
    ];

    protected $hidden = [
        'password', 'remember_token',
    ];

    protected $casts = [
        'is_platform_admin' => 'boolean',
    ];

    public function memberships()
    {
        return $this->hasMany(OrganizationMembership::class);
    }

    public function organizations()
    {
        return $this->belongsToMany(Organization::class, 'organization_memberships');
    }

    /**
     * Single authoritative full-name presentation.
     * Prefers explicit first/last name parts; falls back to the stored
     * single name field exactly as the user entered it.
     * Never derived from email or any other field.
     */
    public function getFullNameAttribute(): string
    {
        $parts = array_values(array_filter([
            trim((string) $this->first_name),
            trim((string) $this->last_name),
        ], fn ($p) => $p !== ''));

        if ($parts !== []) {
            return implode(' ', $parts);
        }

        return trim((string) $this->name);
    }

    /**
     * Addresses are stored canonically so a person can type their own address
     * in any case and still be found — on sign-in, on password recovery and on
     * invitation.
     */
    public function setEmailAttribute($value): void
    {
        $this->attributes['email'] = Str::lower(trim((string) $value));
    }

    /**
     * The password broker and the auth provider must both find the person
     * regardless of how the address was typed.
     *
     * @param  array<string, string>  $credentials
     */
    public function retrieveByCredentials(array $credentials)
    {
        if (isset($credentials['email'])) {
            return static::query()
                ->where('email', Str::lower(trim($credentials['email'])))
                ->first();
        }

        return parent::retrieveByCredentials($credentials);
    }

    public function isActive(): bool
    {
        return ($this->status ?? self::STATUS_ACTIVE) === self::STATUS_ACTIVE;
    }

    public function isPendingInvitation(): bool
    {
        return ($this->status ?? null) === self::STATUS_PENDING_INVITE;
    }

    public function isPlatformAdmin(): bool
    {
        return (bool) $this->is_platform_admin;
    }

    /**
     * Recovery mail is localized and always points at the business web app.
     */
    public function sendPasswordResetNotification($token): void
    {
        $this->notify(new ResetPasswordNotification(
            $token,
            $this->preferred_locale ?? 'fa',
        ));
    }

    public function preferredLanguage(): string
    {
        $locale = $this->preferred_locale ?? 'fa';

        return in_array($locale, ['fa', 'en'], true) ? $locale : 'fa';
    }
}
