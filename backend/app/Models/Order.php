<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Order extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'order_number', 'external_order_id', 'organization_id', 'store_id', 'order_date', 'warehouse_id',
        'customer_id', 'customer_name', 'subtotal', 'discount_amount',
        'tax_amount', 'tax_rate', 'total_amount', 'payment_method', 'payment_status',
        'fulfillment_status', 'notes', 'source', 'is_historical', 'import_job_id',
        'created_by_user_id', 'cancelled_at', 'cancelled_by_user_id'
    ];

    protected $casts = [
        'is_historical' => 'boolean',
        'order_date' => 'datetime',
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
