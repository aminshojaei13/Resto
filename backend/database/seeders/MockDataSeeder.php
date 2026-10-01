<?php

namespace Database\Seeders;

use App\Models\Account;
use App\Models\ChartOfAccounts;
use App\Models\Customer;
use App\Models\Expense;
use App\Models\JournalEntry;
use App\Models\JournalEntryLine;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Organization;
use App\Models\OrganizationMembership;
use App\Models\Payment;
use App\Models\Product;
use App\Models\ProductVariant;
use App\Models\Purchase;
use App\Models\PurchaseItem;
use App\Models\StockMovement;
use App\Models\Store;
use App\Models\Supplier;
use App\Models\SupplierPayment;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class MockDataSeeder extends Seeder
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
            'is_platform_admin' => true,
        ]);

        $cashier = User::create([
            'id' => 'usr_cashier_1',
            'name' => 'Elena Rostova',
            'email' => 'elena@calcuapp.com',
            'phone' => '+1 (555) 019-9988',
            'password' => Hash::make('password123'),
            'role' => 'Cashier',
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

        // Memberships
        OrganizationMembership::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_apex',
            'user_id' => 'usr_admin_1',
            'role' => 'Owner',
        ]);

        OrganizationMembership::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_apex',
            'user_id' => 'usr_cashier_1',
            'role' => 'Cashier',
        ]);

        OrganizationMembership::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_braveboy',
            'user_id' => 'usr_admin_1',
            'role' => 'Owner',
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

        $storeApex2 = Store::create([
            'id' => 'store_apex_2',
            'organization_id' => 'org_apex',
            'name' => 'Apex Express (Northside)',
            'code' => 'APX-NS',
            'address' => '450 North Blvd, San Francisco, CA',
            'phone' => '+1 (555) 019-3311',
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

        // 4. Chart of Accounts & Accounts
        $coaAssets = ChartOfAccounts::create([
            'id' => 'coa_asset',
            'organization_id' => 'org_apex',
            'code' => '1000',
            'name' => 'Assets',
            'type' => 'ASSET',
        ]);

        $coaLiab = ChartOfAccounts::create([
            'id' => 'coa_liability',
            'organization_id' => 'org_apex',
            'code' => '2000',
            'name' => 'Liabilities',
            'type' => 'LIABILITY',
        ]);

        $coaRev = ChartOfAccounts::create([
            'id' => 'coa_revenue',
            'organization_id' => 'org_apex',
            'code' => '4000',
            'name' => 'Revenue',
            'type' => 'REVENUE',
        ]);

        $coaExp = ChartOfAccounts::create([
            'id' => 'coa_expense',
            'organization_id' => 'org_apex',
            'code' => '5000',
            'name' => 'Expenses',
            'type' => 'EXPENSE',
        ]);

        $cashAcc = Account::create([
            'id' => 'acc_1010',
            'organization_id' => 'org_apex',
            'chart_of_account_id' => 'coa_asset',
            'code' => '1010',
            'name' => 'Cash / POS Drawer',
            'balance' => 15000.00,
        ]);

        $inventoryAcc = Account::create([
            'id' => 'acc_1200',
            'organization_id' => 'org_apex',
            'chart_of_account_id' => 'coa_asset',
            'code' => '1200',
            'name' => 'Inventory Asset',
            'balance' => 25000.00,
        ]);

        $payableAcc = Account::create([
            'id' => 'acc_2010',
            'organization_id' => 'org_apex',
            'chart_of_account_id' => 'coa_liability',
            'code' => '2010',
            'name' => 'Accounts Payable',
            'balance' => 5000.00,
        ]);

        $salesAcc = Account::create([
            'id' => 'acc_4010',
            'organization_id' => 'org_apex',
            'chart_of_account_id' => 'coa_revenue',
            'code' => '4010',
            'name' => 'Sales Revenue',
            'balance' => 35000.00,
        ]);

        $expAcc = Account::create([
            'id' => 'acc_5010',
            'organization_id' => 'org_apex',
            'chart_of_account_id' => 'coa_expense',
            'code' => '5010',
            'name' => 'Operating Expense',
            'balance' => 4500.00,
        ]);

        // 5. Products & Stock
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

        $monitor = Product::create([
            'id' => 'prod_3',
            'organization_id' => 'org_apex',
            'sku' => 'APX-DIS-003',
            'barcode' => '880609123403',
            'name' => 'UltraWide 34" Curved Display',
            'description' => '144Hz 4K IPS panel monitor with USB-C Hub.',
            'price' => 599.99,
            'cost_price' => 380.00,
            'category' => 'Electronics',
            'unit' => 'pcs',
            'image_url' => 'https://picsum.photos/id/1060/300/300',
        ]);

        WarehouseStock::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'org_apex',
            'warehouse_id' => 'wh_apex_1a',
            'product_id' => 'prod_3',
            'quantity' => 12,
        ]);

        // 6. Suppliers & Purchasing
        $sup1 = Supplier::create([
            'id' => 'sup_1',
            'organization_id' => 'org_apex',
            'name' => 'TechImport Global Co.',
            'email' => 'sales@techimport.com',
            'phone' => '+1 (800) 555-0199',
            'address' => '500 Logistics Way, San Jose, CA',
        ]);

        $sup2 = Supplier::create([
            'id' => 'sup_2',
            'organization_id' => 'org_apex',
            'name' => 'ElectroComponents Inc.',
            'email' => 'orders@electrocomponents.com',
            'phone' => '+1 (800) 555-0288',
            'address' => '12 Industrial Park, Austin, TX',
        ]);

        $po1 = Purchase::create([
            'id' => 'po_1001',
            'organization_id' => 'org_apex',
            'store_id' => 'store_apex_1',
            'warehouse_id' => 'wh_apex_1a',
            'supplier_id' => 'sup_1',
            'purchase_number' => 'PO-2025-1001',
            'total_amount' => 8500.00,
            'status' => 'RECEIVED',
            'payment_status' => 'PAID',
        ]);

        PurchaseItem::create([
            'id' => (string) Str::uuid(),
            'purchase_id' => 'po_1001',
            'product_id' => 'prod_1',
            'quantity' => 10,
            'unit_cost' => 850.00,
            'total_cost' => 8500.00,
        ]);

        // 7. Customers & Orders
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

        $john = Customer::create([
            'id' => 'cust_2',
            'organization_id' => 'org_apex',
            'name' => 'John Wick',
            'email' => 'john.wick@example.com',
            'phone' => '+1 (555) 999-0000',
            'address' => '1 Continental Hotel, New York, NY',
            'total_purchases' => 249.99,
            'loyalty_points' => 25,
        ]);

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

        // 8. Expenses
        Expense::create([
            'id' => 'exp_1001',
            'organization_id' => 'org_apex',
            'store_id' => 'store_apex_1',
            'category' => 'Store Utilities',
            'amount' => 450.00,
            'payment_method' => 'BANK_TRANSFER',
            'date' => date('Y-m-d'),
            'notes' => 'Monthly electricity bill for Apex Flagship',
            'user_id' => 'usr_admin_1',
        ]);
    }
}
