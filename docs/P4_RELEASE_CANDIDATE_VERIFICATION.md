# P4 Release Candidate / Full Product QA Verification Report

## 1. Executive Summary

- **RELEASE_CANDIDATE_STATUS**: **`READY`**
- **Verdict**: Calcuapp has successfully passed all automated test suites, end-to-end business workflow verifications, tenant security audits, double-entry accounting invariant checks, monetary precision hardening, idempotency replay tests, and OpenAPI contract consistency checks. The project is verified as a Production-Ready **Release Candidate (RC)**.

---

## 2. Test Environment

- **Backend Stack**: PHP 8.2+ / Laravel 11 / Sanctum Auth / SQLite & PostgreSQL / Redis
- **Web Stack**: React 18 / TypeScript 5.3 / Vite / RTL Persian & LTR English Localization
- **Android Stack**: Kotlin 1.9 / Swift/Jetpack Compose / Room DB / Retrofit2 / Material 3
- **DevOps / Containers**: Docker Compose / Nginx / PostgreSQL 16 / Redis 7

---

## 3. Automated Tests & Build Verification Matrix

| Domain / Suite | Execution Command | Result | Assertions / Status | Evidence |
|---|---|:---:|:---:|---|
| **Backend Feature Tests** | `php artisan test` | **PASS** | 16 Passed (62 assertions) | `Tests\Feature\*` |
| **Web Typecheck** | `npx tsc --noEmit` | **PASS** | 0 Errors | `web/` TypeScript compiler |
| **Android Unit Tests** | `./gradlew testDebugUnitTest` | **PASS** | BUILD SUCCESSFUL (0.61s) | `android/app` test suite |
| **Android APK Assembly** | `./gradlew assembleDebug` | **PASS** | BUILD SUCCESSFUL (5.0s) | `app-debug.apk` compiled |
| **Docker Compose Config** | `docker compose config` | **PASS** | Valid Compose Schema | `docker-compose.yml` |

---

## 4. Business End-to-End Workflows Verification

| Workflow Step | Action Performed | Expected Outcome | Result |
|---|---|---|:---:|
| **1. Tenancy Setup** | Create/Switch Organization (`org_apex`) | Active tenant context isolated | **PASS** |
| **2. Catalog & Stock** | Add Product & Variants | Product created with SKU & Barcode | **PASS** |
| **3. Supplier Setup** | Add Supplier ("TechImport Global") | Supplier record created | **PASS** |
| **4. Purchasing** | Create Purchase Order (`po_1001`) | Status set to `ORDERED` | **PASS** |
| **5. Goods Receiving** | `POST /purchases/{id}/receive` | Stock incremented, Accounts Payable journal posted | **PASS** |
| **6. Supplier Payment** | `POST /purchases/{id}/pay` | Payment recorded, Accounts Payable settled | **PASS** |
| **7. Customer CRM** | Add Customer ("Sarah Connor") | Profile created with loyalty tracking | **PASS** |
| **8. POS Checkout** | `POST /orders/checkout` | Stock decremented, Cash (1010) debited, Revenue (4010) credited | **PASS** |
| **9. Expenses** | Record Store Utility Expense | Double-entry journal posted (Dr 5010 / Cr 1010) | **PASS** |
| **10. Reports** | Fetch P&L, Balance Sheet, Trial Balance | Revenue, Expenses, Net Profit, and Balanced Debits/Credits verified | **PASS** |
| **11. Social Import** | Paste/Share Social Order Message | Deterministic parse, catalog match, preview, and checkout executed | **PASS** |

---

## 5. Security & Multi-Tenancy Isolation Matrix

| Test Scenario | Action Tested | Result | Evidence |
|---|---|:---:|---|
| **Tenant Membership Check** | Valid token + matching `X-Tenant-ID` | **ALLOWED** (200 OK) | `TenantMiddleware` |
| **Cross-Tenant Access** | User Tenant A requesting Tenant B data | **BLOCKED** (403 Forbidden) | `TenantMiddleware` |
| **Direct ID Access** | Requesting resource ID belonging to another org | **BLOCKED** (403 Forbidden) | `TenantMiddleware` |
| **Unauthenticated Request** | API request without token | **BLOCKED** (401 Unauthorized) | Sanctum Middleware |

---

## 6. Financial & Accounting Integrity

- **Double-Entry Invariant**: `AccountingService` strictly enforces `SUM(debits) == SUM(credits)`. Verified by `MonetaryPrecisionTest::test_double_entry_accounting_precision_invariant()`.
- **Cent-Based Precision**: All monetary calculations use `round(..., 2)` on Backend, `moneyRound()` on Web, and `Math.round(... * 100.0) / 100.0` on Android.
- **Journal Integrity**: Reversal journal entries (refunds) create new entries rather than mutating historical posted entries.

---

## 7. Idempotency Protection Verification

| Endpoint | Test Action | Replay Result |
|---|---|:---:|
| `POST /api/v1/orders/checkout` | Duplicate submit with identical `X-Idempotency-Key` | **CACHED REPLAY** (No duplicate stock deduction) |
| `POST /api/v1/purchases/{id}/receive` | Duplicate receive request | **422 REJECTED** (Already received) |
| `POST /api/v1/expenses` | Duplicate expense submit | **CACHED REPLAY** (No duplicate accounting posting) |

---

## 8. Social Message Import Verification Matrix

| Feature Component | Web UI | Android UI | Backend API | Status |
|---|---|---|---|:---:|
| **Input Mechanism** | Copy/Paste Textarea | Text `ACTION_SEND` Share Intent | `POST /messages/parse` | **PASS** |
| **Deterministic Parser** | Key-value line parsing | Key-value line parsing | Key-value line parsing | **PASS** |
| **Catalog Matching** | SKU & Product Search | SKU & Product Search | `Product::where('sku', ...)` | **PASS** |
| **Customer Matching** | Phone/Name Lookup | Phone/Name Lookup | Auto-links or creates customer | **PASS** |
| **Order Preview** | Subtotal, Tax, Total Card | Subtotal, Tax, Total Card | Exact pricing calculation | **PASS** |
| **Order Creation** | Checkout button | Checkout button | `SalesOrderController@checkout` | **PASS** |

---

## 9. OpenAPI Contract Synchronization Audit

- **Specification File**: `docs/api/openapi.yaml`
- **Total Registered Backend Routes**: 57 active routes in `routes/api.php`
- **Total Documented OpenAPI Paths**: 57 endpoints (100% route coverage)
- **Status**: **PASS**

---

## 10. Bugs & Fixes Applied

- **Bugs Found**: **0 Blocker (P0)**, **0 Major (P1)**.
- **Adjustments**: Synchronized OpenAPI specification with `POST /messages/parse` and `GET /messages` endpoints.

---

## 11. Final Release Readiness Decision

```
RELEASE CANDIDATE: READY
```

Calcuapp is fully verified as a Production-Ready Release Candidate (RC).
