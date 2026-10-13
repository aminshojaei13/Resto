<?php

namespace App\Services\Import\Template;

use App\Support\Import\ImportType;
use InvalidArgumentException;

class ImportTemplateService
{
    /**
     * Get template metadata and schema for a specific import type.
     *
     * @return array{
     *     type: string,
     *     filename: string,
     *     title_fa: string,
     *     title_en: string,
     *     description_fa: string,
     *     description_en: string,
     *     is_multiline: bool,
     *     headers: string[],
     *     field_keys: string[],
     *     required: bool[],
     *     sample_rows: string[][],
     *     domain_notes_fa: string[],
     *     domain_notes_en: string[]
     * }
     */
    public function getTemplate(string $type): array
    {
        return match ($type) {
            ImportType::PRODUCTS => $this->productsTemplate(),
            ImportType::CUSTOMERS => $this->customersTemplate(),
            ImportType::INVENTORY => $this->inventoryTemplate(),
            ImportType::EXPENSES => $this->expensesTemplate(),
            ImportType::ORDERS => $this->ordersTemplate(),
            ImportType::SUPPLIERS => $this->suppliersTemplate(),
            ImportType::PURCHASES => $this->purchasesTemplate(),
            default => throw new InvalidArgumentException("نوع قالب '{$type}' پشتیبانی نمی‌شود."),
        };
    }

    /**
     * Get catalog of all supported templates.
     *
     * @return array<string, array<string, mixed>>
     */
    public function getAllTemplates(): array
    {
        $templates = [];
        foreach (ImportType::ALL as $type) {
            $templates[$type] = $this->getTemplate($type);
        }
        return $templates;
    }

    /**
     * Generate UTF-8 BOM CSV content for downloading.
     */
    public function generateCsv(string $type): string
    {
        $template = $this->getTemplate($type);

        $handle = fopen('php://memory', 'r+');
        if ($handle === false) {
            throw new \RuntimeException('Failed to open memory stream for template generation.');
        }

        // Write header row
        fputcsv($handle, $template['headers']);

        // Write educational sample rows
        foreach ($template['sample_rows'] as $row) {
            fputcsv($handle, $row);
        }

        rewind($handle);
        $csvBody = stream_get_contents($handle);
        fclose($handle);

        // Prepend UTF-8 BOM (\xEF\xBB\xBF) so Excel correctly displays Persian characters
        return "\xEF\xBB\xBF" . $csvBody;
    }

    private function productsTemplate(): array
    {
        return [
            'type' => ImportType::PRODUCTS,
            'filename' => 'resto_template_products.csv',
            'title_fa' => 'قالب کالاها و محصولات',
            'title_en' => 'Products & Catalog Template',
            'description_fa' => 'فهرست محصولات و کالاهای قابل فروش یا مصرفی به همراه کد شناسایی، قیمت فروش و ویژگی‌ها.',
            'description_en' => 'Product catalog with SKU identifiers, sale prices, and optional attributes.',
            'is_multiline' => false,
            'headers' => [
                'نام کالا',
                'کد کالا',
                'قیمت فروش',
                'قیمت خرید',
                'واحد سنجش',
                'بارکد',
                'دسته‌بندی',
                'توضیحات',
            ],
            'field_keys' => [
                'name',
                'sku',
                'price',
                'cost_price',
                'unit',
                'barcode',
                'category',
                'description',
            ],
            'required' => [true, true, true, false, false, false, false, false],
            'sample_rows' => [
                ['قهوه اسپرسو ۱۰۰٪ عربیکا', 'SKU-COF-001', '350000', '220000', 'بسته', '6260123456789', 'دان قهوه', 'دان قهوه روست مدیوم کلمبیا ۲۵۰ گرمی'],
                ['چای سیاه معطر لاهیجان', 'SKU-TEA-002', '180000', '110000', 'بسته', '6260987654321', 'چای و دمنوش', 'چای شکسته ممتاز ۵۰۰ گرمی'],
                ['شربت کارامل خانگی', 'SKU-SYR-003', '95000', '60000', 'شیشه', '', 'سیروپ و طعم‌دهنده', 'بطری شیشه‌ای ۷۵۰ میلی‌لیتر'],
            ],
            'domain_notes_fa' => [
                'فیلدهای «نام کالا»، «کد کالا» و «قیمت فروش» الزامی هستند.',
                'کد کالا (SKU) در سطح سازمان یکتا است و برای پیوند با موجودی و سفارش‌ها استفاده می‌شود.',
                'قیمت‌ها به ریال و بدون کاما یا علامت واحد پول وارد شوند.',
                'ورود کالاها تغییری در موجودی انبار ایجاد نمی‌کند؛ برای ثبت موجودی از قالب موجودی اولیه استفاده فرمایید.',
            ],
            'domain_notes_en' => [
                'Product Name, SKU, and Sale Price are required.',
                'SKU is unique per organization and links with inventory and order lines.',
                'Monetary amounts must be plain numbers without commas or currency symbols.',
                'Product catalog import does NOT mutate warehouse stock; use Opening Inventory for initial counts.',
            ],
        ];
    }

    private function customersTemplate(): array
    {
        return [
            'type' => ImportType::CUSTOMERS,
            'filename' => 'resto_template_customers.csv',
            'title_fa' => 'قالب مشتریان',
            'title_en' => 'Customers Template',
            'description_fa' => 'اطلاعات مشتریان جهت سوابق تماس، تخفیف‌ها و باشگاه مشتریان.',
            'description_en' => 'Customer master data for contact, loyalty, and purchase history.',
            'is_multiline' => false,
            'headers' => [
                'نام مشتری',
                'شماره تماس',
                'ایمیل',
                'نشانی',
            ],
            'field_keys' => [
                'name',
                'phone',
                'email',
                'address',
            ],
            'required' => [true, false, false, false],
            'sample_rows' => [
                ['رضا محمدی', '09121112233', 'reza.m@example.com', 'تهران، خیابان ولیعصر، پلاک ۱۲۳'],
                ['سارا کاظمی', '09359876543', 'sara.k@example.com', 'تهران، سعادت‌آباد، خیابان سرو'],
                ['کافه کتاب اندیشه', '02188776655', 'info@andisheh-book.ir', 'تهران، خیابان انقلاب، نبش فخر رازی'],
            ],
            'domain_notes_fa' => [
                'فیلد «نام مشتری» الزامی است.',
                'شماره تماس و ایمیل برای جلوگیری از ثبت تکراری و ادغام سوابق بررسی می‌شوند.',
                'شماره موبایل به فرمت 09xxxxxxxxx یا فرمت استاندارد محلی وارد شود.',
            ],
            'domain_notes_en' => [
                'Customer Name is required.',
                'Phone and email are checked for duplicate prevention.',
                'Phone format should follow standard mobile/landline formats.',
            ],
        ];
    }

    private function inventoryTemplate(): array
    {
        return [
            'type' => ImportType::INVENTORY,
            'filename' => 'resto_template_inventory.csv',
            'title_fa' => 'قالب موجودی اولیه انبار',
            'title_en' => 'Opening Inventory Template',
            'description_fa' => 'ثبت شمارش فیزیکی اولیه کالاها در انبارهای سازمان جهت آغاز دوره عملیاتی.',
            'description_en' => 'Opening physical stock counts per product and warehouse.',
            'is_multiline' => false,
            'headers' => [
                'کد کالا',
                'موجودی اولیه',
                'انبار',
                'بارکد',
                'بهای تمام شده',
                'توضیحات',
            ],
            'field_keys' => [
                'sku',
                'quantity',
                'warehouse',
                'barcode',
                'unit_cost',
                'notes',
            ],
            'required' => [true, true, false, false, false, false],
            'sample_rows' => [
                ['SKU-COF-001', '50', 'انبار مرکزی', '6260123456789', '220000', 'شمارش انبارگردانی ابتدای دوره'],
                ['SKU-TEA-002', '100', 'انبار مرکزی', '6260987654321', '110000', 'شمارش انبارگردانی ابتدای دوره'],
                ['SKU-SYR-003', '30', 'انبار بار شعبه', '', '60000', 'موجودی اولیه کافه بار'],
            ],
            'domain_notes_fa' => [
                'کد کالا باید با کالاهای قبلاً تعریف‌شده در کاتالوگ سازمان مطابقت داشته باشد.',
                'موجودی اولیه باید عدد صحیح مثبت (بزرگتر از صفر) باشد.',
                'در صورت خالی بودن ستون انبار، از انبار پیش‌فرض انتخاب‌شده در فرم تطبیق استفاده می‌شود.',
                'موجودی اولیه سند افتتاحیه انبار ایجاد می‌کند و بر اسناد فروش و حسابداری جاری اثری ندارد.',
            ],
            'domain_notes_en' => [
                'SKU must match existing products already defined in tenant catalog.',
                'Quantity must be a positive integer greater than zero.',
                'If warehouse column is left empty, the default warehouse selected in mapping is applied.',
                'Creates opening stock ledger entries without mutating operational sales accounting.',
            ],
        ];
    }

    private function expensesTemplate(): array
    {
        return [
            'type' => ImportType::EXPENSES,
            'filename' => 'resto_template_expenses.csv',
            'title_fa' => 'قالب هزینه‌های جاری',
            'title_en' => 'Operating Expenses Template',
            'description_fa' => 'ثبت هزینه‌های عملیاتی جاری کسب‌وکار (اجاره، قبوض، حقوق، تعمیرات، تبلیغات و ...).',
            'description_en' => 'Operating expense records (rent, utilities, payroll, repairs, marketing).',
            'is_multiline' => false,
            'headers' => [
                'عنوان هزینه',
                'دسته‌بندی',
                'مبلغ',
                'تاریخ',
                'فروشگاه',
                'روش پرداخت',
                'یادداشت',
            ],
            'field_keys' => [
                'title',
                'category',
                'amount',
                'date',
                'store',
                'payment_method',
                'notes',
            ],
            'required' => [true, true, true, true, false, false, false],
            'sample_rows' => [
                ['قبض برق دوره مهر ماه', 'برق', '1450000', '1403/07/15', 'شعبه مرکزی', 'CARD', 'پرداخت با کارتخوان شعبه'],
                ['شارژ اینترنت پرسرعت یکساله', 'اینترنت', '3200000', '1403/07/01', 'شعبه مرکزی', 'BANK_TRANSFER', 'حواله آنلاین به حساب شرکت ارائه‌دهنده'],
                ['تعمیر و سرویس دستگاه اسپرسوساز', 'تعمیرات', '2800000', '1403/07/20', '', 'CASH', 'فاکتور خدمات فنی آرمان'],
            ],
            'domain_notes_fa' => [
                'عنوان، دسته‌بندی، مبلغ و تاریخ الزامی هستند.',
                'دسته‌بندی باید یکی از موارد مجاز باشد: اجاره، اینترنت، برق، آب، حمل‌ونقل، تبلیغات، حقوق، تعمیرات، خدمات، کارمزد، سایر.',
                'تاریخ به صورت شمسی (۱۴۰۳/۰۷/۱۵) یا میلادی (2024-10-06) پذیرفته می‌شود.',
                'هزینه‌های ثبت‌شده مستقیماً سند دوبل بدهکار هزینه و بستانکار وجه نقد/بانک را در حسابداری ثبت می‌کنند.',
            ],
            'domain_notes_en' => [
                'Title, Category, Amount, and Date are required.',
                'Category must resolve to: RENT, INTERNET, ELECTRICITY, WATER, TRANSPORT, ADVERTISING, SALARIES, REPAIRS, SERVICES, FEES, OTHER.',
                'Date accepted in Jalali (1403/07/15) or ISO Gregorian (2024-10-06).',
                'Posts balanced double-entry accounting records to general ledger.',
            ],
        ];
    }

    private function ordersTemplate(): array
    {
        return [
            'type' => ImportType::ORDERS,
            'filename' => 'resto_template_historical_orders.csv',
            'title_fa' => 'قالب سوابق سفارش‌ها (تاریخی)',
            'title_en' => 'Historical Orders Template',
            'description_fa' => 'ورود سوابق فاکتورهای فروش گذشته برای تاریخچه مشتریان و گزارش‌های تحلیلی.',
            'description_en' => 'Past customer order history for analytics and CRM historical reporting.',
            'is_multiline' => true,
            'headers' => [
                'شناسه سفارش',
                'کد کالا',
                'تعداد',
                'قیمت واحد',
                'تاریخ',
                'تخفیف',
                'مالیات',
                'مبلغ کل',
                'مشتری',
                'فروشگاه',
                'روش پرداخت',
                'وضعیت',
                'یادداشت',
            ],
            'field_keys' => [
                'order_id',
                'sku',
                'quantity',
                'unit_price',
                'date',
                'discount_amount',
                'tax_amount',
                'total_amount',
                'customer',
                'store',
                'payment_method',
                'status',
                'notes',
            ],
            'required' => [true, true, true, true, true, false, false, false, false, false, false, false, false],
            'sample_rows' => [
                ['ORD-2024-101', 'SKU-COF-001', '2', '350000', '1403/06/10', '20000', '61200', '741200', 'رضا محمدی', 'شعبه مرکزی', 'CARD', 'COMPLETED', 'سفارش بیرون‌بر'],
                ['ORD-2024-101', 'SKU-SYR-003', '1', '95000', '1403/06/10', '0', '8550', '741200', 'رضا محمدی', 'شعبه مرکزی', 'CARD', 'COMPLETED', 'سفارش بیرون‌بر'],
                ['ORD-2024-102', 'SKU-TEA-002', '1', '180000', '1403/06/11', '0', '16200', '196200', 'سارا کاظمی', 'شعبه مرکزی', 'CASH', 'COMPLETED', 'میز ۴'],
            ],
            'domain_notes_fa' => [
                'ساختار چندسطری: برای سفارش‌های با چند قلم کالا، شناسه سفارش، تاریخ، مشتری و فروشگاه در تمام ردیف‌های آن سفارش باید کاملاً یکسان باشد (مانند ردیف ۱ و ۲ نمونه بالا).',
                'کد کالا (SKU) باید در سیستم از قبل ثبت شده باشد.',
                'تعداد باید عدد صحیح مثبت باشد.',
                'قرارداد HISTORICAL_RECORD_ONLY: سفارش‌های تاریخی صرفاً جهت سوابق و تحلیل وارد می‌شوند؛ بر موجودی انبار فعلی و صندوق/دفتر کل زنده امروز اثر نمی‌گذارند.',
            ],
            'domain_notes_en' => [
                'Multi-line grouping: Multiple items belonging to the same order must share identical Order ID, Date, Customer, and Store.',
                'SKU must exist in the product catalog.',
                'Quantity must be a positive integer.',
                'HISTORICAL_RECORD_ONLY contract: Zero inventory deduction, zero live GL revenue entries, zero cash drawer pollution.',
            ],
        ];
    }

    private function suppliersTemplate(): array
    {
        return [
            'type' => ImportType::SUPPLIERS,
            'filename' => 'resto_template_suppliers.csv',
            'title_fa' => 'قالب تأمین‌کنندگان',
            'title_en' => 'Suppliers Template',
            'description_fa' => 'اطلاعات تأمین‌کنندگان، شرکت‌های پخش و فروشندگان مواد اولیه و کالا.',
            'description_en' => 'Supplier master directory for procurement and purchasing records.',
            'is_multiline' => false,
            'headers' => [
                'نام تأمین‌کننده',
                'کد تأمین‌کننده',
                'شماره تماس',
                'ایمیل',
                'شناسه ملی / کد اقتصادی',
                'آدرس',
                'یادداشت',
            ],
            'field_keys' => [
                'name',
                'code',
                'phone',
                'email',
                'tax_id',
                'address',
                'notes',
            ],
            'required' => [true, false, false, false, false, false, false],
            'sample_rows' => [
                ['تأمین قهوه البرز', 'SUP-101', '09121112233', 'alborz@coffee.ir', '14001234567', 'تهران، خیابان ولیعصر، برج سپهر', 'تأمین‌کننده اصلی دان سبز و رست شده'],
                ['صنایع بسته‌بندی پارس', 'SUP-102', '02188990011', 'info@parspack.com', '14009876543', 'کرج، شهرک صنعتی بهارستان', 'تأمین کارتن و لیوان دوجداره'],
                ['لبنیات و مواد اولیه صبا', 'SUP-103', '09124445566', 'orders@sabadairy.ir', '10103322110', 'تهران، جاده مخصوص، کیلومتر ۱۱', 'شیر تازه و خامه قنادی'],
            ],
            'domain_notes_fa' => [
                'فیلد «نام تأمین‌کننده» الزامی است.',
                'کد تأمین‌کننده در صورت ثبت، شناسه یکتای پیوند با فاکتورهای خرید خواهد بود.',
                'تطبیق تکراری‌ها فقط بر اساس کد، شماره تماس یا ایمیل انجام می‌شود (صرف تشابه نام ادغام ایجاد نمی‌کند).',
            ],
            'domain_notes_en' => [
                'Supplier Name is required.',
                'Supplier Code serves as the unique identifier for linking purchase invoices.',
                'Duplicate matching uses code, phone, or email only (never merges solely on similar names).',
            ],
        ];
    }

    private function purchasesTemplate(): array
    {
        return [
            'type' => ImportType::PURCHASES,
            'filename' => 'resto_template_historical_purchases.csv',
            'title_fa' => 'قالب سوابق خرید (تاریخی)',
            'title_en' => 'Historical Purchases Template',
            'description_fa' => 'ورود سوابق فاکتورهای خرید گذشته از تأمین‌کنندگان جهت تحلیل بهای تمام‌شده و تاریخچه تأمین.',
            'description_en' => 'Past procurement invoices from suppliers for cost analytics and vendor history.',
            'is_multiline' => true,
            'headers' => [
                'شناسه خرید',
                'کد کالا',
                'تعداد',
                'قیمت خرید',
                'تاریخ',
                'تأمین‌کننده',
                'انبار مقصد',
                'فروشگاه',
                'تخفیف',
                'مالیات',
                'مبلغ کل فاکتور',
                'وضعیت',
                'وضعیت پرداخت',
                'یادداشت',
            ],
            'field_keys' => [
                'purchase_id',
                'sku',
                'quantity',
                'unit_cost',
                'date',
                'supplier',
                'warehouse',
                'store',
                'discount_amount',
                'tax_amount',
                'total_amount',
                'status',
                'payment_status',
                'notes',
            ],
            'required' => [true, true, true, true, true, false, false, false, false, false, false, false, false, false],
            'sample_rows' => [
                ['PO-2024-501', 'SKU-COF-001', '20', '220000', '1403/05/20', 'تأمین قهوه البرز', 'انبار مرکزی', 'شعبه مرکزی', '100000', '387000', '4687000', 'RECEIVED', 'PAID', 'خرید با فاکتور رسمی ۶۵۴'],
                ['PO-2024-501', 'SKU-TEA-002', '10', '110000', '1403/05/20', 'تأمین قهوه البرز', 'انبار مرکزی', 'شعبه مرکزی', '0', '99000', '4687000', 'RECEIVED', 'PAID', 'خرید با فاکتور رسمی ۶۵۴'],
                ['PO-2024-502', 'SKU-SYR-003', '15', '60000', '1403/05/25', 'صنایع بسته‌بندی پارس', 'انبار مرکزی', 'شعبه مرکزی', '50000', '76500', '926500', 'RECEIVED', 'PAID', 'تحویل باربری'],
            ],
            'domain_notes_fa' => [
                'ساختار چندسطری: اقلام یک فاکتور باید دارای شناسه خرید، تاریخ، تأمین‌کننده و انبار یکسان باشند (مانند ردیف ۱ و ۲ نمونه بالا).',
                'کد کالا (SKU) باید در کاتالوگ سازمان از قبل ثبت شده باشد.',
                'قیمت خرید و تعداد باید مثبت باشند.',
                'قرارداد HISTORICAL_RECORD_ONLY: خریدهای تاریخی موجودی فیزیکی انبار را تغییر نمی‌دهند، سند دوبل پرداخت ثبت نمی‌کنند و وارد صف دریافت انبار زنده نمی‌شوند.',
            ],
            'domain_notes_en' => [
                'Multi-line grouping: Multiple items belonging to the same purchase invoice must share identical Purchase ID, Date, Supplier, and Warehouse.',
                'SKU must exist in the product catalog.',
                'Unit cost and quantity must be positive numbers.',
                'HISTORICAL_RECORD_ONLY contract: Zero inventory mutations, zero GL journal entries, zero supplier payments, excluded from receiving queue.',
            ],
        ];
    }
}
