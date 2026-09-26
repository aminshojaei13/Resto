<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Redis;

class HealthController extends Controller
{
    public function check()
    {
        $status = 'healthy';
        $dbStatus = 'ok';
        $redisStatus = 'ok';

        try {
            DB::connection()->getPdo();
        } catch (\Throwable $e) {
            $status = 'unhealthy';
            $dbStatus = 'error';
        }

        try {
            Redis::ping();
        } catch (\Throwable $e) {
            $redisStatus = 'degraded'; // Redis optional fallback for cache
        }

        $httpCode = $status === 'healthy' ? 200 : 503;

        return response()->json([
            'status' => $status,
            'services' => [
                'database' => $dbStatus,
                'redis' => $redisStatus,
            ],
            'timestamp' => now()->toIso8601String(),
        ], $httpCode);
    }
}
