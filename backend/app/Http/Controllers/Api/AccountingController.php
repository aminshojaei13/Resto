<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Account;
use App\Models\Expense;
use App\Models\JournalEntry;
use App\Models\Order;
use App\Services\AccountingService;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class AccountingController extends Controller
{
    protected AccountingService $accountingService;

    public function __construct(AccountingService $accountingService)
    {
        $this->accountingService = $accountingService;
    }

    public function accounts(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';
        $accounts = Account::where('organization_id', $orgId)->get();
        return response()->json($accounts);
    }

    public function createAccount(Request $request)
    {
        $orgId = $request->get('org_id') ?? $request->input('org_id') ?? 'org_apex';

        $request->validate([
            'chart_of_account_id' => 'required|string',
            'code' => 'required|string',
            'name' => 'required|string',
        ]);

        $account = Account::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $orgId,
            'chart_of_account_id' => $request->chart_of_account_id,
            'code' => $request->code,
            'name' => $request->name,
            'balance' => $request->initial_balance ?? 0.0,
        ]);

        return response()->json($account, 201);
    }

    public function journal(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';
        $entries = JournalEntry::where('organization_id', $orgId)
            ->with('lines.account')
            ->orderBy('created_at', 'desc')
            ->get();

        return response()->json($entries);
    }

    public function postEntry(Request $request)
    {
        $orgId = $request->get('org_id') ?? $request->input('org_id') ?? 'org_apex';

        $request->validate([
            'store_id' => 'required',
            'description' => 'required',
            'lines' => 'required|array|min:2',
        ]);

        $entry = $this->accountingService->postJournalEntry(
            $orgId,
            $request->store_id,
            $request->description,
            $request->lines,
            $request->reference_type,
            $request->reference_id
        );

        return response()->json($entry, 201);
    }

    public function summary(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';

        $totalRevenue = Order::where('organization_id', $orgId)->sum('total_amount');
        $todayRevenue = Order::where('organization_id', $orgId)
            ->whereDate('created_at', date('Y-m-d'))
            ->sum('total_amount');

        $totalExpenses = Expense::where('organization_id', $orgId)->sum('amount');

        $totalSalesCount = Order::where('organization_id', $orgId)->count();
        $todaySalesCount = Order::where('organization_id', $orgId)
            ->whereDate('created_at', date('Y-m-d'))
            ->count();

        $accounts = Account::where('organization_id', $orgId)->get();

        return response()->json([
            'total_revenue' => (float) $totalRevenue,
            'today_revenue' => (float) $todayRevenue,
            'total_expenses' => (float) $totalExpenses,
            'net_profit' => (float) ($totalRevenue - $totalExpenses),
            'total_sales_count' => $totalSalesCount,
            'today_sales_count' => $todaySalesCount,
            'accounts' => $accounts,
        ]);
    }

    public function profitAndLoss(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';

        $totalRevenue = Order::where('organization_id', $orgId)->where('payment_status', 'PAID')->sum('total_amount');
        $totalExpenses = Expense::where('organization_id', $orgId)->sum('amount');
        $netResult = $totalRevenue - $totalExpenses;

        return response()->json([
            'organization_id' => $orgId,
            'gross_revenue' => (float) $totalRevenue,
            'operating_expenses' => (float) $totalExpenses,
            'net_income' => (float) $netResult,
        ]);
    }

    public function balanceSheet(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';

        $accounts = Account::where('organization_id', $orgId)->get();

        $assets = $accounts->filter(fn($a) => Str::startsWith($a->chart_of_account_id, 'coa_asset') || Str::startsWith($a->code, '1'))->sum('balance');
        $liabilities = $accounts->filter(fn($a) => Str::startsWith($a->chart_of_account_id, 'coa_liability') || Str::startsWith($a->code, '2'))->sum('balance');
        $equity = $accounts->filter(fn($a) => Str::startsWith($a->chart_of_account_id, 'coa_equity') || Str::startsWith($a->code, '3'))->sum('balance');

        return response()->json([
            'organization_id' => $orgId,
            'total_assets' => (float) $assets,
            'total_liabilities' => (float) $liabilities,
            'total_equity' => (float) $equity,
            'is_balanced' => abs($assets - ($liabilities + $equity)) < 0.01,
        ]);
    }

    public function trialBalance(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';
        $accounts = Account::where('organization_id', $orgId)->get();

        $rows = $accounts->map(function($acc) {
            return [
                'account_id' => $acc->id,
                'code' => $acc->code,
                'name' => $acc->name,
                'debit' => $acc->balance >= 0 ? (float) $acc->balance : 0,
                'credit' => $acc->balance < 0 ? (float) abs($acc->balance) : 0,
            ];
        });

        $totalDebit = $rows->sum('debit');
        $totalCredit = $rows->sum('credit');

        return response()->json([
            'accounts' => $rows,
            'total_debit' => $totalDebit,
            'total_credit' => $totalCredit,
            'is_balanced' => abs($totalDebit - $totalCredit) < 0.01,
        ]);
    }
}
