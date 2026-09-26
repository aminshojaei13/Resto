<?php

use App\Http\Controllers\Api\AccountingController;
use App\Http\Controllers\Api\AuditLogController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\CustomerController;
use App\Http\Controllers\Api\ExpenseController;
use App\Http\Controllers\Api\InventoryController;
use App\Http\Controllers\Api\MessageImportController;
use App\Http\Controllers\Api\OrganizationController;
use App\Http\Controllers\Api\Platform\PlatformApplicationController;
use App\Http\Controllers\Api\ProductController;
use App\Http\Controllers\Api\Public\BusinessApplicationController;
use App\Http\Controllers\Api\PurchaseController;
use App\Http\Controllers\Api\SalesOrderController;
use App\Http\Controllers\Api\SupplierController;
use App\Http\Controllers\Api\Tenant\OnboardingController;
use App\Http\Middleware\IdempotencyMiddleware;
use App\Http\Middleware\TenantMiddleware;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->middleware([TenantMiddleware::class, IdempotencyMiddleware::class])->group(function () {
    // Public SaaS Registration
    Route::post('/business-applications', [BusinessApplicationController::class, 'store']);
    Route::get('/business-applications/{id}/status', [BusinessApplicationController::class, 'showStatus']);

    // Platform Admin Review & Provisioning
    Route::get('/platform/business-applications', [PlatformApplicationController::class, 'index']);
    Route::get('/platform/business-applications/{id}', [PlatformApplicationController::class, 'show']);
    Route::post('/platform/business-applications/{id}/approve', [PlatformApplicationController::class, 'approve']);
    Route::post('/platform/business-applications/{id}/reject', [PlatformApplicationController::class, 'reject']);

    // Tenant Onboarding
    Route::get('/tenant/onboarding', [OnboardingController::class, 'show']);
    Route::post('/tenant/onboarding/complete', [OnboardingController::class, 'complete']);

    // Auth & Profile
    Route::post('/auth/login', [AuthController::class, 'login']);
    Route::get('/auth/profile', [AuthController::class, 'profile']);

    // Organizations & Stores
    Route::get('/organizations', [OrganizationController::class, 'index']);
    Route::get('/organizations/{id}', [OrganizationController::class, 'show']);
    Route::post('/organizations', [OrganizationController::class, 'store']);
    Route::put('/organizations/{id}', [OrganizationController::class, 'update']);
    Route::get('/organizations/{orgId}/stores', [OrganizationController::class, 'stores']);
    Route::post('/stores', [OrganizationController::class, 'createStore']);
    Route::put('/stores/{id}', [OrganizationController::class, 'updateStore']);
    Route::get('/stores/{storeId}/warehouses', [OrganizationController::class, 'warehouses']);
    Route::post('/warehouses', [OrganizationController::class, 'createWarehouse']);
    Route::put('/warehouses/{id}', [OrganizationController::class, 'updateWarehouse']);

    // Products Catalog & Variants
    Route::get('/products', [ProductController::class, 'index']);
    Route::get('/products/barcode/{barcode}', [ProductController::class, 'barcode']);
    Route::get('/products/{id}', [ProductController::class, 'show']);
    Route::post('/products', [ProductController::class, 'store']);
    Route::put('/products/{id}', [ProductController::class, 'update']);
    Route::delete('/products/{id}', [ProductController::class, 'destroy']);
    Route::post('/products/{id}/variants', [ProductController::class, 'createVariant']);
    Route::put('/variants/{id}', [ProductController::class, 'updateVariant']);

    // Suppliers
    Route::get('/suppliers', [SupplierController::class, 'index']);
    Route::get('/suppliers/{id}', [SupplierController::class, 'show']);
    Route::post('/suppliers', [SupplierController::class, 'store']);
    Route::put('/suppliers/{id}', [SupplierController::class, 'update']);
    Route::delete('/suppliers/{id}', [SupplierController::class, 'destroy']);

    // Purchasing
    Route::get('/purchases', [PurchaseController::class, 'index']);
    Route::get('/purchases/{id}', [PurchaseController::class, 'show']);
    Route::post('/purchases', [PurchaseController::class, 'store']);
    Route::post('/purchases/{id}/receive', [PurchaseController::class, 'receive']);
    Route::post('/purchases/{id}/pay', [PurchaseController::class, 'pay']);

    // Inventory & Stock
    Route::get('/inventory/stock', [InventoryController::class, 'stock']);
    Route::post('/inventory/adjust', [InventoryController::class, 'adjust']);
    Route::post('/inventory/transfer', [InventoryController::class, 'transfer']);

    // Sales Orders & POS
    Route::get('/orders', [SalesOrderController::class, 'index']);
    Route::get('/orders/{id}', [SalesOrderController::class, 'show']);
    Route::post('/orders/checkout', [SalesOrderController::class, 'checkout']);
    Route::post('/orders/{id}/cancel', [SalesOrderController::class, 'cancel']);
    Route::post('/orders/{id}/refund', [SalesOrderController::class, 'refund']);

    // Customers
    Route::get('/customers', [CustomerController::class, 'index']);
    Route::get('/customers/{id}', [CustomerController::class, 'show']);
    Route::post('/customers', [CustomerController::class, 'store']);
    Route::put('/customers/{id}', [CustomerController::class, 'update']);
    Route::delete('/customers/{id}', [CustomerController::class, 'destroy']);

    // Expenses
    Route::get('/expenses', [ExpenseController::class, 'index']);
    Route::get('/expenses/{id}', [ExpenseController::class, 'show']);
    Route::post('/expenses', [ExpenseController::class, 'store']);
    Route::put('/expenses/{id}', [ExpenseController::class, 'update']);
    Route::delete('/expenses/{id}', [ExpenseController::class, 'destroy']);

    // Social Messages Import
    Route::get('/messages', [MessageImportController::class, 'index']);
    Route::post('/messages/parse', [MessageImportController::class, 'parse']);

    // Financial Accounting & Ledger
    Route::get('/accounting/accounts', [AccountingController::class, 'accounts']);
    Route::post('/accounting/accounts', [AccountingController::class, 'createAccount']);
    Route::get('/accounting/journal', [AccountingController::class, 'journal']);
    Route::post('/accounting/entry', [AccountingController::class, 'postEntry']);
    Route::get('/accounting/summary', [AccountingController::class, 'summary']);
    Route::get('/accounting/reports/profit-loss', [AccountingController::class, 'profitAndLoss']);
    Route::get('/accounting/reports/balance-sheet', [AccountingController::class, 'balanceSheet']);
    Route::get('/accounting/reports/trial-balance', [AccountingController::class, 'trialBalance']);

    // Audit Logs
    Route::get('/audit-logs', [AuditLogController::class, 'index']);
});
