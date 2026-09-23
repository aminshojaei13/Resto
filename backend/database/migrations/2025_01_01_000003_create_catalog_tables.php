<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('categories', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('name');
            $table->string('slug');
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('brands', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('name');
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('products', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('sku');
            $table->string('barcode')->index();
            $table->string('name');
            $table->text('description')->nullable();
            $table->decimal('price', 12, 2);
            $table->decimal('cost_price', 12, 2);
            $table->string('category');
            $table->string('unit')->default('pcs');
            $table->string('image_url')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('product_variants', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('product_id');
            $table->string('sku');
            $table->string('barcode')->index();
            $table->string('name');
            $table->decimal('price', 12, 2);
            $table->decimal('cost_price', 12, 2);
            $table->timestamps();

            $table->foreign('product_id')->references('id')->on('products')->onDelete('cascade');
        });

        Schema::create('product_images', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('product_id');
            $table->string('image_url');
            $table->boolean('is_primary')->default(false);
            $table->timestamps();

            $table->foreign('product_id')->references('id')->on('products')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('product_images');
        Schema::dropIfExists('product_variants');
        Schema::dropIfExists('products');
        Schema::dropIfExists('brands');
        Schema::dropIfExists('categories');
    }
};
