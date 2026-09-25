<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use App\Models\IdempotencyKey;
use Symfony\Component\HttpFoundation\Response;
use Illuminate\Support\Str;

class IdempotencyMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        $idempotencyKey = $request->header('X-Idempotency-Key');

        if (!$idempotencyKey) {
            return $next($request);
        }

        $orgId = $request->attributes->get('org_id') ?? $request->header('X-Tenant-ID') ?? 'org_apex';

        $existing = IdempotencyKey::where('organization_id', $orgId)
            ->where('key', $idempotencyKey)
            ->first();

        if ($existing) {
            return response($existing->response_body, $existing->response_code)
                ->header('Content-Type', 'application/json')
                ->header('X-Cache-Lookup', 'HIT-IDEMPOTENT');
        }

        $response = $next($request);

        if ($response->getStatusCode() >= 200 && $response->getStatusCode() < 300) {
            IdempotencyKey::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $orgId,
                'key' => $idempotencyKey,
                'request_path' => $request->path(),
                'response_code' => $response->getStatusCode(),
                'response_body' => $response->getContent() ?? '',
            ]);
        }

        return $response;
    }
}
