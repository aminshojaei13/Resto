package com.braveboy.calcuapp.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.CustomerRepository
import com.braveboy.calcuapp.data.repository.MessageRepository
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val tenantRepository: TenantRepository,
    private val messageRepository: MessageRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

    private val _rawText = MutableStateFlow("")
    val rawText: StateFlow<String> = _rawText.asStateFlow()

    private val _parsedPayload = MutableStateFlow<Map<String, Any>?>(null)
    val parsedPayload: StateFlow<Map<String, Any>?> = _parsedPayload.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setRawText(text: String) {
        _rawText.value = text
    }

    fun parseMessage(text: String, source: String = "manual_paste") {
        viewModelScope.launch {
            _isParsing.value = true
            try {
                val result = messageRepository.parseMessage(text, source)
                _parsedPayload.value = result
                _isParsing.value = false
            } catch (e: Exception) {
                _isParsing.value = false
                _snackbarMessage.value = "Failed to parse message: ${e.localizedMessage}"
            }
        }
    }

    fun checkoutParsedOrder(
        customerName: String,
        customerId: String?,
        paymentMethod: PaymentMethod,
        items: List<CartItem>
    ) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                val customer = if (customerId != null) {
                    Customer(id = customerId, orgId = state.activeOrgId, name = customerName)
                } else {
                    Customer(orgId = state.activeOrgId, name = customerName)
                }

                salesOrderRepository.processCheckout(
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId,
                    warehouseId = state.activeWarehouseId,
                    customer = customer,
                    cartItems = items,
                    paymentMethod = paymentMethod,
                    notes = "Checkout created from imported social message"
                )
                _snackbarMessage.value = "Order checked out successfully from social message!"
                _parsedPayload.value = null
                _rawText.value = ""
            } catch (e: Exception) {
                _snackbarMessage.value = "Failed to checkout order: ${e.localizedMessage}"
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val messageRepository: MessageRepository,
        private val salesOrderRepository: SalesOrderRepository,
        private val customerRepository: CustomerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessagesViewModel(
                tenantRepository,
                messageRepository,
                salesOrderRepository,
                customerRepository
            ) as T
        }
    }
}
