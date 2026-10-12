# Resto P14.5 — Historical Orders Import: Implementation & Safety Audit Report

**Status**: `READY`  
**Domain Contract**: `HISTORICAL_RECORD_ONLY`  
**Phase**: P14.5 — Historical Orders Import  
**Branch**: `Products-Customers-Import`  
**Date**: 2026-10-12  

---

## 1. Executive Summary

In response to the domain resolution of the initial `BLOCKED` status, the **`HISTORICAL_RECORD_ONLY`** domain contract was designed and strictly implemented for Resto's Import Center. Historical orders represent closed past transactions intended for customer purchase history, auditing, and historical business analytics. They must never trigger live POS or real-time ledger side effects.

### Critical Invariants Enforced
1. **Zero Live Inventory Impact**:
   - `WarehouseStock` is never mutated.
   - Zero `StockMovement` records are created.
   - P14.3 Opening Inventory physical count balances remain 100% pristine.
2. **Zero Live Accounting Impact**:
   - No Journal Entries are posted to live ledger accounts (Account `1010` Cash or Account `4010` Sales Revenue).
   - Operational revenue reports and P&L statements (`AccountingController::summary`, `AccountingController::profitAndLoss`) strictly filter out historical orders (`where('is_historical', false)`).
3. **Zero Live Payment Impact**:
   - No active `Payment` records are created that could pollute today's cash registers or end-of-day register closures.
4. **Multi-Line Order Grouping**:
   - Multiple rows sharing the same `order_id` in CSV/XLSX are safely grouped into a single parent `Order` with corresponding `OrderItem` lines.
5. **Exact Decimal Monetary Arithmetic**:
   - All price, discount, tax, subtotal, and total comparisons strictly utilize exact integer minor-unit math (`App\Support\Money`).
6. **Strict Idempotency**:
   - Deterministic UUIDs (`deterministicUuid("resto_imp_{org}_{job}_ord_{id}")`) guarantee that re-running or retrying an import execution never creates duplicate orders or order items.

---

## 2. Implementation Architecture

### 2.1 Database Schema & Migration
* **Migration**: `backend/database/migrations/2026_10_12_181622_add_historical_and_date_fields_to_orders_table.php`
* **Added Fields to `orders` Table**:
  - `order_date` (`TIMESTAMP NULL`): Preserves the true historical transaction date distinct from system ingestion time.
  - `is_historical` (`BOOLEAN DEFAULT false`, indexed): Flags imported past orders and enables index-accelerated operational filtering.
  - `external_order_id` (`VARCHAR(100) NULL`): Original identifier from legacy systems/spreadsheets, indexed with `organization_id`.
  - `import_job_id` (`UUID NULL`): Foreign reference linking the order to its originating `ImportJob`.
* **Model Casts**: Updated `App\Models\Order` with `$fillable` fields and cast `'is_historical' => 'boolean'`, `'order_date' => 'datetime'`.

### 2.2 Operational Reporting Isolation
* **`AccountingController::summary`**:
  - Daily sales count and daily revenue explicitly scope out historical records:
    ```php
    $todayOrders = Order::where('organization_id', $orgId)
        ->where('is_historical', false)
        ->whereDate('created_at', $today)
        ->where('status', 'COMPLETED');
    ```
* **`AccountingController::profitAndLoss`**:
  - Direct order revenue fallback queries enforce `where('is_historical', false)`.

### 2.3 Import Center Services Extended
1. **`ImportMappingService`**:
   - Defined target fields for `ImportType::ORDERS`:
     - Mandatory: `order_id`, `sku`, `quantity`, `unit_price`, `date`
     - Optional: `discount_amount`, `tax_amount`, `total_amount`, `customer`, `store`, `payment_method`, `status`, `notes`
   - Added Persian and English aliases (`شماره سفارش`, `کد کالا`, `تعداد`, `قیمت واحد`, `تاریخ`, `تخفیف`, `مالیات`, `مبلغ کل`, etc.).
2. **`ImportJobController`**:
   - Extended `getMapping` to provide tenant stores and warehouses for default selection.
   - Extended `saveMapping` to validate that selected default `store_id` belongs strictly to the authenticated tenant.
3. **`ImportValidationService` (`validateOrders`)**:
   - Streams CSV/XLSX rows and groups them into multi-line orders by `order_id`.
   - Validates that every line SKU exists in the active tenant's catalog.
   - Validates positive integers for line quantities and valid money formats for prices.
   - Validates internal consistency across order lines: identical dates, identical customers, identical stores.
   - Calculates mathematical totals (`sum(unit_price * qty - discount + tax)`) and flags discrepancies with header `total_amount` (`ORDER_TOTAL_MISMATCH`).
   - Detects existing orders in the system (`EXISTING_RECORD_MATCH`).
4. **`ImportExecutionService` (`executeOrders`)**:
   - Executes order groups inside atomic database transactions.
   - Enforces duplicate strategies:
     - `skip`: Preserves existing database order unmodified.
     - `update`: Updates only mutable metadata (`notes`, `payment_method`, `customer_id`) while preserving immutable monetary totals and tenant ownership.
   - Creates historical orders directly using `is_historical = true`, `source = 'IMPORT_HISTORICAL'`, and explicit `order_date`.
   - Populates child `OrderItem` records with unit price, subtotal, and tax amounts.
   - Completely bypasses `InventoryService`, `AccountingService`, and `SalesOrderController::checkout`, ensuring 0 side effects.

### 2.4 Web UI Integration
* **`web/src/pages/ImportCenterPage.tsx`**:
  - Enabled `orders` dataset in `IMPORT_TYPES` with shopping cart icon and bilingual descriptions.
  - Added Default Store selector dropdown on the mapping step for order imports.

---

## 3. Verification & Test Suite

### 3.1 Dedicated Test Suite: `HistoricalOrdersImportTest`
A comprehensive feature test suite was implemented in `backend/tests/Feature/HistoricalOrdersImportTest.php`:
1. `test_mapping_service_suggests_and_validates_order_fields`: Verifies Persian and English alias mapping and required field rules.
2. `test_orders_validation_groups_multiline_orders_and_detects_errors`: Validates multi-line row grouping, SKU lookup, missing ID detection, and error logging.
3. `test_execution_creates_orders_without_inventory_or_accounting_side_effects`:
   - Asserts historical order and items are created.
   - Asserts `WarehouseStock::quantity` remains identical (50 before == 50 after).
   - Asserts `StockMovement::count()` is unchanged (+0).
   - Asserts Account `1010` and Account `4010` balances are unchanged.
   - Asserts `JournalEntry::count()` is unchanged (+0).
   - Asserts `Payment::count()` is unchanged (+0).
4. `test_accounting_summary_excludes_historical_orders_from_daily_and_pl_metrics`: Asserts live KPI dashboards exclude historical imports.
5. `test_duplicate_strategy_skip_and_update_behavior`: Asserts `skip` does not alter records, and `update` updates permitted fields without altering financials.
6. `test_tenant_isolation_prevents_foreign_store_or_cross_tenant_access`: Asserts tenants cannot reference foreign stores or cross-tenant products.
7. `test_normal_pos_checkout_pipeline_remains_fully_unaffected`: Asserts that normal live POS checkout continues to decrement stock, create payments, and post journals normally.

### 3.2 Verification Results
* **Historical Orders Suite**:
  ```
  PASS  Tests\Feature\HistoricalOrdersImportTest
  ✓ mapping service suggests and validates order fields                  0.14s  
  ✓ orders validation groups multiline orders and detects errors         0.03s  
  ✓ execution creates orders without inventory or accounting side effec… 0.02s  
  ✓ accounting summary excludes historical orders from daily and pl met… 0.03s  
  ✓ duplicate strategy skip and update behavior                          0.02s  
  ✓ tenant isolation prevents foreign store or cross tenant access       0.02s  
  ✓ normal pos checkout pipeline remains fully unaffected                0.02s  

  Tests: 7 passed (53 assertions)
  ```
* **Full Backend Regression Suite**:
  ```
  Tests: 179 passed (871 assertions)
  Duration: 6.23s
  ```
* **Web Test Suite**:
  ```
  Test Suites: 2 passed, 2 total
  Tests:       47 passed, 47 total
  ```
* **Web Production Build**:
  - `BUILD_PATH=build/business react-scripts build`: Compiled successfully (112.79 kB gzip).
  - `BUILD_PATH=build/admin react-scripts build`: Compiled successfully (65.55 kB gzip).

---

## 4. Final Verdict

**VERDICT: `READY`**  
The Historical Orders Import feature operates under the safe, audited `HISTORICAL_RECORD_ONLY` domain contract. Zero side effects on inventory, accounting, or payments are guaranteed. All regression tests pass without flaw.
