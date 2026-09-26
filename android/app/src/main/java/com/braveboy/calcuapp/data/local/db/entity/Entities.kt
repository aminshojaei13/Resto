package com.braveboy.calcuapp.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.OrderItem
import com.braveboy.calcuapp.data.model.Organization
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.ProductVariant
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.data.model.Store
import com.braveboy.calcuapp.data.model.Supplier
import com.braveboy.calcuapp.data.model.Warehouse

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val logoUrl: String,
    val currencySymbol: String,
    val currencyCode: String,
    val subscriptionTier: String
) {
    fun toDomain(stores: List<Store> = emptyList()) = Organization(
        id = id,
        name = name,
        code = code,
        logoUrl = logoUrl,
        currencySymbol = currencySymbol,
        currencyCode = currencyCode,
        subscriptionTier = subscriptionTier,
        stores = stores
    )
}

fun Organization.toEntity() = OrganizationEntity(
    id = id,
    name = name,
    code = code,
    logoUrl = logoUrl,
    currencySymbol = currencySymbol,
    currencyCode = currencyCode,
    subscriptionTier = subscriptionTier
)

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val orgId: String,
    val name: String,
    val code: String,
    val address: String,
    val phone: String
) {
    fun toDomain(warehouses: List<Warehouse> = emptyList()) = Store(
        id = id,
        orgId = orgId,
        name = name,
        code = code,
        address = address,
        phone = phone,
        warehouses = warehouses
    )
}

fun Store.toEntity() = StoreEntity(
    id = id,
    orgId = orgId,
    name = name,
    code = code,
    address = address,
    phone = phone
)

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val orgId: String,
    val name: String,
    val code: String,
    val address: String
) {
    fun toDomain() = Warehouse(
        id = id,
        storeId = storeId,
        orgId = orgId,
        name = name,
        code = code,
        address = address
    )
}

fun Warehouse.toEntity() = WarehouseEntity(
    id = id,
    storeId = storeId,
    orgId = orgId,
    name = name,
    code = code,
    address = address
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val orgId: String,
    val sku: String,
    val barcode: String,
    val name: String,
    val description: String,
    val price: Double,
    val costPrice: Double,
    val category: String,
    val imageUrl: String,
    val unit: String,
    val variants: List<ProductVariant>,
    val stockQuantityByWarehouse: Map<String, Int>
) {
    fun toDomain() = Product(
        id = id,
        orgId = orgId,
        sku = sku,
        barcode = barcode,
        name = name,
        description = description,
        price = price,
        costPrice = costPrice,
        category = category,
        imageUrl = imageUrl,
        unit = unit,
        variants = variants,
        stockQuantityByWarehouse = stockQuantityByWarehouse
    )
}

fun Product.toEntity() = ProductEntity(
    id = id,
    orgId = orgId,
    sku = sku,
    barcode = barcode,
    name = name,
    description = description,
    price = price,
    costPrice = costPrice,
    category = category,
    imageUrl = imageUrl,
    unit = unit,
    variants = variants,
    stockQuantityByWarehouse = stockQuantityByWarehouse
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val orgId: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val totalPurchases: Double,
    val loyaltyPoints: Int,
    val createdAt: Long
) {
    fun toDomain() = Customer(
        id = id,
        orgId = orgId,
        name = name,
        email = email,
        phone = phone,
        address = address,
        totalPurchases = totalPurchases,
        loyaltyPoints = loyaltyPoints,
        createdAt = createdAt
    )
}

fun Customer.toEntity() = CustomerEntity(
    id = id,
    orgId = orgId,
    name = name,
    email = email,
    phone = phone,
    address = address,
    totalPurchases = totalPurchases,
    loyaltyPoints = loyaltyPoints,
    createdAt = createdAt
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val orgId: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val createdAt: Long
) {
    fun toDomain() = Supplier(
        id = id,
        orgId = orgId,
        name = name,
        email = email,
        phone = phone,
        address = address,
        createdAt = createdAt
    )
}

fun Supplier.toEntity() = SupplierEntity(
    id = id,
    orgId = orgId,
    name = name,
    email = email,
    phone = phone,
    address = address,
    createdAt = createdAt
)

@Entity(tableName = "sales_orders")
data class SalesOrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val orgId: String,
    val storeId: String,
    val warehouseId: String,
    val customerId: String?,
    val customerName: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val fulfillmentStatus: FulfillmentStatus,
    val notes: String,
    val createdAt: Long
) {
    fun toDomain() = SalesOrder(
        id = id,
        orderNumber = orderNumber,
        orgId = orgId,
        storeId = storeId,
        warehouseId = warehouseId,
        customerId = customerId,
        customerName = customerName,
        items = items,
        subtotal = subtotal,
        discountAmount = discountAmount,
        taxAmount = taxAmount,
        totalAmount = totalAmount,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        fulfillmentStatus = fulfillmentStatus,
        notes = notes,
        createdAt = createdAt
    )
}

fun SalesOrder.toEntity() = SalesOrderEntity(
    id = id,
    orderNumber = orderNumber,
    orgId = orgId,
    storeId = storeId,
    warehouseId = warehouseId,
    customerId = customerId,
    customerName = customerName,
    items = items,
    subtotal = subtotal,
    discountAmount = discountAmount,
    taxAmount = taxAmount,
    totalAmount = totalAmount,
    paymentMethod = paymentMethod,
    paymentStatus = paymentStatus,
    fulfillmentStatus = fulfillmentStatus,
    notes = notes,
    createdAt = createdAt
)

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val orgId: String,
    val storeId: String,
    val entryNumber: String,
    val type: LedgerType,
    val category: LedgerCategory,
    val amount: Double,
    val description: String,
    val referenceId: String?,
    val createdAt: Long
) {
    fun toDomain() = LedgerEntry(
        id = id,
        orgId = orgId,
        storeId = storeId,
        entryNumber = entryNumber,
        type = type,
        category = category,
        amount = amount,
        description = description,
        referenceId = referenceId,
        createdAt = createdAt
    )
}

fun LedgerEntry.toEntity() = LedgerEntryEntity(
    id = id,
    orgId = orgId,
    storeId = storeId,
    entryNumber = entryNumber,
    type = type,
    category = category,
    amount = amount,
    description = description,
    referenceId = referenceId,
    createdAt = createdAt
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val id: String,
    val orgId: String,
    val storeId: String,
    val productId: String,
    val variantId: String?,
    val productName: String,
    val variantName: String?,
    val sku: String,
    val barcode: String,
    val price: Double,
    val quantity: Int,
    val discountPercent: Double,
    val taxRate: Double,
    val imageUrl: String
) {
    fun toDomain() = CartItem(
        id = id,
        orgId = orgId,
        storeId = storeId,
        productId = productId,
        variantId = variantId,
        productName = productName,
        variantName = variantName,
        sku = sku,
        barcode = barcode,
        price = price,
        quantity = quantity,
        discountPercent = discountPercent,
        taxRate = taxRate,
        imageUrl = imageUrl
    )
}

fun CartItem.toEntity() = CartItemEntity(
    id = id,
    orgId = orgId,
    storeId = storeId,
    productId = productId,
    variantId = variantId,
    productName = productName,
    variantName = variantName,
    sku = sku,
    barcode = barcode,
    price = price,
    quantity = quantity,
    discountPercent = discountPercent,
    taxRate = taxRate,
    imageUrl = imageUrl
)
