<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Supplier;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class SupplierController extends Controller
{
    public function index(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';
        $query = $request->query('query');

        $builder = Supplier::where('organization_id', $orgId);

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
        $supplier = Supplier::with(['purchases.items'])->findOrFail($id);
        return response()->json($supplier);
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

        $supplier = Supplier::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $orgId,
            'name' => $request->name,
            'email' => $request->email ?? '',
            'phone' => $request->phone ?? '',
            'address' => $request->address ?? '',
        ]);

        return response()->json($supplier, 201);
    }

    public function update(Request $request, string $id)
    {
        $supplier = Supplier::findOrFail($id);

        $request->validate([
            'name' => 'sometimes|required|string',
            'email' => 'nullable|email',
            'phone' => 'nullable|string',
            'address' => 'nullable|string',
        ]);

        $supplier->update($request->only(['name', 'email', 'phone', 'address']));

        return response()->json($supplier);
    }

    public function destroy(string $id)
    {
        $supplier = Supplier::findOrFail($id);
        $supplier->delete();
        return response()->json(['message' => 'Supplier deleted/archived successfully']);
    }
}
