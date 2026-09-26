# Calcuapp Domain Review — Corrected Domain Model Architecture

**Date**: March 2025  
**Author**: Senior Product Architect & Technical Lead  
**Repository**: Calcuapp Monorepo (`/Users/aminshojaei/AndroidStudioProjects/Calcuapp`)

---

## 1. Executive Summary

This document describes the corrected domain model architecture for Calcuapp. Calcuapp is a multi-tenant business and commerce management platform designed to manage the end-to-end commercial and financial lifecycle of online and offline businesses.

The central domain model establishes that **POS, Inventory, Accounting, Purchasing, CRM, and Reporting** are interconnected modules operating on top of a unified backend domain layer.

---

## 2. Comprehensive Domain Relationship Diagram

```text
                                 +-------------------------+
                                 |  Organization / Tenant  |
                                 +------------+------------+
                                              |
      +-------------------+-------------------+-------------------+-------------------+
      |                   |                   |                   |                   |
      v                   v                   v                   v                   v
+-----------+       +-----------+       +-----------+       +-----------+       +-----------+
| Users &   |       |  Stores   |       | Warehouses|       | Product   |       |  Chart of |
| Roles     |       +-----+-----+       +-----+-----+       | Catalog   |       | Accounts  |
+-----------+             |                   |             +-----+-----+       +-----+-----+
                          |                   |                   |                   |
                          v                   v                   v                   |
                    +-----------+       +-----------+       +-----------+             |
                    |  Channels |       | Warehouse |       | Suppliers |             |
                    +-----+-----+       | Stock     |       +-----+-----+             |
                          |             +-----+-----+             |                   |
                          |                   ^                   |                   |
                          |                   | (Receiving)       v                   |
                          |                   |             +-----------+             |
                          v                   +-------------|Purchasing |             |
                    +-----------+                           +-----+-----+             |
                    | Core      |                                 |                   |
                    | Orders    |<--------------------------------+ (AP Entry)        |
                    +-----+-----+                                 |                   |
                          |                                       v                   v
            +-------------+-------------+                   +-----------+       +-----------+
            |                           |                   | Expenses  |------>| Double-   |
            v                           v                   +-----------+       | Entry     |
      +-----------+               +-----------+                   |             | General   |
      | Customers |               | Payments  |-------------------+------------>| Ledger    |
      +-----------+               +-----------+                                 +-----+-----+
                                                                                      |
                                                                                      v
                                                                                +-----------+
                                                                                | Reports & |
                                                                                | Executive |
                                                                                | Dashboard |
                                                                                +-----------+
```

---

## 3. Domain Descriptions & Key Business Rules

### 3.1 Business / Organization Domain
- **Role**: Primary isolation boundary and legal business entity.
- **Entities**: `Organization`, `Store`, `Warehouse`.
- **Relationships**: An Organization owns multiple Users, Stores, Warehouses, Products, Suppliers, Customers, Accounts, and Channels.
- **Key Business Rules**:
  - All database queries must scope data strictly by `organization_id`.
  - Client-provided organization/store IDs in API headers (`X-Tenant-ID`, `X-Store-ID`) must be validated against user active memberships in backend middleware.

---

### 3.2 Users, Roles & Permissions Domain
- **Role**: Identity, access control, and user permission management.
- **Entities**: `User`, `OrganizationMembership`, `Role`, `Permission`.
- **Relationships**: A User belongs to multiple Organizations via `OrganizationMembership`. Each membership grants domain permissions.
- **Key Business Rules**:
  - Domain permissions are platform-independent strings (e.g. `inventory.view`, `inventory.adjust`, `purchase.create`, `purchase.approve`, `accounting.post`, `sales.checkout`).
  - Authorization guards must be enforced consistently across REST API endpoints, Web admin panels, Android, and iOS client UIs.

---

### 3.3 Product Catalog Domain
- **Role**: Global master record of goods, raw materials, and services available for resale or manufacturing.
- **Entities**: `Product`, `ProductVariant`, `Category`, `Brand`, `Unit`.
- **Relationships**: Products belong to an Organization. A Product has multiple Variants, a primary Supplier, a Category, a Tax Class, and default selling and cost prices.
- **Key Business Rules**:
  - Products exist independently of sales channels. A single product is sold across POS, Web, and Marketplaces without record duplication.
  - Cost price (`cost_price`) serves as the base for purchase valuation and Cost of Goods Sold (COGS) calculations.

---

### 3.4 Supplier Management Domain
- **Role**: First-class entity representing vendors from whom goods, raw materials, or services are purchased.
- **Entities**: `Supplier`, `SupplierContact`, `SupplierStatement`.
- **Relationships**: A Supplier belongs to an Organization, sells Products, receives Purchase Orders, generates Accounts Payable, and receives Supplier Payments.
- **Key Business Rules**:
  - Supplier statement tracks total purchases, returns, payments made, and current balance payable ($AP$).
  - Overdue payables affect credit terms and business purchasing decisions.

---

### 3.5 Purchasing & Goods Receiving Domain
- **Role**: Procurement lifecycle for ordering inventory and raw materials from suppliers.
- **Entities**: `Purchase`, `PurchaseItem`, `ReceivingLog`.
- **Relationships**: A Purchase is issued to a Supplier, references Products, targets a specific Warehouse, and generates inventory stock updates and accounting entries.
- **Lifecycle**: Draft $\rightarrow$ Approved / Ordered $\rightarrow$ Partially Received $\rightarrow$ Fully Received $\rightarrow$ Accounts Payable Posted $\rightarrow$ Paid.
- **Key Business Rules**:
  - Receiving goods into a Warehouse triggers an explicit inventory `IN` movement in `stock_movements`.
  - Fully received purchases generate double-entry accounting entries:
    $$\text{Dr Inventory (Asset)} \quad \text{at Total Cost}$$
    $$\text{Cr Accounts Payable (Liability)} \quad \text{at Total Cost}$$

---

### 3.6 Inventory Engine Domain
- **Role**: Operational transaction-ledger tracking stock quantity, location, state, and movements.
- **Entities**: `WarehouseStock`, `StockMovement`, `StockReservation`, `StockTransfer`.
- **Relationships**: Stock is stored in a Warehouse per Product/Variant.
- **Inventory Equality**:
  $$\text{Available Stock} = \text{On Hand Stock} - \text{Reserved Stock}$$
- **Key Business Rules**:
  - Source of truth is transactional movements, not a mutable scalar quantity.
  - Concurrency safety during stock updates is enforced using database pessimistic row locking (`lockForUpdate()`).
  - Stock movements react to: Purchase Receiving (`IN`), Sales Fulfillment (`OUT`), Order Reservation (`RESERVE`), Reservation Release (`CANCEL`), Damage/Adjustment (`ADJUST`), and Warehouse Transfers (`TRANSFER`).

---

### 3.7 Core Order & Sales Domain
- **Role**: Central commercial domain recording customer orders, sales, fulfillments, and cancellations across all channels.
- **Entities**: `Order`, `OrderItem`, `FulfillmentLog`.
- **Relationships**: An Order belongs to an Organization, Store, Warehouse, and optional Customer. Originated via a Channel.
- **Lifecycle**: Draft $\rightarrow$ Pending $\rightarrow$ Confirmed $\rightarrow$ Paid $\rightarrow$ Processing $\rightarrow$ Packed $\rightarrow$ Shipped $\rightarrow$ Delivered $\rightarrow$ Completed (or Cancelled / Refunded).
- **Key Business Rules**:
  - Sales logic is channel-independent. POS, Website, and Manual sales generate normalized records in the core Order domain.
  - Order completion triggers inventory deduction and double-entry revenue and COGS journal entries:
    $$\text{Dr Cash / Bank / Accounts Receivable} \quad \text{at Sales Total}$$
    $$\text{Cr Sales Revenue} \quad \text{at Sales Total}$$
    $$\text{Dr Cost of Goods Sold (COGS)} \quad \text{at Inventory Cost}$$
    $$\text{Cr Inventory (Asset)} \quad \text{at Inventory Cost}$$

---

### 3.8 Customer Management (CRM) Domain
- **Role**: Commercial record of buyers, credit history, receivables, and engagement.
- **Entities**: `Customer`, `CustomerAddress`, `CustomerStatement`.
- **Relationships**: A Customer belongs to an Organization, places Orders, holds an Accounts Receivable ($AR$) balance, and makes Payments.
- **Key Business Rules**:
  - Tracks customer credit limits. Credit sales post to Accounts Receivable:
    $$\text{Dr Accounts Receivable (Asset)}$$
    $$\text{Cr Sales Revenue}$$
  - Customer payments settle $AR$ balances:
    $$\text{Dr Cash / Bank}$$
    $$\text{Cr Accounts Receivable}$$

---

### 3.9 Payments Domain
- **Role**: Financial settlement layer operating independently from sales UI channels.
- **Entities**: `Payment`, `PaymentMethod`, `Refund`.
- **Relationships**: Payments settle Orders, Supplier Payables, or Expense invoices.
- **Key Business Rules**:
  - Supports multiple methods: Cash, Bank Transfer, POS Card Terminal, Payment Gateway, Customer Credit.
  - Critical payment endpoints require client `X-Idempotency-Key` headers to prevent duplicate network execution.

---

### 3.10 Expenses Domain
- **Role**: Recording operational, administrative, selling, and logistics expenses.
- **Entities**: `Expense`, `ExpenseCategory`, `Vendor`.
- **Relationships**: Expenses belong to an Organization, Store, and Expense Category (e.g. Rent, Salaries, Advertising, Shipping, Packaging, Gateway Fees).
- **Key Business Rules**:
  - Expenses generate double-entry accounting entries:
    $$\text{Dr Expense Account (e.g. Advertising Expense)}$$
    $$\text{Cr Cash / Bank / Accounts Payable}$$
  - Expenses directly reduce business Net Profit in executive reporting.

---

### 3.11 Financial Accounting Engine Domain
- **Role**: Financial representation of business operations using true double-entry bookkeeping.
- **Entities**: `ChartOfAccounts`, `Account`, `Journal`, `JournalEntry`, `JournalEntryLine`.
- **Key Accounting Principle**:
  $$\sum \text{Debits} = \sum \text{Credits} \quad \text{for every posted Journal Entry}$$
- **Account Types**: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE.
- **Key Business Rules**:
  - Financial history is immutable. Corrections must be executed via reversing journal entries.
  - Monetary values must maintain exact decimal precision using BCMath or integer cents to prevent floating-point rounding errors.

---

### 3.12 Reports & Executive Dashboard Domain
- **Role**: Business intelligence and financial analytics for decision makers.
- **Entities**: `DashboardMetrics`, `ProfitAndLossStatement`, `BalanceSheet`, `TrialBalance`, `StockValuationReport`, `APARSummary`.
- **Key Business Formulae**:
  $$\text{Gross Profit} = \text{Sales Revenue} - \text{Cost of Goods Sold (COGS)}$$
  $$\text{Net Profit} = \text{Gross Profit} - \text{Total Operating Expenses}$$
- **Key Business Rules**:
  - Reports reflect total business financial activity across all channels and locations, not merely POS activity.

---

### 3.13 Sales Channels & Ingress Connectors
- **Role**: External connectivity layer mapping incoming order channels (Website, POS, Instagram, Telegram, Marketplaces) into the core Order domain.
- **Entities**: `Channel`, `ChannelConnector`, `ChannelWebhook`.
- **Key Business Rules**:
  - Channels contain zero core inventory or accounting logic; they normalize payload formats and pass orders into shared domain services.
