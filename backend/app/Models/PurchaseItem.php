<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class PurchaseItem extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'purchase_id', 'product_id', 'unit', 'quantity', 'received_quantity', 'unit_cost', 'total_cost'
    ];

    public function product()
    {
        return $this->belongsTo(Product::class);
    }
}
