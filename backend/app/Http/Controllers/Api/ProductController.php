<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Product;
use App\Models\ProductVariant;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class ProductController extends Controller
{
    public function index(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';
        $query = $request->query('query');
        $category = $request->query('category');

        $builder = Product::where('organization_id', $orgId)->with(['variants', 'stock']);

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

    public function show(string $id)
    {
        $product = Product::with(['variants', 'stock.warehouse'])->findOrFail($id);
        return response()->json($product);
    }

    public function barcode(Request $request, string $barcode)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';
        $product = Product::where('organization_id', $orgId)
            ->where('barcode', $barcode)
            ->with(['variants', 'stock'])
            ->first();

        if (!$product) {
            return response()->json(['message' => 'Product not found for barcode: ' . $barcode], 404);
        }

        return response()->json($product);
    }

    public function store(Request $request)
    {
        $orgId = $request->header('X-Tenant-ID') ?? $request->get('org_id') ?? $request->input('org_id') ?? '';

        $request->validate([
            'sku' => 'required|string',
            'barcode' => 'required|string',
            'name' => 'required|string',
            'price' => 'required|numeric|min:0',
            'cost_price' => 'required|numeric|min:0',
            'category' => 'required|string'
        ]);

        $product = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $orgId,
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

    public function update(Request $request, string $id)
    {
        $product = Product::findOrFail($id);

        $request->validate([
            'name' => 'sometimes|required|string',
            'price' => 'sometimes|required|numeric|min:0',
            'cost_price' => 'sometimes|required|numeric|min:0',
            'sku' => 'sometimes|required|string',
            'barcode' => 'sometimes|required|string',
            'category' => 'sometimes|required|string',
        ]);

        $product->update($request->only([
            'sku', 'barcode', 'name', 'description', 'price', 'cost_price', 'category', 'unit', 'image_url'
        ]));

        return response()->json($product);
    }

    public function destroy(string $id)
    {
        $product = Product::findOrFail($id);
        $product->delete();
        return response()->json(['message' => 'Product deleted/archived successfully']);
    }

    public function createVariant(Request $request, string $productId)
    {
        $product = Product::findOrFail($productId);

        $request->validate([
            'sku' => 'required|string',
            'barcode' => 'required|string',
            'name' => 'required|string',
            'price' => 'required|numeric|min:0',
            'cost_price' => 'required|numeric|min:0',
        ]);

        $variant = ProductVariant::create([
            'id' => (string) Str::uuid(),
            'product_id' => $product->id,
            'sku' => $request->sku,
            'barcode' => $request->barcode,
            'name' => $request->name,
            'price' => $request->price,
            'cost_price' => $request->cost_price,
        ]);

        return response()->json($variant, 201);
    }

    public function updateVariant(Request $request, string $variantId)
    {
        $variant = ProductVariant::findOrFail($variantId);
        $variant->update($request->only(['sku', 'barcode', 'name', 'price', 'cost_price']));
        return response()->json($variant);
    }
}
