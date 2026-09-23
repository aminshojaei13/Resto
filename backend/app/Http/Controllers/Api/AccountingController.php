<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Account;
use App\Models\JournalEntry;
use App\Models\Order;
use App\Services\AccountingService;
use Illuminate\Http\Request;

class AccountingController extends Controller
{
    protected AccountingService $accountingService;

    public function __construct(AccountingService $accountingService)
    {
        $this->accountingService = $accountingService;
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
        $request->validate([
            'org_id' => 'required',
            'store_id' => 'required',
            'description' => 'required',
            'lines' => 'required|array|min:2',
        ]);

        $entry = $this->accountingService->postJournalEntry(
            $request->org_id,
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

        $totalSalesCount = Order::where('organization_id', $orgId)->count();
        $todaySalesCount = Order::where('organization_id', $orgId)
            ->whereDate('created_at', date('Y-m-d'))
            ->count();

        $accounts = Account::where('organization_id', $orgId)->get();

        return response()->json([
            'total_revenue' => (float) $totalRevenue,
            'today_revenue' => (float) $todayRevenue,
            'total_sales_count' => $totalSalesCount,
            'today_sales_count' => $todaySalesCount,
            'accounts' => $accounts,
        ]);
    }
}
