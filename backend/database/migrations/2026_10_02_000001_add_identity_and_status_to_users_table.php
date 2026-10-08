<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Identity split: first_name/last_name alongside the legacy single name field.
        // Legacy `name` stays populated (kept in sync by the application) so
        // existing rows and reports keep showing the same person.
        Schema::table('users', function (Blueprint $table) {
            if (!Schema::hasColumn('users', 'first_name')) {
                $table->string('first_name')->nullable()->after('name');
            }
            if (!Schema::hasColumn('users', 'last_name')) {
                $table->string('last_name')->nullable()->after('first_name');
            }
            if (!Schema::hasColumn('users', 'status')) {
                $table->string('status')->default('ACTIVE')->after('is_platform_admin');
            }
            if (!Schema::hasColumn('users', 'preferred_locale')) {
                $table->string('preferred_locale', 5)->default('fa')->after('status');
            }
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            foreach (['first_name', 'last_name', 'status', 'preferred_locale'] as $column) {
                if (Schema::hasColumn('users', $column)) {
                    $table->dropColumn($column);
                }
            }
        });
    }
};
