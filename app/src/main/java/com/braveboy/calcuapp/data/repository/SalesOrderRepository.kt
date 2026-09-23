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
        paymentStatus: PaymentStatus,
        fulfillmentStatus: FulfillmentStatus
    )
}

class SalesOrderRepositoryImpl(
    private val salesOrderDao: SalesOrderDao,
    private val productDao: ProductDao,
    private val ledgerDao: LedgerDao,
    private val cartDao: CartDao,
    private val customerDao: CustomerDao
) : SalesOrderRepository {

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
        require(cartItems.isNotEmpty()) { "Cart cannot be empty for checkout" }

        val orderCount = salesOrderDao.getOrderCountForOrg(orgId) + 1001
        val orderNumber = "ORD-${System.currentTimeMillis().toString().takeLast(4)}-$orderCount"

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

        val subtotal = cartItems.sumOf { it.subtotal }
        val discountAmount = cartItems.sumOf { it.discountAmount }
        val taxAmount = cartItems.sumOf { it.taxAmount }
        val totalAmount = cartItems.sumOf { it.total }

        val order = SalesOrder(
            id = UUID.randomUUID().toString(),
            orderNumber = orderNumber,
            orgId = orgId,
            storeId = storeId,
            warehouseId = warehouseId,
            customerId = customer?.id,
            customerName = customer?.name ?: "Walk-in Customer",
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

        // Save Order to DB
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
        paymentStatus: PaymentStatus,
        fulfillmentStatus: FulfillmentStatus
    ) {
        val existingOrder = salesOrderDao.getOrderById(orderId)
        // Note: For full status update implementation, we can fetch, modify, insert
    }
}
