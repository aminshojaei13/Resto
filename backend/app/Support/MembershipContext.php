<?php

namespace App\Support;

use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\User;
use Illuminate\Http\Request;
use RuntimeException;

/**
 * Resolves the *authenticated* caller's business context.
 *
 * There is exactly one authoritative source for "who is this request":
 * the Sanctum-authenticated User. The active organization is then derived
 * from that user's own OrganizationMembership records — never from a
 * hardcoded id, never from the first organization in the table, and never
 * from an unverified header value alone.
 */
class MembershipContext
{
    /**
     * Active memberships of the authenticated user, optionally scoped to one organization.
     *
     * @return \Illuminate\Database\Eloquent\Collection<int, OrganizationMembership>
     */
    public static function memberships(Request $request, ?string $organizationId = null)
    {
        $user = self::user($request);

        if (!$user) {
            return OrganizationMembership::query()->whereRaw('1 = 0')->get();
        }

        return self::membershipsFor($user, $organizationId);
    }

    /**
     * Active memberships of an explicitly given user record.
     *
     * @return \Illuminate\Database\Eloquent\Collection<int, OrganizationMembership>
     */
    public static function membershipsFor(User $user, ?string $organizationId = null)
    {
        $query = OrganizationMembership::query()
            ->with(['organization.stores.warehouses', 'user'])
            ->where('user_id', $user->id)
            ->where('status', OrganizationMembership::STATUS_ACTIVE);

        if ($organizationId !== null) {
            $query->where('organization_id', $organizationId);
        }

        return $query->get();
    }

    public static function user(Request $request): ?User
    {
        $user = $request->user();

        return $user instanceof User ? $user : null;
    }

    /**
     * The organization the request is operating on.
     *
     * Resolution order:
     *   1. attribute set by TenantMiddleware (after it verified membership)
     *   2. explicit X-Tenant-ID header / org_id input, still verified below
     *   3. the caller's single membership (or their only OWNER membership)
     *
     * @throws RuntimeException when the caller has no access at all
     */
    public static function activeOrganizationId(Request $request): string
    {
        $resolved = $request->attributes->get('org_id');

        if (is_string($resolved) && $resolved !== '') {
            return $resolved;
        }

        $requested = $request->header('X-Tenant-ID')
            ?? $request->query('org_id')
            ?? $request->input('org_id');

        if (is_string($requested) && $requested !== '') {
            return $requested;
        }

        $memberships = self::memberships($request);

        if ($memberships->count() === 1) {
            return (string) $memberships->first()->organization_id;
        }

        $ownerMembership = $memberships->first(
            fn (OrganizationMembership $m) => $m->role === OrganizationMembership::ROLE_OWNER
        );

        if ($ownerMembership) {
            return (string) $ownerMembership->organization_id;
        }

        throw new RuntimeException('No accessible business was found for the authenticated user.');
    }

    /**
     * Role of the authenticated user inside $organizationId, or null when
     * the caller has no active membership there.
     */
    public static function roleFor(Request $request, string $organizationId): ?string
    {
        $membership = self::memberships($request, $organizationId)->first();

        return $membership ? (string) $membership->role : null;
    }

    /**
     * Active store id for the request, validated against the active organization.
     */
    public static function activeStoreId(Request $request, string $organizationId): ?string
    {
        $storeId = $request->attributes->get('store_id')
            ?? $request->header('X-Store-ID')
            ?? $request->query('store_id')
            ?? $request->input('store_id');

        if (is_string($storeId) && $storeId !== '') {
            return $storeId;
        }

        $organization = Organization::with('stores')->find($organizationId);

        return $organization?->stores->first()?->id;
    }
}
