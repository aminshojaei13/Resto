<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use HasApiTokens, HasFactory, Notifiable;

    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'name', 'email', 'phone', 'password', 'role', 'is_platform_admin'
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
}
