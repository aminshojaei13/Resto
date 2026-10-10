# P12.2 — Android Messaging Intake & Order Creation Verification Report

**Phase:** P12.2 — Android Messaging Intake & Order Creation  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3, Room, Retrofit) & Laravel REST API Backend  
**Date:** 2026-10-10  
**Verification Status:** **READY / VERIFIED (All 26 Unit Tests Passing & Debug Build Successful)**

---

## 1. Executive Summary

In Phase P12.2, Resto was extended with a secure, production-ready, deterministic intake pipeline for customer orders received via social messaging channels (Instagram, Telegram, WhatsApp, Android Share, and Clipboard Paste).

### Key Accomplishments:
1. **Android Share & Selection Intake:** Integrated `ACTION_SEND` (MIME `text/*`) and `ACTION_PROCESS_TEXT` into `AndroidManifest.xml` and `MainActivity.kt`.
2. **Resilient Session & State Preserving Intake:** Implemented `PendingImportManager` singleton ensuring shared text is preserved, sanitized (removing control characters and capping at 8,000 characters), and automatically restored even if the user is unauthenticated at share time.
3. **Deterministic Parsing Engine:** Implemented `DeterministicMessageParser.kt` providing offline, instant extraction of customer details (names, Iranian & international phone numbers, delivery addresses), quantities, units of measurement, payment methods, and catalog matching with fuzzy confidence scoring.
4. **Zero AI Runtime Dependency:** The parser operates with zero external AI models or runtime AI services, strictly relying on deterministic regex tokenization and character normalization (converting Persian `۰-۹` and Arabic `٠-٩` numerals to standard ASCII, normalizing Kaf/Yeh, and stripping zero-width non-joiners).
5. **Interactive Review & Matching UI:** Redesigned `MessagesScreen.kt` adhering strictly to Material 3, Persian-first RTL, `RestoSpacing`, and `RestoShapes`. Features include editable customer fields, line item quantity steppers, catalog product picker dialog for unmatched items, warehouse delivery selector, payment method toggles, and live price breakdowns in tenant currency.
6. **Strict Order Validation & Idempotent Checkout:** Prevented accidental or duplicate order placement. Orders with unmatched items cannot be checked out until matched. Checkout injects an explicit `X-Idempotency-Key` (`msg-ord-{uuid}`) processed through Laravel backend `IdempotencyMiddleware`.
7. **Official Social Integrations Feasibility Assessment:** Authored a complete technical, compliance, and architectural assessment for official Instagram, WhatsApp, and Telegram integrations.

---

## 2. Pre-Implementation Audit & Discovered Defects

Before writing new code, a full audit of the codebase was conducted:

| Component | Discovered Defect / Limitation | Resolution in P12.2 |
| :--- | :--- | :--- |
| `CalcuappApiService.kt` | `parseMessage` sent `@Query("raw_text") String`, which fails on multi-line text or URLs due to URL encoding/query length limits. | Converted to `@POST("messages/parse")` with `@Body ParseMessageRequestDto`. |
| `CheckoutRequestDto` | Missing `source`, `payment_status`, and `fulfillment_status` parameters, preventing the backend from tagging social orders. | Added fields to `CheckoutRequestDto` with `@Json` annotations. |
| `MainActivity.kt` | No support for `ACTION_PROCESS_TEXT` (text selection menu). Shared text was lost if the app was reopened or if user was logged out. | Added `ACTION_PROCESS_TEXT` intent filter and decoupled reception into `PendingImportManager`. |
| `MessagesViewModel.kt` | Had hardcoded English strings, lacked catalog product injection, had no offline parsing fallback, and swallowed errors. | Rewrote with StateFlow, injected `ProductRepository` and `CustomerRepository`, added fallback parsing, and full draft editing. |
| `MessagesScreen.kt` | Contained hardcoded English texts, hardcoded `$`, lacked Material 3 design tokens, lacked a product matching modal dialog. | Redesigned with Material 3, Persian RTL, dynamic currency, catalog picker dialog, and order creation feedback. |
| `SalesOrderRepositoryImpl` | Did not pass `idempotencyKey` down to `apiService.checkout`, risking double billing upon retry. | Added `@Header("X-Idempotency-Key")` and generated unique UUIDs per checkout invocation. |

---

## 3. Architecture & End-to-End Data Flow

```
[Customer Message in Instagram/Telegram/WhatsApp]
                      │
    ┌─────────────────┴─────────────────┐
    ▼                                   ▼
[Android Share Intent (ACTION_SEND)]  [Copy to Clipboard]
    │                                   │
    ▼                                   ▼
[MainActivity: handleIncomingIntent]  [MessagesScreen: "چسباندن از حافظه"]
    │                                   │
    └─────────────────┬─────────────────┘
                      ▼
            [PendingImportManager]
             - Strips NULL/control bytes
             - Truncates to max 8,000 chars
             - Detects source (INSTAGRAM, TELEGRAM, WHATSAPP, etc.)
                      │
                      ▼
            [MessagesViewModel.parseMessage]
                      │
         ┌────────────┴────────────┐
         ▼ (Network Available)     ▼ (Offline or Fallback)
    [Laravel Backend API]     [DeterministicMessageParser]
    POST /api/v1/messages/parse  - Numeral normalization (۰-۹ -> 0-9)
         │                       - Customer name, phone, address regex
         │                       - Item tokenization & unit matching
         │                       - Local Room Catalog matching & confidence
         └────────────┬────────────┘
                      ▼
            [ParsedOrderDraft StateFlow]
                      │
                      ▼
            [MessagesScreen: Interactive Review]
             1. Customer details (editable)
             2. Extracted items (stepper, remove, catalog matcher dialog)
             3. Delivery warehouse selector
             4. Payment method selector
             5. Real-time pricing breakdown (Subtotal, Discount, Tax, Grand Total)
                      │
                      ▼ (User clicks "تأیید نهایی و صدور فاکتور فروش")
            [Pre-flight Validation]
             - Has items?
             - Any unmatched items remaining? (Blocked if true)
             - Warehouse selected?
                      │
                      ▼
            [SalesOrderRepository.processCheckout]
             - Injects X-Idempotency-Key: msg-ord-{UUID}
             - Records in local Room DB
             - POST /api/v1/orders/checkout with source, customer, items
                      │
                      ▼
            [Sales Order Created]
             - Order number returned (e.g. ORD-20261010-001)
             - Draft cleared, success modal displayed
```

---

## 4. Android Manifest & Text Intake Implementation

### Intent Filters in `AndroidManifest.xml`
```xml
<!-- Android Share Target (ACTION_SEND) -->
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
    <data android:mimeType="text/*" />
</intent-filter>

<!-- Process Selected Text (ACTION_PROCESS_TEXT) -->
<intent-filter>
    <action android:name="android.intent.action.PROCESS_TEXT" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>
```

### Sanitization and Security in `PendingImportManager.kt`
- **Max length:** Capped at 8,000 characters to prevent Memory Exhaustion or UI jank.
- **Control character sanitization:** Strips `\u0000` null bytes and unprintable characters.
- **Source Auto-Detection:** Analyzes text for keywords and links (e.g., `instagram.com`, `t.me/`, `wa.me/`, `اینستاگرام`, `واتس‌اپ`, `تلگرام`).

---

## 5. Deterministic Parsing Engine Specification

Implemented in `com.braveboy.calcuapp.data.intake.DeterministicMessageParser`:

1. **Character & Numeral Normalization:**
   - Converts Persian digits `۰۱۲۳۴۵۶۷۸۹` to ASCII `0123456789`.
   - Converts Arabic digits `٠١٢٣٤٥٦٧٨٩` to ASCII `0123456789`.
   - Normalizes Arabic Kaf (`ك` -> `ک`) and Arabic Yeh (`ي` -> `ی`).
   - Removes zero-width non-joiners (`\u200C`) in matching comparisons.
2. **Customer Information Extraction:**
   - **Name:** Recognizes labels `نام:`, `مشتری:`, `گیرنده:`, `نام و نام خانوادگی:`, conversational greetings (`علی رضایی هستم`).
   - **Phone:** Extracts numbers with or without Iranian prefix (`09...`, `+989...`, `00989...`, `شماره تماس:`, `تلفن همراه:`).
   - **Address:** Captures delivery addresses from lines starting with `آدرس:`, `نشانی:`, `مقصد:`, `آدرس تحویل:`.
3. **Item & Quantity Parsing:**
   - Matches quantity prefixes: `۲ بسته قهوه`, `3 عدد کیک`, `1x پیتزا`, `چای - ۲ عدد`.
   - Strips conversational and courtesy noise lines (`ممنون`, `با تشکر`, `سپاس`, `سلام`, `سفارش:`, `اقلام:`).
4. **Catalog Matching with Confidence:**
   - Matches against the tenant's active Room database products (`List<Product>`).
   - Prioritizes SKU exact matches, then exact product names, followed by token intersection scoring.
   - Items with confidence `< 0.6` are flagged with `isMatched = false` and marked for manual review.

---

## 6. UI & UX Review Flow

- **Language & Direction:** Pure Persian-first RTL (`LocalLayoutDirection provides LayoutDirection.Rtl`).
- **Design Tokens:** Strict enforcement of `RestoSpacing` (xs=8dp, sm=12dp, md=16dp, etc.), `RestoShapes`, and `RestoBadge` semantic states.
- **Product Catalog Picker Dialog:** Allows users to search the tenant's catalog by name or SKU, view active warehouse inventory levels, and match unmatched items with a single tap.
- **Order Placement Safety:** The confirmation button is disabled if any items remain unmatched.

---

## 7. Official Direct Messaging Integrations Feasibility Assessment

As requested, official direct messaging APIs (Meta Instagram Graph API, WhatsApp Cloud API, and Telegram Bot API) were evaluated for future phases:

### A. Meta Instagram Graph API / Messenger API for Instagram
* **Feasibility:** High, but requires Meta App Review and Business Verification.
* **Architecture:**
  - Webhook endpoint (`POST /api/v1/webhooks/instagram`) on Laravel backend.
  - Subscribes to `messages` and `messaging_postbacks`.
  - Android app receives synchronized incoming messages via Server-Sent Events (SSE) or WebSockets.
* **Limitations & Compliance:**
  - 24-hour messaging window (Standard Messaging Window) to reply to customers without paying for message tags.
  - Requires Instagram Professional Account connected to a verified Facebook Page.
  - App Review requires submitting screen recordings and business documentation.

### B. Meta WhatsApp Cloud API (Graph API)
* **Feasibility:** Very High, established for commerce.
* **Architecture:**
  - Registered Meta Developer App with WhatsApp Business Account (WABA).
  - Webhooks delivered to Laravel backend (`POST /api/v1/webhooks/whatsapp`).
  - Supports Interactive Messages (List Messages, Reply Buttons, Catalogs).
* **Cost & Compliance:**
  - Pricing is conversation-based (Utility, Authentication, Marketing, Service).
  - Utility and service conversations within customer-initiated 24-hour windows have lower tier costs.
  - Requires phone number not associated with a personal WhatsApp account.

### C. Telegram Bot API
* **Feasibility:** Extremely High, lowest barrier to entry.
* **Architecture:**
  - Dedicated Telegram Bot (e.g. `@RestoStoreBot`) created via BotFather.
  - Webhook configured (`POST /api/v1/webhooks/telegram`).
  - Supports inline web apps (Telegram Mini Apps) and custom keyboards for order confirmations.
* **Cost & Compliance:**
  - 100% free with no per-message fees.
  - Fast implementation with official Bot API.
  - Network considerations in specific regions (may require backend proxy if hosted in environments with restricted access).

---

## 8. Verification & Build Verification Results

### Build Verification Command:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew testDebugUnitTest assembleDebug
```

### Result:
```
BUILD SUCCESSFUL in 7s
44 actionable tasks: 9 executed, 35 up-to-date
```

### Automated Unit Test Results (`26 passed, 0 failed`):
1. `testPersianAndArabicNumeralNormalization` — **PASSED** (Converts `۱۲۳۴۵۶۷۸۹۰` & `١٢٣٤٥٦٧٨٩٠` to `1234567890`)
2. `testExtractCustomerDetailsFromPersianMessage` — **PASSED** (Extracts name, 0912... phone, delivery address)
3. `testProductMatchingWithCatalog` — **PASSED** (Matches coffee and tea catalog items, quantities, and prices)
4. `testUnmatchedItemsDetection` — **PASSED** (Identifies unknown items, marks `isMatched=false`, and sets ambiguity reasons)
5. `testDraftFinancialCalculationsWithTax` — **PASSED** (Computes subtotal, tax rate, discount, and grand total)
6. `testPendingImportManagerSanitizationAndSourceDetection` — **PASSED** (Strips null bytes, detects Instagram source)
7. `testPendingImportManagerCapsLengthTo8000Chars` — **PASSED** (Truncates 12k char payloads to 8k chars)
8. `testIdempotencyKeyUniqueness` — **PASSED** (Ensures distinct UUID keys per checkout request)
9. *All 18 pre-existing test cases (Auth, Session, POS, Accounting, Currency, PersianFormatter)* — **PASSED**

---

## 9. Conclusion

Phase P12.2 has been successfully completed. The Resto Android app now possesses a robust, secure, and user-friendly intake flow for social media order capture with zero AI runtime dependency, deterministic Persian text processing, complete catalog integration, and strictly enforced idempotency.
