# P4.1 Project Rename & Rebrand Verification Report: Calcuapp → Resto

## 1. Objective

This task migrates the user-facing product identity and repository branding from **Calcuapp** to **Resto** across Web, Android, Backend, OpenAPI specifications, and documentation, while intentionally preserving technical internal identifiers (such as Android package names and database identifiers) to guarantee zero technical desync or compilation regressions.

---

## 2. Repository Audit Before Changes

Occurrences of `Calcuapp` / `calcuapp` were searched across the repository and classified into two categories:

- **User-Facing Product Identity (Migrated to Resto)**:
  - Web browser HTML title (`web/public/index.html`)
  - Web AppShell Brand Logo & Text (`web/src/components/AppShell.tsx`)
  - Web package metadata (`web/package.json`)
  - Android App Label (`android/app/src/main/res/values/strings.xml` -> `<string name="app_name">Resto</string>`)
  - OpenAPI Specification Title (`docs/api/openapi.yaml` -> `title: Resto SaaS Platform REST API`)
  - Backend Application Name (`backend/.env.example` -> `APP_NAME=Resto`)
  - Monorepo README (`README.md`)
  - Social Message Sample Template (`RESTO_ORDER`)
- **Internal Technical Identifiers (Intentionally Preserved)**:
  - Android package namespace / applicationId (`com.braveboy.calcuapp`)
  - Android Room database file name (`calcuapp_db`)
  - Docker container & network names (`calcuapp_app`, `calcuapp_postgres`, `calcuapp_redis`, `calcuapp-network`)
  - Historical verification report files in `docs/` (`P1_*`, `P2_*`, `P3_*`, `P4_*`) to maintain accurate development history records.

---

## 3. Changes Made

1. **Web Client**:
   - `web/public/index.html`: Updated browser title to `<title>Resto - Multi-Tenant Commerce OS & POS</title>`.
   - `web/src/components/AppShell.tsx`: Updated brand logo avatar to `R` and title to `رسـتو` / `Resto`.
   - `web/package.json`: Updated `"name": "resto-web"` and `"description": "Resto SaaS React Admin & POS Dashboard"`.
   - `web/src/pages/MessagesPage.tsx`: Updated sample message header to `RESTO_ORDER`.
2. **Android Client**:
   - `android/app/src/main/res/values/strings.xml`: Updated `<string name="app_name">Resto</string>`.
   - `android/app/src/main/java/com/braveboy/calcuapp/ui/messages/MessagesScreen.kt`: Updated sample message template to `RESTO_ORDER`.
3. **Backend & API**:
   - `backend/composer.json`: Updated `"name": "resto/backend"` and description.
   - `backend/.env.example`: Updated `APP_NAME=Resto`.
   - `docs/api/openapi.yaml`: Updated `title: Resto SaaS Platform REST API`.
4. **Documentation**:
   - `README.md`: Updated title to `# Resto SaaS Platform Monorepo`.

---

## 4. References Intentionally Preserved

| Remaining Reference | File / Location | Reason for Preservation | Technical Impact if Changed | Safe? |
|---|---|---|---|:---:|
| `com.braveboy.calcuapp` | `android/app/build.gradle.kts`, Android Kotlin source files | Android package namespace & applicationId | Changing package paths creates risk of Room/KSP generated code desync | **YES** |
| `calcuapp_db` | `AppDatabase.kt` | Room SQLite database file name | Preserved for local DB migration compatibility | **YES** |
| `calcuapp_app` / `calcuapp_postgres` | `docker-compose.yml` | Container & service name identifiers | Preserved for container network compatibility | **YES** |
| Historical Reports (`P1_*`, `P2_*`, `P3_*`) | `docs/*.md` | Historical audit logs & phase verification reports | Preserves true development audit trail | **YES** |

---

## 5. Automated Validation & Test Results

| Validation Step | Execution Command | Result | Evidence |
|---|---|:---:|---|
| **Backend Tests** | `php artisan test` | **PASS** | 16 Passed (62 assertions, 0.45s) |
| **Web Typecheck** | `npx tsc --noEmit` | **PASS** | 0 Errors |
| **Android Unit Tests** | `./gradlew testDebugUnitTest` | **PASS** | BUILD SUCCESSFUL (7.0s) |
| **Android APK Build** | `./gradlew assembleDebug` | **PASS** | BUILD SUCCESSFUL |
| **Docker Compose Config** | `docker compose config` | **PASS** | Valid compose schema |

---

## 6. Regression Assessment

The rebranding changes were strictly limited to identity, metadata, and user-facing branding text. Zero changes were made to:
- Multi-tenant architecture & isolation middleware
- Business logic (Inventory, Sales, Purchasing, Expenses, CRM)
- Double-entry accounting rules (`AccountingService.php`)
- API routes, HTTP contracts, headers, or idempotency behavior
- Financial precision calculations

---

## 7. Final Verification Decision

```
STATUS: COMPLETE WITH DOCUMENTED TECHNICAL REFERENCES
```
