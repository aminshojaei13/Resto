<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Organization;
use App\Models\Store;
use App\Models\Warehouse;
use Illuminate\Http\Request;

class OrganizationController extends Controller
{
    public function index()
    {
        return response()->json(Organization::with('stores.warehouses')->get());
    }

    public function stores(string $orgId)
    {
        return response()->json(Store::where('organization_id', $orgId)->with('warehouses')->get());
    }

    public function warehouses(string $storeId)
    {
        return response()->json(Warehouse::where('store_id', $storeId)->get());
    }
}
