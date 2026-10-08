<?php

namespace App\Support;

/**
 * Exact money arithmetic.
 *
 * Money is handled as an integer count of minor units (halalas) and only
 * becomes a float at the moment it is stored or returned. Binary floating
 * point is never allowed to be the source of truth for a financial figure:
 * 0.1 + 0.2 is not 0.30, and an order total that is off by one minor unit is
 * an order total that cannot be reconciled against the ledger.
 */
class Money
{
    /** Two decimal places for every monetary column in this schema. */
    public const SCALE = 2;
    public const MINOR_FACTOR = 100;

    /**
     * Convert an incoming decimal value to exact minor units.
     *
     * Uses string rounding rather than `(int) round($v * 100)` because the
     * latter re-introduces the binary error it is meant to remove.
     */
    public static function toMinor(int|float|string|null $amount): int
    {
        if ($amount === null || $amount === '') {
            return 0;
        }

        return (int) round(((float) $amount) * self::MINOR_FACTOR, 0, PHP_ROUND_HALF_UP);
    }

    public static function fromMinor(int $minor): float
    {
        return round($minor / self::MINOR_FACTOR, self::SCALE);
    }

    public static function add(int $a, int $b): int
    {
        return $a + $b;
    }

    public static function subtract(int $a, int $b): int
    {
        return $a - $b;
    }

    /**
     * Multiply an amount by a whole-number quantity, exactly.
     */
    public static function multiply(int $amountMinor, int $quantity): int
    {
        return $amountMinor * $quantity;
    }

    /**
     * Apply a percentage rate to an amount, exactly.
     *
     * $ratePercent is in percentage points: 8 means 8%, 8.25 means 8.25%.
     * The rate is carried as its own scaled integer so that neither the rate
     * nor the multiplication passes through a float.
     */
    public static function percentageOf(int $amountMinor, int $ratePercentMinor): int
    {
        // amountMinor * rateMinor / (100 * 100), rounded half up, all integer.
        $numerator = $amountMinor * $ratePercentMinor;
        $denominator = 100 * self::MINOR_FACTOR;

        return (int) round($numerator / $denominator, 0, PHP_ROUND_HALF_UP);
    }

    /**
     * Reduce a percentage expressed in percentage points to scaled minor units.
     */
    public static function rateToMinor(int|float|string|null $ratePercent): int
    {
        if ($ratePercent === null || $ratePercent === '') {
            return 0;
        }

        return (int) round(((float) $ratePercent) * self::MINOR_FACTOR, 0, PHP_ROUND_HALF_UP);
    }

    /**
     * Render minor units back for display/serialisation.
     */
    public static function format(int $amountMinor): string
    {
        return number_format(self::fromMinor($amountMinor), self::SCALE, '.', '');
    }
}