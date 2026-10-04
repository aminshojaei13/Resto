<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Support\MembershipContext;
use App\Models\Customer;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class CustomerController extends Controller
{
    public function index(Request $request)
    {
        $orgId = MembershipContext::activeOrganizationId($request);
        $query = $request->query('query');

        $builder = Customer::where('organization_id', $orgId);

        if ($query) {
            $builder->where(function($q) use ($query) {
                $q->where('name', 'LIKE', "%{$query}%")
                  ->orWhere('phone', 'LIKE', "%{$query}%")
                  ->orWhere('email', 'LIKE', "%{$query}%");
            });
        }

        return response()->json($builder->get());
    }

    public function show(Request $request, string $id)
    {
        $orgId = MembershipContext::activeOrganizationId($request);

        return response()->json(Customer::where('organization_id', $orgId)->with(['addresses'])->findOrFail($id));
    }

    public function store(Request $request)
    {
        $orgId = MembershipContext::activeOrganizationId($request);

        $request->validate([
            'name' => 'required|string',
            'email' => 'nullable|email',
            'phone' => 'nullable|string',
            'address' => 'nullable|string',
        ]);

        $customer = Customer::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $orgId,
            'name' => $request->name,
            'email' => $request->email ?? '',
            'phone' => $request->phone ?? '',
            'address' => $request->address ?? '',
        ]);

        return response()->json($customer, 201);
    }

    public function update(Request $request, string $id)
    {
        $orgId = MembershipContext::activeOrganizationId($request);
        $customer = Customer::where('organization_id', $orgId)->findOrFail($id);

        $request->validate([
            'name' => 'sometimes|required|string',
            'email' => 'nullable|email',
            'phone' => 'nullable|string',
            'address' => 'nullable|string',
        ]);

        $customer->update($request->only(['name', 'email', 'phone', 'address', 'total_purchases', 'loyalty_points']));

        return response()->json($customer);
    }

    public function destroy(Request $request, string $id)
    {
        $orgId = MembershipContext::activeOrganizationId($request);

        Customer::where('organization_id', $orgId)->findOrFail($id)->delete();

        return response()->json(['message' => 'مشتری حذف شد.']);
    }
}
