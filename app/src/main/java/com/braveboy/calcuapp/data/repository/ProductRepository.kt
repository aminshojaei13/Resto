package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.LedgerDao
import com.braveboy.calcuapp.data.local.db.dao.ProductDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.Product
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
}

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val ledgerDao: LedgerDao
) : ProductRepository {

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

        // Record inventory ledger entry
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
}
