package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.datastore.TenantPreferences
import com.braveboy.calcuapp.data.local.db.dao.TenantDao
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
        // Intentionally no-op: All organizations, stores, and entities are sourced from real backend
    }
}
