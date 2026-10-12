<?php

namespace App\Services\Import\Mapping;

use App\Support\Import\ImportErrorCode;
use App\Support\Import\ImportType;
use InvalidArgumentException;

class ImportMappingService
{
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
                        'نام', 'نام کالا', 'نام محصول', 'عنوان', 'کالا', 'محصول', 'شرح کالا',
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
                        'کد', 'کد کالا', 'کد محصول', 'شناسه', 'شناسه کالا', 'کد فنی', 'کد انبار',
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

        throw new InvalidArgumentException("نوع درون‌ریزی '{$importType}' برای تطبیق ستون‌ها پشتیبانی نمی‌شود.");
    }

    /**
     * Generate deterministic mapping suggestions for a list of source headers.
     *
     * @param string[] $sourceHeaders
     * @return array<int, array{
     *     source_column: string,
     *     source_index: int,
     *     target_field: string|null,
     *     confidence: float|null
     * }>
     */
    public function suggestMappings(array $sourceHeaders, string $importType): array
    {
        $targetFields = $this->getTargetFields($importType);
        $assignedTargets = [];
        $suggestions = [];

        foreach ($sourceHeaders as $index => $rawHeader) {
            $normalizedHeader = $this->normalizeString($rawHeader);
            $bestMatchField = null;
            $bestConfidence = 0.0;

            foreach ($targetFields as $fieldName => $def) {
                // If this field is already assigned, don't assign it again
                if (in_array($fieldName, $assignedTargets, true)) {
                    continue;
                }

                // Check exact field name match
                if ($normalizedHeader === $this->normalizeString($fieldName)) {
                    $bestMatchField = $fieldName;
                    $bestConfidence = 1.0;
                    break;
                }

                // Check aliases
                foreach ($def['aliases'] as $alias) {
                    $normAlias = $this->normalizeString($alias);
                    if ($normalizedHeader === $normAlias) {
                        $bestMatchField = $fieldName;
                        $bestConfidence = 1.0;
                        break 2;
                    }

                    // Substring match with high threshold
                    if (strlen($normAlias) > 3 && (
                        str_starts_with($normalizedHeader, $normAlias) ||
                        str_ends_with($normalizedHeader, $normAlias)
                    )) {
                        if ($bestConfidence < 0.8) {
                            $bestMatchField = $fieldName;
                            $bestConfidence = 0.8;
                        }
                    }
                }
            }

            if ($bestMatchField !== null) {
                $assignedTargets[] = $bestMatchField;
                $suggestions[] = [
                    'source_column' => trim($rawHeader),
                    'source_index' => $index,
                    'target_field' => $bestMatchField,
                    'confidence' => $bestConfidence,
                ];
            } else {
                $suggestions[] = [
                    'source_column' => trim($rawHeader),
                    'source_index' => $index,
                    'target_field' => null,
                    'confidence' => null,
                ];
            }
        }

        return $suggestions;
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
        // Normalize Persian letters (Arabic Ye -> Persian Ye, etc.)
        $val = str_replace(
            ['ي', 'ك', 'ة', 'ئ', 'أ', 'إ', 'آ', "\u{200C}", 'ـ'],
            ['ی', 'ک', 'ه', 'ی', 'ا', 'ا', 'ا', ' ', ''],
            $val
        );

        // Convert Persian & Arabic numbers to English
        $persianNums = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
        $arabicNums  = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
        $englishNums = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
        $val = str_replace($persianNums, $englishNums, $val);
        $val = str_replace($arabicNums, $englishNums, $val);

        // Replace common punctuation with spaces
        $val = preg_replace('/[_\-\(\)\[\]\{\}\:\/\#\*\.]+/u', ' ', $val);

        // Collapse multiple spaces and lowercase
        $val = preg_replace('/\s+/u', ' ', $val);

        return mb_strtolower(trim($val), 'UTF-8');
    }
}
