<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Product;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class ProductController extends Controller
{
    public function index(Request $request)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';
        $query = $request->query('query');
        $category = $request->query('category');

        $builder = Product::where('organization_id', $orgId)->with('variants');

        if ($query) {
            $builder->where(function($q) use ($query) {
                $q->where('name', 'LIKE', "%{$query}%")
                  ->orWhere('sku', 'LIKE', "%{$query}%")
                  ->orWhere('barcode', 'LIKE', "%{$query}%")
                  ->orWhere('category', 'LIKE', "%{$query}%");
            });
        }

        if ($category) {
            $builder->where('category', $category);
        }

        return response()->json($builder->get());
    }

    public function barcode(Request $request, string $barcode)
    {
        $orgId = $request->get('org_id') ?? 'org_apex';
        $product = Product::where('organization_id', $orgId)
            ->where('barcode', $barcode)
            ->with('variants')
            ->first();

        if (!$product) {
            return response()->json(['message' => 'Product not found for barcode: ' . $barcode], 404);
        }

        return response()->json($product);
    }

    public function store(Request $request)
    {
        $request->validate([
            'org_id' => 'required',
            'sku' => 'required',
            'barcode' => 'required',
            'name' => 'required',
            'price' => 'required|numeric',
            'cost_price' => 'required|numeric',
            'category' => 'required'
        ]);

        $product = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $request->org_id,
            'sku' => $request->sku,
            'barcode' => $request->barcode,
            'name' => $request->name,
            'description' => $request->description ?? '',
            'price' => $request->price,
            'cost_price' => $request->cost_price,
            'category' => $request->category,
            'unit' => $request->unit ?? 'pcs',
            'image_url' => $request->image_url ?? '',
        ]);

        return response()->json($product, 201);
    }
}
