# ADR-001 — Hesabi به‌عنوان SDK درونی Resto

**Status:** Proposed · **Round:** 1 · **Date:** 2026-10-29 · **Owner:** System Architect
**Inputs:** `docs/product/_team/hesabi-sdk/CONTEXT_BRIEF.md`, `ARCHITECTURE.md`, `AGENTS.md`

## Context

Resto یک POS چند-مستأجری با دفتر دوطرفه است (`backend`: `JournalEntry`, `Expense`). Hesabi یک دفتر شخصی تک‌کاربره با منطق خالصِ آزموده است (C1 پارس پیامک بانکی، C2 تقویم جلالی/پول، C8 تشخیص تکرار). آنچه ارزش دارد **منطق** است، نه اپلیکیشن. هر راه‌حلی باید بدون ایجاد منبع حقیقت دوم برای پول، این منطق را در دسترس Resto بگذارد.

## Decision

**M1v — «Vendored Composite Modules»**: کد Hesabi به سه ماژول Gradle **درون** مخزن Resto (`android/hesabi-sdk/`) تبدیل می‌شود و با اسکریپت از تگ‌های Git هسابی همگام می‌شود؛ خود Hesabi ابتدا Git + SemVer می‌گیرد. انتشار AAR (M2) **مقصد** است، نه نقطه شروع — و چون مرز ماژول‌ها یکی است، مهاجرت بعدی سه خط تغییر وابستگی است.

### Weighted trade-off (1–5, وزن‌ها مجموعاً ۱.۰۰)

| Criterion | W | M1v vendored | M1 includeBuild | M2 AAR | M5 backend | M6 copy |
|---|---|---|---|---|---|---|
| Time-to-first-value | .15 | 5 | 5 | 2 | 2 | 5 |
| Isolation | .10 | 3 | 2 | 5 | 5 | 1 |
| Upgrade path | .15 | 4 | 2 | 5 | 4 | 1 |
| Testability | .10 | 5 | 4 | 4 | 3 | 3 |
| Multi-tenant safety | .15 | 4 | 4 | 4 | 5 | 2 |
| Play policy | .10 | 4 | 4 | 5 | 5 | 4 |
| Toolchain risk | .15 | 3 | 1 | 4 | 5 | 4 |
| Maintenance cost | .10 | 4 | 2 | 3 | 4 | 1 |
| **Weighted** | | **4.00** | 3.00 | 3.95 | *gate fail* | 2.70 |

**Gate rejects:** M5 — پارس پیامک بانکی روی دستگاه اتفاق می‌افتد؛ بک‌اند پیامکی که نمی‌بیند را نمی‌تواند پارس کند، پس C1 را اصلاً تحویل نمی‌دهد. M3 (KMP) — ریسک ابزار برای افق دو هفته. M4 — دو نصب، UX غیرقابل قبول. M6 — rot.
**چرا M1v نه M1 خالص:** `includeBuild("../Hesabi")` کد را با Kotlin `2.4.21`/Java 17 کامپایل می‌کند و مصرف‌کننده `2.2.10` است → ناسازگاری metadata. کپی فروشده اجازه می‌دهد toolchain ماژول‌ها به Resto پین شود، بدون شکستن build خود Hesabi.

## Module graph

```text
android/settings.gradle.kts
  include(":app")
  include(":hesabi-core")   -> hesabi-sdk/core   (kotlin("jvm"), no Android)
  include(":hesabi-data")   -> hesabi-sdk/data   (com.android.library + Room/KSP)
  include(":hesabi-ui")     -> hesabi-sdk/ui     (Phase 4, optional)
```

| Moves → `:hesabi-core` (JVM) | Moves → `:hesabi-data` (Android) | Stays in Hesabi `:app` |
|---|---|---|
| `core/money`, `core/date`, `core/hash` | `data/local` (DB, entities, DAOs) | همه‌ی `ui/*`، theme، navigation |
| `domain/model`, `domain/logic` | `data/repository`, `data/seed` | `di/AppContainer.kt`, `MainActivity` |
| `data/sms/BankSmsParser` (pure) | `data/backup` | `SmsReceiver.kt` + permission (Play) |

`BankSmsParser` به core می‌رود چون خالص است و باید بدون Robolectric آزموده شود. `SmsReceiver` هرگز منتقل نمی‌شود (سیاست Play).

## Public API

```kotlin
package com.braveboy.hesabi.sdk

data class LedgerScope(val orgId: String, val storeId: String) { init { require(orgId.isNotBlank() && storeId.isNotBlank()) } }
data class HesabiConfig(val scope: LedgerScope, val currency: Currency = Currency.TOMAN,
    val clock: HesabiClock = SystemHesabiClock, val databaseName: String = "hesabi_ledger.db",
    val features: Set<HesabiFeature> = setOf(HesabiFeature.SMS_IMPORT))

interface HesabiSdk {                       // تنها نقطه ورود
    val ledger: LedgerApi; val sms: SmsImportApi; val budgets: BudgetApi; val reports: ReportApi
    suspend fun close()
}
interface LedgerApi {
    fun observe(period: MonthPeriod? = null): Flow<List<LedgerTransaction>>
    suspend fun record(draft: TransactionDraft): Result<LedgerTransaction, HesabiError>
}
interface SmsImportApi {
    suspend fun parse(sender: String, body: String, atMillis: Long): ParseOutcome
    suspend fun accept(parsedId: String, categoryId: String?): Result<LedgerTransaction, HesabiError>
}
sealed interface HesabiError {
    data class ScopeMismatch(val expected: LedgerScope) : HesabiError
    data class Validation(val fields: List<String>) : HesabiError
    data class Storage(val cause: Throwable) : HesabiError
}
object Hesabi { fun create(context: Context, config: HesabiConfig): HesabiSdk }
```

**`internal`:** `HesabiDatabase`, همه‌ی `*Entity`/`*Dao`/`*RepositoryImpl`, `BackupManager`, `SeedData`, mapperها. `:app` فقط `api(":hesabi-core")` + `implementation(":hesabi-data")` می‌گیرد؛ تنها کلاس عمومی داده‌ای `Hesabi` است.

## Hard boundaries

**(i) بدون tenant id.** DB جدید است، پس schema از v1 با `orgId`/`storeId` متولد می‌شود — بدون هزینه مهاجرت. هر ۶ جدول ستون‌های scope می‌گیرند و **کلیدهای یکتا scope-دار** می‌شوند: `(orgId,storeId,externalRef)`, `(orgId,storeId,rawHash)`, `(orgId,storeId,categoryId,periodKey)`. DAO هرگز scope پیش‌فرض ندارد؛ facade فقط از `LedgerScope` ساخته می‌شود. `verifyScopeIntegrity()` در باز شدن DB، اگر ردیفی با scope دیگر دید، fail-closed می‌کند.

**(ii) دو Room DB.** `calcuapp_db` و `hesabi_ledger.db` جدا می‌مانند: Resto از `fallbackToDestructiveMigration` استفاده می‌کند (`AppDatabase.kt:70`)؛ اگر دفتر شخصی داخل آن بود، یک version bump داده‌ی مالی کاربر را پاک می‌کرد. `hesabi_ledger.db` هرگز destructive ندارد و `exportSchema=true` با `schemas/` کامیت‌شده. چون Room تراکنش بین دو DB ندارد، اتصال **saga پس از commit** است، نه تراکنش توزیع‌شده.

**(iii) پول.** مرز API فقط `Long` تومان است؛ `Double` هرگز وارد `:hesabi-core` نمی‌شود (گارد: `grep -r Double hesabi-sdk/core` در verify). تبدیل یک‌جا در `:app`/`HesabiExpenseBridge` با `Toman.toRestoDouble()` و `Double.toTomanStrict()` (reject غیرصحیح) انجام می‌شود — همان نقطه‌ای که وقتی Resto بدهی `Double` را (`docs/P2_2_...md`) تسویه کرد، عوض می‌شود.

**(iv) شمارش مضاعف (P0).** هر ردیف `ledgerDomain: PERSONAL | BUSINESS` دارد. `PostingPolicy` در bridge: `PERSONAL_ONLY` (پیش‌فرض، صفر نوشتن در Resto) یا `BUSINESS_EXPENSE` (فقط یک `Expense` با `externalRef` = شناسه‌ی ردیف؛ journal را بک‌اند از Expense می‌سازد). SDK هرگز در `LedgerEntryEntity` نمی‌نویسد. گاردها: unique index روی `expense.externalRef`، و تستی که BUSINESS ⇒ دقیقاً ۱ Expense و ۰ journal.

**(v) تم.** در فاز ۱–۳ هیچ UI از Hesabi منتقل نمی‌شود. صفحه‌ی بازبینی پیامک با `Resto*` کامپوننت‌ها و `RestoTheme` (dynamic color + edge-to-edge) در `ui/wallet` نوشته می‌شود و `@Preview` می‌گیرد. `:hesabi-ui` فقط اگر PM تأیید کند، و آن هم بدون `HesabiTheme`.

## Versioning

**Git + SemVer + vendored sync.** گام‌ها: (1) `git init` در Hesabi، `.gitignore`، کامیت baseline، تگ `v0.1.0-baseline`. (2) استخراج ماژول‌ها **ابتدا داخل Hesabi** و سبز کردن ۵ فایل تست موجود — تنها تضمین رفتار-محافظ. (3) تگ `v1.0.0`. SemVer: MAJOR=شکست API/schema، MINOR=افزودنی، PATCH=fix. (4) `scripts/sync-hesabi.sh <tag>` با `git archive` + بازنویسی نسخه‌ها به کاتالوگ Resto + نوشتن `SYNCED_AT` (tag, SHA, date). (5) `scripts/check-hesabi-vendor.sh` در `verify-all.sh`: sync به پوشه‌ی موقت و diff — ویرایش دستی ممنوع.

## Cutover

| Phase | کار | Rollback |
|---|---|---|
| 0 (۰.۵ روز) | git init + تگ + سند استخراج | حذف `.git` |
| 1 (۱–۲ روز) | فقط `:hesabi-core`; جانشینی `PersianFormatter` پشت flag | flag خاموش، حذف ماژول |
| 2 (۳–۴ روز) | `:hesabi-data` + DB scope-دار + facade؛ حالت shadow (پارس بدون نوشتن) | flag خاموش؛ DB جدید بدون داده |
| 3 (۳–۴ روز) | bridge + `BUSINESS_EXPENSE` + صفحه‌ی بازبینی | برگشت به `PERSONAL_ONLY` + پاک‌سازی با `externalRef` |
| 4 | گزارش/بودجه، `:hesabi-ui` اختیاری | پنهان‌کردن entry point |
| 5 | انتشار AAR | برگشت به `project(...)` |

## Risks

| # | Risk | Mitigation |
|---|---|---|
| 1 | شمارش مضاعف هزینه در دفتر شخصی و دوطرفه (P0) | `ledgerDomain` + policy + unique `externalRef` + تست یکپارچگی |
| 2 | نشت داده بین مستأجرها روی دستگاه مشترک (P0) | ستون/ایندکس scope، facade اجباری، `verifyScopeIntegrity()` fail-closed، تست «B صفر ردیف A را می‌بیند» |
| 3 | شکست build: Kotlin 2.4.21/Java17/minSdk26 → 2.2.10/Java11/minSdk24 (P0) | پین toolchain در ماژول‌های vendored + اجرای ۵ فایل تست در `verify-all.sh`؛ fallback: ارتقای Kotlin در Resto به‌صورت PR مستقل |
| 4 | سیاست Play روی `READ_SMS` (P1) | `SmsReceiver` منتقل نمی‌شود؛ ورودی از `ACTION_SEND`/`PROCESS_TEXT` موجود در manifest؛ پارسر خالص ⇒ کانال ورودی قابل تعویض |
| 5 | واگرایی خاموش چون Hesabi تاریخ ندارد (P1) | Git+SemVer+`SYNCED_AT`+`check-hesabi-vendor.sh` در quality gate |

POSITION: Hesabi را به‌صورت سه ماژول Gradle فروشده (`:hesabi-core` خالص، `:hesabi-data` با DB scope-دار، `:hesabi-ui` اختیاری) با facade تک‌نقطه‌ای و همگام‌سازی از تگ‌های Git بپذیریم، نه به‌عنوان AAR منتشرشده و نه با `includeBuild` بین‌مخزنی، و UI آن را هرگز قرض نگیریم.
