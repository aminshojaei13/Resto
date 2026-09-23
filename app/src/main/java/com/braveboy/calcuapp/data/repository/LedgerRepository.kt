package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.LedgerDao
import com.braveboy.calcuapp.data.local.db.dao.ProductDao
import com.braveboy.calcuapp.data.local.db.dao.SalesOrderDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.MetricsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

interface LedgerRepository {
    fun getLedgerEntriesByStore(orgId: String, storeId: String): Flow<List<LedgerEntry>>
    fun getLedgerEntriesByOrg(orgId: String): Flow<List<LedgerEntry>>
    suspend fun recordEntry(entry: LedgerEntry)
    fun getMetricsSummary(orgId: String, storeId: String?): Flow<MetricsSummary>
}

class LedgerRepositoryImpl(
    private val ledgerDao: LedgerDao,
    private val salesOrderDao: SalesOrderDao,
    private val productDao: ProductDao
) : LedgerRepository {

    override fun getLedgerEntriesByStore(orgId: String, storeId: String): Flow<List<LedgerEntry>> {
        return ledgerDao.getLedgerEntriesByStore(orgId, storeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLedgerEntriesByOrg(orgId: String): Flow<List<LedgerEntry>> {
        return ledgerDao.getLedgerEntriesByOrg(orgId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordEntry(entry: LedgerEntry) {
        ledgerDao.insertLedgerEntry(entry.toEntity())
    }

    override fun getMetricsSummary(orgId: String, storeId: String?): Flow<MetricsSummary> {
        val ordersFlow = if (storeId != null) {
            salesOrderDao.getOrdersByStore(orgId, storeId)
        } else {
            salesOrderDao.getAllOrdersByOrg(orgId)
        }

        val ledgerFlow = if (storeId != null) {
            ledgerDao.getLedgerEntriesByStore(orgId, storeId)
        } else {
            ledgerDao.getLedgerEntriesByOrg(orgId)
        }

        val productsFlow = productDao.getProductsByOrg(orgId)

        return combine(ordersFlow, ledgerFlow, productsFlow) { orders, ledger, products ->
            val now = Calendar.getInstance()
            now.set(Calendar.HOUR_OF_DAY, 0)
            now.set(Calendar.MINUTE, 0)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            val startOfToday = now.timeInMillis

            val totalRevenue = orders.sumOf { it.totalAmount }
            val todayOrders = orders.filter { it.createdAt >= startOfToday }
            val todayRevenue = todayOrders.sumOf { it.totalAmount }

            val totalExpenses = ledger.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }
            val netProfit = totalRevenue - totalExpenses

            val totalInventoryItems = products.sumOf { it.toDomain().getTotalStock() }
            val totalInventoryValue = products.sumOf { it.toDomain().getTotalStock() * it.costPrice }
            val lowStockCount = products.count { it.toDomain().getTotalStock() < 10 }

            MetricsSummary(
                totalRevenue = totalRevenue,
                todayRevenue = todayRevenue,
                totalSalesCount = orders.size,
                todaySalesCount = todayOrders.size,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                totalInventoryItems = totalInventoryItems,
                totalInventoryValue = totalInventoryValue,
                lowStockCount = lowStockCount
            )
        }
    }
}
