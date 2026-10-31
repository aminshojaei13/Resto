package com.braveboy.calcuapp.data.sync

import com.braveboy.calcuapp.data.local.db.AppDatabase
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.dto.CheckoutItemDto
import com.braveboy.calcuapp.data.remote.dto.CheckoutRequestDto
import com.braveboy.calcuapp.data.remote.dto.CustomerDto
import com.braveboy.calcuapp.data.remote.dto.ExpenseDto
import com.braveboy.calcuapp.data.remote.dto.ProductDto
import com.braveboy.calcuapp.data.remote.dto.SupplierDto
import com.braveboy.calcuapp.data.repository.CustomerRepository
import com.braveboy.calcuapp.data.repository.ExpenseRepository
import com.braveboy.calcuapp.data.repository.ProductRepository
import com.braveboy.calcuapp.data.repository.PurchaseRepository
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
import com.braveboy.calcuapp.data.repository.SupplierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

class SyncManager(
    private val database: AppDatabase,
    private val apiService: CalcuappApiService,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val expenseRepository: ExpenseRepository,
    private val purchaseRepository: PurchaseRepository
) {
    private val _syncState = MutableStateFlow(SyncStatus.IDLE)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    val unsyncedCount: Flow<Int> = database.salesOrderDao().getTotalUnsyncedCount()

    suspend fun triggerSync(orgId: String): Boolean = withContext(Dispatchers.IO) {
        if (orgId.isBlank()) return@withContext false
        _syncState.value = SyncStatus.SYNCING

        var pushSuccess = true

        try {
            // 1. Push Unsynced Sales Orders
            val unsyncedOrders = database.salesOrderDao().getUnsyncedSalesOrders()
            for (order in unsyncedOrders) {
                try {
                    val response = apiService.checkout(
                        request = CheckoutRequestDto(
                            orgId = order.orgId,
                            storeId = order.storeId,
                            warehouseId = order.warehouseId,
                            customerId = order.customerId,
                            customerName = order.customerName,
                            paymentMethod = order.paymentMethod.name,
                            notes = order.notes,
                            source = "POS_OFFLINE",
                            paymentStatus = order.paymentStatus.name,
                            fulfillmentStatus = order.fulfillmentStatus.name,
                            items = order.items.map { item ->
                                CheckoutItemDto(
                                    productId = item.productId,
                                    variantId = item.variantId,
                                    productName = item.productName,
                                    sku = item.sku,
                                    price = item.unitPrice,
                                    quantity = item.quantity,
                                    discountPercent = item.discountPercent,
                                    taxRate = 0.0
                                )
                            }
                        ),
                        idempotencyKey = order.id
                    )
                    if (response.isSuccessful) {
                        database.salesOrderDao().markSalesOrderSynced(order.id)
                    } else {
                        pushSuccess = false
                    }
                } catch (e: Exception) {
                    pushSuccess = false
                    if (e is IOException) {
                        _syncState.value = SyncStatus.OFFLINE
                        return@withContext false
                    }
                }
            }

            // 2. Push Unsynced Products
            val unsyncedProducts = database.productDao().getUnsyncedProducts()
            for (product in unsyncedProducts) {
                try {
                    val dto = ProductDto(
                        id = product.id,
                        organizationId = product.orgId,
                        sku = product.sku,
                        barcode = product.barcode,
                        name = product.name,
                        description = product.description,
                        price = product.price,
                        costPrice = product.costPrice,
                        category = product.category,
                        unit = product.unit,
                        imageUrl = product.imageUrl
                    )
                    val response = apiService.addProduct(dto)
                    if (response.isSuccessful) {
                        database.productDao().markProductSynced(product.id)
                    } else {
                        pushSuccess = false
                    }
                } catch (e: Exception) {
                    pushSuccess = false
                    if (e is IOException) {
                        _syncState.value = SyncStatus.OFFLINE
                        return@withContext false
                    }
                }
            }

            // 3. Push Unsynced Customers
            val unsyncedCustomers = database.customerDao().getUnsyncedCustomers()
            for (customer in unsyncedCustomers) {
                try {
                    val dto = CustomerDto(
                        id = customer.id,
                        organizationId = customer.orgId,
                        name = customer.name,
                        email = customer.email,
                        phone = customer.phone,
                        address = customer.address,
                        totalPurchases = customer.totalPurchases,
                        loyaltyPoints = customer.loyaltyPoints
                    )
                    val response = apiService.addCustomer(dto)
                    if (response.isSuccessful) {
                        database.customerDao().markCustomerSynced(customer.id)
                    } else {
                        pushSuccess = false
                    }
                } catch (e: Exception) {
                    pushSuccess = false
                    if (e is IOException) {
                        _syncState.value = SyncStatus.OFFLINE
                        return@withContext false
                    }
                }
            }

            // 4. Push Unsynced Expenses
            val unsyncedExpenses = database.expenseDao().getUnsyncedExpenses()
            for (expense in unsyncedExpenses) {
                try {
                    val dto = ExpenseDto(
                        id = expense.id,
                        organizationId = expense.orgId,
                        storeId = expense.storeId,
                        category = expense.category,
                        amount = expense.amount,
                        paymentMethod = expense.paymentMethod,
                        date = expense.date,
                        notes = expense.notes
                    )
                    val response = apiService.addExpense(dto)
                    if (response.isSuccessful) {
                        database.expenseDao().markExpenseSynced(expense.id)
                    } else {
                        pushSuccess = false
                    }
                } catch (e: Exception) {
                    pushSuccess = false
                    if (e is IOException) {
                        _syncState.value = SyncStatus.OFFLINE
                        return@withContext false
                    }
                }
            }

            // 5. Push Unsynced Suppliers
            val unsyncedSuppliers = database.supplierDao().getUnsyncedSuppliers()
            for (supplier in unsyncedSuppliers) {
                try {
                    val dto = SupplierDto(
                        id = supplier.id,
                        organizationId = supplier.orgId,
                        name = supplier.name,
                        email = supplier.email,
                        phone = supplier.phone,
                        address = supplier.address
                    )
                    val response = apiService.addSupplier(dto)
                    if (response.isSuccessful) {
                        database.supplierDao().markSupplierSynced(supplier.id)
                    } else {
                        pushSuccess = false
                    }
                } catch (e: Exception) {
                    pushSuccess = false
                    if (e is IOException) {
                        _syncState.value = SyncStatus.OFFLINE
                        return@withContext false
                    }
                }
            }

            // 6. Push Unsynced Purchases
            val unsyncedPurchases = database.purchaseDao().getUnsyncedPurchases()
            for (purchase in unsyncedPurchases) {
                try {
                    val payload = mapOf<String, Any>(
                        "store_id" to purchase.storeId,
                        "warehouse_id" to purchase.warehouseId,
                        "supplier_id" to purchase.supplierId,
                        "items" to purchase.items.map { item ->
                            mapOf(
                                "product_id" to item.productId,
                                "quantity" to item.quantity,
                                "unit_cost" to item.unitCost
                            )
                        }
                    )
                    val response = apiService.createPurchase(payload)
                    if (response.isSuccessful) {
                        database.purchaseDao().markPurchaseSynced(purchase.id)
                    } else {
                        pushSuccess = false
                    }
                } catch (e: Exception) {
                    pushSuccess = false
                    if (e is IOException) {
                        _syncState.value = SyncStatus.OFFLINE
                        return@withContext false
                    }
                }
            }

            // 7. Pull Latest Remote Data from Backend
            productRepository.refreshProducts(orgId)
            customerRepository.refreshCustomers(orgId)
            supplierRepository.refreshSuppliers(orgId)
            salesOrderRepository.refreshOrders(orgId)
            expenseRepository.refreshExpenses(orgId)
            purchaseRepository.refreshPurchases(orgId)

            if (pushSuccess) {
                _syncState.value = SyncStatus.SUCCESS
            } else {
                _syncState.value = SyncStatus.ERROR
            }
            return@withContext pushSuccess
        } catch (e: Exception) {
            if (e is IOException) {
                _syncState.value = SyncStatus.OFFLINE
            } else {
                _syncState.value = SyncStatus.ERROR
            }
            return@withContext false
        }
    }
}
