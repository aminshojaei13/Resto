package com.braveboy.calcuapp.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.Purchase
import com.braveboy.calcuapp.data.model.PurchaseItem
import com.braveboy.calcuapp.data.model.Supplier
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.ProductRepository
import com.braveboy.calcuapp.data.repository.PurchaseRepository
import com.braveboy.calcuapp.data.repository.SupplierRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PurchasesViewModel(
    private val tenantRepository: TenantRepository,
    private val purchaseRepository: PurchaseRepository,
    private val supplierRepository: SupplierRepository,
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
                    purchaseRepository.refreshPurchases(state.activeOrgId)
                    supplierRepository.refreshSuppliers(state.activeOrgId)
                    productRepository.refreshProducts(state.activeOrgId)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val purchases: StateFlow<List<Purchase>> = tenantState.flatMapLatest { state ->
        purchaseRepository.getPurchases(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val suppliers: StateFlow<List<Supplier>> = tenantState.flatMapLatest { state ->
        supplierRepository.getSuppliers(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = tenantState.flatMapLatest { state ->
        productRepository.getProducts(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isCreatePurchaseDialogOpen = MutableStateFlow(false)
    val isCreatePurchaseDialogOpen: StateFlow<Boolean> = _isCreatePurchaseDialogOpen.asStateFlow()

    private val _selectedPurchaseDetail = MutableStateFlow<Purchase?>(null)
    val selectedPurchaseDetail: StateFlow<Purchase?> = _selectedPurchaseDetail.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun openCreatePurchaseDialog() {
        _isCreatePurchaseDialogOpen.value = true
    }

    fun closeCreatePurchaseDialog() {
        _isCreatePurchaseDialogOpen.value = false
    }

    fun selectPurchaseDetail(purchase: Purchase?) {
        _selectedPurchaseDetail.value = purchase
    }

    fun createPurchase(
        supplierId: String,
        supplierName: String,
        warehouseId: String,
        items: List<PurchaseItem>
    ) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                purchaseRepository.createPurchase(
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId,
                    warehouseId = warehouseId,
                    supplierId = supplierId,
                    supplierName = supplierName,
                    items = items
                )
                _snackbarMessage.value = "Purchase Order created successfully"
                _isCreatePurchaseDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to create PO: ${e.localizedMessage}"
            }
        }
    }

    fun receiveGoods(purchase: Purchase) {
        viewModelScope.launch {
            try {
                purchaseRepository.receivePurchase(purchase)
                _snackbarMessage.value = "Goods received into warehouse and stock updated!"
                _selectedPurchaseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to receive goods: ${e.localizedMessage}"
            }
        }
    }

    fun paySupplier(purchaseId: String, amount: Double, method: String) {
        viewModelScope.launch {
            try {
                purchaseRepository.payPurchase(purchaseId, amount, method)
                _snackbarMessage.value = "Supplier payment recorded!"
                _selectedPurchaseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to record payment: ${e.localizedMessage}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val purchaseRepository: PurchaseRepository,
        private val supplierRepository: SupplierRepository,
        private val productRepository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PurchasesViewModel(
                tenantRepository,
                purchaseRepository,
                supplierRepository,
                productRepository
            ) as T
        }
    }
}
