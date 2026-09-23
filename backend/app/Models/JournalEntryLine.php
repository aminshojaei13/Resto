<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class JournalEntryLine extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'journal_entry_id', 'account_id', 'type', 'amount', 'description'
    ];

    public function account()
    {
        return $this->belongsTo(Account::class);
    }
}
