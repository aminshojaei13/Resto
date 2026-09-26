<?php

namespace App\Http\Controllers\Api\Tenant;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Organization;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class OnboardingController extends Controller
{
    public function show(Request $request)
    {
        $orgId = $request->get('org_id') ?? $request->header('X-Tenant-ID') ?? 'org_apex';
        $organization = Organization::with(['stores.warehouses'])->findOrFail($orgId);

        return response()->json([
            'organization_id' => $organization->id,
            'name' => $organization->name,
            'business_type' => $organization->business_type,
            'onboarding_status' => $organization->onboarding_status,
            'stores_count' => $organization->stores->count(),
            'warehouses_count' => $organization->stores->sum(fn($s) => $s.warehouses->count()),
        ]);
    }

    public function complete(Request $request)
    {
        $orgId = $request->get('org_id') ?? $request->header('X-Tenant-ID') ?? 'org_apex';
        $organization = Organization::findOrFail($orgId);

        $organization->onboarding_status = 'COMPLETED';
        $organization->save();

        AuditLog::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $organization->id,
            'user_id' => $request->user()?->id,
            'action' => 'onboarding.completed',
            'entity_type' => 'Organization',
            'entity_id' => $organization->id,
            'details' => "Onboarding completed for business '{$organization->name}'",
            'ip_address' => $request->ip(),
        ]);

        return response()->json([
            'message' => 'Onboarding completed successfully.',
            'organization' => $organization,
        ]);
    }
}
