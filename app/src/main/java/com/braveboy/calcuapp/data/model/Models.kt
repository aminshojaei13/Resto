package com.braveboy.calcuapp.data.model

import java.util.UUID

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    CARD("Card / POS Terminal"),
    MOBILE_PAYMENT("Mobile / QR Payment"),
    STORE_CREDIT("Store Credit / Account")
}

enum class PaymentStatus(val displayName: String) {
    PAID("Paid"),
    PENDING("Pending"),
    PARTIAL("Partial"),
    REFUNDED("Refunded")
}

enum class FulfillmentStatus(val displayName: String) {
    COMPLETED("Completed"),
    PROCESSING("Processing"),
    PENDING("Pending"),
    CANCELLED("Cancelled")
}

enum class LedgerType {
    DEBIT,  // Expense, Cash Out, Inventory Purchase
    CREDIT  // Income, Sales, Cash In
}

enum class LedgerCategory(val displayName: String) {
    SALES("Sales Income"),
    INVENTORY_ADJUSTMENT("Stock Adjustment"),
    EXPENSE("Operating Expense"),
    CASH_IN("Cash Deposit / Float"),
    CASH_OUT("Cash Withdrawal / Payout"),
    REFUND("Customer Refund")
}

data class TenantState(
    val activeOrgId: String = "",
    val activeStoreId: String = "",
    val activeWarehouseId: String = "",
    val userId: String = "usr_admin_1",
    val userName: String = "Admin User",
    val userRole: String = "Manager / Owner",
    val isLoggedIn: Boolean = true
)

data class Organization(
    val id: String,
    val name: String,
    val code: String,
    val logoUrl: String = "",
    val currencySymbol: String = "$",
    val currencyCode: String = "USD",
    val subscriptionTier: String = "ENTERPRISE",
    val stores: List<Store> = emptyList()
)

data class Store(
    val id: String,
    val orgId: String,
    val name: String,
    val code: String,
    val address: String,
    val phone: String = "",
    val warehouses: List<Warehouse> = emptyList()
)

data class Warehouse(
    val id: String,
    val storeId: String,
    val orgId: String,
    val name: String,
    val code: String,
    val address: String
)

data class ProductVariant(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val sku: String,
    val barcode: String,
    val name: String,
    val price: Double,
    val costPrice: Double,
    val stockQuantityByWarehouse: Map<String, Int> = emptyMap()
)

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val orgId: String,
    val sku: String,
    val barcode: String,
    val name: String,
    val description: String = "",
    val price: Double,
    val costPrice: Double,
    val category: String,
    val imageUrl: String = "",
    val unit: String = "pcs",
    val variants: List<ProductVariant> = emptyList(),
    val stockQuantityByWarehouse: Map<String, Int> = emptyMap() // warehouseId -> qty
) {
    fun getTotalStock(): Int = stockQuantityByWarehouse.values.sum()
    fun getStockForWarehouse(warehouseId: String): Int = stockQuantityByWarehouse[warehouseId] ?: 0
}

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val orgId: String,
    val storeId: String,
    val productId: String,
    val variantId: String? = null,
    val productName: String,
    val variantName: String? = null,
    val sku: String,
    val barcode: String,
    val price: Double,
    val quantity: Int = 1,
    val discountPercent: Double = 0.0,
    val taxRate: Double = 0.08, // 8% default
    val imageUrl: String = ""
) {
    val subtotal: Double get() = price * quantity
    val discountAmount: Double get() = subtotal * (discountPercent / 100.0)
    val taxableAmount: Double get() = subtotal - discountAmount
    val taxAmount: Double get() = taxableAmount * taxRate
    val total: Double get() = taxableAmount + taxAmount
}

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val orgId: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val loyaltyPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderItem(
    val productId: String,
    val variantId: String? = null,
    val productName: String,
    val sku: String,
    val unitPrice: Double,
    val quantity: Int,
    val discountPercent: Double,
    val taxAmount: Double,
    val totalPrice: Double
)

data class SalesOrder(
    val id: String = UUID.randomUUID().toString(),
    val orderNumber: String,
    val orgId: String,
    val storeId: String,
    val warehouseId: String,
    val customerId: String? = null,
    val customerName: String = "Walk-in Customer",
    val items: List<OrderItem>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val fulfillmentStatus: FulfillmentStatus = FulfillmentStatus.COMPLETED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class LedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val orgId: String,
    val storeId: String,
    val entryNumber: String,
    val type: LedgerType,
    val category: LedgerCategory,
    val amount: Double,
    val description: String,
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class MetricsSummary(
    val totalRevenue: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val totalSalesCount: Int = 0,
    val todaySalesCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalInventoryItems: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val lowStockCount: Int = 0
)
