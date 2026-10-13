# گزارش نهایی فاز P14.8 — تطبیق هوشمند ستون‌ها و نرمال‌سازی مقادیر در مرکز درون‌ریزی Resto
**Resto P14.8 — Smart Mapping & Value Normalization Report**

---

## ۱. وضعیت نهایی و جمع‌بندی حاکمیتی (Executive Verdict)
- **وضعیت فاز**: `READY` (آماده تایید و استقرار)
- **شرح کلی**: در فاز P14.8، قابلیت تطبیق هوشمند، قطعی (Deterministic) و توضیح‌پذیر (Explainable) ستون‌ها به همراه نرمال‌سازی پیشرفته مقادیر و عبارات برای تمام ۷ نوع داده مرکز درون‌ریزی (`products`, `customers`, `inventory`, `expenses`, `orders`, `suppliers`, `purchases`) پیاده‌سازی شد. سیستم با دسته‌بندی دقیق ۵‌گانه تطبیق، محافظت کامل در برابر ابهام، مدیریت تداخل رقابت ستون‌ها، شناسایی فیلدهای ضروری متصل‌نشده، و جلوگیری از انتساب تکراری ستون‌های فایل، استانداردهای یکپارچگی داده و تجربه کاربری را تکمیل نمود.
- **تعداد تست‌های بک‌اند پاس‌شده**: ۲۱۰ تست با ۱۴۰۴ assertion (شامل ۱۱ تست جامع و مستقل در `SmartMappingTest.php` با ۲۷۸ assertion و ۰ پس‌رفت).
- **تعداد تست‌های وب پاس‌شده**: ۵۲ تست با موفقیت کامل در Jest / React Testing Library.
- **وضعیت بیلد فرانت‌اند**: بیلد بهینه‌شده پروداکشن (`npm run build` شامل پنل‌های business و admin) با موفقیت کامل و بدون خطای تایپ یا لینتینگ ایجاد شد.
- **حفظ یکپارچگی دامنه‌ای**: قراردادهای `HISTORICAL_RECORD_ONLY`، ایزولاسیون کامل چندمستأجری (`Tenant Isolation`)، عدم تغییر در محاسبات مالی و انبار زنده گذشته، و دقت ریاضی اعشاری بدون تغییر و کاملاً محفوظ باقی ماندند.

---

## ۲. معماری الگوریتم تطبیق هوشمند (Smart Mapping Architecture)

### ۲.۱. سطوح تطبیق و ضرایب اطمینان (Match Categories & Confidence Scores)
الگوریتم تطبیق در `ImportMappingService` بر اساس معیارهای قطعی و شفاف پیاده‌سازی شده و از هرگونه نگاشت تصادفی یا حدسی خودداری می‌کند:

| ردیف | دسته‌بندی تطبیق (`match_category`) | ضریب اطمینان (`confidence`) | شرایط انطباق | نمونه تطبیق |
| :---: | :---: | :---: | :---: | :---: |
| ۱ | `exact_canonical` | `1.0` (۱۰۰٪) | تطبیق مستقیم هدر با نام فیلد سیستم، یا عنوان فارسی یا انگلیسی رسمی فیلد | `sku` -> `sku`، `نام کالا` -> `name` |
| ۲ | `exact_alias` | `0.95` (۹۵٪) | تطبیق مستقیم با یکی از الیاس‌ها و مترادف‌های استاندارد ثبت‌شده | `کد فنی` -> `sku`، `فی فروش` -> `price` |
| ۳ | `normalized_alias` | `0.90` (۹۰٪) | تطبیق پس از نرمال‌سازی حروفی، ارقام فارسی/عربی، حذف اعراب/حرکات، نیم‌فاصله یا اتصال کلمات مرکب | `قِیمَت` -> `price`، `كُد كالا` -> `sku`، `کدکالا` -> `sku` |
| ۴ | `fuzzy_high` | `0.80` (۸۰٪) | تطبیق با پیشوند/پسوند معتبر (طول کاراکتر $\ge 4$) یا شباهت متنی بالای ۸۵٪ | `توضیحات تکمیلی` -> `description` |
| ۵ | `ambiguous` | `null` | شناسایی ابهام: زمانی که یک ستون با بیش از یک فیلد مقصد امتیاز یکسان کسب کند | هدرهای چندمنظوره یا مشترک |
| ۶ | `conflict` | `null` | تداخل: زمانی که چند ستون منبع برای یک فیلد مقصد رقابت کنند و برتری قاطع وجود نداشته باشد | دو ستون با عنوان `کد کالا` و `کد محصول` |
| ۷ | `unmapped` | `null` | عدم یافتن هیچ فیلد مقصدی با حداقل اطمینان تعیین‌شده | ستون‌های متفرقه یا فاقد عنوان مرتبط |

---

### ۲.۲. محافظت در برابر ابهام (Ambiguity Safeguard)
اگر یک ستون منبع همزمان با دو یا چند فیلد مقصد انطباق با بالاترین امتیاز کسب کند:
- مقدار `target_field` برابر با `null` قرار می‌گیرد.
- ویژگی `is_ambiguous = true` تنظیم می‌شود.
- دسته‌بندی `match_category = 'ambiguous'` ثبت می‌شود.
- لیست کامل فیلدهای کاندید در `candidate_fields` و `ambiguous_headers` به کاربر ارائه می‌شود.
- سیستم از اتصال خودکار و حدسی خودداری کرده و تصمیم‌گیری نهایی را به کاربر واگذار می‌کند.

---

### ۲.۳. مدیریت رقابت ستون‌ها و تداخل (Conflict Resolution Across Competing Columns)
چنانچه چند ستون منبع برای اتصال به یک فیلد مقصد کاندید شوند:
1. **برتری قطعی اطمینان (`Dominance`)**: اگر یک ستون ضریب اطمینان اکیداً بالاتری نسبت به بقیه داشته باشد (مانند `exact_canonical` با امتیاز 1.0 در برابر `exact_alias` با امتیاز 0.95)، ستون برتر متصل شده و سایر ستون‌های رقیب با نشان `has_conflict = true` و دسته‌بندی `conflict` بدون اتصال باقی می‌مانند.
2. **برابری امتیاز (`Tied Competition`)**: در صورت برابری امتیاز دو یا چند ستون منبع برای یک فیلد مقصد، هیچ‌کدام به‌صورت خاموش برنده اعلام نمی‌شوند؛ هر دو ستون بدون اتصال باقی مانده، `has_conflict = true` دریافت کرده و نام ستون‌های رقیب در `competing_columns` و بنر `conflicts` ثبت می‌گردد.

---

### ۲.۴. فهرست فیلدهای ضروری متصل‌نشده (`unmapped_required_fields`)
در خروجی سرویس و کنترلر، فیلدهایی که در دامنه مربوطه الزامی (`required: true`) هستند ولی به هیچ ستون منبعی متصل نشده‌اند، در آرایه `unmapped_required_fields` بازگردانده شده و در رابط کاربری به‌صورت بنر هشدار نمایش داده می‌شوند.

---

## ۳. ماتریس تطبیق در تمام ۷ دامنه مرکز درون‌ریزی

| دامنه | فیلدهای الزامی | فیلدهای اختیاری | نمونه الیاس‌های پشتیبانی‌شده |
| :---: | :---: | :---: | :---: |
| `products` | `name`, `sku`, `price` | `cost_price`, `unit`, `barcode`, `category`, `description` | نام کالا، کد محصول، قیمت فروش، بهای تمام شده، واحد سنجش، بارکد، دسته‌بندی، توضیحات |
| `customers` | `name` | `phone`, `email`, `address` | نام مشتری، شماره همراه/موبایل، رایانامه، آدرس کامل |
| `inventory` | `sku`, `quantity` | `warehouse`, `barcode`, `unit_cost`, `notes` | کد کالا، موجودی اولیه، انبار/کد انبار، بارکد، قیمت خرید، یادداشت |
| `expenses` | `title`, `category`, `amount`, `date` | `store`, `payment_method`, `notes` | عنوان هزینه، دسته‌بندی هزینه، مبلغ، تاریخ پرداخت، فروشگاه/مرکز هزینه، روش پرداخت |
| `orders` | `order_id`, `sku`, `quantity`, `unit_price`, `date` | `discount_amount`, `tax_amount`, `total_amount`, `customer`, `store`, `payment_method`, `status`, `notes` | شماره فاکتور، کد کالا، تعداد، قیمت واحد، تاریخ سفارش، تخفیف، مالیات، نام مشتری |
| `suppliers` | `name` | `code`, `phone`, `email`, `address`, `tax_id`, `notes` | نام تأمین‌کننده، کد فروشنده، شماره تماس، ایمیل، نشانی، شناسه ملی/کد اقتصادی |
| `purchases` | `purchase_id`, `sku`, `quantity`, `unit_cost`, `date` | `supplier`, `warehouse`, `store`, `discount_amount`, `tax_amount`, `total_amount`, `status`, `payment_status`, `notes` | شناسه خرید، کد کالا، تعداد خرید، قیمت واحد خرید، تاریخ خرید، نام تأمین‌کننده، انبار مقصد |

> [!NOTE]
> در این فاز، تداخل الیاس `کد انبار` که در فازهای قبل به‌صورت اشتباه در الیاس‌های `sku` در دامنه inventory و purchases قرار داشت حذف شد تا از ابهام ساختگی با فیلد `warehouse` جلوگیری شود. همچنین تکرار الیاس `شرح کالا` میان `name` و `description` کالاها تفکیک شد.

---

## ۴. نرمال‌سازی داده‌ها و عبارات (`DataNormalizer`)
سرویس‌های `DataNormalizer` و متدهای نرمال‌سازی `ImportMappingService` به موارد زیر مجهز شدند:
- تبدیل کلیه کاراکترهای عربی (`ي` به `ی`، `ك` به `ک`، `ة` به `ه`، `أ/إ/آ/ٱ` به `ا`، `ؤ` به `و`).
- حذف اعراب و حرکات عربی و فارسی (`فتحه، ضمه، کسره، تنوین‌ها، تشدید، سکون` در محدوده یونی‌کد `\x{064B}-\x{065F}\x{0670}`).
- تبدیل ارقام فارسی (`۰-۹`) و عربی (`٠-٩`) به ارقام استاندارد انگلیسی (`0-9`).
- تبدیل نیم‌فاصله استاندارد (`\u{200C}`) و کشیده (`ـ`) به فاصله و نرمال‌سازی فواصل پی‌درپی.
- حذف علائم نگارشی و نویسه‌های متفرقه (`_`, `-`, `(`, `)`, `:`, `/`, `*`, `#`).
- قابلیت انطباق کلمات مرکب بدون فاصله (`کدکالا` $\leftrightarrow$ `کد کالا`).

---

## ۵. بهبودهای تجربه کاربری فرانت‌اند (`Web UI/UX`)

1. **تمایز تطبیق‌های پیشنهادی خودکار از انتخاب‌های تاییدشده کاربر**:
   - پیشنهادات اولیه با نشان‌های رنگی دسته‌بندی (`تطبیق قطعی ۱۰۰٪` سبز، `مترادف دقیق ۹۵٪` فیروزه‌ای، `مترادف استاندارد ۹۰٪` آبی، `شباهت بالا ۸۰٪` کهربایی) نمایش داده می‌شوند.
   - به محض تغییر مقدار توسط کاربر یا کلیک روی دکمه تایید، نشان سبز اختصاصی `✓ تایید کاربر` روی ردیف ظاهر می‌شود.
2. **نمایش شفاف دلایل تطبیق (`Explainability`)**:
   - در کنار نشان هر ستون، توضیح متنی فارسی یا انگلیسی (`reason_fa` / `reason_en`) و نام الیاس منطبق‌شده نمایش داده می‌شود.
3. **جلوگیری از انتساب تکراری ستون‌های منبع (`Duplicate Assignment Prevention`)**:
   - در صورت انتخاب یک ستون منبع برای بیش از یک فیلد مقصد، بنر خطای قرمز در بالای جدول ظاهر شده، حاشیه دراپ‌داون‌های مربوطه قرمز شده و پیام هشدار خطای تکرار نمایش می‌یابد.
   - دکمه ذخیره و اعتبارسنجی تا زمان رفع تکرار غیرفعال می‌شود.
4. **دکمه‌های اقدام گروهی (`Bulk Actions`)**:
   - دکمه `✓ تایید همه پیشنهادها`: تمام پیشنهادات معتبر را یکجا اعمال و تایید می‌کند.
   - دکمه `✕ پاک کردن همه`: تمام تطبیق‌ها را خالی می‌کند تا کاربر از ابتدا به صورت دستی انتخاب نماید.
5. **بنرهای اطلاع‌رسانی تداخل و ابهام**:
   - نمایش لیست ستون‌های دارای تداخل و نحوه مدیریت آن‌ها.
   - نمایش ستون‌های دارای ابهام با ذکر فیلدهای کاندید.
   - نمایش لیست فیلدهای ضروری که هنوز متصل نشده‌اند.

---

## ۶. نتایج اعتبارسنجی و تست‌ها (Verification Results)

### ۶.۱. تست‌های فیچر بک‌اند (`SmartMappingTest.php`)
```
PASS  Tests\Feature\SmartMappingTest
✓ exact canonical matching across all seven import types               0.14s
✓ exact alias matching persian and english                             0.02s
✓ normalized matching handles diacritics arabic chars and zwnj         0.02s
✓ fuzzy high matching                                                  0.01s
✓ ambiguous header matching multiple fields is left unmapped           0.01s
✓ competing source columns with equal confidence flags conflict and leaves unmapped 0.01s
✓ competing source columns with dominant column assigns winner and flags loser 0.02s
✓ unmapped required fields list                                        0.01s
✓ unmapped unknown header is flagged cleanly                           0.01s
✓ get mapping api endpoint returns rich smart mapping metadata         0.04s
✓ tenant isolation on mapping endpoint                                 0.02s

Tests: 11 passed (278 assertions)
```

### ۶.۲. آزمون کامل سوئیت تست بک‌اند
```
Tests:    210 passed (1404 assertions)
Duration: 6.84s
```

### ۶.۳. آزمون کامل سوئیت تست وب
```
Test Suites: 2 passed, 2 total
Tests:       52 passed, 52 total
Duration:    2.10s
```

### ۶.۴. بیلد پروداکشن وب
```
Creating an optimized production build...
Compiled successfully.
File sizes after gzip:
  119.98 kB  business/static/js/main.a77957a3.js
  65.79 kB   admin/static/js/main.b16e2b0a.js
```

---

## ۷. فهرست فایل‌های تغییریافته و ایجادشده

### ۷.۱. بک‌اند (Backend)
- `backend/app/Services/Import/Mapping/ImportMappingService.php`
- `backend/app/Http/Controllers/Api/Import/ImportJobController.php`
- `backend/tests/Feature/SmartMappingTest.php`

### ۷.۲. مستندات قرارداد API (OpenAPI)
- `docs/api/openapi.yaml`

### ۷.۳. فرانت‌اند (Web)
- `web/src/types/index.ts`
- `web/src/pages/ImportCenterPage.tsx`
- `web/src/pages/ImportCenterPage.test.tsx`

### ۷.۴. گزارش نهایی (Report)
- `docs/P14_8_SMART_MAPPING_REPORT.md`

---

## ۸. دستورات استیجینگ گیت (Git Staging)
تمامی فایل‌های فوق با استفاده از دستورات صریح `git add` در شاخه کاری استیج شدند. طبق مقررات توسعه، هیچ کامیتی ثبت نشده و فاز بعدی (P14.9) شروع نشده است.
