# Project Plan

Calcuapp SaaS Platform Monorepo - Audit existing Android codebase, build Monorepo structure, Laravel REST API backend, PostgreSQL database migrations, transactional inventory engine, double-entry accounting engine, OpenAPI specification, React Native Web, SwiftUI iOS app, Docker dev environment, CI/CD, and connect Android client to real backend APIs.

## Project Brief

# Project Brief: Calcuapp Android Application

## Features

1. **Multi-Tenant Authentication & Store Switcher**: Secure tenant login connecting to the Laravel REST API with context switching across organizations, stores, and warehouses.
2. **Mobile Point of Sale (POS) & Quick Checkout**: Touch-first sales order entry with barcode scanning support, dynamic cart calculations, multi-payment processing, and receipt generation.
3. **Real-Time Inventory & Catalog Management**: Live product catalog browsing with variant tracking, stock lookup, and ledger-backed inventory adjustment requests.
4. **Double-Entry Financial & Sales Dashboard**: Visual reporting dashboard tracking daily sales totals, revenue trends, and key double-entry ledger summaries.
5. **Customer & Sales Order Management**: Access customer profiles, view past order history, check order fulfillment statuses, and review real-time activity logs.

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3) energetic color palette and edge-to-edge layout execution
- **Navigation**: Jetpack Navigation 3 (state-driven)
- **Adaptive Layouts**: Compose Material Adaptive library (`ListDetailPaneScaffold`, `SupportingPaneScaffold` for phone, foldables, and tablet form factors)
- **Architecture & Asynchronous Engine**: Kotlin Coroutines, Flow, ViewModel, Clean Architecture / MVVM pattern
- **Networking**: Retrofit, OkHttp, Moshi JSON serializer for REST API communication with OpenAPI specs

## Implementation Steps
**Total Duration:** 27m 18s

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

