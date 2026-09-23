<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class OrganizationMembership extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'user_id', 'role'
    ];
}
