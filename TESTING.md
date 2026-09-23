# TESTING.md - Test Strategy & Automation Standards

## 1. Unit Testing
- **Android**: JUnit 4 / Kotlinx Coroutines Test for ViewModels, Cart Math, DataStore preferences, and Room DAOs (`./gradlew testDebugUnitTest`).
- **Backend**: Pest PHP / PHPUnit for API endpoints, double-entry ledger balance validation, and inventory stock adjustment triggers.

## 2. Integration & Instrumented Testing
- **Android**: Compose UI Test (`androidx.compose.ui.test.junit4`) verifying POS checkout dialog, barcode scanner modal, and stock adjustment dialogs.
- **Backend**: Database migration tests and OpenAPI request/response validation.

## 3. Automation Commands
```bash
# Android Unit Tests
cd android && ./gradlew testDebugUnitTest

# Android Debug Assemble
cd android && ./gradlew assembleDebug
```
