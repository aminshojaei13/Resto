package com.braveboy.calcuapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.MetricsSummary
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.LedgerRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
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
    private val ledgerRepository: LedgerRepository
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

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val ledgerRepository: LedgerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(tenantRepository, ledgerRepository) as T
        }
    }
}
