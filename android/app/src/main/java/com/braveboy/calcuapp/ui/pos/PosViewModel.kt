package com.braveboy.calcuapp.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.ProductVariant
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.CartRepository
import com.braveboy.calcuapp.data.repository.CustomerRepository
import com.braveboy.calcuapp.data.repository.ProductRepository
import com.braveboy.calcuapp.data.repository.SalesOrderRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(
    private val tenantRepository: TenantRepository,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val customerRepository: CustomerRepository
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
    val categories: StateFlow<List<String>> = tenantState.flatMapLatest { state ->
        productRepository.getProducts(state.activeOrgId).map { list ->
            list.map { it.category }.distinct().sorted()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val cartItems: StateFlow<List<CartItem>> = tenantState.flatMapLatest { state ->
        cartRepository.getCart(state.activeOrgId, state.activeStoreId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> = tenantState.flatMapLatest { state ->
        customerRepository.getCustomers(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _selectedVariantProduct = MutableStateFlow<Product?>(null)
    val selectedVariantProduct: StateFlow<Product?> = _selectedVariantProduct.asStateFlow()

    private val _isCheckoutModalOpen = MutableStateFlow(false)
    val isCheckoutModalOpen: StateFlow<Boolean> = _isCheckoutModalOpen.asStateFlow()

    private val _isBarcodeScannerOpen = MutableStateFlow(false)
    val isBarcodeScannerOpen: StateFlow<Boolean> = _isBarcodeScannerOpen.asStateFlow()

    private val _lastCompletedOrder = MutableStateFlow<SalesOrder?>(null)
    val lastCompletedOrder: StateFlow<SalesOrder?> = _lastCompletedOrder.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun onProductClicked(product: Product) {
        if (product.variants.isNotEmpty()) {
            _selectedVariantProduct.value = product
        } else {
            addToCart(product)
        }
    }

    fun dismissVariantDialog() {
        _selectedVariantProduct.value = null
    }

    fun onVariantSelected(product: Product, variant: ProductVariant) {
        addToCart(product, variant)
        _selectedVariantProduct.value = null
    }

    fun addToCart(product: Product, variant: ProductVariant? = null, qty: Int = 1) {
        viewModelScope.launch {
            val state = tenantState.value
            cartRepository.addToCart(state.activeOrgId, state.activeStoreId, product, variant, qty)
            _snackbarMessage.value = "Added '${product.name}' to cart"
        }
    }

    fun updateCartQuantity(cartItem: CartItem, delta: Int) {
        viewModelScope.launch {
            val newQty = cartItem.quantity + delta
            cartRepository.updateQuantity(cartItem.id, newQty)
        }
    }

    fun applyCartItemDiscount(cartItemId: String, discountPercent: Double) {
        viewModelScope.launch {
            cartRepository.applyItemDiscount(cartItemId, discountPercent)
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(cartItemId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            val state = tenantState.value
            cartRepository.clearCart(state.activeOrgId, state.activeStoreId)
        }
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun openCheckoutModal() {
        if (cartItems.value.isNotEmpty()) {
            _isCheckoutModalOpen.value = true
        } else {
            _snackbarMessage.value = "Cart is empty"
        }
    }

    fun closeCheckoutModal() {
        _isCheckoutModalOpen.value = false
    }

    fun openBarcodeScanner() {
        _isBarcodeScannerOpen.value = true
    }

    fun closeBarcodeScanner() {
        _isBarcodeScannerOpen.value = false
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val state = tenantState.value
            val matchedProduct = productRepository.getProductByBarcode(state.activeOrgId, barcode).firstOrNull()
            if (matchedProduct != null) {
                addToCart(matchedProduct)
                _snackbarMessage.value = "Scanned: ${matchedProduct.name}"
            } else {
                _snackbarMessage.value = "No product found with barcode: $barcode"
            }
        }
    }

    fun processCheckout(paymentMethod: PaymentMethod, notes: String) {
        viewModelScope.launch {
            try {
                val state = tenantState.value
                val items = cartItems.value
                if (items.isEmpty()) return@launch

                val order = salesOrderRepository.processCheckout(
                    orgId = state.activeOrgId,
                    storeId = state.activeStoreId,
                    warehouseId = state.activeWarehouseId,
                    customer = selectedCustomer.value,
                    cartItems = items,
                    paymentMethod = paymentMethod,
                    notes = notes
                )

                _lastCompletedOrder.value = order
                _isCheckoutModalOpen.value = false
                _selectedCustomer.value = null
                _snackbarMessage.value = "Order ${order.orderNumber} completed!"
            } catch (e: Exception) {
                _snackbarMessage.value = "Checkout failed: ${e.localizedMessage}"
            }
        }
    }

    fun dismissReceipt() {
        _lastCompletedOrder.value = null
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val productRepository: ProductRepository,
        private val cartRepository: CartRepository,
        private val salesOrderRepository: SalesOrderRepository,
        private val customerRepository: CustomerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PosViewModel(
                tenantRepository,
                productRepository,
                cartRepository,
                salesOrderRepository,
                customerRepository
            ) as T
        }
    }
}
