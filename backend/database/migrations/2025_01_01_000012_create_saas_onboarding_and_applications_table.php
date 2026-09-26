<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('business_applications', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('business_name');
            $table->string('owner_name');
            $table->string('email');
            $table->string('phone')->nullable();
            $table->string('business_type')->default('RETAIL');
            $table->string('country')->default('IR');
            $table->string('city')->nullable();
            $table->text('address')->nullable();
            $table->text('notes')->nullable();
            $table->string('status')->default('PENDING'); // PENDING, APPROVED, REJECTED
            $table->text('rejection_reason')->nullable();
            $table->string('organization_id')->nullable();
            $table->string('reviewed_by')->nullable();
            $table->timestamp('reviewed_at')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('set null');
        });

        Schema::table('users', function (Blueprint $table) {
            if (!Schema::hasColumn('users', 'is_platform_admin')) {
                $table->boolean('is_platform_admin')->default(false)->after('role');
            }
        });

        Schema::table('organizations', function (Blueprint $table) {
            if (!Schema::hasColumn('organizations', 'business_type')) {
                $table->string('business_type')->default('RETAIL')->after('subscription_tier');
            }
            if (!Schema::hasColumn('organizations', 'onboarding_status')) {
                $table->string('onboarding_status')->default('IN_PROGRESS')->after('business_type'); // IN_PROGRESS, COMPLETED
            }
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('business_applications');

        Schema::table('users', function (Blueprint $table) {
            if (Schema::hasColumn('users', 'is_platform_admin')) {
                $table->dropColumn('is_platform_admin');
            }
        });

        Schema::table('organizations', function (Blueprint $table) {
            if (Schema::hasColumn('organizations', 'business_type')) {
                $table->dropColumn('business_type');
            }
            if (Schema::hasColumn('organizations', 'onboarding_status')) {
                $table->dropColumn('onboarding_status');
            }
        });
    }
};
