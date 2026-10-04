<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ImportedMessage extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'source', 'raw_text', 'status', 'customer_id', 'order_id'
    ];

    public function customer()
    {
        return $this->belongsTo(Customer::class);
    }

    public function order()
    {
        return $this->belongsTo(Order::class);
    }
}
