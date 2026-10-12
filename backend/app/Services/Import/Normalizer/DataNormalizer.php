<?php

namespace App\Services\Import\Normalizer;

use App\Support\UnitCatalog;

class DataNormalizer
{
    /**
     * Clean basic string: trim, normalize whitespace and control characters, UTF-8 check.
     */
    public static function cleanString(?string $value): string
    {
        if ($value === null) {
            return '';
        }

        $val = trim($value);

        if (!mb_check_encoding($val, 'UTF-8')) {
            $val = mb_convert_encoding($val, 'UTF-8', 'ISO-8859-1');
        }

        // Normalize Persian letters (Arabic Ye -> Persian Ye, Kaf -> Persian Kaf, etc.)
        $val = str_replace(
            ['ي', 'ك', 'ة', 'ئ', 'أ', 'إ', 'آ', "\u{200C}", 'ـ'],
            ['ی', 'ک', 'ه', 'ی', 'ا', 'ا', 'ا', ' ', ''],
            $val
        );

        // Remove unprintable control characters except standard whitespace
        $val = preg_replace('/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/u', '', $val);

        // Normalize multiple spaces into single space
        $val = preg_replace('/\s+/u', ' ', $val);

        return trim($val);
    }

    /**
     * Convert Persian and Arabic digits to English digits.
     */
    public static function toEnglishDigits(string $value): string
    {
        $persianNums = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
        $arabicNums  = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
        $englishNums = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];

        $value = str_replace($persianNums, $englishNums, $value);
        return str_replace($arabicNums, $englishNums, $value);
    }

    /**
     * Parse monetary or numeric amount to exact 2-decimal numeric string (e.g. "15000.00").
     * Returns null if value is empty or not a valid number.
     */
    public static function parseMoney(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return null;
        }

        $cleaned = self::toEnglishDigits(trim($value));

        // Replace Persian/Arabic decimal separator '٫' or comma when used as decimal, or currency names
        $cleaned = str_replace(['٫', 'ریال', 'تومان', 'تومن', 'IRR', 'TOMAN', '$'], ['.', '', '', '', '', '', ''], $cleaned);

        // Remove thousands separators (commas and spaces)
        // If there's a dot, remove all commas. If there are commas and no dot, check if comma is decimal or thousands
        if (str_contains($cleaned, '.')) {
            $cleaned = str_replace([',', ' '], '', $cleaned);
        } else {
            // Count commas: if 1 comma and followed by 1 or 2 digits at end, could be decimal, otherwise separator
            if (preg_match('/^(\d+),(\d{1,2})$/', $cleaned, $matches)) {
                $cleaned = $matches[1] . '.' . $matches[2];
            } else {
                $cleaned = str_replace([',', ' '], '', $cleaned);
            }
        }

        $cleaned = trim($cleaned);

        if (!is_numeric($cleaned)) {
            return null;
        }

        $floatVal = (float) $cleaned;
        if ($floatVal < 0) {
            return null; // Prices cannot be negative
        }

        return number_format($floatVal, 2, '.', '');
    }

    /**
     * Normalize and validate Iranian and international phone numbers.
     * Returns normalized phone string e.g. "09123456789" or null if invalid.
     */
    public static function normalizePhone(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return null;
        }

        $cleaned = self::toEnglishDigits(trim($value));
        // Remove spaces, dashes, parentheses
        $cleaned = preg_replace('/[\s\-\(\)\.\+]/', '', $cleaned);

        if ($cleaned === '') {
            return null;
        }

        // Handle +98 or 0098 prefix
        if (str_starts_with($cleaned, '0098')) {
            $cleaned = '0' . substr($cleaned, 4);
        } elseif (str_starts_with($cleaned, '98') && strlen($cleaned) >= 12) {
            $cleaned = '0' . substr($cleaned, 2);
        }

        // If 10 digits starting with 9 (e.g. 9123456789), prepend 0
        if (strlen($cleaned) === 10 && str_starts_with($cleaned, '9')) {
            $cleaned = '0' . $cleaned;
        }

        // Validate: must be digits only and length between 7 and 15
        if (!preg_match('/^\d{7,15}$/', $cleaned)) {
            return null;
        }

        return $cleaned;
    }

    /**
     * Normalize and validate email address.
     */
    public static function normalizeEmail(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return null;
        }

        $cleaned = mb_strtolower(trim($value), 'UTF-8');
        if (filter_var($cleaned, FILTER_VALIDATE_EMAIL)) {
            return $cleaned;
        }

        return null;
    }

    /**
     * Normalize product unit of measure using UnitCatalog.
     * Returns canonical unit code (e.g. 'piece', 'kilogram') or null if invalid.
     */
    public static function normalizeUnit(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return UnitCatalog::DEFAULT_UNIT;
        }

        $cleaned = self::cleanString($value);
        return UnitCatalog::normalize($cleaned);
    }

    /**
     * Parse date string (Persian Jalali, Gregorian, or Excel numeric serial) to standard 'YYYY-MM-DD'.
     * Returns null if invalid or cannot be parsed.
     */
    public static function parseDate(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return null;
        }

        $raw = self::cleanString(self::toEnglishDigits($value));
        if ($raw === '') {
            return null;
        }

        // 1. Handle numeric Excel date serials (e.g. 45230)
        if (is_numeric($raw) && (int) $raw > 10000 && (int) $raw < 100000 && !str_contains($raw, '-') && !str_contains($raw, '/')) {
            $days = (int) $raw;
            // Excel epoch starts at 1899-12-30 (accounting for the Lotus 1-2-3 leap year bug)
            $timestamp = ($days - 25569) * 86400;
            return gmdate('Y-m-d', $timestamp);
        }

        // 2. Normalize separators: convert '/' and '.' to '-'
        $normalized = str_replace(['/', '.'], '-', $raw);

        // Pattern: YYYY-MM-DD
        if (preg_match('/^(\d{4})-(\d{1,2})-(\d{1,2})$/', $normalized, $matches)) {
            $y = (int) $matches[1];
            $m = (int) $matches[2];
            $d = (int) $matches[3];

            // Shamsi/Jalali year detection (1300 to 1500)
            if ($y >= 1300 && $y <= 1500) {
                return self::jalaliToGregorian($y, $m, $d);
            }

            // Gregorian year (1900 to 2100)
            if ($y >= 1900 && $y <= 2100 && checkdate($m, $d, $y)) {
                return sprintf('%04d-%02d-%02d', $y, $m, $d);
            }

            return null;
        }

        // Pattern: DD-MM-YYYY
        if (preg_match('/^(\d{1,2})-(\d{1,2})-(\d{4})$/', $normalized, $matches)) {
            $d = (int) $matches[1];
            $m = (int) $matches[2];
            $y = (int) $matches[3];

            if ($y >= 1300 && $y <= 1500) {
                return self::jalaliToGregorian($y, $m, $d);
            }

            if ($y >= 1900 && $y <= 2100 && checkdate($m, $d, $y)) {
                return sprintf('%04d-%02d-%02d', $y, $m, $d);
            }

            return null;
        }

        // Try standard PHP strtotime / Carbon for other standard date formats,
        // but strictly reject pure numeric, negative or nonsensical strings.
        if (preg_match('/^[a-zA-Z0-9\s,\.\-\/]+$/', $raw) && !is_numeric($raw) && !str_starts_with($raw, '-')) {
            try {
                $parsed = \Illuminate\Support\Carbon::parse($raw);
                if ($parsed && $parsed->year >= 1900 && $parsed->year <= 2100) {
                    return $parsed->format('Y-m-d');
                }
            } catch (\Throwable) {
                // Unparseable date
            }
        }

        return null;
    }

    /**
     * Convert Persian Jalali date (Year, Month, Day) to Gregorian 'YYYY-MM-DD'.
     */
    public static function jalaliToGregorian(int $jy, int $jm, int $jd): ?string
    {
        if ($jy < 1200 || $jy > 1600 || $jm < 1 || $jm > 12 || $jd < 1 || $jd > 31) {
            return null;
        }
        if ($jm > 6 && $jd > 30) {
            return null;
        }

        $jy -= 979;
        $jm -= 1;
        $jd -= 1;

        $j_day_no = 365 * $jy + intdiv($jy, 33) * 8 + intdiv(($jy % 33) + 3, 4);
        for ($i = 0; $i < $jm; ++$i) {
            $j_day_no += ($i < 6) ? 31 : 30;
        }
        $j_day_no += $jd;

        $g_day_no = $j_day_no + 79;

        $gy = 1600 + 400 * intdiv($g_day_no, 146097);
        $g_day_no %= 146097;

        $leap = true;
        if ($g_day_no >= 36525) {
            $g_day_no--;
            $gy += 100 * intdiv($g_day_no, 36524);
            $g_day_no %= 36524;

            if ($g_day_no >= 365) {
                $g_day_no++;
            } else {
                $leap = false;
            }
        }

        $gy += 4 * intdiv($g_day_no, 1461);
        $g_day_no %= 1461;

        if ($g_day_no >= 366) {
            $leap = false;
            $g_day_no--;
            $gy += intdiv($g_day_no, 365);
            $g_day_no %= 365;
        }

        $g_days = [31, ($leap ? 29 : 28), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        for ($gm = 0; $gm < 12 && $g_day_no >= $g_days[$gm]; $gm++) {
            $g_day_no -= $g_days[$gm];
        }

        $gd = $g_day_no + 1;
        $gm += 1;

        if (!checkdate($gm, $gd, $gy)) {
            return null;
        }

        return sprintf('%04d-%02d-%02d', $gy, $gm, $gd);
    }

    /**
     * Resolve expense category from user inputs (Persian label, English label, or code).
     */
    public static function resolveExpenseCategory(?string $value): ?string
    {
        if ($value === null || trim($value) === '') {
            return null;
        }

        $clean = self::cleanString($value);
        $lower = mb_strtolower($clean, 'UTF-8');
        $stripped = str_replace([' ', '‌', '_', '-'], '', $lower);

        $categories = [
            'RENT' => ['rent', 'اجاره', 'کرایه', 'اجارهبها', 'اجارهمغازه', 'اجارهمحل'],
            'INTERNET' => ['internet', 'اینترنت', 'اینترنتوتلفن', 'تلفن', 'خطتلفن', 'مودم', 'adsl', 'phone'],
            'ELECTRICITY' => ['electricity', 'برق', 'قبضبرق', 'برقمغازه', 'power'],
            'WATER' => ['water', 'آب', 'اب', 'قبضآب', 'قبضاب', 'آبوفاضلاب'],
            'TRANSPORT' => ['transport', 'حملونقل', 'حملونقل', 'ایابوذهاب', 'پیک', 'اسنپ', 'باربری', 'کرایهحمل', 'تاکسی', 'سوخت', 'بنزین', 'shipping'],
            'ADVERTISING' => ['advertising', 'تبلیغات', 'آگهی', 'تبلیغ', 'مارکتینگ', 'بازاریابی', 'بنر', 'چاپ', 'marketing', 'ads'],
            'SALARIES' => ['salaries', 'حقوق', 'دستمزد', 'حقوقودستمزد', 'پرسنل', 'کارمندان', 'پاداش', 'مزایا', 'salary', 'payroll', 'wage'],
            'REPAIRS' => ['repairs', 'تعمیرات', 'تعمیر', 'نگهداری', 'بازسازی', 'نوسازی', 'repair', 'maintenance'],
            'SERVICES' => ['services', 'خدمات', 'نظافت', 'شارژ', 'شارژساختمان', 'قبضگاز', 'گاز', 'سرویس', 'service', 'cleaning'],
            'FEES' => ['fees', 'کارمزد', 'کارمزدبانکی', 'بانک', 'حقاشتراک', 'مالیاتبرارزشافزوده', 'کمیسیون', 'fee', 'bankfee'],
            'OTHER' => ['other', 'سایرهزینهها', 'سایرهزینهها', 'سایر', 'متفرقه', 'هزینههایمتفرقه', 'غیره', 'misc', 'general'],
        ];

        foreach ($categories as $code => $keywords) {
            if (strcasecmp($code, $clean) === 0) {
                return $code;
            }
            foreach ($keywords as $kw) {
                if ($stripped === $kw || str_contains($stripped, $kw)) {
                    return $code;
                }
            }
        }

        return null;
    }
}
