<?php

namespace Tests\Feature;

use Database\Seeders\MockDataSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

class BackupRestoreTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(MockDataSeeder::class);
    }

    public function test_database_schema_and_double_entry_balance_integrity(): void
    {
        // Verify key tables exist and have seeded data
        $this->assertDatabaseHas('organizations', ['id' => 'org_apex']);
        $this->assertDatabaseHas('users', ['email' => 'alex.mercer@calcuapp.com']);
        $this->assertDatabaseHas('products', ['sku' => 'APX-LAP-001']);
        $this->assertDatabaseHas('purchases', ['purchase_number' => 'PO-2025-1001']);
        $this->assertDatabaseHas('orders', ['order_number' => 'ORD-2025-1001']);

        // Verify Double-Entry Journal Entry Balance Invariant
        $debits = DB::table('journal_entries')->sum('total_debit');
        $credits = DB::table('journal_entries')->sum('total_credit');

        $this::assertEquals(
            round($debits, 2),
            round($credits, 2),
            "Double-Entry Accounting Invariant Violation: Total Debits ($debits) must equal Total Credits ($credits)"
        );
    }
}
