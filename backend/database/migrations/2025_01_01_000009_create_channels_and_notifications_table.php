<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('channels', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('type'); // POS, WEB, MOBILE_APP, ECOMMERCE
            $table->string('name');
            $table->string('status')->default('ACTIVE');
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('conversations', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('customer_id')->nullable();
            $table->string('subject')->nullable();
            $table->timestamps();
        });

        Schema::create('messages', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('conversation_id');
            $table->string('sender_type'); // USER, CUSTOMER, SYSTEM
            $table->string('sender_id')->nullable();
            $table->text('content');
            $table->timestamps();

            $table->foreign('conversation_id')->references('id')->on('conversations')->onDelete('cascade');
        });

        Schema::create('notifications', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('user_id')->nullable();
            $table->string('type'); // LOW_STOCK, ORDER_COMPLETED, SYSTEM
            $table->string('title');
            $table->text('body');
            $table->boolean('is_read')->default(false);
            $table->timestamps();
        });

        Schema::create('audit_logs', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('user_id')->nullable();
            $table->string('action'); // CREATE, UPDATE, DELETE, ADJUST_STOCK, CHECKOUT
            $table->string('entity_type');
            $table->string('entity_id');
            $table->text('details')->nullable();
            $table->string('ip_address')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('audit_logs');
        Schema::dropIfExists('notifications');
        Schema::dropIfExists('messages');
        Schema::dropIfExists('conversations');
        Schema::dropIfExists('channels');
    }
};
