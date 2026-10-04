package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.PurchaseDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.Purchase
import com.braveboy.calcuapp.data.model.PurchaseItem
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface PurchaseRepository {
    fun getPurchases(orgId: String): Flow<List<Purchase>>
    fun getPurchaseById(id: String): Flow<Purchase?>
    suspend fun createPurchase(
        orgId: String,
        storeId: String,
        warehouseId: String,
        supplierId: String,
        supplierName: String,
        items: List<PurchaseItem>
    )
    suspend fun receivePurchase(purchase: Purchase)
    suspend fun payPurchase(purchaseId: String, amount: Double, method: String)
}

class PurchaseRepositoryImpl(
    private val purchaseDao: PurchaseDao,
    private val apiService: CalcuappApiService = NetworkModule.apiService
) : PurchaseRepository {

    override fun getPurchases(orgId: String): Flow<List<Purchase>> {
        return purchaseDao.getPurchasesByOrg(orgId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPurchaseById(id: String): Flow<Purchase?> {
        return purchaseDao.getPurchaseById(id).map { it?.toDomain() }
    }

    override suspend fun createPurchase(
        orgId: String,
        storeId: String,
        warehouseId: String,
        supplierId: String,
        supplierName: String,
        items: List<PurchaseItem>
    ) {
        val totalAmount = items.sumOf { it.totalCost }
        val purchaseNumber = "PO-${System.currentTimeMillis().toString().takeLast(6)}"
        val purchase = Purchase(
            id = UUID.randomUUID().toString(),
            purchaseNumber = purchaseNumber,
            orgId = orgId,
            storeId = storeId,
            warehouseId = warehouseId,
            supplierId = supplierId,
            supplierName = supplierName,
            items = items,
            totalAmount = totalAmount,
            status = "ORDERED",
            paymentStatus = "UNPAID"
        )

        try {
            apiService.createPurchase(
                payload = mapOf(
                    "store_id" to storeId,
                    "warehouse_id" to warehouseId,
                    "supplier_id" to supplierId,
                    "items" to items.map {
                        mapOf(
                            "product_id" to it.productId,
                            "quantity" to it.quantity,
                            "unit_cost" to it.unitCost
                        )
                    }
                )
            )
        } catch (_: Exception) {
            // Offline fallback
        }

        purchaseDao.insertPurchase(purchase.toEntity())
    }

    override suspend fun receivePurchase(purchase: Purchase) {
        val updated = purchase.copy(status = "RECEIVED")
        try {
            apiService.receivePurchase(purchase.id)
        } catch (_: Exception) {
            // Offline fallback
        }
        purchaseDao.insertPurchase(updated.toEntity())
    }

    override suspend fun payPurchase(purchaseId: String, amount: Double, method: String) {
        try {
            apiService.payPurchase(purchaseId, amount, method)
        } catch (_: Exception) {
            // Offline fallback
        }

        val existing = purchaseDao.getPurchaseByIdDirect(purchaseId)
        if (existing != null) {
            val domain = existing.toDomain()
            val updated = domain.copy(paymentStatus = "PAID")
            purchaseDao.insertPurchase(updated.toEntity())
        }
    }
}
