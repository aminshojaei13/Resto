package com.braveboy.resto.data.repository

import com.braveboy.resto.data.local.db.dao.SupplierDao
import com.braveboy.resto.data.local.db.entity.toEntity
import com.braveboy.resto.data.model.Supplier
import com.braveboy.resto.data.remote.RestoApiService
import com.braveboy.resto.data.remote.NetworkModule
import com.braveboy.resto.data.remote.dto.SupplierDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SupplierRepository {
    fun getSuppliers(orgId: String): Flow<List<Supplier>>
    fun searchSuppliers(orgId: String, query: String): Flow<List<Supplier>>
    fun getSupplierById(id: String): Flow<Supplier?>
    suspend fun addSupplier(supplier: Supplier)
    suspend fun updateSupplier(supplier: Supplier)
    suspend fun deleteSupplier(id: String)
    suspend fun refreshSuppliers(orgId: String)
}

class SupplierRepositoryImpl(
    private val supplierDao: SupplierDao,
    private val apiService: RestoApiService = NetworkModule.apiService
) : SupplierRepository {

    override suspend fun refreshSuppliers(orgId: String) {
        if (orgId.isBlank()) return
        try {
            val response = apiService.getSuppliers(orgId = orgId)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val suppliers = dtos.map { dto ->
                    Supplier(
                        id = dto.id,
                        orgId = dto.organizationId,
                        name = dto.name,
                        email = dto.email ?: "",
                        phone = dto.phone ?: "",
                        address = dto.address ?: ""
                    )
                }
                supplierDao.insertSuppliers(suppliers.map { it.toEntity() })
            }
        } catch (_: Exception) {
            // Keep local cache if offline
        }
    }

    override fun getSuppliers(orgId: String): Flow<List<Supplier>> {
        return supplierDao.getSuppliersByOrg(orgId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchSuppliers(orgId: String, query: String): Flow<List<Supplier>> {
        if (query.isBlank()) return getSuppliers(orgId)
        return supplierDao.searchSuppliers(orgId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSupplierById(id: String): Flow<Supplier?> {
        return supplierDao.getSupplierById(id).map { it?.toDomain() }
    }

    override suspend fun addSupplier(supplier: Supplier) {
        var isSynced = true
        try {
            val response = apiService.addSupplier(
                SupplierDto(
                    id = supplier.id,
                    organizationId = supplier.orgId,
                    name = supplier.name,
                    email = supplier.email,
                    phone = supplier.phone,
                    address = supplier.address
                )
            )
            if (!response.isSuccessful) {
                isSynced = false
            }
        } catch (_: Exception) {
            isSynced = false
        }
        supplierDao.insertSupplier(supplier.toEntity(isSynced = isSynced))
    }

    override suspend fun updateSupplier(supplier: Supplier) {
        var isSynced = true
        try {
            val response = apiService.updateSupplier(
                id = supplier.id,
                supplier = SupplierDto(
                    id = supplier.id,
                    organizationId = supplier.orgId,
                    name = supplier.name,
                    email = supplier.email,
                    phone = supplier.phone,
                    address = supplier.address
                )
            )
            if (!response.isSuccessful) {
                isSynced = false
            }
        } catch (_: Exception) {
            isSynced = false
        }
        supplierDao.insertSupplier(supplier.toEntity(isSynced = isSynced))
    }

    override suspend fun deleteSupplier(id: String) {
        try {
            apiService.deleteSupplier(id)
        } catch (_: Exception) {
            // Offline fallback
        }
        supplierDao.deleteSupplier(id)
    }
}
