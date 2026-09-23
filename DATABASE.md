# DATABASE.md - Database Schema & Data Integrity Design

## 1. Schema Overview (PostgreSQL / Room)

### Core Tables & Entities

#### `organizations` (Tenants)
- `id` (VARCHAR PK)
- `name` (VARCHAR)
- `code` (VARCHAR)
- `currency_symbol` (VARCHAR)
- `currency_code` (VARCHAR)
- `subscription_tier` (VARCHAR)

#### `stores`
- `id` (VARCHAR PK)
- `org_id` (VARCHAR FK -> organizations.id)
- `name` (VARCHAR)
- `code` (VARCHAR)
- `address` (TEXT)
- `phone` (VARCHAR)

#### `warehouses`
- `id` (VARCHAR PK)
- `store_id` (VARCHAR FK -> stores.id)
- `org_id` (VARCHAR FK -> organizations.id)
- `name` (VARCHAR)
- `code` (VARCHAR)

#### `products`
- `id` (VARCHAR PK)
- `org_id` (VARCHAR FK -> organizations.id)
- `sku` (VARCHAR)
- `barcode` (VARCHAR INDEX)
- `name` (VARCHAR)
- `description` (TEXT)
- `price` (NUMERIC)
- `cost_price` (NUMERIC)
- `category` (VARCHAR)
- `unit` (VARCHAR)

#### `sales_orders`
- `id` (VARCHAR PK)
- `order_number` (VARCHAR INDEX)
- `org_id` (VARCHAR FK -> organizations.id)
- `store_id` (VARCHAR FK -> stores.id)
- `warehouse_id` (VARCHAR FK -> warehouses.id)
- `customer_id` (VARCHAR FK -> customers.id, NULLABLE)
- `total_amount` (NUMERIC)
- `payment_method` (VARCHAR)
- `payment_status` (VARCHAR)
- `fulfillment_status` (VARCHAR)
- `created_at` (TIMESTAMP)

#### `ledger_entries` (Double-Entry Financial Accounting)
- `id` (VARCHAR PK)
- `org_id` (VARCHAR FK -> organizations.id)
- `store_id` (VARCHAR FK -> stores.id)
- `entry_number` (VARCHAR)
- `type` (VARCHAR: DEBIT, CREDIT)
- `category` (VARCHAR: SALES, INVENTORY_ADJUSTMENT, EXPENSE, CASH_IN, CASH_OUT, REFUND)
- `amount` (NUMERIC)
- `description` (TEXT)
- `reference_id` (VARCHAR, NULLABLE)
- `created_at` (TIMESTAMP)
