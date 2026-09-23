<?php

namespace Database\Seeders;

use App\Models\Account;
use App\Models\ChartOfAccounts;
use App\Models\Customer;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Organization;
use App\Models\Product;
use App\Models\Store;
use App\Models\Supplier;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // 1. Users
        $admin = User::create([
            'id' => 'usr_admin_1',
            'name' => 'Alex Mercer',
            'email' => 'alex.mercer@calcuapp.com',
            'phone' => '+1 (555) 019-2834',
            'password' => Hash::make('password123'),
            'role' => 'Owner',
        ]);

        // 2. Organizations
        $apex = Organization::create([
            'id' => 'org_apex',
            'name' => 'Apex Retail Group',
            'code' => 'APEX',
            'logo_url' => 'https://picsum.photos/id/1018/200',
            'currency_symbol' => '$',
            'currency_code' => 'USD',
            'subscription_tier' => 'ENTERPRISE',
        ]);

        $braveboy = Organization::create([
            'id' => 'org_braveboy',
            'name' => 'BraveBoy Electronics',
            'code' => 'BBE',
            'logo_url' => 'https://picsum.photos/id/1019/200',
            'currency_symbol' => '$',
            'currency_code' => 'USD',
            'subscription_tier' => 'PRO',
        ]);

        // 3. Stores & Warehouses
        $storeApex1 = Store::create([
            'id' => 'store_apex_1',
            'organization_id' => 'org_apex',
            'name' => 'Apex Flagship Store (Downtown)',
            'code' => 'APX-DT',
            'address' => '100 Market St, San Francisco, CA',
            'phone' => '+1 (555) 019-2834',
        ]);

        $whApex1a = Warehouse::create([
            'id' => 'wh_apex_1a',
            'store_id' => 'store_apex_1',
            'organization_id' => 'org_apex',
            'name' => 'Main Warehouse',
            'code' => 'WH-MAIN',
            'address' => '100 Market St B1, San Francisco',
        ]);

        $whApex1b = Warehouse::create([
            'id' => 'wh_apex_1b',
            'store_id' => 'store_apex_1',
            'organization_id' => 'org_apex',
            'name' => 'Express Storage Hub',
            'code' => 'WH-EXP',
            'address' => '120 Market St, San Francisco',
        ]);

        // 4. Products & Stock
        $probook = Product::create([
            'id' => 'prod_1',
            'organization_id' => 'org_apex',
            'sku' => 'APX-LAP-001',
            'barcode' => '880609123401',
            'name' => 'ProBook Ultra 15 M3',
            'description' => 'High-performance laptop featuring 16GB RAM and 512GB SSD.',
            'price' => 1299.99,
            'cost_price' => 850.00,
            'category' => 'Electronics',
            'unit' => 'pcs',
            'image_url' => 'https://picsum.photos/id/0/300/300',
        ]);

        WarehouseStock::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_apex',
            'warehouse_id' => 'wh_apex_1a',
            'product_id' => 'prod_1',
            'quantity' => 22,
        ]);

        $headphones = Product::create([
            'id' => 'prod_2',
            'organization_id' => 'org_apex',
            'sku' => 'APX-AUD-002',
            'barcode' => '880609123402',
            'name' => 'NoiseCancel Studio Headphones',
            'description' => 'Active noise cancelling wireless headphones with 30-hour battery life.',
            'price' => 249.99,
            'cost_price' => 120.00,
            'category' => 'Audio',
            'unit' => 'pcs',
            'image_url' => 'https://picsum.photos/id/1057/300/300',
        ]);

        WarehouseStock::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_apex',
            'warehouse_id' => 'wh_apex_1a',
            'product_id' => 'prod_2',
            'quantity' => 45,
        ]);

        // 5. Customers
        $sarah = Customer::create([
            'id' => 'cust_1',
            'organization_id' => 'org_apex',
            'name' => 'Sarah Connor',
            'email' => 'sarah.connor@example.com',
            'phone' => '+1 (555) 234-5678',
            'address' => '789 Cyberdyne Way, Los Angeles, CA',
            'total_purchases' => 1603.78,
            'loyalty_points' => 160,
        ]);

        // 6. Orders
        $order = Order::create([
            'id' => 'ord_1001',
            'order_number' => 'ORD-2025-1001',
            'organization_id' => 'org_apex',
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'customer_id' => 'cust_1',
            'customer_name' => 'Sarah Connor',
            'subtotal' => 1549.98,
            'discount_amount' => 65.00,
            'tax_amount' => 118.80,
            'total_amount' => 1603.78,
            'payment_method' => 'CARD',
            'payment_status' => 'PAID',
            'fulfillment_status' => 'COMPLETED',
            'notes' => 'Customer paid with Visa credit card.',
        ]);

        OrderItem::create([
            'id' => (string) Str::uuid(),
            'order_id' => 'ord_1001',
            'product_id' => 'prod_1',
            'product_name' => 'ProBook Ultra 15 M3',
            'sku' => 'APX-LAP-001',
            'unit_price' => 1299.99,
            'quantity' => 1,
            'discount_percent' => 5.0,
            'tax_amount' => 98.80,
            'total_price' => 1333.79,
        ]);
    }
}
