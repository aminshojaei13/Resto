<?php

namespace App\Support\Import;

class FileFormat
{
    public const XLSX = 'xlsx';
    public const CSV = 'csv';

    public const SUPPORTED = [
        self::XLSX,
        self::CSV,
    ];

    public static function isSupported(string $format): bool
    {
        return in_array(strtolower($format), self::SUPPORTED, true);
    }
}
