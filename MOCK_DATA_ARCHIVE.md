# MOCK DATA ARCHIVE & RESTORATION GUIDE
## آرشیو داده‌های ماک و راهنمای بازگردانی در پروژه Calcuapp

این فایل شامل تمامی داده‌های ماک اولیه پروژه و دستورالعمل‌های نحوه بازگردانی (Re-seed) آن در لایه‌های مختلف پروژه (Backend، Android، Web، iOS) می‌باشد.

---

## ۱. نحوه استفاده و بازگردانی داده‌های ماک (Restoration Guide)

### الف) بازگردانی داده‌های ماک در بک‌اند (Laravel Backend)
برای اعمال مجدد داده‌های ماک در دیتابیس SQLite بک‌اند، از دستور زیر در ترمینال پوشه `backend` استفاده کنید:

```bash
cd backend
php artisan db:seed --class=MockDataSeeder
```

در صورتی که می‌خواهید ابتدا دیتابیس را پاک و سپس داده‌های ماک را اضافه کنید:

```bash
cd backend
php artisan migrate:fresh --seed --seeder=MockDataSeeder
```

### ب) بازگردانی داده‌های ماک در اپلیکیشن اندروید (Android Jetpack Compose)
در سورس‌کد اندروید، فایل زیر به عنوان مرجع آرشیو ساخته شده است:
- `android/app/src/main/java/com/braveboy/calcuapp/data/mock/MockDataArchive.kt`

جهت بارگذاری داده‌های ماک در دیتابیس روم (Room DB) می‌توانید متد زیر را صدا بزنید:

```kotlin
import com.braveboy.calcuapp.data.mock.MockDataArchive

// در scope مورد نظر:
MockDataArchive.seedMockDataToRoom(appDatabase)
```

### ج) نحوه اجرا و تست از ابتدا (Clean Testing Mode)
در حالت فعلی، تمامی دیتابیس‌ها و منبع داده‌های پیش‌فرض پاکسازی شده‌اند تا بتوانید تست‌های سیستم را کاملاً از صفر (Empty Database State) اجرا کنید.

---

## ۲. آرشیو کامل داده‌های ماک (Archived Datasets)

### ۱. کاربران سیستم (Users)
| ID | Name | Email | Phone | Role | Password (Plain) |
|---|---|---|---|---|---|
| `usr_admin_1` | Alex Mercer | alex.mercer@calcuapp.com | +1 (555) 019-2834 | Owner | `password123` |
| `usr_cashier_1` | Elena Rostova | elena@calcuapp.com | +1 (555) 019-9988 | Cashier | `password123` |

---

### ۲. سازمان‌ها، شعب و انبارها (Organizations, Stores, Warehouses)

#### سازمان ۱: Apex Retail Group (`org_apex`)
- **کد سازمان**: `APEX`
- **ارز**: USD (`$`)
- **سطح اشتراک**: `ENTERPRISE`
- **شعبه ۱**: Apex Flagship Store (Downtown) (`store_apex_1`) - آدرس: 100 Market St, San Francisco, CA
  - **انبار ۱**: Main Warehouse (`wh_apex_1a`) - کد: `WH-MAIN`
  - **انبار ۲**: Express Storage Hub (`wh_apex_1b`) - کد: `WH-EXP`
- **شعبه ۲**: Apex Express / Outlet Store (`store_apex_2`) - آدرس: 500 Commerce Ave, Oakland, CA
  - **انبار ۱**: Outlet Stockroom (`wh_apex_2a`) - کد: `WH-OUTLET`

#### سازمان ۲: BraveBoy Electronics (`org_braveboy`)
- **کد سازمان**: `BBE`
- **ارز**: USD (`$`)
- **سطح اشتراک**: `PRO`
- **شعبه ۱**: Tech Hub Metro (`store_bb_1`) - آدرس: 742 Evergreen Terrace, Springfield
  - **انبار ۱**: Metro Depot (`wh_bb_1`) - کد: `WH-BB1`

---

### ۳. کالاها و موجودی انبار (Products & Inventory)

```json
[
  {
    "id": "prod_1",
    "org_id": "org_apex",
    "sku": "APX-LAP-001",
    "barcode": "880609123401",
    "name": "ProBook Ultra 15 M3",
    "description": "High-performance laptop featuring 16GB RAM and 512GB SSD.",
    "price": 1299.99,
    "cost_price": 850.00,
    "category": "Electronics",
    "unit": "pcs",
    "stock": {
      "wh_apex_1a": 22,
      "wh_apex_1b": 7
    }
  },
  {
    "id": "prod_2",
    "org_id": "org_apex",
    "sku": "APX-AUD-002",
    "barcode": "880609123402",
    "name": "NoiseCancel Studio Headphones",
    "description": "Active noise cancelling wireless headphones with 30-hour battery life.",
    "price": 249.99,
    "cost_price": 120.00,
    "category": "Audio",
    "unit": "pcs",
    "stock": {
      "wh_apex_1a": 45,
      "wh_apex_1b": 18
    }
  },
  {
    "id": "prod_3",
    "org_id": "org_apex",
    "sku": "APX-DIS-003",
    "barcode": "880609123403",
    "name": "UltraWide 34\" Curved Display",
    "description": "144Hz 4K IPS panel monitor with USB-C Hub.",
    "price": 599.99,
    "cost_price": 380.00,
    "category": "Electronics",
    "unit": "pcs",
    "stock": {
      "wh_apex_1a": 12,
      "wh_apex_1b": 0
    }
  }
]
```

---

### ۴. تامین‌کنندگان و سفارشات خرید (Suppliers & Purchases)

#### تامین‌کنندگان:
1. **TechImport Global Co.** (`sup_1`) - sales@techimport.com - +1 (800) 555-0199
2. **ElectroComponents Inc.** (`sup_2`) - orders@electrocomponents.com - +1 (800) 555-0288

#### سفارش خرید (Purchase Order):
- **ID**: `po_1001`
- **شماره سفارش**: `PO-2025-1001`
- **تامین کننده**: `sup_1` (TechImport Global Co.)
- **شعبه و انبار**: `store_apex_1` / `wh_apex_1a`
- **اقلام**: ۱۰ عدد ProBook Ultra 15 M3 با قیمت واحد $850.00 (مجموع $8,500.00)
- **وضعیت**: `RECEIVED` / پرداخت شده (`PAID`)

---

### ۵. مشتریان و سفارشات فروش (Customers & Sales Orders)

#### مشتریان:
1. **Sarah Connor** (`cust_1`) - sarah.connor@example.com - +1 (555) 234-5678 - خرید: $3,548.50 - امتیاز: 350
2. **John Wick** (`cust_2`) - john.wick@example.com - +1 (555) 999-0000 - خرید: $249.99 - امتیاز: 25

#### سفارش فروش (Sales Order):
- **ID**: `ord_1001`
- **شماره فاکتور**: `ORD-2025-1001`
- **مشتری**: Sarah Connor (`cust_1`)
- **اقلام**: ۱ عدد ProBook Ultra 15 M3
- **جمع کل**: $1,603.78 ($1,549.98 پایه - $65.00 تخفیف + $118.80 مالیات)
- **روش پرداخت**: کارت اعتباری (`CARD`) - تکمیل شده (`COMPLETED`)

---

### ۶. هزینه‌ها (Expenses)
- **ID**: `exp_1001`
- **دسته**: Store Utilities
- **مبلغ**: $450.00
- **روش پرداخت**: `BANK_TRANSFER`
- **توضیحات**: Monthly electricity bill for Apex Flagship

---

### ۷. سرفصل حساب‌ها و دفاتر (Chart of Accounts & Ledger)
- **1000 Assets**:
  - `1010` Cash / POS Drawer (موجودی: $15,000.00)
  - `1200` Inventory Asset (موجودی: $25,000.00)
- **2000 Liabilities**:
  - `2010` Accounts Payable (موجودی: $5,000.00)
- **4000 Revenue**:
  - `4010` Sales Revenue (موجودی: $35,000.00)
- **5000 Expenses**:
  - `5010` Operating Expense (موجودی: $4,500.00)
