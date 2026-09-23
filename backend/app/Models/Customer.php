<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Customer extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'name', 'email', 'phone', 'address', 'total_purchases', 'loyalty_points'
    ];

    public function orders()
    {
        return $this->hasMany(Order::class);
    }
}
