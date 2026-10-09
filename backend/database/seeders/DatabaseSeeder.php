<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     * DatabaseSeeder is intentionally kept empty so production/development environments start completely clean without mock data.
     *
     * To seed mock data for manual testing or demonstration, run:
     * php artisan db:seed --class=MockDataSeeder
     */
    public function run(): void
    {
        $this->call(AdminUserSeeder::class);
        $this->call(MockDataSeeder::class);
    }
}
