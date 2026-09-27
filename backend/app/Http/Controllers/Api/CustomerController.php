<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Customer;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class CustomerController extends Controller
{
    public function index(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';
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

    public function show(string $id)
    {
        $customer = Customer::with(['addresses'])->findOrFail($id);
        return response()->json($customer);
    }

    public function store(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';

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
        $customer = Customer::findOrFail($id);

        $request->validate([
            'name' => 'sometimes|required|string',
            'email' => 'nullable|email',
            'phone' => 'nullable|string',
            'address' => 'nullable|string',
        ]);

        $customer->update($request->only(['name', 'email', 'phone', 'address', 'total_purchases', 'loyalty_points']));

        return response()->json($customer);
    }

    public function destroy(string $id)
    {
        $customer = Customer::findOrFail($id);
        $customer->delete();
        return response()->json(['message' => 'Customer deleted/archived successfully']);
    }
}
