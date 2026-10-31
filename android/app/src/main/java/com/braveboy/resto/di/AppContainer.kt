package com.braveboy.resto.di

import android.content.Context
import com.braveboy.resto.data.local.datastore.TenantPreferences
import com.braveboy.resto.data.local.db.AppDatabase
import com.braveboy.resto.data.repository.CartRepository
import com.braveboy.resto.data.repository.CartRepositoryImpl
import com.braveboy.resto.data.repository.CustomerRepository
import com.braveboy.resto.data.repository.CustomerRepositoryImpl
import com.braveboy.resto.data.repository.ExpenseRepository
import com.braveboy.resto.data.repository.ExpenseRepositoryImpl
import com.braveboy.resto.data.repository.LedgerRepository
import com.braveboy.resto.data.repository.LedgerRepositoryImpl
import com.braveboy.resto.data.repository.MessageRepository
import com.braveboy.resto.data.repository.MessageRepositoryImpl
import com.braveboy.resto.data.repository.ProductRepository
import com.braveboy.resto.data.repository.ProductRepositoryImpl
import com.braveboy.resto.data.repository.PurchaseRepository
import com.braveboy.resto.data.repository.PurchaseRepositoryImpl
import com.braveboy.resto.data.repository.SalesOrderRepository
import com.braveboy.resto.data.repository.SalesOrderRepositoryImpl
import com.braveboy.resto.data.repository.SupplierRepository
import com.braveboy.resto.data.repository.SupplierRepositoryImpl
import com.braveboy.resto.data.repository.TenantRepository
import com.braveboy.resto.data.local.datastore.SessionStore
import com.braveboy.resto.data.remote.NetworkModule
import com.braveboy.resto.data.repository.AuthRepository
import com.braveboy.resto.data.repository.AuthRepositoryImpl
import com.braveboy.resto.data.repository.TenantRepositoryImpl
import com.braveboy.resto.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val sessionStore: SessionStore by lazy {
        SessionStore(context)
    }

    val tenantPreferences: TenantPreferences by lazy {
        TenantPreferences(context)
    }

    val guidePreferences: com.braveboy.resto.ui.guide.GuidePreferences by lazy {
        com.braveboy.resto.ui.guide.GuidePreferences(context)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            apiService = NetworkModule.apiService,
            sessionStore = sessionStore,
            tenantPreferences = tenantPreferences,
            database = database
        )
    }

    init {
        NetworkModule.initialize(sessionStore) {
            CoroutineScope(Dispatchers.IO).launch {
                authRepository.invalidateSession()
            }
        }
    }

    val tenantRepository: TenantRepository by lazy {
        TenantRepositoryImpl(
            tenantDao = database.tenantDao(),
            tenantPreferences = tenantPreferences
        )
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            productDao = database.productDao(),
            ledgerDao = database.ledgerDao()
        )
    }

    val cartRepository: CartRepository by lazy {
        CartRepositoryImpl(
            cartDao = database.cartDao()
        )
    }

    val salesOrderRepository: SalesOrderRepository by lazy {
        SalesOrderRepositoryImpl(
            salesOrderDao = database.salesOrderDao(),
            productDao = database.productDao(),
            ledgerDao = database.ledgerDao(),
            cartDao = database.cartDao(),
            customerDao = database.customerDao()
        )
    }

    val customerRepository: CustomerRepository by lazy {
        CustomerRepositoryImpl(
            customerDao = database.customerDao()
        )
    }

    val supplierRepository: SupplierRepository by lazy {
        SupplierRepositoryImpl(
            supplierDao = database.supplierDao()
        )
    }

    val purchaseRepository: PurchaseRepository by lazy {
        PurchaseRepositoryImpl(
            purchaseDao = database.purchaseDao()
        )
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(
            expenseDao = database.expenseDao()
        )
    }

    val messageRepository: MessageRepository by lazy {
        MessageRepositoryImpl()
    }

    val ledgerRepository: LedgerRepository by lazy {
        LedgerRepositoryImpl(
            ledgerDao = database.ledgerDao(),
            salesOrderDao = database.salesOrderDao(),
            productDao = database.productDao()
        )
    }

    val syncManager: SyncManager by lazy {
        SyncManager(
            database = database,
            apiService = NetworkModule.apiService,
            productRepository = productRepository,
            customerRepository = customerRepository,
            supplierRepository = supplierRepository,
            salesOrderRepository = salesOrderRepository,
            expenseRepository = expenseRepository,
            purchaseRepository = purchaseRepository
        )
    }
}
