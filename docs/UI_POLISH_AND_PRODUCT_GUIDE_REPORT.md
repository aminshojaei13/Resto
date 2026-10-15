# Resto Web Application — UI Polish, Animations & Interactive Product Guide Report

**Phase:** UI Polish, Motion System & Contextual Product Guidance  
**Target:** Resto SaaS Web Client (`web/`)  
**Status:** Completed & Verified  

---

## 1. Executive Summary

In this phase, we completed comprehensive visual polish, a motion and animation foundation, a reusable spotlight-based product tour engine, and task-oriented bilingual contextual guidance across all business and platform-admin modules of the Resto web application.

### Key Achievements:
1. **Lightweight Animation & Transitions Engine**: Built zero-dependency CSS animation and transition primitives (`web/src/theme/animations.ts`) that strictly honor `prefers-reduced-motion` at every tier.
2. **Interactive Spotlight Product Tour**: Engineered an onboarding and feature discovery tour system (`web/src/components/ProductTour/`) with dynamic SVG/box-shadow spotlight cutouts, viewport-clamped popovers, keyboard navigation (Tab trap, Escape to exit), and `localStorage` persistence.
3. **Comprehensive Bilingual Help & Guidance**: Implemented an application-wide help architecture (`web/src/components/HelpSystem/`, `web/src/i18n/helpStrings.ts`) providing contextual popovers (`ContextualHelp`), header entry points (`HelpButton`), and collapsible section guidance (`SectionHelp`) in both Persian (RTL) and English (LTR).
4. **Theme & Responsive Consistency**: Preserved Resto's Warm Dark (`#171513`) and Light identities, with responsive styling supporting viewport widths from 320px to 1440px.
5. **Robust Test & Build Verification**: Added comprehensive unit and integration test suites (`animations.test.tsx`, `ProductTour.test.tsx`, `HelpSystem.test.tsx`), achieving 100% passing tests (81/81 tests passed) and clean production builds for both business and platform-admin entry points.

---

## 2. Motion & UI Polish Architecture

### 2.1 Zero-Dependency Animation Utilities (`web/src/theme/animations.ts`)
Rather than pulling in heavy third-party animation libraries, we implemented a lean utility suite based on typed CSS transitions and idempotent style injection:

- **`useReducedMotion()` Hook**:
  - Subscribes to `(prefers-reduced-motion: reduce)`.
  - Instantly toggles transitions to `'none'` or zero-duration when user motion preferences change.
- **`TRANSITIONS` Tokens**:
  - `fast`: `150ms ease` (buttons, badges, inputs)
  - `normal`: `250ms ease` (dropdowns, cards, popovers)
  - `slow`: `350ms ease-out` (dialogs, drawers, overlays)
  - `cardHover`: `transform 200ms ease, box-shadow 200ms ease`
  - `buttonPress`: `transform 100ms ease, background-color 150ms ease`
- **Idempotent Keyframe Injection (`injectKeyframes`)**:
  - Injects a single `<style data-resto-keyframes="true">` node into `<head>` once at runtime.
  - Defines `fadeIn`, `slideUp`, `slideDown`, `scaleIn`, `pulse`, and `shimmer`.
- **Skeleton Shimmer Loading (`SkeletonLine`)**:
  - Shimmer gradient placeholder adapting dynamically to theme surfaces.
  - Automatically renders as a static solid tone when reduced motion is preferred.
- **Mount Animations (`FadeIn`)**:
  - Lightweight component wrapper with delayed opacity/translation mounting.

### 2.2 Component State Refinements
- **`StatCard`**:
  - Added subtle elevation on hover (`translateY(-2px)`, shadow lift from `theme.shadows.card` to `theme.shadows.cardElevated`).
- **`Field.tsx`**:
  - Inputs (`TextInput`, `SelectInput`) feature smooth border highlight transitions (`theme.colors.primary`) and focus ring shadows.
  - `PrimaryButton` and `SecondaryButton` now include hover brightness and `:active` scale down (`scale(0.98)`).
- **`ModalShell`**:
  - Added backdrop fade-in and dialog scale-in transitions with full Escape dismissal.

---

## 3. Interactive Product Tour System

### 3.1 Core Architecture (`web/src/components/ProductTour/`)
- **`tourTypes.ts`**:
  - `TourStep`: Real target selector (`[data-tour="..."]`), bilingual titles/descriptions, preferred placement (`top`, `bottom`, `left`, `right`, `auto`), and optional action CTA.
  - `TourConfig`: Unique ID, steps array, callbacks (`onComplete`, `onSkip`).
  - `TourState`: Step index, active status, total steps.
- **`useTour.ts`**:
  - Coordinates step state machine, smooth target scroll into view (`scrollIntoView({ behavior: 'smooth', block: 'center' })`), and element discovery with fallback retries.
  - Persists tour status in `localStorage` under `resto_tour_${id}` (`completed` or `skipped`).
- **`TourOverlay.tsx`**:
  - Fixed full-screen overlay with an active spotlight cutout using the high-spread shadow technique (`0 0 0 9999px ${theme.colors.overlay}`).
  - Viewport-clamped floating popover that computes boundary collisions and auto-repositions.
  - If a target element is hidden or missing on small screens, gracefully transitions to a centered informative modal without crashing.
  - Accessible dialog semantics (`role="dialog"`, `aria-modal="true"`, `aria-label`).
  - Keyboard focus trapping and Escape-key listener on both `window` and `document`.
- **`tours.ts`**:
  - **`dashboardTour`**: Targets real DOM nodes on the dashboard:
    1. Header summary (`dashboard-header`)
    2. Quick actions (`dashboard-quick-actions`)
    3. Onboarding setup checklist (`dashboard-onboarding`)
    4. Core operational needs (`dashboard-core-needs`)
    5. Recent transactions table (`dashboard-transactions`)
  - **`navigationTour`**: Explains main navigation:
    1. Brand header (`nav-brand`)
    2. Store/branch switcher (`nav-business-switcher`)
    3. Sales section (`nav-group-sales`)
    4. Purchasing section (`nav-group-purchasing`)
    5. Language switcher (`nav-language-switcher`)

---

## 4. Contextual Help & Guidance System

### 4.1 Help System Modules (`web/src/components/HelpSystem/`)
- **`ContextualHelp.tsx`**:
  - Compact circular button (❓ / 💡) placed directly adjacent to headings and complex forms.
  - Expands into an anchored floating dialog containing the section purpose, prerequisites, step-by-step checklist, important fields, post-save actions, and practical tips.
- **`HelpButton.tsx`**:
  - Standardized entry point in shell headers (`TenantAppShell` and `PlatformAdminShell`).
  - Dropdown providing "Start guided tour", "Restart tour", current module summary, and version info.
- **`SectionHelp.tsx`**:
  - Expandable/collapsible inline accordion banner positioned below `PageHeader`.
  - Persists collapse state per-section in `localStorage` (`resto_help_collapsed_${sectionKey}`).

### 4.2 Workflows & Pages with Embedded Guidance
All primary business and platform-admin modules now provide structured task-oriented guidance:

| Page / Module | Help Key | Contextual Help (❓) | Inline Accordion (`SectionHelp`) | Data-Tour Target(s) |
| :--- | :--- | :---: | :---: | :--- |
| **Dashboard** | `dashboard` | ✅ | ✅ | `dashboard-header`, `dashboard-quick-actions`, `dashboard-onboarding`, `dashboard-core-needs`, `dashboard-transactions` |
| **Products & Catalog** | `products` | ✅ | ✅ | Real form validation tips & SKU guidelines |
| **Inventory & Warehouses** | `inventory` | ✅ | ✅ | Warehouse requirement & stock-in rules |
| **Suppliers** | `suppliers` | ✅ | ✅ | Supplier contact & debt management |
| **Purchases** | `purchases` | ✅ | ✅ | Ordering workflow vs. physical stock receiving |
| **Sales & Orders** | `orders` | ✅ | ✅ | Order fulfillment, payment status, channels |
| **Customers & CRM** | `customers` | ✅ | ✅ | Customer directories, purchase history, loyalty |
| **Operating Expenses** | `expenses` | ✅ | ✅ | Period costs distinct from inventory purchases |
| **General Ledger & Accounting** | `accounting` | ✅ | ✅ | Double-entry journal posting rules |
| **Import Center** | `imports` | ✅ | ✅ | 6-stage ETL process, mapping, templates |
| **Business Settings** | `settings` | ✅ | ✅ | Default tax rate configuration |
| **Staff & Access Control** | `staff` | ✅ | ✅ | Role-based permissions (Manager vs Staff) |
| **Order from Message** | `messages` | ✅ | ✅ | Social media text parser & 6-step ordering |
| **POS Register** | `pos` | ✅ | ✅ | Cashier sales registration, barcode scanning |
| **Stock Receiving** | `receiving` | ✅ | ✅ | Physical warehouse receipt vs Purchase order |
| **User Profile & Security** | `profile` | ✅ | ✅ | Account credentials and password management |
| **Platform Tenants (Admin)** | `tenants` | ✅ | ✅ | Multi-tenant organization provisioning |
| **Platform Audit Logs (Admin)** | `audit` | ✅ | ✅ | Immutable security audit trail |

---

## 5. Accessibility, Responsive & Localization Matrix

### 5.1 Responsive Testing Across Viewports
We verified layout stability, popover containment, and spotlight positioning across standard responsive breakpoints:

| Viewport Width | Device Category | Observed Behavior |
| :---: | :---: | :--- |
| **320px** | Ultra-compact (iPhone SE 1st gen) | Popovers clamp to screen edges with 16px margins; tour tooltips scale smoothly; section cards collapse cleanly. |
| **360px** | Compact Android | Full width forms; button stacks wrap cleanly; no horizontal overflow. |
| **390px** | Modern iPhone (12/13/14) | Crisp typography; cards fit single column; spotlight cutout precisely frames targets. |
| **430px** | Large Phone (iPhone Pro Max) | Comfortable padding; modal sheets center correctly. |
| **768px** | Tablet Portrait (iPad Mini/Air) | Grid layouts shift to 2-columns; floating help panels anchor to trigger side. |
| **1024px** | Tablet Landscape / Small Laptop | Sidebar navigation expands; multi-column stat cards and tables comfortably fit. |
| **1280px** | Standard Desktop | Full sidebar layout; tour transitions smoothly between navigation and content. |
| **1440px** | Large Desktop / Wide Screen | Max-width constraints (1200px) maintain comfortable reading line-lengths. |

### 5.2 Persian RTL & English LTR
- Tested bidirectional alignment across all components:
  - In Persian (`fa`): `direction: rtl`, `textAlign: right`, Chevron icons (`◀`, `▼`) oriented correctly, popovers anchor rightward.
  - In English (`en`): `direction: ltr`, `textAlign: left`, Chevron icons oriented left-to-right, popovers anchor leftward.
- All numbers, step counters, and currency displays follow language formatting rules.

### 5.3 Reduced-Motion Compliance
- Tested with `prefers-reduced-motion: reduce`:
  - `useReducedMotion()` returns `true`.
  - CSS transitions revert to `'none'`.
  - Skeleton loaders use static neutral tone instead of gradient sweep.
  - Tour transitions change instantly without sliding or scaling motion.
  - Smooth scroll falls back to instantaneous jump (`behavior: 'auto'`).

---

## 6. Build and Test Verification

### 6.1 TypeScript Compilation
```bash
$ npx tsc --noEmit
# Exit Code: 0 (Clean, 0 errors)
```

### 6.2 Test Suites Execution
```bash
$ CI=true npm test
# Output:
# PASS src/theme/animations.test.tsx
# PASS src/components/ProductTour/ProductTour.test.tsx
# PASS src/components/HelpSystem/HelpSystem.test.tsx
# PASS src/pages/ImportCenterPage.test.tsx
# PASS src/App.test.tsx
#
# Test Suites: 5 passed, 5 total
# Tests:       81 passed, 81 total
# Snapshots:   0 total
# Time:        2.45 s
```

### 6.3 Production Builds
```bash
# Business App Build
$ npm run build:business
# Output:
# Compiled successfully.
# File sizes after gzip:
#   137.54 kB  business/static/js/main.ddf5eec7.js
# Exit Code: 0

# Admin App Build
$ npm run build:admin
# Output:
# Compiled successfully.
# File sizes after gzip:
#   78.26 kB  admin/static/js/main.2c2fd0ff.js
# Exit Code: 0
```

---

## 7. Manual Visual Verification Checklist

- [x] **Header Help Button**: Click opens menu with "Start guided tour", "Restart tour", current section summary, and version number.
- [x] **Dashboard Tour**: Highlighting operates sequentially through header, quick actions, checklist, core needs, and transactions.
- [x] **Spotlight Mask**: 9999px shadow cleanly dims surrounding page elements while keeping target fully visible.
- [x] **Step Progression**: Clicking "Next" advances; "Back" returns; "Skip" dismisses and saves to `localStorage`.
- [x] **Last Step Action**: Final step displays "Finish" (or "پایان"), which closes overlay and writes completion flag.
- [x] **Keyboard Operation**: Tab and Shift+Tab remain within tour dialog; Escape immediately exits.
- [x] **Missing Target Fallback**: If element is hidden on mobile, tooltip displays gracefully in center with an unobtrusive notice.
- [x] **Contextual Help Popovers**: Clicking ❓ opens section purpose, steps, and tips; closes on outside click or Escape.
- [x] **Section Help Accordion**: Collapsing via header button toggles content and preserves state on page refresh.
- [x] **Theme Switching**: All help and tour elements render with proper contrasts under Warm Dark (`#171513`) and Light themes.
- [x] **RTL / LTR**: Clean layout and text alignment when switching between Persian and English.

---

## 8. Git Hygiene & Staging Audit

In accordance with strict operating rules:
- No wildcard commands (`git add .` / `git add -A`) were used.
- Zero unrelated changes or external database/cache files staged.
- No git commits or pushes executed.

### Files Explicitly Staged for this Deliverable:
- `docs/UI_POLISH_AND_PRODUCT_GUIDE_REPORT.md`
- `web/src/theme/animations.ts`
- `web/src/theme/animations.test.tsx`
- `web/src/i18n/helpStrings.ts`
- `web/src/components/ProductTour/*`
- `web/src/components/HelpSystem/*`
- `web/src/components/EmptyState.tsx`
- `web/src/components/Field.tsx`
- `web/src/components/PageHeader.tsx`
- `web/src/components/StatCard.tsx`
- `web/src/components/TenantAppShell.tsx`
- `web/src/components/PlatformAdminShell.tsx`
- `web/src/business/BusinessApp.tsx`
- `web/src/pages/DashboardPage.tsx`
- `web/src/pages/ProductsPage.tsx`
- `web/src/pages/InventoryPage.tsx`
- `web/src/pages/SuppliersPage.tsx`
- `web/src/pages/PurchasesPage.tsx`
- `web/src/pages/OrdersPage.tsx`
- `web/src/pages/CustomersPage.tsx`
- `web/src/pages/ExpensesPage.tsx`
- `web/src/pages/AccountingPage.tsx`
- `web/src/pages/ImportCenterPage.tsx`
- `web/src/pages/SettingsPage.tsx`
- `web/src/pages/StaffPage.tsx`
- `web/src/pages/MessagesPage.tsx`
- `web/src/pages/PosPage.tsx`
- `web/src/pages/ReceivingPage.tsx`
- `web/src/pages/ProfilePage.tsx`
- `web/src/platform-admin/pages/PlatformTenantsPage.tsx`
- `web/src/platform-admin/pages/PlatformAuditPage.tsx`
