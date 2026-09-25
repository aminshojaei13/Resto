<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Supplier extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'name', 'email', 'phone', 'address'
    ];

    public function purchases()
    {
        return $this->hasMany(Purchase::class);
    }
}
