<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('organization_memberships', function (Blueprint $table) {
            if (!Schema::hasColumn('organization_memberships', 'status')) {
                $table->string('status')->default('ACTIVE')->after('role');
            }
            if (!Schema::hasColumn('organization_memberships', 'invited_by')) {
                $table->string('invited_by')->nullable()->after('status');
            }
            if (!Schema::hasColumn('organization_memberships', 'joined_at')) {
                $table->timestamp('joined_at')->nullable()->after('invited_by');
            }
        });
    }

    public function down(): void
    {
        Schema::table('organization_memberships', function (Blueprint $table) {
            foreach (['status', 'invited_by', 'joined_at'] as $column) {
                if (Schema::hasColumn('organization_memberships', $column)) {
                    $table->dropColumn($column);
                }
            }
        });
    }
};
