<?php

namespace App\Services;

use App\Models\Expense;
use App\Models\JournalEntry;
use App\Models\User;
use App\Support\Money;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use InvalidArgumentException;

/**
 * Canonical operating-expense management service.
 *
 * Encapsulates transactional creation, updating, reversal, and accounting journal
 * postings for business operating expenses. Ensures 100% parity between the database,
 * user-facing reports, and double-entry general ledger.
 */
class ExpenseService
{
    public function __construct(private AccountingService $accountingService)
    {
    }

    /**
     * Create and record a new expense and post its double-entry journal entry.
     *
     * @param array<string, mixed> $data
     * @param User|null $user
     * @return Expense
     */
    public function createExpense(array $data, ?User $user = null): Expense
    {
        $organizationId = $data['organization_id'] ?? null;
        $storeId = $data['store_id'] ?? null;
        $title = trim((string) ($data['title'] ?? ''));
        $category = $data['category'] ?? null;
        $date = $data['date'] ?? null;
        $rawAmount = $data['amount'] ?? null;

        if (!$organizationId) {
            throw new InvalidArgumentException('شناسه سازمان برای ثبت هزینه الزامی است.');
        }

        if (!$storeId) {
            throw new InvalidArgumentException('برای ثبت هزینه باید فروشگاه مشخص باشد.');
        }

        if ($title === '') {
            throw new InvalidArgumentException('عنوان هزینه الزامی است.');
        }

        if (!$category || !in_array($category, Expense::categoryCodes(), true)) {
            throw new InvalidArgumentException('دسته‌بندی هزینه نامعتبر است.');
        }

        if (!$date) {
            throw new InvalidArgumentException('تاریخ هزینه الزامی است.');
        }

        $amount = Money::fromMinor(Money::toMinor($rawAmount));
        if ($amount <= 0) {
            throw new InvalidArgumentException('مبلغ هزینه باید بزرگ‌تر از صفر باشد.');
        }

        $id = $data['id'] ?? (string) Str::uuid();

        return DB::transaction(function () use ($id, $organizationId, $storeId, $title, $category, $amount, $date, $data, $user) {
            $expense = Expense::create([
                'id' => $id,
                'organization_id' => $organizationId,
                'store_id' => $storeId,
                'title' => $title,
                'category' => $category,
                'amount' => $amount,
                'payment_method' => $data['payment_method'] ?? 'CASH',
                'date' => $date,
                'notes' => $data['notes'] ?? '',
                'attachment_url' => $data['attachment_url'] ?? null,
                'is_recurring' => (bool) ($data['is_recurring'] ?? false),
                'recurrence_period' => $data['recurrence_period'] ?? null,
                'recurrence_starts_on' => $data['recurrence_starts_on'] ?? null,
                'recurrence_ends_on' => $data['recurrence_ends_on'] ?? null,
                'user_id' => $user?->id ?? ($data['user_id'] ?? null),
            ]);

            $this->postExpenseEntry($expense);

            return $expense->fresh();
        });
    }

    /**
     * Update an expense, reversing the existing ledger entry if financial attributes changed.
     *
     * @param Expense $expense
     * @param array<string, mixed> $data
     * @return Expense
     */
    public function updateExpense(Expense $expense, array $data): Expense
    {
        return DB::transaction(function () use ($expense, $data) {
            $posted = [
                'amount' => (float) $expense->amount,
                'category' => $expense->category,
                'title' => $expense->title,
            ];

            $expense->fill($data);

            if (isset($data['amount'])) {
                $expense->amount = Money::fromMinor(Money::toMinor($data['amount']));
            }

            $expense->save();

            $changed = Money::toMinor($posted['amount']) !== Money::toMinor($expense->amount)
                || $posted['category'] !== $expense->category
                || $posted['title'] !== $expense->title;

            if ($changed) {
                $this->reverseExpenseEntry($expense, $posted);
                $this->postExpenseEntry($expense);
            }

            return $expense->fresh();
        });
    }

    /**
     * Delete an expense and reverse its double-entry ledger entry.
     *
     * @param Expense $expense
     */
    public function deleteExpense(Expense $expense): void
    {
        DB::transaction(function () use ($expense) {
            $this->reverseExpenseEntry($expense, [
                'amount' => (float) $expense->amount,
                'category' => $expense->category,
                'title' => $expense->title,
            ]);

            $expense->delete();
        });
    }

    /**
     * Dr Operating expense (5010), Cr Cash (1010) — standard double-entry treatment.
     */
    public function postExpenseEntry(Expense $expense): JournalEntry
    {
        $expenseAccount = $this->accountingService->account(
            $expense->organization_id,
            AccountingService::ACCOUNT_OPERATING_EXPENSE,
            'Operating Expense',
            'coa_expense'
        );

        $cashAccount = $this->accountingService->account(
            $expense->organization_id,
            AccountingService::ACCOUNT_CASH,
            'Cash / POS Drawer',
            'coa_asset'
        );

        return $this->accountingService->postJournalEntry(
            $expense->organization_id,
            $expense->store_id,
            "هزینه جاری: {$expense->title}",
            [
                [
                    'account_id' => $expenseAccount->id,
                    'type' => 'DEBIT',
                    'amount' => $expense->amount,
                    'description' => Expense::categoryLabel($expense->category) . " — {$expense->title}",
                ],
                [
                    'account_id' => $cashAccount->id,
                    'type' => 'CREDIT',
                    'amount' => $expense->amount,
                    'description' => "پرداخت هزینه «{$expense->title}»",
                ],
            ],
            'Expense',
            $expense->id
        );
    }

    /**
     * Reverse a previously posted expense ledger entry (Cr Operating expense, Dr Cash).
     *
     * @param Expense $expense
     * @param array{amount: float, category: string, title: string} $posted
     * @return JournalEntry|null
     */
    public function reverseExpenseEntry(Expense $expense, array $posted): ?JournalEntry
    {
        $amount = (float) $posted['amount'];
        if ($amount <= 0) {
            return null;
        }

        $expenseAccount = $this->accountingService->account(
            $expense->organization_id,
            AccountingService::ACCOUNT_OPERATING_EXPENSE,
            'Operating Expense',
            'coa_expense'
        );

        $cashAccount = $this->accountingService->account(
            $expense->organization_id,
            AccountingService::ACCOUNT_CASH,
            'Cash / POS Drawer',
            'coa_asset'
        );

        return $this->accountingService->postJournalEntry(
            $expense->organization_id,
            $expense->store_id,
            "برگشت هزینه جاری: {$posted['title']}",
            [
                [
                    'account_id' => $cashAccount->id,
                    'type' => 'DEBIT',
                    'amount' => $amount,
                    'description' => "برگشت پرداخت هزینه «{$posted['title']}»",
                ],
                [
                    'account_id' => $expenseAccount->id,
                    'type' => 'CREDIT',
                    'amount' => $amount,
                    'description' => "برگشت هزینه ثبت‌شده «{$posted['title']}»",
                ],
            ],
            'ExpenseReversal',
            $expense->id
        );
    }

    /**
     * Generate deterministic RFC 4122 v4 UUID from seed string for per-row import idempotency.
     */
    public static function deterministicUuid(string $seed): string
    {
        $hash = md5($seed);
        return sprintf(
            '%08s-%04s-4%03s-%04x-%012s',
            substr($hash, 0, 8),
            substr($hash, 8, 4),
            substr($hash, 12, 3),
            hexdec(substr($hash, 15, 4)) & 0x3fff | 0x8000,
            substr($hash, 19, 12)
        );
    }
}
