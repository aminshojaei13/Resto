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
    public const INVALID_DATE_FORMAT = 'ERR_INVALID_DATE_FORMAT';
    public const INVALID_NUMBER_FORMAT = 'ERR_INVALID_NUMBER_FORMAT';
    public const DUPLICATE_KEY_CANDIDATE = 'WARN_DUPLICATE_KEY_CANDIDATE';
    public const FOREIGN_ENTITY_NOT_FOUND = 'ERR_FOREIGN_ENTITY_NOT_FOUND';
    public const ROW_LIMIT_EXCEEDED = 'ERR_ROW_LIMIT_EXCEEDED';
    public const PARSING_FAILED = 'ERR_PARSING_FAILED';

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
        ];

        return $messages[$code][$locale] ?? $messages[$code]['en'] ?? $code;
    }
}
