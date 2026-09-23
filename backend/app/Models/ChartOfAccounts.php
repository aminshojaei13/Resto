<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ChartOfAccounts extends Model
{
    protected $table = 'chart_of_accounts';
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'code', 'name', 'type'
    ];
}
