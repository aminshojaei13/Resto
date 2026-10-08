<?php

namespace App\Support;

/**
 * The unit of measurement a business counts stock in.
 *
 * The catalog already had exactly one field — `products.unit` — and it is
 * still the only one. What it lacked was a controlled vocabulary, so people
 * typed "pcs", "PCs", "عدد", "kg" and got four different spellings of the same
 * thing spread across orders and reports.
 *
 * This class is the single place that decides which spellings mean the same
 * unit, and what each unit is called in Persian and English. No conversion
 * between units is offered or implied: Resto counts a product in exactly the
 * unit the product declares, and that is stated everywhere the quantity shows.
 *
 * Internal codes are stored in `products.unit`. They are never shown to a
 * person; `label()` renders the human wording.
 */
class UnitCatalog
{
    /**
     * code => [Persian label, English label].
     *
     * Order is the order the picker offers them in: counted items first, then
     * weight/volume/length, then packaging, then prepared-service units.
     */
    private const UNITS = [
        'piece' => ['عدد', 'Piece'],
        'kilogram' => ['کیلوگرم', 'Kilogram'],
        'gram' => ['گرم', 'Gram'],
        'liter' => ['لیتر', 'Liter'],
        'meter' => ['متر', 'Meter'],
        'pack' => ['بسته', 'Pack'],
        'box' => ['جعبه', 'Box'],
        'bottle' => ['بطری', 'Bottle'],
        'set' => ['دست', 'Set'],
        'serving' => ['سرو', 'Serving'],
        'order' => ['سفارش', 'Order'],
    ];

    /**
     * Spellings that mean an existing unit.
     *
     * These are recognition only. Normalising "PCS" to `piece` does not change
     * any quantity — `piece` and `pcs` were always counted the same way.
     */
    private const ALIASES = [
        'pcs' => 'piece',
        'pc' => 'piece',
        'units' => 'piece',
        'unit' => 'piece',
        'each' => 'piece',
        'qty' => 'piece',
        'count' => 'piece',
        'عدد' => 'piece',
        'دانه' => 'piece',
        'kg' => 'kilogram',
        'kgs' => 'kilogram',
        'کیلو' => 'kilogram',
        'کیلوگرمی' => 'kilogram',
        'g' => 'gram',
        'gr' => 'gram',
        'گرمی' => 'gram',
        'l' => 'liter',
        'ltr' => 'liter',
        'lt' => 'liter',
        'لیتری' => 'liter',
        'm' => 'meter',
        'mtr' => 'meter',
        'متری' => 'meter',
        'packets' => 'pack',
        'packing' => 'pack',
        'بسته‌ای' => 'pack',
        'بسته ای' => 'pack',
        'bxs' => 'box',
        'carton' => 'box',
        'بسته بندی' => 'box',
        'btl' => 'bottle',
        'bottles' => 'bottle',
        'دسته' => 'set',
        'sets' => 'set',
        'پک' => 'pack',
        'سرویس' => 'serving',
        'سرویز' => 'serving',
        'پورشن' => 'serving',
        'orders' => 'order',
    ];

    public const DEFAULT_UNIT = 'piece';

    /** @return array<int, array{code: string, fa: string, en: string}> */
    public static function all(): array
    {
        $units = [];

        foreach (self::UNITS as $code => [$fa, $en]) {
            $units[] = ['code' => $code, 'fa' => $fa, 'en' => $en];
        }

        return $units;
    }

    public static function exists(string $code): bool
    {
        return array_key_exists(strtolower(trim($code)), self::UNITS);
    }

    /**
     * Resolve whatever was stored or typed to a canonical code.
     *
     * Returns null for a value that names no known unit, so the caller can
     * reject it instead of storing something it cannot later explain.
     */
    public static function normalize(?string $value): ?string
    {
        $trimmed = strtolower(trim((string) $value));

        if ($trimmed === '') {
            return null;
        }

        if (isset(self::UNITS[$trimmed])) {
            return $trimmed;
        }

        return self::ALIASES[$trimmed] ?? null;
    }

    /**
     * Human wording for a stored code.
     *
     * An unrecognised stored value is returned unchanged rather than hidden:
     * old data stays visible instead of silently disappearing from a report.
     */
    public static function label(?string $code, string $locale = 'fa'): string
    {
        $code = strtolower(trim((string) $code));
        $index = $locale === 'en' ? 1 : 0;

        return self::UNITS[$code][$index] ?? ($code !== '' ? $code : self::UNITS[self::DEFAULT_UNIT][$index]);
    }

    /** Validation rule for an incoming unit field. */
    public static function validationRule(): string
    {
        return 'required|string|max:40';
    }
}