<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class StockMovement extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'warehouse_id', 'product_id', 'product_variant_id', 'type', 'quantity', 'unit_cost', 'reason', 'reference_id'
    ];
}
