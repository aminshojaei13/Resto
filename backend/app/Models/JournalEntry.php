<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class JournalEntry extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'store_id', 'entry_number', 'journal_id',
        'reference_type', 'reference_id', 'description', 'total_debit', 'total_credit', 'is_posted'
    ];

    public function lines()
    {
        return $this->hasMany(JournalEntryLine::class);
    }
}
