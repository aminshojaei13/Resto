package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.datastore.TenantPreferences
import com.braveboy.calcuapp.data.local.db.dao.CustomerDao
import com.braveboy.calcuapp.data.local.db.dao.LedgerDao
import com.braveboy.calcuapp.data.local.db.dao.ProductDao
import com.braveboy.calcuapp.data.local.db.dao.SalesOrderDao
import com.braveboy.calcuapp.data.local.db.dao.TenantDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.mock.MockSaaSDataSource
import com.braveboy.calcuapp.data.model.Organization
import com.braveboy.calcuapp.data.model.Store
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.model.Warehouse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface TenantRepository {
    fun getOrganizations(): Flow<List<Organization>>
    fun getStoresForOrg(orgId: String): Flow<List<Store>>
    fun getWarehousesForStore(storeId: String): Flow<List<Warehouse>>
    fun getTenantState(): Flow<TenantState>
    suspend fun setActiveTenant(orgId: String, storeId: String, warehouseId: String)
    suspend fun setActiveStore(storeId: String, warehouseId: String)
    suspend fun setActiveWarehouse(warehouseId: String)
    suspend fun setLanguage(language: String)
    suspend fun seedInitialData()
}

class TenantRepositoryImpl(
    private val tenantDao: TenantDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val salesOrderDao: SalesOrderDao,
    private val ledgerDao: LedgerDao,
    private val tenantPreferences: TenantPreferences
) : TenantRepository {

    override fun getOrganizations(): Flow<List<Organization>> {
        return tenantDao.getAllOrganizations().map { orgEntities ->
            orgEntities.map { it.toDomain() }
        }
    }

    override fun getStoresForOrg(orgId: String): Flow<List<Store>> {
        return combine(
            tenantDao.getStoresForOrg(orgId),
            tenantDao.getAllOrganizations()
        ) { stores, _ ->
            stores.map { storeEntity ->
                storeEntity.toDomain()
            }
        }
    }

    override fun getWarehousesForStore(storeId: String): Flow<List<Warehouse>> {
        return tenantDao.getWarehousesForStore(storeId).map { warehouseEntities ->
            warehouseEntities.map { it.toDomain() }
        }
    }

    override fun getTenantState(): Flow<TenantState> = tenantPreferences.tenantState

    override suspend fun setActiveTenant(orgId: String, storeId: String, warehouseId: String) {
        tenantPreferences.setActiveTenant(orgId, storeId, warehouseId)
    }

    override suspend fun setActiveStore(storeId: String, warehouseId: String) {
        tenantPreferences.setActiveStore(storeId, warehouseId)
    }

    override suspend fun setActiveWarehouse(warehouseId: String) {
        tenantPreferences.setActiveWarehouse(warehouseId)
    }

    override suspend fun setLanguage(language: String) {
        tenantPreferences.setLanguage(language)
    }

    override suspend fun seedInitialData() {
        if (tenantDao.getOrganizationCount() == 0) {
            val orgs = MockSaaSDataSource.organizations
            val stores = orgs.flatMap { it.stores }
            val warehouses = stores.flatMap { it.warehouses }

            tenantDao.insertOrganizations(orgs.map { it.toEntity() })
            tenantDao.insertStores(stores.map { it.toEntity() })
            tenantDao.insertWarehouses(warehouses.map { it.toEntity() })

            productDao.insertProducts(MockSaaSDataSource.products.map { it.toEntity() })
            customerDao.insertCustomers(MockSaaSDataSource.customers.map { it.toEntity() })
            salesOrderDao.insertSalesOrders(MockSaaSDataSource.salesOrders.map { it.toEntity() })
            ledgerDao.insertLedgerEntries(MockSaaSDataSource.ledgerEntries.map { it.toEntity() })
        }
    }
}
