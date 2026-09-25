<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('expenses', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('store_id');
            $table->string('category');
            $table->decimal('amount', 12, 2);
            $table->string('payment_method')->default('CASH');
            $table->date('date');
            $table->text('notes')->nullable();
            $table->string('user_id')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('idempotency_keys', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('key');
            $table->string('request_path');
            $table->integer('response_code');
            $table->longText('response_body');
            $table->timestamps();

            $table->unique(['organization_id', 'key']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('idempotency_keys');
        Schema::dropIfExists('expenses');
    }
};
