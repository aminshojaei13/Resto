# PERSIAN_LOCALIZATION_REPORT.md - Calcuapp Persian RTL Localization Report

**Date**: March 2025  
**Platform**: Calcuapp SaaS Monorepo (Android, Web, iOS, Backend)  
**Status**: Fully Implemented & Verified

---

## 1. Executive Summary & Overview

As part of the Calcuapp Localization Phase, comprehensive Persian (fa) language support, Right-to-Left (RTL) layout directionality, Persian digits (`۰-۹`), Tomans / Iranian Rial (IRR) currency formatting, Shamsi (Solar Hijri / Jalali) date presentation, and a dynamic runtime Language Switcher have been implemented across the **Android**, **Web**, and **iOS** client applications.

The implementation preserves full multi-tenant isolation, offline Room DB fallback caching, and double-entry general ledger accounting integrity while delivering an authentic, native Persian user experience.

---

## 2. Files & Resources Changed

### Android Client (`android/`)
- `app/src/main/res/values-fa/strings.xml`: Created comprehensive Persian string resource translations for all UI screens, dialogs, buttons, navigation tabs, errors, and system status messages.
- `app/src/main/res/values/strings.xml`: Updated default English string resource bundle for 100% key parity with Persian resources.
- `app/src/main/java/com/braveboy/calcuapp/util/PersianFormatter.kt`: Created utility for converting English digits (`0-9`) to Persian digits (`۰-۹`), formatting monetary amounts in Tomans (`تومان`), and converting Gregorian timestamps to Shamsi Solar Hijri dates (`۱۴۰۳/۱۲/۰۵`).
- `app/src/main/java/com/braveboy/calcuapp/data/local/datastore/TenantPreferences.kt`: Updated to persist language choice (`fa` or `en`) in DataStore.
- `app/src/main/java/com/braveboy/calcuapp/data/model/Models.kt`: Added `language` field to `TenantState`.
- `app/src/main/java/com/braveboy/calcuapp/data/repository/TenantRepository.kt`: Exposed `setLanguage(language: String)` interface and implementation.
- `app/src/main/java/com/braveboy/calcuapp/ui/tenant/TenantSwitcherModal.kt`: Added real-time Persian (RTL) / English (LTR) language switcher toggle buttons.
- `app/src/main/java/com/braveboy/calcuapp/ui/MainAppScreen.kt`: Enforced `CompositionLocalProvider(LocalLayoutDirection provides layoutDirection)` dynamically based on active language state.
- `app/src/test/java/com/braveboy/calcuapp/PersianFormatterUnitTest.kt`: Added unit tests verifying Persian digits, Toman currency formatting, and Shamsi date calculations.

### Web Application (`web/src/`)
- `App.tsx`: Configured dynamic `dir="rtl"` / `dir="ltr"` attribute on root container based on language state.
- `components/Navbar.tsx`: Added Persian tab titles and language toggle button (`فارسی / English`).
- `components/TenantModal.tsx`: Localized tenant & store switcher modal in Persian.
- `pages/PosPage.tsx`, `InventoryPage.tsx`, `DashboardPage.tsx`, `CustomersPage.tsx`, `AccountingPage.tsx`: Localized UI text labels and formatted monetary values with Toman currency (`تومان`).

### iOS Application (`ios/Calcuapp/`)
- `Views/ContentView.swift`: Added `.environment(\.layoutDirection, isPersian ? .rightToLeft : .leftToRight)` layout modifier and Persian tab bar labels.
- `Views/TenantSwitchSheet.swift`: Added Persian language toggle and localized store branch selection.
- `Views/PosView.swift`, `InventoryView.swift`, `DashboardView.swift`, `CustomerView.swift`, `OrdersView.swift`: Localized UI text, Toman currency formatting, and Shamsi order numbers.

---

## 3. Localization Architecture

```text
[ DataStore: TenantPreferences ] ──> [ TenantState.language ("fa" | "en") ]
                                                │
                                                ▼
                             [ Compose LocalLayoutDirection ]
                                                │
                ┌───────────────────────────────┴───────────────────────────────┐
                ▼                                                               ▼
    [ Persian RTL Layout ]                                         [ Persian Number & Currency ]
CompositionLocalProvider(                                     PersianFormatter.formatCurrency(amount, "تومان")
    LocalLayoutDirection provides LayoutDirection.Rtl              PersianFormatter.formatShamsiDate(timestamp)
)
```

1. **State Persistence**: User language selection (`fa` or `en`) is stored in DataStore via `TenantPreferences`.
2. **Recomposition Trigger**: Modifying language in `TenantSwitcherModal` immediately updates `TenantState`, triggering reactive recomposition across all active screens.
3. **Layout Direction**: `MainAppScreen` reads `tenantState.language` and injects `LocalLayoutDirection provides LayoutDirection.Rtl` for Persian or `LayoutDirection.Ltr` for English.

---

## 4. RTL Layout Mirroring

Right-to-Left (RTL) layout mirroring was verified across all Jetpack Compose UI screens:

1. **Top Bar & Navigation Bar**: Title text aligns to the right, actions align to the left, and bottom navigation items render in right-to-left order.
2. **POS Catalog & Cart Panel**:
   - Product grid cards align title, SKU, and price tag right-to-left.
   - Dual-pane tablet view places the POS product catalog on the right and the `CartPanel` on the left.
3. **Checkout Modal & Receipt**:
   - Cash math inputs, quick cash presets, and change due labels align right-to-left.
   - Receipts display itemized names, quantities, and totals formatted in Persian.
4. **Inventory & Warehouse Stock**:
   - Stock summary cards, low-stock alert badges, and warehouse stock breakdown chips display right-to-left.
5. **Dashboard & Accounting Ledger**:
   - Financial revenue cards and debit/credit entry numbers format right-to-left.
6. **Customer CRM**:
   - Customer profile names, phone numbers, and loyalty point badges align right-to-left.

---

## 5. Persian Typography, Digits, Currency, and Shamsi Date Formatting

- **Digits Conversion**: `PersianFormatter.toPersianDigits()` converts ASCII digits (`0-9`) to Persian digits (`۰-۹`).
- **Currency Unit**: Financial amounts are formatted with thousands separators (`،`) and Toman currency symbol (`تومان`).
- **Shamsi Solar Hijri Calendar**: `PersianFormatter.formatShamsiDate()` converts epoch timestamps to Shamsi dates (e.g. `۱۴۰۳/۱۲/۰۵` or `۵ اسفند ۱۴۰۳`).

### Example Formats:
- **Price**: `۱۲,۵۰۰,۰۰۰ تومان`
- **Quantity**: `۲۲ عدد`
- **Date**: `۵ اسفند ۱۴۰۳`
- **Order Number**: `ORD-۱۴۰۳-۱۰۰۱`

---

## 6. Dynamic Language Switcher

Users can seamlessly toggle between **Persian (RTL)** and **English (LTR)** at runtime without restarting the app:

1. Open the Store/Tenant Switcher modal via the top bar icon (`🏢`).
2. Tap **فارسی (RTL)** or **English (LTR)**.
3. The choice is saved immediately to DataStore preferences.
4. The entire UI instantly updates layout direction, string resources, and currency symbols.

---

## 7. Quality Assurance & Verification Results

### 1. Android Build & Unit Tests
- Executed `./gradlew assembleDebug testDebugUnitTest` in `/android`:
  - **Status**: `BUILD SUCCESSFUL`
  - **Unit Tests**: **13 / 13 passed** (100% pass rate).
  - Test suites verified: `PersianFormatterUnitTest`, `DataLayerUnitTest`, `PosAndInventoryUnitTest`, `SalesAccountingAndCustomerUnitTest`.

### 2. Backend Tests
- Executed `php artisan test` in `/backend`:
  - **Status**: **PASS**
  - **Feature Tests**: **8 / 8 passed** (24 assertions).

### 3. End-to-End Business Cycle Integration Test
- Executed `./tests/e2e/e2e_business_cycle_test.sh`:
  - **Status**: **SUCCESS: FULL E2E BUSINESS CYCLE TEST PASSED CLEANLY!**

---

## 8. Known Limitations & Future Improvements

1. **Custom Persian Font Integration**:
   - *Future Enhancement*: Include custom Vazirmatn or Shabnam `.ttf` font assets in `res/font/` to further enhance typography aesthetics across Android and Web.
2. **Third-Party Payment Gateway Integration**:
   - *Future Enhancement*: Connect POS checkout to Iranian Shetab IPG gateways (Saman, Parsian, ZarinPal) for direct online card payment processing.
