<?php

namespace Tests\Feature;

use App\Models\BusinessApplication;
use App\Models\Organization;
use App\Models\User;
use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Tests\TestCase;

class SaasOnboardingTest extends TestCase
{
    use RefreshDatabase;

    protected User $adminUser;
    protected User $normalUser;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);

        // Platform Admin
        $this->adminUser = User::create([
            'id' => (string) Str::uuid(),
            'name' => 'Resto Platform Admin',
            'email' => 'admin@resto.com',
            'password' => bcrypt('password123'),
            'role' => 'Owner',
            'is_platform_admin' => true,
        ]);

        // Regular Tenant User
        $this->normalUser = User::create([
            'id' => (string) Str::uuid(),
            'name' => 'Regular User',
            'email' => 'user@example.com',
            'password' => bcrypt('password123'),
            'role' => 'Manager',
            'is_platform_admin' => false,
        ]);
    }

    public function test_public_business_registration_creates_pending_application(): void
    {
        $payload = [
            'business_name' => 'Grand Coffee Roasters',
            'owner_name' => 'Reza Alavi',
            'email' => 'reza@grandcoffee.com',
            'phone' => '+1 (555) 333-4444',
            'business_type' => 'RESTAURANT',
            'city' => 'Tehran',
            'address' => 'Valiasr St, Tehran',
        ];

        $response = $this->postJson('/api/v1/business-applications', $payload);

        $response->assertStatus(201)
                 ->assertJsonFragment(['status' => 'PENDING']);

        $this->assertDatabaseHas('business_applications', [
            'business_name' => 'Grand Coffee Roasters',
            'status' => 'PENDING',
        ]);
    }

    public function test_platform_admin_authorization_enforcement(): void
    {
        // Normal user attempting to list platform applications
        $response = $this->actingAs($this->normalUser)
                         ->getJson('/api/v1/platform/business-applications');

        $response->assertStatus(403);
    }

    public function test_platform_admin_approval_and_transactional_provisioning(): void
    {
        $app = BusinessApplication::create([
            'id' => (string) Str::uuid(),
            'business_name' => 'Gourmet Bakery',
            'owner_name' => 'Sara Hosseini',
            'email' => 'sara@gourmetbakery.com',
            'phone' => '+1 (555) 777-8888',
            'business_type' => 'BAKERY',
            'city' => 'Isfahan',
            'status' => 'PENDING',
        ]);

        $response = $this->actingAs($this->adminUser)
                         ->postJson("/api/v1/platform/business-applications/{$app->id}/approve");

        $response->assertStatus(200)
                 ->assertJsonFragment(['message' => 'درخواست تأیید و کسب‌وکار ایجاد شد. لینک فعال‌سازی برای مالک ارسال گردید.']);

        $app->refresh();
        $this::assertEquals('APPROVED', $app->status);
        $this::assertNotNull($app->organization_id);

        // Verify Tenant, Owner, Membership, Default Store, and Default Warehouse were created
        $this->assertDatabaseHas('organizations', ['id' => $app->organization_id, 'name' => 'Gourmet Bakery']);
        $this->assertDatabaseHas('users', ['email' => 'sara@gourmetbakery.com']);
        $this->assertDatabaseHas('organization_memberships', ['organization_id' => $app->organization_id, 'role' => 'OWNER']);
        $this->assertDatabaseHas('stores', ['organization_id' => $app->organization_id]);
        $this->assertDatabaseHas('warehouses', ['organization_id' => $app->organization_id]);

        // The owner must never receive a platform-chosen password: they are
        // invited and set their own.
        $owner = User::where('email', 'sara@gourmetbakery.com')->first();
        $this::assertEquals(User::STATUS_PENDING_INVITE, $owner->status);
        $this::assertFalse(Hash::check('password123', $owner->password));
        $this->assertDatabaseHas('staff_invitations', [
            'organization_id' => $app->organization_id,
            'email' => 'sara@gourmetbakery.com',
            'status' => 'PENDING',
        ]);

        // The response must not contain the invitation token.
        $this::assertStringNotContainsString('token', (string) $response->json('owner.invitation_id'));
        $this::assertArrayNotHasKey('token', (array) $response->json('owner'));
    }

    public function test_platform_admin_rejection_flow(): void
    {
        $app = BusinessApplication::create([
            'id' => (string) Str::uuid(),
            'business_name' => 'Incomplete Request Co',
            'owner_name' => 'Unknown Owner',
            'email' => 'unknown@example.com',
            'status' => 'PENDING',
        ]);

        $response = $this->actingAs($this->adminUser)
                         ->postJson("/api/v1/platform/business-applications/{$app->id}/reject", [
                             'reason' => 'Missing valid business documentation and contact phone.'
                         ]);

        $response->assertStatus(200)
                 ->assertJsonFragment(['message' => 'Business application rejected.']);

        $app->refresh();
        $this::assertEquals('REJECTED', $app->status);
        $this::assertEquals('Missing valid business documentation and contact phone.', $app->rejection_reason);
    }

    public function test_tenant_onboarding_completion_flow(): void
    {
        // An owner of org_apex completes setup.
        $owner = User::find('usr_admin_1');

        $response = $this->actingAs($owner)
                         ->postJson('/api/v1/tenant/onboarding/complete', [], ['X-Tenant-ID' => 'org_apex']);

        $response->assertStatus(200)
                 ->assertJsonFragment(['onboarding_status' => 'COMPLETED']);

        $this->assertDatabaseHas('organizations', [
            'id' => 'org_apex',
            'onboarding_status' => 'COMPLETED',
        ]);
    }

    public function test_onboarding_cannot_target_a_business_the_caller_does_not_belong_to(): void
    {
        $stranger = User::create([
            'id' => (string) Str::uuid(),
            'name' => 'Outsider',
            'email' => 'outsider@example.com',
            'password' => bcrypt('Str0ng-Passphrase!'),
            'status' => User::STATUS_ACTIVE,
        ]);

        $this->actingAs($stranger)
            ->postJson('/api/v1/tenant/onboarding/complete', [], ['X-Tenant-ID' => 'org_apex'])
            ->assertStatus(403);
    }
}
