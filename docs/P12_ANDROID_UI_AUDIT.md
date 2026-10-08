# P12 Android UI Audit

This document outlines the comprehensive UI, UX, and architectural audit findings for the Resto Android application based on the recent codebase audit.

## Architecture

*   **P0: Missing Authentication**
    *   *Description*: No authentication/login screen exists. The app goes directly to `MainAppScreen` with a hardcoded `TenantState` (`userId: "usr_admin_1"`, `userName: "Admin User"`, `isLoggedIn: true`).
    *   *Fix*: Implement a Login screen as the start destination. Manage session state properly via a SessionManager.
*   **P1: Navigation Architecture**
    *   *Description*: No proper navigation architecture (like Jetpack Navigation component) is used. It relies on simple state-based screen switching (`currentDestination` enum).
    *   *Fix*: Migrate to Jetpack Navigation Compose with a proper `NavHost`.
*   **P1: Dependency Injection**
    *   *Description*: All ViewModels use manual Factory pattern instead of Dependency Injection.
    *   *Fix*: Integrate Hilt for ViewModels and other dependencies.
*   **P1: State Management Pattern**
    *   *Description*: No proper `UiState` sealed class pattern. Raw domain objects are often exposed directly to UI.
    *   *Fix*: Refactor ViewModels to expose a single `UiState` sealed class/interface containing Loading, Success, Error, and Empty states.

## Design System

*   **P0: Reusable Components Missing**
    *   *Description*: No reusable components (e.g., `RestoButton`, `RestoTextField`, `RestoCard`). Each screen builds its own UI elements, leading to inconsistency.
    *   *Fix*: Create a `components/` directory and implement standardized Material 3 components following the design system.
*   **P1: Typography Incomplete**
    *   *Description*: `Type.kt` only defines `bodyLarge`. All other styles use Material defaults or are commented out.
    *   *Fix*: Define a complete typography scale in `Type.kt` using standard font sizes.
*   **P1: Hardcoded Spacing**
    *   *Description*: Random dp values are used throughout the app (4, 6, 8, 10, 12, 14, 16, 20, 24, 32, 44, 48, 80) instead of tokenized spacing constants.
    *   *Fix*: Define a `RestoSpacing` object (e.g., `Small`, `Medium`, `Large`) and use it consistently.
*   **P1: Color System Usage**
    *   *Description*: Hardcoded alpha values on colors are used in some screens despite a good color palette in `Color.kt`.
    *   *Fix*: Use color roles defined in `MaterialTheme.colorScheme` exclusively.
*   **P2: Missing Shape System**
    *   *Description*: No centralized shape system defined.
    *   *Fix*: Define Material 3 Shapes in `Shape.kt` and apply them to the `MaterialTheme`.

## Navigation

*   **P1: Incorrect Landing Page**
    *   *Description*: POS is currently the default landing page. Per the product spec, the Dashboard should be the landing page.
    *   *Fix*: Change the default initial route to `Dashboard`.
*   **P1: Missing Screens**
    *   *Description*: Settings, Reports, Profile, Login/Auth, Warehouse Management, and Accounting Details screens are missing.
    *   *Fix*: Implement these placeholder screens and add them to the navigation map.

## RTL & Localization

*   **P0: Missing Persian Localization**
    *   *Description*: `CustomerScreen` and `SupplierScreen` strings are entirely hardcoded in English (e.g., "Customer CRM & Profiles"). No Persian support exists here. Many screens mix Persian and English.
    *   *Fix*: Extract all strings to `strings.xml` (or equivalent localization pattern) and implement dual language support driven by `tenantState.isPersian`.
*   **P1: Currency Formatting**
    *   *Description*: Currency always shows as `$` regardless of locale. Hardcoded `$` used across screens.
    *   *Fix*: Use a tenant-aware currency formatter that displays `تومان` in Persian mode and the appropriate symbol in English mode.
*   **P1: Date Formatting**
    *   *Description*: Dates always show in Gregorian format, never Shamsi/Jalali.
    *   *Fix*: Implement a date formatter that uses Jalali dates when `isPersian` is true.
*   **P1: Persian Digits**
    *   *Description*: Numbers are not consistently converted to Persian digits.
    *   *Fix*: Implement `PersianFormatter` for number formatting throughout the app.

## Screens

### DashboardScreen
*   **P0**: Shows raw org/store IDs to user ("Org: org_acme | Store: store_main"). *Fix*: Display user-friendly store and organization names.
*   **P0**: Hardcoded `$` currency everywhere. *Fix*: Use locale-aware currency formatter.
*   **P1**: No loading, error, or proper empty state (plain text without illustration). *Fix*: Implement standard `UiState` handling with standard illustrations/actions.
*   **P1**: Shows technical "General Ledger Activity Log" to business users. *Fix*: Rename to user-friendly terms like "Recent Activity".
*   **P2**: No pull-to-refresh or localized date format. *Fix*: Add `pullRefresh` modifier and Jalali date support.

### CustomerScreen & SupplierScreen
*   **P0**: All strings hardcoded in English. NO Persian support. *Fix*: Extract and localize strings.
*   **P1**: `AddCustomerDialog` and `EditCustomerDialog` are 95% duplicate code (same for Suppliers). *Fix*: Consolidate into a single reusable form dialog component.
*   **P1**: No keyboard scroll support in dialogs. *Fix*: Add `verticalScroll` modifier and handle IME padding.
*   **P2**: No loading state and Search has no debounce. *Fix*: Add standard `UiState` and debounce logic in ViewModel.

### InventoryScreen
*   **P1**: No loading state and hardcoded English strings. *Fix*: Implement state management and extract strings.
*   **P1**: Stock adjustment dialog has poor UX - no warehouse preselection. *Fix*: Improve form UX by setting sensible defaults.
*   **P2**: No search functionality or filtering by stock status. *Fix*: Add search bar and filter chips.

### POS Screen
*   **P1**: Overly complex POS-centric design conflicts with product vision (8 files in pos/ folder). *Fix*: Re-evaluate POS workflow against current product requirements.
*   **P2**: Barcode scanner requires proper camera permission handling. *Fix*: Add Accompanist Permissions or standard Jetpack permission requests.

### Other Screens
*   **SalesOrdersScreen**: P1 No loading state, hardcoded strings. P2 No order detail view, no filtering.
*   **ExpensesScreen**: P1 No category explanation, hardcoded currency. P2 No loading state.
*   **PurchasesScreen**: P1 Hardcoded English strings, complex form dialog. P2 No loading state.
*   **MessagesScreen**: P1 Message parsing with regex, no AI. P2 Complex workflow.

## Data & Technical Issues

*   **P0: Swallowed Exceptions**
    *   *Description*: API calls are swallowed with empty catch blocks in `SalesOrderRepositoryImpl`.
    *   *Fix*: Handle exceptions properly by logging and emitting error states to the UI.
*   **P1: Hardcoded API URL**
    *   *Description*: API endpoint URL is hardcoded to `10.0.2.2` (emulator localhost).
    *   *Fix*: Move URL to `BuildConfig` and support different environments.
*   **P1: Lifecycle Unaware State Collection**
    *   *Description*: `collectAsState()` is used instead of `collectAsStateWithLifecycle()`.
    *   *Fix*: Update all Compose state collections to use `collectAsStateWithLifecycle()`.
*   **P1: State Loss on Configuration Change**
    *   *Description*: Form state is lost on rotation because `remember` is used instead of `rememberSaveable`.
    *   *Fix*: Use `rememberSaveable` for all form inputs.
*   **P1: Missing Input Validation**
    *   *Description*: No input validation beyond basic `isEmpty` checks.
    *   *Fix*: Implement proper form validation logic.
*   **P2: Missing Debounce**
    *   *Description*: No search debounce in any screen.
    *   *Fix*: Implement a standard Coroutine Flow debounce for search inputs.

## Mock Data

*   **P0: Fake Data Seeding**
    *   *Description*: `MockDataArchive.kt` contains fake organizations, stores, and products. `MockSaaSDataSource.kt` seeds fake data.
    *   *Fix*: Remove mock data from production builds. Ensure the app communicates with the real backend.
*   **P1: Hardcoded Constants**
    *   *Description*: Hardcoded taxRate: 0.08 (8%) in `CartItem`.
    *   *Fix*: Fetch tax rates from the backend configuration.
*   **P1: Hardcoded TenantState**
    *   *Description*: `TenantState` has hardcoded `userId: "usr_admin_1"`, `userName: "Admin User"`.
    *   *Fix*: Provide this through the authentication flow and session state.
