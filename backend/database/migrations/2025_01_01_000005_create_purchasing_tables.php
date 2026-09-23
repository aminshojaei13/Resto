<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('suppliers', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('name');
            $table->string('email')->nullable();
            $table->string('phone')->nullable();
            $table->text('address')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('purchases', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('store_id');
            $table->string('warehouse_id');
            $table->string('supplier_id');
            $table->string('purchase_number')->unique();
            $table->decimal('total_amount', 12, 2);
            $table->string('status')->default('RECEIVED'); // ORDERED, RECEIVED, CANCELLED
            $table->string('payment_status')->default('PAID'); // PAID, UNPAID, PARTIAL
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
            $table->foreign('supplier_id')->references('id')->on('suppliers')->onDelete('cascade');
        });

        Schema::create('purchase_items', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('purchase_id');
            $table->string('product_id');
            $table->integer('quantity');
            $table->decimal('unit_cost', 12, 2);
            $table->decimal('total_cost', 12, 2);
            $table->timestamps();

            $table->foreign('purchase_id')->references('id')->on('purchases')->onDelete('cascade');
        });

        Schema::create('supplier_payments', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('purchase_id');
            $table->decimal('amount', 12, 2);
            $table->string('payment_method');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('supplier_payments');
        Schema::dropIfExists('purchase_items');
        Schema::dropIfExists('purchases');
        Schema::dropIfExists('suppliers');
    }
};
