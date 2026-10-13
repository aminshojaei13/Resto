# Resto P14.6 — Suppliers & Purchases Import: Implementation & Safety Audit Report

**Status**: `READY`  
**Domain Contract**: `HISTORICAL_RECORD_ONLY`  
**Phase**: P14.6 — Suppliers & Purchases Import  
**Branch**: `Products-Customers-Import`  
**Date**: 2026-10-13  

---

## 1. Executive Summary

Phase **P14.6 — Suppliers & Purchases Import** has been successfully implemented on top of the established Import Center foundation. In accordance with the system-wide architecture and the `HISTORICAL_RECORD_ONLY` domain contract, the import engine now provides full multi-tenant CSV/XLSX import capabilities for:
1. **Suppliers**: Vendor master records with unique code/phone/email constraints, contact details, and strict safeguards against erroneous name-only merging.
2. **Purchases (Historical)**: Multi-line procurement records grouped by external purchase/invoice identifier, maintaining historical audit trails while guaranteeing **zero side effects** on inventory physical balances, general ledger accounts, cash drawers, or live operational receiving queues.

### Critical Invariants Enforced
1. **Zero Live Inventory Impact**:
   - `WarehouseStock` quantities are never modified.
   - Zero `StockMovement` records are created.
   - Physical opening stock balances and ledger counts established in P14.3 remain 100% unaffected.
2. **Zero Live Accounting Impact**:
   - Zero `JournalEntry` records are created in general ledger accounts.
   - Accounts payable and cash balances are not mutated.
3. **Zero Cash Drawer / Supplier Payment Impact**:
   - Zero `SupplierPayment` records are created.
   - Cash register balances and end-of-day register closures remain unaffected.
4. **No Accidental Vendor Merging**:
   - Suppliers are matched strictly by `code`, `phone`, or `email`.
   - Suppliers with identical or similar names but distinct codes/phones/emails are never merged into a single vendor record.
5. **Multi-Line Purchase Invoicing**:
   - Multiple rows sharing the same `purchase_id` in a CSV/XLSX file are deterministically grouped into a single parent `Purchase` with child `PurchaseItem` records.
6. **Exact Minor-Unit Arithmetic**:
   - All unit costs, subtotals, discounts, taxes, and invoice totals strictly enforce integer minor-unit arithmetic via `App\Support\Money`. Discrepancies between line item totals and declared invoice totals trigger immediate validation errors (`PURCHASE_TOTAL_MISMATCH`).
7. **Operational Workflow Isolation**:
   - Historical purchases (`is_historical = true`, `source = 'IMPORT_HISTORICAL'`) are filtered out of the warehouse receiving queue (`PurchaseController::receivingQueue`).
   - The `/purchases/{id}/receive` and `/purchases/{id}/pay` API endpoints strictly reject historical purchases with localized 422 ValidationException responses.
8. **Strict Idempotency & Tenant Isolation**:
   - Deterministic UUIDs (`resto_imp_{org}_{job}_po_{id}`) prevent duplicate creation upon queue retries or repeated confirmation requests.
   - Foreign-tenant products, warehouses, and suppliers are rejected at validation and execution gates.

---

## 2. Implementation Architecture

### 2.1 Database Schema & Migration
* **Migration**: `backend/database/migrations/2026_10_13_000001_add_import_fields_to_suppliers_and_purchases_tables.php`
* **Changes to `suppliers`**:
  - `code` (`VARCHAR(100) NULL`): Vendor code, indexed with `organization_id`.
  - `tax_id` (`VARCHAR(50) NULL`): National tax/economic identifier.
  - `notes` (`TEXT NULL`): Supplier notes.
* **Changes to `purchases`**:
  - `external_purchase_id` (`VARCHAR(100) NULL`): Source invoice/purchase number from legacy spreadsheets, indexed with `organization_id`.
  - `subtotal` (`DECIMAL(12, 2) NULL`): Line item subtotal before discounts/taxes.
  - `discount_amount` (`DECIMAL(12, 2) DEFAULT 0`): Invoice-level discount.
  - `tax_amount` (`DECIMAL(12, 2) DEFAULT 0`): Invoice-level tax.
  - `notes` (`TEXT NULL`): Descriptive invoice notes.
  - `source` (`VARCHAR(50) DEFAULT 'MANUAL'`): Set to `'IMPORT_HISTORICAL'` for imported purchases.
  - `is_historical` (`BOOLEAN DEFAULT false`, indexed): Flags imported past purchases for operational scoping.
  - `import_job_id` (`UUID NULL`): Links purchase to the originating `ImportJob`.
* **Changes to `purchase_items`**:
  - `subtotal` (`DECIMAL(12, 2) NULL`): Line item subtotal.
  - `discount_amount` (`DECIMAL(12, 2) DEFAULT 0`): Line item discount.
  - `tax_amount` (`DECIMAL(12, 2) DEFAULT 0`): Line item tax.

### 2.2 Model Upgrades
* **`App\Models\Supplier`**:
  - Added `'code'`, `'tax_id'`, `'notes'` to `$fillable`.
* **`App\Models\Purchase`**:
  - Added `'external_purchase_id'`, `'subtotal'`, `'discount_amount'`, `'tax_amount'`, `'notes'`, `'source'`, `'is_historical'`, `'import_job_id'` to `$fillable`.
  - Added cast `'is_historical' => 'boolean'`.
* **`App\Models\PurchaseItem`**:
  - Added `'subtotal'`, `'discount_amount'`, `'tax_amount'` to `$fillable`.

### 2.3 Operational Workflow Guards
* **`PurchaseController::receivingQueue`**:
  - Scoped with `where('is_historical', false)`, preventing past imported purchases from cluttering the active warehouse receiving queue.
* **`PurchaseController::receive`**:
  - Added domain guard:
    ```php
    if ($purchase->is_historical) {
        throw ValidationException::withMessages([
            'purchase' => ['عملیات دریافت کالا برای خریدهای تاریخی واردشده امکان‌پذیر نیست.'],
        ]);
    }
    ```
* **`PurchaseController::pay`**:
  - Added domain guard:
    ```php
    if ($purchase->is_historical) {
        throw ValidationException::withMessages([
            'purchase' => ['عملیات پرداخت برای خریدهای تاریخی واردشده امکان‌پذیر نیست.'],
        ]);
    }
    ```

### 2.4 Import Center Services Extended
1. **`ImportErrorCode`**:
   - Added error codes and bilingual Persian/English messages:
     - `SUPPLIER_NOT_FOUND`
     - `PURCHASE_ID_REQUIRED`
     - `PURCHASE_TOTAL_MISMATCH`
     - `PURCHASE_LINE_INCONSISTENCY`
     - `INVALID_PURCHASE_STATUS`
2. **`ImportMappingService`**:
   - Implemented `getTargetFields` for `ImportType::SUPPLIERS` and `ImportType::PURCHASES` with extensive Persian/English aliases (e.g. `کد تامین‌کننده`, `شناسه خرید`, `شماره فاکتور`, `قیمت خرید`, `تخفیف`, `مالیات`, `مبلغ کل`).
3. **`ImportJobController`**:
   - `getMapping`: Provides tenant `warehouses`, `stores`, and `suppliers` for purchases.
   - `saveMapping`: Validates that selected default `warehouse_id`, `store_id`, and `supplier_id` belong strictly to the authenticated tenant.
4. **`ImportValidationService`**:
   - Implemented `validateSuppliers`: Enforces name requirements, format of code/phone/email, in-file duplicate detection, and matching against database suppliers only by code, phone, or email.
   - Implemented `validatePurchases`: Groups rows by `purchase_id`, validates intra-invoice consistency (date, store, warehouse, supplier, status), verifies product SKU/barcode existence within tenant, checks quantity (positive integer) and unit cost (valid non-negative money), calculates exact minor-unit totals, and verifies parity against declared `total_amount`.
5. **`ImportExecutionService`**:
   - Implemented `executeSuppliers`: Streams rows, processes in transactions with chunking, implements deterministic UUIDs, enforces `skip` and `update` duplicate strategies (updating only permitted descriptive fields), and preserves tenant ownership.
   - Implemented `executePurchases` & `processPurchaseGroup`: Groups multi-line rows, enforces deterministic UUIDs for purchases and purchase items, validates duplicate strategy (`skip` preserves existing purchase; `update` mutates non-financial metadata only while financial amounts and inventory remain immutable), persists purchases and items with `is_historical = true` and `source = 'IMPORT_HISTORICAL'`, and completely bypasses stock, ledger, and payment services.

### 2.5 Web UI Integration
* **`web/src/pages/ImportCenterPage.tsx`**:
  - Activated `suppliers` and `purchases` datasets in `IMPORT_TYPES`.
  - Added Default Warehouse, Default Store, and Default Supplier selector dropdowns on Step 1 (Mapping) when appropriate.
* **`web/src/types/index.ts` & `web/src/api/apiClient.ts`**:
  - Extended `ImportMappingResponse` with `suppliers` list and `supplier_id`.
  - Extended `saveImportMapping` API client method with `supplierId` parameter.

---

## 3. Verification & Test Results

### 3.1 Feature Test Suite: `SuppliersPurchasesImportTest`
A comprehensive feature test suite was written in `backend/tests/Feature/SuppliersPurchasesImportTest.php` covering 11 critical test cases:
1. `test_mapping_service_provides_target_fields_for_suppliers`: Verifies supplier fields and aliases.
2. `test_mapping_service_provides_target_fields_for_purchases`: Verifies purchase fields and aliases.
3. `test_mapping_endpoint_returns_suppliers_warehouses_and_stores_for_purchases`: Verifies API response contract.
4. `test_suppliers_validation_and_execution_success`: Verifies complete supplier ingestion and database persistence.
5. `test_suppliers_do_not_merge_solely_on_similar_name`: Verifies two vendors with identical names but different codes/phones remain distinct records.
6. `test_suppliers_duplicate_skip_and_update_strategies`: Verifies `skip` retains existing record while `update` updates permitted metadata.
7. `test_purchases_multiline_grouping_and_exact_math`: Verifies multi-line invoice grouping and flags declared total discrepancies (`PURCHASE_TOTAL_MISMATCH`).
8. `test_purchases_zero_side_effects_on_inventory_and_accounting`: Verifies zero changes to `WarehouseStock`, zero `StockMovement` records, zero `JournalEntry` records, and zero `SupplierPayment` records.
9. `test_purchases_workflow_guards_block_receive_and_pay`: Verifies exclusion from `receivingQueue` and rejection of `receive` and `pay` operations.
10. `test_purchases_idempotency_and_duplicate_skip_update`: Verifies deterministic UUID retry safety, idempotency, and metadata-only updates.
11. `test_tenant_isolation_prevents_importing_cross_tenant_products`: Verifies cross-tenant data isolation and error reporting (`PRODUCT_NOT_FOUND`).

**Result**: 11 passed (75 assertions).

### 3.2 Full Regression Test Suite
* **Backend**: `php artisan test`
  - **193 tests passed**, **966 assertions**, **0 regressions**, execution time ~6.4s.
* **Web**:
  - `npm test -- --watchAll=false`: 2 test suites passed, 47 tests passed.
  - `npm run build`: Production builds for `resto-web:business` and `resto-web:admin` compiled with 0 errors.

---

## 4. Staging Status & Next Steps

All changes are localized to P14.6 scope and prepared for atomic review. Staging will be performed file-by-file without touching unrelated cache or build files.

Phase P14.7 must not begin until P14.6 review and staging are finalized.
