<?php

namespace Tests\Feature;

use App\Models\AuditLog;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\User;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Tests\TestCase;

/**
 * P9 — identity and authentication.
 */
class IdentityAndAuthenticationTest extends TestCase
{
    use RefreshDatabase;

    private User $owner;
    private User $cashier;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);

        $this->owner = User::create([
            'id' => 'usr_p9_owner',
            'first_name' => 'محمد',
            'last_name' => 'رضایی',
            'name' => 'محمد رضایی',
            'email' => 'm.rezaei@example.com',
            'phone' => '+98 912 000 0000',
            'password' => Hash::make('Correct-Horse-9'),
            'role' => 'Owner',
            'status' => User::STATUS_ACTIVE,
        ]);

        $this->cashier = User::create([
            'id' => 'usr_p9_cashier',
            'first_name' => 'سارا',
            'last_name' => 'کریمی',
            'name' => 'سارا کریمی',
            'email' => 's.karimi@example.com',
            'password' => Hash::make('Correct-Horse-9'),
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
            'user_id' => $this->cashier->id,
            'role' => 'STAFF',
            'status' => 'ACTIVE',
            'joined_at' => now(),
        ]);
    }

    /* ---------------------------------------------------------------- login */

    public function test_login_returns_the_authenticated_person_and_a_usable_token(): void
    {
        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com',
            'password' => 'Correct-Horse-9',
        ]);

        $response->assertStatus(200)
            ->assertJsonStructure(['access_token', 'token_type', 'user'])
            ->assertJsonFragment([
                'id' => 'usr_p9_owner',
                'first_name' => 'محمد',
                'last_name' => 'رضایی',
                'name' => 'محمد رضایی',
                'email' => 'm.rezaei@example.com',
                'role' => 'OWNER',
            ]);

        $this::assertContains('catalog.manage', $response->json('user.permissions'));

        $this->assertNotEmpty($response->json('access_token'));

        // The token really authenticates subsequent requests.
        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $response->json('access_token'))
            ->getJson('/api/v1/auth/profile')
            ->assertStatus(200)
            ->assertJsonFragment(['email' => 'm.rezaei@example.com']);
    }

    public function test_login_never_ever_reveals_whether_an_account_exists(): void
    {
        $unknown = $this->postJson('/api/v1/auth/login', [
            'email' => 'nobody@example.com',
            'password' => 'Correct-Horse-9',
        ]);

        $wrongPassword = $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com',
            'password' => 'Wrong-Horse-9',
        ]);

        $unknown->assertStatus(422);
        $wrongPassword->assertStatus(422);

        $this->assertSame(
            $unknown->json(),
            $wrongPassword->json(),
            'Login must respond identically for an unknown address and a wrong password.'
        );
    }

    public function test_login_does_not_accept_a_password_or_identity_from_the_body_or_headers(): void
    {
        // The X-User-ID header is the legacy impersonation vector and is gone.
        $this->withHeader('X-User-ID', 'usr_p9_owner')
            ->postJson('/api/v1/auth/login', [
                'email' => 's.karimi@example.com',
                'password' => 'Correct-Horse-9',
            ])
            ->assertStatus(200)
            ->assertJsonFragment(['id' => 'usr_p9_cashier', 'name' => 'سارا کریمی']);
    }

    public function test_login_is_rate_limited(): void
    {
        config(['resto.login.max_attempts' => 3]);

        for ($i = 0; $i < 3; $i++) {
            $this->postJson('/api/v1/auth/login', [
                'email' => 'm.rezaei@example.com',
                'password' => 'Wrong-Horse-9',
            ])->assertStatus(422);
        }

        $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com',
            'password' => 'Correct-Horse-9',
        ])->assertStatus(429);
    }

    public function test_suspended_account_cannot_sign_in(): void
    {
        $this->owner->update(['status' => User::STATUS_BANNED]);

        $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com',
            'password' => 'Correct-Horse-9',
        ])->assertStatus(422);
    }

    /* ------------------------------------------------------------- identity */

    public function test_profile_comes_from_the_token_and_never_from_a_header(): void
    {
        $response = $this->actingAs($this->cashier)
            ->withHeader('X-User-ID', 'usr_p9_owner')
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->getJson('/api/v1/auth/profile');

        $response->assertStatus(200)
            ->assertJsonFragment([
                'id' => 'usr_p9_cashier',
                'name' => 'سارا کریمی',
                'email' => 's.karimi@example.com',
                'role' => 'STAFF',
            ])
            // Not the platform admin, not the owner, not a hardcoded fixture.
            ->assertJsonMissing(['name' => 'Alex Mercer'])
            ->assertJsonMissing(['email' => 'alex.mercer@calcuapp.com']);
    }

    public function test_profile_requires_authentication(): void
    {
        $this->getJson('/api/v1/auth/profile')->assertStatus(401);
    }

    public function test_profile_only_lists_the_callers_own_businesses(): void
    {
        Organization::create(['id' => 'org_other', 'name' => 'Other Business', 'code' => 'OTHR']);

        $response = $this->actingAs($this->owner)->getJson('/api/v1/auth/profile');

        $response->assertStatus(200);
        $names = array_column($response->json('memberships'), 'name');
        $this->assertContains('Apex Retail Group', $names);
        $this->assertNotContains('Other Business', $names);
    }

    public function test_full_name_is_never_manufactured_from_the_email(): void
    {
        $nameless = User::create([
            'id' => 'usr_p9_nameless',
            'name' => '',
            'email' => 'someone@example.com',
            'password' => Hash::make('Correct-Horse-9'),
            'status' => User::STATUS_ACTIVE,
        ]);

        $this->assertSame('', $nameless->full_name);
        $this->assertStringNotContainsString('someone', $nameless->full_name);
    }

    public function test_user_can_update_their_own_identity_only(): void
    {
        $this->actingAs($this->cashier)
            ->putJson('/api/v1/auth/profile', ['first_name' => 'سارا', 'last_name' => 'کریمی', 'phone' => '+98 913 111 2222'])
            ->assertStatus(200)
            ->assertJsonFragment(['name' => 'سارا کریمی', 'phone' => '+98 913 111 2222']);

        $this->assertDatabaseHas('users', [
            'id' => 'usr_p9_cashier',
            'first_name' => 'سارا',
            'last_name' => 'کریمی',
        ]);
    }

    /* --------------------------------------------------------------- logout */

    public function test_logout_revokes_only_the_current_session(): void
    {
        $phone = $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com', 'password' => 'Correct-Horse-9',
        ])->json('access_token');

        $tablet = $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com', 'password' => 'Correct-Horse-9',
        ])->json('access_token');

        $this->withHeader('Authorization', 'Bearer ' . $phone)
            ->postJson('/api/v1/auth/logout')
            ->assertStatus(200);

        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $phone)
            ->getJson('/api/v1/auth/profile')->assertStatus(401);

        $this->withHeader('Authorization', 'Bearer ' . $tablet)
            ->getJson('/api/v1/auth/profile')->assertStatus(200);
    }

    public function test_logout_all_revokes_every_session(): void
    {
        $first = $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com', 'password' => 'Correct-Horse-9',
        ])->json('access_token');
        $second = $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com', 'password' => 'Correct-Horse-9',
        ])->json('access_token');

        $this->withHeader('Authorization', 'Bearer ' . $first)
            ->postJson('/api/v1/auth/logout-all')
            ->assertStatus(200);

        $this->withFreshAuth()->withHeader('Authorization', 'Bearer ' . $first)
            ->getJson('/api/v1/auth/profile')->assertStatus(401);
        $this->withFreshAuth()->withHeader('Authorization', 'Bearer ' . $second)
            ->getJson('/api/v1/auth/profile')->assertStatus(401);
    }

    /* ------------------------------------------------------------- sessions */

    public function test_deactivating_a_membership_locks_the_person_out_immediately(): void
    {
        $token = $this->postJson('/api/v1/auth/login', [
            'email' => 's.karimi@example.com', 'password' => 'Correct-Horse-9',
        ])->json('access_token');

        $this->withFreshAuth()->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/auth/profile')->assertStatus(200);

        OrganizationMembership::where('user_id', $this->cashier->id)
            ->where('organization_id', 'org_apex')
            ->update(['status' => 'DEACTIVATED']);

        $this->withFreshAuth()->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/products')
            ->assertStatus(403, 'But the business is closed to them.');
    }

    public function test_banned_user_is_rejected_and_loses_their_tokens(): void
    {
        $token = $this->postJson('/api/v1/auth/login', [
            'email' => 's.karimi@example.com', 'password' => 'Correct-Horse-9',
        ])->json('access_token');

        $this->cashier->update(['status' => User::STATUS_BANNED]);

        $this->withFreshAuth()->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/auth/profile')
            ->assertStatus(403);

        $this->assertDatabaseCount('personal_access_tokens', 0);
    }

    /* ---------------------------------------------------------- permissions */

    public function test_staff_cannot_reach_owner_only_endpoints(): void
    {
        $this->actingAs($this->cashier)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', [
                'email' => 'new@example.com',
                'first_name' => 'نفر',
            ])
            ->assertStatus(403);

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/staff/invitations', [
                'email' => 'new@example.com',
                'first_name' => 'نفر',
            ])
            ->assertStatus(201);
    }

    public function test_staff_cannot_adjust_stock_but_owner_can(): void
    {
        $this->actingAs($this->cashier)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/inventory/adjust', [
                'warehouse_id' => 'wh_apex_1a',
                'product_id' => 'prod_1',
                'delta' => 5,
                'reason' => 'test',
            ])
            ->assertStatus(403);

        $this->actingAs($this->owner)
            ->withHeader('X-Tenant-ID', 'org_apex')
            ->postJson('/api/v1/inventory/adjust', [
                'warehouse_id' => 'wh_apex_1a',
                'product_id' => 'prod_1',
                'delta' => 5,
                'reason' => 'corrective count',
            ])
            ->assertStatus(200);
    }

    public function test_platform_console_requires_a_real_platform_administrator(): void
    {
        $this->actingAs($this->owner)
            ->getJson('/api/v1/platform/business-applications')->assertStatus(403);

        // The legacy escalation headers do not promote a signed-in employee.
        $this->withHeader('X-User-ID', 'usr_admin_1')
            ->withHeader('X-Platform-Admin', 'true')
            ->getJson('/api/v1/platform/business-applications')
            ->assertStatus(403);

        // With no token at all they grant nothing either.
        $this->withFreshAuth()
            ->withHeader('X-User-ID', 'usr_admin_1')
            ->withHeader('X-Platform-Admin', 'true')
            ->getJson('/api/v1/platform/business-applications')
            ->assertStatus(401);

        $this->actingAs(User::find('usr_admin_1'))
            ->getJson('/api/v1/platform/business-applications')->assertStatus(200);
    }

    /* ------------------------------------------------------------- auditing */

    public function test_security_events_are_audit_logged_without_secrets(): void
    {
        $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com', 'password' => 'Correct-Horse-9',
        ])->assertStatus(200);

        $this->assertDatabaseHas('audit_logs', [
            'action' => 'auth.login',
            'user_id' => 'usr_p9_owner',
        ]);

        $this->postJson('/api/v1/auth/login', [
            'email' => 'm.rezaei@example.com', 'password' => 'Wrong-Horse-9',
        ])->assertStatus(422);

        $this->assertDatabaseHas('audit_logs', ['action' => 'auth.login_failed']);

        // Nothing sensitive is written to the audit stream.
        $blob = AuditLog::pluck('details')->implode(' ');
        $this->assertStringNotContainsString('Correct-Horse-9', $blob);
        $this->assertStringNotContainsString('Wrong-Horse-9', $blob);
    }
}
