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

    init {
        viewModelScope.launch {
            tenantState.collect { state ->
                if (state.activeOrgId.isNotBlank()) {
                    productRepository.refreshProducts(state.activeOrgId)
                }
            }
        }
    }

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

    private val _isAddProductDialogOpen = MutableStateFlow(false)
    val isAddProductDialogOpen: StateFlow<Boolean> = _isAddProductDialogOpen.asStateFlow()

    private val _selectedProductForAdjustment = MutableStateFlow<Product?>(null)
    val selectedProductForAdjustment: StateFlow<Product?> = _selectedProductForAdjustment.asStateFlow()

    private val _selectedProductForEdit = MutableStateFlow<Product?>(null)
    val selectedProductForEdit: StateFlow<Product?> = _selectedProductForEdit.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun openAddProductDialog() {
        _isAddProductDialogOpen.value = true
    }

    fun closeAddProductDialog() {
        _isAddProductDialogOpen.value = false
    }

    fun addProduct(
        name: String,
        sku: String,
        barcode: String,
        price: Double,
        costPrice: Double,
        category: String,
        unit: String,
        description: String,
        initialStock: Int,
        warehouseId: String
    ) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                val newProduct = Product(
                    id = java.util.UUID.randomUUID().toString(),
                    orgId = state.activeOrgId,
                    sku = sku.trim(),
                    barcode = barcode.trim(),
                    name = name.trim(),
                    description = description.trim(),
                    price = price,
                    costPrice = costPrice,
                    category = category.trim().ifBlank { "عمومی" },
                    unit = unit.trim().ifBlank { "عدد" }
                )
                val targetWarehouseId = warehouseId.ifBlank { state.activeWarehouseId }
                productRepository.addProduct(
                    product = newProduct,
                    initialStock = initialStock,
                    warehouseId = targetWarehouseId,
                    storeId = state.activeStoreId
                )
                _snackbarMessage.value = "کالای '${newProduct.name}' با موفقیت ثبت شد"
                _isAddProductDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در ثبت کالا: ${e.localizedMessage}"
            }
        }
    }

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

    fun openEditProduct(product: Product) {
        _selectedProductForEdit.value = product
    }

    fun closeEditProduct() {
        _selectedProductForEdit.value = null
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                productRepository.updateProduct(product)
                _snackbarMessage.value = "Product '${product.name}' updated successfully"
                _selectedProductForEdit.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to update product: ${e.localizedMessage}"
            }
        }
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
