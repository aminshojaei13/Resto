<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class TenantMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        $tenantId = $request->header('X-Tenant-ID') ?? $request->query('org_id') ?? 'org_apex';
        $storeId = $request->header('X-Store-ID') ?? $request->query('store_id') ?? 'store_apex_1';

        $request->attributes->set('org_id', $tenantId);
        $request->attributes->set('store_id', $storeId);

        return $next($request);
    }
}
