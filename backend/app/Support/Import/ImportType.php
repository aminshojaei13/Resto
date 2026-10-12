<?php

namespace App\Support\Import;

class ImportType
{
    public const PRODUCTS = 'products';
    public const CUSTOMERS = 'customers';
    public const INVENTORY = 'inventory';
    public const EXPENSES = 'expenses';
    public const ORDERS = 'orders';
    public const SUPPLIERS = 'suppliers';
    public const PURCHASES = 'purchases';

    public const ALL = [
        self::PRODUCTS,
        self::CUSTOMERS,
        self::INVENTORY,
        self::EXPENSES,
        self::ORDERS,
        self::SUPPLIERS,
        self::PURCHASES,
    ];

    public static function isValid(string $type): bool
    {
        return in_array($type, self::ALL, true);
    }
}
