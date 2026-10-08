package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.CartDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.ProductVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface CartRepository {
    fun getCart(orgId: String, storeId: String): Flow<List<CartItem>>
    suspend fun addToCart(
        orgId: String,
        storeId: String,
        product: Product,
        variant: ProductVariant? = null,
        quantity: Int = 1,
        taxRate: Double = 0.0
    )
    suspend fun updateQuantity(cartItemId: String, quantity: Int)
    suspend fun applyItemDiscount(cartItemId: String, discountPercent: Double)
    suspend fun removeFromCart(cartItemId: String)
    suspend fun clearCart(orgId: String, storeId: String)
}

class CartRepositoryImpl(
    private val cartDao: CartDao
) : CartRepository {

    override fun getCart(orgId: String, storeId: String): Flow<List<CartItem>> {
        return cartDao.getCartItems(orgId, storeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addToCart(
        orgId: String,
        storeId: String,
        product: Product,
        variant: ProductVariant?,
        quantity: Int,
        taxRate: Double
    ) {
        val cartItemId = "${product.id}_${variant?.id ?: "base"}"
        val existingItemEntity = cartDao.getCartItemById(cartItemId)

        if (existingItemEntity != null) {
            val updated = existingItemEntity.toDomain().copy(
                quantity = existingItemEntity.quantity + quantity,
                taxRate = taxRate
            )
            cartDao.insertOrUpdateCartItem(updated.toEntity())
        } else {
            val price = variant?.price ?: product.price
            val sku = variant?.sku ?: product.sku
            val barcode = variant?.barcode ?: product.barcode

            val newItem = CartItem(
                id = cartItemId,
                orgId = orgId,
                storeId = storeId,
                productId = product.id,
                variantId = variant?.id,
                productName = product.name,
                variantName = variant?.name,
                sku = sku,
                barcode = barcode,
                price = price,
                quantity = quantity,
                discountPercent = 0.0,
                taxRate = taxRate,
                imageUrl = product.imageUrl
            )
            cartDao.insertOrUpdateCartItem(newItem.toEntity())
        }
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteCartItem(cartItemId)
        } else {
            val existing = cartDao.getCartItemById(cartItemId) ?: return
            val updated = existing.toDomain().copy(quantity = quantity)
            cartDao.insertOrUpdateCartItem(updated.toEntity())
        }
    }

    override suspend fun applyItemDiscount(cartItemId: String, discountPercent: Double) {
        val existing = cartDao.getCartItemById(cartItemId) ?: return
        val updated = existing.toDomain().copy(discountPercent = discountPercent.coerceIn(0.0, 100.0))
        cartDao.insertOrUpdateCartItem(updated.toEntity())
    }

    override suspend fun removeFromCart(cartItemId: String) {
        cartDao.deleteCartItem(cartItemId)
    }

    override suspend fun clearCart(orgId: String, storeId: String) {
        cartDao.clearCart(orgId, storeId)
    }
}
