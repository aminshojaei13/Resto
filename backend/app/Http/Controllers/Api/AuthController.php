<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\OrganizationMembership;
use App\Models\User;
use App\Support\MembershipContext;
use App\Support\PasswordPolicy;
use App\Support\RolePermission;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Password;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;
use Laravel\Sanctum\PersonalAccessToken;

/**
 * Authentication and identity.
 *
 * Rules enforced here:
 *  - the current person is always `$request->user()`; there is no header,
 *    body, "first admin" or hardcoded fallback identity anywhere;
 *  - no endpoint reveals whether an email address exists;
 *  - passwords are never echoed, logged or returned;
 *  - every security relevant action is audit logged.
 */
class AuthController extends Controller
{
    private const GENERIC_AUTH_ERROR = 'ایمیل یا رمز عبور صحیح نیست.';
    private const GENERIC_RESET_MESSAGE = 'اگر این ایمیل در سامانه ثبت شده باشد، لینک بازیابی رمز عبور برای آن ارسال می‌شود.';

    public function login(Request $request)
    {
        $credentials = $request->validate([
            'email' => ['required', 'string', 'email', 'max:255'],
            'password' => ['required', 'string', 'max:' . PasswordPolicy::MAX_LENGTH],
            'device_name' => ['nullable', 'string', 'max:120'],
        ]);

        $email = Str::lower(trim($credentials['email']));
        $user = User::where('email', $email)->first();

        // Always run a hash comparison so a missing account and a wrong
        // password take a comparable amount of time.
        $hash = $user?->password ?? '$2y$12$usesomesillystringfore7hnbRJHxXVLeakoG8K30oukPsA.ztMG';

        if (!Hash::check($credentials['password'], $hash) || !$user) {
            $this->audit('auth.login_failed', null, $request, [
                'email' => $email,
                'reason' => 'invalid_credentials',
            ]);

            throw ValidationException::withMessages([
                'email' => [self::GENERIC_AUTH_ERROR],
            ]);
        }

        if (!$user->isActive()) {
            $this->audit('auth.login_blocked', $user, $request, ['reason' => 'inactive_account']);

            // Deliberately identical to the generic error: the existence of the
            // account is still not revealed.
            throw ValidationException::withMessages([
                'email' => [self::GENERIC_AUTH_ERROR],
            ]);
        }

        $token = $user->createToken($credentials['device_name'] ?? 'web')->plainTextToken;

        $this->audit('auth.login', $user, $request, []);

        return response()->json([
            'access_token' => $token,
            'token_type' => 'Bearer',
            'user' => $this->identityPayload($request, $user),
        ]);
    }

    /**
     * The authoritative profile of the authenticated caller.
     */
    public function profile(Request $request)
    {
        $user = $request->user();

        return response()->json($this->identityPayload($request, $user));
    }

    public function updateProfile(Request $request)
    {
        $user = $request->user();

        $validated = $request->validate([
            'first_name' => ['sometimes', 'nullable', 'string', 'max:120'],
            'last_name' => ['sometimes', 'nullable', 'string', 'max:120'],
            'phone' => ['sometimes', 'nullable', 'string', 'max:40'],
        ]);

        foreach (['first_name', 'last_name', 'phone'] as $field) {
            if (array_key_exists($field, $validated)) {
                $value = $validated[$field];
                $user->{$field} = is_string($value) && trim($value) !== '' ? trim($value) : null;
            }
        }

        // Keep the legacy single name column in sync so anything still reading
        // `name` shows the same person, never a different one.
        $user->name = $this->synchronisedName($user);
        $user->save();

        $this->audit('auth.profile_updated', $user, $request, ['fields' => array_keys($validated)]);

        return response()->json([
            'message' => 'اطلاعات حساب شما به‌روزرسانی شد.',
            'user' => $this->identityPayload($request, $user->fresh()),
        ]);
    }

    /**
     * Log out: revoke exactly the token that made this request.
     */
    public function logout(Request $request)
    {
        $user = $request->user();
        $token = $user->currentAccessToken();

        if ($token) {
            $token->delete();
        }

        $this->audit('auth.logout', $user, $request, []);

        return response()->json(['message' => 'از حساب کاربری خود خارج شدید.']);
    }

    /**
     * Log out everywhere: revoke every active token of the caller.
     */
    public function logoutAll(Request $request)
    {
        $user = $request->user();
        $count = $user->tokens()->count();

        $user->tokens()->delete();

        $this->audit('auth.logout_all', $user, $request, ['revoked_tokens' => $count]);

        return response()->json([
            'message' => 'از همه دستگاه‌ها خارج شدید.',
            'revoked_sessions' => $count,
        ]);
    }

    /**
     * Start a real password recovery flow.
     *
     * A cryptographically random, hashed, expiring, single-use token is created
     * by the framework password broker and emailed. The response is identical
     * whether or not the address exists and never contains a code or a link.
     */
    public function forgotPassword(Request $request)
    {
        $request->validate([
            'email' => ['required', 'string', 'email', 'max:255'],
        ]);

        $email = Str::lower(trim($request->email));
        $user = User::where('email', $email)->first();

        if ($user) {
            Password::broker()->sendResetLink(['email' => $email]);

            $this->audit('auth.password_reset_requested', $user, $request, []);
        } else {
            $this->audit('auth.password_reset_requested', null, $request, ['email' => $email, 'matched' => false]);
        }

        return response()->json([
            'message' => self::GENERIC_RESET_MESSAGE,
        ]);
    }

    /**
     * Consume a reset token. Single use, expiring, policy checked, and every
     * existing session of the user is revoked afterwards.
     */
    public function resetPassword(Request $request)
    {
        $request->validate([
            'email' => ['required', 'string', 'email', 'max:255'],
            'token' => ['required', 'string', 'max:512'],
            'password' => ['required', 'string', 'confirmed'],
        ]);

        $email = Str::lower(trim($request->email));
        $errors = PasswordPolicy::validate($request->input('password'));

        if ($errors !== []) {
            throw ValidationException::withMessages(['password' => $errors]);
        }

        $status = Password::broker()->reset(
            [
                'email' => $email,
                'token' => $request->token,
                'password' => $request->password,
                'password_confirmation' => $request->password,
            ],
            function (User $user, string $password) {
                $user->forceFill([
                    'password' => Hash::make($password),
                    'remember_token' => Str::random(60),
                ])->save();

                // A reset must invalidate every existing session.
                $user->tokens()->delete();
            }
        );

        if ($status !== Password::PASSWORD_RESET) {
            $user = User::where('email', $email)->first();
            $this->audit('auth.password_reset_failed', $user, $request, ['reason' => $status]);

            // One generic message for unknown address, wrong token and expired
            // token: the client learns nothing about which one it was.
            throw ValidationException::withMessages([
                'token' => ['لینک بازیابی رمز عبور نامعتبر یا منقضی شده است. لطفاً دوباره درخواست بازیابی بدهید.'],
            ]);
        }

        $user = User::where('email', $email)->first();
        $this->audit('auth.password_reset', $user, $request, []);

        return response()->json([
            'message' => 'رمز عبور شما با موفقیت تغییر کرد. اکنون می‌توانید وارد شوید.',
        ]);
    }

    /**
     * Change the password of the signed-in user.
     *
     * Requires the current password (re-authentication) and, by policy,
     * revokes all other sessions afterwards.
     */
    public function changePassword(Request $request)
    {
        $user = $request->user();

        $request->validate([
            'current_password' => ['required', 'string', 'max:' . PasswordPolicy::MAX_LENGTH],
            'new_password' => ['required', 'string', 'confirmed'],
        ]);

        if (!Hash::check($request->current_password, (string) $user->password)) {
            $this->audit('auth.change_password_failed', $user, $request, ['reason' => 'wrong_current_password']);

            throw ValidationException::withMessages([
                'current_password' => ['رمز عبور فعلی صحیح نیست.'],
            ]);
        }

        $errors = PasswordPolicy::validate($request->input('new_password'));

        if ($errors !== []) {
            throw ValidationException::withMessages(['new_password' => $errors]);
        }

        if (Hash::check($request->new_password, (string) $user->password)) {
            throw ValidationException::withMessages([
                'new_password' => ['رمز عبور جدید باید با رمز عبور فعلی متفاوت باشد.'],
            ]);
        }

        // Keep the caller's own session alive, sign out every other device.
        $currentToken = $user->currentAccessToken();
        $currentTokenId = $currentToken instanceof PersonalAccessToken ? $currentToken->id : null;

        $user->forceFill([
            'password' => Hash::make($request->new_password),
            'remember_token' => Str::random(60),
        ])->save();

        $user->tokens()
            ->when($currentTokenId !== null, fn ($query) => $query->where('id', '!=', $currentTokenId))
            ->delete();

        $this->audit('auth.password_changed', $user, $request, []);

        return response()->json([
            'message' => 'رمز عبور شما با موفقیت تغییر کرد.',
        ]);
    }

    /**
     * Everything the UI is allowed to know about the signed-in person.
     * Derived exclusively from the authenticated record and their memberships.
     */
    private function identityPayload(Request $request, User $user): array
    {
        // Built from the record that was just authenticated — never from
        // whatever the request resolver happens to hold.
        $memberships = MembershipContext::membershipsFor($user)
            ->filter(fn (OrganizationMembership $m) => !empty($m->organization));

        $organizations = $memberships->map(fn (OrganizationMembership $m) => [
            'id' => $m->organization_id,
            'name' => $m->organization->name,
            'role' => RolePermission::canonical($m->role),
            'status' => $m->status,
            'stores' => $m->organization->stores->map(fn ($store) => [
                'id' => $store->id,
                'name' => $store->name,
                'warehouses' => $store->warehouses->map(fn ($warehouse) => [
                    'id' => $warehouse->id,
                    'name' => $warehouse->name,
                ])->values(),
            ])->values(),
        ])->values();

        $primaryMembership = $memberships->first();

        return [
            'id' => $user->id,
            'first_name' => $user->first_name,
            'last_name' => $user->last_name,
            'name' => $user->full_name,
            'display_name' => $user->full_name !== '' ? $user->full_name : $user->email,
            'email' => $user->email,
            'phone' => $user->phone,
            'status' => $user->status ?? User::STATUS_ACTIVE,
            'is_platform_admin' => (bool) $user->is_platform_admin,
            'role' => $primaryMembership ? RolePermission::canonical($primaryMembership->role) : null,
            'permissions' => $primaryMembership ? RolePermission::forRole($primaryMembership->role) : [],
            'memberships' => $organizations,
            'organizations' => $organizations,
        ];
    }

    private function synchronisedName(User $user): string
    {
        $parts = array_filter([
            trim((string) $user->first_name),
            trim((string) $user->last_name),
        ], fn ($p) => $p !== '');

        if ($parts !== []) {
            return implode(' ', $parts);
        }

        return trim((string) $user->name) ?: (string) $user->email;
    }

    /**
     * Security audit trail. Deliberately never records passwords or tokens.
     */
    private function audit(string $action, ?User $user, Request $request, array $details): void
    {
        try {
            AuditLog::create([
                'id' => (string) Str::uuid(),
                'organization_id' => $details['organization_id'] ?? 'system',
                'user_id' => $user?->id,
                'action' => $action,
                'entity_type' => 'User',
                'entity_id' => $user?->id,
                'details' => json_encode($details, JSON_UNESCAPED_UNICODE) ?: '{}',
                'ip_address' => $request->ip(),
            ]);
        } catch (\Throwable $e) {
            // Auditing must never break the request it is recording.
            Log::warning('audit_log_write_failed', ['action' => $action, 'error' => $e->getMessage()]);
        }
    }
}
