# گزارش ممیزی نهایی و اعتبارسنجی جامع P14.2 — Products & Customers Import

**تاریخ ممیزی:** ۲۱ مهر ۱۴۰۵ (12 اکتبر 2026)  
**شاخه فعال مخزن:** `Products-Customers-Import`  
**وضعیت نهایی:** **READY**  

---

## ۱. یافته‌های مبتنی بر کد واقعی (Actual Source Code Findings)

در این ممیزی نهایی، کدهای منبع، قیود پایگاه‌داده، تراکنش‌ها، رفتار صف، سیاست‌های احراز هویت و دسترسی و سناریوهای مرزی بررسی شدند:

### ۱. ایزولاسیون و راهبرد رفع تکرار (`ImportExecutionService`)
- **راهبرد `skip`**: بررسی مستقیم متد `executeProducts` و `executeCustomers` نشان داد که در صورت وجود رکورد مشابه (`$existing`), تنها شمارنده `$skippedRows++` افزایش یافته و دستور `continue` اجرا می‌گردد. هیچ کوئری یا متد `update` یا تغییری بر روی رکورد موجود صورت نمی‌گیرد.
- **راهبرد `update`**: در حالت بروزرسانی، تنها فیلدهای مجاز کاتالوگ (`name`, `price`, `cost_price`, `unit`, `barcode`, `category`, `description` در کالاها و `name`, `phone`, `email`, `address` در مشتریان) به روز می‌شوند. شناسه سازمانی (`organization_id`) به هیچ عنوان از طریق داده‌های ورودی بازنویسی نشده و همواره مقید به مستأجر فعال (`$job->organization_id`) است.
- **تضمین سلامت انبار و اسناد مالی**:
  - عملیات درون‌ریزی کالاها صرفاً بر روی جدول `products` متمرکز است.
  - هیچ رکوردی در جداول موجودی (`warehouse_stocks`)، حرکات انبار (`stock_movements`)، فاکتورها (`orders`, `order_items`) یا دفترکل حسابداری (`ledger_entries`) ایجاد یا ویرایش نمی‌شود. این موضوع با کوئری مستقیم در تست‌های رگرسیون تایید شد.
- **ایزولاسیون چندمستأجری در مشتریان**:
  - جستجو و تطبیق مشتریان (`Customer::where('organization_id', $job->organization_id)`) کاملاً محدود به سازمان جاری است و مشتریان سازمان‌های دیگر غیرقابل دسترسی و نامرئی هستند.

### ۲. حفاظت در برابر اعتبارسنجی منقضی و اجرای تکراری (`ImportJobController` & `ImportStateMachine`)
- **ابطال خودکار اعتبارسنجی قبلی با تغییر نگاشت**:
  - در متد `saveMapping`، هنگام ذخیره نگاشت جدید، کلیه رکوردهای اعتبارسنجی قبلی در جدول `import_validation_results` حذف شده، پیش‌نمایش قبلی پاکسازی و شمارنده‌های `valid_rows`، `invalid_rows` و `processed_rows` به صفر ریست می‌شوند و وضعیت عملیات به `ready_to_validate` تغییر می‌یابد. بدین ترتیب امکان استفاده از نتایج نامعتبر پس از تغییر فایل یا نگاشت وجود ندارد.
- **جلوگیری از اجرای مجدد عملیات تکمیل‌شده (Idempotent Execution Guard)**:
  - متد `executeJob` صرفاً اجازه اجرای عملیات‌هایی را می‌دهد که در وضعیت `preview_ready` یا `ready_to_import` باشند. اگر درخواستی برای عملیات `completed` یا `failed` ارسال شود، با خطای ۴۲۲ (`InvalidArgumentException`) رد می‌گردد.

### ۳. امنیت و بهینه‌سازی پردازش فایل‌ها (`SpreadsheetReaderService`)
- پردازش فایل‌های XLSX با `XMLReader` سخت‌سازی شد؛ پرچم‌های امنیتی `LIBXML_NONET | LIBXML_NOWARNING | LIBXML_NOERROR` اعمال شدند تا از هرگونه بارگذاری موجودیت‌های خارجی (XXE) جلوگیری شود.
- مقادیر سلول‌ها بدون ارزیابی فرمول‌ها خوانده می‌شوند.
- خواندن خط به خط جریانی (Streaming) حافظه مصرفی را در حد مجاز و ثابت نگه می‌دارد.

---

## ۲. تست‌های رگرسیون اضافه‌شده و نتایج دقیق

تست‌های رگرسیون زیر در فایل `backend/tests/Feature/ProductsCustomersImportTest.php` اضافه و راستی‌آزمایی شدند:
1. `test_saving_new_mapping_invalidates_stale_validation_and_preview_results`: بررسی ریست شدن شمارنده‌ها و حذف نتایج اعتبارسنجی و پیش‌نمایش در صورت تغییر پیکربندی نگاشت.
2. `test_repeated_execution_request_on_completed_job_is_rejected`: تایید رد درخواست‌های تکراری اجرای درون‌ریزی پس از پایان عملیات.
3. `test_update_strategy_preserves_tenant_ownership_and_unmapped_fields`: اطمینان از عدم تغییر `organization_id` و فیلدهای نگاشت‌نشده حین بروزرسانی.

### نتایج تست‌های بک‌اند (Backend Tests):
```bash
php artisan test tests/Feature/ProductsCustomersImportTest.php
PASS Tests\Feature\ProductsCustomersImportTest
  ✓ normalizer handles persian digits money phones and units             0.20s  
  ✓ mapping service suggests canonical persian and english aliases       0.02s  
  ✓ mapping validation enforces required fields and prevents conflicts   0.02s  
  ✓ api mapping get and save                                             0.06s  
  ✓ product validation pipeline detects errors duplicates and matches    0.02s  
  ✓ product execution with skip and update modes and zero inventory alt… 0.02s  
  ✓ customer validation and execution                                    0.05s  
  ✓ tenant isolation prevents foreign data leak or matching              0.02s  
  ✓ saving new mapping invalidates stale validation and preview results  0.02s  
  ✓ repeated execution request on completed job is rejected              0.02s  
  ✓ update strategy preserves tenant ownership and unmapped fields       0.02s  

Tests: 11 passed (92 assertions)
Duration: 0.51s
```

```bash
php artisan test
Tests: 146 passed (679 assertions)
Duration: 5.50s
```

### نتایج تست‌های وب (Frontend Tests):
```bash
npm test -- --watchAll=false
Test Suites: 2 passed, 2 total
Tests:       47 passed, 47 total
Snapshots:   0 total
Time:        2.211 s
```

---

## ۳. بررسی گردش کار کاربر (User Workflow Verification)

- رابط کاربری پیاده‌سازی‌شده در `ImportCenterPage.tsx` قبل از ارسال درخواست اجرا، صریحاً نیازمند تایید کاربر بر روی داده‌های پیش‌نمایش و انتخاب سیاست رفع تکرار (`skip` یا `update`) است.
- پیش‌نمایش، خطاها، هشدارها و تعداد ردیف‌ها مستقیماً بر اساس پاسخ‌های زنده سرور (`GET /api/v1/import/jobs/{id}/preview` و `GET /api/v1/import/jobs/{id}/errors`) رندر می‌شوند.

---

## ۴. ممیزی گیت و کنترل نسخه‌ها (Git / VCS Audit)

### خروجی `git diff --cached --check`:
خروجی پاک است (بدون خطا یا فاصله‌های خالی نامناسب).

### فایل‌های در مرحله Staging (`git diff --cached --name-status`):
```
M	backend/app/Http/Controllers/Api/Import/ImportJobController.php
M	backend/app/Services/Import/Reader/SpreadsheetReaderService.php
M	backend/tests/Feature/ProductsCustomersImportTest.php
```
*(به همراه کامیت `3f0b7898` که تغییرات اولیه فاز را ثبت کرده بود)*

### مسیرها و فایل‌های Untracked و وضعیت آن‌ها:
- `.freebuff/`: فایل‌های موقت داخلی ابزار
- `backend/public/build/`: خروجی‌های موقت بیلد عمومی
- `backend/storage/framework/testing/`: فایل‌های فریم‌ورک تست لاراول
- `backend/vendor/predis/`: پکیج مدیریت کش ردیس
- `web/build/admin/` & `web/build/business/`: خروجی‌های بیلد فرانت‌اند
- `web/node_modules/`: وابستگی‌های جاوااسکریپت و کش بیبل

هیچ‌کدام از موارد Untracked فوق اطلاعات محرمانه (راز، کلید یا پسورد) نبوده و جزو فایل‌های بیلد، کش یا وابستگی‌ها هستند که نباید به VCS افزوده شوند.

---

## ۵. محدودیت‌های باقی‌مانده و مرزهای فاز (Remaining Limitations)

- **دامنه مجاز فاز**: درون‌ریزی انبار، هزینه‌ها، سوابق تاریخی فاکتورها، تأمین‌کنندگان، خریدها و اسناد حسابداری طبق ماموریت پروژه برای فازهای آتی (P14.3 و بعد) رزرو شده و در این فاز دست‌نخورده باقی مانده است.
- **پردازش بدافزار و فایل‌های اجرایی**: فایل‌های اسکریپت و ماکروهای VBA اکسل بر اساس قوانین پسوند و چک‌سام رد می‌شوند، ولی مخزن ادعای ضدبدافزار نامحدود برای تمامی انواع محتوای باینری ندارد.

---

## ۶. وضعیت نهایی آمادگی (Final Readiness Status)

### **READY**
تمامی نیازمندی‌های یکپارچگی داده، امنیت چندمستأجری، ایزولاسیون انبار، رفع تکرار قطعی و پایداری اعتبارسنجی فاز **P14.2** با تست‌های خودکار اثبات و تایید شدند.
