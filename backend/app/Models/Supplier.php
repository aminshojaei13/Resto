<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Supplier extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'name', 'code', 'email', 'phone', 'address', 'tax_id', 'notes'
    ];

    public function purchases()
    {
        return $this->hasMany(Purchase::class);
    }
}
