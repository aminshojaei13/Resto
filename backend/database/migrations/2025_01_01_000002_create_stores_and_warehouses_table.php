<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('stores', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('name');
            $table->string('code');
            $table->text('address')->nullable();
            $table->string('phone')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('warehouses', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('store_id');
            $table->string('organization_id');
            $table->string('name');
            $table->string('code');
            $table->text('address')->nullable();
            $table->timestamps();

            $table->foreign('store_id')->references('id')->on('stores')->onDelete('cascade');
            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('warehouses');
        Schema::dropIfExists('stores');
    }
};
