<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Expense extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'store_id', 'category', 'amount', 'payment_method', 'date', 'notes', 'user_id'
    ];
}
