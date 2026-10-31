# Shared Context Brief — Hesabi → Resto Integration

**Prepared by:** Tech Lead (Round 0 fact-finding)
**Date:** 2026-10-29
**Status:** Input for the 3-round team protocol
**Rule:** This file is the single source of verified facts. Do not invent APIs, files, or numbers. If something is unknown, write `UNKNOWN` and state how to verify it.

---

## 1. The two codebases (verified)

### A. Hesabi — personal finance app

- Path: `/Users/aminshojaei/AndroidStudioProjects/Hesabi`
- **Not a git repository.** There is no history, no tags, no remote. This matters for any "submodule / published artifact" plan.
- Single Gradle module `:app`, namespace + applicationId `com.braveboy.hesabi`.
- Toolchain: AGP `9.4.1`, Kotlin `2.4.21`, KSP `2.3.12`, Compose BOM `2026.09.00`, compileSdk `37`, minSdk `26`, targetSdk `36`, Java 17.
- Dependencies: Compose + Material3, Navigation Compose, Room `2.8.5` (KSP), DataStore Preferences `1.3.0`, WorkManager `2.12.0`, kotlinx-serialization, coroutines. Tests: JUnit4, Robolectric, Turbine, coroutines-test.
- No Hilt/Koin. Manual DI via `di/AppContainer.kt`.
- Persistence: Room DB `hesabi.db`, schema v1, **exportSchema = true**, 6 entities:
  `accounts`, `categories`, `transactions`, `budgets`, `sms_events`, `sender_rules`.
- DataStore file holds `AppSettings` (theme, currency, hideAmounts, smsCaptureEnabled, amountsAreRial, defaultAccountId…).
- **Pure, Android-free logic (the crown jewels):**
  - `core/money/Money.kt` — Persian/Arabic digit normalization, grouping, `Currency` (TOMAN/RIAL), Rial↔Toman, `parseToman("45 هزار")`, compact formatting. **Money is `Long` Toman everywhere; no Double.**
  - `core/date/JalaliDate.kt` — full Jalali calendar (`JalaliDate`, `MonthPeriod`, leap years, epoch-day storage, Persian month/weekday names, formatting).
  - `core/hash/Hashing.kt` — SHA-256 helpers (used for SMS dedupe).
  - `domain/logic/ReportCalculator.kt` — pure monthly aggregation, category slices, daily points, budget math, excludes transfers from income/expense.
  - `domain/logic/BudgetAndDuplicates.kt` — budget status + duplicate detector (type + amount + account + ±1 day).
  - `data/sms/BankSmsParser.kt` — pure deterministic Iranian bank-SMS parser: 30+ bank sender map, OTP/promo/balance-only rejection, amount extraction with Rial/Toman units, direction detection (debit/credit/transfer), last-4 extraction, Jalali date extraction, merchant extraction, confidence score, review flag. Unit-tested against a corpus.
  - `data/backup/` — JSON `BackupEnvelope` (schema 1), AES-based encrypted backup, CSV export with BOM.
- Domain model: `Account`, `Category`, `Transaction` (EXPENSE/INCOME/TRANSFER, positive Long Toman, account + optional toAccount + category + epochDay + source), `Budget` (per Jalali month, optional category), `SmsEvent`, `SenderRule`, `TransactionDraft` (validation).
- UI: full Compose screens — Home, Transactions, Reports, Budget, Accounts, Categories, SMS inbox, Backup, Settings, Onboarding, plus ~8 component files, theme (dynamic color, edge-to-edge), previews.
- **No multi-tenancy, no org/store concept, no auth, no server, no sync.** It is a single-user, local-first app.
- Test suite: 5 unit test files (Money, JalaliDate, BankSmsParser, BudgetAndDuplicates, ReportCalculator), previously green.

### B. Resto / Calcuapp — multi-tenant POS + accounting SaaS

- Path: `/Users/aminshojaei/AndroidStudioProjects/Calcuapp`
- Monorepo: `android/`, `backend/` (Laravel 11), `web/` (React 18 + TS), `ios/` (prototype, no xcodeproj), `docs/`, `scripts/`.
- Android: single `:app` module, `com.braveboy.calcuapp`, AGP `9.2.1`, Kotlin `2.2.10`, KSP `2.3.5`, compileSdk `37`, **minSdk `24`**, Java **11**, Compose BOM `2026.09.00`, Navigation3 + adaptive, Room `2.8.5` (`calcuapp_db`, v2, `fallbackToDestructiveMigration`), DataStore, Retrofit + Moshi + OkHttp.
- Android architecture: manual DI (`di/AppContainer.kt`), repositories + StateFlow ViewModels, multi-tenant via `TenantPreferences` (orgId/storeId/warehouseId/userId) and `SessionStore` (token).
- Android already has: POS, inventory, purchases, suppliers, customers, sales orders, **expenses** (local + remote API), **ledger entries**, **messages/intake** (`DeterministicMessageParser` parses *order* messages, not bank SMS), dashboard, settings, onboarding, 17 shared components, preview gallery.
- Android money type is `Double` in most models (`Product.price`, `Expense.amount`, `LedgerEntry.amount`, `CartItem` with rounding hacks). Known precision debt (see `docs/P2_2_MONETARY_PRECISION_VERIFICATION.md`).
- Android manifest: `INTERNET`, `CAMERA` only. Has `ACTION_SEND` (text/plain) and `PROCESS_TEXT` intent filters — a share-to-import path already exists. **No SMS permission, no receiver, no notification listener.**
- Backend: Laravel multi-tenant, `Expense` model + `/api/v1/expenses` CRUD + summary + categories, double-entry accounting (`Account`, `JournalEntry`, `JournalEntryLine`, `ChartOfAccounts`), `ImportedMessage` intake, Import Center (CSV/XLSX mapping), audit log, idempotency keys.
- Web: `ExpensesPage`, `AccountingPage`, `MessagesPage`, `ImportCenterPage`, guide system in `web/src/guide/`.
- Quality gate: `./scripts/verify-all.sh [web|backend|android|all]` (web tsc+tests+build, backend `php artisan test`, android `assembleDebug testDebugUnitTest`).
- AGENTS.md rules that bind any solution: pure Compose (no XML layouts), M3 **dynamic color + edge-to-edge**, preview for every new shared component, one guide system only, full absolute paths, `verify-all.sh` must pass.
- Team protocol: `docs/TEAM_OPERATING_MODEL.md` (3 rounds, file ownership, senior rubric).

---

## 2. What "use Hesabi's capabilities inside Resto" can mean (candidate scope)

Capability inventory, each judged on its own:

| # | Hesabi capability | Overlap in Resto today | Candidate value |
|---|---|---|---|
| C1 | Bank SMS parsing (30+ banks, direction, last-4, merchant, confidence, dedupe) | None (Resto parses order text, not bank SMS) | High for expense capture |
| C2 | Jalali calendar + Persian money/digit formatting | Weak/inline (`PersianFormatter` has a different Gregorian→Jalali algorithm and `Double` formatting) | High, shared correctness |
| C3 | Personal accounts/categories/transactions/transfers ledger | Resto has org-scoped `Expense` + double-entry `JournalEntry` | Medium — model conflict risk |
| C4 | Monthly budgets + overspend alerts | None | Medium-high |
| C5 | Reports (category slices, daily series, MoM delta, budget vs spend) | Resto has dashboard metrics + accounting reports | Medium — must not double count |
| C6 | Encrypted backup/restore + CSV export | Resto has backend + import center | Low-medium |
| C7 | Compose UI (screens, charts, amount input, SMS review inbox) | Resto has its own Resto* design system | Medium — must adopt Resto theme |
| C8 | Duplicate detection | None for expenses | High |
| C9 | Local-first/offline personal ledger | Resto is offline-cache + API sync | Medium |

**Critical product question the team must answer:** is this (a) a *personal wallet* feature inside a business app (owner's own money, not the business's books), (b) *business expense capture* fed by bank SMS, or (c) *both, clearly separated*? Double counting business expenses into both the personal ledger and the double-entry books is the single biggest correctness risk.

---

## 3. Candidate integration mechanisms (to be judged, not assumed)

| Option | Shape | Pros | Cons / risks |
|---|---|---|---|
| M1 | **Gradle composite / included build** — add `includeBuild("../Hesabi")` or vendor `android/hesabi/` as modules `:hesabi-core`, `:hesabi-data`, `:hesabi-ui` | fastest, source-level refactor possible, no publishing infra | Hesabi has no git; cross-repo path coupling; version skew |
| M2 | **Published Android library (AAR)** via `maven-publish` → `mavenLocal` / GitHub Packages, SemVer | clean contract, real SDK | needs CI + repo + versioning discipline; AAR can't carry Compose resources cleanly unless careful |
| M3 | **Kotlin Multiplatform library** (`core` pure Kotlin, `android` target) | core logic reusable on web/iOS later | overkill for 2-week horizon; KMP adds toolchain risk |
| M4 | **Separate app + IPC** (ContentProvider/AIDL/deep link) | hard isolation, Play-policy isolation for SMS | two installs, terrible UX, no shared UI |
| M5 | **Backend-first** — implement capabilities in Laravel, consume via API | consistent multi-tenant sync, web+iOS get it | duplicates the local-first advantage; no offline; longer |
| M6 | **Copy/paste code** into Resto | zero infra | fork rot, two sources of truth — reject unless timeboxed |

Non-negotiables any option must satisfy:
1. No second source of truth for the same money.
2. Multi-tenant isolation (orgId/storeId) for anything persisted.
3. Room schema safety (Resto DB v2, destructive migration; Hesabi DB v1, exported schema).
4. Play Store policy on `READ_SMS` (restricted permission; alternative: notification listener / share intent / clipboard).
5. Resto AGENTS.md (dynamic color, previews, verify-all) and Resto design system.
6. minSdk 24 vs 26, Java 11 vs 17, Kotlin 2.2.10 vs 2.4.21, AGP 9.2.1 vs 9.4.1 — concrete compatibility plan required.

---

## 4. Team roles and what each must produce in Round 1

| Role | Must answer |
|---|---|
| **PM** | Which capabilities, for which user, in which release; MVP vs later; acceptance criteria; success metric; what we explicitly will NOT do |
| **Architect** | Chosen mechanism with ADR-style trade-off table, module graph, public API surface, versioning, migration path, fallback |
| **UX** | Where it lives in Resto IA, flows for SMS review / manual entry / budgets, empty/loading/error/offline, RTL, a11y, previews, separation of personal vs business money |
| **Android Engineer** | Concrete Gradle/module plan, package/namespace strategy, Room coexistence + tenant scoping, DI wiring, theme bridge, SMS permission strategy, build risks, test plan |
| **Backend/Data Engineer** | What syncs, API contracts, idempotency, mapping to `Expense`/`JournalEntry`, conflict rules, privacy/retention, audit |
| **Web/iOS Engineer** | What (if anything) reaches web/iOS now, how the shared contract avoids a second implementation, guide-system impact |
| **QA / Skeptic** | Attack the premise: policy, privacy, double counting, build/toolchain, data loss, maintenance; P0/P1/P2 findings; GO/NO-GO criteria |
| **Growth/Business** | Why this belongs in Resto, packaging/pricing, activation, support cost, risk to the SaaS story |

---

## 5. Output rules

- Persian for the final deliverable; English is fine inside code/ADR tables.
- No code changes in Rounds 1–2. Read-only analysis.
- Every claim about the codebase must cite a real path.
- Round 1: max ~900 words per role, written to `docs/product/_team/hesabi-sdk/round1/<role>.md`.
- Round 2: max ~700 words per role, written to `docs/product/_team/hesabi-sdk/round2/<role>.md`.
- End Round 1 and Round 2 with an explicit **position** line: `POSITION: <one sentence>`.
