<?php

namespace App\Http\Middleware;

use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\User;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Server-side tenant isolation.
 *
 * Runs only on routes that are already behind `auth:sanctum`, so the user is
 * always an authenticated database record. The active organization is only
 * accepted when the caller has an ACTIVE membership in it — a deactivated
 * membership or a foreign organization header is rejected, never ignored.
 */
class TenantMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (!$user instanceof User) {
            return response()->json([
                'message' => 'برای انجام این عملیات باید وارد حساب کاربری خود شوید.',
            ], 401);
        }

        $requestedOrgId = $request->header('X-Tenant-ID')
            ?? $request->query('org_id')
            ?? $request->input('org_id');

        if (is_string($requestedOrgId) && $requestedOrgId !== '') {
            if (!Organization::where('id', $requestedOrgId)->exists()) {
                return response()->json(['message' => 'کسب‌وکار مورد نظر یافت نشد.'], 404);
            }

            $membership = OrganizationMembership::where('organization_id', $requestedOrgId)
                ->where('user_id', $user->id)
                ->first();

            if (!$membership) {
                return response()->json([
                    'message' => 'شما عضو این کسب‌وکار نیستید.',
                ], 403);
            }

            if (!$membership->isActive()) {
                return response()->json([
                    'message' => 'دسترسی شما به این کسب‌وکار غیرفعال شده است.',
                ], 403);
            }
        }

        $storeId = $request->header('X-Store-ID')
            ?? $request->query('store_id')
            ?? $request->input('store_id');

        if (is_string($storeId) && $storeId !== '') {
            $storeExists = \App\Models\Store::where('id', $storeId)->exists();

            if (!$storeExists) {
                return response()->json(['message' => 'فروشگاه مورد نظر یافت نشد.'], 404);
            }
        }

        $request->attributes->set('org_id', is_string($requestedOrgId) ? $requestedOrgId : null);
        $request->attributes->set('store_id', is_string($storeId) ? $storeId : null);

        return $next($request);
    }
}
