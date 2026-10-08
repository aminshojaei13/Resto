<?php

namespace Tests\Feature;

use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\StaffInvitation;
use App\Models\User;
use App\Notifications\StaffInvitationNotification;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Str;
use Tests\TestCase;

/**
 * P9 — staff access, invitation lifecycle and tenant isolation.
 */
class StaffAccessTest extends TestCase
{
    use RefreshDatabase;

    private User $owner;
    private User $otherOwner;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
        Notification::fake();

        $this->owner = User::create([
            'id' => 'usr_p9_owner',
            'first_name' => 'حسین',
            'last_name' => 'کاظمی',
            'name' => 'حسین کاظمی',
            'email' => 'owner@apex.example',
            'password' => Hash::make('Owner-Pass-123'),
            'status' => User::STATUS_ACTIVE,
        ]);

        $this->otherOwner = User::create([
            'id' => 'usr_p9_owner2',
            'first_name' => 'دوم',
            'last_name' => 'مالک',
            'name' => 'دوم مالک',
            'email' => 'owner2@apex.example',
            'password' => Hash::make('Owner-Pass-123'),
            'status' => User::STATUS_ACTIVE,
        ]);

        OrganizationMembership::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_apex',
            'user_id' => $this->owner->id,
            'role' => 'OWNER',
            'status' => 'ACTIVE',
            'joined_at' => now(),
        ]);
        OrganizationMembership::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_apex',
            'user_id' => $this->otherOwner->id,
            'role' => 'OWNER',
            'status' => 'ACTIVE',
            'joined_at' => now(),
        ]);
    }

    /**
     * The raw invitation token only ever exists inside the emailed link, so a
     * test has to read it from the notification that was actually sent.
     */
    private function lastInvitationToken(): ?string
    {
        $find = function ($node) use (&$find) {
            if ($node instanceof StaffInvitationNotification) {
                return $node->rawToken;
            }

            if (! is_array($node)) {
                return null;
            }

            foreach ($node as $child) {
                $token = $find(is_array($child) ? ($child['notification'] ?? $child) : $child);

                if ($token !== null) {
                    return $token;
                }
            }

            return null;
        };

        return $find(Notification::sentNotifications());
    }

    /* -------------------------------------------------------------- inviting */

    public function test_owner_invites_a_person_by_email_and_a_link_is_emailed(): void
    {
        $response = $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', [
                'email' => 'new.staff@example.com',
                'first_name' => 'رضا',
                'last_name' => 'جعفری',
                'role' => 'STAFF',
            ]);

        $response->assertStatus(201)
            ->assertJsonFragment(['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $this->assertDatabaseHas('staff_invitations', [
            'organization_id' => 'org_apex',
            'email' => 'new.staff@example.com',
            'status' => 'PENDING',
        ]);

        Notification::assertSentToTimes(
            new \Illuminate\Notifications\AnonymousNotifiable,
            StaffInvitationNotification::class,
            1
        );
    }

    public function test_the_invitation_response_never_leaks_the_token(): void
    {
        $response = $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', [
                'email' => 'new.staff@example.com',
                'first_name' => 'رضا',
            ]);

        $body = (string) $response->getContent();

        $this->assertStringNotContainsString('token', $body);
        $this->assertArrayNotHasKey('token', (array) $response->json('invitation'));
    }

    public function test_stored_invitation_token_is_hashed(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $raw = $this->lastInvitationToken();
        $stored = StaffInvitation::first()->token;

        $this->assertNotSame($raw, $stored);
        $this->assertSame(hash('sha256', $raw), $stored);
    }

    public function test_cannot_invite_someone_who_is_already_a_member(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', [
                'email' => 'owner2@apex.example',
                'first_name' => 'دوم',
            ])
            ->assertStatus(422);
    }

    public function test_cannot_invite_as_owner(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', [
                'email' => 'new.staff@example.com',
                'first_name' => 'رضا',
                'role' => 'OWNER',
            ])
            ->assertStatus(422);
    }

    /* -------------------------------------------------------------- accepting */

    public function test_invitee_accepts_sets_their_own_password_and_gains_access(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $token = $this->lastInvitationToken();

        // Preview is public and greets the person by their real name.
        $preview = $this->getJson("/api/v1/staff/invitations/{$token}")->assertStatus(200);

        $preview->assertJsonPath('invitation.email', 'new.staff@example.com')
            ->assertJsonPath('invitation.first_name', 'رضا')
            ->assertJsonPath('invitation.organization_name', 'Apex Retail Group')
            ->assertJsonPath('invitation.status', 'PENDING')
            ->assertJsonMissingPath('invitation.token');

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $token,
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(200);

        $this->assertDatabaseHas('users', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);
        $this->assertDatabaseHas('organization_memberships', [
            'organization_id' => 'org_apex',
            'user_id' => User::where('email', 'new.staff@example.com')->value('id'),
            'role' => 'STAFF',
            'status' => 'ACTIVE',
        ]);

        // The new person signs in and sees their own identity, not the owner's.
        $login = $this->postJson('/api/v1/auth/login', [
            'email' => 'new.staff@example.com',
            'password' => 'Their-Own-Pass-9',
        ])->assertStatus(200);

        $login->assertJsonFragment(['first_name' => 'رضا', 'role' => 'STAFF']);

        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $login->json('access_token'))
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->getJson('/api/v1/auth/profile')
            ->assertStatus(200)
            ->assertJsonPath('name', 'رضا')
            ->assertJsonPath('role', 'STAFF');
    }

    public function test_invitation_token_is_single_use(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $token = $this->lastInvitationToken();

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $token,
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(200);

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $token,
            'password' => 'Another-Pass-99',
            'password_confirmation' => 'Another-Pass-99',
        ])->assertStatus(422);
    }

    public function test_expired_invitation_is_rejected(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $token = $this->lastInvitationToken();

        $this->travel(config('resto.staff_invitation_hours') + 2)->hours();

        $this->getJson("/api/v1/staff/invitations/{$token}")->assertStatus(404);

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $token,
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(422);

        $this->assertDatabaseMissing('users', ['email' => 'new.staff@example.com']);
    }

    public function test_invitation_enforces_the_password_policy(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $this->lastInvitationToken(),
            'password' => 'password1',
            'password_confirmation' => 'password1',
        ])->assertStatus(422);
    }

    public function test_invitation_can_be_revoked_and_resent(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $invitationId = StaffInvitation::first()->id;

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->deleteJson("/api/v1/staff/invitations/{$invitationId}")
            ->assertStatus(200);

        $this->assertDatabaseHas('staff_invitations', ['id' => $invitationId, 'status' => 'REVOKED']);

        // A revoked link no longer works.
        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $this->lastInvitationToken(),
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(422);
    }

    /* --------------------------------------------------------- staff listing */

    public function test_staff_list_shows_each_person_with_their_own_identity(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $this->lastInvitationToken(),
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(200);

        $response = $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->getJson('/api/v1/staff');

        $response->assertStatus(200);

        $names = array_column($response->json('members'), 'name');
        $this->assertContains('حسین کاظمی', $names);
        $this->assertContains('رضا', $names);
        $this->assertNotContains('دوم مالک', array_diff($names, ['دوم مالک']));
    }

    public function test_staff_cannot_manage_staff(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $this->lastInvitationToken(),
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(200);

        $staffId = User::where('email', 'new.staff@example.com')->value('id');

        $login = $this->postJson('/api/v1/auth/login', [
            'email' => 'new.staff@example.com', 'password' => 'Their-Own-Pass-9',
        ])->json('access_token');

        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $login)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->putJson("/api/v1/staff/{$staffId}/role", ['role' => 'OWNER'])
            ->assertStatus(403);

        $this->assertDatabaseHas('organization_memberships', [
            'user_id' => $staffId, 'role' => 'STAFF',
        ]);
    }

    /* ---------------------------------------------------------- deactivation */

    public function test_deactivated_staff_cannot_access_the_business(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $this->lastInvitationToken(),
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(200);

        $staffId = User::where('email', 'new.staff@example.com')->value('id');
        $login = $this->postJson('/api/v1/auth/login', [
            'email' => 'new.staff@example.com', 'password' => 'Their-Own-Pass-9',
        ])->json('access_token');

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->putJson("/api/v1/staff/{$staffId}/status", ['status' => 'DEACTIVATED'])
            ->assertStatus(200);

        // Deactivation takes effect immediately: the existing session is dead.
        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $login)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->getJson('/api/v1/products')
            ->assertStatus(401);

        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $login)
            ->getJson('/api/v1/auth/profile')
            ->assertStatus(401);

        // The account itself is untouched: they can still sign in ...
        $relogin = $this->withFreshAuth()
            ->postJson('/api/v1/auth/login', [
                'email' => 'new.staff@example.com', 'password' => 'Their-Own-Pass-9',
            ])->assertStatus(200);

        // ... but the business is closed to them.
        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $relogin->json('access_token'))
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->getJson('/api/v1/products')
            ->assertStatus(403);

        // Reactivating restores access.
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->putJson("/api/v1/staff/{$staffId}/status", ['status' => 'ACTIVE'])
            ->assertStatus(200);

        $login2 = $this->postJson('/api/v1/auth/login', [
            'email' => 'new.staff@example.com', 'password' => 'Their-Own-Pass-9',
        ])->json('access_token');

        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $login2)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->getJson('/api/v1/products')
            ->assertStatus(200);
    }

    public function test_last_active_owner_cannot_be_demoted_deactivated_or_removed(): void
    {
        // Demote the only other owner so the first one becomes the last.
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->putJson("/api/v1/staff/{$this->otherOwner->id}/role", ['role' => 'STAFF'])
            ->assertStatus(200);

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->putJson("/api/v1/staff/{$this->owner->id}/role", ['role' => 'STAFF'])
            ->assertStatus(422);

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->putJson("/api/v1/staff/{$this->owner->id}/status", ['status' => 'DEACTIVATED'])
            ->assertStatus(422);

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->deleteJson("/api/v1/staff/{$this->owner->id}")
            ->assertStatus(422);

        $this->assertDatabaseHas('organization_memberships', [
            'user_id' => $this->owner->id, 'role' => 'OWNER', 'status' => 'ACTIVE',
        ]);
    }

    /* --------------------------------------------------------- isolation */

    public function test_staff_management_cannot_cross_tenants(): void
    {
        Organization::create(['id' => 'org_zenith', 'name' => 'Zenith Retail', 'code' => 'ZENI']);
        OrganizationMembership::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_zenith',
            'user_id' => $this->owner->id,
            'role' => 'OWNER',
            'status' => 'ACTIVE',
        ]);

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_zenith')
            ->putJson("/api/v1/staff/{$this->otherOwner->id}/status", ['status' => 'DEACTIVATED'])
            ->assertStatus(422, 'That user is not a member of the business in the header.');

        $this->assertDatabaseHas('organization_memberships', [
            'user_id' => $this->otherOwner->id,
            'organization_id' => 'org_apex',
            'status' => 'ACTIVE',
        ]);
    }

    public function test_acting_on_another_business_entirely_is_rejected(): void
    {
        Organization::create(['id' => 'org_zenith', 'name' => 'Zenith Retail', 'code' => 'ZENI']);

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_zenith')
            ->getJson('/api/v1/staff')
            ->assertStatus(403);
    }

    /* ----------------------------------------------------------- audit trail */

    public function test_staff_events_are_audit_logged(): void
    {
        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', ['email' => 'new.staff@example.com', 'first_name' => 'رضا']);

        $this->assertDatabaseHas('audit_logs', [
            'action' => 'staff.invited',
            'user_id' => $this->owner->id,
            'organization_id' => 'org_apex',
        ]);

        $this->postJson('/api/v1/staff/invitations/accept', [
            'token' => $this->lastInvitationToken(),
            'password' => 'Their-Own-Pass-9',
            'password_confirmation' => 'Their-Own-Pass-9',
        ])->assertStatus(200);

        $this->assertDatabaseHas('audit_logs', ['action' => 'staff.invitation_accepted']);
    }
}
