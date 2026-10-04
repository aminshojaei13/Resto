package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.SupplierDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.Supplier
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule
import com.braveboy.calcuapp.data.remote.dto.SupplierDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SupplierRepository {
    fun getSuppliers(orgId: String): Flow<List<Supplier>>
    fun searchSuppliers(orgId: String, query: String): Flow<List<Supplier>>
    fun getSupplierById(id: String): Flow<Supplier?>
    suspend fun addSupplier(supplier: Supplier)
    suspend fun updateSupplier(supplier: Supplier)
    suspend fun deleteSupplier(id: String)
}

class SupplierRepositoryImpl(
    private val supplierDao: SupplierDao,
    private val apiService: CalcuappApiService = NetworkModule.apiService
) : SupplierRepository {

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
        try {
            apiService.addSupplier(
                SupplierDto(
                    id = supplier.id,
                    organizationId = supplier.orgId,
                    name = supplier.name,
                    email = supplier.email,
                    phone = supplier.phone,
                    address = supplier.address
                )
            )
        } catch (_: Exception) {
            // Offline fallback
        }
        supplierDao.insertSupplier(supplier.toEntity())
    }

    override suspend fun updateSupplier(supplier: Supplier) {
        try {
            apiService.updateSupplier(
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
        } catch (_: Exception) {
            // Offline fallback
        }
        supplierDao.insertSupplier(supplier.toEntity())
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
