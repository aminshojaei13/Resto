<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Account;
use App\Models\Expense;
use App\Services\AccountingService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class ExpenseController extends Controller
{
    protected AccountingService $accountingService;

    public function __construct(AccountingService $accountingService)
    {
        $this->accountingService = $accountingService;
    }

    public function index(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';
        $expenses = Expense::where('organization_id', $orgId)->orderBy('date', 'desc')->get();
        return response()->json($expenses);
    }

    public function show(string $id)
    {
        $expense = Expense::findOrFail($id);
        return response()->json($expense);
    }

    public function store(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';

        $request->validate([
            'store_id' => 'required|string',
            'category' => 'required|string',
            'amount' => 'required|numeric|min:0.01',
            'date' => 'required|date',
            'payment_method' => 'nullable|string',
            'notes' => 'nullable|string',
        ]);

        return DB::transaction(function () use ($request, $orgId) {
            $expense = Expense::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $orgId,
                'store_id' => $request->store_id,
                'category' => $request->category,
                'amount' => $request->amount,
                'payment_method' => $request->payment_method ?? 'CASH',
                'date' => $request->date,
                'notes' => $request->notes ?? '',
                'user_id' => $request->user()?->id,
            ]);

            // Accounting entry: Dr Expense (5010), Cr Cash/Bank (1010)
            $expenseAccount = Account::firstOrCreate(
                ['organization_id' => $orgId, 'code' => '5010'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_expense', 'name' => 'Operating Expense', 'balance' => 0]
            );

            $cashAccount = Account::firstOrCreate(
                ['organization_id' => $orgId, 'code' => '1010'],
                ['id' => (string) Str::uuid(), 'chart_of_account_id' => 'coa_asset', 'name' => 'Cash / POS Drawer', 'balance' => 0]
            );

            $this->accountingService->postJournalEntry(
                $orgId,
                $request->store_id,
                "Operating Expense: {$request->category}",
                [
                    [
                        'account_id' => $expenseAccount->id,
                        'type' => 'DEBIT',
                        'amount' => $request->amount,
                        'description' => "Expense [{$request->category}]: {$request->notes}"
                    ],
                    [
                        'account_id' => $cashAccount->id,
                        'type' => 'CREDIT',
                        'amount' => $request->amount,
                        'description' => "Cash payment for expense [{$request->category}]"
                    ]
                ],
                'Expense',
                $expense->id
            );

            return response()->json($expense, 201);
        });
    }

    public function update(Request $request, string $id)
    {
        $expense = Expense::findOrFail($id);

        $request->validate([
            'category' => 'sometimes|required|string',
            'amount' => 'sometimes|required|numeric|min:0.01',
            'date' => 'sometimes|required|date',
            'notes' => 'nullable|string',
        ]);

        $expense->update($request->only(['category', 'amount', 'date', 'payment_method', 'notes']));

        return response()->json($expense);
    }

    public function destroy(string $id)
    {
        $expense = Expense::findOrFail($id);
        $expense->delete();
        return response()->json(['message' => 'Expense deleted successfully']);
    }
}
