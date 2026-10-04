<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Expense extends Model
{
    public $incrementing = false;
    protected $keyType = 'string';

    protected $fillable = [
        'id', 'organization_id', 'store_id', 'category', 'title', 'amount', 'payment_method',
        'date', 'notes', 'attachment_url', 'user_id', 'is_recurring', 'recurrence_period',
        'recurrence_starts_on', 'recurrence_ends_on'
    ];

    /**
     * The stable set of operating-expense categories this business records.
     *
     * These are costs of running the business, not the purchase of stock: rent,
     * utilities, transport, salaries and similar. Buying goods from a supplier
     * is a purchase order and is never filed here.
     *
     * @return array<int, array{code: string, fa: string, en: string}>
     */
    public const CATEGORIES = [
        ['code' => 'RENT', 'fa' => 'اجاره', 'en' => 'Rent'],
        ['code' => 'INTERNET', 'fa' => 'اینترنت', 'en' => 'Internet'],
        ['code' => 'ELECTRICITY', 'fa' => 'برق', 'en' => 'Electricity'],
        ['code' => 'WATER', 'fa' => 'آب', 'en' => 'Water'],
        ['code' => 'TRANSPORT', 'fa' => 'حمل‌ونقل', 'en' => 'Transport'],
        ['code' => 'ADVERTISING', 'fa' => 'تبلیغات', 'en' => 'Advertising'],
        ['code' => 'SALARIES', 'fa' => 'حقوق', 'en' => 'Salaries'],
        ['code' => 'REPAIRS', 'fa' => 'تعمیرات', 'en' => 'Repairs'],
        ['code' => 'SERVICES', 'fa' => 'خدمات', 'en' => 'Services'],
        ['code' => 'FEES', 'fa' => 'کارمزد', 'en' => 'Fees'],
        ['code' => 'OTHER', 'fa' => 'سایر هزینه‌ها', 'en' => 'Other'],
    ];

    /** @return array<int, string> */
    public static function categoryCodes(): array
    {
        return array_column(self::CATEGORIES, 'code');
    }

    public static function categoryLabel(string $code, string $locale = 'fa'): string
    {
        foreach (self::CATEGORIES as $category) {
            if ($category['code'] === $code) {
                return $locale === 'en' ? $category['en'] : $category['fa'];
            }
        }

        return $code;
    }
}
