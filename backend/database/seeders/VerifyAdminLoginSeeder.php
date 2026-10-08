<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Http;

/**
 * Proves the administrator account can actually sign in through the real
 * HTTP endpoint — not just that the row exists.
 *
 * Run with:  php artisan db:seed --class=VerifyAdminLoginSeeder
 */
class VerifyAdminLoginSeeder extends Seeder
{
    public function run(): void
    {
        $user = User::where('email', AdminUserSeeder::EMAIL)->first();

        if (! $user) {
            $this->command?->error('Administrator not found. Run AdminUserSeeder first.');

            return;
        }

        $this->command?->line('id:          ' . $user->id);
        $this->command?->line('email:       ' . $user->email);
        $this->command?->line('password:    ' . AdminUserSeeder::PASSWORD);
        $this->command?->line('status:      ' . $user->status);
        $this->command?->line('admin flag:  ' . ($user->is_platform_admin ? 'yes' : 'no'));
        $this->command?->line('hash check:  ' . (Hash::check(AdminUserSeeder::PASSWORD, $user->password) ? 'matches' : 'DOES NOT MATCH'));

        // In-process check of the real controller path.
        $request = \Illuminate\Http\Request::create('/api/v1/auth/login', 'POST', [
            'email' => AdminUserSeeder::EMAIL,
            'password' => AdminUserSeeder::PASSWORD,
            'locale' => 'fa',
        ]);
        $request->headers->set('Accept', 'application/json');

        $response = app(\App\Http\Controllers\Api\AuthController::class)->login($request);

        $this->command?->line('login status: ' . $response->getStatusCode());

        if ($response->getStatusCode() === 200) {
            $payload = json_decode($response->getContent(), true);
            $this->command?->info('LOGIN OK — name: ' . ($payload['user']['name'] ?? '?') . ', admin: ' . var_export($payload['user']['is_platform_admin'] ?? false, true));
        } else {
            $this->command?->error('LOGIN FAILED — ' . $response->getContent());
        }
    }
}