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
        Schema::table('suppliers', function (Blueprint $table) {
            $table->string('code')->nullable()->after('name');
            $table->string('tax_id')->nullable()->after('address');
            $table->text('notes')->nullable()->after('tax_id');

            $table->index(['organization_id', 'code']);
            $table->index(['organization_id', 'phone']);
            $table->index(['organization_id', 'email']);
        });

        Schema::table('purchases', function (Blueprint $table) {
            $table->string('external_purchase_id')->nullable()->after('purchase_number');
            $table->decimal('subtotal', 12, 2)->default(0)->after('purchase_date');
            $table->decimal('discount_amount', 12, 2)->default(0)->after('subtotal');
            $table->decimal('tax_amount', 12, 2)->default(0)->after('discount_amount');
            $table->text('notes')->nullable()->after('payment_status');
            $table->string('source')->default('PURCHASE')->after('notes');
            $table->boolean('is_historical')->default(false)->after('source');
            $table->string('import_job_id')->nullable()->after('is_historical');

            $table->index(['organization_id', 'is_historical']);
            $table->index(['organization_id', 'external_purchase_id']);
            $table->index(['organization_id', 'purchase_date']);
        });

        Schema::table('purchase_items', function (Blueprint $table) {
            $table->decimal('subtotal', 12, 2)->default(0)->after('total_cost');
            $table->decimal('discount_amount', 12, 2)->default(0)->after('subtotal');
            $table->decimal('tax_amount', 12, 2)->default(0)->after('discount_amount');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('purchase_items', function (Blueprint $table) {
            $table->dropColumn([
                'tax_amount',
                'discount_amount',
                'subtotal',
            ]);
        });

        Schema::table('purchases', function (Blueprint $table) {
            $table->dropIndex(['organization_id', 'purchase_date']);
            $table->dropIndex(['organization_id', 'external_purchase_id']);
            $table->dropIndex(['organization_id', 'is_historical']);

            $table->dropColumn([
                'import_job_id',
                'is_historical',
                'source',
                'notes',
                'tax_amount',
                'discount_amount',
                'subtotal',
                'external_purchase_id',
            ]);
        });

        Schema::table('suppliers', function (Blueprint $table) {
            $table->dropIndex(['organization_id', 'email']);
            $table->dropIndex(['organization_id', 'phone']);
            $table->dropIndex(['organization_id', 'code']);

            $table->dropColumn([
                'notes',
                'tax_id',
                'code',
            ]);
        });
    }
};
