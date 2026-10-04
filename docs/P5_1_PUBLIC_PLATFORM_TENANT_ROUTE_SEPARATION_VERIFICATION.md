# P5.1 Public / Platform Admin / Tenant Route Separation Verification

## 1. Objective
The objective of this task (P5.1) is to perform a strict UI, routing, and authorization separation correction for the Resto SaaS platform. Previously, public organization registration, Platform Admin application management, and normal tenant business dashboard operations were displayed together inside a single application shell experience.

This implementation enforces explicit separation across three distinct application experiences, introduces dedicated shells (`PublicShell`, `PlatformAdminShell`, `TenantAppShell`), implements route guards with automatic role-based redirects, and verifies the existing transactional tenant provisioning lifecycle without modifying backend domain models or architecture.

---

## 2. Existing Architecture Audited
The underlying backend architecture was thoroughly audited prior to frontend refactoring:
- **Backend Model & Provisioning**:
  - `BusinessApplication`: Stores incoming registration requests (`PENDING`, `APPROVED`, `REJECTED`).
  - `Organization`: Represents tenant isolation boundaries (`code`, `currency_code`, `subscription_tier`, `onboarding_status`).
  - `OrganizationMembership`: Maps users to organizations (`OWNER`, `MANAGER`).
  - `User`: Handles authentication (`password`, `is_platform_admin` boolean flag).
  - Platform Admin approval (`POST /api/v1/platform/business-applications/{id}/approve`) transactionally creates the tenant `Organization`, owner `User`, `OrganizationMembership`, default `Store`, and default `Warehouse`.
- **Backend Security Boundaries**:
  - `TenantMiddleware` and `is_platform_admin` policy checks remain strictly enforced on all API endpoints.
  - No fake frontend credentials or second provisioning mechanisms were introduced.

---

## 3. Final Route Structure

### Public Routes
- `/register`: SaaS Business Registration Page (Organization application submission only)
- `/login`: Tenant User Login Page
- `/platform/login`: Dedicated Platform Administrator Login Page
- `/activation`: Organization Activation & Access Information Guide Page

### Platform Admin Routes
- `/platform/applications`: Platform Admin Business Applications Console (Review, Approve, Reject)
- `/platform/applications/:id`: Platform Admin Detailed Application Review View

### Tenant Application Routes
- `/app/dashboard`: Main Executive Business Dashboard
- `/app/pos`: Point of Sale & Terminal Checkout
- `/app/orders`: Sales Orders & Fulfillment Management
- `/app/products`: Product Catalog & Variant Management
- `/app/inventory`: Warehouse Inventory & Stock Adjustments
- `/app/purchases`: Purchasing & Supplier Purchase Orders
- `/app/suppliers`: Supplier CRM & Directory
- `/app/customers`: Customer CRM & Loyalty Tracking
- `/app/expenses`: Expense Tracking & Categorization
- `/app/messages`: Social Order Message Import Parser
- `/app/accounting`: Double-Entry General Ledger
- `/app/reports`: Financial Reports & Profit & Loss
- `/app/settings`: Business Settings & Store Configuration

### Root Route (`/`) Resolution
- **Anonymous / Unauthenticated**: `/` → `/register`
- **Authenticated Tenant User**: `/` → `/app/dashboard`
- **Authenticated Platform Admin**: `/` → `/platform/applications`

---

## 4. Public Registration Flow (FLOW A)
- **URL**: `/register`
- **Shell**: `PublicShell` (Public SaaS header with Resto branding, language switcher `fa`/`en`, and public links; **NO** tenant sidebar, **NO** admin sidebar, **NO** tenant dashboard elements).
- **User Action**: Business owner submits business name, owner full name, work email, phone, business category, city, address, and notes.
- **Outcome**: On submission, API creates a `BusinessApplication` with status `PENDING`.
- **UI State**: The page transitions to a clear pending/submitted state displaying the tracking application ID and owner details. It does **NOT** enter the tenant business dashboard or render tenant navigation.

---

## 5. Platform Admin Flow (FLOW B)
- **URL**: `/platform/applications` & `/platform/applications/:id`
- **Shell**: `PlatformAdminShell` (Dark slate admin control panel sidebar with cyan admin badge, super-admin top bar with admin user details and logout button; **NO** normal tenant business navigation like products, suppliers, inventory, POS, accounting, or reports).
- **Authorization**: Access is strictly restricted to users with `isPlatformAdmin === true` (`is_platform_admin` on backend).
- **Features**:
  - Filter applications by `PENDING`, `APPROVED`, `REJECTED`.
  - Transactionally approve pending applications (`POST /api/v1/platform/business-applications/{id}/approve`), auto-provisioning tenant organization, store, warehouse, and owner account.
  - Reject pending applications with mandatory rejection reason (`POST /api/v1/platform/business-applications/{id}/reject`).

---

## 6. Tenant Application Flow (FLOW C)
- **URL**: `/app/*` (e.g. `/app/dashboard`, `/app/pos`, `/app/inventory`, etc.)
- **Shell**: `TenantAppShell` (Dark charcoal sidebar, active tenant organization & store switcher modal trigger, active workspace indicator, business operation links, user profile badge, and logout button).
- **Navigation Scope**: Navigation items are strictly limited to business operations (Dashboard, POS, Sales Orders, Product Catalog, Inventory, Purchasing, Suppliers, Customers, Expenses, Messages, Accounting, Reports, Settings).
- **Isolation**: Public registration (`public_register`) and Platform Admin (`platform_admin`) links are removed from the tenant sidebar.

---

## 7. Authentication / Activation Flow
- **Dedicated Login Pages**: `/login` (Tenant users) and `/platform/login` (Platform Admins).
- **Dedicated Activation Page**: `/activation`
  - **Explicit Status Declaration**: `ACTIVATION_CODE_STATUS: NOT_IMPLEMENTED`
  - Explains that explicit activation/invitation codes are not required in Resto SaaS.
  - Explains that upon Platform Admin approval, tenant credentials (owner email & initial password) are created automatically, directing approved owners to `/login`.

---

## 8. Route Guard Matrix

| User Type | Targeted Route | Guard Decision | Resulting Action / Rendered View |
| :--- | :--- | :--- | :--- |
| Anonymous | `/register` | Allow | Renders `PublicRegisterPage` in `PublicShell` |
| Anonymous | `/login` | Allow | Renders `LoginPage` in `PublicShell` |
| Anonymous | `/activation` | Allow | Renders `ActivationPage` in `PublicShell` |
| Anonymous | `/platform/login` | Allow | Renders `PlatformLoginPage` in `PublicShell` |
| Anonymous | `/app/dashboard` | Redirect | Redirects to `/login` |
| Anonymous | `/platform/applications` | Redirect | Redirects to `/platform/login` |
| Anonymous | `/` | Redirect | Redirects to `/register` |
| Tenant User | `/app/dashboard` | Allow | Renders `DashboardPage` in `TenantAppShell` |
| Tenant User | `/app/pos` | Allow | Renders `PosPage` in `TenantAppShell` |
| Tenant User | `/platform/applications` | Deny | Redirects to `/app/dashboard` with Access Denied banner |
| Tenant User | `/` | Redirect | Redirects to `/app/dashboard` |
| Platform Admin | `/platform/applications` | Allow | Renders `PlatformAdminPage` in `PlatformAdminShell` |
| Platform Admin | `/app/dashboard` | Redirect | Redirects to `/platform/applications` with Admin Notice |
| Platform Admin | `/` | Redirect | Redirects to `/platform/applications` |

---

## 9. Manual UI Verification

Manual UI verification was performed across all three separated experiences:

1. **Screen A: Public Registration (`/register`)**
   - **Observed**: Renders clean SaaS public header and organization registration form only.
   - **Verified**: **NO** business dashboard visible, **NO** Platform Admin menu, **NO** tenant sidebar.

2. **Screen B: Platform Admin Console (`/platform/applications`)**
   - **Observed**: Renders dark slate Platform Admin Shell with application review table and status filter tabs (PENDING/APPROVED/REJECTED).
   - **Verified**: **NO** tenant business sidebar items (no products, suppliers, purchases, inventory, POS, accounting, or reports).

3. **Screen C: Tenant Business Application (`/app/dashboard`)**
   - **Observed**: Renders full Resto business dashboard with dark charcoal sidebar, active workspace badge, and tenant switcher trigger.
   - **Verified**: **NO** public registration form, **NO** Platform Admin menu inside sidebar.

---

## 10. Automated Tests

### Web Frontend Automated Tests
All 11 automated test suites in `web/src/App.test.tsx` executed and passed successfully using `npm test`:

```
PASS src/App.test.tsx
  Resto SaaS UI & Route Separation Verification
    ✓ 1. Public registration is accessible without authentication (35 ms)
    ✓ 2 & 3. Public registration does NOT render tenant sidebar or tenant dashboard (12 ms)
    ✓ 4. Public registration successfully creates a pending application using existing API (49 ms)
    ✓ 5. Unauthenticated user cannot access tenant dashboard and is redirected to login (6 ms)
    ✓ 6. Unauthenticated user cannot access Platform Admin pages and is redirected to platform login (6 ms)
    ✓ 7. Normal tenant user can access tenant dashboard (36 ms)
    ✓ 8. Normal tenant user cannot access Platform Admin (18 ms)
    ✓ 9 & 10. Platform Admin can access Platform Admin pages and view applications (6 ms)
    ✓ 11. Activation info page displays NOT_IMPLEMENTED status (6 ms)
    ✓ 12 & 13. Root route behaves correctly for unauthenticated user (6 ms)
    ✓ 14. Tenant business routes work for authenticated tenant user (9 ms)

Test Suites: 1 passed, 1 total
Tests:       11 passed, 11 total
Time:        1.001 s
```

### TypeScript Compilation & Web Production Build Verification
- **TypeScript Check**: `npx tsc --noEmit` executed with **0 errors**.
- **Production Build**: `npm run build` compiled successfully without warnings.

### Backend PHPUnit Automated Tests
All 23 backend PHPUnit tests executed and passed 100%:

```
PHPUnit 11.5.56 by Sebastian Bergmann and contributors.
Runtime: PHP 8.5.10

.......................                                           23 / 23 (100%)
OK (23 tests, 90 assertions)
```

---

## 11. Activation Code Status
ACTIVATION_CODE_STATUS: NOT_IMPLEMENTED

**Audit Explanation**: Inspection of backend models, controllers (`BusinessApplicationController`, `PlatformApplicationController`, `AuthController`), database migrations, and API routes confirmed that an activation-code or invitation-code mechanism is **NOT_IMPLEMENTED** in the backend API.
Resto SaaS authenticates organization owners directly using email and password credentials generated upon Platform Admin application approval. No fake frontend codes or insecure frontend credentials were created.

---

## 12. Files Changed

### Created Files
- `web/src/components/PublicShell.tsx`
- `web/src/components/PlatformAdminShell.tsx`
- `web/src/components/TenantAppShell.tsx`
- `web/src/pages/LoginPage.tsx`
- `web/src/pages/PlatformLoginPage.tsx`
- `web/src/pages/ActivationPage.tsx`
- `web/src/App.test.tsx`
- `docs/P5_1_PUBLIC_PLATFORM_TENANT_ROUTE_SEPARATION_VERIFICATION.md`

### Modified Files
- `web/src/App.tsx` (Refactored routing, experience separation, route guards, and shell resolution)
- `web/src/types/index.ts` (Added `AuthUser` interface)
- `web/src/pages/PublicRegisterPage.tsx` (Updated props, added navigation callbacks)
- `web/src/pages/PlatformAdminPage.tsx` (Updated props, safe error handling for app list loading)
- `web/src/pages/DashboardPage.tsx` (Safe promise handling for accounting summary/journal calls)
- `web/src/pages/PosPage.tsx` (Safe array handling for product list loading)
- `web/package.json` (Added `"test": "react-scripts test --watchAll=false"`, `@testing-library/react`, `@testing-library/dom`, `@testing-library/jest-dom`, `@types/jest`)

---

## 13. Known Limitations
None. All routing boundaries, shell separations, and backend authorizations operate deterministically.

---

## 14. Final Decision
STATUS: COMPLETE
