package com.braveboy.calcuapp.ui.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.braveboy.calcuapp.data.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToTenantSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val selectedVariantProduct by viewModel.selectedVariantProduct.collectAsState()
    val isCheckoutModalOpen by viewModel.isCheckoutModalOpen.collectAsState()
    val isBarcodeScannerOpen by viewModel.isBarcodeScannerOpen.collectAsState()
    val lastCompletedOrder by viewModel.lastCompletedOrder.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var isMobileCartSheetOpen by remember { mutableStateOf(false) }
    var isCustomerPickerOpen by remember { mutableStateOf(false) }

    // Adaptive Window Size check
    val windowInfo = currentWindowAdaptiveInfo()
    val isExpandedWidth = windowInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mobile POS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Org: ${tenantState.activeOrgId} | Store: ${tenantState.activeStoreId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openBarcodeScanner() }) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scan Barcode"
                        )
                    }
                    IconButton(onClick = onNavigateToTenantSwitch) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = "Switch Store / Org"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (!isExpandedWidth) {
                val totalItems = cartItems.sumOf { it.quantity }
                BadgedBox(
                    badge = {
                        if (totalItems > 0) {
                            Badge { Text("$totalItems") }
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = { isMobileCartSheetOpen = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(imageVector = Icons.Rounded.ShoppingCart, contentDescription = "View Cart")
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Main Product Catalog Area
            Column(
                modifier = Modifier
                    .weight(if (isExpandedWidth) 1.5f else 1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search product name, SKU, barcode, category...") },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All Categories") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product Grid
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                activeWarehouseId = tenantState.activeWarehouseId,
                                onProductClick = { viewModel.onProductClicked(it) }
                            )
                        }
                    }
                }
            }

            // Expanded Tablet / Desktop Side Cart Panel
            if (isExpandedWidth) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CartPanel(
                        cartItems = cartItems,
                        selectedCustomer = selectedCustomer,
                        onUpdateQuantity = { item, delta -> viewModel.updateCartQuantity(item, delta) },
                        onApplyDiscount = { id, percent -> viewModel.applyCartItemDiscount(id, percent) },
                        onRemoveItem = { id -> viewModel.removeFromCart(id) },
                        onClearCart = { viewModel.clearCart() },
                        onSelectCustomerClick = { isCustomerPickerOpen = true },
                        onCheckoutClick = { viewModel.openCheckoutModal() }
                    )
                }
            }
        }
    }

    // Mobile Bottom Sheet Cart
    if (!isExpandedWidth && isMobileCartSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isMobileCartSheetOpen = false }
        ) {
            CartPanel(
                cartItems = cartItems,
                selectedCustomer = selectedCustomer,
                onUpdateQuantity = { item, delta -> viewModel.updateCartQuantity(item, delta) },
                onApplyDiscount = { id, percent -> viewModel.applyCartItemDiscount(id, percent) },
                onRemoveItem = { id -> viewModel.removeFromCart(id) },
                onClearCart = { viewModel.clearCart() },
                onSelectCustomerClick = {
                    isCustomerPickerOpen = true
                },
                onCheckoutClick = {
                    isMobileCartSheetOpen = false
                    viewModel.openCheckoutModal()
                }
            )
        }
    }

    // Customer Selection Modal
    if (isCustomerPickerOpen) {
        CustomerPickerModal(
            customers = customers,
            selectedCustomer = selectedCustomer,
            onSelectCustomer = {
                viewModel.selectCustomer(it)
                isCustomerPickerOpen = false
            },
            onDismiss = { isCustomerPickerOpen = false }
        )
    }

    // Variant Selection Dialog
    selectedVariantProduct?.let { product ->
        VariantSelectionDialog(
            product = product,
            onVariantSelected = { variant -> viewModel.onVariantSelected(product, variant) },
            onDismiss = { viewModel.dismissVariantDialog() }
        )
    }

    // Checkout Modal
    if (isCheckoutModalOpen) {
        CheckoutModal(
            cartItems = cartItems,
            selectedCustomer = selectedCustomer,
            onProcessCheckout = { method, notes -> viewModel.processCheckout(method, notes) },
            onDismiss = { viewModel.closeCheckoutModal() }
        )
    }

    // Receipt Modal
    lastCompletedOrder?.let { order ->
        ReceiptDialog(
            order = order,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }

    // Barcode Scanner Dialog
    if (isBarcodeScannerOpen) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode -> viewModel.onBarcodeScanned(barcode) },
            onDismiss = { viewModel.closeBarcodeScanner() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerModal(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelectCustomer: (Customer?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Attach Customer Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Walk-in default option
            Card(
                onClick = { onSelectCustomer(null) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedCustomer == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Walk-in Customer (Guest)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customers) { customer ->
                    Card(
                        onClick = { onSelectCustomer(customer) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCustomer?.id == customer.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = customer.phone.ifBlank { customer.email },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Points: ${customer.loyaltyPoints}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
