<?php

namespace App\Support\Import;

class ImportErrorCode
{
    public const FILE_CORRUPTED = 'ERR_FILE_CORRUPTED';
    public const FILE_TOO_LARGE = 'ERR_FILE_TOO_LARGE';
    public const FILE_FORMAT_UNSUPPORTED = 'ERR_FILE_FORMAT_UNSUPPORTED';
    public const SUSPICIOUS_CONTENT = 'ERR_SUSPICIOUS_CONTENT';
    public const MACRO_DETECTED = 'ERR_MACRO_DETECTED';
    public const NO_DATA_FOUND = 'ERR_NO_DATA_FOUND';
    public const MISSING_REQUIRED_HEADER = 'ERR_MISSING_REQUIRED_HEADER';
    public const DUPLICATE_HEADER = 'ERR_DUPLICATE_HEADER';
    public const INVALID_FIELD_TYPE = 'ERR_INVALID_FIELD_TYPE';
    public const MISSING_REQUIRED_FIELD = 'ERR_MISSING_REQUIRED_FIELD';
    public const REQUIRED_FIELD_MISSING = 'ERR_MISSING_REQUIRED_FIELD';
    public const INVALID_DATE_FORMAT = 'ERR_INVALID_DATE_FORMAT';
    public const INVALID_NUMBER_FORMAT = 'ERR_INVALID_NUMBER_FORMAT';
    public const NUMERIC_INVALID = 'ERR_INVALID_NUMBER_FORMAT';
    public const STRING_TOO_LONG = 'ERR_STRING_TOO_LONG';
    public const DUPLICATE_KEY_CANDIDATE = 'WARN_DUPLICATE_KEY_CANDIDATE';
    public const FOREIGN_ENTITY_NOT_FOUND = 'ERR_FOREIGN_ENTITY_NOT_FOUND';
    public const ROW_LIMIT_EXCEEDED = 'ERR_ROW_LIMIT_EXCEEDED';
    public const PARSING_FAILED = 'ERR_PARSING_FAILED';
    public const INVALID_UNIT = 'ERR_INVALID_UNIT';
    public const INVALID_PHONE = 'ERR_INVALID_PHONE';
    public const INVALID_EMAIL = 'ERR_INVALID_EMAIL';
    public const DUPLICATE_IN_FILE = 'ERR_DUPLICATE_IN_FILE';
    public const EXISTING_RECORD_MATCH = 'WARN_EXISTING_RECORD_MATCH';
    public const AMBIGUOUS_MAPPING = 'ERR_AMBIGUOUS_MAPPING';
    public const PRODUCT_NOT_FOUND = 'ERR_PRODUCT_NOT_FOUND';
    public const AMBIGUOUS_PRODUCT = 'ERR_AMBIGUOUS_PRODUCT';
    public const WAREHOUSE_NOT_FOUND = 'ERR_WAREHOUSE_NOT_FOUND';
    public const INVALID_QUANTITY = 'ERR_INVALID_QUANTITY';
    public const EXISTING_BALANCE_CONFLICT = 'WARN_EXISTING_BALANCE_CONFLICT';
    public const INVALID_CATEGORY = 'ERR_INVALID_CATEGORY';
    public const INVALID_AMOUNT = 'ERR_INVALID_AMOUNT';
    public const STORE_NOT_FOUND = 'ERR_STORE_NOT_FOUND';
    public const INVALID_DATE = 'ERR_INVALID_DATE_FORMAT';
    public const ORDER_ID_REQUIRED = 'ERR_ORDER_ID_REQUIRED';
    public const ORDER_TOTAL_MISMATCH = 'ERR_ORDER_TOTAL_MISMATCH';
    public const ORDER_LINE_INCONSISTENCY = 'ERR_ORDER_LINE_INCONSISTENCY';
    public const INVALID_ORDER_STATUS = 'ERR_INVALID_ORDER_STATUS';
    public const SUPPLIER_NOT_FOUND = 'ERR_SUPPLIER_NOT_FOUND';
    public const PURCHASE_ID_REQUIRED = 'ERR_PURCHASE_ID_REQUIRED';
    public const PURCHASE_TOTAL_MISMATCH = 'ERR_PURCHASE_TOTAL_MISMATCH';
    public const PURCHASE_LINE_INCONSISTENCY = 'ERR_PURCHASE_LINE_INCONSISTENCY';
    public const INVALID_PURCHASE_STATUS = 'ERR_INVALID_PURCHASE_STATUS';

    public static function persianMessage(string $code, array $params = []): string
    {
        if ($code === self::REQUIRED_FIELD_MISSING && isset($params['field'])) {
            return "فیلد اجباری '{$params['field']}' مقداردهی نشده است.";
        }
        return self::defaultMessage($code, 'fa');
    }

    /**
     * Get default localized message for an error code.
     */
    public static function defaultMessage(string $code, string $locale = 'fa'): string
    {
        $messages = [
            self::FILE_CORRUPTED => [
                'fa' => 'فایل بارگذاری‌شده مخدوش است و امکان خواندن آن وجود ندارد.',
                'en' => 'The uploaded file is corrupted and cannot be read.',
            ],
            self::FILE_TOO_LARGE => [
                'fa' => 'حجم فایل از حد مجاز بیشتر است.',
                'en' => 'The file size exceeds the allowed limit.',
            ],
            self::FILE_FORMAT_UNSUPPORTED => [
                'fa' => 'فرمت فایل پشتیبانی نمی‌شود. لطفاً فایل اکسل (.xlsx) یا CSV بارگذاری کنید.',
                'en' => 'Unsupported file format. Please upload an Excel (.xlsx) or CSV file.',
            ],
            self::SUSPICIOUS_CONTENT => [
                'fa' => 'محتوای نامعتبر یا مشکوک در فایل شناسایی شد.',
                'en' => 'Invalid or suspicious content detected in the file.',
            ],
            self::MACRO_DETECTED => [
                'fa' => 'فایل‌های حاوی ماکرو یا اسکریپت به دلایل امنیتی پذیرفته نمی‌شوند.',
                'en' => 'Macro-enabled spreadsheets are rejected for security reasons.',
            ],
            self::NO_DATA_FOUND => [
                'fa' => 'هیچ ردیف داده معتبری در کاربرگ یافت نشد.',
                'en' => 'No valid data rows found in the worksheet.',
            ],
            self::MISSING_REQUIRED_HEADER => [
                'fa' => 'سرستون‌های ضروری در فایل یافت نشدند.',
                'en' => 'Required column headers were not found in the file.',
            ],
            self::DUPLICATE_HEADER => [
                'fa' => 'سرستون‌های تکراری در فایل وجود دارد.',
                'en' => 'Duplicate column headers found in the file.',
            ],
            self::INVALID_FIELD_TYPE => [
                'fa' => 'نوع داده ستون با فیلد مورد نظر همخوانی ندارد.',
                'en' => 'Column data type does not match the target field.',
            ],
            self::MISSING_REQUIRED_FIELD => [
                'fa' => 'فیلد اجباری مقداردهی نشده است.',
                'en' => 'Required field is missing.',
            ],
            self::INVALID_DATE_FORMAT => [
                'fa' => 'فرمت تاریخ معتبر نیست.',
                'en' => 'Invalid date format.',
            ],
            self::INVALID_NUMBER_FORMAT => [
                'fa' => 'فرمت عدد نامعتبر است.',
                'en' => 'Invalid number format.',
            ],
            self::DUPLICATE_KEY_CANDIDATE => [
                'fa' => 'شناسه این ردیف قبلاً در سامانه ثبت شده است.',
                'en' => 'This row identifier is already registered in the system.',
            ],
            self::FOREIGN_ENTITY_NOT_FOUND => [
                'fa' => 'موجودیت مرتبط (دسته‌بندی، تأمین‌کننده یا انبار) در سیستم یافت نشد.',
                'en' => 'Related entity was not found in the system.',
            ],
            self::ROW_LIMIT_EXCEEDED => [
                'fa' => 'تعداد ردیف‌ها بیش از حد مجاز یک فایل مهاجرت است.',
                'en' => 'Number of rows exceeds the migration file limit.',
            ],
            self::PARSING_FAILED => [
                'fa' => 'خطا در پردازش محتوای فایل.',
                'en' => 'Error parsing file content.',
            ],
            self::INVALID_UNIT => [
                'fa' => 'واحد کالا در سامانه تعریف نشده است. لطفاً از واحدهای مجاز (مانند عدد، کیلوگرم، بسته و ...) استفاده کنید.',
                'en' => 'Invalid unit of measurement. Please use a recognized unit (e.g. piece, kilogram, pack, etc.).',
            ],
            self::INVALID_PHONE => [
                'fa' => 'شماره تلفن معتبر نیست.',
                'en' => 'Invalid phone number format.',
            ],
            self::INVALID_EMAIL => [
                'fa' => 'فرمت ایمیل نامعتبر است.',
                'en' => 'Invalid email address format.',
            ],
            self::DUPLICATE_IN_FILE => [
                'fa' => 'این رکورد در ردیف دیگری از همین فایل تکرار شده است.',
                'en' => 'This record is duplicated within the source file.',
            ],
            self::EXISTING_RECORD_MATCH => [
                'fa' => 'رکورد مشابهی از قبل در سامانه ثبت شده است.',
                'en' => 'A matching record already exists in the system.',
            ],
            self::AMBIGUOUS_MAPPING => [
                'fa' => 'تطبیق ستون با ابهام مواجه است و نیازمند بررسی توسط کاربر می‌باشد.',
                'en' => 'Column mapping is ambiguous and requires user review.',
            ],
            self::PRODUCT_NOT_FOUND => [
                'fa' => 'کالایی با این شناسه/بارکد در سیستم یافت نشد. موجودی اولیه فقط برای کالاهای از قبل ثبت‌شده امکان‌پذیر است.',
                'en' => 'Product was not found in the system. Opening inventory can only be imported for pre-existing products.',
            ],
            self::AMBIGUOUS_PRODUCT => [
                'fa' => 'چند کالا با این مشخصات یافت شد و شناسه محصول مبهم است.',
                'en' => 'Multiple products match this identifier, leading to ambiguity.',
            ],
            self::WAREHOUSE_NOT_FOUND => [
                'fa' => 'انبار مشخص‌شده در سیستم یافت نشد یا در دسترس نیست.',
                'en' => 'The specified warehouse was not found or is inaccessible.',
            ],
            self::INVALID_QUANTITY => [
                'fa' => 'مقدار موجودی نامعتبر است. مقدار باید عدد صحیح مثبت (بزرگتر از صفر) باشد.',
                'en' => 'Invalid quantity. Must be a positive integer greater than zero.',
            ],
            self::EXISTING_BALANCE_CONFLICT => [
                'fa' => 'این کالا از قبل در این انبار دارای موجودی ثبت‌شده است.',
                'en' => 'This product already has an existing stock balance in this warehouse.',
            ],
            self::INVALID_CATEGORY => [
                'fa' => 'دسته‌بندی هزینه نامعتبر است. دسته‌بندی باید یکی از موارد مجاز سیستم (اجاره، اینترنت، برق، آب، حمل‌ونقل، تبلیغات، حقوق، تعمیرات، خدمات، کارمزد، سایر) باشد.',
                'en' => 'Invalid expense category. Must be one of the recognized categories.',
            ],
            self::INVALID_AMOUNT => [
                'fa' => 'مبلغ هزینه نامعتبر است. مبلغ باید عددی مثبت و بزرگتر از صفر باشد.',
                'en' => 'Invalid expense amount. Must be a positive number greater than zero.',
            ],
            self::STORE_NOT_FOUND => [
                'fa' => 'فروشگاه مشخص‌شده در سیستم یافت نشد یا به این سازمان تعلق ندارد.',
                'en' => 'The specified store was not found or does not belong to this organization.',
            ],
            self::ORDER_ID_REQUIRED => [
                'fa' => 'شناسه سفارش خارجی برای گروه‌بندی اقلام سفارش الزامی است.',
                'en' => 'External order identifier is required to group order line items.',
            ],
            self::ORDER_TOTAL_MISMATCH => [
                'fa' => 'مجموع مبالغ اقلام، تخفیف و مالیات با مبلغ کل سفارش همخوانی ندارد.',
                'en' => 'The sum of line items, discounts, and taxes does not match total amount.',
            ],
            self::ORDER_LINE_INCONSISTENCY => [
                'fa' => 'اقلام این سفارش دارای تاریخ، مشتری یا فروشگاه متناقض هستند.',
                'en' => 'Line items for this order have conflicting dates, customers, or stores.',
            ],
            self::INVALID_ORDER_STATUS => [
                'fa' => 'وضعیت سفارش نامعتبر است (وضعیت‌های مجاز: COMPLETED, NEW, PREPARING, CANCELLED).',
                'en' => 'Invalid order status (allowed: COMPLETED, NEW, PREPARING, CANCELLED).',
            ],
            self::SUPPLIER_NOT_FOUND => [
                'fa' => 'تأمین‌کننده در سیستم یافت نشد یا به این سازمان تعلق ندارد.',
                'en' => 'Supplier was not found or does not belong to this organization.',
            ],
            self::PURCHASE_ID_REQUIRED => [
                'fa' => 'شناسه خرید یا شماره فاکتور برای گروه‌بندی اقلام خرید الزامی است.',
                'en' => 'Purchase identifier or invoice number is required to group purchase line items.',
            ],
            self::PURCHASE_TOTAL_MISMATCH => [
                'fa' => 'مجموع مبالغ اقلام، تخفیف و مالیات با مبلغ کل خرید همخوانی ندارد.',
                'en' => 'The sum of line items, discounts, and taxes does not match total purchase amount.',
            ],
            self::PURCHASE_LINE_INCONSISTENCY => [
                'fa' => 'اقلام این خرید دارای تاریخ، تأمین‌کننده، انبار یا وضعیت متناقض هستند.',
                'en' => 'Line items for this purchase have conflicting dates, suppliers, warehouses, or statuses.',
            ],
            self::INVALID_PURCHASE_STATUS => [
                'fa' => 'وضعیت خرید نامعتبر است (وضعیت‌های مجاز: RECEIVED, ORDERED, PARTIALLY_RECEIVED, CANCELLED).',
                'en' => 'Invalid purchase status (allowed: RECEIVED, ORDERED, PARTIALLY_RECEIVED, CANCELLED).',
            ],
        ];

        return $messages[$code][$locale] ?? $messages[$code]['en'] ?? $code;
    }
}
