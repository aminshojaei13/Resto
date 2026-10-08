<?php

namespace App\Services;

use App\Models\Organization;
use App\Support\Money;
use App\Support\RolePermission;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Business-level settings, and the single authoritative source for the sales
 * tax rate.
 *
 * Before this, 8% existed as a literal in four different places (two
 * controllers, the POS screen, the message screen) with no way to change it
 * and nothing explaining where it came from. Now there is exactly one place:
 * `organizations.default_tax_rate`. Everything that needs a tax rate asks this
 * service.
 *
 * Tax policy
 * ----------
 * The business default applies to every new order. A single order may only
 * differ from it when the person placing it holds the tax-override
 * permission; everyone else gets the configured rate and cannot influence it
 * through the client. The applied rate is stored on the order, so a past
 * order can always be explained even after the setting changes.
 */
class BusinessSettingsService
{
    public const DEFAULT_TAX_RATE = '8.00';

    /** Refuse anything that would silently zero or explode an order total. */
    public const MIN_TAX_RATE = 0;
    public const MAX_TAX_RATE = 100;

    public function defaultTaxRate(string $organizationId): float
    {
        $value = Organization::where('id', $organizationId)->value('default_tax_rate');

        if ($value === null) {
            // A business created before this setting existed has no stored
            // rate. Returning the documented default keeps it tradeable; the
            // value is written back by the next settings save.
            return (float) self::DEFAULT_TAX_RATE;
        }

        return (float) $value;
    }

    /**
     * Validate and persist the business default tax rate.
     */
    public function updateDefaultTaxRate(Request $request, string $organizationId): Organization
    {
        $validated = $request->validate([
            'default_tax_rate' => ['required', 'numeric', 'min:' . self::MIN_TAX_RATE, 'max:' . self::MAX_TAX_RATE],
        ]);

        $rate = Money::fromMinor(Money::rateToMinor($validated['default_tax_rate']));

        if ($rate < 0 || $rate > self::MAX_TAX_RATE) {
            throw ValidationException::withMessages([
                'default_tax_rate' => ['نرخ مالیات باید عددی بین ۰ تا ۱۰۰ باشد.'],
            ]);
        }

        $organization = Organization::findOrFail($organizationId);
        $organization->default_tax_rate = $rate;
        $organization->save();

        return $organization;
    }

    /**
     * The rate that will actually be applied to a new order.
     *
     * A per-order rate is honoured only when the person is allowed to override
     * the business default. Otherwise the configured default wins — the client
     * is never trusted to decide the tax on a sale.
     */
    public function resolveTaxRate(Request $request, string $organizationId): float
    {
        $default = $this->defaultTaxRate($organizationId);
        $requested = $request->input('tax_rate');

        if ($requested === null || $requested === '') {
            return $default;
        }

        if (!is_numeric($requested)) {
            throw ValidationException::withMessages([
                'tax_rate' => ['نرخ مالیات باید عدد باشد.'],
            ]);
        }

        $requested = Money::fromMinor(Money::rateToMinor($requested));

        if ($requested < 0 || $requested > self::MAX_TAX_RATE) {
            throw ValidationException::withMessages([
                'tax_rate' => ['نرخ مالیات باید عددی بین ۰ تا ۱۰۰ باشد.'],
            ]);
        }

        $role = \App\Support\MembershipContext::roleFor($request, $organizationId);

        if (!RolePermission::allows($role, RolePermission::SALES_TAX_OVERRIDE)) {
            // Silently applying the default rather than failing keeps an
            // ordinary cashier's checkout working; the rate they get is the
            // business rate, which is the only one they are allowed to charge.
            return $default;
        }

        return $requested;
    }

    /**
     * Whether the current person may change the tax rate on a single order.
     */
    public function canOverrideTax(Request $request, string $organizationId): bool
    {
        return RolePermission::allows(
            \App\Support\MembershipContext::roleFor($request, $organizationId),
            RolePermission::SALES_TAX_OVERRIDE
        );
    }

    /**
     * The settings payload the UI reads.
     */
    public function settingsPayload(Request $request, string $organizationId): array
    {
        $organization = Organization::findOrFail($organizationId);

        return [
            'default_tax_rate' => Money::fromMinor(Money::rateToMinor($this->defaultTaxRate($organizationId))),
            'tax_inclusive_pricing' => (bool) $organization->tax_inclusive_pricing,
            'can_override_tax_per_order' => $this->canOverrideTax($request, $organizationId),
            'min_tax_rate' => self::MIN_TAX_RATE,
            'max_tax_rate' => self::MAX_TAX_RATE,
        ];
    }
}