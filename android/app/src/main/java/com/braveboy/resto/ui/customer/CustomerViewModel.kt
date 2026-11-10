package com.braveboy.resto.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.repository.CustomerRepository
import com.braveboy.resto.data.repository.TenantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.firstOrNull

class CustomerViewModel(
    tenantRepository: TenantRepository,
    private val customerRepository: CustomerRepository
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
                    val localCustomers = customerRepository.getCustomers(state.activeOrgId).firstOrNull() ?: emptyList()
                    _isLoading.value = localCustomers.isEmpty()

                    viewModelScope.launch {
                        try {
                            customerRepository.refreshCustomers(state.activeOrgId)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> = combine(
        tenantState,
        _searchQuery
    ) { state, query ->
        Pair(state.activeOrgId, query)
    }.flatMapLatest { (orgId, query) ->
        customerRepository.searchCustomers(orgId, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isAddCustomerDialogOpen = MutableStateFlow(false)
    val isAddCustomerDialogOpen: StateFlow<Boolean> = _isAddCustomerDialogOpen.asStateFlow()

    private val _selectedCustomerForDetail = MutableStateFlow<Customer?>(null)
    val selectedCustomerForDetail: StateFlow<Customer?> = _selectedCustomerForDetail.asStateFlow()

    private val _selectedCustomerForEdit = MutableStateFlow<Customer?>(null)
    val selectedCustomerForEdit: StateFlow<Customer?> = _selectedCustomerForEdit.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddCustomerDialog() {
        _isAddCustomerDialogOpen.value = true
    }

    fun closeAddCustomerDialog() {
        _isAddCustomerDialogOpen.value = false
    }

    fun openEditCustomer(customer: Customer) {
        _selectedCustomerForEdit.value = customer
    }

    fun closeEditCustomer() {
        _selectedCustomerForEdit.value = null
    }

    fun addCustomer(name: String, email: String, phone: String, address: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val state = tenantState.value
                val cleanPhone = phone.trim()
                if (cleanPhone.isBlank()) {
                    _snackbarMessage.value = "شماره موبایل مشتری الزامی است"
                    return@launch
                }
                val existing = customerRepository.getCustomerByPhone(state.activeOrgId, cleanPhone)
                if (existing != null) {
                    _snackbarMessage.value = "مشتری دیگری با شماره '$cleanPhone' قبلاً ثبت شده است"
                    return@launch
                }
                val finalName = name.trim().ifBlank { "(بی نام)" }
                val customer = Customer(
                    orgId = state.activeOrgId,
                    name = finalName,
                    email = email.trim(),
                    phone = cleanPhone,
                    address = address.trim()
                )
                customerRepository.addCustomer(customer)
                _snackbarMessage.value = "مشتری '${customer.name}' با موفقیت ثبت شد"
                _isAddCustomerDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در افزودن مشتری: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val state = tenantState.value
                val cleanPhone = customer.phone.trim()
                if (cleanPhone.isBlank()) {
                    _snackbarMessage.value = "شماره موبایل مشتری الزامی است"
                    return@launch
                }
                val existing = customerRepository.getCustomerByPhone(state.activeOrgId, cleanPhone)
                if (existing != null && existing.id != customer.id) {
                    _snackbarMessage.value = "شماره موبایل '$cleanPhone' متعلق به مشتری دیگری است"
                    return@launch
                }
                val updatedName = customer.name.trim().ifBlank { "(بی نام)" }
                val updatedCustomer = customer.copy(phone = cleanPhone, name = updatedName)
                customerRepository.updateCustomer(updatedCustomer)
                _snackbarMessage.value = "اطلاعات '${updatedCustomer.name}' به روز شد"
                _selectedCustomerForEdit.value = null
                _selectedCustomerForDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در ویرایش مشتری: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun selectCustomerForDetail(customer: Customer?) {
        _selectedCustomerForDetail.value = customer
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val customerRepository: CustomerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomerViewModel(tenantRepository, customerRepository) as T
        }
    }
}

