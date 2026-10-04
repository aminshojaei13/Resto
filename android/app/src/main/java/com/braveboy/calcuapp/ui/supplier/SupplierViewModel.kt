package com.braveboy.calcuapp.ui.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.Supplier
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.SupplierRepository
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

class SupplierViewModel(
    private val tenantRepository: TenantRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val suppliers: StateFlow<List<Supplier>> = combine(
        tenantState,
        _searchQuery
    ) { state, query ->
        Pair(state.activeOrgId, query)
    }.flatMapLatest { (orgId, query) ->
        supplierRepository.searchSuppliers(orgId, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isAddSupplierDialogOpen = MutableStateFlow(false)
    val isAddSupplierDialogOpen: StateFlow<Boolean> = _isAddSupplierDialogOpen.asStateFlow()

    private val _selectedSupplierForDetail = MutableStateFlow<Supplier?>(null)
    val selectedSupplierForDetail: StateFlow<Supplier?> = _selectedSupplierForDetail.asStateFlow()

    private val _selectedSupplierForEdit = MutableStateFlow<Supplier?>(null)
    val selectedSupplierForEdit: StateFlow<Supplier?> = _selectedSupplierForEdit.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddSupplierDialog() {
        _isAddSupplierDialogOpen.value = true
    }

    fun closeAddSupplierDialog() {
        _isAddSupplierDialogOpen.value = false
    }

    fun openEditSupplier(supplier: Supplier) {
        _selectedSupplierForEdit.value = supplier
    }

    fun closeEditSupplier() {
        _selectedSupplierForEdit.value = null
    }

    fun selectSupplierForDetail(supplier: Supplier?) {
        _selectedSupplierForDetail.value = supplier
    }

    fun addSupplier(name: String, email: String, phone: String, address: String) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                val supplier = Supplier(
                    orgId = state.activeOrgId,
                    name = name,
                    email = email,
                    phone = phone,
                    address = address
                )
                supplierRepository.addSupplier(supplier)
                _snackbarMessage.value = "Added supplier '$name'"
                _isAddSupplierDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to add supplier: ${e.localizedMessage}"
            }
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                supplierRepository.updateSupplier(supplier)
                _snackbarMessage.value = "Updated supplier '${supplier.name}'"
                _selectedSupplierForEdit.value = null
                _selectedSupplierForDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to update supplier: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                supplierRepository.deleteSupplier(supplier.id)
                _snackbarMessage.value = "Deleted supplier '${supplier.name}'"
                _selectedSupplierForDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to delete supplier: ${e.localizedMessage}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val supplierRepository: SupplierRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SupplierViewModel(tenantRepository, supplierRepository) as T
        }
    }
}
