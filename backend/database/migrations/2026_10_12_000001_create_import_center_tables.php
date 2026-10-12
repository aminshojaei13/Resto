<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('import_jobs', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('organization_id');
            $table->string('user_id')->nullable();
            $table->string('import_type'); // products, customers, inventory, expenses, orders, suppliers, purchases
            $table->string('status')->default('uploaded');
            $table->string('original_filename');
            $table->string('detected_format'); // xlsx, csv
            $table->unsignedBigInteger('file_size')->default(0);

            // Progress counters
            $table->integer('total_rows')->default(0);
            $table->integer('processed_rows')->default(0);
            $table->integer('valid_rows')->default(0);
            $table->integer('invalid_rows')->default(0);
            $table->integer('imported_rows')->default(0);
            $table->integer('skipped_rows')->default(0);
            $table->integer('failed_rows')->default(0);

            $table->text('error_summary')->nullable();
            $table->string('idempotency_key')->nullable();
            $table->json('mapping_config')->nullable();
            $table->json('metadata')->nullable();

            $table->timestamp('started_at')->nullable();
            $table->timestamp('completed_at')->nullable();
            $table->timestamps();

            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
            $table->foreign('user_id')->references('id')->on('users')->onDelete('set null');

            $table->index(['organization_id', 'status']);
            $table->index(['organization_id', 'created_at']);
            $table->index(['organization_id', 'idempotency_key']);
        });

        Schema::create('import_sources', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('import_job_id');
            $table->string('organization_id');
            $table->string('original_filename');
            $table->string('storage_path');
            $table->string('file_format');
            $table->unsignedBigInteger('file_size');
            $table->string('checksum', 64);
            $table->string('upload_status')->default('uploaded');
            $table->json('metadata')->nullable();
            $table->timestamps();

            $table->foreign('import_job_id')->references('id')->on('import_jobs')->onDelete('cascade');
            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
            $table->index(['organization_id', 'import_job_id']);
        });

        Schema::create('import_datasets', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('import_job_id');
            $table->string('import_source_id');
            $table->string('organization_id');
            $table->string('name');
            $table->integer('sheet_index')->default(0);
            $table->integer('row_count')->default(0);
            $table->json('headers')->nullable();
            $table->json('sample_rows')->nullable();
            $table->string('status')->default('detected');
            $table->json('metadata')->nullable();
            $table->timestamps();

            $table->foreign('import_job_id')->references('id')->on('import_jobs')->onDelete('cascade');
            $table->foreign('import_source_id')->references('id')->on('import_sources')->onDelete('cascade');
            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
            $table->index(['organization_id', 'import_job_id']);
        });

        Schema::create('import_validation_results', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->string('import_job_id');
            $table->string('organization_id');
            $table->string('source_file_id')->nullable();
            $table->string('worksheet_name')->nullable();
            $table->integer('row_number');
            $table->string('field_name')->nullable();
            $table->text('original_value')->nullable();
            $table->string('error_code', 64);
            $table->text('message');
            $table->text('suggested_action')->nullable();
            $table->string('severity')->default('error'); // error, warning
            $table->boolean('is_eligible')->default(false);
            $table->timestamps();

            $table->foreign('import_job_id')->references('id')->on('import_jobs')->onDelete('cascade');
            $table->foreign('organization_id')->references('id')->on('organizations')->onDelete('cascade');
            $table->index(['import_job_id', 'row_number']);
            $table->index(['organization_id', 'error_code']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('import_validation_results');
        Schema::dropIfExists('import_datasets');
        Schema::dropIfExists('import_sources');
        Schema::dropIfExists('import_jobs');
    }
};
