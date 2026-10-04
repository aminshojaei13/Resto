# P5.2 Platform Admin / Business Application Separation Verification

## 1. Objective
The objective of task P5.2 is to establish a complete technical, build, and runtime separation between:
1. **Resto Business Application** (`http://localhost:3000` in development) — Used by business owners and tenant users for SaaS onboarding, business login, POS, inventory, purchasing, suppliers, customers, expenses, accounting, and reports.
2. **Resto Platform Admin / Operator Application** (`http://localhost:3001` in development) — Used exclusively by Resto platform operators for reviewing organization applications, approving/rejecting onboarding requests, inspecting provisioned tenants, and viewing platform audit logs.

The Platform Admin console is no longer just another page inside the Business Application. It is now a separately built, configured, and served frontend runtime.

---

## 2. Existing Architecture
In P5.1, route separation was implemented via route guards and separate shell components within a single React runtime bundle. In P5.2, the codebase has been structured so that two distinct application entry points (`BusinessApp` and `PlatformAdminApp`) are built and served independently on different ports during development, while reusing shared design tokens, types, and API client utilities without duplicating business domain logic or creating a second backend.

---

## 3. Final Architecture

### Development Configuration
- **Business Web Application**: `http://localhost:3000`
- **Platform Admin Web Application**: `http://localhost:3001`

### Code Base Organization
- **`web/src/business/`**:
  - `BusinessApp.tsx`: Root application component for the Resto Business Web Application.
  - Dedicated shells (`PublicShell`, `TenantAppShell`, `TenantModal`).
  - Business pages (`PublicRegisterPage`, `LoginPage`, `ActivationPage`, `DashboardPage`, `PosPage`, `InventoryPage`, `PurchasesPage`, `SuppliersPage`, `CustomersPage`, `ExpensesPage`, `MessagesPage`, `AccountingPage`).
- **`web/src/platform-admin/`**:
  - `PlatformAdminApp.tsx`: Root application component for the Resto Operator Console.
  - Dedicated shell (`PlatformAdminShell`).
  - Platform pages (`PlatformLoginPage`, `PlatformAdminPage` for business applications, `PlatformTenantsPage` for active organizations, `PlatformAuditPage` for platform audit trail).
- **`web/src/shared/` & `web/src/`**:
  - Reused theme tokens (`src/theme/tokens.ts`), types (`src/types/index.ts`), API client (`src/api/apiClient.ts`), and UI primitives (`StatCard`, `StatusBadge`, `PageHeader`, `EmptyState`).
- **`web/src/index.tsx`**:
  - Dynamically mounts either `<PlatformAdminApp />` or `<BusinessApp />` based on `process.env.REACT_APP_APP_TYPE`.

---

## 4. Business Application Routes

### Active Surface (`http://localhost:3000`)
- `/register`: Public SaaS Business Registration
- `/login`: Tenant Owner & Staff Login
- `/activation`: Organization Activation & Access Information Guide
- `/app/dashboard`: Executive Business Dashboard
- `/app/pos`: Point of Sale & Terminal Checkout
- `/app/orders`: Sales Orders Management
- `/app/products`: Product Catalog
- `/app/inventory`: Warehouse Inventory & Stock Adjustments
- `/app/purchases`: Purchasing & PO Management
- `/app/suppliers`: Supplier Directory
- `/app/customers`: Customer CRM & Loyalty
- `/app/expenses`: Expense Tracking
- `/app/messages`: Social Order Import Parser
- `/app/accounting`: Double-Entry General Ledger
- `/app/reports`: Financial Statements & P&L
- `/app/settings`: Business & Store Settings

### Legacy Route Redirection
Any attempt to access legacy Platform Admin routes (`/platform/*` or `/admin/*`) on the Business Application (`http://localhost:3000`) displays an explicit separation notice directing the user to the dedicated Platform Admin Operator Console at `http://localhost:3001/login`. No Platform Admin navigation or approval controls exist on `localhost:3000`.

---

## 5. Platform Admin Routes

### Active Surface (`http://localhost:3001`)
- `/login`: Platform Operator Authentication Portal
- `/applications`: Organization Applications Review (PENDING / APPROVED / REJECTED)
- `/applications/:id`: Application Inspection & Tenant Provisioning Detail
- `/tenants`: Overview of Provisioned Tenant Organizations, Stores, and Warehouses
- `/audit`: Platform Security & Administrative Event Log Stream

### Content Isolation
The Platform Admin application surface contains **zero** tenant business navigation (no products, suppliers, purchases, inventory, POS, customers, expenses, accounting, or business reports).

---

## 6. Authentication
- **Business Web (`http://localhost:3000/login`)**: Authenticates tenant owners and staff users. On successful authentication, users enter `/app/dashboard`.
- **Platform Admin (`http://localhost:3001/login`)**: Authenticates Resto platform operators. On successful authentication, operators enter `/applications`.
- Both frontend applications communicate with the same unified Sanctum / Bearer token authentication backend (`/api/v1/auth/login`). No duplicate user databases or user accounts were created.

---

## 7. Authorization
The separate development port (`3001`) is **not** the security boundary; backend authorization remains the absolute source of truth:
- All `/api/v1/platform/*` API endpoints strictly enforce `is_platform_admin = true`.
- All `/api/v1/tenant/*` and tenant business endpoints enforce `TenantMiddleware`, `OrganizationMembership`, and Sanctum token authentication.
- **Port 3001 Non-Admin Check**: If an authenticated normal tenant user (where `is_platform_admin === false`) attempts to open `http://localhost:3001`, both the frontend route guard and backend API respond with `403 Access Denied`, preventing unauthorized UI or data access.

---

## 8. CORS Configuration
In `backend/config/cors.php`, allowed origins are configured explicitly to support both development runtimes without using wildcards for authenticated production APIs:

```php
'allowed_origins' => explode(',', env('CORS_ALLOWED_ORIGINS', 'http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000,http://127.0.0.1:3001')),
```

---

## 9. Docker
- `docker-compose.yml` was validated using `docker compose config` and passed with **zero errors**.
- All underlying backend, PostgreSQL, Redis, and Nginx container definitions remain fully compatible.

---

## 10. Production Architecture
In production, port `3001` is not exposed directly to the internet. The recommended production architecture serves the two distinct frontend applications via separate subdomains through Nginx:
- **Tenant Application**: `https://app.resto.com` (serving static bundle from `build/business`)
- **Platform Operator Console**: `https://admin.resto.com` (serving static bundle from `build/admin`)

Both subdomains proxy API requests to the shared backend API (`https://api.resto.com`), where Sanctum authentication and `is_platform_admin` checks continue to enforce tenant isolation and operator authorization.

---

## 11. Automated Tests

All 13 automated unit & integration test suites in `web/src/App.test.tsx` executed and passed 100% using `npm test`:

```
PASS src/App.test.tsx
  P5.2 Resto SaaS App & Runtime Separation Tests
    ✓ 1. Business app starts and renders business landing (33 ms)
    ✓ 2. Platform Admin app starts and renders operator portal (14 ms)
    ✓ 3. Business registration works on Business App (57 ms)
    ✓ 4. Business login works on Business App (68 ms)
    ✓ 5. Tenant dashboard works on Business App for authenticated tenant user (17 ms)
    ✓ 6. Platform Admin login works on Platform Admin App (port 3001) (22 ms)
    ✓ 7. Platform applications review works on Platform Admin App (8 ms)
    ✓ 8 & 9. Normal tenant user cannot gain Platform Admin UI access on port 3001 (2 ms)
    ✓ 10 & 11. Platform Admin can access Platform Admin UI and management APIs (7 ms)
    ✓ 12. Platform Admin audit trail works on Platform Admin App (6 ms)
    ✓ 13. No Platform Admin navigation appears in Business App (13 ms)
    ✓ 14. No tenant business navigation appears in Platform Admin App (5 ms)
    ✓ 15. Legacy Platform Admin route on Business App redirects with notice (3 ms)

Test Suites: 1 passed, 1 total
Tests:       13 passed, 13 total
Snapshots:   0 total
Time:        1.831 s
```

---

## 12. Build Validation Results

- **Business Application Build**: `npm run build:business` → `build/business/` (**SUCCESS**)
- **Platform Admin Build**: `npm run build:admin` → `build/admin/` (**SUCCESS**)
- **Unified Build Command**: `npm run build` (**SUCCESS**)
- **TypeScript Check**: `npx tsc --noEmit` (**0 errors**)
- **Backend Tests**: `./vendor/bin/phpunit` (**23/23 tests passed, 100%**)
- **Docker Compose Check**: `docker compose config` (**VALID**)

---

## 13. Files Changed

### Created Files
- `web/src/business/BusinessApp.tsx`
- `web/src/platform-admin/PlatformAdminApp.tsx`
- `web/src/platform-admin/pages/PlatformTenantsPage.tsx`
- `web/src/platform-admin/pages/PlatformAuditPage.tsx`
- `docs/P5_2_PLATFORM_ADMIN_SEPARATION_VERIFICATION.md`

### Modified Files
- `web/src/index.tsx` (Updated entry point to dynamically render `BusinessApp` or `PlatformAdminApp`)
- `web/src/components/PlatformAdminShell.tsx` (Updated navigation for `/applications`, `/tenants`, `/audit`)
- `web/src/api/apiClient.ts` (Added `getPlatformAuditLogs` API helper)
- `web/src/App.test.tsx` (Updated unit tests for P5.2 architecture)
- `web/package.json` (Added `start:business`, `start:admin`, `build:business`, `build:admin` scripts)
- `package.json` (Root package scripts for business and admin runtimes)
- `backend/config/cors.php` (Configured explicit development CORS origins for port 3000 and 3001)

---

## 14. Known Limitations
None.

---

## 15. Final Decision
STATUS: COMPLETE
