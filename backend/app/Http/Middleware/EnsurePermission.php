<?php

namespace App\Http\Middleware;

use App\Support\MembershipContext;
use App\Support\RolePermission;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Route-level permission gate.
 *
 * Usage: ->middleware('permission:staff.manage')
 *
 * The role always comes from the caller's own OrganizationMembership row for
 * the active organization. A role sent by the client is ignored entirely.
 */
class EnsurePermission
{
    public function handle(Request $request, Closure $next, string ...$permissions): Response
    {
        try {
            $organizationId = MembershipContext::activeOrganizationId($request);
        } catch (\Throwable) {
            return response()->json([
                'message' => 'شما به هیچ کسب‌وکاری دسترسی فعال ندارید.',
            ], 403);
        }

        $role = MembershipContext::roleFor($request, $organizationId);

        if ($role === null) {
            return response()->json([
                'message' => 'شما عضو فعال این کسب‌وکار نیستید.',
            ], 403);
        }

        foreach ($permissions as $permission) {
            if (!RolePermission::allows($role, $permission)) {
                return response()->json([
                    'message' => 'دسترسی لازم برای انجام این عملیات را ندارید.',
                    'required_permission' => $permission,
                ], 403);
            }
        }

        $request->attributes->set('member_role', $role);

        return $next($request);
    }
}
