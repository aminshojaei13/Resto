<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Product extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'sku', 'barcode', 'name', 'description', 'price', 'cost_price', 'category', 'unit', 'image_url'
    ];

    public function variants()
    {
        return $this->hasMany(ProductVariant::class);
    }

    public function stock()
    {
        return $this->hasMany(WarehouseStock::class);
    }
}
