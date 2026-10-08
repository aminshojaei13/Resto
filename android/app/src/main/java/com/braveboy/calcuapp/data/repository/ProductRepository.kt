package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.LedgerDao
import com.braveboy.calcuapp.data.local.db.dao.ProductDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule
import com.braveboy.calcuapp.data.remote.dto.ProductDto
import com.braveboy.calcuapp.data.remote.dto.StockAdjustRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    suspend fun updateProduct(product: Product)
    suspend fun refreshProducts(orgId: String)
}

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val ledgerDao: LedgerDao,
    private val apiService: CalcuappApiService = NetworkModule.apiService
) : ProductRepository {

    override suspend fun refreshProducts(orgId: String) {
        if (orgId.isBlank()) return
        try {
            val response = apiService.getProducts(orgId = orgId)
            if (response.isSuccessful && response.body() != null) {
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
        // Try calling remote API if available
        try {
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
        } catch (e: Exception) {
            // Offline fallback - update local Room DB directly
        }

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
    }

    override suspend fun upsertProduct(product: Product) {
        productDao.insertProduct(product.toEntity())
    }

    override suspend fun updateProduct(product: Product) {
        try {
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
        } catch (e: Exception) {
            // Offline fallback - syncs to Room DB
        }
        productDao.insertProduct(product.toEntity())
    }
}
