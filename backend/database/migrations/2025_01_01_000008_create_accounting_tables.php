<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('chart_of_accounts', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('code')->unique();
            $table->string('name');
            $table->string('type'); // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('accounts', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('chart_of_account_id');
            $table->string('code');
            $table->string('name');
            $table->decimal('balance', 14, 2)->default(0);
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('journals', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('name'); // General Journal, Sales Journal, Purchase Journal
            $table->timestamps();
        });

        Schema::create('journal_entries', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('store_id');
            $table->string('entry_number')->unique();
            $table->string('journal_id')->nullable();
            $table->string('reference_type')->nullable(); // SalesOrder, Purchase, StockAdjustment
            $table->string('reference_id')->nullable();
            $table->text('description');
            $table->decimal('total_debit', 14, 2);
            $table->decimal('total_credit', 14, 2);
            $table->boolean('is_posted')->default(true);
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
        });

        Schema::create('journal_entry_lines', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('journal_entry_id');
            $table->string('account_id');
            $table->string('type'); // DEBIT or CREDIT
            $table->decimal('amount', 14, 2);
            $table->string('description')->nullable();
            $table->timestamps();

            $table->foreign('journal_entry_id')->references('id')->on('journal_entries')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('journal_entry_lines');
        Schema::dropIfExists('journal_entries');
        Schema::dropIfExists('journals');
        Schema::dropIfExists('accounts');
        Schema::dropIfExists('chart_of_accounts');
    }
};
