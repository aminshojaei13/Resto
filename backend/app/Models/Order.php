<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Order extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'order_number', 'organization_id', 'store_id', 'warehouse_id',
        'customer_id', 'customer_name', 'subtotal', 'discount_amount',
        'tax_amount', 'tax_rate', 'total_amount', 'payment_method', 'payment_status',
        'fulfillment_status', 'notes', 'source', 'created_by_user_id',
        'cancelled_at', 'cancelled_by_user_id'
    ];

    public function items()
    {
        return $this->hasMany(OrderItem::class);
    }

    public function payments()
    {
        return $this->hasMany(Payment::class);
    }

    public function customer()
    {
        return $this->belongsTo(Customer::class);
    }
}
