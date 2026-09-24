# Project Plan

Calcuapp Persian RTL Localization Phase - Convert Android, Web, and iOS clients into full Persian (fa) language, RTL layout directionality, Persian currency/number formatting, Persian typography, localized POS, inventory, accounting screens, and generate PERSIAN_LOCALIZATION_REPORT.md.

## Project Brief

# Project Brief: Calcuapp Persian RTL Localization

## Features

1. **RTL Layout Directionality & UI Mirroring**: Native right-to-left (RTL) layout mirroring across all screens, navigation components, adaptive scaffold panes, and interactive UI controls.
2. **Persian String Localization & Language Switcher**: Comprehensive `strings-fa` resource bundle integration with dynamic runtime language selection between Persian and English.
3. **Persian Typography & Currency/Number Formatting**: Typography setup using Persian fonts (such as Vazirmatn) with localized Persian digits (۰-۹) and Tomans / Iranian Rial (IRR) currency formatting.
4. **Persian Solar Hijri (Shamsi) Calendar Presentation**: Shamsi date pickers and formatted date displays across transaction logs, receipts, and financial reports.
5. **RTL Localized POS & Inventory Management**: Fully mirrored Point of Sale (POS) checkout screen with localized product search, cart item listing, and inventory stock status in Persian.

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3) energetic color palette, explicit `LayoutDirection.Rtl` composition support, and edge-to-edge layouting
- **Navigation**: Jetpack Navigation 3 (state-driven)
- **Adaptive Strategy**: Compose Material Adaptive library (`ListDetailPaneScaffold`, `SupportingPaneScaffold` supporting RTL pane transitions)
- **Architecture & State Management**: Kotlin Coroutines, Flow, StateFlow, ViewModel
- **Networking & Data Parsing**: Retrofit, OkHttp, Moshi JSON converter (configured for localized API headers and responses)

## Implementation Steps
**Total Duration:** 55m 58s

### Task_1_AuditAndMonorepoStructure: Audit Android codebase, generate master documentation (PROJECT_SPEC.md, ARCHITECTURE.md, AGENTS.md, DATABASE.md, API.md, SECURITY.md, TESTING.md, README.md), and set up monorepo directory structure (backend, web, android, ios, infrastructure, docs, scripts).
- **Status:** COMPLETED
- **Updates:** Audited Android codebase, generated master documentation files (PROJECT_SPEC.md, ARCHITECTURE.md, AGENTS.md, DATABASE.md, API.md, SECURITY.md, TESTING.md, README.md), created monorepo structure (backend, web, android, ios, infrastructure, docs, scripts, tests), moved Android project cleanly into /android directory, and verified ./gradlew assembleDebug and unit tests inside /android.
- **Acceptance Criteria:**
  - Android codebase audited and preserved
  - Master documentation created
  - Monorepo directory structure created
  - Android project moved into /android folder cleanly
- **Duration:** 4m 24s

### Task_2_LaravelBackendAndDatabase: Build Laravel REST API backend, PostgreSQL database migrations, seeders, and core business modules (Identity, Tenancy, Catalog, Transactional Inventory, Purchasing, Sales, Payments, Double-Entry Accounting, Channels, Notifications, Audit logs).
- **Status:** COMPLETED
- **Updates:** Initialized Laravel 11 REST API backend in backend/ directory. Implemented database migrations for all entities (Users, Orgs, Stores, Warehouses, Products, Variants, StockMovements, Purchases, Orders, JournalEntries, Channels, AuditLogs). Implemented InventoryService with pessimistic concurrency locking, AccountingService enforcing Double-Entry Debit == Credit equality, API Controllers, routes/api.php, and DatabaseSeeder. Executed php artisan test with 7 passing tests.
- **Acceptance Criteria:**
  - Laravel project initialized with PostgreSQL & Redis support
  - Database migrations for all entities
  - Transactional inventory and double-entry accounting engines implemented
  - REST API controllers and auth implemented
- **Duration:** 14m 50s

### Task_3_OpenAPIAndAndroidAPIIntegration: Create OpenAPI 3.0 specification file (docs/api/openapi.yaml) and connect existing Android MVP codebase to the real Laravel REST API via Retrofit/Moshi.
- **Status:** COMPLETED
- **Updates:** Created OpenAPI 3.0 specification file (docs/api/openapi.yaml). Built Moshi JSON DTOs, CalcuappApiService Retrofit interface, and NetworkModule with multi-tenant header interceptor in android/. Updated Android repositories to communicate with REST API and sync with local Room DB cache. Verified build and tests cleanly with 10 unit tests passing.
- **Acceptance Criteria:**
  - OpenAPI spec created
  - Retrofit API service and Moshi DTOs implemented in Android
  - Android repositories updated to communicate with REST API
  - All Android MVP screens function seamlessly with real API
- **Duration:** 2m

### Task_4_WebAndIOSApps: Create React Native Web application and SwiftUI iOS application prototypes consuming the shared Laravel REST API.
- **Status:** COMPLETED
- **Updates:** Created React Native Web application in web/ with package.json, apiClient.ts, and screens for Dashboard, POS/Cart, Inventory, Customers, and Accounting. Created SwiftUI iOS application in ios/ with CalcuappApp.swift, ApiService.swift, ViewModels, and SwiftUI views for POS, Inventory, Sales, Customers, and Dashboard. Verified shared API contract across platforms.
- **Acceptance Criteria:**
  - React Native Web admin application created and connected to REST API
  - SwiftUI iOS app created and connected to REST API
  - Shared domain and API contract verified across platforms
- **Duration:** 3m 59s

### Task_5_DockerCICDAndFullVerification: Configure Docker Compose development environment, CI/CD pipeline, and execute full end-to-end integration test suite.
- **Status:** COMPLETED
- **Updates:** Configured docker-compose.yml (PostgreSQL 16, Redis, Nginx, PHP 8.3), infrastructure/docker/Dockerfile.backend, and nginx.conf. Created GitHub Actions CI/CD workflow (.github/workflows/ci.yml). Created and executed tests/e2e/e2e_business_cycle_test.sh, verifying full end-to-end business cycle (Organization -> Store -> Warehouse -> Product -> Purchase receiving -> POS Sale -> Inventory deduction -> Double-entry Accounting equality Debits == Credits). All tests passed cleanly.
- **Acceptance Criteria:**
  - Docker Compose environment starts PostgreSQL, Redis, and Laravel backend
  - CI workflow configured
  - Full end-to-end business cycle test executed and verified
- **Duration:** 2m 5s

### Task_6_PersianRTLAndLocalizationImplementation: Implement Persian (fa) string resources, RTL layout directionality across Jetpack Compose screens, Persian digits & currency (Toman/Rial) formatting, Shamsi date support, and language switcher in Android, Web, and iOS clients.
- **Status:** COMPLETED
- **Updates:** Implemented values-fa/strings.xml, values/strings.xml, PersianFormatter.kt (Persian digits ۰-۹, Toman currency, Shamsi Jalali date formatting), CompositionLocalProvider RTL layout directionality, dynamic Persian/English language switcher in TenantSwitcherModal, Web dir=rtl and localized UI, and iOS SwiftUI RTL layout direction and Persian labels. Verified assembleDebug and testDebugUnitTest in /android (13 tests passed) and php artisan test in /backend (8 tests passed).
- **Acceptance Criteria:**
  - Persian strings values-fa/strings.xml created and mapped for all screens
  - RTL layout directionality supported in Compose UI
  - Persian number and currency (Toman/Rial) formatting utility implemented
  - Language selection toggle (Persian / English) integrated
  - Web and iOS apps localized into Persian RTL
- **Duration:** 8m 10s

### Task_7_PersianLocalizationVerificationAndReport: Verify complete Persian RTL localization, test application stability without crashes across POS, Inventory, and Accounting screens, and generate PERSIAN_LOCALIZATION_REPORT.md.
- **Status:** COMPLETED
- **Updates:** Verified build pass (./gradlew assembleDebug), unit tests (13/13 unit tests passing in /android, 8/8 feature tests passing in /backend), runtime stability on Pixel Tablet emulator (0 crashes, 0 fatal exceptions), Persian RTL layout mirroring across POS, Inventory, Customer CRM, Sales Orders, and Accounting Dashboard screens, Persian digits (۰-۹), Toman currency formatting, Shamsi date rendering, dynamic language switcher (فارسی / English), and generated PERSIAN_LOCALIZATION_REPORT.md at repository root.
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - PERSIAN_LOCALIZATION_REPORT.md documentation created
  - Persian RTL layout and formatting verified across all application screens
- **Duration:** 20m 30s

