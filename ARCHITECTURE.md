# ARCHITECTURE.md - Calcuapp Platform System Architecture

## 1. System Overview

```text
+-----------------------+      +-----------------------+      +-----------------------+
|  Android Client (M3)  |      |   iOS Client (Swift)  |      |  Web Portal (React)   |
+-----------+-----------+      +-----------+-----------+      +-----------+-----------+
            |                              |                              |
            +------------------------------+------------------------------+
                                           |
                                           v  HTTPS / REST API / OpenAPI
                               +-----------------------+
                               |  Laravel 11 REST API  |
                               +-----------+-----------+
                                           |
                   +-----------------------+-----------------------+
                   |                                               |
                   v                                               v
        +---------------------+                         +---------------------+
        |  PostgreSQL 16 DB   |                         |     Redis Cache     |
        +---------------------+                         +---------------------+
```

## 2. Android Client Architecture

The Android application follows **Clean Architecture & MVVM Pattern**:

```text
[ UI Layer: Compose Screens & Components ]
                  │
                  ▼
[ ViewModel Layer: StateFlow & Coroutines ]
                  │
                  ▼
[ Repository Layer: Interface & Impl ]
         ┌────────┴────────┐
         ▼                 ▼
[ Local Room DB & DataStore ]  [ Remote REST API / Mock SaaS ]
```

### Core Layers:
- **`data/model`**: Pure Kotlin domain data models (`Organization`, `Store`, `Product`, `CartItem`, `SalesOrder`, `LedgerEntry`, `Customer`).
- **`data/local/db`**: Room DB `AppDatabase`, Entities, and DAOs (`TenantDao`, `ProductDao`, `CustomerDao`, `SalesOrderDao`, `LedgerDao`, `CartDao`).
- **`data/local/datastore`**: `TenantPreferences` managing active tenant session state.
- **`data/repository`**: Repositories encapsulating offline caching and remote synchronization logic.
- **`ui/`**: Jetpack Compose screens for POS, Inventory, Dashboard, Orders, and Customers with Material 3 Expressive styling.
- **`di/`**: `AppContainer` providing dependency injection.
