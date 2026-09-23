<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ProductVariant extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'product_id', 'sku', 'barcode', 'name', 'price', 'cost_price'
    ];

    public function product()
    {
        return $this->belongsTo(Product::class);
    }
}
