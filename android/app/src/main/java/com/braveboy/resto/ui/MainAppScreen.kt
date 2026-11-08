package com.braveboy.resto.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.braveboy.resto.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.di.AppContainer
import com.braveboy.resto.ui.components.RestoBottomNav
import com.braveboy.resto.ui.customer.CustomerScreen
import com.braveboy.resto.ui.customer.CustomerViewModel
import com.braveboy.resto.ui.dashboard.DashboardScreen
import com.braveboy.resto.ui.dashboard.DashboardViewModel
import com.braveboy.resto.ui.expenses.ExpensesScreen
import com.braveboy.resto.ui.expenses.ExpensesViewModel
import com.braveboy.resto.ui.inventory.InventoryScreen
import com.braveboy.resto.ui.inventory.InventoryViewModel
import com.braveboy.resto.ui.messages.MessagesScreen
import com.braveboy.resto.ui.messages.MessagesViewModel
import com.braveboy.resto.ui.orders.SalesOrdersScreen
import com.braveboy.resto.ui.orders.SalesOrdersViewModel
import com.braveboy.resto.ui.pos.PosScreen
import com.braveboy.resto.ui.pos.PosViewModel
import com.braveboy.resto.ui.purchases.PurchasesScreen
import com.braveboy.resto.ui.purchases.PurchasesViewModel
import com.braveboy.resto.ui.reports.ReportsScreen
import com.braveboy.resto.ui.reports.ReportsViewModel
import com.braveboy.resto.ui.settings.SettingsScreen
import com.braveboy.resto.ui.supplier.SupplierScreen
import com.braveboy.resto.ui.supplier.SupplierViewModel
import com.braveboy.resto.ui.tenant.TenantSwitcherModal
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing

enum class MainDestination(
    val title: String,
    val titleFa: String,
    val icon: ImageVector
) {
    ACCOUNTING("Dashboard", "داشبورد", Icons.Rounded.Assessment),
    REPORTS("Reports", "گزارش‌ها", Icons.Rounded.Assessment),
    ORDERS("Orders", "سفارش‌ها", Icons.AutoMirrored.Rounded.ReceiptLong),
    INVENTORY("Inventory", "موجودی", Icons.Rounded.Inventory),
    POS("New Sale", "ثبت سفارش", Icons.Rounded.PointOfSale),
    MESSAGES("Social Orders", "سفارش پیامکی", Icons.AutoMirrored.Rounded.Message),
    PURCHASES("Purchases", "خرید و تأمین", Icons.Rounded.ShoppingBag),
    CUSTOMERS("Customers", "مشتریان", Icons.Rounded.People),
    SUPPLIERS("Suppliers", "تأمین‌کنندگان", Icons.Rounded.Domain),
    EXPENSES("Expenses", "هزینه‌های جاری", Icons.Rounded.Payments),
    SETTINGS("Settings", "تنظیمات", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    initialSharedText: String? = null
) {
    // Navigation stack state (defaults to Dashboard)
    var navigationStack by remember { mutableStateOf(listOf(MainDestination.ACCOUNTING)) }
    val currentDestination = navigationStack.lastOrNull() ?: MainDestination.ACCOUNTING

    var isTenantSwitcherOpen by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }

    fun navigateTo(destination: MainDestination) {
        navigationStack = if (destination == MainDestination.ACCOUNTING) {
            listOf(MainDestination.ACCOUNTING)
        } else {
            navigationStack.filter { it != destination } + destination
        }
    }

    fun navigateBack() {
        if (isMoreMenuOpen) {
            isMoreMenuOpen = false
        } else if (isTenantSwitcherOpen) {
            isTenantSwitcherOpen = false
        } else if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        } else if (currentDestination != MainDestination.ACCOUNTING) {
            navigationStack = listOf(MainDestination.ACCOUNTING)
        }
    }

    val isBackHandlerEnabled =
        isMoreMenuOpen || isTenantSwitcherOpen || navigationStack.size > 1 || currentDestination != MainDestination.ACCOUNTING

    BackHandler(enabled = isBackHandlerEnabled) {
        navigateBack()
    }

    val coroutineScope = rememberCoroutineScope()
    val tenantState by appContainer.tenantRepository.getTenantState()
        .collectAsState(initial = TenantState())
    val syncStatus by appContainer.syncManager.syncState
        .collectAsState()
    val unsyncedCount by appContainer.syncManager.unsyncedCount
        .collectAsState(initial = 0)
    val isDarkMode by appContainer.tenantPreferences.isDarkMode
        .collectAsState(initial = true)
    val layoutDirection = LayoutDirection.Rtl

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            navigateTo(MainDestination.MESSAGES)
        }
    }

    val posViewModel: PosViewModel = viewModel(
        factory = PosViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.productRepository,
            appContainer.cartRepository,
            appContainer.salesOrderRepository,
            appContainer.customerRepository
        )
    )

    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.productRepository
        )
    )

    val customerViewModel: CustomerViewModel = viewModel(
        factory = CustomerViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.customerRepository
        )
    )

    val supplierViewModel: SupplierViewModel = viewModel(
        factory = SupplierViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.supplierRepository
        )
    )

    val purchasesViewModel: PurchasesViewModel = viewModel(
        factory = PurchasesViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.purchaseRepository,
            appContainer.supplierRepository,
            appContainer.productRepository
        )
    )

    val expensesViewModel: ExpensesViewModel = viewModel(
        factory = ExpensesViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.expenseRepository
        )
    )

    val messagesViewModel: MessagesViewModel = viewModel(
        factory = MessagesViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.messageRepository,
            appContainer.salesOrderRepository,
            appContainer.customerRepository,
            appContainer.productRepository,
            appContainer.tenantPreferences
        )
    )

    val salesOrdersViewModel: SalesOrdersViewModel = viewModel(
        factory = SalesOrdersViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.salesOrderRepository
        )
    )

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.ledgerRepository,
            appContainer.productRepository,
            appContainer.supplierRepository,
            appContainer.salesOrderRepository
        )
    )

    val reportsViewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModel.Factory(
            appContainer.tenantRepository,
            appContainer.salesOrderRepository,
            appContainer.productRepository,
            appContainer.expenseRepository,
            appContainer.purchaseRepository,
            appContainer.customerRepository
        )
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                RestoBottomNav(
                    currentDestination = currentDestination,
                    onDestinationSelected = { navigateTo(it) },
                    onMoreClick = { isMoreMenuOpen = true }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        val isForward = targetState.ordinal >= initialState.ordinal
                        (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { width -> if (isForward) width / 4 else -width / 4 })
                            .togetherWith(fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { width -> if (isForward) -width / 4 else width / 4 })
                    },
                    label = "ScreenTransition"
                ) { destination ->
                    when (destination) {
                        MainDestination.ACCOUNTING -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                tenantPreferences = appContainer.tenantPreferences,
                                onNavigateToDestination = { dest, openAddProduct ->
                                    if (openAddProduct) {
                                        inventoryViewModel.openAddProductDialog()
                                    }
                                    navigateTo(dest)
                                }
                            )
                        }

                        MainDestination.REPORTS -> {
                            ReportsScreen(
                                viewModel = reportsViewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.ORDERS -> {
                            SalesOrdersScreen(
                                viewModel = salesOrdersViewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.INVENTORY -> {
                            InventoryScreen(
                                viewModel = inventoryViewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.POS -> {
                            PosScreen(
                                viewModel = posViewModel,
                                onNavigateToTenantSwitch = { isTenantSwitcherOpen = true },
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.MESSAGES -> {
                            MessagesScreen(
                                viewModel = messagesViewModel,
                                initialSharedText = initialSharedText,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.PURCHASES -> {
                            PurchasesScreen(
                                viewModel = purchasesViewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.CUSTOMERS -> {
                            CustomerScreen(
                                viewModel = customerViewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.SUPPLIERS -> {
                            SupplierScreen(
                                viewModel = supplierViewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.EXPENSES -> {
                            ExpensesScreen(
                                viewModel = expensesViewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        MainDestination.SETTINGS -> {
                            SettingsScreen(
                                tenantState = tenantState,
                                syncStatus = syncStatus,
                                unsyncedCount = unsyncedCount,
                                isDarkMode = isDarkMode,
                                onToggleTheme = {
                                    coroutineScope.launch {
                                        appContainer.tenantPreferences.setDarkMode(!isDarkMode)
                                    }
                                },
                                onOpenTenantSwitcher = { isTenantSwitcherOpen = true },
                                onBackClick = { navigateBack() },
                                onLogout = {
                                    coroutineScope.launch {
                                        appContainer.authRepository.logout()
                                    }
                                },
                                onTriggerSync = {
                                    coroutineScope.launch {
                                        appContainer.syncManager.triggerSync(tenantState.activeOrgId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Secondary Modules Bottom Sheet
        if (isMoreMenuOpen) {
            ModalBottomSheet(
                onDismissRequest = { isMoreMenuOpen = false },
                shape = RestoShapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentWindowInsets = { BottomSheetDefaults.windowInsets }
            ) {
                Column(
                    modifier = Modifier
                        .padding(RestoSpacing.lg)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.business_operations_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.business_operations_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(RestoSpacing.md))

                    val secondaryDestinations = listOf(
                        MainDestination.REPORTS,
                        MainDestination.PURCHASES,
                        MainDestination.CUSTOMERS,
                        MainDestination.SUPPLIERS,
                        MainDestination.EXPENSES,
                        MainDestination.MESSAGES,
                        MainDestination.SETTINGS
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(secondaryDestinations) { dest ->
                            val isSelected = currentDestination == dest
                            Card(
                                onClick = {
                                    navigateTo(dest)
                                    isMoreMenuOpen = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RestoShapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(RestoSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                                    Text(
                                        text = dest.titleFa,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Card(
                                onClick = {
                                    isMoreMenuOpen = false
                                    isTenantSwitcherOpen = true
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RestoShapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(RestoSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                                    Text(
                                        text = stringResource(R.string.switch_tenant),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(RestoSpacing.xl))
                }
            }
        }

        if (isTenantSwitcherOpen) {
            TenantSwitcherModal(
                tenantRepository = appContainer.tenantRepository,
                onDismiss = { isTenantSwitcherOpen = false }
            )
        }
    }
}
