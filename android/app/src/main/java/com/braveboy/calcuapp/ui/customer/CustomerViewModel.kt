package com.braveboy.calcuapp.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.CustomerRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val tenantRepository: TenantRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

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

    fun addCustomer(name: String, email: String, phone: String, address: String) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                val customer = Customer(
                    orgId = state.activeOrgId,
                    name = name,
                    email = email,
                    phone = phone,
                    address = address
                )
                customerRepository.addCustomer(customer)
                _snackbarMessage.value = "Added customer '$name'"
                _isAddCustomerDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to add customer: ${e.localizedMessage}"
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
