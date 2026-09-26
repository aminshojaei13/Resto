Calcuapp — Product Specification

1. Product Vision

Calcuapp is a multi-tenant SaaS platform for managing online and commerce-based businesses.

The primary user is a business owner, manager, or authorized employee who needs to manage the complete operational and financial lifecycle of a business from one system.

The core business lifecycle is:

Business → Products → Suppliers → Purchasing → Inventory → Sales → Customers → Payments → Expenses → Accounting → Reports

The product is NOT primarily a POS application.

POS is only one possible sales channel.

The product is NOT primarily an accounting application.

Accounting is the financial representation of business operations.

The product is NOT merely an inventory application.

Inventory is the operational representation of purchases, sales, returns, transfers, adjustments, and fulfillment.

⸻

2. Core Business Scenario

A typical user may operate an online business that purchases goods or raw materials from suppliers and sells finished goods/products to customers.

Example:

1. User creates a business.
2. User adds products.
3. User adds suppliers.
4. User purchases goods or raw materials from a supplier.
5. Goods are received into a warehouse.
6. Inventory increases.
7. Supplier payable is created.
8. User pays the supplier.
9. Customer places an order.
10. Order may originate from Website, Instagram, Telegram, POS, Marketplace, or manual entry.
11. Inventory is reserved/deducted.
12. Customer payment is recorded.
13. Order is fulfilled/shipped.
14. Revenue and cost of goods sold are recorded.
15. Expenses are recorded.
16. Accounting reflects the complete transaction lifecycle.
17. Dashboard and reports show sales, purchases, costs, profit, inventory, receivables, and payables.

This complete lifecycle is the central product model.

⸻

3. Product Positioning

Calcuapp should be designed as:

Business Management Platform / Commerce Management Platform

Potential future positioning:

* Business Operating System
* Commerce Management Platform
* Online Business Management SaaS

Do NOT position the product primarily as:

* POS software
* Accounting software
* Inventory software
* CRM software

Those are modules within the larger platform.

⸻

4. Target Users

Primary:

* Online store owners
* Small and medium businesses
* Ecommerce operators
* Retail businesses
* Businesses selling through social media
* Businesses with physical + online sales
* Businesses purchasing goods for resale
* Businesses purchasing raw materials for production
* Business managers
* Employees with delegated permissions

The system must support growth from a single-person business to a multi-user, multi-store business.

⸻

5. Business Hierarchy

Core hierarchy:

Organization / Business
↓
Users & Roles
↓
Stores
↓
Warehouses
↓
Products / Catalog
↓
Sales Channels

A user may belong to multiple organizations.

An organization may contain:

* multiple users
* multiple stores
* multiple warehouses
* multiple sales channels
* multiple suppliers
* multiple customers

⸻

6. Core Modules

6.1 Dashboard

The dashboard provides a business overview.

It should include, where data exists:

* Sales
* Purchases
* Gross profit
* Expenses
* Net result
* Orders
* Customers
* Inventory value
* Low-stock products
* Receivables
* Payables
* Cash/bank balances
* Recent transactions
* Sales by channel

Dashboard must represent the business, not only POS activity.

⸻

7. Product Catalog

Products belong to the organization and can be used across stores/channels.

Product may contain:

* Name
* SKU
* Barcode
* Description
* Images
* Category
* Brand
* Unit
* Tax class
* Variants
* Purchase price
* Selling price
* Minimum stock level
* Active/inactive status

Variants may contain:

* SKU
* Barcode
* Attributes
* Purchase cost
* Selling price
* Inventory identity

Do not duplicate the same product merely because it is sold through different channels.

⸻

8. Suppliers

Suppliers are first-class business entities.

A supplier may have:

* Name
* Contact information
* Address
* Tax information
* Payment terms
* Balance
* Purchase history
* Payment history
* Returns
* Account statement

Supplier lifecycle:

Supplier
→ Purchase
→ Receive
→ Payable
→ Payment
→ Supplier statement

⸻

9. Purchasing

Purchasing is a core module.

A purchase may contain:

* Supplier
* Store
* Warehouse
* Products
* Quantities
* Unit costs
* Discounts
* Taxes
* Shipping costs
* Other costs
* Total
* Payment status
* Receiving status
* Date
* Reference number

Lifecycle:

Draft
→ Confirmed
→ Partially Received
→ Received
→ Partially Paid
→ Paid

Purchasing must integrate with:

* Inventory
* Supplier balance
* Accounts payable
* Accounting
* Expenses/costs

⸻

10. Inventory

Inventory is transaction/ledger based.

Do NOT rely on a single mutable product quantity as the source of truth.

Inventory events include:

* Purchase receiving
* Sale
* Reservation
* Reservation release
* Return
* Damage
* Adjustment
* Transfer
* Opening balance

Inventory states/concepts:

* On Hand
* Reserved
* Available
* Incoming
* In Transit
* Damaged
* Returned

Available:

Available = On Hand - Reserved

Inventory must support multiple warehouses.

Inventory operations must be transactional and concurrency-safe.

⸻

11. Sales

Sales are not limited to POS.

A sale/order can originate from:

* Website
* Instagram
* Telegram
* POS
* Marketplace
* Manual entry
* API
* Future integrations

All sales should ultimately use a common core order/sales domain.

Example:

Order
→ Payment
→ Reservation
→ Fulfillment
→ Shipment
→ Delivery
→ Accounting

Order states may include:

* Draft
* Pending
* Confirmed
* Paid
* Processing
* Packed
* Shipped
* Delivered
* Cancelled
* Returned
* Refunded

⸻

12. Sales Channels

Sales channels are separate from the core order model.

Conceptually:

Channel
→ Connector
→ Normalized incoming data
→ Order creation
→ Core Order/Sales domain

Initial channels:

* Manual
* POS
* Website

Future:

* Instagram
* Telegram
* Marketplace integrations
* External ecommerce platforms

Channel integrations must not create separate business logic for inventory/accounting.

All channels must use shared domain services.

⸻

13. Customers

Customer is a first-class business entity.

Customer may contain:

* Profile
* Contact information
* Addresses
* Orders
* Payments
* Returns
* Refunds
* Receivables
* Account statement

Customer history should provide a complete view of commercial activity.

⸻

14. Payments

Payments must be independent from the UI channel.

Payment sources may include:

* Cash
* Bank transfer
* Card
* Payment gateway
* Wallet
* Other configured methods

Payment lifecycle must support:

* Pending
* Authorized
* Paid
* Failed
* Refunded
* Partially refunded

Critical payment operations must be idempotent.

⸻

15. Expenses

Expenses are a core business feature.

Examples:

* Advertising
* Shipping
* Packaging
* Rent
* Salaries
* Payment gateway fees
* Marketplace commissions
* Import costs
* Transportation
* Other operating expenses

Expenses must integrate with accounting and reporting.

⸻

16. Accounting

Accounting represents business operations financially.

Use true double-entry accounting.

Core entities:

* ChartOfAccounts
* Account
* Journal
* JournalEntry
* JournalEntryLine

Every posted journal must satisfy:

Total Debits = Total Credits

Examples:

Purchase:

Dr Inventory
Cr Accounts Payable

Supplier payment:

Dr Accounts Payable
Cr Cash/Bank

Sale:

Dr Cash/Bank/Accounts Receivable
Cr Sales Revenue

Cost of goods sold:

Dr COGS
Cr Inventory

Expense:

Dr Expense Account
Cr Cash/Bank/Payable

Corrections must preserve audit history through reversal/correction rather than destructive mutation.

⸻

17. Reports

Reports must answer practical business questions.

Examples:

* Sales report
* Purchase report
* Gross profit
* Net result
* Expense report
* Inventory valuation
* Stock movement
* COGS
* Customer statement
* Supplier statement
* Accounts receivable
* Accounts payable
* Cash/bank
* General ledger
* Journal
* Trial balance
* Balance sheet
* Profit and loss
* Sales by channel
* Product profitability

⸻

18. Multi-Tenancy

Organizations must be isolated.

Tenant isolation must apply to:

* API
* Database queries
* Authentication
* Authorization
* Cache
* Queues
* Files
* WebSockets
* Reports

Authenticated users must be verified as members of the requested organization.

Client-provided tenant/store IDs are never trusted without server-side authorization.

⸻

19. Roles and Permissions

Permissions must be domain based and platform independent.

Examples:

inventory.view
inventory.adjust
inventory.transfer

purchase.view
purchase.create
purchase.approve
purchase.receive

sales.view
sales.create
sales.cancel

customers.view
customers.manage

suppliers.view
suppliers.manage

accounting.view
accounting.post

expenses.view
expenses.create

users.manage
settings.manage

Permissions must work consistently across Android, iOS, Web, and API.

⸻

20. Architecture Principle

The backend is the source of truth.

Clients are presentation and interaction layers.

Business rules must not be duplicated independently in:

* Android
* iOS
* Web

Core business rules belong to the backend/domain layer.

⸻

21. Financial Data Integrity

Money must never use binary floating-point as the financial source of truth.

Use exact decimal/integer monetary representation.

Define:

* currency
* scale
* rounding policy
* tax rounding
* discount rounding

All clients must preserve financial precision.

⸻

22. Idempotency

Critical operations must support idempotency.

At minimum:

* Payment
* Refund
* Order creation
* POS checkout
* Purchase receiving
* Inventory transfer
* Inventory adjustment
* Accounting posting
* Webhook processing

Duplicate requests must not create duplicate financial or inventory effects.

⸻

23. Auditability

Important business actions must be auditable.

Audit events include:

* Login
* Permission changes
* Product changes
* Purchase creation
* Purchase receiving
* Inventory adjustment
* Inventory transfer
* Order creation
* Order cancellation
* Payment
* Refund
* Accounting posting
* Configuration changes

Financial and inventory records must not be silently deleted or rewritten.

⸻

24. User Experience

The application must be understandable to a normal business owner.

The main navigation should be business-oriented.

Suggested structure:

Dashboard
Sales
Orders
Products
Inventory
Purchases
Suppliers
Customers
Expenses
Payments
Accounting
Reports
Settings

POS should be accessible under Sales rather than defining the entire application around POS.

⸻

25. Localization

Primary UI language:

Persian (fa)

Direction:

RTL

English should remain supported through localization architecture.

Internal:

* database fields
* API contracts
* class names
* enum identifiers
* service names

must remain stable and language independent.

⸻

26. Existing Code

The existing implementation must NOT be discarded.

Reuse existing:

* Laravel backend
* PostgreSQL
* Redis
* Android
* Web
* iOS
* API
* Inventory service
* Accounting service
* authentication
* tenant structure
* tests
* Docker
* CI/CD

Refactor only where required by this product specification.

⸻

27. Product Priority

Priority order:

P0 — Core business lifecycle

1. Organization/business
2. Users/permissions
3. Products
4. Suppliers
5. Purchasing
6. Inventory
7. Customers
8. Sales/orders
9. Payments
10. Expenses
11. Accounting
12. Reports

P1 — Commerce channels

13. Website
14. POS
15. Manual sales
16. Channel architecture

P2 — Integrations

17. Instagram
18. Telegram
19. Marketplaces
20. Payment gateways
21. Shipping integrations

P3 — Advanced features

22. Automation
23. Advanced analytics
24. Additional integrations

⸻

28. Definition of Product Success

A business owner should be able to:

Create business
→ Add supplier
→ Add product
→ Purchase product
→ Receive product
→ See inventory
→ Sell product
→ Receive customer payment
→ Track customer
→ Track supplier
→ Record expenses
→ See accounting
→ See profit
→ See reports

without requiring separate software for each step.

This complete business lifecycle is the primary success criterion of Calcuapp.