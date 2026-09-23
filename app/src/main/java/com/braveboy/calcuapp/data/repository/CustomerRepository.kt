package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.CustomerDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CustomerRepository {
    fun getCustomers(orgId: String): Flow<List<Customer>>
    fun searchCustomers(orgId: String, query: String): Flow<List<Customer>>
    fun getCustomerById(id: String): Flow<Customer?>
    suspend fun addCustomer(customer: Customer)
}

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao
) : CustomerRepository {

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

    override suspend fun addCustomer(customer: Customer) {
        customerDao.insertCustomer(customer.toEntity())
    }
}
