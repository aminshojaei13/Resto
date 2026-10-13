<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Purchase extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'store_id', 'warehouse_id', 'supplier_id', 'purchase_number',
        'external_purchase_id', 'purchase_date', 'subtotal', 'discount_amount', 'tax_amount',
        'total_amount', 'status', 'payment_status', 'notes', 'source', 'is_historical',
        'import_job_id', 'received_at', 'last_received_at'
    ];

    protected $casts = [
        'is_historical' => 'boolean',
    ];

    public function supplier()
    {
        return $this->belongsTo(Supplier::class);
    }

    public function items()
    {
        return $this->hasMany(PurchaseItem::class);
    }

    public function payments()
    {
        return $this->hasMany(SupplierPayment::class);
    }
}
