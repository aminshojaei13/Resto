<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('warehouse_stock', function (Blueprint $table) {
            $table->unique(
                ['organization_id', 'warehouse_id', 'product_id', 'product_variant_id'],
                'warehouse_stock_org_wh_prod_var_unique'
            );
        });
    }

    public function down(): void
    {
        Schema::table('warehouse_stock', function (Blueprint $table) {
            $table->dropUnique('warehouse_stock_org_wh_prod_var_unique');
        });
    }
};
