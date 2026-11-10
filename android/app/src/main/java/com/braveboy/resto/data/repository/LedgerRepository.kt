package com.braveboy.resto.data.repository

import com.braveboy.resto.data.local.db.dao.LedgerDao
import com.braveboy.resto.data.local.db.dao.ProductDao
import com.braveboy.resto.data.local.db.dao.SalesOrderDao
import com.braveboy.resto.data.local.db.entity.toEntity
import com.braveboy.resto.data.model.LedgerCategory
import com.braveboy.resto.data.model.LedgerEntry
import com.braveboy.resto.data.model.LedgerType
import com.braveboy.resto.data.model.MetricsSummary
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
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfThisMonth = cal.timeInMillis

            cal.add(Calendar.MONTH, -1)
            val startOfLastMonth = cal.timeInMillis

            val totalRevenue = orders.sumOf { it.totalAmount }
            val todayOrders = orders.filter { it.createdAt >= startOfToday }
            val todayRevenue = todayOrders.sumOf { it.totalAmount }

            val thisMonthOrders = orders.filter { it.createdAt >= startOfThisMonth }
            val lastMonthOrders = orders.filter { it.createdAt >= startOfLastMonth && it.createdAt < startOfThisMonth }

            val thisMonthOrdersCount = thisMonthOrders.size
            val lastMonthOrdersCount = lastMonthOrders.size

            val ordersTrendPercent = if (lastMonthOrdersCount > 0) {
                ((thisMonthOrdersCount - lastMonthOrdersCount).toDouble() / lastMonthOrdersCount) * 100
            } else if (thisMonthOrdersCount > 0 || orders.isNotEmpty()) {
                100.0
            } else {
                0.0
            }

            val thisMonthRevenue = thisMonthOrders.sumOf { it.totalAmount }
            val lastMonthRevenue = lastMonthOrders.sumOf { it.totalAmount }

            val revenueTrendPercent = if (lastMonthRevenue > 0) {
                ((thisMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100
            } else if (thisMonthRevenue > 0 || totalRevenue > 0) {
                100.0
            } else {
                0.0
            }

            val totalExpenses = ledger.filter { it.type == LedgerType.DEBIT }.sumOf { it.amount }
            val netProfit = totalRevenue - totalExpenses

            val thisMonthExpenses = ledger.filter { it.createdAt >= startOfThisMonth && it.type == LedgerType.DEBIT }.sumOf { it.amount }
            val lastMonthExpenses = ledger.filter { it.createdAt >= startOfLastMonth && it.createdAt < startOfThisMonth && it.type == LedgerType.DEBIT }.sumOf { it.amount }

            val thisMonthNetProfit = thisMonthRevenue - thisMonthExpenses
            val lastMonthNetProfit = lastMonthRevenue - lastMonthExpenses

            val netProfitTrendPercent = if (lastMonthNetProfit > 0) {
                ((thisMonthNetProfit - lastMonthNetProfit) / lastMonthNetProfit) * 100
            } else if (thisMonthNetProfit > 0 || netProfit > 0) {
                100.0
            } else if (netProfit < 0) {
                -100.0
            } else {
                0.0
            }

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
                lowStockCount = lowStockCount,
                ordersTrendPercent = ordersTrendPercent,
                netProfitTrendPercent = netProfitTrendPercent,
                revenueTrendPercent = revenueTrendPercent
            )
        }
    }
}
