<?php

namespace App\Services;

use App\Models\Order;
use App\Support\Money;

/**
 * Authoritative order pricing.
 *
 * Every figure that ends up on an order — subtotal, discount, tax, total — is
 * computed here, on the server, from product prices the server reads and a tax
 * rate the server resolved. The client never submits a total, and the client
 * never decides the tax.
 *
 * All arithmetic runs on integer minor units so that the order, the payment,
 * and the journal entry are all derived from the same exact numbers. A client
 * preview is a courtesy; this is the record.
 */
class OrderPricingService
{
    /**
     * Price one order from validated line items.
     *
     * @param  array<int, array{product_id:string, quantity:int, unit_price:float, discount_percent?:float}>  $items
     * @return array{
     *     lines: array<int, array<string, mixed>>,
     *     subtotal: float, discount: float, tax: float, total: float
     * }
     */
    public function price(array $items, float $taxRatePercent): array
    {
        $taxRateMinor = Money::rateToMinor($taxRatePercent);

        $lines = [];
        $subtotalMinor = 0;
        $discountMinor = 0;
        $taxMinor = 0;
        $totalMinor = 0;

        foreach ($items as $item) {
            $quantity = (int) $item['quantity'];
            $unitPriceMinor = Money::toMinor($item['unit_price']);

            $lineSubtotalMinor = Money::multiply($unitPriceMinor, $quantity);

            $discountPercent = (float) ($item['discount_percent'] ?? 0);
            $lineDiscountMinor = Money::percentageOf($lineSubtotalMinor, Money::rateToMinor($discountPercent));

            $taxableMinor = Money::subtract($lineSubtotalMinor, $lineDiscountMinor);
            $lineTaxMinor = Money::percentageOf($taxableMinor, $taxRateMinor);
            $lineTotalMinor = Money::add($taxableMinor, $lineTaxMinor);

            $subtotalMinor = Money::add($subtotalMinor, $lineSubtotalMinor);
            $discountMinor = Money::add($discountMinor, $lineDiscountMinor);
            $taxMinor = Money::add($taxMinor, $lineTaxMinor);
            $totalMinor = Money::add($totalMinor, $lineTotalMinor);

            $lines[] = [
                'product_id' => $item['product_id'],
                'product_variant_id' => $item['product_variant_id'] ?? null,
                'product_name' => $item['product_name'] ?? '',
                'sku' => $item['sku'] ?? '',
                'unit' => $item['unit'] ?? null,
                'unit_price' => Money::fromMinor($unitPriceMinor),
                'quantity' => $quantity,
                'discount_percent' => $discountPercent,
                'subtotal' => Money::fromMinor($lineSubtotalMinor),
                'discount_amount' => Money::fromMinor($lineDiscountMinor),
                'tax_amount' => Money::fromMinor($lineTaxMinor),
                'total_price' => Money::fromMinor($lineTotalMinor),
            ];
        }

        return [
            'lines' => $lines,
            'subtotal' => Money::fromMinor($subtotalMinor),
            'discount' => Money::fromMinor($discountMinor),
            'tax' => Money::fromMinor($taxMinor),
            'total' => Money::fromMinor($totalMinor),
        ];
    }

    /**
     * Apply a priced order to its model.
     */
    public function applyToOrder(Order $order, array $priced, float $taxRatePercent): Order
    {
        $order->subtotal = $priced['subtotal'];
        $order->discount_amount = $priced['discount'];
        $order->tax_amount = $priced['tax'];
        $order->tax_rate = Money::fromMinor(Money::rateToMinor($taxRatePercent));
        $order->total_amount = $priced['total'];

        return $order;
    }
}