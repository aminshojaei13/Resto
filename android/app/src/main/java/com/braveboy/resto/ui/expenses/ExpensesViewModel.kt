package com.braveboy.resto.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.resto.data.model.Expense
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.repository.ExpenseRepository
import com.braveboy.resto.data.repository.TenantRepository
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

import kotlinx.coroutines.flow.firstOrNull

class ExpensesViewModel(
    tenantRepository: TenantRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        viewModelScope.launch {
            tenantState.collect { state ->
                if (state.activeOrgId.isNotBlank()) {
                    val localExpenses = expenseRepository.getExpenses(state.activeOrgId).firstOrNull() ?: emptyList()
                    _isLoading.value = localExpenses.isEmpty()

                    viewModelScope.launch {
                        try {
                            expenseRepository.refreshExpenses(state.activeOrgId)
                        } finally {
                            _isLoading.value = false
                        }
                    }
                } else {
                    _isLoading.value = false
                }
            }
        }
    }

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
            _isSaving.value = true
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
                _snackbarMessage.value = "هزینه '$category' ثبت شد"
                _isCreateExpenseDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در ثبت هزینه: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                expenseRepository.updateExpense(expense)
                _snackbarMessage.value = "هزینه '${expense.category}' به روز شد"
                _selectedExpenseForEdit.value = null
                _selectedExpenseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در به روز رسانی هزینه: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                expenseRepository.deleteExpense(expense.id)
                _snackbarMessage.value = "هزینه '${expense.category}' حذف شد"
                _selectedExpenseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در حذف هزینه: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
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
