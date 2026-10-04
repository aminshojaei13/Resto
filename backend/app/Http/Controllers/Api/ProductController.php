<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Product;
use App\Models\ProductVariant;
use App\Support\MembershipContext;
use App\Support\UnitCatalog;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * The product catalog.
 *
 * The unit of measurement is a controlled value from the unit catalog, not a
 * free-text field, so the same word means the same thing in the catalog, in
 * inventory, on a purchase, on an order and in a report.
 */
class ProductController extends Controller
{
    public function index(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $query = trim((string) $request->query('query', ''));
        $category = $request->query('category');

        $builder = Product::where('organization_id', $organizationId)
            ->with(['variants', 'stock.warehouse']);

        if ($query !== '') {
            $builder->where(function ($sub) use ($query) {
                $sub->where('name', 'like', "%{$query}%")
                    ->orWhere('sku', 'like', "%{$query}%")
                    ->orWhere('barcode', 'like', "%{$query}%")
                    ->orWhere('category', 'like', "%{$query}%");
            });
        }

        if (is_string($category) && $category !== '') {
            $builder->where('category', $category);
        }

        $locale = $request->query('locale') === 'en' ? 'en' : 'fa';

        $products = $builder->get()->map(function (Product $product) use ($locale) {
            return $this->present($product, $locale);
        });

        return response()->json($products);
    }

    public function show(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $locale = $request->query('locale') === 'en' ? 'en' : 'fa';

        $product = Product::where('organization_id', $organizationId)
            ->with(['variants', 'stock.warehouse'])
            ->findOrFail($id);

        return response()->json($this->present($product, $locale));
    }

    public function barcode(Request $request, string $barcode)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $product = Product::where('organization_id', $organizationId)
            ->where('barcode', $barcode)
            ->with(['variants', 'stock'])
            ->first();

        if (!$product) {
            return response()->json(['message' => 'کالایی با این بارکد یافت نشد.'], 404);
        }

        return response()->json($this->present($product));
    }

    /**
     * The catalog as the UI consumes it: the stored unit plus its wording, and
     * stock broken down per warehouse so a warehouse can always be named.
     */
    private function present(Product $product, string $locale = 'fa'): array
    {
        $payload = $product->toArray();
        $payload['unit_code'] = UnitCatalog::normalize($product->unit) ?? UnitCatalog::DEFAULT_UNIT;
        $payload['unit_label'] = UnitCatalog::label($product->unit, $locale);
        $payload['stock_by_warehouse'] = $product->stock
            ->map(fn ($stock) => [
                'warehouse_id' => $stock->warehouse_id,
                'warehouse_name' => $stock->warehouse?->name ?? '',
                'quantity' => (int) $stock->quantity,
                'reserved_quantity' => (int) $stock->reserved_quantity,
            ])
            ->values()
            ->all();

        return $payload;
    }

    public function store(Request $request)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $validated = $request->validate([
            'sku' => 'required|string|max:100',
            'barcode' => 'nullable|string|max:100',
            'name' => 'required|string|max:191',
            'price' => 'required|numeric|min:0',
            'cost_price' => 'required|numeric|min:0',
            'category' => 'nullable|string|max:100',
            'unit' => ['required', 'string', 'max:40'],
            'description' => 'nullable|string|max:2000',
            'image_url' => 'nullable|string|max:500',
        ]);

        $product = Product::create([
            'id' => (string) Str::uuid(),
            'organization_id' => $organizationId,
            'sku' => $validated['sku'],
            'barcode' => $validated['barcode'] ?? '',
            'name' => $validated['name'],
            'description' => $validated['description'] ?? '',
            'price' => $validated['price'],
            'cost_price' => $validated['cost_price'],
            'category' => $validated['category'] ?? '',
            'unit' => $this->resolveUnit($validated['unit']),
            'image_url' => $validated['image_url'] ?? '',
        ]);

        return response()->json($this->present($product), 201);
    }

    public function update(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        $product = Product::where('organization_id', $organizationId)->findOrFail($id);

        $validated = $request->validate([
            'name' => 'sometimes|required|string|max:191',
            'price' => 'sometimes|required|numeric|min:0',
            'cost_price' => 'sometimes|required|numeric|min:0',
            'sku' => 'sometimes|required|string|max:100',
            'barcode' => 'sometimes|nullable|string|max:100',
            'category' => 'sometimes|nullable|string|max:100',
            'unit' => ['sometimes', 'required', 'string', 'max:40'],
            'description' => 'sometimes|nullable|string|max:2000',
        ]);

        $product->fill($validated);

        if (isset($validated['unit'])) {
            $product->unit = $this->resolveUnit($validated['unit']);
        }

        $product->save();

        return response()->json($this->present($product->fresh(['variants', 'stock.warehouse'])));
    }

    public function destroy(Request $request, string $id)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);

        Product::where('organization_id', $organizationId)->findOrFail($id)->delete();

        return response()->json(['message' => 'کالا حذف شد.']);
    }

    /**
     * A variant belongs to the same product and therefore counts in the same
     * unit; it does not carry a second, competing unit field.
     */
    public function createVariant(Request $request, string $productId)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $product = Product::where('organization_id', $organizationId)->findOrFail($productId);

        $validated = $request->validate([
            'sku' => 'required|string|max:100',
            'barcode' => 'nullable|string|max:100',
            'name' => 'required|string|max:191',
            'price' => 'required|numeric|min:0',
            'cost_price' => 'required|numeric|min:0',
        ]);

        $variant = ProductVariant::create(array_merge($validated, [
            'id' => (string) Str::uuid(),
            'product_id' => $product->id,
        ]));

        return response()->json($variant, 201);
    }

    public function updateVariant(Request $request, string $variantId)
    {
        $organizationId = MembershipContext::activeOrganizationId($request);
        $variant = ProductVariant::where('product_id', function ($query) use ($organizationId) {
            $query->select('id')
                ->from('products')
                ->where('organization_id', $organizationId);
        })->where('id', $variantId)->firstOrFail();

        $variant->update($request->validate([
            'sku' => 'sometimes|required|string|max:100',
            'barcode' => 'sometimes|nullable|string|max:100',
            'name' => 'sometimes|required|string|max:191',
            'price' => 'sometimes|required|numeric|min:0',
            'cost_price' => 'sometimes|required|numeric|min:0',
        ]));

        return response()->json($variant);
    }

    /**
     * Resolve an incoming unit to its catalog code.
     *
     * An unrecognised value is refused with the list of valid choices rather
     * than stored, because a unit nobody can explain makes every later
     * quantity on that product meaningless.
     */
    private function resolveUnit(string $value): string
    {
        $unit = UnitCatalog::normalize($value);

        if ($unit === null) {
            throw ValidationException::withMessages([
                'unit' => ['واحد کالا معتبر نیست. یکی از واحدهای فهرست‌شده را انتخاب کنید.'],
            ]);
        }

        return $unit;
    }
}