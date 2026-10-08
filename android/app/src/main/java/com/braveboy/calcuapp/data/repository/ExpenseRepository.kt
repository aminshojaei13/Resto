package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.db.dao.ExpenseDao
import com.braveboy.calcuapp.data.local.db.entity.toEntity
import com.braveboy.calcuapp.data.model.Expense
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule
import com.braveboy.calcuapp.data.remote.dto.ExpenseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ExpenseRepository {
    fun getExpenses(orgId: String): Flow<List<Expense>>
    fun getExpenseById(id: String): Flow<Expense?>
    suspend fun addExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(id: String)
    suspend fun refreshExpenses(orgId: String)
}

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val apiService: CalcuappApiService = NetworkModule.apiService
) : ExpenseRepository {

    override suspend fun refreshExpenses(orgId: String) {
        if (orgId.isBlank()) return
        try {
            val response = apiService.getExpenses(orgId = orgId)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val expenses = dtos.map { dto ->
                    Expense(
                        id = dto.id,
                        orgId = dto.organizationId,
                        storeId = dto.storeId,
                        category = dto.category,
                        amount = dto.amount,
                        paymentMethod = dto.paymentMethod,
                        date = dto.date,
                        notes = dto.notes ?: ""
                    )
                }
                expenseDao.insertExpenses(expenses.map { it.toEntity() })
            }
        } catch (_: Exception) {
            // Keep local cache if offline
        }
    }

    override fun getExpenses(orgId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByOrg(orgId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpenseById(id: String): Flow<Expense?> {
        return expenseDao.getExpenseById(id).map { it?.toDomain() }
    }

    override suspend fun addExpense(expense: Expense) {
        try {
            apiService.addExpense(
                ExpenseDto(
                    id = expense.id,
                    organizationId = expense.orgId,
                    storeId = expense.storeId,
                    category = expense.category,
                    amount = expense.amount,
                    paymentMethod = expense.paymentMethod,
                    date = expense.date,
                    notes = expense.notes
                )
            )
        } catch (_: Exception) {
            // Offline fallback
        }
        expenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(id: String) {
        expenseDao.deleteExpense(id)
    }
}
