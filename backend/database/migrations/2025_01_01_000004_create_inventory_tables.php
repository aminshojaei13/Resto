<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('warehouse_stock', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('warehouse_id');
            $table->string('product_id');
            $table->string('product_variant_id')->nullable();
            $table->integer('quantity')->default(0);
            $table->integer('reserved_quantity')->default(0);
            $table->timestamps();

            $table->foreign('warehouse_id')->references('id')->on('warehouses')->onDelete('cascade');
            $table->foreign('product_id')->references('id')->on('products')->onDelete('cascade');
        });

        Schema::create('stock_movements', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('warehouse_id');
            $table->string('product_id');
            $table->string('product_variant_id')->nullable();
            $table->string('type'); // IN, OUT, ADJUSTMENT, TRANSFER
            $table->integer('quantity'); // positive or negative
            $table->decimal('unit_cost', 12, 2);
            $table->string('reason');
            $table->string('reference_id')->nullable();
            $table->timestamps();
        });

        Schema::create('stock_reservations', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('warehouse_id');
            $table->string('product_id');
            $table->integer('quantity');
            $table->string('reference_id');
            $table->timestamp('expires_at')->nullable();
            $table->timestamps();
        });

        Schema::create('stock_transfers', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('source_warehouse_id');
            $table->string('destination_warehouse_id');
            $table->string('status')->default('PENDING'); // PENDING, COMPLETED, CANCELLED
            $table->text('notes')->nullable();
            $table->timestamps();
        });

        Schema::create('stock_transfer_items', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('stock_transfer_id');
            $table->string('product_id');
            $table->integer('quantity');
            $table->timestamps();

            $table->foreign('stock_transfer_id')->references('id')->on('stock_transfers')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('stock_transfer_items');
        Schema::dropIfExists('stock_transfers');
        Schema::dropIfExists('stock_reservations');
        Schema::dropIfExists('stock_movements');
        Schema::dropIfExists('warehouse_stock');
    }
};
