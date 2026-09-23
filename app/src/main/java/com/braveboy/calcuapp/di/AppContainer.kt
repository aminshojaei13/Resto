package com.braveboy.calcuapp.di

import android.content.Context
import com.braveboy.calcuapp.data.local.datastore.TenantPreferences
import com.braveboy.calcuapp.data.local.db.AppDatabase
import com.braveboy.calcuapp.data.repository.CartRepository
import com.braveboy.calcuapp.data.repository.CartRepositoryImpl
import com.braveboy.calcuapp.data.repository.CustomerRepository
import com.braveboy.calcuapp.data.repository.CustomerRepositoryImpl
import com.braveboy.calcuapp.data.repository.LedgerRepository
import com.braveboy.calcuapp.data.repository.LedgerRepositoryImpl
import com.braveboy.calcuapp.data.repository.ProductRepository
import com.braveboy.calcuapp.data.repository.ProductRepositoryImpl
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
import com.braveboy.calcuapp.data.repository.SalesOrderRepositoryImpl
import com.braveboy.calcuapp.data.repository.TenantRepository
import com.braveboy.calcuapp.data.repository.TenantRepositoryImpl

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val tenantPreferences: TenantPreferences by lazy {
        TenantPreferences(context)
    }

    val tenantRepository: TenantRepository by lazy {
        TenantRepositoryImpl(
            tenantDao = database.tenantDao(),
            productDao = database.productDao(),
            customerDao = database.customerDao(),
            salesOrderDao = database.salesOrderDao(),
            ledgerDao = database.ledgerDao(),
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

    val ledgerRepository: LedgerRepository by lazy {
        LedgerRepositoryImpl(
            ledgerDao = database.ledgerDao(),
            salesOrderDao = database.salesOrderDao(),
            productDao = database.productDao()
        )
    }
}
