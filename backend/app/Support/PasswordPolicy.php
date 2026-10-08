<?php

namespace App\Support;

use Illuminate\Support\Str;

/**
 * Modern password policy: length-first, no arbitrary character-class rules.
 * - minimum 10 chars, generous max (no silent truncation)
 * - allows spaces and Unicode
 * - rejects a small list of ubiquitous/common passwords
 */
class PasswordPolicy
{
    public const MIN_LENGTH = 10;
    public const MAX_LENGTH = 256;

    /** Ubiquitous passwords that must never be accepted. */
    private const COMMON_PASSWORDS = [
        'password', 'password1', 'password123', 'password1234', 'passw0rd',
        '123456', '1234567', '12345678', '123456789', '1234567890',
        'qwerty', 'qwerty123', 'qwertyuiop', 'letmein', 'welcome',
        'welcome1', 'admin', 'admin123', 'administrator', 'iloveyou',
        'sunshine', 'princess', 'dragon', 'monkey', 'football',
        'abc123', '111111', '000000', '121212', '654321',
        'password!', 'p@ssw0rd', 'p@ssword', 'trustno1', 'superman',
        'batman', 'master', 'hello', 'freedom', 'whatever', 'secret',
        'resto', 'resto123', 'resto12345', 'calcuapp',
    ];

    /**
     * Validate a plaintext password. Returns list of human-readable
     * (non-technical) problem descriptions, empty when valid.
     *
     * @return string[]
     */
    public static function validate(?string $password): array
    {
        $errors = [];

        if ($password === null || $password === '') {
            return ['رمز عبور الزامی است.'];
        }

        $length = mb_strlen($password);

        if ($length < self::MIN_LENGTH) {
            $errors[] = 'رمز عبور باید حداقل ' . self::MIN_LENGTH . ' نویسه باشد.';
        }

        if ($length > self::MAX_LENGTH) {
            $errors[] = 'رمز عبور نمی‌تواند بیش از ' . self::MAX_LENGTH . ' نویسه باشد.';
        }

        if ($length >= 4 && self::isCommon($password)) {
            $errors[] = 'این رمز عبور بسیار رایج است و مجاز نیست. لطفاً یک رمز منحصربه‌فرد انتخاب کنید.';
        }

        return $errors;
    }

    public static function isCommon(string $password): bool
    {
        $normalized = strtolower(trim($password));
        $squashed = preg_replace('/[^a-z0-9]/', '', $normalized) ?? '';

        foreach (self::COMMON_PASSWORDS as $common) {
            if ($normalized === $common || $squashed === $common) {
                return true;
            }
        }

        // Long digit runs (1234567890, 00001234) are never a real secret.
        if (preg_match('/^\d{6,}$/', $squashed) === 1) {
            return true;
        }

        // A single repeated character ("aaaaaaaa", "---------") is never one.
        if ($squashed !== '' && preg_match('/^(.)\1*$/u', $squashed) === 1) {
            return true;
        }

        // Sequential runs such as "abcdef" or "987654".
        if ($squashed !== '' && self::isSequentialRun($squashed)) {
            return true;
        }

        return false;
    }

    private static function isSequentialRun(string $value): bool
    {
        $length = strlen($value);

        if ($length < 4) {
            return false;
        }

        $delta = ord($value[1]) - ord($value[0]);

        if ($delta !== 1 && $delta !== -1) {
            return false;
        }

        for ($i = 1; $i < $length; $i++) {
            if ((ord($value[$i]) - ord($value[$i - 1])) !== $delta) {
                return false;
            }
        }

        return true;
    }

    /** Laravel validation rule string for request validators. */
    public static function rule(): string
    {
        return 'string|min:' . self::MIN_LENGTH . '|max:' . self::MAX_LENGTH;
    }
}
