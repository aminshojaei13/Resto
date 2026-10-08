package com.braveboy.calcuapp.di

import android.content.Context
import com.braveboy.calcuapp.data.local.datastore.TenantPreferences
import com.braveboy.calcuapp.data.local.db.AppDatabase
import com.braveboy.calcuapp.data.repository.CartRepository
import com.braveboy.calcuapp.data.repository.CartRepositoryImpl
import com.braveboy.calcuapp.data.repository.CustomerRepository
import com.braveboy.calcuapp.data.repository.CustomerRepositoryImpl
import com.braveboy.calcuapp.data.repository.ExpenseRepository
import com.braveboy.calcuapp.data.repository.ExpenseRepositoryImpl
import com.braveboy.calcuapp.data.repository.LedgerRepository
import com.braveboy.calcuapp.data.repository.LedgerRepositoryImpl
import com.braveboy.calcuapp.data.repository.MessageRepository
import com.braveboy.calcuapp.data.repository.MessageRepositoryImpl
import com.braveboy.calcuapp.data.repository.ProductRepository
import com.braveboy.calcuapp.data.repository.ProductRepositoryImpl
import com.braveboy.calcuapp.data.repository.PurchaseRepository
import com.braveboy.calcuapp.data.repository.PurchaseRepositoryImpl
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
import com.braveboy.calcuapp.data.repository.SalesOrderRepositoryImpl
import com.braveboy.calcuapp.data.repository.SupplierRepository
import com.braveboy.calcuapp.data.repository.SupplierRepositoryImpl
import com.braveboy.calcuapp.data.repository.TenantRepository
import com.braveboy.calcuapp.data.local.datastore.SessionStore
import com.braveboy.calcuapp.data.remote.NetworkModule
import com.braveboy.calcuapp.data.repository.AuthRepository
import com.braveboy.calcuapp.data.repository.AuthRepositoryImpl
import com.braveboy.calcuapp.data.repository.TenantRepositoryImpl
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
}
