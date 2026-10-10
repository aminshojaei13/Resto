package com.braveboy.calcuapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.MetricsSummary
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.LedgerRepository
import com.braveboy.calcuapp.data.repository.ProductRepository
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
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

    private val _isWelcomeDialogExplicitlyRequested = MutableStateFlow(false)

    val showWelcomeDialog: StateFlow<Boolean> = combine(
        tenantRepository.hasSeenOnboardingDialog,
        _isWelcomeDialogExplicitlyRequested
    ) { hasSeen, requested ->
        requested || !hasSeen
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val onboardingState: StateFlow<OnboardingState> = tenantState.flatMapLatest { state ->
        val orgId = state.activeOrgId
        val storeId = state.activeStoreId
        combine(
            tenantRepository.isOnboardingDismissed,
            tenantRepository.isSupplierStepSkipped,
            productRepository.getProducts(orgId),
            supplierRepository.getSuppliers(orgId),
            salesOrderRepository.getOrdersByStore(orgId, storeId)
        ) { isDismissed, isSupplierSkipped, products, suppliers, orders ->
            OnboardingState(
                isDismissed = isDismissed,
                hasProducts = products.isNotEmpty(),
                hasSuppliers = suppliers.isNotEmpty(),
                isSupplierSkipped = isSupplierSkipped,
                hasOrders = orders.isNotEmpty(),
                hasCompletedOrders = orders.any { it.fulfillmentStatus == FulfillmentStatus.COMPLETED }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OnboardingState()
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
                _snackbarMessage.value = "Recorded entry $entryNumber"
                _isRecordEntryDialogOpen.value = false
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to record entry: ${e.localizedMessage}"
            }
        }
    }

    fun dismissOnboarding() {
        viewModelScope.launch {
            tenantRepository.setOnboardingDismissed(true)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            tenantRepository.setOnboardingDismissed(false)
            tenantRepository.setSupplierStepSkipped(false)
            tenantRepository.setHasSeenOnboardingDialog(false)
            _isWelcomeDialogExplicitlyRequested.value = true
        }
    }

    fun skipSupplierStep() {
        viewModelScope.launch {
            tenantRepository.setSupplierStepSkipped(true)
        }
    }

    fun dismissWelcomeDialog(permanently: Boolean = true) {
        _isWelcomeDialogExplicitlyRequested.value = false
        if (permanently) {
            viewModelScope.launch {
                tenantRepository.setHasSeenOnboardingDialog(true)
            }
        }
    }

    fun openWelcomeDialog() {
        _isWelcomeDialogExplicitlyRequested.value = true
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
