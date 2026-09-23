<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Store extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'name', 'code', 'address', 'phone'
    ];

    public function organization()
    {
        return $this->belongsTo(Organization::class);
    }

    public function warehouses()
    {
        return $this->hasMany(Warehouse::class);
    }
}
