<?php

namespace App\Services;

use App\Models\Account;
use App\Models\JournalEntry;
use App\Models\JournalEntryLine;
use App\Models\Order;
use App\Support\Money;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Exception;

/**
 * Account codes this system posts to.
 *
 * The chart of accounts already separates assets (1xxx), liabilities (2xxx),
 * revenue (4xxx) and expenses (5xxx); these are the specific accounts the
 * business processes use. They are provisioned on first use against the
 * organization's own chart, which is how this ledger has always worked.
 */
class AccountingService
{
    /**
     * Whether an account's balance grows on the debit side.
     *
     * Derived from the code's leading digit, which is the numbering the chart
     * of accounts already uses: 1xxx assets and 5xxx expenses are
     * debit-normal; 2xxx liabilities, 3xxx equity, 4xxx revenue are
     * credit-normal.
     */
    public static function isDebitNormal(Account $account): bool
    {
        return in_array(substr((string) $account->code, 0, 1), ['1', '5'], true);
    }

    public const ACCOUNT_CASH = '1010';
    public const ACCOUNT_INVENTORY = '1200';
    public const ACCOUNT_PAYABLE = '2010';
    public const ACCOUNT_TAX_PAYABLE = '2020';
    public const ACCOUNT_SALES_REVENUE = '4010';
    public const ACCOUNT_OPERATING_EXPENSE = '5010';

    /**
     * The standard account for a code, created against the organization's own
     * chart of accounts the first time it is needed.
     */
    public function account(string $orgId, string $code, string $name, string $chartOfAccountId): Account
    {
        return Account::firstOrCreate(
            ['organization_id' => $orgId, 'code' => $code],
            [
                'id' => (string) Str::uuid(),
                'chart_of_account_id' => $chartOfAccountId,
                'name' => $name,
                'balance' => 0,
            ]
        );
    }
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

                // Update Account balance.
                //
                // Balance direction follows the account's normal side: assets
                // and expenses grow on the debit side; liabilities, equity and
                // revenue grow on the credit side. Treating every credit as a
                // decrease made every liability — payables and sales tax —
                // report a negative balance for money the business actually
                // owes.
                $account = Account::find($line['account_id']);
                if ($account) {
                    $debitIncreases = self::isDebitNormal($account);

                    if (($line['type'] === 'DEBIT') === $debitIncreases) {
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
     *
     * Tax treatment: the customer hands over the gross amount, so cash is
     * debited for the whole total. Revenue is credited for the net amount the
     * business actually earned, and the tax the business collected is credited
     * to a tax payable liability — it was never the business's money. Booking
     * the whole total as revenue (as this previously did) overstated revenue
     * by exactly the tax the business owes.
     *
     * When tax is zero the entry stays two lines, so a tax-free business is
     * not given an empty liability posting.
     */
    public function recordSaleJournal(string $orgId, string $storeId, Order $order): JournalEntry
    {
        $cashAccount = $this->account($orgId, self::ACCOUNT_CASH, 'Cash / POS Drawer', 'coa_asset');
        $salesAccount = $this->account($orgId, self::ACCOUNT_SALES_REVENUE, 'Sales Revenue', 'coa_revenue');

        $grossMinor = Money::toMinor($order->total_amount);
        $taxMinor = Money::toMinor($order->tax_amount);
        $netMinor = $grossMinor - $taxMinor;

        $lines = [
            [
                'account_id' => $cashAccount->id,
                'type' => 'DEBIT',
                'amount' => Money::fromMinor($grossMinor),
                'description' => "Cash received for Order #{$order->order_number}"
            ],
            [
                'account_id' => $salesAccount->id,
                'type' => 'CREDIT',
                'amount' => Money::fromMinor($netMinor),
                'description' => "Sales Revenue for Order #{$order->order_number}"
            ],
        ];

        if ($taxMinor > 0) {
            $taxAccount = $this->account($orgId, self::ACCOUNT_TAX_PAYABLE, 'Sales Tax Payable', 'coa_liability');

            $lines[] = [
                'account_id' => $taxAccount->id,
                'type' => 'CREDIT',
                'amount' => Money::fromMinor($taxMinor),
                'description' => "Sales tax collected for Order #{$order->order_number}"
            ];
        }

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
