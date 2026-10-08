<?php

namespace App\Support;

use App\Models\OrganizationMembership;
use App\Models\User;

/**
 * Server-side role → permission matrix.
 *
 * This is the single place where "what may a staff member do" is decided.
 * The frontend never decides this; it only renders what this matrix allows.
 * Anything not explicitly granted is denied.
 */
class RolePermission
{
    public const BUSINESS_VIEW = 'business.view';
    public const BUSINESS_MANAGE = 'business.manage';

    public const STAFF_VIEW = 'staff.view';
    public const STAFF_MANAGE = 'staff.manage';

    public const CATALOG_VIEW = 'catalog.view';
    public const CATALOG_MANAGE = 'catalog.manage';

    public const INVENTORY_VIEW = 'inventory.view';
    public const INVENTORY_ADJUST = 'inventory.adjust';
    public const INVENTORY_RECEIVE = 'inventory.receive';

    public const SALES_VIEW = 'sales.view';
    public const SALES_CREATE = 'sales.create';
    public const SALES_REFUND = 'sales.refund';
    /**
     * Change the tax rate on a single order instead of using the business
     * default. Deliberately not granted to STAFF: a cashier registers the
     * sale, they do not decide what the business owes the tax authority.
     */
    public const SALES_TAX_OVERRIDE = 'sales.tax_override';

    public const PURCHASING_VIEW = 'purchasing.view';
    public const PURCHASING_MANAGE = 'purchasing.manage';

    public const CUSTOMERS_VIEW = 'customers.view';
    public const CUSTOMERS_MANAGE = 'customers.manage';

    public const EXPENSES_VIEW = 'expenses.view';
    public const EXPENSES_MANAGE = 'expenses.manage';

    public const ACCOUNTING_VIEW = 'accounting.view';
    public const ACCOUNTING_MANAGE = 'accounting.manage';

    public const MESSAGES_VIEW = 'messages.view';
    public const MESSAGES_IMPORT = 'messages.import';

    public const AUDIT_VIEW = 'audit.view';

    public const ALL = [
        self::BUSINESS_VIEW, self::BUSINESS_MANAGE,
        self::STAFF_VIEW, self::STAFF_MANAGE,
        self::CATALOG_VIEW, self::CATALOG_MANAGE,
        self::INVENTORY_VIEW, self::INVENTORY_ADJUST, self::INVENTORY_RECEIVE,
        self::SALES_VIEW, self::SALES_CREATE, self::SALES_REFUND, self::SALES_TAX_OVERRIDE,
        self::PURCHASING_VIEW, self::PURCHASING_MANAGE,
        self::CUSTOMERS_VIEW, self::CUSTOMERS_MANAGE,
        self::EXPENSES_VIEW, self::EXPENSES_MANAGE,
        self::ACCOUNTING_VIEW, self::ACCOUNTING_MANAGE,
        self::MESSAGES_VIEW, self::MESSAGES_IMPORT,
        self::AUDIT_VIEW,
    ];

    /** @var array<string, string[]> */
    private const MATRIX = [
        OrganizationMembership::ROLE_OWNER => self::ALL,

        OrganizationMembership::ROLE_MANAGER => [
            self::BUSINESS_VIEW,
            self::STAFF_VIEW,
            self::CATALOG_VIEW, self::CATALOG_MANAGE,
            self::INVENTORY_VIEW, self::INVENTORY_ADJUST, self::INVENTORY_RECEIVE,
            self::SALES_VIEW, self::SALES_CREATE, self::SALES_REFUND, self::SALES_TAX_OVERRIDE,
            self::PURCHASING_VIEW, self::PURCHASING_MANAGE,
            self::CUSTOMERS_VIEW, self::CUSTOMERS_MANAGE,
            self::EXPENSES_VIEW, self::EXPENSES_MANAGE,
            self::ACCOUNTING_VIEW, self::ACCOUNTING_MANAGE,
            self::MESSAGES_VIEW, self::MESSAGES_IMPORT,
        ],

        OrganizationMembership::ROLE_STAFF => [
            self::BUSINESS_VIEW,
            self::CATALOG_VIEW,
            self::INVENTORY_VIEW,
            self::SALES_VIEW, self::SALES_CREATE,
            self::PURCHASING_VIEW,
            self::CUSTOMERS_VIEW, self::CUSTOMERS_MANAGE,
            self::EXPENSES_VIEW,
            self::MESSAGES_VIEW, self::MESSAGES_IMPORT,
        ],
    ];

    /**
     * Normalise a stored role string to one of the three canonical roles.
     * Legacy seeded values ('Owner', 'Manager', 'Cashier', ...) are mapped,
     * unknown values are treated as the least-privileged role.
     */
    public static function canonical(?string $role): string
    {
        $normalized = strtoupper(trim((string) $role));

        return match ($normalized) {
            'OWNER', 'ADMIN' => OrganizationMembership::ROLE_OWNER,
            'MANAGER', 'STORE_MANAGER' => OrganizationMembership::ROLE_MANAGER,
            'STAFF', 'CASHIER', 'SELLER', 'EMPLOYEE' => OrganizationMembership::ROLE_STAFF,
            default => OrganizationMembership::ROLE_STAFF,
        };
    }

    /** @return string[] */
    public static function forRole(?string $role): array
    {
        return self::MATRIX[self::canonical($role)] ?? self::MATRIX[OrganizationMembership::ROLE_STAFF];
    }

    public static function allows(?string $role, string $permission): bool
    {
        return in_array($permission, self::forRole($role), true);
    }

    /**
     * Platform administrators bypass business permissions only for the
     * platform console; inside a business they still act through memberships.
     */
    public static function permissionsFor(User $user, ?string $role): array
    {
        return self::forRole($role);
    }
}
