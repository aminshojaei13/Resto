<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * A purchase order says what was agreed with a supplier; it does not say which
 * warehouse the goods will land in. That is decided when the delivery arrives,
 * and a delivery can be split across warehouses.
 *
 * Making the column nullable is what allows "سفارش خرید" and "دریافت کالا" to
 * be genuinely separate operations. Existing rows keep the warehouse they
 * already had.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('purchases', function (Blueprint $table) {
            $table->string('warehouse_id')->nullable()->change();
        });
    }

    public function down(): void
    {
        Schema::table('purchases', function (Blueprint $table) {
            $table->string('warehouse_id')->nullable(false)->change();
        });
    }
};