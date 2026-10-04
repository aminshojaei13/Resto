<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Expense;
use App\Services\AccountingService;
use App\Support\MembershipContext;
use App\Support\Money;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * Operating expenses — money the business spends running itself.
 *
 * This is deliberately *not* the place where goods bought from a supplier are
 * recorded. That is a purchase order: it creates inventory and a payable.
 * An expense here creates neither stock nor inventory; it is a cost against
 * the period (rent, internet, electricity, salaries, transport, advertising,
 * repairs, services, fees, other).
 *
 * Every entry posts through the shared AccountingService, so an expense can
 * never sit in a screen without appearing in the ledger.
 */
class ExpenseController extends Controller
{
    public function __construct(private AccountingService $accountingService)
    {
    }

    /**
     * The category vocabulary the UI offers, with both languages.
     */
    public function categories(Request $request)
    {
        $locale = $request->query('locale') === 'en' ? 'en' : 'fa';

        return response()->json(array_map(
            fn (array $category) => $category + [
                'label' => Expense::categoryLabel($category['code'], $locale),
            ],
            Expense::CATEGORIES
        ));
    }

    public function index(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $query = trim((string) $request->query('q', ''));
        $category = $request->query('category');

        $builder = Expense::where('organization_id', $organizationId);

        if ($query !== '') {
            $builder->where(function ($sub) use ($query) {
                $sub->where('title', 'like', "%{$query}%")
                    ->orWhere('notes', 'like', "%{$query}%");
            });
        }

        if (is_string($category) && $category !== '' && in_array($category, Expense::categoryCodes(), true)) {
            $builder->where('category', $category);
        }

        return response()->json($builder->orderByDesc('date')->orderByDesc('created_at')->get());
    }

    /**
     * The expense dashboard: this month, today, by category, and the trend.
     * Every figure is computed from real rows.
     */
    public function summary(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $now = Carbon::now();
        $startOfMonth = $now->copy()->startOfMonth();
        $today = $now->copy()->startOfDay();

        $monthTotal = (float) Expense::where('organization_id', $organizationId)
            ->where('date', '>=', $startOfMonth->toDateString())
            ->sum('amount');

        $todayTotal = (float) Expense::where('organization_id', $organizationId)
            ->whereDate('date', $today->toDateString())
            ->sum('amount');

        $previousMonthTotal = (float) Expense::where('organization_id', $organizationId)
            ->whereBetween('date', [
                $startOfMonth->copy()->subMonth()->toDateString(),
                $startOfMonth->copy()->subDay()->toDateString(),
            ])
            ->sum('amount');

        $byCategory = Expense::where('organization_id', $organizationId)
            ->where('date', '>=', $startOfMonth->toDateString())
            ->selectRaw('category, SUM(amount) as total, COUNT(*) as entries')
            ->groupBy('category')
            ->get()
            ->map(fn ($row) => [
                'category' => $row->category,
                'label' => Expense::categoryLabel($row->category),
                'total' => (float) $row->total,
                'entries' => (int) $row->entries,
            ])
            ->sortByDesc('total')
            ->values();

        // Six months of trend, oldest first, including months with no expense
        // so the shape of spending is not quietly distorted.
        $trend = [];

        for ($i = 5; $i >= 0; $i--) {
            $month = $startOfMonth->copy()->subMonths($i);

            $trend[] = [
                'month' => $month->format('Y-m'),
                'label' => $month->translatedFormat('M'),
                'total' => (float) Expense::where('organization_id', $organizationId)
                    ->whereBetween('date', [
                        $month->copy()->startOfMonth()->toDateString(),
                        $month->copy()->endOfMonth()->toDateString(),
                    ])
                    ->sum('amount'),
            ];
        }

        return response()->json([
            'month_total' => $monthTotal,
            'today_total' => $todayTotal,
            'previous_month_total' => $previousMonthTotal,
            'by_category' => $byCategory,
            'trend' => $trend,
        ]);
    }

    public function show(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $expense = Expense::where('organization_id', $organizationId)->findOrFail($id);

        $expense->category_label = Expense::categoryLabel($expense->category);

        return response()->json($expense);
    }

    public function store(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $storeId = MembershipContext::activeStoreId($request, $organizationId);

        $validated = $request->validate([
            'store_id' => 'nullable|string',
            'title' => 'required|string|max:191',
            'category' => 'required|string|in:' . implode(',', Expense::categoryCodes()),
            'amount' => 'required|numeric|min:0.01',
            'date' => 'required|date',
            'payment_method' => 'nullable|string|max:40',
            'notes' => 'nullable|string|max:2000',
            'attachment_url' => 'nullable|string|max:500',
            'is_recurring' => 'sometimes|boolean',
            'recurrence_period' => 'nullable|in:DAILY,WEEKLY,MONTHLY,YEARLY',
            'recurrence_starts_on' => 'nullable|date',
            'recurrence_ends_on' => 'nullable|date|after_or_equal:recurrence_starts_on',
        ]);

        $storeId = $validated['store_id'] ?? $storeId;

        if (!$storeId) {
            throw ValidationException::withMessages([
                'store_id' => ['برای ثبت هزینه باید فروشگاه مشخص باشد.'],
            ]);
        }

        $amount = Money::fromMinor(Money::toMinor($validated['amount']));

        if ($amount <= 0) {
            throw ValidationException::withMessages([
                'amount' => ['مبلغ هزینه باید بزرگ‌تر از صفر باشد.'],
            ]);
        }

        return DB::transaction(function () use ($validated, $organizationId, $storeId, $amount, $request) {
            $expense = Expense::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organizationId,
                'store_id' => $storeId,
                'title' => $validated['title'],
                'category' => $validated['category'],
                'amount' => $amount,
                'payment_method' => $validated['payment_method'] ?? 'CASH',
                'date' => $validated['date'],
                'notes' => $validated['notes'] ?? '',
                'attachment_url' => $validated['attachment_url'] ?? null,
                'is_recurring' => (bool) ($validated['is_recurring'] ?? false),
                'recurrence_period' => $validated['recurrence_period'] ?? null,
                'recurrence_starts_on' => $validated['recurrence_starts_on'] ?? null,
                'recurrence_ends_on' => $validated['recurrence_ends_on'] ?? null,
                'user_id' => $request->user()?->id,
            ]);

            $this->postExpenseEntry($expense);

            return response()->json($expense->fresh(), 201);
        });
    }

    /**
     * Correcting a posted expense reverses the old entry and posts the
     * corrected one, so the ledger always matches what the screen shows.
     */
    public function update(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $expense = Expense::where('organization_id', $organizationId)->findOrFail($id);

        $validated = $request->validate([
            'title' => 'sometimes|required|string|max:191',
            'category' => 'sometimes|required|string|in:' . implode(',', Expense::categoryCodes()),
            'amount' => 'sometimes|required|numeric|min:0.01',
            'date' => 'sometimes|required|date',
            'payment_method' => 'sometimes|nullable|string|max:40',
            'notes' => 'sometimes|nullable|string|max:2000',
        ]);

        return DB::transaction(function () use ($expense, $validated) {
            // Captured before the model is touched: the reversal has to undo
            // what was actually posted, not the correction.
            $posted = [
                'amount' => (float) $expense->amount,
                'category' => $expense->category,
                'title' => $expense->title,
            ];

            $expense->fill($validated);

            if (isset($validated['amount'])) {
                $expense->amount = Money::fromMinor(Money::toMinor($validated['amount']));
            }

            $expense->save();

            $changed = Money::toMinor($posted['amount']) !== Money::toMinor($expense->amount)
                || $posted['category'] !== $expense->category
                || $posted['title'] !== $expense->title;

            if ($changed) {
                $this->reverseExpenseEntry($expense, $posted);
                $this->postExpenseEntry($expense);
            }

            return response()->json($expense->fresh());
        });
    }

    /**
     * Remove an expense by reversing its accounting entry. The entry is not
     * silently deleted: the reversal keeps the trail intact.
     */
    public function destroy(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $expense = Expense::where('organization_id', $organizationId)->findOrFail($id);

        DB::transaction(function () use ($expense) {
            $this->reverseExpenseEntry($expense, [
                'amount' => (float) $expense->amount,
                'category' => $expense->category,
                'title' => $expense->title,
            ]);

            $expense->delete();
        });

        return response()->json(['message' => 'هزینه حذف شد و سند حسابداری آن برگشت خورد.']);
    }

    /**
     * Dr Operating expense, Cr Cash — the standard treatment for a paid cost.
     */
    private function postExpenseEntry(Expense $expense): void
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

        $this->accountingService->postJournalEntry(
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

    private function reverseExpenseEntry(Expense $expense, array $posted): void
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

        $amount = (float) $posted['amount'];

        if ($amount <= 0) {
            return;
        }

        $this->accountingService->postJournalEntry(
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
}