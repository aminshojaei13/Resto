package com.braveboy.calcuapp.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.Store
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.model.Warehouse
import com.braveboy.calcuapp.data.repository.ProductRepository
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

class InventoryViewModel(
    private val tenantRepository: TenantRepository,
    private val productRepository: ProductRepository
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
    val products: StateFlow<List<Product>> = combine(
        tenantState,
        _searchQuery,
        _selectedCategory
    ) { state, query, category ->
        Triple(state.activeOrgId, query, category)
    }.flatMapLatest { (orgId, query, category) ->
        productRepository.searchProducts(orgId, query).map { list ->
            if (category == null) list else list.filter { it.category.equals(category, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val stores: StateFlow<List<Store>> = tenantState.flatMapLatest { state ->
        tenantRepository.getStoresForOrg(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val warehouses: StateFlow<List<Warehouse>> = tenantState.flatMapLatest { state ->
        tenantRepository.getWarehousesForStore(state.activeStoreId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedProductForAdjustment = MutableStateFlow<Product?>(null)
    val selectedProductForAdjustment: StateFlow<Product?> = _selectedProductForAdjustment.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun openStockAdjustment(product: Product) {
        _selectedProductForAdjustment.value = product
    }

    fun closeStockAdjustment() {
        _selectedProductForAdjustment.value = null
    }

    fun submitStockAdjustment(
        productId: String,
        warehouseId: String,
        delta: Int,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                productRepository.adjustStock(
                    productId = productId,
                    warehouseId = warehouseId,
                    delta = delta,
                    reason = reason,
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId
                )
                _snackbarMessage.value = "Stock updated successfully"
                _selectedProductForAdjustment.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to update stock: ${e.localizedMessage}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val productRepository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InventoryViewModel(tenantRepository, productRepository) as T
        }
    }
}
