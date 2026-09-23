# Project Plan

Calcuapp - Multi-tenant SaaS Business Management Android Application with POS, Inventory, Sales, Accounting, and Organization management.

## Project Brief

# Project Brief: Calcuapp Android MVP

## Features

1. **Multi-Tenant & Store Switcher**: Secure multi-tenant authentication with real-time switching between active organizations, stores, and warehouses.
2. **Mobile Point of Sale (POS) & Quick Checkout**: Touch-friendly checkout interface supporting product variant selection, barcode scanning via camera, cart management, and payment handling.
3. **Inventory & Product Catalog Lookup**: Real-time ledger-based stock level checks, variant management, and on-the-fly inventory adjustments across warehouses.
4. **Sales & Accounting Overview**: Dashboard displaying real-time business metrics, daily revenue totals, ledger summaries, and basic reporting indicators.
5. **Customer & Sales Order Management**: Customer profile lookup, sales order history, transaction receipts, and fulfillment status updates.

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3) energetic color palette and full edge-to-edge layout support
- **Navigation Strategy**: Jetpack Navigation 3 (state-driven)
- **Adaptive Strategy**: Compose Material Adaptive library (`ListDetailPaneScaffold`, `SupportingPaneScaffold` for seamless smartphone, tablet, and foldable responsive layouts)
- **Concurrency & Architecture**: Kotlin Coroutines, Flow, ViewModel, Clean Architecture / MVVM state flow patterns
- **Networking**: Retrofit, OkHttp, Moshi (JSON parsing) for SaaS REST API synchronization

## Implementation Steps
**Total Duration:** 23m 3s

### Task_1_DataLayerAndTenantManagement: Implement domain data models, Room database, DataStore for active tenant/store selection state, and repositories for Organization, Store, Product catalog, Cart, Sales Orders, and Accounting ledgers.
- **Status:** COMPLETED
- **Updates:** Implemented data models (Organization, Store, Warehouse, Product, Variant, CartItem, Customer, SalesOrder, LedgerEntry), TenantPreferences DataStore, Room Database with entities and DAOs, MockSaaSDataSource, repositories for tenant, products, cart, orders, customers, and accounting ledger, and AppContainer. Build and tests passed successfully.
- **Acceptance Criteria:**
  - Data models defined for Tenant/Org, Store, Product, Cart, SalesOrder, Customer, Ledger
  - DataStore for managing active tenant and store selection
  - Room database and DAOs configured for local stock & offline ledger caching
  - Repository layer implemented with mock SaaS services and local storage
  - Project builds successfully
- **Duration:** 16m 24s

### Task_2_POSAndInventoryUI: Build the Mobile POS interface and Inventory management UI using Jetpack Compose M3. Implement product search, variant selector, cart updates, checkout/payment workflow, CameraX barcode scanner, and inventory stock adjustment screens.
- **Status:** COMPLETED
- **Updates:** Implemented PosViewModel, PosScreen with variant selection, cart management, checkout modal, receipt view, CameraX BarcodeScannerDialog with emulator fallback, InventoryViewModel, InventoryScreen, StockAdjustmentDialog with stock transfer/adjustment handling, and Material 3 adaptive multi-pane layouts. Verified build and tests.
- **Acceptance Criteria:**
  - Touch-friendly POS screen with cart management and checkout modal
  - CameraX barcode scanner integration for quick checkout
  - Inventory lookup and warehouse stock adjustment UI
  - Adaptive layouts for phone and tablet using Material 3 Adaptive library
- **Duration:** 3m 45s

### Task_3_SalesAccountingAndCustomerUI: Build the Sales & Accounting Overview Dashboard, Customer Management, and Sales Order History UI using Jetpack Compose and Material 3.
- **Status:** COMPLETED
- **Updates:** Implemented DashboardScreen with revenue metrics and general ledger activity, CustomerScreen for lookup and profile management, SalesOrdersScreen for order history, fulfillment status, and receipt inspection, TenantSwitcherModal for organization/store switching, and MainAppScreen with M3 NavigationBar. Verified with assembleDebug and unit tests.
- **Acceptance Criteria:**
  - Sales & Accounting Dashboard displaying revenue metrics, ledger summaries, and sales indicators
  - Customer lookup and profile management UI
  - Sales Order history with detailed receipt and fulfillment status view
  - Clean state management and ViewModel integration
- **Duration:** 2m 54s

### Task_4_AppIconThemeAndNavigationIntegration: Configure vibrant Material 3 energetic light/dark color themes, edge-to-edge layout, adaptive app icon, and integrate state-driven Jetpack Navigation 3 with top-level tenant and store switcher.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Adaptive app icon matching Calcuapp SaaS function
  - Vibrant Material 3 energetic color theme with light and dark mode support
  - Full Edge-to-Edge display support
  - Real-time Organization & Store switcher accessible in top bar or navigation drawer
  - Project builds cleanly
- **StartTime:** 2026-09-23 20:58:05 IRST

### Task_5_RunAndVerify: Run and verify the complete MVP application. Instruct critic agent to verify application stability, ensure no crashes occur during POS checkout or tenant switching, and verify all acceptance criteria are met.
- **Status:** PENDING
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - Verified multi-tenant switching, POS checkout flow, inventory updates, and sales reporting stability

