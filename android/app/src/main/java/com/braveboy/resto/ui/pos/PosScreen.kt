package com.braveboy.resto.ui.pos

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PersonAdd
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.ui.components.RestoLoadingState
import com.braveboy.resto.ui.components.RestoTopBar

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToTenantSwitch: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val customGuestName by viewModel.customGuestName.collectAsState()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsState()
    val selectedVariantProduct by viewModel.selectedVariantProduct.collectAsState()
    val isCheckoutModalOpen by viewModel.isCheckoutModalOpen.collectAsState()
    val isBarcodeScannerOpen by viewModel.isBarcodeScannerOpen.collectAsState()
    val lastCompletedOrder by viewModel.lastCompletedOrder.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCheckingOut by viewModel.isCheckingOut.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var isMobileCartSheetOpen by remember { mutableStateOf(false) }
    var isCustomerPickerOpen by remember { mutableStateOf(false) }

    val isPersian = tenantState.language == "fa"
    val currencyUnit = if (isPersian) "تومان" else "$"

    val windowInfo = currentWindowAdaptiveInfo()
    val isExpandedWidth =
        windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = if (isPersian) "کاتالوگ فروش" else "Point of Sale",
                onNavigationClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.openBarcodeScanner() }) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = if (isPersian) "اسکن بارکد" else "Barcode Scanner",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onNavigateToTenantSwitch) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = if (isPersian) "تغییر شعبه / فروشگاه" else "Switch Store",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isExpandedWidth) {
                val totalItems = cartItems.sumOf { it.quantity }
                BadgedBox(
                    badge = {
                        if (totalItems > 0) {
                            Badge {
                                Text(
                                    if (isPersian) com.braveboy.resto.util.PersianFormatter.toPersianDigits(
                                        totalItems
                                    ) else "$totalItems"
                                )
                            }
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = { isMobileCartSheetOpen = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingCart,
                            contentDescription = if (isPersian) "مشاهده سبد خرید" else "View Cart"
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
                    placeholder = {
                        Text(
                            text = if (isPersian) "جستجوی کالا، بارکد یا کد SKU..." else "Search product name, SKU, barcode, category...",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = if (isPersian) "پاک کردن جستجو" else "Clear search"
                                )
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
                            label = {
                                Text(
                                    if (isPersian) "همه دسته‌ها" else "All Categories",
                                    maxLines = 1
                                )
                            }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product Grid / Loading / Empty
                if (isLoading && products.isEmpty()) {
                    RestoLoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        message = if (isPersian) "در حال دریافت محصولات..." else "Loading products..."
                    )
                } else if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPersian) "کالایی در این دسته‌بندی یافت نشد" else "No products found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                activeWarehouseId = tenantState.activeWarehouseId,
                                onProductClick = { viewModel.onProductClicked(it) },
                                isPersian = isPersian,
                                currencyUnit = currencyUnit
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
                        selectedChannel = selectedChannel,
                        customGuestName = customGuestName,
                        onUpdateQuantity = { item, delta ->
                            viewModel.updateCartQuantity(
                                item,
                                delta
                            )
                        },
                        onApplyDiscount = { id, percent ->
                            viewModel.applyCartItemDiscount(
                                id,
                                percent
                            )
                        },
                        onRemoveItem = { id -> viewModel.removeFromCart(id) },
                        onClearCart = { viewModel.clearCart() },
                        onSelectCustomerClick = { isCustomerPickerOpen = true },
                        onCheckoutClick = { viewModel.openCheckoutModal() },
                        isPersian = isPersian,
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
                },
                isPersian = isPersian,
                currencyUnit = currencyUnit
            )
        }
    }

    // Customer Selection Modal
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
                isCustomerPickerOpen = false
            },
            onCreateQuickCustomer = { name, phone, address ->
                viewModel.createQuickCustomer(name, phone, address) {
                    isCustomerPickerOpen = false
                }
            },
            onDismiss = { isCustomerPickerOpen = false },
            isPersian = isPersian
        )
    }

    // Variant Selection Dialog
    selectedVariantProduct?.let { product ->
        VariantSelectionDialog(
            product = product,
            onVariantSelected = { variant -> viewModel.onVariantSelected(product, variant) },
            onDismiss = { viewModel.dismissVariantDialog() },
            isPersian = isPersian,
            currencyUnit = currencyUnit
        )
    }

    // Checkout Modal
    if (isCheckoutModalOpen) {
        CheckoutModal(
            cartItems = cartItems,
            selectedCustomer = selectedCustomer,
            initialChannel = selectedChannel,
            customGuestName = customGuestName,
            onProcessCheckout = { method, notes, channel -> viewModel.processCheckout(method, notes, channel) },
            onDismiss = { viewModel.closeCheckoutModal() },
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            isCheckingOut = isCheckingOut
        )
    }

    // Receipt Modal
    lastCompletedOrder?.let { order ->
        ReceiptDialog(
            order = order,
            onDismiss = { viewModel.dismissReceipt() },
            isPersian = isPersian,
            currencyUnit = currencyUnit
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
    selectedChannel: SalesChannel,
    customGuestName: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectChannel: (SalesChannel) -> Unit,
    onCustomGuestNameChange: (String) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onCreateQuickCustomer: (name: String, phone: String, address: String) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true
) {
    var isQuickAddOpen by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerAddress by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (isPersian) "انتخاب مشتری یا کانال ثبت سفارش" else "Attach Customer or Sales Channel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: Sales Channel Selection (Non-registered / Guest Channel)
            Text(
                text = if (isPersian) "۱. تعیین کانال ثبت سفارش (مشتری مهمان / تلفنی / اینترنتی)" else "1. Order Channel (Guest / Phone / Online)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SalesChannel.entries.size) { idx ->
                    val channel = SalesChannel.entries[idx]
                    FilterChip(
                        selected = selectedChannel == channel && selectedCustomer == null,
                        onClick = {
                            onSelectChannel(channel)
                            onSelectCustomer(null)
                        },
                        label = {
                            Text(
                                text = if (isPersian) channel.titleFa else channel.titleEn,
                                fontWeight = if (selectedChannel == channel && selectedCustomer == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customGuestName,
                onValueChange = {
                    onCustomGuestNameChange(it)
                    if (it.isNotBlank()) onSelectCustomer(null)
                },
                label = {
                    Text(if (isPersian) "نام یا شماره تماس مشتری مهمان / سفارشی (اختیاری)" else "Custom Guest Name / Phone (Optional)")
                },
                placeholder = {
                    Text(if (isPersian) "مثلاً: آقای علوی (تلفنی) یا سفارش اینستاگرام" else "e.g., John (Phone) or Insta Order")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: Search & Select Registered Customer (CRM)
            Text(
                text = if (isPersian) "۲. یا انتخاب از مشتریان ثبت‌شده (CRM)" else "2. Select Registered Customer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(if (isPersian) "جستجوی نام، تلفن یا ایمیل مشتری..." else "Search customer name or phone...")
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

            Spacer(modifier = Modifier.height(8.dp))

            if (customers.isEmpty()) {
                Text(
                    text = if (isPersian) "مشتری متناسب با جستجو یافت نشد" else "No matching registered customer found",
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
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                                            text = customer.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "${if (isPersian) "امتیاز:" else "Pts:"} ${
                                        if (isPersian) com.braveboy.resto.util.PersianFormatter.toPersianDigits(
                                            customer.loyaltyPoints
                                        ) else customer.loyaltyPoints
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: Quick Add New Customer
            if (!isQuickAddOpen) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { isQuickAddOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPersian) "+ ثبت مشتری جدید در دیتابیس" else "+ Add New Customer")
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
                            text = if (isPersian) "ثبت مشتری جدید" else "Quick New Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text(if (isPersian) "نام و نام خانوادگی" else "Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newCustomerPhone,
                            onValueChange = { newCustomerPhone = it },
                            label = { Text(if (isPersian) "شماره تلفن" else "Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newCustomerAddress,
                            onValueChange = { newCustomerAddress = it },
                            label = { Text(if (isPersian) "آدرس" else "Address") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { isQuickAddOpen = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isPersian) "انصراف" else "Cancel")
                            }

                            androidx.compose.material3.Button(
                                onClick = {
                                    if (newCustomerName.isNotBlank()) {
                                        onCreateQuickCustomer(newCustomerName, newCustomerPhone, newCustomerAddress)
                                        isQuickAddOpen = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isPersian) "ثبت و انتخاب" else "Save & Select")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
