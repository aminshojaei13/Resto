<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Warehouse extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'store_id', 'organization_id', 'name', 'code', 'address'
    ];

    public function store()
    {
        return $this->belongsTo(Store::class);
    }
}
