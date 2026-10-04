<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Support\MembershipContext;
use App\Models\AuditLog;
use Illuminate\Http\Request;

class AuditLogController extends Controller
{
    public function index(Request $request)
    {
        $orgId = MembershipContext::activeOrganizationId($request);
        $logs = AuditLog::where('organization_id', $orgId)->orderBy('created_at', 'desc')->get();
        return response()->json($logs);
    }
}
