# Resto P14.4 — Operating Expenses Import Implementation Report

**Verdict**: `READY`  
**Phase**: P14.4 — Expenses Import  
**Branch**: `Products-Customers-Import`  
**Date**: 2026-10-12  

---

## 1. Executive Summary

P14.4 adds historical operating expense importing (CSV and XLSX) to the Resto Import Center foundation established in P14.1, P14.2, and P14.3. Operating expenses represent pure operational outflows (Rent, Utilities, Internet, Salaries, Advertising, etc.) and are strictly recorded via double-entry journal postings in Resto's general ledger.

This implementation preserves all financial invariants:
1. **Canonical Double-Entry Accounting**: Created `ExpenseService` to encapsulate transactional creation, updating, reversal, and accounting integration (`AccountingService::postExpenseEntry`: Debit `5010` Operating Expenses, Credit `1010` Cash). Refactored `ExpenseController` to use this canonical path. No unposted or shadow expenses can ever be created.
2. **Exact Money Representation**: Amounts are validated strictly as positive decimals formatted with 2 decimal places. Non-positive numbers or invalid formats are rejected with `INVALID_AMOUNT`.
3. **Multi-Calendar Date Normalization**: `DataNormalizer::parseDate` handles Persian Solar Jalali dates (`1403/07/21` $\rightarrow$ `2024-10-12`) via the astronomical Kazimierz Borkowski algorithm, standard Gregorian strings (`YYYY-MM-DD`, `DD/MM/YYYY`, `MM/DD/YYYY`), and Excel numeric serial timestamps (`45577` $\rightarrow$ `2024-10-12`).
4. **Deterministic Per-Row Idempotency**: Leverages `ExpenseService::deterministicUuid("resto_imp_{orgId}_{jobId}_{rowNumber}")` based on RFC 4122 v4 UUID hashing to guarantee that network retries or queue re-runs never duplicate expenses or ledger entries.
5. **Duplicate Detection & Safe Execution**: In-file duplicate tracking and existing database matching by `(organization_id, store_id, date, category, amount, title/notes)` allows flexible `skip` (preserves existing record and ledger) or `update` (updates mutable fields like notes/payment method without corrupting ledger history).
6. **Multi-Tenant Isolation**: Store lookup and validation is strictly scoped to the authenticated user's `organization_id`. Cross-tenant store assignment is impossible.

---

## 2. Architecture & Domain Discovery

### 2.1 Schema & Expense Invariants
Inspection of the `expenses` table (`2024_01_01_000010_create_expenses_table.php`):
- Columns: `id` (UUID), `organization_id`, `store_id`, `category`, `amount` (decimal 12,2), `payment_method`, `date` (date), `notes` (text), `attachment_url`, `user_id`, `is_recurring`, `recurrence_period`, `recurrence_starts_on`, `recurrence_ends_on`.
- Invariant: Expenses are pure operational overhead. They have **no** relationship to supplier tables, inventory stock, or purchase orders.
- Standard Categories (`Expense::CATEGORIES`):
  `RENT`, `INTERNET`, `ELECTRICITY`, `WATER`, `TRANSPORT`, `ADVERTISING`, `SALARIES`, `REPAIRS`, `SERVICES`, `FEES`, `OTHER`.

### 2.2 Canonical Accounting Integration
Every expense in Resto is linked to a balanced journal entry in `journal_entries`:
- Entry Type: `EXPENSE`
- Reference ID: `expense->id`
- Debit: Account `5010` (Operating Expenses)
- Credit: Account `1010` (Cash / Cash on Hand)

`ExpenseService` provides:
- `createExpense(array $data, ?string $deterministicId = null)`
- `updateExpense(Expense $expense, array $data)`
- `reverseExpense(Expense $expense, string $reason)`

---

## 3. Implementation Details

### 3.1 Normalization & Date Engine
File: `backend/app/Services/Import/Normalizer/DataNormalizer.php`
- `parseDate(mixed $value)`:
  - Detects Excel integer/float serial numbers (e.g. 45577 corresponds to 2024-10-12).
  - Detects Jalali Solar Hijri dates (`13xx` or `14xx`), converts Persian/Arabic numerals, and translates to Gregorian date string `YYYY-MM-DD`.
  - Parses standard Gregorian formats using `Carbon::parse`.
- `resolveExpenseCategory(mixed $value)`:
  - Maps Persian terms (اجاره $\rightarrow$ `RENT`, اینترنت $\rightarrow$ `INTERNET`, برق $\rightarrow$ `ELECTRICITY`, آب $\rightarrow$ `WATER`, حمل و نقل $\rightarrow$ `TRANSPORT`, تبلیغات $\rightarrow$ `ADVERTISING`, حقوق $\rightarrow$ `SALARIES`, تعمیرات $\rightarrow$ `REPAIRS`, خدمات $\rightarrow$ `SERVICES`, کارمزد $\rightarrow$ `FEES`, سایر $\rightarrow$ `OTHER`) and English aliases to valid `Expense::CATEGORIES`.

### 3.2 Mapping & Target Fields
File: `backend/app/Services/Import/Mapping/ImportMappingService.php`
- Added target fields for `ImportType::EXPENSES`:
  - `title` (required/alias for description/notes)
  - `category` (required)
  - `amount` (required)
  - `date` (required)
  - `store` (optional store name/code)
  - `payment_method` (optional: CASH, CARD, BANK_TRANSFER)
  - `notes` (optional)
- Extensive bilingual aliases for Persian and English auto-detection.

### 3.3 Validation & Duplicate Protection
File: `backend/app/Services/Import/Validation/ImportValidationService.php`
- `validateExpenses()`:
  - Enforces mandatory fields (`category`, `amount`, `date`).
  - Resolves store: row-specific store name/code or fallback to job-level mapping `store_id`. Verifies store belongs to tenant (`organization_id`).
  - Resolves category to canon enum or raises `INVALID_CATEGORY`.
  - Validates positive amount or raises `INVALID_AMOUNT`.
  - Flags duplicate rows in file (`DUPLICATE_IN_FILE`).
  - Detects existing matching expenses in the tenant store database:
    `$existing = Expense::where('organization_id', $orgId)->where('store_id', $storeId)->whereDate('date', $date)->where('category', $category)->where('amount', $amount)->first()`.
  - Generates row-level validation results and UI preview items.

### 3.4 Execution & Idempotency Engine
File: `backend/app/Services/Import/Execution/ImportExecutionService.php`
- `executeExpenses()` and `processExpensesBatch()`:
  - Batching in chunks of 50 inside database transactions.
  - Per-row deterministic UUID:
    ```php
    $idempotencyKey = "resto_imp_{$orgId}_{$job->id}_{$rowNumber}";
    $deterministicId = ExpenseService::deterministicUuid($idempotencyKey);
    ```
  - If record with `$deterministicId` already exists, execution gracefully treats row as already processed (idempotent retry).
  - Safe duplicate resolution:
    - `skip`: Records row as skipped without mutating database or posting journal entries.
    - `update`: Updates non-financial attributes (`notes`, `payment_method`) without corrupting ledger entry balances.
    - `insert`: Invokes `ExpenseService::createExpense()`, posting balanced journal entry.

### 3.5 Web UI Integration
- `web/src/types/index.ts`: Updated `ImportMappingResponse` with `stores?: Array<{ id: string; name: string }>` and `categories?: string[]`.
- `web/src/api/apiClient.ts`: Added optional `storeId` parameter to `saveImportMapping`.
- `web/src/pages/ImportCenterPage.tsx`:
  - Enabled `expenses` dataset tile.
  - Added Default Store selector in mapping stage when importing expenses.
  - Passed `selectedStoreId` to `apiClient.saveImportMapping`.
  - Clean bilingual support (English LTR and Persian RTL).

---

## 4. Verification & Test Evidence

### 4.1 Feature Tests
New dedicated suite: `backend/tests/Feature/ExpensesImportTest.php`
- `test_expenses_import_validates_and_previews_data`: Verified parsing, category normalization, and preview generation.
- `test_expenses_import_parses_jalali_and_excel_dates`: Tested Jalali `1403/07/21` $\rightarrow$ `2024-10-12` and Excel serial `45577` $\rightarrow$ `2024-10-12`.
- `test_expenses_import_enforces_canonical_categories_and_positive_amounts`: Verified `INVALID_CATEGORY` and `INVALID_AMOUNT` error codes.
- `test_expenses_import_enforces_tenant_scoped_store_resolution`: Tested cross-tenant store isolation and resolution.
- `test_expenses_import_executes_and_creates_balanced_accounting_entries`: Verified Dr `5010` and Cr `1010` journal entries.
- `test_expenses_import_idempotency_prevents_duplicate_on_retry`: Verified deterministic UUID idempotency prevents duplicates on retry.
- `test_expenses_import_duplicate_strategy_skip_and_update`: Verified `skip` and `update` handling on matching existing expenses.
- `test_expenses_import_mapping_endpoint_returns_stores_and_categories`: Verified API response structure for frontend dropdown.

**Result**: 12 feature tests passed (66 assertions).

### 4.2 Full Backend Regression Suite
Command: `php artisan test`
- **Result**: **168 passed (799 assertions), 0 failures**
- Includes all operational workflows, tenant isolation, accounting entries, and previous import tests (P14.1, P14.2, P14.3).

### 4.3 Web Test Suite & Production Build
Commands:
- `CI=true npm test`: **2 test suites passed, 47 passed**.
- `npm run build`: Both `business` and `platform-admin` web builds compiled successfully without errors.

---

## 5. Git Staging Status

All P14.4 files staged explicitly one by one:
- `backend/app/Services/ExpenseService.php`
- `backend/app/Http/Controllers/Api/ExpenseController.php`
- `backend/app/Services/Import/Normalizer/DataNormalizer.php`
- `backend/app/Support/Import/ImportErrorCode.php`
- `backend/app/Services/Import/Mapping/ImportMappingService.php`
- `backend/app/Services/Import/Validation/ImportValidationService.php`
- `backend/app/Services/Import/Execution/ImportExecutionService.php`
- `backend/app/Http/Controllers/Api/Import/ImportJobController.php`
- `backend/tests/Feature/ExpensesImportTest.php`
- `docs/api/openapi.yaml`
- `web/src/types/index.ts`
- `web/src/api/apiClient.ts`
- `web/src/pages/ImportCenterPage.tsx`
- `docs/P14_4_EXPENSES_IMPORT_REPORT.md`

No commits or pushes were made.
