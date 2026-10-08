<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\OrganizationMembership;
use App\Models\StaffInvitation;
use App\Models\User;
use App\Notifications\StaffInvitationNotification;
use App\Support\MembershipContext;
use App\Support\PasswordPolicy;
use App\Support\RolePermission;
use Illuminate\Http\Request;
use Illuminate\Notifications\AnonymousNotifiable;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Str;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

/**
 * Staff access management.
 *
 * An owner (or a manager with the staff.manage permission) invites a person by
 * email. The invitee receives a single-use link, sets their own password, and
 * only then becomes a member. A member's role lives on their membership, never
 * on the user record, so a staff member can never present themselves as an owner.
 */
class StaffController extends Controller
{
    /**
     * People who can be reached at this business.
     */
    public function index(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $memberships = OrganizationMembership::query()
            ->with(['user'])
            ->where('organization_id', $organizationId)
            ->orderBy('role')
            ->get();

        $role = MembershipContext::roleFor($request, $organizationId);

        $pendingInvitations = [];
        if (RolePermission::allows($role, RolePermission::STAFF_MANAGE)) {
            $pendingInvitations = StaffInvitation::where('organization_id', $organizationId)
                ->where('status', StaffInvitation::STATUS_PENDING)
                ->get()
                ->filter(fn (StaffInvitation $i) => $i->isPending())
                ->map(fn (StaffInvitation $i) => $i->toPreviewArray() + ['id' => $i->id])
                ->values();
        }

        return response()->json([
            'members' => $memberships->map(fn (OrganizationMembership $m) => $this->memberPayload($request, $m))->values(),
            'pending_invitations' => $pendingInvitations,
            'can_manage_staff' => RolePermission::allows($role, RolePermission::STAFF_MANAGE),
        ]);
    }

    /**
     * Invite a person. The response never contains the invitation token.
     */
    public function store(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'email' => ['required', 'string', 'email', 'max:255'],
            'first_name' => ['required', 'string', 'max:120'],
            'last_name' => ['nullable', 'string', 'max:120'],
            'role' => ['nullable', Rule::in([OrganizationMembership::ROLE_MANAGER, OrganizationMembership::ROLE_STAFF])],
            'locale' => ['nullable', Rule::in(['fa', 'en'])],
        ]);

        $email = Str::lower(trim($validated['email']));
        $role = $validated['role'] ?? OrganizationMembership::ROLE_STAFF;

        $existingUser = User::where('email', $email)->first();
        $alreadyMember = OrganizationMembership::where('organization_id', $organizationId)
            ->where(fn ($q) => $existingUser
                ? $q->where('user_id', $existingUser->id)
                : $q->whereRaw('1 = 0'))
            ->exists();

        if ($alreadyMember) {
            throw ValidationException::withMessages([
                'email' => ['این شخص از قبل عضو کسب‌وکار شماست.'],
            ]);
        }

        $pending = StaffInvitation::where('organization_id', $organizationId)
            ->where('email', $email)
            ->where('status', StaffInvitation::STATUS_PENDING)
            ->get()
            ->first(fn (StaffInvitation $i) => $i->isPending());

        if ($pending) {
            throw ValidationException::withMessages([
                'email' => ['برای این ایمیل یک دعوت فعال وجود دارد. لطفاً پیش از دعوت دوباره، زمان اعتبار آن را بررسی کنید.'],
            ]);
        }

        $rawToken = Str::random(64);
        $inviter = $request->user();

        $invitation = StaffInvitation::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $organizationId,
            'email' => $email,
            'first_name' => trim($validated['first_name']),
            'last_name' => isset($validated['last_name']) ? trim($validated['last_name']) : null,
            'role' => $role,
            'token' => hash('sha256', $rawToken),
            'invited_by' => $inviter->id,
            'status' => StaffInvitation::STATUS_PENDING,
            'expires_at' => now()->addHours((int) config('resto.staff_invitation_hours', 72)),
        ]);

        $this->sendInvitationMail($invitation, $rawToken, $inviter, $validated['locale'] ?? $inviter->preferredLanguage());

        $this->audit('staff.invited', $inviter, $request, [
            'organization_id' => $organizationId,
            'email' => $email,
            'role' => RolePermission::canonical($role),
        ]);

        return response()->json([
            'message' => 'دعوت‌نامه برای این شخص ارسال شد.',
            'invitation' => $invitation->toPreviewArray() + ['id' => $invitation->id],
        ], 201);
    }

    /**
     * Re-issue an invitation with a fresh token.
     */
    public function resend(Request $request, string $invitationId)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $invitation = $this->findInvitation($request, $invitationId);

        if ($invitation->status !== StaffInvitation::STATUS_PENDING) {
            throw ValidationException::withMessages([
                'invitation' => ['این دعوت دیگر فعال نیست. لطفاً یک دعوت جدید ایجاد کنید.'],
            ]);
        }

        $rawToken = Str::random(64);
        $invitation->forceFill([
            'token' => hash('sha256', $rawToken),
            'expires_at' => now()->addHours((int) config('resto.staff_invitation_hours', 72)),
        ])->save();

        $this->sendInvitationMail($invitation, $rawToken, $request->user(), $request->user()->preferredLanguage());

        $this->audit('staff.invitation_resent', $request->user(), $request, [
            'organization_id' => $organizationId,
            'invitation_id' => $invitation->id,
        ]);

        return response()->json([
            'message' => 'دعوت‌نامه دوباره ارسال شد.',
            'invitation' => $invitation->toPreviewArray() + ['id' => $invitation->id],
        ]);
    }

    public function revoke(Request $request, string $invitationId)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $invitation = $this->findInvitation($request, $invitationId);

        if ($invitation->status !== StaffInvitation::STATUS_PENDING) {
            throw ValidationException::withMessages([
                'invitation' => ['فقط دعوت‌های فعال قابل لغو هستند.'],
            ]);
        }

        $invitation->update(['status' => StaffInvitation::STATUS_REVOKED]);

        $this->audit('staff.invitation_revoked', $request->user(), $request, [
            'organization_id' => $organizationId,
            'invitation_id' => $invitation->id,
        ]);

        return response()->json(['message' => 'دعوت لغو شد.']);
    }

    /**
     * Public: preview an invitation so the accept screen can greet the person
     * by their real name. Reveals nothing beyond what the email already shows.
     */
    public function showInvitation(Request $request, string $token)
    {
        $invitation = $this->findInvitationByToken($token);

        if (!$invitation || !$invitation->isPending()) {
            return response()->json([
                'message' => 'این دعوت‌نامه نامعتبر یا منقضی شده است. لطفاً از مدیر کسب‌وکار بخواهید دعوت تازه‌ای بفرستد.',
            ], 404);
        }

        return response()->json(['invitation' => $invitation->toPreviewArray()]);
    }

    /**
     * Public: accept an invitation, create the account if needed, let the
     * person set their own password, and create their membership.
     */
    public function accept(Request $request)
    {
        $request->validate([
            'token' => ['required', 'string', 'size:64'],
            'password' => ['required', 'string', 'confirmed'],
            'locale' => ['nullable', Rule::in(['fa', 'en'])],
        ]);

        $invitation = $this->findInvitationByToken($request->token);

        if (!$invitation || !$invitation->isPending()) {
            throw ValidationException::withMessages([
                'token' => ['این دعوت‌نامه نامعتبر یا منقضی شده است. لطفاً از مدیر کسب‌وکار بخواهید دعوت تازه‌ای بفرستد.'],
            ]);
        }

        $errors = PasswordPolicy::validate($request->input('password'));

        if ($errors !== []) {
            throw ValidationException::withMessages(['password' => $errors]);
        }

        $user = DB::transaction(function () use ($invitation, $request) {
            $user = User::where('email', $invitation->email)->first();

            if (!$user) {
                $user = User::create([
                    'id' => (string) Str::uuid(),
                    'name' => trim($invitation->first_name . ' ' . (string) $invitation->last_name),
                    'first_name' => $invitation->first_name,
                    'last_name' => $invitation->last_name,
                    'email' => $invitation->email,
                    'password' => Hash::make($request->password),
                    'role' => 'Staff',
                    'status' => User::STATUS_ACTIVE,
                    'is_platform_admin' => false,
                    'preferred_locale' => $request->input('locale', 'fa'),
                ]);
            } else {
                // A pre-existing account (e.g. the platform owner) keeps its
                // identity; only the password is set by the invitee.
                $user->first_name = $user->first_name ?: $invitation->first_name;
                $user->last_name = $user->last_name ?: $invitation->last_name;
                $user->password = Hash::make($request->password);
                $user->status = User::STATUS_ACTIVE;
                $user->save();
            }

            // Re-inviting an existing member simply reactivates them.
            $membership = OrganizationMembership::where('organization_id', $invitation->organization_id)
                ->where('user_id', $user->id)
                ->first();

            if ($membership) {
                $membership->update([
                    'status' => OrganizationMembership::STATUS_ACTIVE,
                    'joined_at' => now(),
                ]);
            } else {
                OrganizationMembership::create([
                    'id' => (string) Str::uuid(),
                    'organization_id' => $invitation->organization_id,
                    'user_id' => $user->id,
                    'role' => $invitation->role,
                    'status' => OrganizationMembership::STATUS_ACTIVE,
                    'invited_by' => $invitation->invited_by,
                    'joined_at' => now(),
                ]);
            }

            $invitation->update([
                'status' => StaffInvitation::STATUS_ACCEPTED,
                'accepted_at' => now(),
            ]);

            return $user;
        });

        $this->audit('staff.invitation_accepted', $user, $request, [
            'organization_id' => $invitation->organization_id,
            'invitation_id' => $invitation->id,
        ]);

        return response()->json([
            'message' => 'حساب کاربری شما ساخته شد. اکنون می‌توانید وارد شوید.',
            'user' => [
                'id' => $user->id,
                'name' => $user->full_name,
                'email' => $user->email,
            ],
        ]);
    }

    public function updateRole(Request $request, string $userId)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'role' => ['required', Rule::in([
                OrganizationMembership::ROLE_OWNER,
                OrganizationMembership::ROLE_MANAGER,
                OrganizationMembership::ROLE_STAFF,
            ])],
        ]);

        $membership = $this->findMembership($request, $userId);
        $newRole = $validated['role'];

        $this->assertKeepsAnOwner($membership, $organizationId, 'تغییر نقش');

        $membership->update(['role' => $newRole]);

        $this->audit('staff.role_changed', $request->user(), $request, [
            'organization_id' => $organizationId,
            'user_id' => $membership->user_id,
            'from' => $membership->getOriginal('role'),
            'to' => $newRole,
        ]);

        return response()->json([
            'message' => 'نقش کاربر به‌روزرسانی شد.',
            'member' => $this->memberPayload($request, $membership->fresh('user')),
        ]);
    }

    /**
     * Deactivate / reactivate a member. Their next request is rejected
     * server-side, which also revokes their tokens.
     */
    public function setStatus(Request $request, string $userId)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'status' => ['required', Rule::in([
                OrganizationMembership::STATUS_ACTIVE,
                OrganizationMembership::STATUS_DEACTIVATED,
            ])],
        ]);

        $membership = $this->findMembership($request, $userId);

        if ($validated['status'] === OrganizationMembership::STATUS_DEACTIVATED) {
            $this->assertKeepsAnOwner($membership, $organizationId, 'غیرفعال‌سازی');
        }

        $membership->update(['status' => $validated['status']]);

        if ($validated['status'] === OrganizationMembership::STATUS_DEACTIVATED) {
            $membership->user?->tokens()->delete();
        }

        $this->audit('staff.status_changed', $request->user(), $request, [
            'organization_id' => $organizationId,
            'user_id' => $membership->user_id,
            'status' => $validated['status'],
        ]);

        return response()->json([
            'message' => $validated['status'] === OrganizationMembership::STATUS_ACTIVE
                ? 'دسترسی کاربر دوباره فعال شد.'
                : 'دسترسی کاربر غیرفعال شد.',
            'member' => $this->memberPayload($request, $membership->fresh('user')),
        ]);
    }

    public function destroy(Request $request, string $userId)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $membership = $this->findMembership($request, $userId);

        $this->assertKeepsAnOwner($membership, $organizationId, 'حذف');

        $membership->user?->tokens()->delete();
        $membership->delete();

        $this->audit('staff.removed', $request->user(), $request, [
            'organization_id' => $organizationId,
            'user_id' => $userId,
        ]);

        return response()->json(['message' => 'دسترسی کاربر حذف شد.']);
    }

    /* ------------------------------------------------------------------ */

    private function memberPayload(Request $request, OrganizationMembership $membership): array
    {
        $user = $membership->user;

        return [
            'membership_id' => $membership->id,
            'id' => $membership->user_id,
            'first_name' => $user?->first_name,
            'last_name' => $user?->last_name,
            'name' => $user?->full_name,
            'display_name' => ($user?->full_name ?: $user?->email) ?? 'کاربر حذف‌شده',
            'email' => $user?->email,
            'phone' => $user?->phone,
            'role' => RolePermission::canonical($membership->role),
            'status' => $membership->status ?? OrganizationMembership::STATUS_ACTIVE,
            'joined_at' => optional($membership->joined_at)->toIso8601String(),
            'is_self' => $membership->user_id === $request->user()?->id,
        ];
    }

    private function findMembership(Request $request, string $userId): OrganizationMembership
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $membership = OrganizationMembership::where('organization_id', $organizationId)
            ->where('user_id', $userId)
            ->first();

        if (!$membership) {
            throw ValidationException::withMessages([
                'user' => ['این کاربر عضو کسب‌وکار شما نیست.'],
            ]);
        }

        return $membership->load('user');
    }

    private function findInvitation(Request $request, string $invitationId): StaffInvitation
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $invitation = StaffInvitation::where('organization_id', $organizationId)
            ->where('id', $invitationId)
            ->first();

        if (!$invitation) {
            throw ValidationException::withMessages([
                'invitation' => ['دعوت‌نامه مورد نظر یافت نشد.'],
            ]);
        }

        return $invitation;
    }

    private function findInvitationByToken(string $token): ?StaffInvitation
    {
        return StaffInvitation::where('token', hash('sha256', $token))->first();
    }

    /**
     * A business must always keep at least one active owner, otherwise nobody
     * could ever manage it again.
     */
    private function assertKeepsAnOwner(OrganizationMembership $membership, string $organizationId, string $action): void
    {
        if (RolePermission::canonical($membership->role) !== OrganizationMembership::ROLE_OWNER) {
            return;
        }

        $remainingOwners = OrganizationMembership::where('organization_id', $organizationId)
            ->where('role', OrganizationMembership::ROLE_OWNER)
            ->where('status', OrganizationMembership::STATUS_ACTIVE)
            ->where('id', '!=', $membership->id)
            ->count();

        if ($remainingOwners === 0) {
            throw ValidationException::withMessages([
                'user' => ["{$action} مالک اصلی کسب‌وکار مجاز نیست. ابتدا مالک دیگری تعیین کنید."],
            ]);
        }
    }

    private function sendInvitationMail(
        StaffInvitation $invitation,
        string $rawToken,
        User $inviter,
        string $locale
    ): void {
        $organizationName = $invitation->organization?->name ?? 'کسب‌وکار شما';

        Notification::send(
            new AnonymousNotifiable,
            new StaffInvitationNotification(
                $rawToken,
                $organizationName,
                $inviter->full_name !== '' ? $inviter->full_name : $inviter->email,
                RolePermission::canonical($invitation->role),
                $locale,
            ),
        );
    }

    private function audit(string $action, ?User $user, Request $request, array $details): void
    {
        unset($details['password'], $details['token']);

        try {
            AuditLog::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $details['organization_id'] ?? 'system',
                'user_id' => $user?->id,
                'action' => $action,
                'entity_type' => 'OrganizationMembership',
                'entity_id' => $details['user_id'] ?? $details['invitation_id'] ?? null,
                'details' => json_encode($details, JSON_UNESCAPED_UNICODE) ?: '{}',
                'ip_address' => $request->ip(),
            ]);
        } catch (\Throwable $e) {
            Log::warning('audit_log_write_failed', ['action' => $action, 'error' => $e->getMessage()]);
        }
    }
}
