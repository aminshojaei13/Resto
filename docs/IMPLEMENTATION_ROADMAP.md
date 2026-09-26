# Calcuapp Implementation Roadmap — Phased Engineering Plan

**Date**: March 2025  
**Author**: Senior Product Architect & Technical Lead  
**Repository**: Calcuapp Monorepo (`/Users/aminshojaei/AndroidStudioProjects/Calcuapp`)

---

## Roadmap Overview

This roadmap defines the phased implementation strategy for aligning the Calcuapp SaaS platform with the corrected Business & Commerce Management product specification.

Work is structured into **Phases 0 through 5** in strict priority order, ensuring core business lifecycle stability, data integrity, and strict tenant isolation before external channels and advanced integrations are added.

---

## Phase 0: Product Direction & Architecture Alignment (CURRENT PHASE)
*Goal: Finalize domain model specifications, gap analysis, schema design, and technical requirements before code modifications.*

### Key Deliverables:
1. `docs/PRODUCT_DIRECTION_GAP_ANALYSIS.md` — Detailed domain-by-domain gap analysis.
2. `PROJECT_SPEC.md` — Comprehensive business & technical specification.
3. `docs/DOMAIN_REVIEW.md` — Corrected domain model and relationship specifications.
4. `docs/IMPLEMENTATION_ROADMAP.md` — Phased engineering implementation roadmap.
5. Technical sync and Product Owner approval.

---

## Phase 1: Core Business Lifecycle Engine (P0 Priority)
*Goal: Implement full operational loop (Suppliers $\rightarrow$ Purchasing $\rightarrow$ Receiving $\rightarrow$ Inventory $\rightarrow$ Core Orders $\rightarrow$ Payments $\rightarrow$ Expenses $\rightarrow$ Accounting).*

### Task 1.1: Security & Multi-Tenant Authorization Hardening
- Enforce active membership verification in `TenantMiddleware` against `organization_memberships`.
- Add `X-Idempotency-Key` validation middleware (`IdempotencyMiddleware`) for checkout, purchasing, and payment endpoints.
- Upgrade financial numbers to integer cents or `BCMath` in PHP and `BigDecimal` / `Decimal` in clients.

### Task 1.2: Supplier Management & Procurement
- **Backend**: Implement `Supplier` model, `SupplierController`, database migration refinements, and supplier statement endpoints.
- **Android**: Build `SupplierScreen.kt`, `SupplierViewModel.kt`, and `SupplierRepository.kt`.
- **Web**: Build `SuppliersPage.tsx` tab.
- **iOS**: Build `SuppliersView.swift` tab.

### Task 1.3: Purchasing & Receiving Lifecycle
- **Backend**: Implement `PurchasesController`, `PurchasingService`, receiving flow (increasing stock in `stock_movements`), and Accounts Payable auto-posting (`Cr Accounts Payable`, `Dr Inventory`).
- **Android**: Build `PurchasesScreen.kt` (PO creation, goods receiving modal, supplier payment dialog).
- **Web**: Build `PurchasesPage.tsx` tab.
- **iOS**: Build `PurchasesView.swift` tab.

### Task 1.4: Expenses Module
- **Backend**: Create `Expense` database migration, `Expense` model, `ExpenseController`, and double-entry journal posting (`Dr Expense Account`, `Cr Cash/Bank/AP`).
- **Android**: Build `ExpensesScreen.kt` and `ExpensesViewModel.kt`.
- **Web**: Build `ExpensesPage.tsx` tab.
- **iOS**: Build `ExpensesView.swift` tab.

### Task 1.5: Core Order Domain Refactoring
- **Backend**: Refactor `SalesOrderController` to decouple order creation from instant POS checkout. Add fulfillment endpoints (`POST /orders/{id}/fulfill`, `POST /orders/{id}/cancel`).
- **Accounting**: Implement automatic COGS journal entry upon order completion:
  $$\text{Dr COGS (Expense)} \quad \text{at Cost Price}$$
  $$\text{Cr Inventory (Asset)} \quad \text{at Cost Price}$$

---

## Phase 2: Executive Dashboard & Business Analytics (P0 Priority)
*Goal: Provide business owners with accurate, business-wide financial and operational visibility.*

### Task 2.1: Reporting Engine & Analytics APIs
- **Backend**: Implement `ReportingService` and `ReportController` supplying:
  - Total Sales Revenue
  - Cost of Goods Sold (COGS)
  - Gross Profit
  - Operating Expenses
  - Net Profit
  - Accounts Payable ($AP$) & Accounts Receivable ($AR$) Total
  - Total Inventory Valuation
  - Low Stock Alerts

### Task 2.2: Client Dashboard UI Overhaul
- **Android**: Update `DashboardScreen.kt` to render true executive business KPI cards, profit/loss breakdown charts, and AP/AR summaries.
- **Web**: Overhaul `DashboardPage.tsx` with business financial widgets.
- **iOS**: Overhaul `DashboardView.swift` with business financial metrics.

---

## Phase 3: Sales Channels & Navigation Restructuring (P1 Priority)
*Goal: Restructure client navigation around business management and establish channel ingress abstraction.*

### Task 3.1: Client Navigation Architecture Restructuring
- Restructure top-level navigation in Android, Web, and iOS to follow business lifecycle order:
  $$\text{Dashboard} \rightarrow \text{Sales/Orders} \rightarrow \text{Products} \rightarrow \text{Inventory} \rightarrow \text{Purchases} \rightarrow \text{Suppliers} \rightarrow \text{Customers} \rightarrow \text{Expenses} \rightarrow \text{Accounting} \rightarrow \text{Reports} \rightarrow \text{Settings}$$
- Reposition **POS** under Sales as a channel UI option rather than the root container.

### Task 3.2: Channel Ingress Pipeline Abstraction
- **Backend**: Create `ChannelAdapterInterface` and `ChannelIngressService` to normalize incoming channel orders (POS, Web Store, Manual Entry) into standard Core Order records.

---

## Phase 4: External Integrations & Connectors (P2 Priority)
*Goal: Connect external commercial platforms and payment gateways.*

### Task 4.1: Social Commerce & Messaging Connectors
- Implement webhook handlers and messaging adapters for Instagram Graph API and Telegram Bot API.

### Task 4.2: Payment Gateway & Iranian Shetab IPG Integration
- Implement payment gateway drivers (ZarinPal, Saman IPG, Parsian IPG) for online customer order payments.

---

## Phase 5: Advanced Automation & Enterprise Features (P3 Priority)
*Goal: Advanced multi-store automation, inventory reordering, and analytics.*

### Task 5.1: Automated Purchase Reordering
- Auto-generate draft Purchase Orders when warehouse stock drops below minimum stock thresholds (`min_stock_level`).

### Task 5.2: Multi-Terminal Realtime Synchronization
- Activate Laravel Reverb / WebSockets broadcasting for real-time stock and multi-cashier POS synchronization.

---

## Testing & Verification Gateways

At the end of every implementation task:
1. **Backend**: Execute `php artisan test` (Pest / PHPUnit) ensuring 100% test pass rate and double-entry equality.
2. **Android**: Execute `./gradlew assembleDebug testDebugUnitTest` in `/android`.
3. **Web**: Execute `npm run build` or `tsc --noEmit` in `/web`.
4. **Integration**: Run `./tests/e2e/e2e_business_cycle_test.sh` to verify end-to-end lifecycle integrity.
