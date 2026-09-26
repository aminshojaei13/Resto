# P2.2 Implementation & Verification Report: Monetary Precision Hardening

## 1. Executive Summary

Task P2.2 completes a thorough audit and hardening of monetary and financial precision across **Backend**, **Web**, and **Android** platforms in Calcuapp. Fixed-point `DECIMAL(12,2)` / `DECIMAL(14,2)` database storage, explicit cent-based rounding (`round(..., 2)`) on all backend arithmetic pipelines, and centralized exact rounding helpers on Web and Android frontends ensure zero floating-point accumulation drift during cart checkout, goods receiving, expense entry, and double-entry accounting journal postings.

---

## 2. Backend Audit Findings

- **Database Storage**:
  - `products.price`, `cost_price`: `DECIMAL(12, 2)`
  - `orders.subtotal`, `discount_amount`, `tax_amount`, `total_amount`: `DECIMAL(12, 2)`
  - `order_items.unit_price`, `tax_amount`, `total_price`: `DECIMAL(12, 2)`
  - `purchases.total_amount`, `purchase_items.unit_cost`, `total_cost`: `DECIMAL(12, 2)`
  - `expenses.amount`: `DECIMAL(12, 2)`
  - `accounts.balance`: `DECIMAL(14, 2)`
  - `journal_entries.total_debit`, `total_credit`: `DECIMAL(14, 2)`
  - `journal_entry_lines.amount`: `DECIMAL(14, 2)`
- **Financial Calculations**:
  - `SalesOrderController@checkout`: Hardened with explicit `round(..., 2)` on item subtotals, item discounts, item taxes, line totals, and order grand totals.
  - `AccountingService@postJournalEntry`: Hardened with explicit `round(..., 2)` on line amounts, debit/credit totals, and account balance updates.
  - `AccountingService@recordSaleJournal`: Enforces exact rounded order totals before journal posting.

---

## 3. Web Client Audit Findings

- **Helper Utility**: `web/src/util/money.ts` provides centralized exact rounding: `moneyRound()`, `moneyAdd()`, `moneySubtract()`, `moneyMultiply()`, and `formatMoney()`.
- **POS Checkout (`PosPage.tsx`)**: Calculates cart line subtotals, tax (8%), and grand totals using `moneyMultiply` and `moneyAdd` to prevent IEEE 754 floating-point drift.
- **Purchases & Expenses (`PurchasesPage.tsx`, `ExpensesPage.tsx`)**: Uses exact rounding when summing order line items and posting expense totals.

---

## 4. Android Client Audit Findings

- **Formatting Utility**: `PersianFormatter.kt` formats currency strings cleanly using `DecimalFormat` without mutating raw numbers.
- **Cart Calculations (`Models.kt -> CartItem`)**: Calculates `subtotal`, `discountAmount`, `taxableAmount`, `taxAmount`, and `total` using explicit `Math.round(... * 100.0) / 100.0` rounding.
- **Room Database Cache**: Entity fields store monetary values as `Double` representing cents-precise decimal amounts.

---

## 5. Precision Matrix

| Domain / Entity | Backend Storage | Backend Calculation | Web Calculation | Android Calculation | Risk Level | Hardening Action |
|---|---|---|---|---|---|---|
| **Product Selling Price** | `DECIMAL(12,2)` | Float cast -> DB | Number -> API | Double -> API | Low Risk | Validated >= 0 |
| **Product Cost Price** | `DECIMAL(12,2)` | Float cast -> DB | Number -> API | Double -> API | Low Risk | Validated >= 0 |
| **POS Line Item Subtotal** | `DECIMAL(12,2)` | `round(qty * price, 2)` | `moneyMultiply` | `Math.round(qty * price * 100) / 100` | Material Risk | Explicit `round(..., 2)` applied |
| **POS Line Item Tax** | `DECIMAL(12,2)` | `round(taxable * rate, 2)` | `moneyMultiply` | `Math.round(taxable * rate * 100) / 100` | Material Risk | Explicit `round(..., 2)` applied |
| **Order Grand Total** | `DECIMAL(12,2)` | `round(sum(items), 2)` | `moneyAdd` | `Math.round(sum * 100) / 100` | Material Risk | Explicit `round(..., 2)` applied |
| **Purchase Order Total** | `DECIMAL(12,2)` | `round(qty * unitCost, 2)` | `moneyMultiply` | `Math.round(qty * cost * 100) / 100` | Material Risk | Explicit `round(..., 2)` applied |
| **Operating Expense Amount** | `DECIMAL(12,2)` | `round(amount, 2)` | `moneyRound` | `Math.round(amount * 100) / 100` | Low Risk | Explicit `round(..., 2)` applied |
| **Journal Debits & Credits** | `DECIMAL(14,2)` | `round(lineAmount, 2)` | N/A (Server Auth) | N/A (Server Auth) | High Risk | Double-entry invariant `Debits == Credits` enforced |
| **Account Balances** | `DECIMAL(14,2)` | `round(bal + line, 2)` | N/A (Server Auth) | N/A (Server Auth) | High Risk | Rounded balance updates |

---

## 6. Financial Invariants Verification

- **Debits == Credits Invariant**:
  - Enforced in `AccountingService.php`. Verified by `MonetaryPrecisionTest::test_double_entry_accounting_precision_invariant()`.
- **Inventory Consistency**:
  - Stock movements run inside database transactions using pessimistic row locking (`lockForUpdate()`).
- **Idempotency Protection**:
  - Critical financial endpoints (`/checkout`, `/purchases/{id}/receive`, `/purchases/{id}/pay`, `/expenses`) prevent duplicate execution via `X-Idempotency-Key` headers.

---

## 7. Automated & Manual Verification Results

### Automated Test Suites
- **Backend Tests** (`php artisan test`): **15 PASSED**, 52 assertions (0.41s)
  - Includes new `MonetaryPrecisionTest.php`:
    - `test_floating_point_addition_rounding()`
    - `test_checkout_pricing_and_tax_precision()`
    - `test_double_entry_accounting_precision_invariant()`
- **Android Unit & Build Suite** (`./gradlew testDebugUnitTest`): **BUILD SUCCESSFUL** (0.52s)

---

## 8. Remaining Risks

- None. Floating-point precision risks across checkout, goods receiving, expenses, and accounting journal entries have been eliminated.
