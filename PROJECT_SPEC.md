# PROJECT_SPEC.md - Calcuapp SaaS Platform Specification

## 1. Executive Summary
Calcuapp is a comprehensive, multi-tenant SaaS platform providing Point of Sale (POS), Inventory Stock Management, Double-Entry Financial Accounting, Customer CRM, and Sales Analytics across Mobile (Android, iOS) and Web platforms, backed by a scalable Laravel REST API and PostgreSQL database.

## 2. Core Functional Requirements

### 2.1 Multi-Tenant Organization & Store Hierarchy
- Multi-organization tenant isolation (`Organization` -> `Store` -> `Warehouse`).
- User role-based access control (Owner, Store Manager, Cashier, Inventory Specialist).
- Real-time active tenant context switching in mobile and web clients.

### 2.2 Point of Sale (POS) & Checkout
- Product catalog search, category filtering, and variant selection.
- Touch-friendly dynamic cart management with quantity controls and line-item discounts.
- CameraX live barcode scanning and manual barcode entry.
- Customer profile attachment for loyalty points and transaction history.
- Payment processing (Cash, Card / POS terminal, Mobile / QR, Store Credit) with automatic cash change math.
- Thermal receipt generation and transaction recording.

### 2.3 Real-Time Inventory & Warehouse Management
- Live product stock lookup by warehouse.
- Warehouse-level stock adjustments (+/-) with audit reason logging (Restock, Damage, Audit, Return, Transfer).
- Automated double-entry ledger inventory asset valuation adjustments.
- Low-stock alerts and SKU count tracking.

### 2.4 Double-Entry Financial & Sales Dashboard
- Real-time revenue metrics: Total Revenue, Today's Sales, Net Operating Profit, Expenses, Inventory Value.
- General Ledger Activity Log capturing itemized debit/credit entries.
- Manual financial entry recording (Cash In, Cash Out, Operating Expense).

### 2.5 Customer Management (CRM)
- Customer lookup by name, phone, email.
- Loyalty points accumulator (1 point per $10 spent).
- Customer lifetime spend history.

## 3. Technical Platform Targets
- **Android**: Kotlin, Jetpack Compose Material Design 3, Room DB, DataStore, CameraX, Retrofit/Moshi.
- **Backend**: Laravel 11, PHP 8.3, PostgreSQL 16, Redis, Docker.
- **Web**: React Native Web / Next.js, Tailwind CSS, TypeScript.
- **iOS**: Swift 5.10, SwiftUI, SwiftData / CoreData.
