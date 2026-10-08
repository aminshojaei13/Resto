<?php

namespace App\Http\Middleware;

use App\Models\User;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Gate for the platform console.
 *
 * The only accepted proof of platform administration is an authenticated
 * user whose own record has is_platform_admin = true. There is deliberately
 * no header override, no environment fallback and no "first admin" lookup.
 */
class EnsurePlatformAdmin
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (!$user instanceof User) {
            return response()->json(['message' => 'برای دسترسی به این بخش باید وارد حساب مدیریت سامانه شوید.'], 401);
        }

        if (!$user->is_platform_admin) {
            return response()->json(['message' => 'شما اجازه دسترسی به بخش مدیریت سامانه را ندارید.'], 403);
        }

        return $next($request);
    }
}
