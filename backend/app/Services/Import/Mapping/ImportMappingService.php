<?php

namespace App\Services\Import\Mapping;

use App\Support\Import\ImportErrorCode;
use App\Support\Import\ImportType;
use InvalidArgumentException;

class ImportMappingService
{
    public const MATCH_EXACT_CANONICAL = 'exact_canonical';
    public const MATCH_EXACT_ALIAS     = 'exact_alias';
    public const MATCH_NORMALIZED_ALIAS = 'normalized_alias';
    public const MATCH_FUZZY_HIGH       = 'fuzzy_high';
    public const MATCH_AMBIGUOUS        = 'ambiguous';
    public const MATCH_CONFLICT         = 'conflict';
    public const MATCH_UNMAPPED         = 'unmapped';

    /**
     * Get field definitions for the specified domain.
     *
     * @return array<string, array{
     *     name: string,
     *     label_fa: string,
     *     label_en: string,
     *     type: string,
     *     required: bool,
     *     description_fa: string,
     *     description_en: string,
     *     aliases: string[]
     * }>
     */
    public function getTargetFields(string $importType): array
    {
        if ($importType === ImportType::PRODUCTS) {
            return [
                'name' => [
                    'name' => 'name',
                    'label_fa' => 'نام کالا',
                    'label_en' => 'Product Name',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'نام کامل کالا یا محصول (حداکثر ۱۹۱ کاراکتر)',
                    'description_en' => 'Full product or item title (max 191 chars)',
                    'aliases' => [
                        'نام', 'نام کالا', 'نام محصول', 'عنوان', 'کالا', 'محصول',
                        'name', 'product name', 'product_name', 'title', 'item name', 'item_name', 'product'
                    ],
                ],
                'sku' => [
                    'name' => 'sku',
                    'label_fa' => 'کد کالا / SKU',
                    'label_en' => 'SKU / Product Code',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'کد اختصاصی کالا در انبار یا سامانه قبلی (حداکثر ۱۰۰ کاراکتر)',
                    'description_en' => 'Unique product code or SKU (max 100 chars)',
                    'aliases' => [
                        'کد', 'کد کالا', 'کد محصول', 'شناسه', 'شناسه کالا', 'کد فنی', 'کد انبار',
                        'sku', 'code', 'product code', 'product_code', 'item code', 'item_code'
                    ],
                ],
                'price' => [
                    'name' => 'price',
                    'label_fa' => 'قیمت فروش',
                    'label_en' => 'Sale Price',
                    'type' => 'numeric',
                    'required' => true,
                    'description_fa' => 'قیمت فروش کالا به مشتری',
                    'description_en' => 'Selling price per unit',
                    'aliases' => [
                        'قیمت', 'قیمت فروش', 'مبلغ', 'نرخ', 'مبلغ فروش', 'فی', 'فی فروش', 'قیمت مصرف کننده',
                        'price', 'sale price', 'sale_price', 'selling price', 'unit price', 'unit_price', 'rate'
                    ],
                ],
                'cost_price' => [
                    'name' => 'cost_price',
                    'label_fa' => 'قیمت خرید',
                    'label_en' => 'Cost Price',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'قیمت تمام شده یا خرید کالا (در صورت خالی بودن ۰ در نظر گرفته می‌شود)',
                    'description_en' => 'Purchase or cost price per unit (defaults to 0)',
                    'aliases' => [
                        'قیمت خرید', 'بهای تمام شده', 'قیمت تمام شده', 'هزینه خرید', 'فی خرید', 'قیمت پایه',
                        'cost', 'cost price', 'cost_price', 'purchase price', 'purchase_price', 'buy price'
                    ],
                ],
                'unit' => [
                    'name' => 'unit',
                    'label_fa' => 'واحد سنجش',
                    'label_en' => 'Unit of Measure',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'واحد شمارش (مانند عدد، کیلوگرم، بسته، متر و ...)',
                    'description_en' => 'Unit of measurement (defaults to piece)',
                    'aliases' => [
                        'واحد', 'واحد شمارش', 'واحد سنجش', 'واحد کالا', 'واحد اندازه گیری',
                        'unit', 'uom', 'measure', 'unit of measure'
                    ],
                ],
                'barcode' => [
                    'name' => 'barcode',
                    'label_fa' => 'بارکد',
                    'label_en' => 'Barcode',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'کد میله‌ای استاندارد کالا',
                    'description_en' => 'Standard product barcode (EAN, UPC)',
                    'aliases' => [
                        'بارکد', 'کد میله ای', 'کد میله‌ای', 'بارکد کالا',
                        'barcode', 'bar code', 'upc', 'ean'
                    ],
                ],
                'category' => [
                    'name' => 'category',
                    'label_fa' => 'دسته‌بندی',
                    'label_en' => 'Category',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام گروه یا دسته‌بندی کالا',
                    'description_en' => 'Product category or group name',
                    'aliases' => [
                        'دسته', 'دسته‌بندی', 'دسته بندی', 'گروه', 'گروه کالا', 'طبقه بندی',
                        'category', 'group', 'type', 'category name'
                    ],
                ],
                'description' => [
                    'name' => 'description',
                    'label_fa' => 'توضیحات',
                    'label_en' => 'Description',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'توضیحات یا ویژگی‌های تکمیلی کالا',
                    'description_en' => 'Detailed product description or notes',
                    'aliases' => [
                        'توضیحات', 'شرح', 'شرح کالا', 'توضیح', 'یادداشت',
                        'description', 'notes', 'details', 'desc'
                    ],
                ],
            ];
        }

        if ($importType === ImportType::CUSTOMERS) {
            return [
                'name' => [
                    'name' => 'name',
                    'label_fa' => 'نام مشتری',
                    'label_en' => 'Customer Name',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'نام کامل یا عنوان مشتری / خریدار',
                    'description_en' => 'Full name or company name of customer',
                    'aliases' => [
                        'نام', 'نام مشتری', 'نام و نام خانوادگی', 'خریدار', 'نام مخاطب', 'نام خانوادگی', 'مشتری',
                        'name', 'customer name', 'customer_name', 'full name', 'full_name', 'client', 'contact'
                    ],
                ],
                'phone' => [
                    'name' => 'phone',
                    'label_fa' => 'شماره تماس / موبایل',
                    'label_en' => 'Phone / Mobile',
                    'type' => 'phone',
                    'required' => false,
                    'description_fa' => 'شماره تلفن همراه یا ثابت مشتری',
                    'description_en' => 'Mobile or landline phone number',
                    'aliases' => [
                        'تلفن', 'موبایل', 'شماره تماس', 'شماره همراه', 'همراه', 'تلفن همراه', 'تلفن ثابت', 'شماره تلفن',
                        'phone', 'mobile', 'cell', 'telephone', 'phone number', 'phone_number', 'mobile number', 'tel'
                    ],
                ],
                'email' => [
                    'name' => 'email',
                    'label_fa' => 'ایمیل',
                    'label_en' => 'Email Address',
                    'type' => 'email',
                    'required' => false,
                    'description_fa' => 'پست الکترونیکی مشتری',
                    'description_en' => 'Customer email address',
                    'aliases' => [
                        'ایمیل', 'پست الکترونیک', 'رایانامه', 'پست الکترونیکی',
                        'email', 'e-mail', 'mail', 'email address'
                    ],
                ],
                'address' => [
                    'name' => 'address',
                    'label_fa' => 'نشانی / آدرس',
                    'label_en' => 'Address',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'آدرس محل سکونت یا ارسال کالا',
                    'description_en' => 'Physical or billing address',
                    'aliases' => [
                        'آدرس', 'نشانی', 'آدرس کامل', 'محل سکونت', 'آدرس مشتری', 'نشانی پستی',
                        'address', 'location', 'street', 'full address'
                    ],
                ],
            ];
        }

        if ($importType === ImportType::INVENTORY) {
            return [
                'sku' => [
                    'name' => 'sku',
                    'label_fa' => 'کد کالا / SKU',
                    'label_en' => 'Product SKU / Code',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'کد یا شناسه کالا در سیستم (برای تطبیق با کالای از قبل تعریف شده)',
                    'description_en' => 'Product SKU or code to match existing product',
                    'aliases' => [
                        'کد', 'کد کالا', 'کد محصول', 'شناسه', 'شناسه کالا', 'کد فنی',
                        'sku', 'code', 'product code', 'product_code', 'item code', 'item_code'
                    ],
                ],
                'quantity' => [
                    'name' => 'quantity',
                    'label_fa' => 'موجودی اولیه',
                    'label_en' => 'Opening Quantity',
                    'type' => 'integer',
                    'required' => true,
                    'description_fa' => 'تعداد یا موجودی اولیه کالا در انبار (عدد صحیح بزرگتر از صفر)',
                    'description_en' => 'Opening balance quantity (integer greater than 0)',
                    'aliases' => [
                        'موجودی', 'موجودی اولیه', 'تعداد', 'مقدار', 'موجودی انبار', 'تعداد اولیه',
                        'quantity', 'qty', 'stock', 'opening stock', 'balance', 'count'
                    ],
                ],
                'warehouse' => [
                    'name' => 'warehouse',
                    'label_fa' => 'انبار / کد انبار',
                    'label_en' => 'Warehouse',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام یا کد انبار (در صورت عدم انتخاب، از انبار پیش‌فرض استفاده می‌شود)',
                    'description_en' => 'Warehouse code or name (falls back to default warehouse)',
                    'aliases' => [
                        'انبار', 'کد انبار', 'نام انبار', 'انبار کالا', 'محل انبار',
                        'warehouse', 'warehouse code', 'warehouse_code', 'wh', 'location'
                    ],
                ],
                'barcode' => [
                    'name' => 'barcode',
                    'label_fa' => 'بارکد کالا',
                    'label_en' => 'Barcode',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'بارکد کالا به عنوان شناسه تکمیلی',
                    'description_en' => 'Product barcode as secondary identifier',
                    'aliases' => [
                        'بارکد', 'کد میله ای', 'کد میله‌ای', 'بارکد کالا',
                        'barcode', 'bar code', 'upc', 'ean'
                    ],
                ],
                'unit_cost' => [
                    'name' => 'unit_cost',
                    'label_fa' => 'بهای تمام شده / قیمت خرید',
                    'label_en' => 'Unit Cost',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'ارزش واحد موجودی (در صورت خالی بودن از بهای خرید تعریف شده کالا استفاده می‌شود)',
                    'description_en' => 'Unit cost of opening stock (defaults to product cost price)',
                    'aliases' => [
                        'قیمت خرید', 'بهای تمام شده', 'قیمت تمام شده', 'فی خرید', 'هزینه واحد',
                        'cost', 'unit cost', 'unit_cost', 'cost price', 'purchase price'
                    ],
                ],
                'notes' => [
                    'name' => 'notes',
                    'label_fa' => 'توضیحات / یادداشت',
                    'label_en' => 'Notes',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'یادداشت یا توضیحات برای گردش موجودی',
                    'description_en' => 'Audit notes or remarks for stock adjustment',
                    'aliases' => [
                        'توضیحات', 'یادداشت', 'شرح', 'علت', 'علت موجودی',
                        'notes', 'description', 'remark', 'remarks'
                    ],
                ],
            ];
        }

        if ($importType === ImportType::EXPENSES) {
            return [
                'title' => [
                    'name' => 'title',
                    'label_fa' => 'عنوان هزینه',
                    'label_en' => 'Expense Title',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'عنوان یا شرح کوتاه هزینه (حداکثر ۱۹۱ کاراکتر)',
                    'description_en' => 'Short title or description of expense (max 191 chars)',
                    'aliases' => [
                        'عنوان', 'شرح', 'شرح هزینه', 'عنوان هزینه', 'بابت', 'توضیح', 'نام هزینه',
                        'title', 'description', 'expense title', 'expense_title', 'name', 'item'
                    ],
                ],
                'category' => [
                    'name' => 'category',
                    'label_fa' => 'دسته‌بندی هزینه',
                    'label_en' => 'Expense Category',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'دسته‌بندی هزینه (مانند اجاره، اینترنت، برق، آب، حمل‌ونقل، تبلیغات، حقوق، تعمیرات، خدمات، کارمزد، سایر)',
                    'description_en' => 'Expense category code or name (RENT, INTERNET, ELECTRICITY, WATER, TRANSPORT, ADVERTISING, SALARIES, REPAIRS, SERVICES, FEES, OTHER)',
                    'aliases' => [
                        'دسته', 'دسته بندی', 'دسته‌بندی', 'نوع هزینه', 'گروه هزینه', 'سرفصل', 'کد دسته',
                        'category', 'expense category', 'expense_category', 'type', 'group', 'cost category'
                    ],
                ],
                'amount' => [
                    'name' => 'amount',
                    'label_fa' => 'مبلغ هزینه',
                    'label_en' => 'Expense Amount',
                    'type' => 'numeric',
                    'required' => true,
                    'description_fa' => 'مبلغ هزینه پرداخت‌شده (بزرگتر از صفر)',
                    'description_en' => 'Total expense amount paid (greater than 0)',
                    'aliases' => [
                        'مبلغ', 'مبلغ هزینه', 'قیمت', 'هزینه', 'مقدار', 'میزان', 'بها',
                        'amount', 'expense amount', 'expense_amount', 'total', 'cost', 'price', 'paid'
                    ],
                ],
                'date' => [
                    'name' => 'date',
                    'label_fa' => 'تاریخ پرداخت / وقوع',
                    'label_en' => 'Expense Date',
                    'type' => 'date',
                    'required' => true,
                    'description_fa' => 'تاریخ هزینه به صورت شمسی (۱۴۰۳/۰۷/۲۱) یا میلادی (2024-10-12)',
                    'description_en' => 'Expense date in Persian Jalali or Gregorian format',
                    'aliases' => [
                        'تاریخ', 'تاریخ پرداخت', 'تاریخ ثبت', 'تاریخ هزینه', 'زمان', 'تاریخ فاکتور',
                        'date', 'expense date', 'expense_date', 'payment date', 'payment_date'
                    ],
                ],
                'store' => [
                    'name' => 'store',
                    'label_fa' => 'فروشگاه / شعبه',
                    'label_en' => 'Store / Branch',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام یا کد فروشگاه پرداخت‌کننده (در صورت خالی بودن از فروشگاه پیش‌فرض استفاده می‌شود)',
                    'description_en' => 'Store name or code (defaults to selected default store)',
                    'aliases' => [
                        'فروشگاه', 'کد فروشگاه', 'نام فروشگاه', 'شعبه', 'کد شعبه', 'مرکز هزینه',
                        'store', 'store code', 'store_code', 'branch', 'location'
                    ],
                ],
                'payment_method' => [
                    'name' => 'payment_method',
                    'label_fa' => 'روش پرداخت',
                    'label_en' => 'Payment Method',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'شیوه پرداخت هزینه (نقدی CASH، کارت CARD، حواله بانکی BANK_TRANSFER، چک CHEQUE)',
                    'description_en' => 'Payment method (CASH, CARD, BANK_TRANSFER, CHEQUE)',
                    'aliases' => [
                        'روش پرداخت', 'نحوه پرداخت', 'نوع پرداخت', 'شیوه پرداخت', 'حساب پرداخت',
                        'payment method', 'payment_method', 'method', 'payment type', 'paid by'
                    ],
                ],
                'notes' => [
                    'name' => 'notes',
                    'label_fa' => 'یادداشت / توضیحات',
                    'label_en' => 'Notes / Remarks',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'توضیحات تکمیلی یا یادداشت پیوست هزینه',
                    'description_en' => 'Additional notes or remarks',
                    'aliases' => [
                        'توضیحات', 'یادداشت', 'شرح تکمیلی', 'ملاحظات', 'پی نوشت',
                        'notes', 'remarks', 'comment', 'memo', 'note'
                    ],
                ],
            ];
        }

        if ($importType === ImportType::ORDERS) {
            return [
                'order_id' => [
                    'name' => 'order_id',
                    'label_fa' => 'شناسه سفارش / شماره فاکتور خارجی',
                    'label_en' => 'Order ID / External Invoice #',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'شناسه یکتا یا شماره فاکتور سفارش در سیستم قبلی (جهت گروه‌بندی اقلام)',
                    'description_en' => 'Unique order ID or invoice number in legacy system (for grouping items)',
                    'aliases' => [
                        'شماره سفارش', 'شماره فاکتور', 'کد سفارش', 'کد فاکتور', 'شناسه سفارش', 'شماره قبض', 'فاکتور',
                        'order_id', 'order id', 'order_number', 'invoice_no', 'invoice', 'invoice_number', 'bill_no'
                    ],
                ],
                'sku' => [
                    'name' => 'sku',
                    'label_fa' => 'کد کالا / SKU',
                    'label_en' => 'Product SKU',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'کد یا شناسه کالا در سیستم Resto',
                    'description_en' => 'Product SKU or code in Resto catalog',
                    'aliases' => [
                        'کد کالا', 'کد محصول', 'کد', 'شناسه کالا', 'شناسه محصول', 'بارکد',
                        'sku', 'product_sku', 'item_code', 'code', 'product_code', 'barcode'
                    ],
                ],
                'quantity' => [
                    'name' => 'quantity',
                    'label_fa' => 'تعداد / مقدار',
                    'label_en' => 'Quantity',
                    'type' => 'integer',
                    'required' => true,
                    'description_fa' => 'تعداد کالای خریداری شده در این ردیف (عدد صحیح مثبت)',
                    'description_en' => 'Quantity purchased in this line item (positive integer)',
                    'aliases' => [
                        'تعداد', 'مقدار', 'میزان', 'حجم',
                        'quantity', 'qty', 'count', 'amount'
                    ],
                ],
                'unit_price' => [
                    'name' => 'unit_price',
                    'label_fa' => 'قیمت واحد',
                    'label_en' => 'Unit Price',
                    'type' => 'numeric',
                    'required' => true,
                    'description_fa' => 'قیمت واحد تاریخی کالا در زمان ثبت سفارش',
                    'description_en' => 'Historical unit price at the time of sale',
                    'aliases' => [
                        'قیمت واحد', 'فی', 'نرخ', 'قیمت تک', 'مبلغ واحد', 'قیمت فروش',
                        'unit_price', 'price', 'unit price', 'rate', 'item_price'
                    ],
                ],
                'date' => [
                    'name' => 'date',
                    'label_fa' => 'تاریخ و زمان سفارش',
                    'label_en' => 'Order Date & Time',
                    'type' => 'datetime',
                    'required' => true,
                    'description_fa' => 'تاریخ و زمان وقوع سفارش (شمسی یا میلادی)',
                    'description_en' => 'Date and time when the order occurred (Jalali or Gregorian)',
                    'aliases' => [
                        'تاریخ سفارش', 'تاریخ فاکتور', 'تاریخ', 'زمان', 'تاریخ و ساعت', 'زمان ثبت',
                        'order_date', 'date', 'created_at', 'invoice_date', 'datetime', 'time'
                    ],
                ],
                'discount_amount' => [
                    'name' => 'discount_amount',
                    'label_fa' => 'تخفیف',
                    'label_en' => 'Discount',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'مبلغ تخفیف این ردیف یا سفارش',
                    'description_en' => 'Discount amount for this line item or order',
                    'aliases' => [
                        'تخفیف', 'مبلغ تخفیف', 'درصد تخفیف', 'کسورات',
                        'discount', 'discount_amount', 'discount_percent', 'disc'
                    ],
                ],
                'tax_amount' => [
                    'name' => 'tax_amount',
                    'label_fa' => 'مالیات / عوارض',
                    'label_en' => 'Tax Amount',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'مبلغ مالیات بر ارزش افزوده تاریخی',
                    'description_en' => 'Historical tax or VAT amount',
                    'aliases' => [
                        'مالیات', 'ارزش افزوده', 'عوارض', 'مالیات و عوارض',
                        'tax', 'tax_amount', 'vat', 'tax_rate'
                    ],
                ],
                'total_amount' => [
                    'name' => 'total_amount',
                    'label_fa' => 'مبلغ کل سفارش',
                    'label_en' => 'Total Amount',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'مبلغ نهایی پرداخت‌شده فاکتور (جهت اعتبارسنجی جمع اقلام)',
                    'description_en' => 'Total paid invoice amount (for validating sum of items)',
                    'aliases' => [
                        'مبلغ کل', 'جمع کل', 'مبلغ نهایی', 'مبلغ فاکتور', 'قابل پرداخت', 'کل',
                        'total', 'total_amount', 'final_amount', 'grand_total', 'net_amount'
                    ],
                ],
                'customer' => [
                    'name' => 'customer',
                    'label_fa' => 'مشتری (نام / تلفن / شناسه)',
                    'label_en' => 'Customer (Name / Phone / ID)',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام، شماره تلفن یا شناسه مشتری در سیستم',
                    'description_en' => 'Customer name, phone number, or ID',
                    'aliases' => [
                        'مشتری', 'نام مشتری', 'خریدار', 'تلفن مشتری', 'موبایل مشتری', 'شماره مشتری',
                        'customer', 'customer_name', 'client', 'buyer', 'phone', 'customer_phone'
                    ],
                ],
                'store' => [
                    'name' => 'store',
                    'label_fa' => 'فروشگاه / شعبه',
                    'label_en' => 'Store / Branch',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام یا کد فروشگاه ثبات سفارش (در صورت خالی بودن از فروشگاه پیش‌فرض استفاده می‌شود)',
                    'description_en' => 'Store name or code (defaults to selected default store)',
                    'aliases' => [
                        'فروشگاه', 'شعبه', 'کد شعبه', 'کد فروشگاه',
                        'store', 'branch', 'store_code', 'location'
                    ],
                ],
                'payment_method' => [
                    'name' => 'payment_method',
                    'label_fa' => 'روش پرداخت',
                    'label_en' => 'Payment Method',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'شیوه پرداخت (CASH, CARD, BANK_TRANSFER, ONLINE)',
                    'description_en' => 'Payment method (CASH, CARD, BANK_TRANSFER, ONLINE)',
                    'aliases' => [
                        'روش پرداخت', 'نحوه پرداخت', 'نوع پرداخت',
                        'payment_method', 'method', 'paid_by'
                    ],
                ],
                'status' => [
                    'name' => 'status',
                    'label_fa' => 'وضعیت سفارش',
                    'label_en' => 'Order Status',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'وضعیت سفارش (COMPLETED, NEW, PREPARING, CANCELLED)',
                    'description_en' => 'Order status (COMPLETED, NEW, PREPARING, CANCELLED)',
                    'aliases' => [
                        'وضعیت', 'وضعیت سفارش', 'حالت',
                        'status', 'order_status', 'state'
                    ],
                ],
                'notes' => [
                    'name' => 'notes',
                    'label_fa' => 'یادداشت / توضیحات',
                    'label_en' => 'Notes',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'توضیحات تکمیلی یا یادداشت فاکتور',
                    'description_en' => 'Additional notes or remarks',
                    'aliases' => [
                        'یادداشت', 'توضیحات', 'شرح', 'ملاحظات',
                        'notes', 'description', 'remarks', 'comment'
                    ],
                ],
            ];
        }

        if ($importType === ImportType::SUPPLIERS) {
            return [
                'name' => [
                    'name' => 'name',
                    'label_fa' => 'نام تأمین‌کننده',
                    'label_en' => 'Supplier Name',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'نام کامل تأمین‌کننده یا فروشنده (حداکثر ۱۹۱ کاراکتر)',
                    'description_en' => 'Full name or company name of supplier (max 191 chars)',
                    'aliases' => [
                        'نام', 'نام تامین کننده', 'تامین کننده', 'نام فروشنده', 'فروشنده', 'نام شرکت',
                        'name', 'supplier', 'supplier name', 'supplier_name', 'vendor', 'vendor name', 'vendor_name', 'company'
                    ],
                ],
                'code' => [
                    'name' => 'code',
                    'label_fa' => 'کد تأمین‌کننده',
                    'label_en' => 'Supplier Code',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'کد یا شناسه اختصاصی تأمین‌کننده در سیستم قبلی',
                    'description_en' => 'Unique code or identifier of supplier in legacy system',
                    'aliases' => [
                        'کد', 'کد تامین کننده', 'شناسه تامین کننده', 'کد فروشنده', 'شناسه فروشنده',
                        'code', 'supplier code', 'supplier_code', 'vendor code', 'vendor_code', 'id'
                    ],
                ],
                'phone' => [
                    'name' => 'phone',
                    'label_fa' => 'شماره تماس',
                    'label_en' => 'Phone',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'شماره تلفن یا موبایل تماس با تأمین‌کننده',
                    'description_en' => 'Contact phone or mobile number',
                    'aliases' => [
                        'تلفن', 'موبایل', 'شماره تماس', 'شماره تلفن', 'تماس', 'تلفن ثابت',
                        'phone', 'mobile', 'tel', 'cell', 'phone number', 'phone_number', 'contact'
                    ],
                ],
                'email' => [
                    'name' => 'email',
                    'label_fa' => 'ایمیل',
                    'label_en' => 'Email',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'آدرس پست الکترونیکی معتبر',
                    'description_en' => 'Valid email address',
                    'aliases' => [
                        'ایمیل', 'پست الکترونیک', 'رایانامه',
                        'email', 'mail', 'e-mail', 'email address'
                    ],
                ],
                'address' => [
                    'name' => 'address',
                    'label_fa' => 'آدرس / نشانی',
                    'label_en' => 'Address',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نشانی یا آدرس دفتر یا کارخانه تأمین‌کننده',
                    'description_en' => 'Physical or billing address of supplier',
                    'aliases' => [
                        'آدرس', 'نشانی', 'محل', 'محل شرکت', 'نشانی کامل',
                        'address', 'location', 'street', 'city'
                    ],
                ],
                'tax_id' => [
                    'name' => 'tax_id',
                    'label_fa' => 'شناسه ملی / کد اقتصادی',
                    'label_en' => 'Tax / National ID',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'شناسه ملی، شماره اقتصادی یا کد مالیاتی شرکت',
                    'description_en' => 'National business registry or tax ID',
                    'aliases' => [
                        'شناسه ملی', 'کد اقتصادی', 'شماره اقتصادی', 'کد مالیاتی', 'شماره مالیاتی', 'کد ملی',
                        'tax id', 'tax_id', 'vat id', 'economic code', 'economic_code', 'national id', 'tax number'
                    ],
                ],
                'notes' => [
                    'name' => 'notes',
                    'label_fa' => 'یادداشت / توضیحات',
                    'label_en' => 'Notes',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'توضیحات یا یادداشت‌های تکمیلی درباره تأمین‌کننده',
                    'description_en' => 'Additional notes or remarks regarding supplier',
                    'aliases' => [
                        'یادداشت', 'توضیحات', 'شرح', 'ملاحظات',
                        'notes', 'description', 'remarks', 'comment'
                    ],
                ],
            ];
        }

        if ($importType === ImportType::PURCHASES) {
            return [
                'purchase_id' => [
                    'name' => 'purchase_id',
                    'label_fa' => 'شناسه خرید / شماره فاکتور',
                    'label_en' => 'Purchase / Invoice ID',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'شناسه فاکتور خرید در سیستم قبلی (جهت گروه‌بندی اقلام چندسطری)',
                    'description_en' => 'Invoice or purchase identifier in legacy system (groups multi-line items)',
                    'aliases' => [
                        'شماره فاکتور', 'شماره خرید', 'شناسه خرید', 'شماره فاکتور خرید', 'کد خرید', 'شماره سفارش خرید',
                        'purchase_id', 'purchase id', 'invoice_number', 'invoice', 'invoice id', 'po_number', 'po number', 'order_id'
                    ],
                ],
                'sku' => [
                    'name' => 'sku',
                    'label_fa' => 'کد کالا / SKU / بارکد',
                    'label_en' => 'Product SKU / Barcode',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'کد، شناسه یا بارکد کالا در کاتالوگ سازمان',
                    'description_en' => 'Product code, SKU or barcode matching tenant catalog',
                    'aliases' => [
                        'کد کالا', 'بارکد', 'کد محصول', 'شناسه کالا', 'کد فنی',
                        'sku', 'barcode', 'code', 'product_code', 'product code', 'item_code'
                    ],
                ],
                'quantity' => [
                    'name' => 'quantity',
                    'label_fa' => 'تعداد خرید',
                    'label_en' => 'Quantity',
                    'type' => 'numeric',
                    'required' => true,
                    'description_fa' => 'تعداد یا مقدار کالای خریداری‌شده (عدد صحیح مثبت)',
                    'description_en' => 'Quantity purchased per item (positive integer)',
                    'aliases' => [
                        'تعداد', 'مقدار', 'حجم', 'تعداد خرید', 'تعداد اقلام',
                        'quantity', 'qty', 'count', 'amount'
                    ],
                ],
                'unit_cost' => [
                    'name' => 'unit_cost',
                    'label_fa' => 'قیمت واحد خرید',
                    'label_en' => 'Unit Cost',
                    'type' => 'numeric',
                    'required' => true,
                    'description_fa' => 'قیمت خرید هر واحد کالا در فاکتور',
                    'description_en' => 'Purchase price per single unit in invoice',
                    'aliases' => [
                        'قیمت خرید', 'فی خرید', 'قیمت واحد', 'فی', 'نرخ خرید', 'بهای خرید',
                        'unit_cost', 'unit cost', 'cost', 'cost_price', 'cost price', 'unit_price', 'unit price', 'price', 'buy_price'
                    ],
                ],
                'date' => [
                    'name' => 'date',
                    'label_fa' => 'تاریخ خرید',
                    'label_en' => 'Purchase Date',
                    'type' => 'string',
                    'required' => true,
                    'description_fa' => 'تاریخ وقوع خرید (شمسی مانند 1402/05/15 یا میلادی YYYY-MM-DD)',
                    'description_en' => 'Purchase date in solar Jalali or Gregorian ISO format',
                    'aliases' => [
                        'تاریخ', 'تاریخ خرید', 'تاریخ فاکتور', 'زمان خرید', 'تاریخ سند',
                        'date', 'purchase_date', 'purchase date', 'invoice_date', 'invoice date'
                    ],
                ],
                'supplier' => [
                    'name' => 'supplier',
                    'label_fa' => 'تأمین‌کننده',
                    'label_en' => 'Supplier',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام یا کد تأمین‌کننده / فروشنده طرف حساب',
                    'description_en' => 'Supplier name or code',
                    'aliases' => [
                        'تامین کننده', 'نام تامین کننده', 'کد تامین کننده', 'فروشنده', 'طرف حساب',
                        'supplier', 'vendor', 'supplier_name', 'supplier name', 'supplier_code', 'vendor_name'
                    ],
                ],
                'warehouse' => [
                    'name' => 'warehouse',
                    'label_fa' => 'انبار مقصد',
                    'label_en' => 'Destination Warehouse',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام یا کد انبار مقصد دریافت کالا',
                    'description_en' => 'Warehouse name or code receiving the goods',
                    'aliases' => [
                        'انبار', 'انبار مقصد', 'کد انبار', 'نام انبار',
                        'warehouse', 'warehouse_name', 'warehouse_code', 'depot'
                    ],
                ],
                'store' => [
                    'name' => 'store',
                    'label_fa' => 'فروشگاه / شعبه',
                    'label_en' => 'Store / Branch',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'نام یا کد شعبه یا فروشگاه مرتبط با خرید',
                    'description_en' => 'Store or branch name or code',
                    'aliases' => [
                        'فروشگاه', 'شعبه', 'کد شعبه', 'کد فروشگاه',
                        'store', 'branch', 'store_code', 'location'
                    ],
                ],
                'discount_amount' => [
                    'name' => 'discount_amount',
                    'label_fa' => 'مبلغ تخفیف',
                    'label_en' => 'Discount Amount',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'مبلغ تخفیف ردیف یا فاکتور خرید',
                    'description_en' => 'Discount amount on item line or invoice',
                    'aliases' => [
                        'تخفیف', 'مبلغ تخفیف', 'تخفیف فاکتور',
                        'discount', 'discount_amount', 'discount amount', 'disc'
                    ],
                ],
                'tax_amount' => [
                    'name' => 'tax_amount',
                    'label_fa' => 'مبلغ مالیات / ارزش افزوده',
                    'label_en' => 'Tax Amount',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'مبلغ مالیات یا ارزش افزوده خرید',
                    'description_en' => 'Tax or VAT amount on item or invoice',
                    'aliases' => [
                        'مالیات', 'ارزش افزوده', 'مبلغ مالیات', 'مالیات بر ارزش افزوده',
                        'tax', 'tax_amount', 'tax amount', 'vat'
                    ],
                ],
                'total_amount' => [
                    'name' => 'total_amount',
                    'label_fa' => 'مبلغ کل فاکتور',
                    'label_en' => 'Total Amount',
                    'type' => 'numeric',
                    'required' => false,
                    'description_fa' => 'مبلغ کل نهایی فاکتور خرید',
                    'description_en' => 'Total payable amount for the invoice',
                    'aliases' => [
                        'مبلغ کل', 'جمع کل', 'مبلغ نهایی', 'کل فاکتور', 'جمع نهایی',
                        'total', 'total_amount', 'total amount', 'grand_total', 'final_amount'
                    ],
                ],
                'status' => [
                    'name' => 'status',
                    'label_fa' => 'وضعیت خرید',
                    'label_en' => 'Purchase Status',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'وضعیت خرید (RECEIVED, ORDERED, PARTIALLY_RECEIVED, CANCELLED)',
                    'description_en' => 'Purchase status (RECEIVED, ORDERED, PARTIALLY_RECEIVED, CANCELLED)',
                    'aliases' => [
                        'وضعیت', 'وضعیت خرید', 'حالت خرید',
                        'status', 'purchase_status', 'state'
                    ],
                ],
                'payment_status' => [
                    'name' => 'payment_status',
                    'label_fa' => 'وضعیت پرداخت / تسویه',
                    'label_en' => 'Payment Status',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'وضعیت تسویه فاکتور (PAID, UNPAID, PARTIAL)',
                    'description_en' => 'Payment settlement status (PAID, UNPAID, PARTIAL)',
                    'aliases' => [
                        'وضعیت پرداخت', 'نحوه تسویه', 'تسویه', 'وضعیت تسویه',
                        'payment_status', 'paid_status', 'settlement'
                    ],
                ],
                'notes' => [
                    'name' => 'notes',
                    'label_fa' => 'یادداشت / توضیحات',
                    'label_en' => 'Notes',
                    'type' => 'string',
                    'required' => false,
                    'description_fa' => 'توضیحات تکمیلی یا یادداشت فاکتور خرید',
                    'description_en' => 'Additional notes or remarks for purchase',
                    'aliases' => [
                        'یادداشت', 'توضیحات', 'شرح', 'ملاحظات',
                        'notes', 'description', 'remarks', 'comment'
                    ],
                ],
            ];
        }

        throw new InvalidArgumentException("نوع درون‌ریزی '{$importType}' برای تطبیق ستون‌ها پشتیبانی نمی‌شود.");
    }

    /**
     * Generate deterministic mapping suggestions for a list of source headers.
     * Preserves backward compatibility while providing rich match metadata.
     *
     * @param string[] $sourceHeaders
     * @return array<int, array{
     *     source_column: string,
     *     source_index: int,
     *     target_field: string|null,
     *     confidence: float|null,
     *     match_category: string,
     *     is_ambiguous: bool,
     *     has_conflict: bool,
     *     matched_alias: string|null,
     *     candidate_fields: string[],
     *     competing_columns: string[],
     *     reason_fa: string,
     *     reason_en: string
     * }>
     */
    public function suggestMappings(array $sourceHeaders, string $importType): array
    {
        return $this->suggestMappingsDetailed($sourceHeaders, $importType)['suggestions'];
    }

    /**
     * Generate detailed deterministic mapping suggestions, conflict analysis,
     * ambiguity detection, and unmapped required fields check.
     *
     * @param string[] $sourceHeaders
     * @return array{
     *     suggestions: array<int, array{
     *         source_column: string,
     *         source_index: int,
     *         target_field: string|null,
     *         confidence: float|null,
     *         match_category: string,
     *         is_ambiguous: bool,
     *         has_conflict: bool,
     *         matched_alias: string|null,
     *         candidate_fields: string[],
     *         competing_columns: string[],
     *         reason_fa: string,
     *         reason_en: string
     *     }>,
     *     conflicts: array<int, array{
     *         target_field: string,
     *         target_label_fa: string,
     *         target_label_en: string,
     *         competing_columns: string[],
     *         dominant_column: string|null,
     *         message_fa: string,
     *         message_en: string
     *     }>,
     *     ambiguous_headers: array<int, array{
     *         source_column: string,
     *         source_index: int,
     *         candidate_fields: string[],
     *         candidate_labels_fa: string[],
     *         confidence: float,
     *         message_fa: string,
     *         message_en: string
     *     }>,
     *     unmapped_required_fields: array<int, array{
     *         name: string,
     *         label_fa: string,
     *         label_en: string,
     *         type: string,
     *         description_fa: string,
     *         description_en: string
     *     }>
     * }
     */
    public function suggestMappingsDetailed(array $sourceHeaders, string $importType): array
    {
        $targetFields = $this->getTargetFields($importType);

        // Step 1: Candidate matching per source header
        $headerCandidates = [];

        foreach ($sourceHeaders as $index => $rawHeader) {
            $rawTrimmed = trim((string) $rawHeader);
            if ($rawTrimmed === '') {
                $headerCandidates[$index] = [
                    'source_column' => '',
                    'source_index' => $index,
                    'candidates' => [],
                    'top_candidates' => [],
                    'is_ambiguous' => false,
                ];
                continue;
            }

            $candidates = [];
            foreach ($targetFields as $fieldName => $def) {
                $match = $this->matchCandidate($rawTrimmed, $fieldName, $def);
                if ($match !== null) {
                    $candidates[] = $match;
                }
            }

            // Sort candidates by score descending
            usort($candidates, fn ($a, $b) => $b['score'] <=> $a['score']);

            $isAmbiguous = false;
            $topCandidates = [];
            if (!empty($candidates)) {
                $maxScore = $candidates[0]['score'];
                $topCandidates = array_values(array_filter(
                    $candidates,
                    fn ($c) => abs($c['score'] - $maxScore) < 0.001
                ));
                if (count($topCandidates) > 1) {
                    $isAmbiguous = true;
                }
            }

            $headerCandidates[$index] = [
                'source_column' => $rawTrimmed,
                'source_index' => $index,
                'candidates' => $candidates,
                'top_candidates' => $topCandidates,
                'is_ambiguous' => $isAmbiguous,
            ];
        }

        // Step 2: Identify ambiguous headers
        $ambiguousHeaders = [];
        foreach ($headerCandidates as $index => $item) {
            if ($item['is_ambiguous']) {
                $candNames = array_column($item['top_candidates'], 'target_field');
                $candLabels = array_map(fn ($f) => $targetFields[$f]['label_fa'], $candNames);

                $ambiguousHeaders[] = [
                    'source_column' => $item['source_column'],
                    'source_index' => $item['source_index'],
                    'candidate_fields' => $candNames,
                    'candidate_labels_fa' => $candLabels,
                    'confidence' => $item['top_candidates'][0]['score'],
                    'message_fa' => "ستون '{$item['source_column']}' با فیلدهای " . implode(' و ', array_map(fn ($l) => "'{$l}'", $candLabels)) . " انطباق یکسان دارد و نیازمند انتخاب دستی است.",
                    'message_en' => "Column '{$item['source_column']}' matches multiple fields (" . implode(', ', $candNames) . ") with equal confidence.",
                ];
            }
        }

        // Step 3: Group unambiguous top candidates by target field to detect competing columns (conflicts)
        $targetProposals = [];
        foreach ($headerCandidates as $index => $item) {
            if (!$item['is_ambiguous'] && !empty($item['top_candidates'])) {
                $winner = $item['top_candidates'][0];
                $targetField = $winner['target_field'];
                $targetProposals[$targetField][] = [
                    'source_index' => $index,
                    'source_column' => $item['source_column'],
                    'match' => $winner,
                ];
            }
        }

        // Step 4: Resolve competition for each target field
        $finalAssignments = [];
        $conflicts = [];

        foreach ($targetProposals as $targetField => $proposers) {
            if (count($proposers) === 1) {
                // No competition, single column mapped cleanly
                $p = $proposers[0];
                $finalAssignments[$p['source_index']] = [
                    'target_field' => $targetField,
                    'confidence' => $p['match']['score'],
                    'match_category' => $p['match']['category'],
                    'matched_alias' => $p['match']['matched_alias'],
                    'has_conflict' => false,
                    'competing_columns' => [],
                    'reason_fa' => $p['match']['reason_fa'],
                    'reason_en' => $p['match']['reason_en'],
                ];
            } else {
                // Multiple columns competing for the same target field
                usort($proposers, fn ($a, $b) => $b['match']['score'] <=> $a['match']['score']);
                $maxScore = $proposers[0]['match']['score'];
                $dominantProposers = array_values(array_filter(
                    $proposers,
                    fn ($p) => abs($p['match']['score'] - $maxScore) < 0.001
                ));

                $allColumnNames = array_column($proposers, 'source_column');

                if (count($dominantProposers) > 1) {
                    // Tie! Neither wins silently; both flagged as conflict and left unmapped
                    $conflicts[] = [
                        'target_field' => $targetField,
                        'target_label_fa' => $targetFields[$targetField]['label_fa'],
                        'target_label_en' => $targetFields[$targetField]['label_en'],
                        'competing_columns' => $allColumnNames,
                        'dominant_column' => null,
                        'message_fa' => "ستون‌های " . implode(' و ', array_map(fn ($c) => "'{$c}'", $allColumnNames)) . " برای فیلد مقصد '{$targetFields[$targetField]['label_fa']}' با امتیاز یکسان رقابت دارند.",
                        'message_en' => "Columns " . implode(' and ', $allColumnNames) . " compete for target field '{$targetField}' with equal confidence.",
                    ];

                    foreach ($proposers as $p) {
                        $otherCols = array_values(array_diff($allColumnNames, [$p['source_column']]));
                        $finalAssignments[$p['source_index']] = [
                            'target_field' => null,
                            'confidence' => null,
                            'match_category' => self::MATCH_CONFLICT,
                            'matched_alias' => $p['match']['matched_alias'],
                            'has_conflict' => true,
                            'competing_columns' => $otherCols,
                            'reason_fa' => "تداخل: این ستون با ستون‌های " . implode(' و ', array_map(fn ($c) => "'{$c}'", $otherCols)) . " برای فیلد '{$targetFields[$targetField]['label_fa']}' رقابت یکسان دارد.",
                            'reason_en' => "Conflict: this column competes with " . implode(', ', $otherCols) . " for '{$targetField}' with equal confidence.",
                        ];
                    }
                } else {
                    // One strictly dominates!
                    $winner = $dominantProposers[0];
                    $losingColumns = array_values(array_diff($allColumnNames, [$winner['source_column']]));

                    $conflicts[] = [
                        'target_field' => $targetField,
                        'target_label_fa' => $targetFields[$targetField]['label_fa'],
                        'target_label_en' => $targetFields[$targetField]['label_en'],
                        'competing_columns' => $allColumnNames,
                        'dominant_column' => $winner['source_column'],
                        'message_fa' => "ستون '{$winner['source_column']}' به دلیل امتیاز بالاتر نسبت به " . implode(' و ', array_map(fn ($c) => "'{$c}'", $losingColumns)) . " به فیلد مقصد '{$targetFields[$targetField]['label_fa']}' اختصاص یافت.",
                        'message_en' => "Column '{$winner['source_column']}' was assigned to '{$targetField}' over " . implode(', ', $losingColumns) . " due to higher confidence.",
                    ];

                    // Winning column is assigned
                    $finalAssignments[$winner['source_index']] = [
                        'target_field' => $targetField,
                        'confidence' => $winner['match']['score'],
                        'match_category' => $winner['match']['category'],
                        'matched_alias' => $winner['match']['matched_alias'],
                        'has_conflict' => false,
                        'competing_columns' => $losingColumns,
                        'reason_fa' => $winner['match']['reason_fa'],
                        'reason_en' => $winner['match']['reason_en'],
                    ];

                    // Losing columns are unmapped and flagged with conflict
                    foreach ($proposers as $p) {
                        if ($p['source_index'] === $winner['source_index']) {
                            continue;
                        }
                        $finalAssignments[$p['source_index']] = [
                            'target_field' => null,
                            'confidence' => null,
                            'match_category' => self::MATCH_CONFLICT,
                            'matched_alias' => $p['match']['matched_alias'],
                            'has_conflict' => true,
                            'competing_columns' => [$winner['source_column']],
                            'reason_fa' => "ستون منبع دیگری ('{$winner['source_column']}') با اطمینان بالاتر به فیلد '{$targetFields[$targetField]['label_fa']}' اختصاص یافت.",
                            'reason_en' => "Another source column ('{$winner['source_column']}') with higher confidence was assigned to '{$targetField}'.",
                        ];
                    }
                }
            }
        }

        // Step 5: Build final suggestions list preserving original header order
        $suggestions = [];
        $assignedTargets = [];

        foreach ($sourceHeaders as $index => $rawHeader) {
            $rawTrimmed = trim((string) $rawHeader);
            $candInfo = $headerCandidates[$index];

            if ($candInfo['is_ambiguous']) {
                $candNames = array_column($candInfo['top_candidates'], 'target_field');
                $candLabels = array_map(fn ($f) => $targetFields[$f]['label_fa'], $candNames);

                $suggestions[] = [
                    'source_column' => $rawTrimmed,
                    'source_index' => $index,
                    'target_field' => null,
                    'confidence' => null,
                    'match_category' => self::MATCH_AMBIGUOUS,
                    'is_ambiguous' => true,
                    'has_conflict' => false,
                    'matched_alias' => null,
                    'candidate_fields' => $candNames,
                    'competing_columns' => [],
                    'reason_fa' => "ستون با چندین فیلد مقصد (" . implode(' و ', array_map(fn ($l) => "'{$l}'", $candLabels)) . ") مطابقت یکسان دارد و به صورت خودکار تعیین نشد.",
                    'reason_en' => "Column matches multiple target fields (" . implode(', ', $candNames) . ") with equal confidence; left unmapped.",
                ];
            } elseif (isset($finalAssignments[$index])) {
                $assignment = $finalAssignments[$index];
                if ($assignment['target_field'] !== null) {
                    $assignedTargets[] = $assignment['target_field'];
                }

                $suggestions[] = [
                    'source_column' => $rawTrimmed,
                    'source_index' => $index,
                    'target_field' => $assignment['target_field'],
                    'confidence' => $assignment['confidence'],
                    'match_category' => $assignment['match_category'],
                    'is_ambiguous' => false,
                    'has_conflict' => $assignment['has_conflict'],
                    'matched_alias' => $assignment['matched_alias'],
                    'candidate_fields' => !empty($candInfo['candidates']) ? array_column($candInfo['candidates'], 'target_field') : [],
                    'competing_columns' => $assignment['competing_columns'],
                    'reason_fa' => $assignment['reason_fa'],
                    'reason_en' => $assignment['reason_en'],
                ];
            } else {
                // No match at all
                $suggestions[] = [
                    'source_column' => $rawTrimmed,
                    'source_index' => $index,
                    'target_field' => null,
                    'confidence' => null,
                    'match_category' => self::MATCH_UNMAPPED,
                    'is_ambiguous' => false,
                    'has_conflict' => false,
                    'matched_alias' => null,
                    'candidate_fields' => [],
                    'competing_columns' => [],
                    'reason_fa' => "هیچ فیلد مقصدی با اطمینان کافی تطبیق پیدا نکرد.",
                    'reason_en' => "No matching target field found with sufficient confidence.",
                ];
            }
        }

        // Step 6: Identify unmapped required fields
        $unmappedRequiredFields = [];
        foreach ($targetFields as $fieldName => $def) {
            if ($def['required'] && !in_array($fieldName, $assignedTargets, true)) {
                $unmappedRequiredFields[] = [
                    'name' => $fieldName,
                    'label_fa' => $def['label_fa'],
                    'label_en' => $def['label_en'],
                    'type' => $def['type'],
                    'description_fa' => $def['description_fa'],
                    'description_en' => $def['description_en'],
                ];
            }
        }

        return [
            'suggestions' => $suggestions,
            'conflicts' => $conflicts,
            'ambiguous_headers' => $ambiguousHeaders,
            'unmapped_required_fields' => $unmappedRequiredFields,
        ];
    }

    /**
     * Match a single raw header against a target field definition.
     * Evaluates exact canonical (1.0), exact alias (0.95), normalized (0.90), and fuzzy (0.80).
     *
     * @param array{
     *     name: string,
     *     label_fa: string,
     *     label_en: string,
     *     type: string,
     *     required: bool,
     *     description_fa: string,
     *     description_en: string,
     *     aliases: string[]
     * } $targetFieldDef
     * @return array{
     *     target_field: string,
     *     score: float,
     *     category: string,
     *     matched_alias: string,
     *     reason_fa: string,
     *     reason_en: string
     * }|null
     */
    protected function matchCandidate(string $rawHeader, string $targetFieldName, array $targetFieldDef): ?array
    {
        $rawLower = mb_strtolower(trim($rawHeader), 'UTF-8');
        $normHeader = $this->normalizeString($rawHeader);
        $compactHeader = $this->stripSeparators($rawHeader);

        // 1. Exact Canonical (score 1.0)
        if ($rawLower === mb_strtolower($targetFieldName, 'UTF-8')) {
            return [
                'target_field' => $targetFieldName,
                'score' => 1.0,
                'category' => self::MATCH_EXACT_CANONICAL,
                'matched_alias' => $targetFieldName,
                'reason_fa' => "تطبیق قطعی با شناسه فیلد ('{$targetFieldName}')",
                'reason_en' => "Exact match with canonical field identifier ('{$targetFieldName}')",
            ];
        }

        if ($rawLower === mb_strtolower(trim($targetFieldDef['label_fa']), 'UTF-8')) {
            return [
                'target_field' => $targetFieldName,
                'score' => 1.0,
                'category' => self::MATCH_EXACT_CANONICAL,
                'matched_alias' => $targetFieldDef['label_fa'],
                'reason_fa' => "تطبیق قطعی با عنوان رسمی فیلد ('{$targetFieldDef['label_fa']}')",
                'reason_en' => "Exact match with canonical Persian label ('{$targetFieldDef['label_fa']}')",
            ];
        }

        if ($rawLower === mb_strtolower(trim($targetFieldDef['label_en']), 'UTF-8')) {
            return [
                'target_field' => $targetFieldName,
                'score' => 1.0,
                'category' => self::MATCH_EXACT_CANONICAL,
                'matched_alias' => $targetFieldDef['label_en'],
                'reason_fa' => "تطبیق قطعی با عنوان انگلیسی فیلد ('{$targetFieldDef['label_en']}')",
                'reason_en' => "Exact match with canonical English label ('{$targetFieldDef['label_en']}')",
            ];
        }

        // 2. Exact Alias (score 0.95)
        foreach ($targetFieldDef['aliases'] as $alias) {
            if ($rawLower === mb_strtolower(trim($alias), 'UTF-8')) {
                return [
                    'target_field' => $targetFieldName,
                    'score' => 0.95,
                    'category' => self::MATCH_EXACT_ALIAS,
                    'matched_alias' => $alias,
                    'reason_fa' => "تطبیق دقیق با عنوان مترادف ('{$alias}')",
                    'reason_en' => "Exact match with alias ('{$alias}')",
                ];
            }
        }

        // 3. Normalized Match (score 0.90)
        if ($normHeader === $this->normalizeString($targetFieldName) ||
            $compactHeader === $this->stripSeparators($targetFieldName)) {
            return [
                'target_field' => $targetFieldName,
                'score' => 0.90,
                'category' => self::MATCH_NORMALIZED_ALIAS,
                'matched_alias' => $targetFieldName,
                'reason_fa' => "تطبیق نرمال‌شده با شناسه فیلد ('{$targetFieldName}')",
                'reason_en' => "Normalized match with canonical field identifier ('{$targetFieldName}')",
            ];
        }

        if ($normHeader === $this->normalizeString($targetFieldDef['label_fa']) ||
            $compactHeader === $this->stripSeparators($targetFieldDef['label_fa'])) {
            return [
                'target_field' => $targetFieldName,
                'score' => 0.90,
                'category' => self::MATCH_NORMALIZED_ALIAS,
                'matched_alias' => $targetFieldDef['label_fa'],
                'reason_fa' => "تطبیق نرمال‌شده با عنوان رسمی فیلد ('{$targetFieldDef['label_fa']}')",
                'reason_en' => "Normalized match with canonical label ('{$targetFieldDef['label_fa']}')",
            ];
        }

        foreach ($targetFieldDef['aliases'] as $alias) {
            $normAlias = $this->normalizeString($alias);
            $compactAlias = $this->stripSeparators($alias);

            if ($normHeader === $normAlias || $compactHeader === $compactAlias) {
                return [
                    'target_field' => $targetFieldName,
                    'score' => 0.90,
                    'category' => self::MATCH_NORMALIZED_ALIAS,
                    'matched_alias' => $alias,
                    'reason_fa' => "تطبیق نرمال‌شده با عنوان مترادف ('{$alias}')",
                    'reason_en' => "Normalized match with alias ('{$alias}')",
                ];
            }
        }

        // 4. Fuzzy High (score 0.80)
        $bestFuzzy = null;
        foreach ($targetFieldDef['aliases'] as $alias) {
            $normAlias = $this->normalizeString($alias);
            $aliasLen = mb_strlen($normAlias, 'UTF-8');
            $headerLen = mb_strlen($normHeader, 'UTF-8');

            if ($aliasLen >= 4 && $headerLen >= 4) {
                if (str_starts_with($normHeader, $normAlias) || str_ends_with($normHeader, $normAlias)) {
                    return [
                        'target_field' => $targetFieldName,
                        'score' => 0.80,
                        'category' => self::MATCH_FUZZY_HIGH,
                        'matched_alias' => $alias,
                        'reason_fa' => "شباهت متنی بالا با عنوان مترادف ('{$alias}')",
                        'reason_en' => "High textual similarity with alias ('{$alias}')",
                    ];
                }

                similar_text($normHeader, $normAlias, $percent);
                if ($percent >= 85.0) {
                    if ($bestFuzzy === null || $percent > $bestFuzzy['percent']) {
                        $bestFuzzy = [
                            'percent' => $percent,
                            'target_field' => $targetFieldName,
                            'score' => 0.80,
                            'category' => self::MATCH_FUZZY_HIGH,
                            'matched_alias' => $alias,
                            'reason_fa' => "شباهت نوشتاری بالا (" . round($percent) . "٪) با عنوان مترادف ('{$alias}')",
                            'reason_en' => "High textual similarity (" . round($percent) . "%) with alias ('{$alias}')",
                        ];
                    }
                }
            }
        }

        if ($bestFuzzy !== null) {
            return [
                'target_field' => $bestFuzzy['target_field'],
                'score' => $bestFuzzy['score'],
                'category' => $bestFuzzy['category'],
                'matched_alias' => $bestFuzzy['matched_alias'],
                'reason_fa' => $bestFuzzy['reason_fa'],
                'reason_en' => $bestFuzzy['reason_en'],
            ];
        }

        return null;
    }

    /**
     * Validate user-confirmed mapping configuration.
     *
     * @param array<string, int|string> $mapping Map of target_field => source_column_name or source_index
     * @return array{valid: bool, errors: string[]}
     */
    public function validateMapping(array $mapping, string $importType): array
    {
        $targetFields = $this->getTargetFields($importType);
        $errors = [];

        // 1. Check required fields
        foreach ($targetFields as $fieldName => $def) {
            if ($def['required']) {
                $isMapped = false;
                foreach ($mapping as $target => $source) {
                    if ($target === $fieldName && $source !== null && $source !== '') {
                        $isMapped = true;
                        break;
                    }
                }
                if (!$isMapped) {
                    $errors[] = "فیلد ضروری '{$def['label_fa']}' ({$def['label_en']}) انتخاب نشده است.";
                }
            }
        }

        // 2. Check for duplicate assignments of same source to multiple fields or same field to multiple sources
        $seenSources = [];
        $seenTargets = [];
        foreach ($mapping as $target => $source) {
            if ($source === null || $source === '') {
                continue;
            }

            if (!isset($targetFields[$target])) {
                $errors[] = "فیلد مقصد '{$target}' در این نوع داده نامعتبر است.";
                continue;
            }

            if (in_array($target, $seenTargets, true)) {
                $errors[] = "فیلد مقصد '{$targetFields[$target]['label_fa']}' بیش از یک بار تطبیق داده شده است.";
            }
            $seenTargets[] = $target;

            if (in_array($source, $seenSources, true)) {
                $errors[] = "ستون منبع '{$source}' به بیش از یک فیلد مقصد اختصاص یافته است.";
            }
            $seenSources[] = $source;
        }

        return [
            'valid' => empty($errors),
            'errors' => $errors,
        ];
    }

    /**
     * Clean and normalize string for robust alias matching.
     */
    public function normalizeString(string $val): string
    {
        $val = trim($val);

        // Remove Arabic / Persian diacritics (harakat/erab)
        $val = preg_replace('/[\x{064B}-\x{065F}\x{0670}]/u', '', $val);

        // Normalize Persian letters (Arabic Ye -> Persian Ye, Kaf -> Persian Kaf, etc.)
        $val = str_replace(
            ['ي', 'ك', 'ة', 'ئ', 'أ', 'إ', 'آ', 'ٱ', 'ؤ', "\u{200C}", 'ـ'],
            ['ی', 'ک', 'ه', 'ی', 'ا', 'ا', 'ا', 'ا', 'و', ' ', ''],
            $val
        );

        // Convert Persian & Arabic numbers to English
        $persianNums = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
        $arabicNums  = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
        $englishNums = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
        $val = str_replace($persianNums, $englishNums, $val);
        $val = str_replace($arabicNums, $englishNums, $val);

        // Replace common punctuation and symbols with spaces
        $val = preg_replace('/[_\-\(\)\[\]\{\}\:\/\#\*\.\,\،\\\]+/u', ' ', $val);

        // Collapse multiple spaces and lowercase
        $val = preg_replace('/\s+/u', ' ', $val);

        return mb_strtolower(trim($val), 'UTF-8');
    }

    /**
     * Remove all whitespaces and separators for compact compound matching.
     */
    public function stripSeparators(string $val): string
    {
        $val = $this->normalizeString($val);
        return preg_replace('/[\s_\-]+/u', '', $val);
    }
}
