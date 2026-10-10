package com.braveboy.calcuapp.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.intake.PendingImportManager
import com.braveboy.calcuapp.data.intake.ParsedCustomer
import com.braveboy.calcuapp.data.intake.ParsedItem
import com.braveboy.calcuapp.data.intake.ParsedOrderDraft
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.model.Warehouse
import com.braveboy.calcuapp.data.repository.CustomerRepository
import com.braveboy.calcuapp.data.repository.MessageRepository
import com.braveboy.calcuapp.data.repository.ProductRepository
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class MessagesUiState(
    val rawText: String = "",
    val source: String = "MANUAL_PASTE",
    val isParsing: Boolean = false,
    val isCheckingOut: Boolean = false,
    val parsedDraft: ParsedOrderDraft? = null,
    val selectedWarehouseId: String = "",
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val orderSuccessNumber: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isProductMatchingDialogOpen: Boolean = false,
    val matchingItemIndex: Int? = null
)

class MessagesViewModel(
    private val tenantRepository: TenantRepository,
    private val messageRepository: MessageRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private val _catalogProducts = MutableStateFlow<List<Product>>(emptyList())
    val catalogProducts: StateFlow<List<Product>> = _catalogProducts.asStateFlow()

    private val _warehouses = MutableStateFlow<List<Warehouse>>(emptyList())
    val warehouses: StateFlow<List<Warehouse>> = _warehouses.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    init {
        viewModelScope.launch {
            tenantState.collect { state ->
                if (state.activeOrgId.isNotBlank()) {
                    launch {
                        productRepository.getProducts(state.activeOrgId).collect {
                            _catalogProducts.value = it
                        }
                    }
                    launch {
                        customerRepository.getCustomers(state.activeOrgId).collect {
                            _customers.value = it
                        }
                    }
                }
                if (state.activeStoreId.isNotBlank()) {
                    launch {
                        tenantRepository.getWarehousesForStore(state.activeStoreId).collect { whList ->
                            _warehouses.value = whList
                            if (_uiState.value.selectedWarehouseId.isBlank() && whList.isNotEmpty()) {
                                val defWh = whList.find { it.id == state.activeWarehouseId } ?: whList.first()
                                _uiState.value = _uiState.value.copy(selectedWarehouseId = defWh.id)
                            }
                        }
                    }
                }
            }
        }
        checkAndConsumePendingImport()
    }

    fun checkAndConsumePendingImport() {
        val pending = PendingImportManager.consumePendingImport()
        if (pending != null && pending.rawText.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                rawText = pending.rawText,
                source = pending.source
            )
            parseMessage(pending.rawText, pending.source)
        }
    }

    fun setRawText(text: String) {
        _uiState.value = _uiState.value.copy(rawText = text)
    }

    fun setSource(source: String) {
        _uiState.value = _uiState.value.copy(source = source)
    }

    fun pasteFromClipboard(text: String) {
        if (text.isNotBlank()) {
            val detected = PendingImportManager.detectSource(text, "MANUAL_PASTE")
            _uiState.value = _uiState.value.copy(
                rawText = text,
                source = detected
            )
        }
    }

    fun parseMessage(text: String? = null, source: String? = null) {
        val raw = text ?: _uiState.value.rawText
        val src = source ?: _uiState.value.source
        if (raw.isBlank()) return

        val state = tenantState.value
        val catalog = _catalogProducts.value
        val whId = _uiState.value.selectedWarehouseId.ifBlank { state.activeWarehouseId }
        val taxRate = 9.0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true, errorMessage = null, infoMessage = null)
            try {
                // Try remote parsing first
                val remoteResponse = messageRepository.parseMessage(raw, src, "fa")
                val draft = if (remoteResponse != null && remoteResponse.customer != null) {
                    val remoteCustomer = remoteResponse.customer
                    // Match customer against local store customers if phone matches
                    val existingCust = _customers.value.find { c ->
                        !c.phone.isNullOrBlank() && !remoteCustomer.phone.isNullOrBlank() &&
                                (c.phone == remoteCustomer.phone || c.phone.endsWith(remoteCustomer.phone.takeLast(7)))
                    }

                    val items = remoteResponse.items.map { itemDto ->
                        val matchedProd = if (!itemDto.productId.isNullOrBlank()) {
                            catalog.find { it.id == itemDto.productId }
                        } else {
                            catalog.find { it.sku.equals(itemDto.sku, ignoreCase = true) || it.name.equals(itemDto.name, ignoreCase = true) }
                        }

                        ParsedItem(
                            productId = matchedProd?.id ?: itemDto.productId,
                            productName = matchedProd?.name ?: itemDto.name,
                            rawText = itemDto.name,
                            sku = matchedProd?.sku ?: itemDto.sku,
                            quantity = itemDto.quantity,
                            unit = matchedProd?.unit ?: itemDto.unit ?: "عدد",
                            unitLabel = matchedProd?.unit ?: itemDto.unitLabel ?: "عدد",
                            catalogPrice = matchedProd?.price ?: itemDto.price,
                            statedPrice = itemDto.price,
                            isMatched = matchedProd != null || !itemDto.productId.isNullOrBlank(),
                            confidence = 0.85f
                        )
                    }

                    val unmatchedItems = remoteResponse.unmatchedItems.map { unmatched ->
                        ParsedItem(
                            productId = null,
                            productName = unmatched.productName.ifBlank { unmatched.rawText ?: "کالای نامشخص" },
                            rawText = unmatched.rawText ?: unmatched.productName,
                            quantity = unmatched.quantity,
                            unit = "عدد",
                            catalogPrice = 0.0,
                            statedPrice = 0.0,
                            isMatched = false,
                            confidence = 0f,
                            ambiguityReason = unmatched.reason
                        )
                    }

                    ParsedOrderDraft(
                        source = remoteResponse.source ?: src,
                        rawText = raw,
                        customer = ParsedCustomer(
                            id = existingCust?.id ?: remoteCustomer.id,
                            name = remoteCustomer.name ?: "مشتری",
                            phone = remoteCustomer.phone,
                            address = remoteCustomer.address,
                            isNew = existingCust == null
                        ),
                        items = items + unmatchedItems,
                        taxRate = remoteResponse.taxRate.takeIf { it > 0 } ?: taxRate,
                        warehouseId = whId
                    )
                } else {
                    // Fallback to local deterministic parser
                    messageRepository.parseMessageLocally(
                        rawText = raw,
                        source = src,
                        catalog = catalog,
                        taxRate = taxRate,
                        warehouseId = whId
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    parsedDraft = draft,
                    notes = draft.notes
                )
            } catch (e: Exception) {
                // If anything fails, use deterministic local parser
                val localDraft = messageRepository.parseMessageLocally(
                    rawText = raw,
                    source = src,
                    catalog = catalog,
                    taxRate = taxRate,
                    warehouseId = whId
                )
                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    parsedDraft = localDraft,
                    notes = localDraft.notes
                )
            }
        }
    }

    fun updateCustomerName(name: String) {
        val currentDraft = _uiState.value.parsedDraft ?: return
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(customer = currentDraft.customer.copy(name = name))
        )
    }

    fun updateCustomerPhone(phone: String) {
        val currentDraft = _uiState.value.parsedDraft ?: return
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(customer = currentDraft.customer.copy(phone = phone))
        )
    }

    fun updateCustomerAddress(address: String) {
        val currentDraft = _uiState.value.parsedDraft ?: return
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(customer = currentDraft.customer.copy(address = address))
        )
    }

    fun updateItemQuantity(index: Int, qty: Int) {
        val currentDraft = _uiState.value.parsedDraft ?: return
        if (index !in currentDraft.items.indices) return
        val updatedItems = currentDraft.items.toMutableList()
        val item = updatedItems[index]
        if (qty <= 0) {
            updatedItems.removeAt(index)
        } else {
            updatedItems[index] = item.copy(quantity = qty)
        }
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(items = updatedItems)
        )
    }

    fun removeItem(index: Int) {
        val currentDraft = _uiState.value.parsedDraft ?: return
        if (index !in currentDraft.items.indices) return
        val updatedItems = currentDraft.items.toMutableList()
        updatedItems.removeAt(index)
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(items = updatedItems)
        )
    }

    fun openProductMatchingDialog(index: Int?) {
        _uiState.value = _uiState.value.copy(
            isProductMatchingDialogOpen = true,
            matchingItemIndex = index
        )
    }

    fun closeProductMatchingDialog() {
        _uiState.value = _uiState.value.copy(
            isProductMatchingDialogOpen = false,
            matchingItemIndex = null
        )
    }

    fun matchProductToItem(itemIndex: Int, product: Product) {
        val currentDraft = _uiState.value.parsedDraft ?: return
        if (itemIndex !in currentDraft.items.indices) return
        val updatedItems = currentDraft.items.toMutableList()
        val old = updatedItems[itemIndex]
        updatedItems[itemIndex] = old.copy(
            productId = product.id,
            productName = product.name,
            sku = product.sku,
            unit = product.unit,
            unitLabel = product.unit,
            catalogPrice = product.price,
            isMatched = true,
            ambiguityReason = null
        )
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(items = updatedItems),
            isProductMatchingDialogOpen = false,
            matchingItemIndex = null
        )
    }

    fun addProductToDraft(product: Product) {
        val currentDraft = _uiState.value.parsedDraft ?: return
        val newItem = ParsedItem(
            productId = product.id,
            productName = product.name,
            rawText = product.name,
            sku = product.sku,
            quantity = 1,
            unit = product.unit,
            unitLabel = product.unit,
            catalogPrice = product.price,
            statedPrice = product.price,
            isMatched = true,
            confidence = 1.0f
        )
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(items = currentDraft.items + newItem)
        )
    }

    fun setWarehouseId(warehouseId: String) {
        _uiState.value = _uiState.value.copy(selectedWarehouseId = warehouseId)
        val currentDraft = _uiState.value.parsedDraft ?: return
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(warehouseId = warehouseId)
        )
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = method)
        val currentDraft = _uiState.value.parsedDraft ?: return
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(paymentMethod = method)
        )
    }

    fun setNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
        val currentDraft = _uiState.value.parsedDraft ?: return
        _uiState.value = _uiState.value.copy(
            parsedDraft = currentDraft.copy(notes = notes)
        )
    }

    fun clearDraft() {
        _uiState.value = _uiState.value.copy(
            rawText = "",
            parsedDraft = null,
            orderSuccessNumber = null,
            errorMessage = null,
            infoMessage = null,
            isProductMatchingDialogOpen = false,
            matchingItemIndex = null
        )
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(orderSuccessNumber = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun confirmAndCheckout(onSuccess: (orderNumber: String) -> Unit = {}) {
        val draft = _uiState.value.parsedDraft
        if (draft == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "پیش‌نویس سفارشی برای ثبت وجود ندارد.")
            return
        }

        if (draft.items.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "سفارش باید حداقل شامل یک کالا باشد.")
            return
        }

        if (draft.hasUnmatchedItems) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "برخی کالاها با کاتالوگ انبار تطبیق نیافته‌اند. لطفاً قبل از ثبت، آن‌ها را مشخص یا حذف کنید."
            )
            return
        }

        val state = tenantState.value
        val whId = _uiState.value.selectedWarehouseId.ifBlank { state.activeWarehouseId }
        if (whId.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "لطفاً انبار تحویل را انتخاب کنید.")
            return
        }

        val custName = draft.customer.name?.trim().takeIf { !it.isNullOrBlank() } ?: "مشتری متفرقه"
        val custPhone = draft.customer.phone?.trim() ?: ""
        val custAddress = draft.customer.address?.trim() ?: ""

        val cartItems = draft.items.mapNotNull { item ->
            val pId = item.productId ?: return@mapNotNull null
            CartItem(
                orgId = state.activeOrgId,
                storeId = state.activeStoreId,
                productId = pId,
                productName = item.productName,
                sku = item.sku ?: "",
                barcode = "",
                price = item.catalogPrice,
                quantity = item.quantity
            )
        }

        if (cartItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "کالاهای سفارش معتبر نیستند.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingOut = true, errorMessage = null)
            try {
                val customer = if (!draft.customer.id.isNullOrBlank()) {
                    Customer(
                        id = draft.customer.id,
                        orgId = state.activeOrgId,
                        name = custName,
                        phone = custPhone,
                        address = custAddress
                    )
                } else {
                    Customer(
                        id = UUID.randomUUID().toString(),
                        orgId = state.activeOrgId,
                        name = custName,
                        phone = custPhone,
                        address = custAddress
                    )
                }

                val idempotencyKey = "msg-ord-${UUID.randomUUID()}"
                val source = _uiState.value.source.lowercase()

                val createdOrder = salesOrderRepository.processCheckout(
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId,
                    warehouseId = whId,
                    customer = customer,
                    cartItems = cartItems,
                    paymentMethod = _uiState.value.selectedPaymentMethod,
                    notes = _uiState.value.notes.ifBlank { "ثبت سفارش از پیام ورودی (${draft.source})" },
                    idempotencyKey = idempotencyKey,
                    source = source,
                    paymentStatus = if (_uiState.value.selectedPaymentMethod == PaymentMethod.CASH) PaymentStatus.PAID else PaymentStatus.PENDING,
                    fulfillmentStatus = FulfillmentStatus.PENDING
                )

                _uiState.value = _uiState.value.copy(
                    isCheckingOut = false,
                    orderSuccessNumber = createdOrder.orderNumber,
                    parsedDraft = null,
                    rawText = ""
                )
                onSuccess(createdOrder.orderNumber)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCheckingOut = false,
                    errorMessage = "خطا در ثبت سفارش: ${e.localizedMessage ?: e.message}"
                )
            }
        }
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val messageRepository: MessageRepository,
        private val salesOrderRepository: SalesOrderRepository,
        private val customerRepository: CustomerRepository,
        private val productRepository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessagesViewModel(
                tenantRepository,
                messageRepository,
                salesOrderRepository,
                customerRepository,
                productRepository
            ) as T
        }
    }
}
