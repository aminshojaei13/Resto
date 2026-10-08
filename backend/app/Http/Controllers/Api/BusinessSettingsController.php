<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Services\BusinessSettingsService;
use App\Support\MembershipContext;
use App\Support\UnitCatalog;
use Illuminate\Http\Request;

/**
 * Business-level settings.
 *
 * The business default sales tax rate lives here, and nowhere else. Every
 * other screen asks for the rate rather than carrying its own copy.
 */
class BusinessSettingsController extends Controller
{
    public function __construct(private BusinessSettingsService $settings)
    {
    }

    public function show(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        return response()->json($this->settings->settingsPayload($request, $organizationId));
    }

    public function update(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $this->settings->updateDefaultTaxRate($request, $organizationId);

        return response()->json([
            'message' => 'تنظیمات مالیات کسب‌وکار ذخیره شد.',
            'settings' => $this->settings->settingsPayload($request, $organizationId),
        ]);
    }

    /**
     * The unit vocabulary a business may choose from for its products.
     */
    public function units(Request $request)
    {
        $locale = $request->query('locale') === 'en' ? 'en' : 'fa';

        $units = array_map(
            fn (array $unit) => $unit + ['label' => UnitCatalog::label($unit['code'], $locale)],
            UnitCatalog::all()
        );

        return response()->json($units);
    }
}