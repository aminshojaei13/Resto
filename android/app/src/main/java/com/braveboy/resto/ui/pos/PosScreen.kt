package com.braveboy.resto.ui.pos

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.ui.components.RestoChip
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoEmptyState
import com.braveboy.resto.ui.components.RestoLoadingState
import com.braveboy.resto.ui.components.RestoSearchField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onBackClick: () -> Unit = {},
    onNavigateToTenantSwitch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val customGuestName by viewModel.customGuestName.collectAsState()
    val isCheckoutModalOpen by viewModel.isCheckoutModalOpen.collectAsState()
    val isCheckingOut by viewModel.isCheckingOut.collectAsState()
    val lastCompletedOrder by viewModel.lastCompletedOrder.collectAsState()
    val selectedVariantProduct by viewModel.selectedVariantProduct.collectAsState()
    val isBarcodeScannerOpen by viewModel.isBarcodeScannerOpen.collectAsState()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    var isMobileCartSheetOpen by remember { mutableStateOf(false) }
    var isCustomerPickerOpen by remember { mutableStateOf(false) }
    var wasMobileCartOpenBeforePicker by remember { mutableStateOf(false) }

    fun closeCustomerPicker() {
        isCustomerPickerOpen = false
        if (wasMobileCartOpenBeforePicker) {
            isMobileCartSheetOpen = true
            wasMobileCartOpenBeforePicker = false
        }
    }

    val currencyUnit = stringResource(R.string.currency_unit)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val totalCartCount = cartItems.sumOf { it.quantity }

    BoxWithConstraints {
        val isExpandedWidth = maxWidth >= 720.dp

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                RestoTopBar(
                    title = stringResource(R.string.pos_catalog_title),
                    onNavigationClick = onBackClick,
                    actions = {
                        IconButton(onClick = { viewModel.openBarcodeScanner() }) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = stringResource(R.string.barcode_scanner_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onNavigateToTenantSwitch) {
                            Icon(
                                imageVector = Icons.Rounded.Storefront,
                                contentDescription = stringResource(R.string.switch_tenant),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!isExpandedWidth) {
                    BadgedBox(
                        badge = {
                            if (totalCartCount > 0) {
                                Badge {
                                    Text(PersianFormatter.toPersianDigits(totalCartCount))
                                }
                            }
                        }
                    ) {
                        FloatingActionButton(
                            onClick = { isMobileCartSheetOpen = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingCart,
                                contentDescription = stringResource(R.string.cart_title)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .fillMaxSize()
            ) {
                // LEFT SIDE: Product Catalog Grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = RestoSpacing.md)
                ) {
                    Spacer(modifier = Modifier.height(RestoSpacing.xs))

                    RestoSearchField(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        placeholder = stringResource(R.string.search_product_hint)
                    )

                    Spacer(modifier = Modifier.height(RestoSpacing.xs))

                    // Categories LazyRow
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            RestoChip(
                                text = stringResource(R.string.all_categories_chip),
                                selected = selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) }
                            )
                        }
                        items(categories) { cat ->
                            RestoChip(
                                text = cat,
                                selected = selectedCategory == cat,
                                onClick = { viewModel.selectCategory(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(RestoSpacing.sm))

                    if (isLoading && products.isEmpty()) {
                        RestoLoadingState(
                            modifier = Modifier.weight(1f),
                            message = stringResource(R.string.loading_pos_catalog)
                        )
                    } else if (products.isEmpty()) {
                        RestoEmptyState(
                            title = stringResource(R.string.no_products_title),
                            description = stringResource(R.string.no_products_desc),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isExpandedWidth) 3 else 2),
                            horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(products, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    activeWarehouseId = tenantState.activeWarehouseId,
                                    onProductClick = { viewModel.onProductClicked(it) },
                                    currencyUnit = currencyUnit
                                )
                            }
                        }
                    }
                }

                // RIGHT SIDE: Desktop/Tablet Expanded Cart
                if (isExpandedWidth) {
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        CartPanel(
                            cartItems = cartItems,
                            selectedCustomer = selectedCustomer,
                            selectedChannel = selectedChannel,
                            customGuestName = customGuestName,
                            onSelectCustomerClick = { isCustomerPickerOpen = true },
                            onUpdateQuantity = { item, delta ->
                                viewModel.updateCartQuantity(
                                    item,
                                    delta
                                )
                            },
                            onApplyDiscount = { itemId, pct ->
                                viewModel.applyCartItemDiscount(
                                    itemId,
                                    pct
                                )
                            },
                            onRemoveItem = { viewModel.removeFromCart(it) },
                            onClearCart = { viewModel.clearCart() },
                            onCheckoutClick = { viewModel.openCheckoutModal() },
                            currencyUnit = currencyUnit
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
                    selectedChannel = selectedChannel,
                    customGuestName = customGuestName,
                    onSelectCustomerClick = {
                        wasMobileCartOpenBeforePicker = true
                        isCustomerPickerOpen = true
                        isMobileCartSheetOpen = false
                    },
                    onUpdateQuantity = { item, delta -> viewModel.updateCartQuantity(item, delta) },
                    onApplyDiscount = { itemId, pct ->
                        viewModel.applyCartItemDiscount(
                            itemId,
                            pct
                        )
                    },
                    onRemoveItem = { viewModel.removeFromCart(it) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckoutClick = {
                        isMobileCartSheetOpen = false
                        viewModel.openCheckoutModal()
                    },
                    currencyUnit = currencyUnit
                )
            }
        }

        // Simplified Customer Picker Modal
        if (isCustomerPickerOpen) {
            CustomerPickerModal(
                customers = customers,
                selectedCustomer = selectedCustomer,
                selectedChannel = selectedChannel,
                customGuestName = customGuestName,
                searchQuery = customerSearchQuery,
                onSearchQueryChange = { viewModel.setCustomerSearchQuery(it) },
                onSelectChannel = { viewModel.selectSalesChannel(it) },
                onCustomGuestNameChange = { viewModel.setCustomGuestName(it) },
                onSelectCustomer = {
                    viewModel.selectCustomer(it)
                    closeCustomerPicker()
                },
                onCreateQuickCustomer = { name, phone, address ->
                    viewModel.createQuickCustomer(name, phone, address) {
                        closeCustomerPicker()
                    }
                },
                onDismiss = { closeCustomerPicker() }
            )
        }

        // Product Variants Dialog
        selectedVariantProduct?.let { product ->
            VariantSelectionDialog(
                product = product,
                onVariantSelected = { variant -> viewModel.onVariantSelected(product, variant) },
                onDismiss = { viewModel.dismissVariantDialog() },
                currencyUnit = currencyUnit
            )
        }

        // Checkout Modal
        if (isCheckoutModalOpen) {
            CheckoutModal(
                cartItems = cartItems,
                selectedCustomer = selectedCustomer,
                selectedChannel = selectedChannel,
                customGuestName = customGuestName,
                isCheckingOut = isCheckingOut,
                onProcessCheckout = { method, notes, channel ->
                    viewModel.processCheckout(
                        method,
                        notes,
                        channel
                    )
                },
                onDismiss = { viewModel.closeCheckoutModal() },
                currencyUnit = currencyUnit
            )
        }

        // Receipt Dialog
        lastCompletedOrder?.let { order ->
            ReceiptDialog(
                order = order,
                customerPhone = selectedCustomer?.phone ?: "",
                onDismiss = { viewModel.dismissReceipt() },
                currencyUnit = currencyUnit
            )
        }

        // Barcode Scanner Dialog Placeholder
        if (isBarcodeScannerOpen) {
            BarcodeScannerModal(
                onBarcodeScanned = { barcode ->
                    viewModel.onBarcodeScanned(barcode)
                    viewModel.closeBarcodeScanner()
                },
                onDismiss = { viewModel.closeBarcodeScanner() }
            )
        }
    }
}

/**
 * Simplified 2-Tab Customer & Sales Channel Picker Modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerModal(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    selectedChannel: SalesChannel,
    customGuestName: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectChannel: (SalesChannel) -> Unit,
    onCustomGuestNameChange: (String) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onCreateQuickCustomer: (name: String, phone: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        CustomerPickerContent(
            customers = customers,
            selectedCustomer = selectedCustomer,
            selectedChannel = selectedChannel,
            customGuestName = customGuestName,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSelectChannel = onSelectChannel,
            onCustomGuestNameChange = onCustomGuestNameChange,
            onSelectCustomer = onSelectCustomer,
            onCreateQuickCustomer = onCreateQuickCustomer,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun CustomerPickerContent(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    selectedChannel: SalesChannel,
    customGuestName: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectChannel: (SalesChannel) -> Unit,
    onCustomGuestNameChange: (String) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onCreateQuickCustomer: (name: String, phone: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(if (selectedCustomer != null) 0 else 1) }
    var isQuickAddOpen by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerAddress by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.customer_and_channel_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2-Tab Segmented Switcher
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        text = stringResource(R.string.crm_customers_tab),
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        text = stringResource(R.string.channel_guest_tab),
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            if (selectedTabIndex == 0) {
                // TAB 1: Registered CRM Customers
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(stringResource(R.string.search_customer_hint_simple))
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Walk-in / Guest Clear Option
                Card(
                    onClick = {
                        onSelectCustomer(null)
                        onSelectChannel(SalesChannel.WALK_IN)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCustomer == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = if (selectedCustomer == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.walk_in_guest_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCustomer == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.walk_in_guest_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedCustomer == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedCustomer == null) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (customers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_matching_customer_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        customers.take(5).forEach { customer ->
                            val isSelected = selectedCustomer?.id == customer.id
                            Card(
                                onClick = { onSelectCustomer(customer) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (customer.phone.isNotBlank()) {
                                            Text(
                                                text = PersianFormatter.toPersianDigits(customer.phone),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "امتیاز: ${PersianFormatter.toPersianDigits(customer.loyaltyPoints)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Add New Customer
                if (!isQuickAddOpen) {
                    OutlinedButton(
                        onClick = { isQuickAddOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_new_customer_db))
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.quick_new_customer),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newCustomerName,
                                onValueChange = { newCustomerName = it },
                                label = { Text(stringResource(R.string.full_name_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newCustomerPhone,
                                onValueChange = { newCustomerPhone = it },
                                label = { Text(stringResource(R.string.phone_number_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newCustomerAddress,
                                onValueChange = { newCustomerAddress = it },
                                label = { Text(stringResource(R.string.address_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { isQuickAddOpen = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }

                                Button(
                                    onClick = {
                                        val finalName = newCustomerName.trim().ifBlank { "(بی نام)" }
                                        onCreateQuickCustomer(
                                            finalName,
                                            newCustomerPhone,
                                            newCustomerAddress
                                        )
                                        isQuickAddOpen = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.save_info))
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 2: Order Sales Channel & Custom Guest Name
                Text(
                    text = stringResource(R.string.select_sales_channel_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SalesChannel.entries.size) { idx ->
                        val channel = SalesChannel.entries[idx]
                        FilterChip(
                            selected = selectedChannel == channel,
                            onClick = { onSelectChannel(channel) },
                            label = {
                                Text(
                                    text = channel.titleFa,
                                    fontWeight = if (selectedChannel == channel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = customGuestName,
                    onValueChange = onCustomGuestNameChange,
                    label = {
                        Text(stringResource(R.string.guest_name_hint))
                    },
                    placeholder = {
                        Text(stringResource(R.string.guest_name_placeholder))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSelectCustomer(null)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.confirm_channel))
                }
            }
        }
    }
}

@Composable
fun BarcodeScannerModal(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }

    RestoDialog(
        title = stringResource(R.string.scan_or_enter_barcode_title),
        confirmText = stringResource(R.string.add_to_cart),
        onConfirm = {
            if (manualBarcode.isNotBlank()) {
                onBarcodeScanned(manualBarcode.trim())
            }
        },
        confirmEnabled = manualBarcode.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.scan_or_enter_barcode_desc),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = manualBarcode,
                onValueChange = { manualBarcode = it },
                label = { Text(stringResource(R.string.barcode_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Customer Picker Content Preview")
@Composable
fun CustomerPickerContentPreview() {
    RestoTheme {
        CustomerPickerContent(
            customers = listOf(
                Customer(
                    id = "c1",
                    orgId = "org1",
                    name = "علی محمدی",
                    phone = "09123456789",
                    loyaltyPoints = 120
                )
            ),
            selectedCustomer = null,
            selectedChannel = SalesChannel.WALK_IN,
            customGuestName = "",
            searchQuery = "",
            onSearchQueryChange = {},
            onSelectChannel = {},
            onCustomGuestNameChange = {},
            onSelectCustomer = {},
            onCreateQuickCustomer = { _, _, _ -> },
            onDismiss = {}
        )
    }
}
