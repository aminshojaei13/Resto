<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WarehouseStock extends Model
{
    protected $table = 'warehouse_stock';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'warehouse_id', 'product_id', 'product_variant_id', 'quantity', 'reserved_quantity'
    ];

    public function product()
    {
        return $this->belongsTo(Product::class);
    }

    public function warehouse()
    {
        return $this->belongsTo(Warehouse::class);
    }
}
