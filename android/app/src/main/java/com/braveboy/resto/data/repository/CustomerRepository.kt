package com.braveboy.resto.data.repository

import com.braveboy.resto.data.local.db.dao.CustomerDao
import com.braveboy.resto.data.local.db.entity.toEntity
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.remote.RestoApiService
import com.braveboy.resto.data.remote.NetworkModule
import com.braveboy.resto.data.remote.dto.CustomerDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

interface CustomerRepository {
    fun getCustomers(orgId: String): Flow<List<Customer>>
    fun searchCustomers(orgId: String, query: String): Flow<List<Customer>>
    fun getCustomerById(id: String): Flow<Customer?>
    suspend fun getCustomerByPhone(orgId: String, phone: String): Customer?
    suspend fun addCustomer(customer: Customer)
    suspend fun updateCustomer(customer: Customer)
    suspend fun refreshCustomers(orgId: String)
}

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao,
    private val apiService: RestoApiService = NetworkModule.apiService
) : CustomerRepository {

    override suspend fun refreshCustomers(orgId: String) {
        if (orgId.isBlank()) return
        try {
            val response = withTimeoutOrNull(2500L) { apiService.getCustomers(orgId = orgId) }
            if (response != null && response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val customers = dtos.map { dto ->
                    Customer(
                        id = dto.id,
                        orgId = dto.organizationId,
                        name = dto.name,
                        email = dto.email ?: "",
                        phone = dto.phone ?: "",
                        address = dto.address ?: "",
                        totalPurchases = dto.totalPurchases,
                        loyaltyPoints = dto.loyaltyPoints
                    )
                }
                customerDao.insertCustomers(customers.map { it.toEntity() })
            }
        } catch (_: Exception) {
            // Keep local cache if offline
        }
    }

    override fun getCustomers(orgId: String): Flow<List<Customer>> {
        return customerDao.getCustomersByOrg(orgId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchCustomers(orgId: String, query: String): Flow<List<Customer>> {
        if (query.isBlank()) return getCustomers(orgId)
        return customerDao.searchCustomers(orgId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCustomerById(id: String): Flow<Customer?> {
        return customerDao.getCustomerById(id).map { it?.toDomain() }
    }

    override suspend fun getCustomerByPhone(orgId: String, phone: String): Customer? {
        return customerDao.getCustomerByPhone(orgId, phone)?.toDomain()
    }

    override suspend fun addCustomer(customer: Customer) {
        // Save locally first for instant UI response
        customerDao.insertCustomer(customer.toEntity(isSynced = false))

        try {
            val response = withTimeoutOrNull(2000L) {
                apiService.addCustomer(
                    CustomerDto(
                        id = customer.id,
                        organizationId = customer.orgId,
                        name = customer.name,
                        email = customer.email,
                        phone = customer.phone,
                        address = customer.address,
                        totalPurchases = customer.totalPurchases,
                        loyaltyPoints = customer.loyaltyPoints
                    )
                )
            }
            if (response != null && response.isSuccessful) {
                customerDao.markCustomerSynced(customer.id)
            }
        } catch (_: Exception) {
            // SyncManager will handle offline push
        }
    }

    override suspend fun updateCustomer(customer: Customer) {
        customerDao.insertCustomer(customer.toEntity(isSynced = false))

        try {
            val response = withTimeoutOrNull(2000L) {
                apiService.updateCustomer(
                    id = customer.id,
                    customer = CustomerDto(
                        id = customer.id,
                        organizationId = customer.orgId,
                        name = customer.name,
                        email = customer.email,
                        phone = customer.phone,
                        address = customer.address,
                        totalPurchases = customer.totalPurchases,
                        loyaltyPoints = customer.loyaltyPoints
                    )
                )
            }
            if (response != null && response.isSuccessful) {
                customerDao.markCustomerSynced(customer.id)
            }
        } catch (_: Exception) {
            // Offline fallback
        }
    }
}

