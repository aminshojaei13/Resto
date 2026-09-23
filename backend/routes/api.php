<?php

use App\Http\Controllers\Api\AccountingController;
use App\Http\Controllers\Api\AuditLogController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\CustomerController;
use App\Http\Controllers\Api\InventoryController;
use App\Http\Controllers\Api\OrganizationController;
use App\Http\Controllers\Api\ProductController;
use App\Http\Controllers\Api\SalesOrderController;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->middleware(['tenant'])->group(function () {
    // Auth & Profile
    Route::post('/auth/login', [AuthController::class, 'login']);
    Route::get('/auth/profile', [AuthController::class, 'profile']);

    // Organizations & Stores
    Route::get('/organizations', [OrganizationController::class, 'index']);
    Route::get('/organizations/{orgId}/stores', [OrganizationController::class, 'stores']);
    Route::get('/stores/{storeId}/warehouses', [OrganizationController::class, 'warehouses']);

    // Products Catalog
    Route::get('/products', [ProductController::class, 'index']);
    Route::get('/products/barcode/{barcode}', [ProductController::class, 'barcode']);
    Route::post('/products', [ProductController::class, 'store']);

    // Inventory & Stock
    Route::get('/inventory/stock', [InventoryController::class, 'stock']);
    Route::post('/inventory/adjust', [InventoryController::class, 'adjust']);
    Route::post('/inventory/transfer', [InventoryController::class, 'transfer']);

    // Sales Orders & POS
    Route::get('/orders', [SalesOrderController::class, 'index']);
    Route::get('/orders/{id}', [SalesOrderController::class, 'show']);
    Route::post('/orders/checkout', [SalesOrderController::class, 'checkout']);

    // Customers
    Route::get('/customers', [CustomerController::class, 'index']);
    Route::post('/customers', [CustomerController::class, 'store']);

    // Financial Accounting & Ledger
    Route::get('/accounting/journal', [AccountingController::class, 'journal']);
    Route::post('/accounting/entry', [AccountingController::class, 'postEntry']);
    Route::get('/accounting/summary', [AccountingController::class, 'summary']);

    // Audit Logs
    Route::get('/audit-logs', [AuditLogController::class, 'index']);
});
