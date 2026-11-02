package com.braveboy.resto.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.resto.data.model.LedgerCategory
import com.braveboy.resto.data.model.LedgerEntry
import com.braveboy.resto.data.model.LedgerType
import com.braveboy.resto.data.model.MetricsSummary
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.repository.LedgerRepository
import com.braveboy.resto.data.repository.ProductRepository
import com.braveboy.resto.data.repository.SalesOrderRepository
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

class DashboardViewModel(
    private val tenantRepository: TenantRepository,
    private val ledgerRepository: LedgerRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val salesOrderRepository: SalesOrderRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val metricsSummary: StateFlow<MetricsSummary> = tenantState.flatMapLatest { state ->
        ledgerRepository.getMetricsSummary(state.activeOrgId, state.activeStoreId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetricsSummary()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val ledgerEntries: StateFlow<List<LedgerEntry>> = tenantState.flatMapLatest { state ->
        ledgerRepository.getLedgerEntriesByStore(state.activeOrgId, state.activeStoreId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentSalesOrders: StateFlow<List<SalesOrder>> = tenantState.flatMapLatest { state ->
        salesOrderRepository.getOrdersByStore(state.activeOrgId, state.activeStoreId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isRecordEntryDialogOpen = MutableStateFlow(false)
    val isRecordEntryDialogOpen: StateFlow<Boolean> = _isRecordEntryDialogOpen.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun openRecordEntryDialog() {
        _isRecordEntryDialogOpen.value = true
    }

    fun closeRecordEntryDialog() {
        _isRecordEntryDialogOpen.value = false
    }

    fun recordLedgerEntry(
        type: LedgerType,
        category: LedgerCategory,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val state = tenantState.value
                val entryNumber = "MAN-${System.currentTimeMillis().toString().takeLast(6)}"
                val entry = LedgerEntry(
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId,
                    entryNumber = entryNumber,
                    type = type,
                    category = category,
                    amount = amount,
                    description = description
                )
                ledgerRepository.recordEntry(entry)
                _snackbarMessage.value = "تراکنش $entryNumber ثبت شد"
                _isRecordEntryDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در ثبت تراکنش: ${e.localizedMessage}"
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
        private val ledgerRepository: LedgerRepository,
        private val productRepository: ProductRepository,
        private val supplierRepository: SupplierRepository,
        private val salesOrderRepository: SalesOrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                tenantRepository,
                ledgerRepository,
                productRepository,
                supplierRepository,
                salesOrderRepository
            ) as T
        }
    }
}
