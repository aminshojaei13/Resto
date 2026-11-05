package com.braveboy.resto.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.Purchase
import com.braveboy.resto.data.model.PurchaseItem
import com.braveboy.resto.data.model.Supplier
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.repository.ProductRepository
import com.braveboy.resto.data.repository.PurchaseRepository
import com.braveboy.resto.data.repository.SupplierRepository
import com.braveboy.resto.data.repository.TenantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.firstOrNull

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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        viewModelScope.launch {
            tenantState.collect { state ->
                if (state.activeOrgId.isNotBlank()) {
                    val localPurchases = purchaseRepository.getPurchases(state.activeOrgId).firstOrNull() ?: emptyList()
                    _isLoading.value = localPurchases.isEmpty()

                    viewModelScope.launch {
                        try {
                            purchaseRepository.refreshPurchases(state.activeOrgId)
                        } finally {
                            _isLoading.value = false
                        }
                    }

                    viewModelScope.launch {
                        try {
                            supplierRepository.refreshSuppliers(state.activeOrgId)
                        } catch (_: Exception) {}
                    }

                    viewModelScope.launch {
                        try {
                            productRepository.refreshProducts(state.activeOrgId)
                        } catch (_: Exception) {}
                    }
                } else {
                    _isLoading.value = false
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
            _isSaving.value = true
            try {
                val state = tenantState.value
                val finalSupplierName = supplierName.ifBlank { "فروشنده بی‌نام" }
                purchaseRepository.createPurchase(
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId,
                    warehouseId = warehouseId,
                    supplierId = supplierId,
                    supplierName = finalSupplierName,
                    items = items
                )
                _snackbarMessage.value = "فاکتور خرید با موفقیت ثبت شد"
                _isCreatePurchaseDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در ثبت فاکتور خرید: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun receiveGoods(purchase: Purchase) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                purchaseRepository.receivePurchase(purchase)
                _snackbarMessage.value = "کالاها تحویل انبار شدند و موجودی افزایش یافت!"
                _selectedPurchaseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در تحویل کالا: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun paySupplier(purchaseId: String, amount: Double, method: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                purchaseRepository.payPurchase(purchaseId, amount, method)
                _snackbarMessage.value = "پرداخت به تأمین‌کننده ثبت شد!"
                _selectedPurchaseDetail.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در ثبت پرداخت: ${e.localizedMessage}"
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
