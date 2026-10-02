<?php

namespace App\Providers;

use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\ServiceProvider;
use Illuminate\Support\Str;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        //
    }

    public function boot(): void
    {
        Schema::defaultStringLength(191);

        $this->configureRateLimiters();
        $this->configurePasswordResetDelivery();
    }

    /**
     * Rate limiting is enforced on the server, not by disabling buttons.
     *
     * There is no permanent lockout: an attacker cannot use the limiter to deny
     * service to a real user, and a legitimate user is slowed down only
     * briefly. Limits are keyed by email *and* IP so one attacker cannot lock
     * out a whole office behind a single shared address.
     */
    private function configureRateLimiters(): void
    {
        RateLimiter::for('login', function (Request $request) {
            $max = (int) config('resto.login.max_attempts', 10);
            $decay = (int) config('resto.login.decay_minutes', 1);

            return Limit::perMinutes($decay, $max)
                ->by(Str::lower((string) $request->input('email')) . '|' . $request->ip());
        });

        RateLimiter::for('password-reset', function (Request $request) {
            $max = (int) config('resto.password_reset.throttle_seconds', 60);

            return Limit::perMinutes(5, $max)
                ->by(Str::lower((string) $request->input('email')) . '|' . $request->ip());
        });

        RateLimiter::for('password-change', function (Request $request) {
            return Limit::perMinutes(10, 10)
                ->by((string) ($request->user()?->id ?? 'anon') . '|' . $request->ip());
        });
    }

    /**
     * The recovery link is built by the User model and points at the business
     * web application, never at the API host.
     */
    private function configurePasswordResetDelivery(): void
    {
        config([
            'auth.passwords.users.table' => 'password_reset_tokens',
            'auth.passwords.users.expire' => (int) config('resto.password_reset.expire_minutes', 60),
            'auth.passwords.users.throttle' => (int) config('resto.password_reset.throttle_seconds', 60),
        ]);
    }
}
