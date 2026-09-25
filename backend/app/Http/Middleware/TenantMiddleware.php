<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use Symfony\Component\HttpFoundation\Response;

class TenantMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        // Skip tenant enforcement for auth login endpoint
        if ($request->is('api/v1/auth/login')) {
            return $next($request);
        }

        $tenantId = $request->header('X-Tenant-ID') ?? $request->query('org_id') ?? $request->input('org_id') ?? 'org_apex';
        $storeId = $request->header('X-Store-ID') ?? $request->query('store_id') ?? $request->input('store_id') ?? 'store_apex_1';

        $orgExists = Organization::where('id', $tenantId)->exists();
        if (!$orgExists) {
            return response()->json(['error' => "Organization '{$tenantId}' not found"], 404);
        }

        // Server-side membership authorization check
        $user = $request->user();
        if ($user) {
            $isMember = OrganizationMembership::where('organization_id', $tenantId)
                ->where('user_id', $user->id)
                ->exists();

            if (!$isMember) {
                return response()->json([
                    'error' => 'Unauthorized organization access',
                    'message' => "User {$user->id} is not authorized to access organization {$tenantId}"
                ], 403);
            }
        }

        $request->attributes->set('org_id', $tenantId);
        $request->attributes->set('store_id', $storeId);

        return $next($request);
    }
}
