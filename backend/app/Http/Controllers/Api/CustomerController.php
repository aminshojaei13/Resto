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
        $orgId = $request->get('org_id') ?? 'org_apex';
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

    public function store(Request $request)
    {
        $request->validate([
            'org_id' => 'required',
            'name' => 'required',
        ]);

        $customer = Customer::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $request->org_id,
            'name' => $request->name,
            'email' => $request->email ?? '',
            'phone' => $request->phone ?? '',
            'address' => $request->address ?? '',
        ]);

        return response()->json($customer, 201);
    }
}
