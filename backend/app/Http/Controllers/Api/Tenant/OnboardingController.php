<?php

namespace App\Http\Controllers\Api\Tenant;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Organization;
use App\Support\MembershipContext;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

class OnboardingController extends Controller
{
    public function show(Request $request)
    {
        $organization = $this->activeOrganization($request);

        return response()->json([
            'organization_id' => $organization->id,
            'name' => $organization->name,
            'business_type' => $organization->business_type,
            'onboarding_status' => $organization->onboarding_status,
            'stores_count' => $organization->stores->count(),
            'warehouses_count' => $organization->stores->sum(fn ($s) => $s->warehouses->count()),
        ]);
    }

    public function complete(Request $request)
    {
        $organization = $this->activeOrganization($request);

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
            'message' => 'راه‌اندازی کسب‌وکار تکمیل شد.',
            'organization' => $organization,
        ]);
    }

    /**
     * The organization is always the authenticated caller's own business.
     * There is no default organization and no "first organization" fallback.
     */
    private function activeOrganization(Request $request): Organization
    {
        try {
            $orgId = MembershipContext::activeOrganizationId($request);
        } catch (\Throwable) {
            throw ValidationException::withMessages([
                'org_id' => ['کسب‌وکار فعالی برای حساب شما یافت نشد.'],
            ]);
        }

        $organization = Organization::with(['stores.warehouses'])->find($orgId);

        if (!$organization) {
            throw ValidationException::withMessages([
                'org_id' => ['کسب‌وکار مورد نظر یافت نشد.'],
            ]);
        }

        return $organization;
    }
}
