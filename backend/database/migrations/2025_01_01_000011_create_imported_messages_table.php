<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('imported_messages', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('source')->default('manual_paste'); // manual_paste, android_share
            $table->text('raw_text');
            $table->string('status')->default('PARSED'); // PARSED, CONVERTED, FAILED
            $table->string('customer_id')->nullable();
            $table->string('order_id')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('imported_messages');
    }
};
