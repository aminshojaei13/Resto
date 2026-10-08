package com.braveboy.calcuapp.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
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

class SalesOrdersViewModel(
    private val tenantRepository: TenantRepository,
    private val salesOrderRepository: SalesOrderRepository
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
                    salesOrderRepository.refreshOrders(state.activeOrgId)
                }
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPaymentStatus = MutableStateFlow<PaymentStatus?>(null)
    val selectedPaymentStatus: StateFlow<PaymentStatus?> = _selectedPaymentStatus.asStateFlow()

    private val _selectedFulfillmentStatus = MutableStateFlow<FulfillmentStatus?>(null)
    val selectedFulfillmentStatus: StateFlow<FulfillmentStatus?> = _selectedFulfillmentStatus.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: StateFlow<List<SalesOrder>> = combine(
        tenantState,
        _searchQuery,
        _selectedPaymentStatus,
        _selectedFulfillmentStatus
    ) { state, query, payStatus, fulStatus ->
        Tuple4(state.activeOrgId, state.activeStoreId, query, Pair(payStatus, fulStatus))
    }.flatMapLatest { (orgId, storeId, query, statusPair) ->
        val (payStatus, fulStatus) = statusPair
        salesOrderRepository.getOrdersByStore(orgId, storeId).map { list ->
            list.filter { order ->
                val matchesQuery = query.isBlank() ||
                        order.orderNumber.contains(query, ignoreCase = true) ||
                        order.customerName.contains(query, ignoreCase = true)

                val matchesPay = payStatus == null || order.paymentStatus == payStatus
                val matchesFul = fulStatus == null || order.fulfillmentStatus == fulStatus

                matchesQuery && matchesPay && matchesFul
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedOrderForDetail = MutableStateFlow<SalesOrder?>(null)
    val selectedOrderForDetail: StateFlow<SalesOrder?> = _selectedOrderForDetail.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun filterPaymentStatus(status: PaymentStatus?) {
        _selectedPaymentStatus.value = if (_selectedPaymentStatus.value == status) null else status
    }

    fun filterFulfillmentStatus(status: FulfillmentStatus?) {
        _selectedFulfillmentStatus.value = if (_selectedFulfillmentStatus.value == status) null else status
    }

    fun selectOrderForDetail(order: SalesOrder?) {
        _selectedOrderForDetail.value = order
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val salesOrderRepository: SalesOrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SalesOrdersViewModel(tenantRepository, salesOrderRepository) as T
        }
    }

    private data class Tuple4<A, B, C, D>(
        val a: A,
        val b: B,
        val c: C,
        val d: D
    )
}
