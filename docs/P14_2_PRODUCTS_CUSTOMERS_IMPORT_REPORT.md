# گزارش جامع پیاده‌سازی فاز P14.2 — ورود داده‌های کالاها و مشتریان (Products & Customers Import)

**تاریخ اجرا:** ۲۱ مهر ۱۴۰۵ (12 اکتبر 2026)  
**شاخه فعال گیت:** `Products-Customers-Import`  
**وضعیت فاز:** تکمیل کامل، تست‌شده و ثبت در مرحله Staging گیت  

---

## ۱. خلاصه اجرایی و دستاوردها

در فاز **P14.2**، معماری و موتور مهاجرت داده‌های کالاها و مشتریان از فایل‌های اکسل (`.xlsx`) و `CSV` بر روی زیرساخت پایه P14.1 Import Center پیاده‌سازی شد.

کلیه الزامات دامنه کسب‌وکار شامل موارد زیر با رعایت کامل اصول امنیت، ایزولاسیون چندمستأجری (Multi-Tenancy) و عدم دستکاری انبار/سفارشات محقق گردید:
1. **تشخیص خودکار و تطبیق هوشمند ستون‌ها (Column Mapping Engine)**:
   - پشتیبانی از مجموعه‌ای جامع از نام‌ها و نام‌های مستعار فارسی و انگلیسی (`نام کالا`, `قیمت فروش`, `کد محصول / SKU`, `بارکد`, `واحد سنجش`, `دسته بندی`, `تلفن`, `موبایل`, `ایمیل`, `آدرس`).
   - تبدیل دقیق ارقام فارسی/عربی (`۰-۹`) به ارقام استاندارد انگلیسی (`0-9`).
   - شناسایی و اعتبارسنجی واحدهای سنجش بر اساس استاندارد واحد سیستم (`UnitCatalog`).
2. **خواندن جریانی داده‌ها (Streaming Row Reader)**:
   - پردازش بهینه ردیف به ردیف بدون اشغال بی‌رویه حافظه برای فایل‌های CSV و XML ورک‌شیت‌های XLSX (بدون ارزیابی یا اجرای فرمول‌ها برای حفظ امنیت).
3. **موتور اعتبارسنجی ردیفی و فیلدی (Validation Pipeline)**:
   - اعتبارسنجی فیلدهای اجباری (`name`, `sku`, `price` در کالاها؛ `name` در مشتریان).
   - اعتبارسنجی مقادیر پولی و قیمت‌ها به صورت دقیق اعشاری (Decimal).
   - شناسایی تکراری‌ها در درون فایل (`ERR_DUPLICATE_IN_FILE`).
   - شناسایی رکوردهای مشابه از قبل موجود در پایگاه داده مستأجر (`WARN_EXISTING_RECORD_MATCH`).
   - ثبت ریز خطاها و هشدارها در جدول `import_validation_results` با کد خطا و پیام فارسی.
   - تولید نمونه پیش‌نمایش ردیف‌های پردازش‌شده جهت بازبینی کاربر قبل از اعمال.
4. **موتور اجرای نهایی و راهبرد تکراری‌ها (Import Execution Engine)**:
   - پشتیبانی از دو راهبرد: صرف‌نظر کردن (`skip`) و بروزرسانی (`update`).
   - اجرای تراکنشی دسته‌ای (Batch Transactions) با ثبت ردیف‌های درج‌شده، رد‌شده و ناموفق.
   - **قانون عدم دستکاری انبار**: عدم ایجاد یا تغییر موجودی در انبارها (`warehouse_stocks`) یا جداول فروش/سفارشات حین درون‌ریزی کالاها.
5. **رابط کاربری تعاملی (Interactive Multi-Step UI Wizard)**:
   - ویزارد چندمرحله‌ای: انتخاب کاربرگ -> تطبیق ستون‌ها با پیشنهادهای هوشمند -> اعتبارسنجی -> پیش‌نمایش نمونه ردیف‌ها و انتخاب راهبرد تکراری -> تایید و اجرای نهایی -> گزارش آماری نتیجه.
   - تطابق کامل با Design Tokens، تم روز/شب و استاندارد راست‌به‌چپ (RTL).

---

## ۲. ساختار فایل‌های پیاده‌سازی‌شده و تغییر یافته

### الف. فایل‌های افزوده شده (New Backend Services & Tests):
- `backend/app/Services/Import/Normalizer/DataNormalizer.php`: نرمال‌سازی نویسه‌ها، ارقام فارسی/عربی، مقادیر مالی، شماره تماس، ایمیل و واحد سنجش.
- `backend/app/Services/Import/Reader/SpreadsheetReaderService.php`: خواندن جریانی فایل‌های CSV و XLSX با XMLReader و بدون لود یکباره حافظه.
- `backend/app/Services/Import/Mapping/ImportMappingService.php`: تعاریف فیلدهای کالا و مشتری، تطبیق قطعی با نام‌های مستعار فارسی/انگلیسی و اعتبارسنجی نگاشت.
- `backend/app/Services/Import/Validation/ImportValidationService.php`: ارزیابی ردیف‌ها، خطایابی، بررسی تکرار درون فایل و پایگاه داده، و تولید پیش‌نمایش.
- `backend/app/Services/Import/Execution/ImportExecutionService.php`: اجرای تراکنشی درج/بروزرسانی داده‌ها بر اساس راهبرد تکرار بدون دستکاری انبار.
- `backend/tests/Feature/ProductsCustomersImportTest.php`: تست یکپارچه ۸ مرحله‌ای برای تمامی سناریوهای نرمال‌سازی، نگاشت، اعتبارسنجی، اجرا، ایزولاسیون و حفاظت انبار.

### ب. فایل‌های بروزرسانی‌شده (Updated Modules):
- `backend/app/Support/Import/ImportErrorCode.php`: افزودن کدهای خطای دامنه (`INVALID_UNIT`, `INVALID_PHONE`, `INVALID_EMAIL`, `DUPLICATE_IN_FILE`, `EXISTING_RECORD_MATCH`, `AMBIGUOUS_MAPPING`, `NUMERIC_INVALID`, `STRING_TOO_LONG`) به همراه ترجمه فارسی.
- `backend/app/Support/UnitCatalog.php`: پشتیبانی از تطبیق مستقیم برچسب‌های فارسی واحد سنجش در متد `normalize()`.
- `backend/app/Http/Controllers/Api/Import/ImportJobController.php`: افزودن متدهای `getMapping`, `saveMapping`, `validateData`, `preview`, `executeImport`.
- `backend/routes/api.php`: ثبت اندپوینت‌های مربوط به نگاشت، اعتبارسنجی، پیش‌نمایش و اجرای درون‌ریزی تحت دسترسی‌های `import.view` و `import.manage`.
- `docs/api/openapi.yaml`: مستندسازی کامل تمامی اندپوینت‌های P14.2 مطابق با استاندارد OpenAPI 3.0.
- `web/src/types/index.ts`: افزودن اینترفیس‌های تایپ‌اسکریپت برای تطبیق ستون‌ها، پیشنهادها، پیش‌نمایش و نتایج اعتبارسنجی.
- `web/src/api/apiClient.ts`: اضافه کردن متدهای ارتباط با API برای گام‌های ویزارد درون‌ریزی.
- `web/src/pages/ImportCenterPage.tsx`: پیاده‌سازی ویزارد چندمرحله‌ای، جداول تطبیق ستون، پیش‌نمایش ردیف‌ها و گزارش اجرا.

---

## ۳. نتایج اعتبارسنجی و تست‌ها (Verification Results)

### بک‌اند (Laravel / PHPUnit):
- تست ویژگی اختصاصی P14.2:
  ```
  PASS Tests\Feature\ProductsCustomersImportTest
  ✓ normalizer handles persian digits money phones and units
  ✓ mapping service suggests canonical persian and english aliases
  ✓ mapping validation enforces required fields and prevents conflicts
  ✓ api mapping get and save
  ✓ product validation pipeline detects errors duplicates and matches
  ✓ product execution with skip and update modes and zero inventory alteration
  ✓ customer validation and execution
  ✓ tenant isolation prevents foreign data leak or matching

  Tests: 8 passed (75 assertions)
  ```
- اجرای کلیه تست‌های بک‌اند مخزن:
  ```
  Tests: 143 passed (662 assertions)
  Duration: 5.28s
  ```

### فرانت‌اند (React / TypeScript / Jest):
- اجرای تست‌های خودکار وب:
  ```
  PASS src/pages/ImportCenterPage.test.tsx
  PASS src/App.test.tsx
  Tests: 47 passed, 47 total
  Time: 2.665 s
  ```
- کامپایل و بیلد نسخه تولیدی وب:
  ```
  npm run build
  Compiled successfully (business & admin bundles created).
  ```

---

## ۴. وضعیت گیت و مرحله‌بندی تغییرات (VCS Staging Status)

کلیه فایل‌های مربوط به فاز P14.2 با دقت و به صورت تک‌به‌تک و صریح بررسی شده و به ناحیه Staging گیت اضافه شدند (هیچ فایل کش، گزارش، بیلد یا اطلاعات حساس استیج نشده است):

```
Changes to be committed:
  modified:   backend/app/Http/Controllers/Api/Import/ImportJobController.php
  new file:   backend/app/Services/Import/Execution/ImportExecutionService.php
  new file:   backend/app/Services/Import/Mapping/ImportMappingService.php
  new file:   backend/app/Services/Import/Normalizer/DataNormalizer.php
  new file:   backend/app/Services/Import/Reader/SpreadsheetReaderService.php
  new file:   backend/app/Services/Import/Validation/ImportValidationService.php
  modified:   backend/app/Support/Import/ImportErrorCode.php
  modified:   backend/app/Support/UnitCatalog.php
  modified:   backend/routes/api.php
  new file:   backend/tests/Feature/ProductsCustomersImportTest.php
  modified:   docs/api/openapi.yaml
  modified:   web/src/api/apiClient.ts
  modified:   web/src/pages/ImportCenterPage.tsx
  modified:   web/src/types/index.ts
```

*تذکر: بنا بر دستورالعمل، هیچ کامیت یا پوش بدون تایید صریح کاربر انجام نگرفت و شاخه آماده بازبینی می‌باشد.*
