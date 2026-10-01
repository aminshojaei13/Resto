<?php

namespace App\Http\Controllers\Api\Public;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\BusinessApplication;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class BusinessApplicationController extends Controller
{
    public function store(Request $request)
    {
        $request->validate([
            'business_name' => 'required|string|max:255',
            'owner_name' => 'required|string|max:255',
            'email' => 'required|email|max:255',
            'phone' => 'nullable|string|max:50',
            'business_type' => 'nullable|string|max:50',
            'country' => 'nullable|string|max:100',
            'city' => 'nullable|string|max:100',
            'address' => 'nullable|string',
            'notes' => 'nullable|string',
        ]);

        // Duplicate PENDING application protection
        $existing = BusinessApplication::where('email', $request->email)
            ->where('status', 'PENDING')
            ->first();

        if ($existing) {
            return response()->json([
                'message' => 'Your application has already been submitted and is pending review.',
                'application_id' => $existing->id,
                'status' => 'PENDING',
            ], 200);
        }

        $autoApprove = $request->boolean('auto_approve', false) || $request->input('auto_approve') === true || $request->input('auto_approve') === 'true';

        $application = BusinessApplication::create([
            'id' => (string) Str::uuid(),
            'business_name' => $request->business_name,
            'owner_name' => $request->owner_name,
            'email' => strtolower(trim($request->email)),
            'phone' => $request->phone,
            'business_type' => $request->business_type ?? 'RETAIL',
            'country' => $request->country ?? 'IR',
            'city' => $request->city,
            'address' => $request->address,
            'notes' => $request->notes,
            'status' => $autoApprove ? 'APPROVED' : 'PENDING',
        ]);

        $organizationId = null;

        if ($autoApprove) {
            // Instant Tenant Provisioning
            $code = strtoupper(substr(preg_replace('/[^A-Za-z0-9]/', '', $application->business_name), 0, 4));
            if (strlen($code) < 3) $code = 'RSTO';
            $code .= rand(100, 999);

            $user = \App\Models\User::where('email', $application->email)->first();
            if (!$user) {
                $user = \App\Models\User::create([
                    'id' => (string) Str::uuid(),
                    'name' => $application->owner_name,
                    'email' => $application->email,
                    'phone' => $application->phone,
                    'password' => \Illuminate\Support\Facades\Hash::make('password123'),
                    'role' => 'Owner',
                    'is_platform_admin' => false,
                ]);
            }

            $organization = \App\Models\Organization::create([
                'id' => (string) Str::uuid(),
                'name' => $application->business_name,
                'code' => $code,
                'currency_symbol' => '$',
                'currency_code' => 'USD',
                'subscription_tier' => 'ENTERPRISE',
                'business_type' => $application->business_type ?? 'RETAIL',
                'onboarding_status' => 'IN_PROGRESS',
            ]);

            \App\Models\OrganizationMembership::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'user_id' => $user->id,
                'role' => 'OWNER',
            ]);

            $store = \App\Models\Store::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'name' => $application->business_name . ' (Main Store)',
                'code' => $code . '-ST1',
                'address' => $application->address ?? 'Main Store Address',
                'phone' => $application->phone ?? '',
            ]);

            \App\Models\Warehouse::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'store_id' => $store->id,
                'name' => 'Main Warehouse',
                'code' => $code . '-WH1',
                'address' => $application->address ?? 'Main Warehouse Address',
            ]);

            $application->organization_id = $organization->id;
            $application->save();
            $organizationId = $organization->id;
        }

        // Audit Log
        AuditLog::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $organizationId ?? 'system',
            'user_id' => null,
            'action' => $autoApprove ? 'business.application.auto_approved' : 'business.application.created',
            'entity_type' => 'BusinessApplication',
            'entity_id' => $application->id,
            'details' => "Application submitted for business '{$application->business_name}' by {$application->email}" . ($autoApprove ? " (Auto-Approved)" : ""),
            'ip_address' => $request->ip(),
        ]);

        return response()->json([
            'message' => $autoApprove ? 'Business application submitted and automatically provisioned!' : 'Your business application has been submitted and is pending review.',
            'application_id' => $application->id,
            'organization_id' => $organizationId,
            'status' => $application->status,
            'created_at' => $application->created_at,
        ], 201);
    }

    public function showStatus(string $id)
    {
        $application = BusinessApplication::findOrFail($id);

        return response()->json([
            'id' => $application->id,
            'business_name' => $application->business_name,
            'owner_name' => $application->owner_name,
            'status' => $application->status,
            'rejection_reason' => $application->rejection_reason,
            'created_at' => $application->created_at,
        ]);
    }
}
