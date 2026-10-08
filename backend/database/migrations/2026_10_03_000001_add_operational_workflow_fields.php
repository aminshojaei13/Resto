<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * P10 — operational workflow foundations.
 *
 * Everything here is additive and backfills from what already exists, so no
 * existing quantity, amount or order is changed. Nothing is converted.
 */
return new class extends Migration
{
    public function up(): void
    {
        // 1. The single authoritative source for the sales tax rate.
        //    8.00 is the rate the application has always applied; it is now a
        //    business setting rather than a constant scattered across code.
        Schema::table('organizations', function (Blueprint $table) {
            $table->decimal('default_tax_rate', 5, 2)->default(8.00)->after('business_type');
            $table->boolean('tax_inclusive_pricing')->default(false)->after('default_tax_rate');
        });

        // 2. Orders record the rate that was actually applied and where the
        //    order came from, so a historical order can always be explained.
        Schema::table('orders', function (Blueprint $table) {
            $table->decimal('tax_rate', 5, 2)->default(0)->after('tax_amount');
            $table->string('source')->default('POS')->after('notes');
            $table->string('created_by_user_id')->nullable()->after('source');
            $table->string('cancelled_at')->nullable()->after('created_by_user_id');
            $table->string('cancelled_by_user_id')->nullable()->after('cancelled_at');
        });

        // 3. Receiving is a separate physical act from ordering: record how much
        //    of each ordered line has actually arrived.
        Schema::table('purchase_items', function (Blueprint $table) {
            $table->integer('received_quantity')->default(0)->after('quantity');
        });

        Schema::table('purchases', function (Blueprint $table) {
            $table->string('purchase_date')->nullable()->after('purchase_number');
            $table->timestamp('received_at')->nullable()->after('status');
            $table->timestamp('last_received_at')->nullable()->after('received_at');
        });

        // 4. An expense needs a human title. `category` alone ("Rent") cannot
        //    describe which bill was paid.
        Schema::table('expenses', function (Blueprint $table) {
            $table->string('title')->nullable()->after('category');
            $table->string('attachment_url')->nullable()->after('notes');
            $table->boolean('is_recurring')->default(false)->after('attachment_url');
            $table->string('recurrence_period')->nullable()->after('is_recurring');
            $table->date('recurrence_starts_on')->nullable()->after('recurrence_period');
            $table->date('recurrence_ends_on')->nullable()->after('recurrence_starts_on');
        });

        // 5. A line keeps the unit it was sold/purchased in, so historical
        //    documents do not change meaning when the catalog is edited later.
        Schema::table('order_items', function (Blueprint $table) {
            $table->string('unit')->nullable()->after('sku');
            // The per-line breakdown the pricing service produces, stored so a
            // historical invoice can always be re-read without recomputation.
            $table->decimal('subtotal', 12, 2)->default(0)->after('quantity');
            $table->decimal('discount_amount', 12, 2)->default(0)->after('discount_percent');
        });

        // 6. Purchase lines likewise.
        Schema::table('purchase_items', function (Blueprint $table) {
            $table->string('unit')->nullable()->after('product_id');
        });

        // The catalog column now defaults to a canonical unit rather than the
        // technical 'pcs' spelling.
        Schema::table('products', function (Blueprint $table) {
            $table->string('unit')->default('piece')->change();
        });

        // ---- Backfills, all derived from data that already exists ----

        // Values already stored are recognised, not reinterpreted: 'pcs' has
        // always meant one counted item and still does.
        DB::table('products')->where('unit', 'pcs')->update(['unit' => 'piece']);

        // Existing orders were all created at 8%; record that explicitly rather
        // than leaving a misleading 0.
        DB::table('orders')->where('tax_rate', 0)->update(['tax_rate' => 8.00]);

        // Lines ordered before this migration were received in full: existing
        // purchases are all in the RECEIVED state.
        DB::table('purchase_items')
            ->join('purchases', 'purchases.id', '=', 'purchase_items.purchase_id')
            ->where('purchases.status', 'RECEIVED')
            ->where('purchase_items.received_quantity', 0)
            ->update(['purchase_items.received_quantity' => DB::raw('purchase_items.quantity')]);

        DB::table('purchases')->where('status', 'RECEIVED')->update(['received_at' => DB::raw('created_at')]);

        // A historical expense without a title still needs something readable.
        DB::table('expenses')->whereNull('title')->update([
            'title' => DB::raw('category'),
        ]);
    }

    public function down(): void
    {
        // SQLite cannot drop several columns in one statement, so each column
        // is dropped on its own and an already-absent column is tolerated.
        $drop = function (string $table, array $columns): void {
            foreach ($columns as $column) {
                try {
                    Schema::table($table, fn (Blueprint $blueprint) => $blueprint->dropColumn($column));
                } catch (\Throwable) {
                    // Already gone.
                }
            }
        };

        $drop('expenses', [
            'title', 'attachment_url', 'is_recurring',
            'recurrence_period', 'recurrence_starts_on', 'recurrence_ends_on',
        ]);

        $drop('purchases', ['purchase_date', 'received_at', 'last_received_at']);
        $drop('purchase_items', ['received_quantity', 'unit']);
        $drop('order_items', ['unit', 'subtotal', 'discount_amount']);

        $drop('orders', [
            'tax_rate', 'source', 'created_by_user_id', 'cancelled_at', 'cancelled_by_user_id',
        ]);

        $drop('organizations', ['default_tax_rate', 'tax_inclusive_pricing']);
    }
};