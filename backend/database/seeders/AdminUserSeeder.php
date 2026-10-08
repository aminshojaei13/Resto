<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

/**
 * A system administrator account for the administration console.
 *
 * Run with:  php artisan db:seed --class=AdminUserSeeder
 *
 * The account is created only if it does not already exist; running the seeder
 * again never resets a password that has since been changed.
 */
class AdminUserSeeder extends Seeder
{
    public const EMAIL = 'admin@gmail.com';

    /** Change this before using the account on anything reachable from outside. */
    public const PASSWORD = '2233';

    public function run(): void
    {
        $existing = User::where('email', self::EMAIL)->first();

        if ($existing) {
            $this->command?->info('Administrator already exists: ' . self::EMAIL);

            if (! $existing->is_platform_admin) {
                $existing->forceFill(['is_platform_admin' => true])->save();
                $this->command?->info('Granted platform administrator access.');
            }

            return;
        }

        User::create([
            'id' => (string) Str::uuid(),
            'name' => 'مدیر سامانه',
            'first_name' => 'مدیر',
            'last_name' => 'سامانه',
            'email' => self::EMAIL,
            'password' => Hash::make(self::PASSWORD),
            'role' => 'Owner',
            'status' => User::STATUS_ACTIVE,
            'is_platform_admin' => true,
            'preferred_locale' => 'fa',
        ]);

        $this->command?->info('Created administrator: ' . self::EMAIL);
    }
}