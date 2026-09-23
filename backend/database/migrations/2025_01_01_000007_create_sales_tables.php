<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('orders', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('order_number')->unique();
            $table->string('organization_id');
            $table->string('store_id');
            $table->string('warehouse_id');
            $table->string('customer_id')->nullable();
            $table->string('customer_name')->default('Walk-in Customer');
            $table->decimal('subtotal', 12, 2);
            $table->decimal('discount_amount', 12, 2)->default(0);
            $table->decimal('tax_amount', 12, 2)->default(0);
            $table->decimal('total_amount', 12, 2);
            $table->string('payment_method');
            $table->string('payment_status')->default('PAID'); // PAID, PENDING, REFUNDED
            $table->string('fulfillment_status')->default('COMPLETED'); // COMPLETED, PROCESSING, CANCELLED
            $table->text('notes')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('order_items', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('order_id');
            $table->string('product_id');
            $table->string('product_variant_id')->nullable();
            $table->string('product_name');
            $table->string('sku');
            $table->decimal('unit_price', 12, 2);
            $table->integer('quantity');
            $table->decimal('discount_percent', 5, 2)->default(0);
            $table->decimal('tax_amount', 12, 2)->default(0);
            $table->decimal('total_price', 12, 2);
            $table->timestamps();

            $table->foreign('order_id')->references('id')->on('orders')->onDelete('cascade');
        });

        Schema::create('payments', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('order_id');
            $table->decimal('amount', 12, 2);
            $table->string('payment_method');
            $table->string('status')->default('SUCCESS');
            $table->string('transaction_reference')->nullable();
            $table->timestamps();

            $table->foreign('order_id')->references('id')->on('orders')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('payments');
        Schema::dropIfExists('order_items');
        Schema::dropIfExists('orders');
    }
};
