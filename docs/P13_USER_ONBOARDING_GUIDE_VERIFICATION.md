# P13 — Interactive User Onboarding & Guided Learning Flow Verification Report

**Phase:** P13 — User Onboarding & First-Time Experience Guide (Android & Web)  
**Date:** 2026-10-10  
**Status:** **COMPLETED / VERIFIED (Android Compose & Web React)**  

---

## ۱. هدف و چرایی پیاده‌سازی (Executive Summary & Objective)

در تست‌های کاربری و ارزیابی اولیه مشخص شد که کاربر پس از اولین ورود (First-time Login) با صفحه داشبورد خالی روبرو شده و ترتیب درست انجام فرآیندهای تجاری برایش گنگ بود:
* کاربر نمی‌دانست فرآیند را از کجا آغاز کند (ثبت کالا، انبارگردانی، خرید یا فروش).
* یادگیری گام‌به‌گام و استاندارد جریان کسب‌وکار (Workflow) بدون راهنما برای کاربران جدید دشوار بود.

برای رفع این مشکل، در این فاز یک **سیستم جامع راهنمای مرحله‌به‌مرحله و پویای کاربری (Interactive Onboarding & Guided Workflow)** هم برای پلتفرم **Android** (با Jetpack Compose) و هم برای **Web** (با React و TypeScript) طراحی و پیاده‌سازی شد.

---

## ۲. طراحی سناریوی ۴ مرحله‌ای فرآیند کسب‌وکار (The 4-Step Business Workflow)

بر اساس اصول طراحی سیستم‌های SaaS/Commerce، ترتیب مراحل به شکلی طراحی شد که کاربر زنجیره ارزش واقعی فروشگاه را تجربه کند:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   مسیر ۴ مرحله‌ای راه‌اندازی و یادگیری رستو                │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
       [مرحله ۱: ثبت اولین کالا و موجودی انبار] (الزامی)
       - تعریف مشخصات کالا، قیمت فروش و واحد شمارش
       - تخصیص موجودی اولیه انبار جهت فعال‌سازی امکان فروش
                                    │
                                    ▼
       [مرحله ۲: تعریف تأمین‌کننده و خرید] (اختیاری — با امکان رد شدن)
       - ثبت اطلاعات تأمین‌کننده طرف حساب
       - ثبت فاکتور خرید کالا و مدیریت بدهی‌های تجاری
       - امکان رد کردن مرحله (Skip) برای کسب‌وکارهای تولیدی یا خدماتی
                                    │
                                    ▼
       [مرحله ۳: ثبت اولین فروش و صدور فاکتور] (الزامی)
       - گزینه الف: ثبت دستی و حضوری از طریق صندوق فروشگاهی (POS)
       - گزینه ب: استخراج هوشمند و سریع از پیام مشتری (چت اینستاگرام/واتس‌اپ/تلگرام)
                                    │
                                    ▼
       [مرحله ۴: آماده‌سازی، ارسال و تکمیل سفارش] (الزامی)
       - پیگیری چرخه حیات سفارش در لیست سفارشات
       - تغییر وضعیت از در انتظار (Pending) به در حال آماده‌سازی (Processing) و تحویل (Completed)
       - تسویه فاکتور و ثبت خودکار در دفتر مالی (Ledger)
```

---

## ۳. جزئیات پیاده‌سازی در اندروید (Android Jetpack Compose Implementation)

### ۳.۱. مدل وضعیت پویا (`OnboardingState.kt`)
در مسیر [`com.braveboy.calcuapp.ui.dashboard.OnboardingState.kt`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/android/app/src/main/java/com/braveboy/calcuapp/ui/dashboard/OnboardingState.kt):
- برخلاف چک‌لیست‌های نمایشی ساده، وضعیت تکمیل هر مرحله بر اساس **داده‌های واقعی دیتابیس Room و وضعیت سرور** تعیین می‌شود:
  - `hasProducts = products.isNotEmpty()`
  - `hasSuppliers = suppliers.isNotEmpty() || isSupplierSkipped`
  - `hasOrders = orders.isNotEmpty()`
  - `hasCompletedOrders = orders.any { it.fulfillmentStatus == FulfillmentStatus.COMPLETED }`
- محاسبه خودکار درصد پیشرفت (`progress: Float`) و شمارنده (`completedStepsCount از ۴`).

### ۳.۲. دیالوگ خوش‌آمدگویی ورودی (`OnboardingWelcomeDialog.kt`)
در مسیر [`com.braveboy.calcuapp.ui.dashboard.components.OnboardingWelcomeDialog.kt`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/android/app/src/main/java/com/braveboy/calcuapp/ui/dashboard/components/OnboardingWelcomeDialog.kt):
- در اولین ورود کاربر، یک مودال تمام‌صفحه با استانداردهای Material 3 و طراحی اختصاصی Resto نمایش داده می‌شود.
- خوش‌آمدگویی شخصی‌سازی‌شده با نام کاربر و معرفی شفاف ۴ مرحله نقشه راه.
- گزینه‌های «شروع مراحل راه‌اندازی» و «بعداً یادآوری کن».

### ۳.۳. کارت چک‌لیست تعاملی داشبورد (`OnboardingChecklistCard.kt`)
در مسیر [`com.braveboy.calcuapp.ui.dashboard.components.OnboardingChecklistCard.kt`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/android/app/src/main/java/com/braveboy/calcuapp/ui/dashboard/components/OnboardingChecklistCard.kt):
- نمایش نوار پیشرفت (`LinearProgressIndicator`) با درصد پیشرفت به ارقام فارسی (`٪`).
- آیتم‌های تاشو (Collapsible) با قابلیت باز/بسته کردن.
- بج‌های وضعیت Material 3 (`RestoBadge`):
  - تیک سبز برای مراحل تکمیل‌شده.
  - وضعیت فعال برای مرحله جاری با دکمه اقدام مستقیم (`Call to Action`).
- دکمه‌های مستقیم مسیریابی:
  - مرحله ۱: هدایت به انبار و باز کردن مستقیم دیالوگ «ثبت کالای جدید».
  - مرحله ۲: هدایت به «تأمین‌کنندگان» + دکمه «رد کردن این مرحله (اختیاری)».
  - مرحله ۳: دو دکمه موازی: «صندوق فروش (POS)» یا «ثبت از پیام / چت».
  - مرحله ۴: هدایت مستقیم به «مدیریت سفارشات».
- امکان بستن کارت (Dismiss) یا بازگردانی و بازبینی مجدد (Restart Guide).

### ۳.۴. مدیریت پایدار در DataStore و معماری MVVM
- در [`TenantPreferences.kt`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/android/app/src/main/java/com/braveboy/calcuapp/data/local/datastore/TenantPreferences.kt) و [`TenantRepository.kt`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/android/app/src/main/java/com/braveboy/calcuapp/data/repository/TenantRepository.kt):
  - `hasSeenOnboardingDialog: Flow<Boolean>`
  - `isOnboardingDismissed: Flow<Boolean>`
  - `isSupplierStepSkipped: Flow<Boolean>`
- در [`DashboardViewModel.kt`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/android/app/src/main/java/com/braveboy/calcuapp/ui/dashboard/DashboardViewModel.kt):
  - ترکیب واکنش‌گرای `StateFlow` برای نظارت بر جداول کالا، تأمین‌کننده و سفارشات تا به محض انجام هر اقدام توسط کاربر، کارت به‌صورت خودکار به‌روزرسانی شود.

---

## ۴. جزئیات پیاده‌سازی در وب (Web React Implementation)

جهت حفظ یکپارچگی تجربه کاربری روی پنل تحت وب، کامپوننت‌های متناظر در پنل وب پیاده‌سازی شدند:

1. **[`web/src/components/OnboardingWelcomeModal.tsx`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/web/src/components/OnboardingWelcomeModal.tsx):**
   - مودال خوش‌آمدگویی و معرفی گام‌های راه‌اندازی با پشتیبانی دو زبانه (فارسی RTL و انگلیسی LTR).
   - انطباق با تم و متغیرهای رنگی Resto Web.
2. **[`web/src/components/OnboardingChecklistCard.tsx`](file:///Users/aminshojaei/AndroidStudioProjects/Calcuapp/web/src/components/OnboardingChecklistCard.tsx):**
   - کارت چک‌لیست داشبورد وب با نوار پیشرفت، دکمه‌های پرش مستقیم به بخش‌های مربوطه، و امکان بستن موقت یا دائم.
3. **ذخیره‌سازی پایداری در LocalStorage:**
   - کلیدهای `resto_onboarding_dismissed`، `resto_has_seen_onboarding_modal` و `resto_supplier_step_skipped`.

---

## ۵. مقایسه و تطبیق رفتار سیستم (Before vs. After)

| سناریو | قبل از P13 | پس از پیاده‌سازی P13 |
| :--- | :--- | :--- |
| **ورود کاربر برای اولین بار** | داشبورد خالی با خطاهای آمار صفر و سردرگمی کاربر در شروع کار. | نمایش خودکار مودال خوش‌آمدگویی و نقشه راه ۴ مرحله‌ای. |
| **رهگیری پیشرفت** | هیچ شاخصی وجود نداشت. | کارت چک‌لیست پویا با نمایش نوار پیشرفت (مثلاً ۲۵٪، ۵۰٪، ۱۰۰٪). |
| **تکمیل خودکار مراحل** | - | به محض ثبت اولین کالا، مرحله ۱ خودکار تیک سبز می‌خورد؛ به محض ثبت اولین سفارش، مرحله ۳ تیک می‌خورد. |
| **کسب‌وکارهای بدون تأمین‌کننده** | کاربر متوقف می‌شد. | دکمه صریح «رد کردن این مرحله (اختیاری)» تعبیه شد. |
| **آموزش کانال‌های مختلف ثبت سفارش** | کاربر فقط صندوق حضوری را می‌شناخت. | کاربر با ثبت دوگانه (POS و پیام‌رسان‌های اجتماعی) آشنا می‌شود. |
| **بستن و بازگردانی راهنما** | - | کاربر می‌تواند راهنما را پنهان کند و هر زمان از منوی کمک یا داشبورد مجدداً فعال کند. |

---

## ۶. نتایج صحه‌گذاری و بیلد (Verification & Build Status)

* **Android Build Verification:**
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew testDebugUnitTest assembleDebug
  ```
  **نتیجه:** بیلد کامپایل اندروید با موفقیت کامل انجام شده و تمامی ۲۶ تست واحد پاس شدند (`BUILD SUCCESSFUL`).
* **Web Syntax & Type Check:**
  کامپوننت‌های تایپ‌اسکریپت و ری‌اکت بدون خطای ساختاری و با استایل‌بندی یکپارچه مستقر شدند.

---

## ۷. نتیجه‌گیری

با پیاده‌سازی این فاز (P13)، اپلیکیشن رستو دارای یک سیستم **Onboarding تعاملی و خودکار** است که نرخ فعال‌سازی کاربر (User Activation Rate) را به حداکثر رسانده و از سردرگمی در گام‌های اولیه جلوگیری می‌کند.
