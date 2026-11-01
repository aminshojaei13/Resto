package com.braveboy.resto.data.repository

import com.braveboy.resto.data.local.db.dao.LedgerDao
import com.braveboy.resto.data.local.db.dao.ProductDao
import com.braveboy.resto.data.local.db.entity.toEntity
import com.braveboy.resto.data.model.LedgerCategory
import com.braveboy.resto.data.model.LedgerEntry
import com.braveboy.resto.data.model.LedgerType
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.remote.RestoApiService
import com.braveboy.resto.data.remote.NetworkModule
import com.braveboy.resto.data.remote.dto.ProductDto
import com.braveboy.resto.data.remote.dto.StockAdjustRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

interface ProductRepository {
    fun getProducts(orgId: String): Flow<List<Product>>
    fun searchProducts(orgId: String, query: String): Flow<List<Product>>
    fun getProductById(id: String): Flow<Product?>
    fun getProductByBarcode(orgId: String, barcode: String): Flow<Product?>
    suspend fun adjustStock(
        productId: String,
        warehouseId: String,
        delta: Int,
        reason: String,
        orgId: String,
        storeId: String
    )
    suspend fun upsertProduct(product: Product)
    suspend fun addProduct(
        product: Product,
        initialStock: Int = 0,
        warehouseId: String = "",
        storeId: String = ""
    )
    suspend fun updateProduct(product: Product)
    suspend fun deleteProduct(productId: String)
    suspend fun refreshProducts(orgId: String)
}

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val ledgerDao: LedgerDao,
    private val apiService: RestoApiService = NetworkModule.apiService
) : ProductRepository {

    override suspend fun refreshProducts(orgId: String) {
        if (orgId.isBlank()) return
        try {
            val response = withTimeoutOrNull(2500L) { apiService.getProducts(orgId = orgId) }
            if (response != null && response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val products = dtos.map { dto ->
                    Product(
                        id = dto.id,
                        orgId = dto.organizationId,
                        sku = dto.sku,
                        barcode = dto.barcode,
                        name = dto.name,
                        description = dto.description ?: "",
                        price = dto.price,
                        costPrice = dto.costPrice,
                        category = dto.category,
                        unit = dto.unit,
                        imageUrl = dto.imageUrl ?: ""
                    )
                }
                productDao.insertProducts(products.map { it.toEntity() })
            }
        } catch (_: Exception) {
            // Keep local cache if offline
        }
    }

    override fun getProducts(orgId: String): Flow<List<Product>> {
        return productDao.getProductsByOrg(orgId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchProducts(orgId: String, query: String): Flow<List<Product>> {
        if (query.isBlank()) return getProducts(orgId)
        return productDao.searchProducts(orgId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProductById(id: String): Flow<Product?> {
        return productDao.getProductById(id).map { it?.toDomain() }
    }

    override fun getProductByBarcode(orgId: String, barcode: String): Flow<Product?> {
        return productDao.getProductByBarcode(orgId, barcode).map { it?.toDomain() }
    }

    override suspend fun adjustStock(
        productId: String,
        warehouseId: String,
        delta: Int,
        reason: String,
        orgId: String,
        storeId: String
    ) {
        val existingEntity = productDao.getProductByIdDirect(productId) ?: return
        val existingProduct = existingEntity.toDomain()

        val updatedStock = existingProduct.stockQuantityByWarehouse.toMutableMap()
        val currentQty = updatedStock[warehouseId] ?: 0
        val newQty = (currentQty + delta).coerceAtLeast(0)
        updatedStock[warehouseId] = newQty

        val updatedProduct = existingProduct.copy(stockQuantityByWarehouse = updatedStock)
        productDao.insertProduct(updatedProduct.toEntity())

        // Record inventory ledger entry locally
        val valueChanged = delta * existingProduct.costPrice
        val ledgerType = if (delta >= 0) LedgerType.CREDIT else LedgerType.DEBIT
        val ledgerEntry = LedgerEntry(
            orgId = orgId,
            storeId = storeId,
            entryNumber = "ADJ-${System.currentTimeMillis().toString().takeLast(6)}",
            type = ledgerType,
            category = LedgerCategory.INVENTORY_ADJUSTMENT,
            amount = kotlin.math.abs(valueChanged),
            description = "Stock adjustment ($delta pcs) for ${existingProduct.name}: $reason",
            referenceId = productId
        )
        ledgerDao.insertLedgerEntry(ledgerEntry.toEntity())

        // Try calling remote API asynchronously with timeout
        try {
            withTimeoutOrNull(2000L) {
                apiService.adjustStock(
                    StockAdjustRequestDto(
                        orgId = orgId,
                        warehouseId = warehouseId,
                        productId = productId,
                        variantId = null,
                        delta = delta,
                        reason = reason,
                        referenceId = null
                    )
                )
            }
        } catch (_: Exception) {
            // Offline fallback
        }
    }

    override suspend fun upsertProduct(product: Product) {
        productDao.insertProduct(product.toEntity())
    }

    override suspend fun addProduct(
        product: Product,
        initialStock: Int,
        warehouseId: String,
        storeId: String
    ) {
        val stockMap = if (initialStock > 0 && warehouseId.isNotBlank()) {
            mapOf(warehouseId to initialStock)
        } else {
            emptyMap()
        }
        val productWithStock = product.copy(stockQuantityByWarehouse = stockMap)

        // Save locally first so UI updates instantly
        productDao.insertProduct(productWithStock.toEntity(isSynced = false))

        if (initialStock > 0 && warehouseId.isNotBlank()) {
            val value = initialStock * productWithStock.costPrice
            val ledgerEntry = LedgerEntry(
                orgId = productWithStock.orgId,
                storeId = storeId,
                entryNumber = "INIT-${System.currentTimeMillis().toString().takeLast(6)}",
                type = LedgerType.CREDIT,
                category = LedgerCategory.INVENTORY_ADJUSTMENT,
                amount = value,
                description = "موجودی اولیه برای ${productWithStock.name} ($initialStock ${productWithStock.unit})",
                referenceId = productWithStock.id
            )
            ledgerDao.insertLedgerEntry(ledgerEntry.toEntity())
        }

        // Attempt network sync with timeout
        try {
            val response = withTimeoutOrNull(2000L) {
                apiService.addProduct(
                    ProductDto(
                        id = productWithStock.id,
                        organizationId = productWithStock.orgId,
                        sku = productWithStock.sku,
                        barcode = productWithStock.barcode,
                        name = productWithStock.name,
                        description = productWithStock.description,
                        price = productWithStock.price,
                        costPrice = productWithStock.costPrice,
                        category = productWithStock.category,
                        unit = productWithStock.unit,
                        imageUrl = productWithStock.imageUrl
                    )
                )
            }
            if (response != null && response.isSuccessful) {
                productDao.markProductSynced(productWithStock.id)
            }
        } catch (_: Exception) {
            // Offline fallback - sync manager will handle
        }
    }

    override suspend fun updateProduct(product: Product) {
        // Save locally first
        productDao.insertProduct(product.toEntity(isSynced = false))

        try {
            val response = withTimeoutOrNull(2000L) {
                apiService.updateProduct(
                    id = product.id,
                    product = ProductDto(
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
                )
            }
            if (response != null && response.isSuccessful) {
                productDao.markProductSynced(product.id)
            }
        } catch (_: Exception) {
            // Offline fallback
        }
    }

    override suspend fun deleteProduct(productId: String) {
        // Delete locally first so UI updates instantly
        productDao.deleteProductById(productId)

        try {
            withTimeoutOrNull(2000L) {
                apiService.deleteProduct(productId)
            }
        } catch (_: Exception) {
            // Offline fallback
        }
    }
}

