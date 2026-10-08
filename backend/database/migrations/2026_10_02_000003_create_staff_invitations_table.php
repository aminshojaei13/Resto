<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('staff_invitations', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('email');
            $table->string('first_name');
            $table->string('last_name')->nullable();
            $table->string('role')->default('STAFF');
            // Hashed token — the raw token is only ever present in the invitation link.
            $table->string('token', 64)->unique();
            $table->string('invited_by');
            $table->string('status')->default('PENDING'); // PENDING, ACCEPTED, REVOKED
            $table->timestamp('expires_at')->nullable();
            $table->timestamp('accepted_at')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
            $table->index(['organization_id', 'email']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('staff_invitations');
    }
};
