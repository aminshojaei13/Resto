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
}
