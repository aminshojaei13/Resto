<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class OrderItem extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'order_id', 'product_id', 'product_variant_id', 'product_name',
        'sku', 'unit_price', 'quantity', 'discount_percent', 'tax_amount', 'total_price'
    ];

    public function order()
    {
        return $this->belongsTo(Order::class);
    }
}
