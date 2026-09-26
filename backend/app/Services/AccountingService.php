<?php

namespace App\Services;

use App\Models\Account;
use App\Models\JournalEntry;
use App\Models\JournalEntryLine;
use App\Models\Order;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Exception;

class AccountingService
{
    /**
     * Posts a double-entry journal entry enforcing Debit == Credit with exact monetary rounding.
     */
    public function postJournalEntry(
        string $orgId,
        string $storeId,
        string $description,
        array $lines, // array of ['account_id' => string, 'type' => 'DEBIT'|'CREDIT', 'amount' => float]
        ?string $refType = null,
        ?string $refId = null
    ): JournalEntry {
        $totalDebit = 0.0;
        $totalCredit = 0.0;

        foreach ($lines as $line) {
            $lineAmount = round((float) $line['amount'], 2);
            if ($line['type'] === 'DEBIT') {
                $totalDebit += $lineAmount;
            } else if ($line['type'] === 'CREDIT') {
                $totalCredit += $lineAmount;
            }
        }

        $totalDebit = round($totalDebit, 2);
        $totalCredit = round($totalCredit, 2);

        // Strict Double-Entry Check
        if (abs($totalDebit - $totalCredit) > 0.001) {
            throw new Exception("Double-entry accounting violation: Total Debits ($totalDebit) must equal Total Credits ($totalCredit)");
        }

        return DB::transaction(function () use ($orgId, $storeId, $description, $lines, $refType, $refId, $totalDebit, $totalCredit) {
            $entryNumber = 'JE-' . date('Ymd') . '-' . rand(1000, 9999);

            $entry = JournalEntry::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $orgId,
                'store_id' => $storeId,
                'entry_number' => $entryNumber,
                'reference_type' => $refType,
                'reference_id' => $refId,
                'description' => $description,
                'total_debit' => $totalDebit,
                'total_credit' => $totalCredit,
                'is_posted' => true,
            ]);

            foreach ($lines as $line) {
                $lineAmount = round((float) $line['amount'], 2);

                JournalEntryLine::create([
                    'id' => (string) Str::uuid(),
                    'journal_entry_id' => $entry->id,
                    'account_id' => $line['account_id'],
                    'type' => $line['type'],
                    'amount' => $lineAmount,
                    'description' => $line['description'] ?? $description,
                ]);

                // Update Account balance
                $account = Account::find($line['account_id']);
                if ($account) {
                    if ($line['type'] === 'DEBIT') {
                        $account->balance = round($account->balance + $lineAmount, 2);
                    } else {
                        $account->balance = round($account->balance - $lineAmount, 2);
                    }
                    $account->save();
                }
            }

            return $entry;
        });
    }

    /**
     * Automatic journal entry for a completed Sales Order.
     */
    public function recordSaleJournal(string $orgId, string $storeId, Order $order): JournalEntry
    {
        // Find or create standard Chart of Accounts for Cash/Bank and Sales Revenue
        $cashAccount = Account::firstOrCreate(
            ['organization_id' => $orgId, 'code' => '1010'],
            ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_asset', 'name' => 'Cash / POS Drawer', 'balance' => 0]
        );

        $salesAccount = Account::firstOrCreate(
            ['organization_id' => $orgId, 'code' => '4010'],
            ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_revenue', 'name' => 'Sales Revenue', 'balance' => 0]
        );

        $lines = [
            [
                'account_id' => $cashAccount->id,
                'type' => 'DEBIT',
                'amount' => round($order->total_amount, 2),
                'description' => "Cash received for Order #{$order->order_number}"
            ],
            [
                'account_id' => $salesAccount->id,
                'type' => 'CREDIT',
                'amount' => round($order->total_amount, 2),
                'description' => "Sales Revenue for Order #{$order->order_number}"
            ]
        ];

        return $this->postJournalEntry(
            $orgId,
            $storeId,
            "Sales Order #{$order->order_number} revenue entry",
            $lines,
            'SalesOrder',
            $order->id
        );
    }
}
