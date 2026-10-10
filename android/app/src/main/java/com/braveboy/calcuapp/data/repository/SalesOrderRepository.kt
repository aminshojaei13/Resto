package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.CartDao
import com.braveboy.calcuapp.data.local.db.dao.CustomerDao
import com.braveboy.calcuapp.data.local.db.dao.LedgerDao
import com.braveboy.calcuapp.data.local.db.dao.ProductDao
import com.braveboy.calcuapp.data.local.db.dao.SalesOrderDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.OrderItem
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule
import com.braveboy.calcuapp.data.remote.dto.CheckoutItemDto
import com.braveboy.calcuapp.data.remote.dto.CheckoutRequestDto
import com.braveboy.calcuapp.data.remote.dto.OrderDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface SalesOrderRepository {
    fun getOrdersByStore(orgId: String, storeId: String): Flow<List<SalesOrder>>
    fun getAllOrdersByOrg(orgId: String): Flow<List<SalesOrder>>
    fun getOrderById(id: String): Flow<SalesOrder?>
    suspend fun processCheckout(
        orgId: String,
        storeId: String,
        warehouseId: String,
        customer: Customer?,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        notes: String = ""
    ): SalesOrder

    suspend fun updateOrderStatus(
        orderId: String,
        paymentStatus: PaymentStatus? = null,
        fulfillmentStatus: FulfillmentStatus? = null
    )
    suspend fun refreshOrders(orgId: String)
}

class SalesOrderRepositoryImpl(
    private val salesOrderDao: SalesOrderDao,
    private val productDao: ProductDao,
    private val ledgerDao: LedgerDao,
    private val cartDao: CartDao,
    private val customerDao: CustomerDao,
    private val apiService: CalcuappApiService = NetworkModule.apiService
) : SalesOrderRepository {

    override suspend fun refreshOrders(orgId: String) {
        if (orgId.isBlank()) return
        try {
            val response = apiService.getOrders(orgId = orgId)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val orders = dtos.map { dto ->
                    SalesOrder(
                        id = dto.id,
                        orderNumber = dto.orderNumber,
                        orgId = dto.organizationId,
                        storeId = dto.storeId,
                        warehouseId = dto.warehouseId,
                        customerId = dto.customerId,
                        customerName = dto.customerName,
                        items = emptyList(),
                        subtotal = dto.subtotal,
                        discountAmount = dto.discountAmount,
                        taxAmount = dto.taxAmount,
                        totalAmount = dto.totalAmount,
                        paymentMethod = try { PaymentMethod.valueOf(dto.paymentMethod) } catch (_: Exception) { PaymentMethod.CASH },
                        paymentStatus = try { PaymentStatus.valueOf(dto.paymentStatus) } catch (_: Exception) { PaymentStatus.PAID },
                        fulfillmentStatus = try { FulfillmentStatus.valueOf(dto.fulfillmentStatus) } catch (_: Exception) { FulfillmentStatus.COMPLETED },
                        notes = dto.notes ?: "",
                        createdAt = System.currentTimeMillis()
                    )
                }
                salesOrderDao.insertSalesOrders(orders.map { it.toEntity() })
            }
        } catch (_: Exception) {
            // Keep local cache if offline
        }
    }

    override fun getOrdersByStore(orgId: String, storeId: String): Flow<List<SalesOrder>> {
        return salesOrderDao.getOrdersByStore(orgId, storeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllOrdersByOrg(orgId: String): Flow<List<SalesOrder>> {
        return salesOrderDao.getAllOrdersByOrg(orgId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOrderById(id: String): Flow<SalesOrder?> {
        return salesOrderDao.getOrderById(id).map { it?.toDomain() }
    }

    override suspend fun processCheckout(
        orgId: String,
        storeId: String,
        warehouseId: String,
        customer: Customer?,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        notes: String
    ): SalesOrder {
        val validCartItems = cartItems.filter { it.productId.isNotBlank() }
        require(validCartItems.isNotEmpty()) { "Cart contains no valid products for checkout" }

        // Sync with remote API (Backend Source of Truth)
        val remoteOrder: OrderDto? = try {
            val response = apiService.checkout(
                CheckoutRequestDto(
                    orgId = orgId,
                    storeId = storeId,
                    warehouseId = warehouseId,
                    customerId = customer?.id,
                    customerName = customer?.name ?: "مشتری حضوری",
                    paymentMethod = paymentMethod.name,
                    notes = notes,
                    items = validCartItems.map {
                        CheckoutItemDto(
                            productId = it.productId,
                            variantId = it.variantId,
                            productName = it.productName,
                            sku = it.sku,
                            price = it.price,
                            quantity = it.quantity,
                            discountPercent = it.discountPercent,
                            taxRate = it.taxRate
                        )
                    }
                )
            )
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                val errorMsg = response.errorBody()?.string() ?: "خطای ناشناخته در سرور"
                throw IllegalStateException("خطا در ثبت سفارش در سرور (${response.code()}): $errorMsg")
            }
        } catch (e: Exception) {
            // Never silently swallow backend errors
            throw e
        }

        val orderCount = salesOrderDao.getOrderCountForOrg(orgId) + 1001
        val orderNumber = remoteOrder?.orderNumber ?: "ORD-${System.currentTimeMillis().toString().takeLast(4)}-$orderCount"

        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                productId = cartItem.productId,
                variantId = cartItem.variantId,
                productName = if (cartItem.variantName != null) "${cartItem.productName} (${cartItem.variantName})" else cartItem.productName,
                sku = cartItem.sku,
                unitPrice = cartItem.price,
                quantity = cartItem.quantity,
                discountPercent = cartItem.discountPercent,
                taxAmount = cartItem.taxAmount,
                totalPrice = cartItem.total
            )
        }

        val subtotal = remoteOrder?.subtotal ?: cartItems.sumOf { it.subtotal }
        val discountAmount = remoteOrder?.discountAmount ?: cartItems.sumOf { it.discountAmount }
        val taxAmount = remoteOrder?.taxAmount ?: cartItems.sumOf { it.taxAmount }
        val totalAmount = remoteOrder?.totalAmount ?: cartItems.sumOf { it.total }

        val order = SalesOrder(
            id = remoteOrder?.id ?: UUID.randomUUID().toString(),
            orderNumber = orderNumber,
            orgId = orgId,
            storeId = storeId,
            warehouseId = warehouseId,
            customerId = customer?.id,
            customerName = customer?.name ?: (remoteOrder?.customerName ?: "مشتری حضوری"),
            items = orderItems,
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            paymentStatus = PaymentStatus.PAID,
            fulfillmentStatus = FulfillmentStatus.COMPLETED,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        // Save Order to Room DB
        salesOrderDao.insertSalesOrder(order.toEntity())

        // Decrement product stock in warehouse
        for (item in cartItems) {
            val productEntity = productDao.getProductByIdDirect(item.productId) ?: continue
            val product = productEntity.toDomain()
            val currentStockMap = product.stockQuantityByWarehouse.toMutableMap()
            val currentQty = currentStockMap[warehouseId] ?: 0
            val newQty = (currentQty - item.quantity).coerceAtLeast(0)
            currentStockMap[warehouseId] = newQty

            val updatedProduct = product.copy(stockQuantityByWarehouse = currentStockMap)
            productDao.insertProduct(updatedProduct.toEntity())
        }

        // Record Ledger Entry
        val ledgerEntry = LedgerEntry(
            orgId = orgId,
            storeId = storeId,
            entryNumber = "LEDG-${order.orderNumber}",
            type = LedgerType.CREDIT,
            category = LedgerCategory.SALES,
            amount = order.totalAmount,
            description = "Sales Order #${order.orderNumber} via ${paymentMethod.displayName}",
            referenceId = order.id
        )
        ledgerDao.insertLedgerEntry(ledgerEntry.toEntity())

        // Update Customer loyalty/total purchases if customer present
        if (customer != null) {
            val updatedCustomer = customer.copy(
                totalPurchases = customer.totalPurchases + order.totalAmount,
                loyaltyPoints = customer.loyaltyPoints + (order.totalAmount / 10.0).toInt()
            )
            customerDao.insertCustomer(updatedCustomer.toEntity())
        }

        // Clear cart
        cartDao.clearCart(orgId, storeId)

        return order
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        paymentStatus: PaymentStatus?,
        fulfillmentStatus: FulfillmentStatus?
    ) {
        val entity = salesOrderDao.getOrderByIdDirect(orderId) ?: return
        val updated = entity.copy(
            paymentStatus = paymentStatus ?: entity.paymentStatus,
            fulfillmentStatus = fulfillmentStatus ?: entity.fulfillmentStatus
        )
        salesOrderDao.insertSalesOrder(updated)
    }
}
