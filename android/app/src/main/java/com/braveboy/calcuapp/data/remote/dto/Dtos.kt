package com.braveboy.calcuapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password,
    @Json(name = "locale") val locale: String,
    @Json(name = "device_name") val deviceName: String = "android"
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "user") val user: UserDto
)

/**
 * The authenticated person, exactly as the server describes them.
 *
 * `name` is the server-composed full name; the app never rebuilds it from the
 * email or from any other field.
 */
@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "is_platform_admin") val isPlatformAdmin: Boolean = false,
    @Json(name = "permissions") val permissions: List<String>? = null,
    @Json(name = "memberships") val memberships: List<MembershipDto>? = null
)

@JsonClass(generateAdapter = true)
data class MembershipDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String? = null,
    @Json(name = "stores") val stores: List<MembershipStoreDto>? = null
)

@JsonClass(generateAdapter = true)
data class MembershipStoreDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "warehouses") val warehouses: List<MembershipWarehouseDto>? = null
)

@JsonClass(generateAdapter = true)
data class MembershipWarehouseDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "locale") val locale: String
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "errors") val errors: Map<String, List<String>>? = null
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequestDto(
    @Json(name = "current_password") val currentPassword: String,
    @Json(name = "new_password") val newPassword: String,
    @Json(name = "new_password_confirmation") val newPasswordConfirmation: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequestDto(
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "phone") val phone: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProfileResponseDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class OrganizationDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "logo_url") val logoUrl: String?,
    @Json(name = "currency_symbol") val currencySymbol: String,
    @Json(name = "currency_code") val currencyCode: String,
    @Json(name = "subscription_tier") val subscriptionTier: String,
    @Json(name = "stores") val stores: List<StoreDto>?
)

@JsonClass(generateAdapter = true)
data class StoreDto(
    @Json(name = "id") val id: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "address") val address: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "warehouses") val warehouses: List<WarehouseDto>?
)

@JsonClass(generateAdapter = true)
data class WarehouseDto(
    @Json(name = "id") val id: String,
    @Json(name = "store_id") val storeId: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "address") val address: String?
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "sku") val sku: String,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?,
    @Json(name = "price") val price: Double,
    @Json(name = "cost_price") val costPrice: Double,
    @Json(name = "category") val category: String,
    @Json(name = "unit") val unit: String,
    @Json(name = "image_url") val imageUrl: String?
)

@JsonClass(generateAdapter = true)
data class SupplierDto(
    @Json(name = "id") val id: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "address") val address: String?
)

@JsonClass(generateAdapter = true)
data class PurchaseDto(
    @Json(name = "id") val id: String,
    @Json(name = "purchase_number") val purchaseNumber: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "store_id") val storeId: String,
    @Json(name = "warehouse_id") val warehouseId: String,
    @Json(name = "supplier_id") val supplierId: String,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "status") val status: String,
    @Json(name = "payment_status") val paymentStatus: String
)

@JsonClass(generateAdapter = true)
data class ExpenseDto(
    @Json(name = "id") val id: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "store_id") val storeId: String,
    @Json(name = "category") val category: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "date") val date: String,
    @Json(name = "notes") val notes: String?
)

@JsonClass(generateAdapter = true)
data class StockAdjustRequestDto(
    @Json(name = "org_id") val orgId: String,
    @Json(name = "warehouse_id") val warehouseId: String,
    @Json(name = "product_id") val productId: String,
    @Json(name = "product_variant_id") val variantId: String?,
    @Json(name = "delta") val delta: Int,
    @Json(name = "reason") val reason: String,
    @Json(name = "reference_id") val referenceId: String?
)

@JsonClass(generateAdapter = true)
data class CheckoutRequestDto(
    @Json(name = "org_id") val orgId: String,
    @Json(name = "store_id") val storeId: String,
    @Json(name = "warehouse_id") val warehouseId: String,
    @Json(name = "customer_id") val customerId: String?,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "notes") val notes: String?,
    @Json(name = "items") val items: List<CheckoutItemDto>
)

@JsonClass(generateAdapter = true)
data class CheckoutItemDto(
    @Json(name = "product_id") val productId: String,
    @Json(name = "variant_id") val variantId: String?,
    @Json(name = "product_name") val productName: String,
    @Json(name = "sku") val sku: String,
    @Json(name = "price") val price: Double,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "discount_percent") val discountPercent: Double,
    @Json(name = "tax_rate") val taxRate: Double
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    @Json(name = "id") val id: String,
    @Json(name = "order_number") val orderNumber: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "store_id") val storeId: String,
    @Json(name = "warehouse_id") val warehouseId: String,
    @Json(name = "customer_id") val customerId: String?,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "subtotal") val subtotal: Double,
    @Json(name = "discount_amount") val discountAmount: Double,
    @Json(name = "tax_amount") val taxAmount: Double,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "payment_status") val paymentStatus: String,
    @Json(name = "fulfillment_status") val fulfillmentStatus: String,
    @Json(name = "notes") val notes: String?,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class CustomerDto(
    @Json(name = "id") val id: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "total_purchases") val totalPurchases: Double,
    @Json(name = "loyalty_points") val loyaltyPoints: Int
)

@JsonClass(generateAdapter = true)
data class JournalEntryDto(
    @Json(name = "id") val id: String,
    @Json(name = "organization_id") val organizationId: String,
    @Json(name = "store_id") val storeId: String,
    @Json(name = "entry_number") val entryNumber: String,
    @Json(name = "description") val description: String,
    @Json(name = "total_debit") val totalDebit: Double,
    @Json(name = "total_credit") val totalCredit: Double,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class AccountingSummaryDto(
    @Json(name = "total_revenue") val totalRevenue: Double,
    @Json(name = "today_revenue") val todayRevenue: Double,
    @Json(name = "total_sales_count") val totalSalesCount: Int,
    @Json(name = "today_sales_count") val todaySalesCount: Int
)
