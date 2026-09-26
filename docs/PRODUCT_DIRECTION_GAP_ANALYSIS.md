# Product Direction Gap Analysis — Calcuapp SaaS Platform

**Date**: March 2025  
**Author**: Senior Product Architect & Technical Lead  
**Repository**: Calcuapp Monorepo (`/Users/aminshojaei/AndroidStudioProjects/Calcuapp`)  
**Status**: Completed Architectural Audit

---

## Executive Overview

Calcuapp was originally positioned and structured around a **POS-centric, mobile-first cashier and lightweight inventory application**. However, the primary business vision for Calcuapp is a **Multi-Tenant Business & Commerce Management Platform (SaaS)** that enables business owners to manage the complete operational and financial lifecycle of an online or commercial business from one central system:

$$\text{Business} \rightarrow \text{Products} \rightarrow \text{Suppliers} \rightarrow \text{Purchasing} \rightarrow \text{Receiving} \rightarrow \text{Inventory} \rightarrow \text{Sales/Orders} \rightarrow \text{Customers} \rightarrow \text{Payments} \rightarrow \text{Fulfillment} \rightarrow \text{Expenses} \rightarrow \text{Accounting} \rightarrow \text{Reports}$$

This document provides a comprehensive, domain-by-domain audit and gap analysis evaluating every major functional area against this target business architecture.

---

## Functional Area Audit Summary

| Functional Domain | Current Status | Core Alignment | Primary Action Required |
| :--- | :--- | :--- | :--- |
| **Business / Organization** | `PARTIAL` | Single org structure, basic stores | Add organizational hierarchy, tax IDs, and multi-currency settings |
| **Users / Roles / Permissions** | `MISALIGNED` | Basic user table, no domain permissions | Implement domain-based permissions (`purchase.create`, `accounting.post`, etc.) |
| **Product Catalog** | `PARTIAL` | Products & Variants exist | Decouple products from channels; add supplier mapping, tax classes, and reorder levels |
| **Suppliers** | `MISSING` / `PARTIAL` | Table exists in migration; no APIs or UI | Implement first-class Supplier CRUD, balance tracking, and account statements |
| **Purchasing & Receiving** | `MISSING` | Table exists in migration; no controllers/UI | Build complete PO lifecycle (Draft $\rightarrow$ Ordered $\rightarrow$ Received $\rightarrow$ Paid), AP accounts, COGS |
| **Inventory Engine** | `CORRECT` | Transactional ledger + pessimistic locks | Retain transactional model; add multi-warehouse stock reservation, transfers, COGS valuation |
| **Sales / Orders** | `MISALIGNED` | POS checkout focused | Standardize core Order domain supporting multi-channel origination, status workflow |
| **Customers (CRM)** | `PARTIAL` | Basic profile & orders | Add customer credit limits, account statements, receivables tracking |
| **Payments** | `PARTIAL` | Embedded in POS order creation | Decouple payment processing from order UI; support partial payments, idempotency |
| **Expenses** | `MISSING` | No table, model, controller, or UI | Create dedicated Expenses module with COGS, operating cost classification, and accounting entries |
| **Accounting** | `CORRECT` | Double-entry journal enforcement | Retain `Debit == Credit` validation; expand chart of accounts for AP, AR, COGS, Expenses |
| **Reports & Dashboard** | `MISALIGNED` | Only POS sales summary | Build executive business dashboard (Gross Profit, Net Profit, AP/AR, Inventory Value, COGS) |
| **Sales Channels** | `MISALIGNED` | POS hardcoded as primary UI | Abstract Channels into independent ingress connectors feeding into Core Order Domain |

---

## Detailed Gap Analysis by Area

### 1. Business / Organization Domain
- **Status**: `PARTIAL`
- **Current Implementation**: `organizations` and `stores` database tables exist. Active tenant is set via HTTP headers (`X-Tenant-ID`, `X-Store-ID`).
- **Intended Product Behavior**: Multi-tenant business entity supporting organization profile, tax settings, default currencies, multi-store and multi-warehouse mapping.
- **Gap**: Missing global business profile settings, tax registration numbers, default financial year definitions, and tenant authorization enforcement in middleware.
- **Recommended Change**: Update `organizations` table to store business metadata, tax configurations, and strict membership checks in `TenantMiddleware`.
- **Files Affected**:
  - Backend: `app/Http/Middleware/TenantMiddleware.php`, `app/Models/Organization.php`, `database/migrations/2025_01_01_000001_create_organizations_table.php`
  - Android: `data/model/Models.kt`, `data/local/datastore/TenantPreferences.kt`
  - Web: `src/components/TenantModal.tsx`
  - iOS: `Calcuapp/Views/TenantSwitchSheet.swift`
- **Database Migration Required**: Yes (add tax/currency metadata columns to `organizations`).
- **API Changes Required**: Yes (`GET/PUT /api/v1/organizations/{id}/settings`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: Low.

---

### 2. Users / Roles & Permissions
- **Status**: `MISALIGNED`
- **Current Implementation**: `users` and `organization_memberships` tables exist with generic `role` strings (`admin`, `cashier`).
- **Intended Product Behavior**: Domain-based granular permission system (`inventory.view`, `inventory.adjust`, `purchase.create`, `purchase.approve`, `accounting.post`, `sales.checkout`).
- **Gap**: Permissions are hardcoded or implicit. No domain-level authorization guards exist in API endpoints or client UI controls.
- **Recommended Change**: Introduce permission tables or role-permission mappings; enforce authorization checks on all REST API controllers.
- **Files Affected**:
  - Backend: `database/migrations/0001_01_01_000000_create_users_table.php`, `app/Models/User.php`, `app/Http/Middleware/TenantMiddleware.php`
  - Android/Web/iOS: User profile state and permission-guarded UI components.
- **Database Migration Required**: Yes (roles and permissions tables).
- **API Changes Required**: Yes (`GET /api/v1/auth/permissions`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: Medium.

---

### 3. Product Catalog Domain
- **Status**: `PARTIAL`
- **Current Implementation**: `products` and `product_variants` tables exist with SKU, barcode, unit price, and cost price.
- **Intended Product Behavior**: Organization-wide unified product catalog decoupled from channels, supporting variants, purchase cost, default supplier, tax class, and min stock reorder points.
- **Gap**: Products cannot be associated with default suppliers, tax classes, or purchase units (e.g. purchasing in boxes, selling in units).
- **Recommended Change**: Extend `products` table with `supplier_id`, `min_stock_level`, `tax_class_id`, and `unit_conversion_ratio`.
- **Files Affected**:
  - Backend: `app/Models/Product.php`, `app/Http/Controllers/Api/ProductController.php`, `database/migrations/2025_01_01_000003_create_catalog_tables.php`
  - Android: `data/model/Models.kt`, `ui/inventory/InventoryScreen.kt`
  - Web: `src/pages/InventoryPage.tsx`
  - iOS: `Calcuapp/Views/InventoryView.swift`
- **Database Migration Required**: Yes.
- **API Changes Required**: Yes (`POST/PUT /api/v1/products`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: Low.

---

### 4. Supplier Management Domain
- **Status**: `MISSING` / `PARTIAL`
- **Current Implementation**: `suppliers` table migration exists in `2025_01_01_000005_create_purchasing_tables.php`, but no Model class, REST Controller, or Client UI exists.
- **Intended Product Behavior**: Suppliers are first-class commercial entities with profile, tax ID, payment terms, current accounts payable balance, and purchase history.
- **Gap**: Business owners cannot manage suppliers, view supplier statements, or track payables.
- **Recommended Change**: Create `Supplier` model, `SupplierController`, supplier statement calculations, and dedicated UI screens across clients.
- **Files Affected**:
  - Backend: `app/Models/Supplier.php`, `app/Http/Controllers/Api/SupplierController.php`, `routes/api.php`
  - Android: `ui/suppliers/SupplierScreen.kt`, `data/repository/SupplierRepository.kt`
  - Web: `src/pages/SuppliersPage.tsx`
  - iOS: `Calcuapp/Views/SuppliersView.swift`
- **Database Migration Required**: Yes (refine supplier fields: `tax_id`, `payment_terms_days`, `opening_balance`).
- **API Changes Required**: Yes (`GET/POST/PUT /api/v1/suppliers`, `GET /api/v1/suppliers/{id}/statement`).
- **Android Changes Required**: Yes (new screen).
- **Web Changes Required**: Yes (new tab).
- **iOS Changes Required**: Yes (new tab).
- **Risk**: Medium.

---

### 5. Purchasing & Goods Receiving Domain
- **Status**: `MISSING`
- **Current Implementation**: `purchases`, `purchase_items`, and `supplier_payments` migration tables exist, but no Eloquent models, controllers, or API routes exist.
- **Intended Product Behavior**: Complete purchasing lifecycle: Draft Purchase Order $\rightarrow$ Approval $\rightarrow$ Goods Receiving (Warehouse Inventory Increase) $\rightarrow$ Accounts Payable creation $\rightarrow$ Supplier Payment $\rightarrow$ COGS tracking.
- **Gap**: Core commercial purchasing lifecycle is totally unimplemented in code. Stock can currently only be increased via manual inventory adjustments.
- **Recommended Change**: Implement `PurchasingService`, `PurchaseController`, receiving flow, auto-posting to Accounts Payable (`Cr Accounts Payable`, `Dr Inventory`), and supplier payment endpoints.
- **Files Affected**:
  - Backend: `app/Models/Purchase.php`, `app/Models/PurchaseItem.php`, `app/Services/PurchasingService.php`, `app/Http/Controllers/Api/PurchaseController.php`, `routes/api.php`
  - Android: `ui/purchases/PurchasesScreen.kt`, `data/repository/PurchaseRepository.kt`
  - Web: `src/pages/PurchasesPage.tsx`
  - iOS: `Calcuapp/Views/PurchasesView.swift`
- **Database Migration Required**: Yes (add `received_at`, `receiving_status`, `warehouse_id` to purchases).
- **API Changes Required**: Yes (`GET/POST /api/v1/purchases`, `POST /api/v1/purchases/{id}/receive`, `POST /api/v1/purchases/{id}/pay`).
- **Android Changes Required**: Yes (new screen).
- **Web Changes Required**: Yes (new tab).
- **iOS Changes Required**: Yes (new tab).
- **Risk**: High (Core financial & operational lifecycle).

---

### 6. Inventory Engine Domain
- **Status**: `CORRECT` (with minor enhancements needed)
- **Current Implementation**: Transactional ledger (`stock_movements`), pessimistic row locking (`lockForUpdate`), and warehouse stock tracking in `InventoryService.php`.
- **Intended Product Behavior**: Real-time multi-warehouse stock management reacting to Receiving, Sales, Reservations, Returns, Adjustments, and Transfers.
- **Gap**: Stock reservation lifecycle (order pending $\rightarrow$ stock reserved $\rightarrow$ order fulfilled $\rightarrow$ stock deducted) is not explicit. Unit valuation (FIFO / Weighted Average) is missing.
- **Recommended Change**: Keep existing `InventoryService.php` locking core. Add reservation/release methods and average unit cost tracking.
- **Files Affected**:
  - Backend: `app/Services/InventoryService.php`, `app/Models/WarehouseStock.php`
- **Database Migration Required**: No.
- **API Changes Required**: Yes (`POST /api/v1/inventory/reserve`, `POST /api/v1/inventory/release`).
- **Android Changes Required**: Minimal.
- **Web Changes Required**: Minimal.
- **iOS Changes Required**: Minimal.
- **Risk**: Low.

---

### 7. Sales & Order Domain
- **Status**: `MISALIGNED`
- **Current Implementation**: Sales orders are generated exclusively during POS instant checkout (`SalesOrderController@checkout`).
- **Intended Product Behavior**: Order domain is a universal commercial entity supporting draft orders, multi-channel origination (POS, Web, Manual, Instagram), status transitions (Pending, Processing, Shipped, Delivered, Cancelled), and partial payments.
- **Gap**: Orders cannot be placed in `PENDING` state or fulfilled separately from checkout. Channel attribution is absent.
- **Recommended Change**: Refactor `SalesOrderController` to separate Order Creation from Instant POS Checkout. Introduce explicit fulfillment and payment steps.
- **Files Affected**:
  - Backend: `app/Models/Order.php`, `app/Http/Controllers/Api/SalesOrderController.php`
  - Android: `ui/orders/SalesOrdersScreen.kt`, `ui/pos/PosViewModel.kt`
  - Web: `src/pages/PosPage.tsx`, `src/pages/OrdersPage.tsx`
  - iOS: `Calcuapp/Views/OrdersView.swift`
- **Database Migration Required**: Yes (add `channel_id`, `fulfillment_status`, `shipping_address` to `orders`).
- **API Changes Required**: Yes (`POST /api/v1/orders`, `POST /api/v1/orders/{id}/fulfill`, `POST /api/v1/orders/{id}/cancel`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: Medium.

---

### 8. Customer Management (CRM) Domain
- **Status**: `PARTIAL`
- **Current Implementation**: `customers` table, `CustomerController`, and client screens exist.
- **Intended Product Behavior**: First-class customer entity with credit limits, accounts receivable ledger, purchase history, and account statements.
- **Gap**: Accounts receivable (customer credit sales) is not integrated into sales checkout or double-entry accounting.
- **Recommended Change**: Integrate Accounts Receivable (`Dr Accounts Receivable`, `Cr Sales Revenue`) when an order is checked out on credit.
- **Files Affected**:
  - Backend: `app/Models/Customer.php`, `app/Http/Controllers/Api/CustomerController.php`, `app/Services/AccountingService.php`
  - Android: `ui/customer/CustomerScreen.kt`
  - Web: `src/pages/CustomersPage.tsx`
  - iOS: `Calcuapp/Views/CustomerView.swift`
- **Database Migration Required**: Yes (add `credit_limit`, `balance` to `customers`).
- **API Changes Required**: Yes (`GET /api/v1/customers/{id}/statement`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: Low.

---

### 9. Payments Domain
- **Status**: `PARTIAL`
- **Current Implementation**: Payments recorded as a nested step inside `SalesOrderController@checkout`.
- **Intended Product Behavior**: Decoupled payment module handling customer order payments, supplier purchase payments, expense payments, idempotency keys, and payment gateway references.
- **Gap**: Payments cannot be recorded independently (e.g. delayed customer payment or partial supplier installment). Missing `X-Idempotency-Key` validation.
- **Recommended Change**: Add `PaymentController` and `IdempotencyMiddleware` to guarantee network retry safety and standalone payment registration.
- **Files Affected**:
  - Backend: `app/Http/Controllers/Api/PaymentController.php`, `app/Http/Middleware/IdempotencyMiddleware.php`, `routes/api.php`
  - Android/Web/iOS: Payment dialogs and state flow.
- **Database Migration Required**: Yes (add `idempotency_key`, `reference_type` to `payments`).
- **API Changes Required**: Yes (`POST /api/v1/payments`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: Medium.

---

### 10. Expenses Domain
- **Status**: `MISSING`
- **Current Implementation**: Expenses are absent from models, controllers, database tables, and client interfaces.
- **Intended Product Behavior**: Expenses (advertising, shipping, packaging, rent, salaries, processing fees) are recorded as first-class business entries affecting accounting (`Dr Expense Account`, `Cr Cash/Bank/Payable`) and net profit calculations.
- **Gap**: Business operating costs cannot be recorded, preventing accurate Net Profit calculations.
- **Recommended Change**: Create `Expense` migration, model, controller, accounting integration, and dedicated client management interfaces.
- **Files Affected**:
  - Backend: `database/migrations/2025_01_01_000010_create_expenses_table.php`, `app/Models/Expense.php`, `app/Http/Controllers/Api/ExpenseController.php`, `routes/api.php`
  - Android: `ui/expenses/ExpensesScreen.kt`, `data/repository/ExpenseRepository.kt`
  - Web: `src/pages/ExpensesPage.tsx`
  - iOS: `Calcuapp/Views/ExpensesView.swift`
- **Database Migration Required**: Yes (`expenses` table).
- **API Changes Required**: Yes (`GET/POST/DELETE /api/v1/expenses`).
- **Android Changes Required**: Yes (new screen).
- **Web Changes Required**: Yes (new tab).
- **iOS Changes Required**: Yes (new tab).
- **Risk**: Medium.

---

### 11. Accounting Domain
- **Status**: `CORRECT` (with schema & precision updates needed)
- **Current Implementation**: `AccountingService.php` enforces strict `Debit == Credit` double-entry equality during journal entry posting.
- **Intended Product Behavior**: Complete financial engine reflecting Sales, COGS, Inventory, Accounts Payable, Accounts Receivable, Expenses, and Cash/Bank balances.
- **Gap**: Automatic journal posting only exists for POS cash sales. Purchasing (AP/Inventory), Expenses, and COGS entries are not automatically posted. Floating point numbers used instead of integer cents or BCMath.
- **Recommended Change**: Implement automatic journal hooks for Purchasing, Expenses, and COGS. Upgrade amounts to BCMath / integer precision.
- **Files Affected**:
  - Backend: `app/Services/AccountingService.php`, `app/Models/JournalEntry.php`
- **Database Migration Required**: Yes (update monetary columns to `DECIMAL(14,4)`).
- **API Changes Required**: Yes (`GET /api/v1/accounting/trial-balance`, `GET /api/v1/accounting/profit-loss`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: High (Financial data integrity).

---

### 12. Reports & Executive Dashboard
- **Status**: `MISALIGNED`
- **Current Implementation**: Dashboard currently shows basic POS sales total and total transactions.
- **Intended Product Behavior**: Comprehensive executive dashboard answering key business questions: Sales Revenue, COGS, Gross Profit, Expenses, Net Result, Accounts Payable, Accounts Receivable, Inventory Valuation, and Low Stock alerts.
- **Gap**: Current dashboard reflects only POS cash activity rather than total business financial state.
- **Recommended Change**: Create `ReportingService` and `ReportController` supplying true financial metrics derived from the double-entry accounting ledger.
- **Files Affected**:
  - Backend: `app/Services/ReportingService.php`, `app/Http/Controllers/Api/ReportController.php`, `routes/api.php`
  - Android: `ui/dashboard/DashboardViewModel.kt`, `ui/dashboard/DashboardScreen.kt`
  - Web: `src/pages/DashboardPage.tsx`
  - iOS: `Calcuapp/Views/DashboardView.swift`
- **Database Migration Required**: No.
- **API Changes Required**: Yes (`GET /api/v1/reports/dashboard`, `GET /api/v1/reports/profit-and-loss`).
- **Android Changes Required**: Yes.
- **Web Changes Required**: Yes.
- **iOS Changes Required**: Yes.
- **Risk**: Low.

---

### 13. Omnichannel Connector Domain
- **Status**: `MISALIGNED`
- **Current Implementation**: `channels`, `conversations`, and `messages` tables exist, but external channel connectors are absent or stubbed.
- **Intended Product Behavior**: Sales channels (Website, POS, Instagram, Telegram, Manual) feed external orders into a normalized ingress pipeline targeting the core Order domain.
- **Gap**: Sales channels logic is embedded directly inside POS checkout rather than abstracted through channel connectors.
- **Recommended Change**: Establish a clean Channel Ingress Service (`ChannelAdapterInterface`) that normalizes external payloads into core `Order` records.
- **Files Affected**:
  - Backend: `app/Services/ChannelIngressService.php`, `app/Http/Controllers/Api/ChannelController.php`
- **Database Migration Required**: Minimal.
- **API Changes Required**: Yes (`POST /api/v1/channels/{channel}/orders`).
- **Android Changes Required**: Minimal.
- **Web Changes Required**: Minimal.
- **iOS Changes Required**: Minimal.
- **Risk**: Medium.

---

## Conclusion & Architectural Recommendation

The technical foundation of Calcuapp (Laravel 11, PostgreSQL, Redis, Jetpack Compose, React Native Web, SwiftUI, double-entry accounting, pessimistic inventory locking) is exceptionally strong and fully reusable. 

However, the **business model gap** requires transitioning the application from a **POS-first tool** to a **Business Management Engine** by implementing the missing core operational loops: **Suppliers, Purchasing, Receiving, Accounts Payable, Expenses, Accounts Receivable, and Business-Wide Profitability Reporting**.
