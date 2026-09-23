<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('organizations', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('name');
            $table->string('code')->unique();
            $table->string('logo_url')->nullable();
            $table->string('currency_symbol')->default('$');
            $table->string('currency_code')->default('USD');
            $table->string('subscription_tier')->default('ENTERPRISE');
            $table->timestamps();
        });

        Schema::create('organization_memberships', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('user_id');
            $table->string('role')->default('Member');
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
            $table->foreign('user_id')->references('id')->on('users')->onDelete('cascade');
        });

        Schema::create('roles', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('name');
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('permissions', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('role_id');
            $table->string('ability');
            $table->timestamps();

            $table->foreign('role_id')->references('id')->on('roles')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('permissions');
        Schema::dropIfExists('roles');
        Schema::dropIfExists('organization_memberships');
        Schema::dropIfExists('organizations');
    }
};
