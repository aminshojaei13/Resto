<?php

use App\Http\Controllers\Api\AccountingController;
use App\Http\Controllers\Api\BusinessSettingsController;
use App\Http\Controllers\Api\AuditLogController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\CustomerController;
use App\Http\Controllers\Api\ExpenseController;
use App\Http\Controllers\Api\HealthController;
use App\Http\Controllers\Api\InventoryController;
use App\Http\Controllers\Api\MessageImportController;
use App\Http\Controllers\Api\OrganizationController;
use App\Http\Controllers\Api\Platform\PlatformApplicationController;
use App\Http\Controllers\Api\ProductController;
use App\Http\Controllers\Api\Public\BusinessApplicationController;
use App\Http\Controllers\Api\PurchaseController;
use App\Http\Controllers\Api\SalesOrderController;
use App\Http\Controllers\Api\StaffController;
use App\Http\Controllers\Api\SupplierController;
use App\Http\Controllers\Api\Tenant\OnboardingController;
use App\Http\Middleware\EnsurePermission;
use App\Http\Middleware\EnsurePlatformAdmin;
use App\Http\Middleware\EnsureUserIsActive;
use App\Http\Middleware\IdempotencyMiddleware;
use App\Http\Middleware\TenantMiddleware;
use App\Support\RolePermission;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->middleware([IdempotencyMiddleware::class])->group(function () {

    /* ------------------------------------------------------------------
     | Public. Reachable without an account, and rate limited.
     | ------------------------------------------------------------------ */
    Route::get('/health', [HealthController::class, 'check']);

    Route::post('/business-applications', [BusinessApplicationController::class, 'store'])
        ->middleware('throttle:20,1');
    Route::get('/business-applications/{id}/status', [BusinessApplicationController::class, 'showStatus'])
        ->middleware('throttle:30,1');

    Route::post('/auth/login', [AuthController::class, 'login'])
        ->middleware('throttle:login');

    Route::post('/auth/forgot-password', [AuthController::class, 'forgotPassword'])
        ->middleware('throttle:password-reset');

    Route::post('/auth/reset-password', [AuthController::class, 'resetPassword'])
        ->middleware('throttle:password-reset');

    // Staff invitation: the token in the link is the only credential, so these
    // two endpoints stay public but can only ever act on that one invitation.
    Route::get('/staff/invitations/{token}', [StaffController::class, 'showInvitation'])
        ->middleware('throttle:30,1');
    Route::post('/staff/invitations/accept', [StaffController::class, 'accept'])
        ->middleware('throttle:10,1');

    /* ------------------------------------------------------------------
     | Authenticated business API.
     |
     | auth:sanctum  -> the caller must present a real token
     | user.active   -> suspended accounts are rejected and lose their tokens
     | tenant        -> the active business must be one they actually belong to
     | ------------------------------------------------------------------ */
    Route::middleware([
        'auth:sanctum',
        EnsureUserIsActive::class,
        TenantMiddleware::class,
    ])->group(function () {

        // Identity
        Route::get('/auth/profile', [AuthController::class, 'profile']);
        Route::put('/auth/profile', [AuthController::class, 'updateProfile']);
        Route::post('/auth/logout', [AuthController::class, 'logout']);
        Route::post('/auth/logout-all', [AuthController::class, 'logoutAll']);
        Route::post('/auth/change-password', [AuthController::class, 'changePassword'])
            ->middleware('throttle:password-change');

        // Onboarding
        Route::get('/tenant/onboarding', [OnboardingController::class, 'show']);
        Route::post('/tenant/onboarding/complete', [OnboardingController::class, 'complete'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);

        // Staff
        Route::get('/staff', [StaffController::class, 'index'])
            ->middleware('permission:' . RolePermission::STAFF_VIEW);
        Route::post('/staff/invitations', [StaffController::class, 'store'])
            ->middleware('permission:' . RolePermission::STAFF_MANAGE);
        Route::post('/staff/invitations/{id}/resend', [StaffController::class, 'resend'])
            ->middleware('permission:' . RolePermission::STAFF_MANAGE);
        Route::delete('/staff/invitations/{id}', [StaffController::class, 'revoke'])
            ->middleware('permission:' . RolePermission::STAFF_MANAGE);
        Route::put('/staff/{userId}/role', [StaffController::class, 'updateRole'])
            ->middleware('permission:' . RolePermission::STAFF_MANAGE);
        Route::put('/staff/{userId}/status', [StaffController::class, 'setStatus'])
            ->middleware('permission:' . RolePermission::STAFF_MANAGE);
        Route::delete('/staff/{userId}', [StaffController::class, 'destroy'])
            ->middleware('permission:' . RolePermission::STAFF_MANAGE);

        // Business settings — the authoritative home of the sales tax rate.
        Route::get('/business/settings', [BusinessSettingsController::class, 'show'])
            ->middleware('permission:' . RolePermission::BUSINESS_VIEW);
        Route::put('/business/settings', [BusinessSettingsController::class, 'update'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);
        Route::get('/units', [BusinessSettingsController::class, 'units'])
            ->middleware('permission:' . RolePermission::CATALOG_VIEW);

        // Business settings
        Route::get('/organizations', [OrganizationController::class, 'index']);
        Route::get('/organizations/{id}', [OrganizationController::class, 'show']);
        Route::post('/organizations', [OrganizationController::class, 'store'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);
        Route::put('/organizations/{id}', [OrganizationController::class, 'update'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);
        Route::get('/organizations/{orgId}/stores', [OrganizationController::class, 'stores']);
        Route::post('/stores', [OrganizationController::class, 'createStore'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);
        Route::put('/stores/{id}', [OrganizationController::class, 'updateStore'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);
        Route::get('/stores/{storeId}/warehouses', [OrganizationController::class, 'warehouses']);
        Route::post('/warehouses', [OrganizationController::class, 'createWarehouse'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);
        Route::put('/warehouses/{id}', [OrganizationController::class, 'updateWarehouse'])
            ->middleware('permission:' . RolePermission::BUSINESS_MANAGE);

        // Catalog
        Route::get('/products', [ProductController::class, 'index'])
            ->middleware('permission:' . RolePermission::CATALOG_VIEW);
        Route::get('/products/barcode/{barcode}', [ProductController::class, 'barcode'])
            ->middleware('permission:' . RolePermission::CATALOG_VIEW);
        Route::get('/products/{id}', [ProductController::class, 'show'])
            ->middleware('permission:' . RolePermission::CATALOG_VIEW);
        Route::post('/products', [ProductController::class, 'store'])
            ->middleware('permission:' . RolePermission::CATALOG_MANAGE);
        Route::put('/products/{id}', [ProductController::class, 'update'])
            ->middleware('permission:' . RolePermission::CATALOG_MANAGE);
        Route::delete('/products/{id}', [ProductController::class, 'destroy'])
            ->middleware('permission:' . RolePermission::CATALOG_MANAGE);
        Route::post('/products/{id}/variants', [ProductController::class, 'createVariant'])
            ->middleware('permission:' . RolePermission::CATALOG_MANAGE);
        Route::put('/variants/{id}', [ProductController::class, 'updateVariant'])
            ->middleware('permission:' . RolePermission::CATALOG_MANAGE);

        // Suppliers
        Route::get('/suppliers', [SupplierController::class, 'index'])
            ->middleware('permission:' . RolePermission::PURCHASING_VIEW);
        Route::get('/suppliers/{id}', [SupplierController::class, 'show'])
            ->middleware('permission:' . RolePermission::PURCHASING_VIEW);
        Route::post('/suppliers', [SupplierController::class, 'store'])
            ->middleware('permission:' . RolePermission::PURCHASING_MANAGE);
        Route::put('/suppliers/{id}', [SupplierController::class, 'update'])
            ->middleware('permission:' . RolePermission::PURCHASING_MANAGE);
        Route::delete('/suppliers/{id}', [SupplierController::class, 'destroy'])
            ->middleware('permission:' . RolePermission::PURCHASING_MANAGE);

        // Purchasing & receiving
        Route::get('/purchases', [PurchaseController::class, 'index'])
            ->middleware('permission:' . RolePermission::PURCHASING_VIEW);
        // Registered before /purchases/{id} so the literal segment is not
        // swallowed by the id pattern.
        Route::get('/purchases/receiving-queue', [PurchaseController::class, 'receivingQueue'])
            ->middleware('permission:' . RolePermission::INVENTORY_VIEW);
        Route::get('/purchases/{id}', [PurchaseController::class, 'show'])
            ->middleware('permission:' . RolePermission::PURCHASING_VIEW);
        Route::post('/purchases', [PurchaseController::class, 'store'])
            ->middleware('permission:' . RolePermission::PURCHASING_MANAGE);
        Route::post('/purchases/{id}/receive', [PurchaseController::class, 'receive'])
            ->middleware('permission:' . RolePermission::INVENTORY_RECEIVE);
        Route::post('/purchases/{id}/pay', [PurchaseController::class, 'pay'])
            ->middleware('permission:' . RolePermission::PURCHASING_MANAGE);

        // Inventory
        Route::get('/inventory/warehouses', [InventoryController::class, 'warehouses'])
            ->middleware('permission:' . RolePermission::INVENTORY_VIEW);
        Route::get('/inventory/stock', [InventoryController::class, 'stock'])
            ->middleware('permission:' . RolePermission::INVENTORY_VIEW);
        Route::get('/inventory/movements', [InventoryController::class, 'movements'])
            ->middleware('permission:' . RolePermission::INVENTORY_VIEW);
        Route::post('/inventory/stock-in', [InventoryController::class, 'stockIn'])
            ->middleware('permission:' . RolePermission::INVENTORY_ADJUST);
        Route::post('/inventory/adjust', [InventoryController::class, 'adjust'])
            ->middleware('permission:' . RolePermission::INVENTORY_ADJUST);
        Route::post('/inventory/transfer', [InventoryController::class, 'transfer'])
            ->middleware('permission:' . RolePermission::INVENTORY_ADJUST);

        // Sales & point of sale
        Route::get('/orders', [SalesOrderController::class, 'index'])
            ->middleware('permission:' . RolePermission::SALES_VIEW);
        Route::get('/orders/{id}', [SalesOrderController::class, 'show'])
            ->middleware('permission:' . RolePermission::SALES_VIEW);
        Route::post('/orders/checkout', [SalesOrderController::class, 'checkout'])
            ->middleware('permission:' . RolePermission::SALES_CREATE);
        Route::post('/orders/{id}/prepare', [SalesOrderController::class, 'prepare'])
            ->middleware('permission:' . RolePermission::SALES_CREATE);
        Route::post('/orders/{id}/pay', [SalesOrderController::class, 'pay'])
            ->middleware('permission:' . RolePermission::SALES_CREATE);
        Route::post('/orders/{id}/cancel', [SalesOrderController::class, 'cancel'])
            ->middleware('permission:' . RolePermission::SALES_CREATE);
        Route::post('/orders/{id}/refund', [SalesOrderController::class, 'refund'])
            ->middleware('permission:' . RolePermission::SALES_REFUND);

        // Customers
        Route::get('/customers', [CustomerController::class, 'index'])
            ->middleware('permission:' . RolePermission::CUSTOMERS_VIEW);
        Route::get('/customers/{id}', [CustomerController::class, 'show'])
            ->middleware('permission:' . RolePermission::CUSTOMERS_VIEW);
        Route::post('/customers', [CustomerController::class, 'store'])
            ->middleware('permission:' . RolePermission::CUSTOMERS_MANAGE);
        Route::put('/customers/{id}', [CustomerController::class, 'update'])
            ->middleware('permission:' . RolePermission::CUSTOMERS_MANAGE);
        Route::delete('/customers/{id}', [CustomerController::class, 'destroy'])
            ->middleware('permission:' . RolePermission::CUSTOMERS_MANAGE);

        // Expenses
        Route::get('/expenses', [ExpenseController::class, 'index'])
            ->middleware('permission:' . RolePermission::EXPENSES_VIEW);
        // Registered before /expenses/{id} for the same reason as purchases.
        Route::get('/expenses/categories', [ExpenseController::class, 'categories'])
            ->middleware('permission:' . RolePermission::EXPENSES_VIEW);
        Route::get('/expenses/summary', [ExpenseController::class, 'summary'])
            ->middleware('permission:' . RolePermission::EXPENSES_VIEW);
        Route::get('/expenses/{id}', [ExpenseController::class, 'show'])
            ->middleware('permission:' . RolePermission::EXPENSES_VIEW);
        Route::post('/expenses', [ExpenseController::class, 'store'])
            ->middleware('permission:' . RolePermission::EXPENSES_MANAGE);
        Route::put('/expenses/{id}', [ExpenseController::class, 'update'])
            ->middleware('permission:' . RolePermission::EXPENSES_MANAGE);
        Route::delete('/expenses/{id}', [ExpenseController::class, 'destroy'])
            ->middleware('permission:' . RolePermission::EXPENSES_MANAGE);

        // Social order import
        Route::get('/messages', [MessageImportController::class, 'index'])
            ->middleware('permission:' . RolePermission::MESSAGES_VIEW);
        Route::post('/messages/parse', [MessageImportController::class, 'parse'])
            ->middleware('permission:' . RolePermission::MESSAGES_IMPORT);

        // Accounting
        Route::get('/accounting/accounts', [AccountingController::class, 'accounts'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_VIEW);
        Route::post('/accounting/accounts', [AccountingController::class, 'createAccount'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_MANAGE);
        Route::get('/accounting/journal', [AccountingController::class, 'journal'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_VIEW);
        Route::post('/accounting/entry', [AccountingController::class, 'postEntry'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_MANAGE);
        Route::get('/accounting/summary', [AccountingController::class, 'summary'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_VIEW);
        Route::get('/accounting/reports/profit-loss', [AccountingController::class, 'profitAndLoss'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_VIEW);
        Route::get('/accounting/reports/balance-sheet', [AccountingController::class, 'balanceSheet'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_VIEW);
        Route::get('/accounting/reports/trial-balance', [AccountingController::class, 'trialBalance'])
            ->middleware('permission:' . RolePermission::ACCOUNTING_VIEW);

        // Audit trail
        Route::get('/audit-logs', [AuditLogController::class, 'index'])
            ->middleware('permission:' . RolePermission::AUDIT_VIEW);
    });

    /* ------------------------------------------------------------------
     | Platform console. Authenticated platform administrators only.
     | ------------------------------------------------------------------ */
    Route::prefix('platform')
        ->middleware(['auth:sanctum', EnsureUserIsActive::class, EnsurePlatformAdmin::class])
        ->group(function () {
            Route::get('/business-applications', [PlatformApplicationController::class, 'index']);
            Route::get('/business-applications/{id}', [PlatformApplicationController::class, 'show']);
            Route::post('/business-applications/{id}/approve', [PlatformApplicationController::class, 'approve']);
            Route::post('/business-applications/{id}/reject', [PlatformApplicationController::class, 'reject']);
        });
});
