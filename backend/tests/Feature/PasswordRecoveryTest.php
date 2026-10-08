<?php

namespace Tests\Feature;

use App\Models\User;
use App\Notifications\ResetPasswordNotification;
use Illuminate\Auth\Notifications\ResetPassword;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Facades\Password;
use Illuminate\Support\Str;
use Tests\TestCase;

/**
 * P9 — password recovery and password change.
 */
class PasswordRecoveryTest extends TestCase
{
    use RefreshDatabase;

    private User $owner;

    protected function setUp(): void
    {
        parent::setUp();
        Notification::fake();

        $this->owner = User::create([
            'id' => 'usr_p9_rec',
            'first_name' => 'مریم',
            'last_name' => 'نوری',
            'name' => 'مریم نوری',
            'email' => 'Maryam@Example.com',
            'password' => Hash::make('Original-Pass-1'),
            'status' => User::STATUS_ACTIVE,
        ]);
    }

    private function tokenFor(string $email): string
    {
        $user = User::where('email', $email)->first();

        return Password::broker()->createToken($user);
    }

    /* ------------------------------------------------------- forgot password */

    public function test_forgot_password_never_returns_a_code_or_link(): void
    {
        $response = $this->postJson('/api/v1/auth/forgot-password', [
            'email' => 'maryam@example.com',
        ]);

        $response->assertStatus(200);
        $body = (string) $response->getContent();

        $this->assertStringNotContainsString('reset_code', $body);
        $this->assertStringNotContainsString('123456', $body);
        $this->assertStringNotContainsString('token', $body);
        $this->assertStringNotContainsString(config('resto.frontend_url'), $body);
    }

    public function test_forgot_password_responds_identically_for_unknown_addresses(): void
    {
        $known = $this->postJson('/api/v1/auth/forgot-password', ['email' => 'maryam@example.com']);
        $unknown = $this->postJson('/api/v1/auth/forgot-password', ['email' => 'ghost@example.com']);

        $known->assertStatus(200);
        $unknown->assertStatus(200);
        $this::assertSame($known->json(), $unknown->json());

        Notification::assertSentToTimes($this->owner, ResetPasswordNotification::class, 1);
        $this::assertCount(1, Notification::sentNotifications()[User::class] ?? []);
    }

    public function test_recovery_email_is_localized_and_links_to_the_web_app(): void
    {
        $this->postJson('/api/v1/auth/forgot-password', ['email' => 'maryam@example.com']);

        Notification::assertSentTo($this->owner, ResetPasswordNotification::class, function ($notification) {
            $mail = $notification->toMail($this->owner);
            $this->assertSame('بازیابی رمز عبور رستو', $mail->subject);
            $this->assertStringContainsString('/reset-password?token=', $mail->actionUrl);
            $this->assertStringContainsString('maryam%40example.com', $mail->actionUrl);

            return true;
        });
    }

    public function test_english_users_receive_an_english_recovery_email(): void
    {
        $this->owner->update(['preferred_locale' => 'en']);

        $this->postJson('/api/v1/auth/forgot-password', ['email' => 'maryam@example.com']);

        Notification::assertSentTo($this->owner, ResetPasswordNotification::class, function ($notification) {
            $mail = $notification->toMail($this->owner);
            $this->assertSame('Reset your Resto password', $mail->subject);
            $this->assertStringNotContainsString('بازیابی', $mail->introLines[0] ?? '');

            return true;
        });
    }

    public function test_stored_reset_token_is_hashed_not_plaintext(): void
    {
        $token = $this->tokenFor('maryam@example.com');
        $this::assertNotNull($token);

        $stored = DB::table('password_reset_tokens')->where('email', 'maryam@example.com')->value('token');

        $this->assertNotSame($token, $stored);
        $this->assertTrue(Hash::check($token, $stored));
    }

    /* --------------------------------------------------------- reset password */

    public function test_reset_password_sets_a_new_password_and_is_single_use(): void
    {
        $token = $this->tokenFor('maryam@example.com');

        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => $token,
            'password' => 'Brand-New-Pass-77',
            'password_confirmation' => 'Brand-New-Pass-77',
        ])->assertStatus(200);

        $this->assertTrue(Hash::check('Brand-New-Pass-77', $this->owner->fresh()->password));

        // The same token must not work twice.
        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => $token,
            'password' => 'Another-Pass-88',
            'password_confirmation' => 'Another-Pass-88',
        ])->assertStatus(422);

        $this->assertTrue(Hash::check('Brand-New-Pass-77', $this->owner->fresh()->password));
    }

    public function test_expired_reset_token_is_rejected(): void
    {
        $token = $this->tokenFor('maryam@example.com');

        $this->travel(config('resto.password_reset.expire_minutes') + 2)->minutes();

        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => $token,
            'password' => 'Too-Late-Pass-1',
            'password_confirmation' => 'Too-Late-Pass-1',
        ])->assertStatus(422);

        $this->assertTrue(Hash::check('Original-Pass-1', $this->owner->fresh()->password));
    }

    public function test_reset_revokes_existing_sessions(): void
    {
        $sessionToken = $this->owner->createToken('web')->plainTextToken;
        $resetToken = $this->tokenFor('maryam@example.com');

        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => $resetToken,
            'password' => 'Brand-New-Pass-77',
            'password_confirmation' => 'Brand-New-Pass-77',
        ])->assertStatus(200);

        $this->withFreshAuth()
            ->withHeader('Authorization', 'Bearer ' . $sessionToken)
            ->getJson('/api/v1/auth/profile')->assertStatus(401);
    }

    public function test_reset_rejects_a_weak_password(): void
    {
        $token = $this->tokenFor('maryam@example.com');

        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => $token,
            'password' => 'password123',
            'password_confirmation' => 'password123',
        ])->assertStatus(422);

        $this->assertTrue(Hash::check('Original-Pass-1', $this->owner->fresh()->password));
    }

    public function test_the_old_demo_reset_code_no_longer_works(): void
    {
        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => '123456',
            'password' => 'Brand-New-Pass-77',
            'password_confirmation' => 'Brand-New-Pass-77',
        ])->assertStatus(422);

        $this->assertTrue(Hash::check('Original-Pass-1', $this->owner->fresh()->password));
    }

    public function test_forgot_password_is_rate_limited(): void
    {
        config(['resto.password_reset.throttle_seconds' => 2]);

        for ($i = 0; $i < 2; $i++) {
            $this->postJson('/api/v1/auth/forgot-password', ['email' => 'maryam@example.com'])
                ->assertStatus(200);
        }

        $this->postJson('/api/v1/auth/forgot-password', ['email' => 'maryam@example.com'])
            ->assertStatus(429);
    }

    /* -------------------------------------------------------- change password */

    public function test_change_password_requires_the_current_password(): void
    {
        $this->actingAs($this->owner)
            ->postJson('/api/v1/auth/change-password', [
                'current_password' => 'Not-My-Password',
                'new_password' => 'Whatever-New-1',
                'new_password_confirmation' => 'Whatever-New-1',
            ])->assertStatus(422);

        $this->assertTrue(Hash::check('Original-Pass-1', $this->owner->fresh()->password));
    }

    public function test_change_password_identifies_the_caller_from_the_token_only(): void
    {
        $other = User::create([
            'id' => 'usr_p9_other',
            'name' => 'Other Person',
            'email' => 'other@example.com',
            'password' => Hash::make('Their-Own-Pass-9'),
            'status' => User::STATUS_ACTIVE,
        ]);

        $this->actingAs($this->owner)
            ->withHeader('X-User-ID', $other->id)
            ->postJson('/api/v1/auth/change-password', [
                'current_password' => 'Original-Pass-1',
                'new_password' => 'Rotated-Pass-22',
                'new_password_confirmation' => 'Rotated-Pass-22',
                'email' => 'other@example.com',
            ])->assertStatus(200);

        $this->assertTrue(Hash::check('Rotated-Pass-22', $this->owner->fresh()->password));
        $this->assertTrue(Hash::check('Their-Own-Pass-9', $other->fresh()->password));
    }

    public function test_change_password_enforces_the_policy_and_keeps_this_session(): void
    {
        $token = $this->owner->createToken('web')->plainTextToken;
        $otherToken = $this->owner->createToken('phone')->plainTextToken;

        $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/auth/change-password', [
                'current_password' => 'Original-Pass-1',
                'new_password' => '12345678',
                'new_password_confirmation' => '12345678',
            ])->assertStatus(422);

        $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/auth/change-password', [
                'current_password' => 'Original-Pass-1',
                'new_password' => 'Rotated-Pass-22',
                'new_password_confirmation' => 'Rotated-Pass-22',
            ])->assertStatus(200);

        $this::assertTrue(Hash::check('Rotated-Pass-22', $this->owner->fresh()->password));

        // Still signed in here, signed out on the other device.
        $this->withFreshAuth()->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/auth/profile')->assertStatus(200);

        $this->withFreshAuth()->withHeader('Authorization', 'Bearer ' . $otherToken)
            ->getJson('/api/v1/auth/profile')->assertStatus(401);
    }

    public function test_change_password_requires_authentication(): void
    {
        $this->postJson('/api/v1/auth/change-password', [
            'current_password' => 'Original-Pass-1',
            'new_password' => 'Rotated-Pass-22',
            'new_password_confirmation' => 'Rotated-Pass-22',
            'email' => 'maryam@example.com',
        ])->assertStatus(401);
    }

    public function test_password_policy_rejects_common_and_short_passwords(): void
    {
        $this->postJson('/api/v1/auth/forgot-password', ['email' => 'maryam@example.com']);
        $token = $this->tokenFor('maryam@example.com');

        foreach (['password123', 'qwerty', 'Resto1234', 'Ab1!', '1234567890123'] as $weak) {
            $this->postJson('/api/v1/auth/reset-password', [
                'email' => 'maryam@example.com',
                'token' => $token,
                'password' => $weak,
                'password_confirmation' => $weak,
            ])->assertStatus(422);
        }

        $this->assertTrue(Hash::check('Original-Pass-1', $this->owner->fresh()->password));
    }

    public function test_password_policy_accepts_a_long_passphrase(): void
    {
        $token = $this->tokenFor('maryam@example.com');

        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => $token,
            'password' => 'یک رمز عبور بلند و فارسی',
            'password_confirmation' => 'یک رمز عبور بلند و فارسی',
        ])->assertStatus(200);
    }

    public function test_default_reset_notification_is_replaced_by_the_localized_one(): void
    {
        $this->assertNotNull(ResetPassword::class);
        $this->owner->sendPasswordResetNotification('some-token');

        Notification::assertSentTo($this->owner, ResetPasswordNotification::class);
    }

    public function test_password_reset_audit_trail_contains_no_token(): void
    {
        $token = $this->tokenFor('maryam@example.com');

        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'maryam@example.com',
            'token' => $token,
            'password' => 'Brand-New-Pass-77',
            'password_confirmation' => 'Brand-New-Pass-77',
        ])->assertStatus(200);

        $details = \App\Models\AuditLog::where('action', 'auth.password_reset')->pluck('details')->implode(' ');

        $this->assertStringNotContainsString($token, $details);
        $this->assertStringNotContainsString('Brand-New-Pass-77', $details);
    }

    public function test_email_lookup_is_case_insensitive(): void
    {
        $this->postJson('/api/v1/auth/forgot-password', ['email' => 'MARYAM@EXAMPLE.COM'])
            ->assertStatus(200);

        Notification::assertSentTo($this->owner, ResetPasswordNotification::class);
    }

    public function test_token_cannot_be_replayed_for_a_different_address(): void
    {
        $token = $this->tokenFor('maryam@example.com');

        $this->postJson('/api/v1/auth/reset-password', [
            'email' => 'stranger@example.com',
            'token' => $token,
            'password' => 'Brand-New-Pass-77',
            'password_confirmation' => 'Brand-New-Pass-77',
        ])->assertStatus(422);
    }

    public function test_reset_token_url_carries_a_long_random_value(): void
    {
        $token = $this->tokenFor('maryam@example.com');
        $this->assertGreaterThanOrEqual(60, strlen($token));
    }
}
