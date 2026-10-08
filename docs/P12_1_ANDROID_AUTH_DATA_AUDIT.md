# P12.1 Android Authentication, Real Data & Tenant Context Audit

## 1. Executive Summary
This audit document establishes the comprehensive assessment of Authentication, Tenant Context, API Integration, and Mock Data across the Resto Android application, cross-referenced with the Laravel backend source of truth.

---

## 2. Authentication Architecture Audit

### 2.1 Backend Authentication Endpoints & Contracts
| Endpoint | Method | Auth Required | Request Payload | Response Contract |
| :--- | :--- | :--- | :--- | :--- |
| `/api/v1/auth/login` | POST | No (Rate-limited) | `email`, `password`, `device_name` (optional) | `{ access_token, token_type: "Bearer", user: UserDto }` |
| `/api/v1/auth/profile` | GET | Yes (`auth:sanctum`) | None (Header: `Authorization: Bearer <token>`) | `UserDto` with `memberships`, `organizations`, `stores`, `warehouses` |
| `/api/v1/auth/logout` | POST | Yes (`auth:sanctum`) | None (Header: `Authorization: Bearer <token>`) | `{ message: "از حساب کاربری خود خارج شدید." }` |
| `/api/v1/auth/logout-all`| POST | Yes (`auth:sanctum`) | None | `{ message, revoked_sessions }` |
| `/api/v1/auth/forgot-password` | POST | No | `email` | `{ message }` |
| `/api/v1/auth/reset-password` | POST | No | `email`, `token`, `password`, `password_confirmation` | `{ message }` |

### 2.2 Token & Session Mechanism
- **Backend Protocol**: Laravel Sanctum Bearer tokens generated via `$user->createToken(...)`.
- **Token Format**: Plaintext Sanctum personal access token (e.g. `1|abcdef...`).
- **Token Storage on Android**: `SessionStore` backed by AndroidX DataStore (`session.preferences_pb`), storing `access_token` and `language`.
- **Session Persistence**: Checked upon app startup; valid non-empty token restores session. A 401 Unauthorized clears token and navigates back to Login.

---

## 3. Tenant Context Audit

### 3.1 Tenant Resolution & Middlewares
- **Backend Middleware**: `App\Http\Middleware\TenantMiddleware` verifies that:
  1. User is authenticated via Sanctum.
  2. Request header `X-Tenant-ID` matches an existing organization.
  3. Authenticated user has an ACTIVE membership in that organization (rejects with 403 otherwise).
  4. Request header `X-Store-ID` matches an existing store within that organization (rejects with 404 otherwise).
- **Android Missing Links**:
  - `NetworkModule.kt` previously only added `X-Tenant-ID` and `X-Store-ID` from in-memory volatile variables, completely omitting the `Authorization: Bearer <token>` header!
  - `NetworkModule.kt` has no session-aware dynamic token attachment.

---

## 4. Data Flow Audit (Module-by-Module)

| Module | Remote Backend Endpoint | Repository Implementation | Issue / Status |
| :--- | :--- | :--- | :--- |
| **Authentication** | `POST /api/v1/auth/login`<br>`GET /api/v1/auth/profile` | Missing dedicated `AuthRepository` in DI container. | **P0**: App bypassed login screen directly into `MainAppScreen`. |
| **Current User** | `GET /api/v1/auth/profile` | Not fetched at startup; relies on `TenantState` defaults. | **P0**: Hardcoded `userId: "usr_admin_1"`, `userName: "Admin User"`. |
| **Organizations** | `GET /api/v1/organizations` | `TenantRepositoryImpl` only queried Room DB. | **P0**: Loaded fake seeded organizations from `MockSaaSDataSource`. |
| **Stores & Warehouses**| `GET /api/v1/organizations/{id}/stores`<br>`GET /api/v1/stores/{id}/warehouses` | `TenantRepositoryImpl` only queried Room DB. | **P0**: Hardcoded `wh_apex_1a` and seeded fake stores. |
| **Products & Catalog**| `GET /api/v1/products`<br>`POST /api/v1/products`<br>`PUT /api/v1/products/{id}` | `ProductRepositoryImpl` queries local Room DB. Remote API errors swallowed. | **P1**: No remote sync on startup; relies on Room cache seeded with mock data. |
| **Inventory Stock** | `GET /api/v1/inventory/stock`<br>`POST /api/v1/inventory/adjust` | `ProductRepositoryImpl.adjustStock` silently catches exceptions and only updates Room. | **P0**: API error swallowed; inventory calculated locally. |
| **Sales Orders** | `GET /api/v1/orders`<br>`POST /api/v1/orders/checkout` | `SalesOrderRepositoryImpl` has empty try/catch block for remote checkout. | **P0**: Fake success on checkout even when backend fails. |
| **Customers** | `GET /api/v1/customers`<br>`POST /api/v1/customers` | `CustomerRepositoryImpl` only interacts with Room DB. | **P1**: Disconnected from backend API. |
| **Suppliers** | `GET /api/v1/suppliers`<br>`POST /api/v1/suppliers` | `SupplierRepositoryImpl` only interacts with Room DB. | **P1**: Disconnected from backend API. |
| **Purchases** | `GET /api/v1/purchases`<br>`POST /api/v1/purchases`<br>`POST /api/v1/purchases/{id}/receive` | `PurchaseRepositoryImpl` only interacts with Room DB. | **P1**: Disconnected from backend API. |
| **Expenses** | `GET /api/v1/expenses`<br>`POST /api/v1/expenses` | `ExpenseRepositoryImpl` only interacts with Room DB. | **P1**: Disconnected from backend API. |
| **Dashboard** | `GET /api/v1/accounting/summary` | `LedgerRepositoryImpl` only sums local Room ledger entries. | **P1**: Relies on locally generated mock ledger entries. |

---

## 5. Mock Data & Hardcoded Literals Audit

| Pattern / Literal | Location(s) | Classification | Impact & Plan |
| :--- | :--- | :--- | :--- |
| `userId = "usr_admin_1"` | `Models.kt:44` | **Production bug** (Unsafe) | Remove hardcoded default; default to empty string. Require authenticated user. |
| `userName = "Admin User"` | `Models.kt:45` | **Production bug** (Unsafe) | Remove hardcoded default; populate from `/api/v1/auth/profile`. |
| `isLoggedIn = true` | `Models.kt:48` | **Production bug** (Unsafe) | Change default to `false`. Login state must be driven by `SessionStore`. |
| `MockDataArchive.kt` | `data/mock/MockDataArchive.kt` | **Dead/Test data** (Safe if unreferenced) | Keep in test/preview; disconnect from all production repositories. |
| `MockSaaSDataSource.kt` | `data/mock/MockSaaSDataSource.kt` | **Production bug** (Unsafe) | Stop calling `seedInitialData()` on production startup in `CalcuappApplication.kt`. |
| `seedInitialData()` | `CalcuappApplication.kt:24`<br>`TenantRepository.kt:85` | **Production bug** (Unsafe) | Remove automatic mock seeding in `Application.onCreate()`. |
| `taxRate = 0.08` | `CartItem.kt:124`<br>`CartRepository.kt:70` | **Production bug** (Unsafe) | Fetch dynamic `default_tax_rate` from `/api/v1/business/settings` instead of hardcoding 8%. |
| `BASE_URL = "http://10.0.2.2:8000/api/v1/"` | `NetworkModule.kt:14` | **Production bug** (Unsafe) | Support configurable host (10.0.2.2 emulator, physical device IP, localhost, or BuildConfig). |
| `empty catch block` | `SalesOrderRepository.kt:110` | **Production bug** (Unsafe) | Remove empty catch block; properly report network failures and sync errors. |

---

## 6. Implementation Action Plan
1. **AuthRepository & Session Management**:
   - Create `AuthRepository` with `login()`, `getProfile()`, `logout()`, `restoreSession()`.
   - Update `NetworkModule` with `AuthInterceptor` providing `Authorization: Bearer <token>`.
   - Handle 401 Unauthorized via Authenticator/Interceptor to invalidate session.
2. **App Startup & Login Screen**:
   - Create `LoginScreen` and `LoginViewModel` (Email, Password, Validation, Error Handling).
   - Update `MainActivity` to check authentication: If authenticated -> load tenant & MainAppScreen; If unauthenticated -> show LoginScreen.
3. **Tenant Context & Business Settings**:
   - Fetch real Organizations, Stores, Warehouses from backend.
   - Fetch real `default_tax_rate` from `/api/v1/business/settings`.
4. **Mock Removal**:
   - Remove `seedInitialData()` call from `CalcuappApplication.kt`.
   - Fix defaults in `Models.kt` (`TenantState`).
5. **Real Data Repositories**:
   - Connect repositories (`ProductRepository`, `SalesOrderRepository`, `CustomerRepository`, etc.) to backend API with proper error propagation.
6. **Verification & Tests**:
   - Unit tests for Auth, SessionStore, Token Interceptor, and Error Propagation.
