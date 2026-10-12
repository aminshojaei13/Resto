<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->dateTime('order_date')->nullable()->after('store_id');
            $table->boolean('is_historical')->default(false)->after('source');
            $table->string('external_order_id')->nullable()->after('order_number');
            $table->string('import_job_id')->nullable()->after('is_historical');

            $table->index(['organization_id', 'is_historical']);
            $table->index(['organization_id', 'external_order_id']);
            $table->index(['organization_id', 'order_date']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->dropIndex(['organization_id', 'order_date']);
            $table->dropIndex(['organization_id', 'external_order_id']);
            $table->dropIndex(['organization_id', 'is_historical']);

            $table->dropColumn([
                'import_job_id',
                'is_historical',
                'external_order_id',
                'order_date',
            ]);
        });
    }
};
