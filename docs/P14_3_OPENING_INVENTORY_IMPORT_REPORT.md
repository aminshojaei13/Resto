# Resto P14.3 — Opening Inventory Import Implementation & Verification Report

**Branch:** `Products-Customers-Import`  
**Date:** October 12, 2026  
**Status:** COMPLETE (All Tests Passing, Verified, Staged Explicitly)

---

## 1. Executive Summary

Phase P14.3 implements tenant-isolated, ledger-backed **Opening Inventory Import** on top of the Import Center infrastructure established in P14.1 and P14.2.

Prior to implementation, the mandatory **P14.2 Safety Gate** was performed, audited, and verified against actual codebase constraints (covering stale mapping invalidation, repeated execution idempotency, and update-strategy tenant isolation).

The Opening Inventory module allows tenant merchants to migrate their opening stock balances from Excel (`.xlsx`) or CSV files into Resto with strict domain integrity:
* **Ledger-Backed Accuracy:** Stock balances are never directly altered without an audit trail; every adjustment atomically updates `warehouse_stock` and creates an audit movement in `stock_movements`.
* **Zero Fake Transactions:** No pseudo-orders, fake invoices, artificial purchases, or mock expenses are created.
* **Pre-Existing Product Requirement:** Products must already exist within the active tenant; unknown identifiers trigger row validation errors (`ERR_PRODUCT_NOT_FOUND`).
* **Warehouse Flexibility:** Supports row-level warehouse mapping (matching by code, name, or ID) with fallback to a user-selected default warehouse.
* **Concurrency & Duplicate Safety:** Employs row-level pessimistic locking (`lockForUpdate()`), in-file `(product, warehouse)` duplicate rejection, and user-confirmed `skip` vs `update` duplicate strategies.
* **Precision & Localization:** Persian and Arabic digits are normalized, non-positive or fractional quantities are rejected, and bidirectional UI (Persian RTL / English LTR) is supported.

---

## 2. Part A: P14.2 Safety Gate Audit & Verification

The P14.2 implementation was audited against core safety invariants:
1. **Duplicate Handling:** Verified that `skip` strategy ignores existing records without modifying attributes, and `update` strategy updates only mapped fields while preserving tenant ownership and unmapped data.
2. **Idempotent Job Execution:** Confirmed that repeated execution requests on completed or failed jobs are rejected (422) by the state machine.
3. **Stale Mapping Invalidation:** Changing column mappings clears old validation results, resets progress counters, and purges stale preview data.
4. **Tenant Isolation:** Scoped queries prevent access, matching, or modification of foreign tenant products or customers.
5. **File Parser Hardening:** `SpreadsheetReaderService` was reinforced with XXE protection flags (`LIBXML_NONET | LIBXML_NOWARNING | LIBXML_NOERROR`) and formula caching safeguards.

---

## 3. Part B: P14.3 Opening Inventory Architecture

### 3.1 Domain Mapping (`ImportMappingService`)
Target fields configured for `ImportType::INVENTORY`:
* `sku` *(required: true)*: Canonical product SKU or code. Aliases: `کد`, `کد کالا`, `کد محصول`, `شناسه`, `شناسه کالا`, `sku`, `code`.
* `quantity` *(required: true)*: Opening balance count. Aliases: `موجودی`, `موجودی اولیه`, `تعداد`, `مقدار`, `quantity`, `qty`, `stock`.
* `warehouse` *(required: false)*: Warehouse name or code. Aliases: `انبار`, `کد انبار`, `نام انبار`, `warehouse`, `location`.
* `barcode` *(required: false)*: Product barcode for secondary identification. Aliases: `بارکد`, `کد میله‌ای`, `barcode`.
* `unit_cost` *(required: false)*: Unit cost for stock movement valuation. Aliases: `قیمت خرید`, `بهای تمام شده`, `cost`, `unit cost`.
* `notes` *(required: false)*: Audit remark or adjustment reason. Aliases: `توضیحات`, `یادداشت`, `notes`.

### 3.2 Product & Warehouse Resolution
* **Product Matching:** Scoped strictly to the active tenant (`organization_id`). Matches primarily by `sku`, secondarily by `barcode`. Ambiguous multiple matches trigger `ERR_AMBIGUOUS_PRODUCT`; unmatched records trigger `ERR_PRODUCT_NOT_FOUND`.
* **Warehouse Resolution:** Scoped strictly to the active tenant. Resolves by `code`, `name`, or `id`. If unmapped in the row, falls back to the job's `warehouse_id` specified during mapping confirmation. If neither is available, triggers `ERR_WAREHOUSE_NOT_FOUND`.

### 3.3 Numeric Parsing & In-File Duplicate Checks
* **Digits & Formats:** Converted via `DataNormalizer::toEnglishDigits`. Quantities must be positive integers (`> 0`). Non-digits or `<= 0` trigger `ERR_INVALID_QUANTITY`.
* **In-File Duplicates:** The pair `(product_id, warehouse_id)` is tracked. If the same product and warehouse appear multiple times in a single import file, subsequent occurrences trigger `ERR_DUPLICATE_IN_FILE`.

### 3.4 Concurrency-Safe Ledger Mutation (`ImportExecutionService` & `InventoryService`)
* Stock adjustments occur within atomic database transactions with pessimistic locking (`WarehouseStock::lockForUpdate()`).
* Mutations invoke `InventoryService::adjustStock(...)`, which updates `warehouse_stock.quantity` and writes a `StockMovement` ledger entry with type (`IN` or `ADJUSTMENT`), movement quantity, unit cost, reason (`موجودی اولیه (درون‌ریزی)` or user notes), and reference ID pointing to `import_job_id`.
* **Conflict Resolution Modes:**
  * **`skip`:** If the product already has stock (`quantity > 0`) in that warehouse, the row is skipped without changing balances (`skipped_rows++`).
  * **`update`:** Calculates the delta between target opening quantity and current balance (`target - current`), creating an adjustment movement to set the inventory balance exactly to the target count (`imported_rows++`).

---

## 4. Web Frontend & API Integration

* **API Endpoints:**
  * `GET /api/v1/import/jobs/{id}/mapping`: Returns target fields, suggested mappings, and available tenant warehouses (`warehouses`).
  * `POST /api/v1/import/jobs/{id}/mapping`: Accepts `fields`, `sheet_index`, and optional `warehouse_id`.
  * `POST /api/v1/import/jobs/{id}/validate`: Runs row-level and field-level inventory validation, generating warnings for existing stock and errors for invalid rows.
  * `GET /api/v1/import/jobs/{id}/preview`: Supplies preview rows with parsed SKU, product name, quantity, warehouse, unit cost, and validation status.
  * `POST /api/v1/import/jobs/{id}/execute`: Executes opening inventory adjustments with `duplicate_strategy: 'skip' | 'update'`.
* **Web UI (`ImportCenterPage.tsx`):**
  * Enabled `inventory` (`موجودی اولیه انبار`) in upload dataset selector.
  * Added default warehouse selection dropdown in the column mapping step when `import_type === 'inventory'`.
  * Preview table and summary chips display valid rows, invalid rows, and existing balance matches with color-coded badges.

---

## 5. Verification & Test Suite

### 5.1 Automated Feature Tests
* **`tests/Feature/InventoryImportTest.php`** (6 tests, 44 assertions — **ALL PASSED**):
  1. `test_mapping_service_suggests_and_validates_inventory_fields`: Validates English and Persian aliases and enforces required fields.
  2. `test_api_mapping_get_and_save_with_warehouse_support`: Verifies API warehouse delivery and storage of default warehouse ID.
  3. `test_inventory_validation_pipeline_detects_errors_and_conflicts`: Confirms detection of unknown products, negative quantities, in-file duplicates, Persian digit normalization, and existing balance warnings.
  4. `test_inventory_execution_skip_mode_preserves_existing_balances`: Tests that pre-existing stock is preserved and skipped in `skip` mode.
  5. `test_inventory_execution_update_mode_adjusts_balances_atomically`: Tests delta calculation and ledger movement generation in `update` mode.
  6. `test_tenant_isolation_prevents_matching_or_modifying_foreign_inventory`: Confirms foreign tenant products cannot be resolved or mutated.

### 5.2 Full Test Suite Runs
* **Backend Suite:** `152 passed` (723 assertions, 0 failures).
* **Web Test Suite:** `2 passed` (47 passed tests, 0 failures).
* **Web Production Build:** `npm run build` compiled both business and admin bundles successfully.

---

## 6. Manifest of Staged Files

The following files represent the exact scope of P14.3:
* `backend/app/Http/Controllers/Api/Import/ImportJobController.php`
* `backend/app/Services/Import/Execution/ImportExecutionService.php`
* `backend/app/Services/Import/Mapping/ImportMappingService.php`
* `backend/app/Services/Import/Validation/ImportValidationService.php`
* `backend/app/Services/InventoryService.php`
* `backend/app/Support/Import/ImportErrorCode.php`
* `backend/tests/Feature/InventoryImportTest.php`
* `docs/api/openapi.yaml`
* `web/src/api/apiClient.ts`
* `web/src/pages/ImportCenterPage.tsx`
* `web/src/types/index.ts`
* `docs/P14_3_OPENING_INVENTORY_IMPORT_REPORT.md`
