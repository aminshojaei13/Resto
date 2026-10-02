<?php

namespace App\Http\Controllers\Api\Platform;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\BusinessApplication;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\StaffInvitation;
use App\Models\Store;
use App\Models\User;
use App\Models\Warehouse;
use App\Notifications\StaffInvitationNotification;
use Illuminate\Http\Request;
use Illuminate\Notifications\AnonymousNotifiable;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Str;
use Exception;

class PlatformApplicationController extends Controller
{
    /**
     * Authorization is handled by the `platform.admin` route middleware.
     * This guard remains only as a defence in depth for direct controller use:
     * the caller must be the authenticated platform administrator. There is no
     * header override, no environment fallback and no "first admin" lookup.
     */
    private function checkPlatformAdmin(Request $request): bool
    {
        $user = $request->user();

        return $user instanceof User && $user->isPlatformAdmin();
    }

    public function index(Request $request)
    {
        if (!$this->checkPlatformAdmin($request)) {
            return response()->json(['error' => 'Unauthorized platform admin access'], 403);
        }

        $query = BusinessApplication::query();
        if ($request->has('status')) {
            $query->where('status', $request->status);
        }

        return response()->json($query->orderBy('created_at', 'desc')->get());
    }

    public function show(Request $request, string $id)
    {
        if (!$this->checkPlatformAdmin($request)) {
            return response()->json(['error' => 'Unauthorized platform admin access'], 403);
        }

        $application = BusinessApplication::with('organization')->findOrFail($id);
        return response()->json($application);
    }

    public function approve(Request $request, string $id)
    {
        if (!$this->checkPlatformAdmin($request)) {
            return response()->json(['error' => 'Unauthorized platform admin access'], 403);
        }

        $application = BusinessApplication::findOrFail($id);

        if ($application->status === 'APPROVED') {
            return response()->json([
                'message' => 'Business application has already been approved.',
                'organization_id' => $application->organization_id,
            ], 200);
        }

        if ($application->status === 'REJECTED') {
            return response()->json(['error' => 'Cannot approve a rejected application.'], 422);
        }

        // Transactional Tenant Provisioning
        return DB::transaction(function () use ($request, $application) {
            $reviewer = $request->user();

            // 1. Create or Find Owner User
            //
            // No password is ever chosen, displayed or stored here. The owner
            // receives an invitation link and sets their own password.
            $user = User::where('email', $application->email)->first();
            if (!$user) {
                $nameParts = preg_split('/\s+/u', trim((string) $application->owner_name), 2) ?: [];
                $user = User::create([
                    'id' => (string) Str::uuid(),
                    'name' => $application->owner_name,
                    'first_name' => $nameParts[0] ?? $application->owner_name,
                    'last_name' => $nameParts[1] ?? null,
                    'email' => $application->email,
                    'phone' => $application->phone,
                    // Placeholder: the account is unusable until the owner
                    // completes the invitation and sets a real password.
                    'password' => Hash::make(Str::random(64)),
                    'role' => 'Owner',
                    'status' => User::STATUS_PENDING_INVITE,
                    'is_platform_admin' => false,
                ]);
            }

            // 2. Create Organization / Tenant
            $code = strtoupper(substr(preg_replace('/[^A-Za-z0-9]/', '', $application->business_name), 0, 4));
            if (strlen($code) < 3) $code = 'RSTO';
            $code .= rand(100, 999);

            $organization = Organization::create([
                'id' => (string) Str::uuid(),
                'name' => $application->business_name,
                'code' => $code,
                'currency_symbol' => '$',
                'currency_code' => 'USD',
                'subscription_tier' => 'ENTERPRISE',
                'business_type' => $application->business_type ?? 'RETAIL',
                'onboarding_status' => 'IN_PROGRESS',
            ]);

            // 3. Create Organization Membership (Owner)
            OrganizationMembership::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'user_id' => $user->id,
                'role' => 'OWNER',
                'status' => OrganizationMembership::STATUS_ACTIVE,
                'invited_by' => $reviewer?->id,
                'joined_at' => now(),
            ]);

            // 4. Provision Default Store
            $store = Store::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'name' => $application->business_name . ' (Main Store)',
                'code' => $code . '-ST1',
                'address' => $application->address ?? 'Main Store Address',
                'phone' => $application->phone ?? '',
            ]);

            // 5. Provision Default Warehouse
            Warehouse::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'store_id' => $store->id,
                'name' => 'Main Warehouse',
                'code' => $code . '-WH1',
                'address' => $application->address ?? 'Main Warehouse Address',
            ]);

            // 6. Update BusinessApplication status
            $application->status = 'APPROVED';
            $application->organization_id = $organization->id;
            $application->reviewed_by = $reviewer?->id;
            $application->reviewed_at = now();
            $application->save();

            // 7. Audit Logging
            $reviewerEmail = $reviewer?->email ?? '';
            AuditLog::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'user_id' => $reviewer?->id,
                'action' => 'business.application.approved',
                'entity_type' => 'Organization',
                'entity_id' => $organization->id,
                'details' => "Business '{$organization->name}' provisioned for owner {$user->email} by platform administrator {$reviewerEmail}",
                'ip_address' => $request->ip(),
            ]);

            // 8. Invite the owner so they can set their own password.
            // The response never contains the invitation token.
            $rawToken = Str::random(64);
            $invitation = StaffInvitation::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $organization->id,
                'email' => $user->email,
                'first_name' => $user->first_name ?? $application->owner_name,
                'last_name' => $user->last_name,
                'role' => OrganizationMembership::ROLE_OWNER,
                'token' => hash('sha256', $rawToken),
                'invited_by' => $reviewer->id,
                'status' => StaffInvitation::STATUS_PENDING,
                'expires_at' => now()->addHours((int) config('resto.staff_invitation_hours', 72)),
            ]);

            Notification::send(new AnonymousNotifiable, new StaffInvitationNotification(
                $rawToken,
                $organization->name,
                $reviewer->full_name !== '' ? $reviewer->full_name : $reviewer->email,
                OrganizationMembership::ROLE_OWNER,
                $user->preferredLanguage(),
            ));

            return response()->json([
                'message' => 'درخواست تأیید و کسب‌وکار ایجاد شد. لینک فعال‌سازی برای مالک ارسال گردید.',
                'application' => $application,
                'organization' => $organization->load(['stores.warehouses']),
                'owner' => [
                    'id' => $user->id,
                    'name' => $user->full_name,
                    'email' => $user->email,
                    'invitation_id' => $invitation->id,
                ]
            ], 200);
        });
    }

    public function reject(Request $request, string $id)
    {
        if (!$this->checkPlatformAdmin($request)) {
            return response()->json(['error' => 'Unauthorized platform admin access'], 403);
        }

        $request->validate([
            'reason' => 'required|string|max:1000',
        ]);

        $application = BusinessApplication::findOrFail($id);

        if ($application->status === 'APPROVED') {
            return response()->json(['error' => 'Cannot reject an already approved application.'], 422);
        }

        $reviewer = $request->user();
        $application->status = 'REJECTED';
        $application->rejection_reason = $request->reason;
        $application->reviewed_by = $reviewer?->id;
        $application->reviewed_at = now();
        $application->save();

        AuditLog::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'system',
            'user_id' => $reviewer?->id,
            'action' => 'business.application.rejected',
            'entity_type' => 'BusinessApplication',
            'entity_id' => $application->id,
            'details' => "Application '{$application->business_name}' rejected: {$application->rejection_reason}",
            'ip_address' => $request->ip(),
        ]);

        return response()->json([
            'message' => 'Business application rejected.',
            'application' => $application,
        ], 200);
    }
}
