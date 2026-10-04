package com.braveboy.calcuapp.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.Expense
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.ExpenseRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val tenantRepository: TenantRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<Expense>> = combine(
        tenantState,
        _searchQuery,
        _selectedCategory
    ) { state, query, category ->
        Triple(state.activeOrgId, query, category)
    }.flatMapLatest { (orgId, query, category) ->
        expenseRepository.getExpenses(orgId).map { list ->
            list.filter { exp ->
                val matchesCat = category == null || exp.category.equals(category, ignoreCase = true)
                val matchesQuery = query.isBlank() || exp.category.contains(query, ignoreCase = true) || exp.notes.contains(query, ignoreCase = true)
                matchesCat && matchesQuery
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isCreateExpenseDialogOpen = MutableStateFlow(false)
    val isCreateExpenseDialogOpen: StateFlow<Boolean> = _isCreateExpenseDialogOpen.asStateFlow()

    private val _selectedExpenseDetail = MutableStateFlow<Expense?>(null)
    val selectedExpenseDetail: StateFlow<Expense?> = _selectedExpenseDetail.asStateFlow()

    private val _selectedExpenseForEdit = MutableStateFlow<Expense?>(null)
    val selectedExpenseForEdit: StateFlow<Expense?> = _selectedExpenseForEdit.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun openCreateExpenseDialog() {
        _isCreateExpenseDialogOpen.value = true
    }

    fun closeCreateExpenseDialog() {
        _isCreateExpenseDialogOpen.value = false
    }

    fun openEditExpense(expense: Expense) {
        _selectedExpenseForEdit.value = expense
    }

    fun closeEditExpense() {
        _selectedExpenseForEdit.value = null
    }

    fun selectExpenseDetail(expense: Expense?) {
        _selectedExpenseDetail.value = expense
    }

    fun addExpense(
        category: String,
        amount: Double,
        date: String,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                val expense = Expense(
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId,
                    category = category,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    date = date,
                    notes = notes,
                    userId = state.userId
                )
                expenseRepository.addExpense(expense)
                _snackbarMessage.value = "Recorded expense '$category'"
                _isCreateExpenseDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to record expense: ${e.localizedMessage}"
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                expenseRepository.updateExpense(expense)
                _snackbarMessage.value = "Updated expense '${expense.category}'"
                _selectedExpenseForEdit.value = null
                _selectedExpenseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to update expense: ${e.localizedMessage}"
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpense(expense.id)
                _snackbarMessage.value = "Deleted expense '${expense.category}'"
                _selectedExpenseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to delete expense: ${e.localizedMessage}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val expenseRepository: ExpenseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExpensesViewModel(tenantRepository, expenseRepository) as T
        }
    }
}
