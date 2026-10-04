<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Support\MembershipContext;
use App\Models\Organization;
use App\Models\Store;
use App\Models\Warehouse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class OrganizationController extends Controller
{
    public function index(Request $request)
    {
        // Only the businesses the caller actually belongs to.
        $memberships = MembershipContext::memberships($request);

        $organizationIds = $memberships->pluck('organization_id')->all();

        return response()->json(
            Organization::whereIn('id', $organizationIds)->with('stores.warehouses')->get()
        );
    }

    public function show(Request $request, string $id)
    {
        $allowed = MembershipContext::memberships($request)->pluck('organization_id')->all();

        if (!in_array($id, $allowed, true)) {
            return response()->json(['message' => 'شما عضو این کسب‌وکار نیستید.'], 403);
        }

        return response()->json(Organization::with('stores.warehouses')->findOrFail($id));
    }

    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string',
            'code' => 'sometimes|nullable|string',
        ]);

        $code = $request->code;
        if (empty($code)) {
            $code = strtoupper(substr(preg_replace('/[^A-Za-z0-9]/', '', $request->name), 0, 4));
            if (strlen($code) < 3) $code = 'RSTO';
            $code .= rand(100, 999);
        }

        $org = Organization::create([
            'id' => (string) Str::uuid(),
            'name' => $request->name,
            'code' => $code,
            'currency_symbol' => $request->currency_symbol ?? '$',
            'currency_code' => $request->currency_code ?? 'USD',
            'subscription_tier' => $request->subscription_tier ?? 'ENTERPRISE',
        ]);

        // Provision Default Store & Warehouse
        $store = Store::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $org->id,
            'name' => $org->name . ' (Main Store)',
            'code' => $code . '-ST1',
            'address' => 'Main Store Address',
            'phone' => '',
        ]);

        Warehouse::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $org->id,
            'store_id' => $store->id,
            'name' => 'Main Warehouse',
            'code' => $code . '-WH1',
            'address' => 'Main Warehouse Address',
        ]);

        return response()->json($org->load('stores.warehouses'), 201);
    }

    public function update(Request $request, string $id)
    {
        $allowed = MembershipContext::memberships($request)->pluck('organization_id')->all();

        if (!in_array($id, $allowed, true)) {
            return response()->json(['message' => 'شما عضو این کسب‌وکار نیستید.'], 403);
        }

        $org = Organization::findOrFail($id);

        $request->validate([
            'name' => 'sometimes|required|string',
            'currency_symbol' => 'nullable|string',
            'currency_code' => 'nullable|string',
        ]);

        $org->update($request->only(['name', 'currency_symbol', 'currency_code', 'logo_url', 'subscription_tier']));

        return response()->json($org);
    }

    public function stores(Request $request, string $orgId)
    {
        $this->assertMember($request, $orgId);

        return response()->json(Store::where('organization_id', $orgId)->with('warehouses')->get());
    }

    /**
     * Refuse a business the caller does not belong to.
     */
    private function assertMember(Request $request, string $organizationId): void
    {
        $allowed = MembershipContext::memberships($request)->pluck('organization_id')->all();

        if (!in_array($organizationId, $allowed, true)) {
            abort(response()->json(['message' => 'شما عضو این کسب‌وکار نیستید.'], 403));
        }
    }

    public function createStore(Request $request)
    {
        $validated = $request->validate([
            'organization_id' => 'required|string',
            'name' => 'required|string',
            'code' => 'required|string',
        ]);

        $this->assertMember($request, $validated['organization_id']);

        $store = Store::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $request->organization_id,
            'name' => $request->name,
            'code' => $request->code,
            'address' => $request->address ?? '',
            'phone' => $request->phone ?? '',
        ]);

        return response()->json($store, 201);
    }

    public function updateStore(Request $request, string $id)
    {
        $store = Store::findOrFail($id);
        $store->update($request->only(['name', 'code', 'address', 'phone']));
        return response()->json($store);
    }

    public function warehouses(string $storeId)
    {
        return response()->json(Warehouse::where('store_id', $storeId)->get());
    }

    public function createWarehouse(Request $request)
    {
        $validated = $request->validate([
            'organization_id' => 'required|string',
            'store_id' => 'required|string',
            'name' => 'required|string',
            'code' => 'required|string',
        ]);

        $this->assertMember($request, $validated['organization_id']);

        $store = Store::find($validated['store_id']);

        if (!$store || $store->organization_id !== $validated['organization_id']) {
            return response()->json([
                'message' => 'انبار باید به فروشگاهی در همین کسب‌وکار تعلق داشته باشد.',
            ], 422);
        }

        $warehouse = Warehouse::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $request->organization_id,
            'store_id' => $request->store_id,
            'name' => $request->name,
            'code' => $request->code,
            'address' => $request->address ?? '',
        ]);

        return response()->json($warehouse, 201);
    }

    public function updateWarehouse(Request $request, string $id)
    {
        $warehouse = Warehouse::findOrFail($id);
        $warehouse->update($request->only(['name', 'code', 'address']));
        return response()->json($warehouse);
    }
}
