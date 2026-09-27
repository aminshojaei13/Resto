package com.braveboy.calcuapp.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.di.AppContainer
import com.braveboy.calcuapp.ui.customer.CustomerScreen
import com.braveboy.calcuapp.ui.customer.CustomerViewModel
import com.braveboy.calcuapp.ui.dashboard.DashboardScreen
import com.braveboy.calcuapp.ui.dashboard.DashboardViewModel
import com.braveboy.calcuapp.ui.expenses.ExpensesScreen
import com.braveboy.calcuapp.ui.expenses.ExpensesViewModel
import com.braveboy.calcuapp.ui.inventory.InventoryScreen
import com.braveboy.calcuapp.ui.inventory.InventoryViewModel
import com.braveboy.calcuapp.ui.messages.MessagesScreen
import com.braveboy.calcuapp.ui.messages.MessagesViewModel
import com.braveboy.calcuapp.ui.orders.SalesOrdersScreen
import com.braveboy.calcuapp.ui.orders.SalesOrdersViewModel
import com.braveboy.calcuapp.ui.pos.PosScreen
import com.braveboy.calcuapp.ui.pos.PosViewModel
import com.braveboy.calcuapp.ui.purchases.PurchasesScreen
import com.braveboy.calcuapp.ui.purchases.PurchasesViewModel
import com.braveboy.calcuapp.ui.supplier.SupplierScreen
import com.braveboy.calcuapp.ui.supplier.SupplierViewModel
import com.braveboy.calcuapp.ui.tenant.TenantSwitcherModal

enum class MainDestination(
    val title: String,
    val titleFa: String,
    val icon: ImageVector
) {
    POS("POS", "فروشگاه", Icons.Rounded.PointOfSale),
    INVENTORY("Inventory", "انبار", Icons.Rounded.Inventory),
    ORDERS("Orders", "سفارشات", Icons.Rounded.ReceiptLong),
    PURCHASES("Purchases", "خرید", Icons.Rounded.ShoppingBag),
    CUSTOMERS("Customers", "مشتریان", Icons.Rounded.People),
    SUPPLIERS("Suppliers", "تامین‌کنندگان", Icons.Rounded.Domain),
    EXPENSES("Expenses", "هزینه‌ها", Icons.Rounded.Payments),
    MESSAGES("Messages", "ورود پیام‌ها", Icons.Rounded.Message),
    ACCOUNTING("Accounting", "حسابداری", Icons.Rounded.AccountBalance)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    initialSharedText: String? = null
) {
    var currentDestination by remember { mutableStateOf(MainDestination.POS) }
    var isTenantSwitcherOpen by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }

    val tenantState by appContainer.tenantRepository.getTenantState().collectAsState(initial = TenantState())
    val isPersian = tenantState.language == "fa"
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            currentDestination = MainDestination.MESSAGES
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
            appContainer.customerRepository
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
            appContainer.ledgerRepository
        )
    )

    val primaryDestinations = listOf(
        MainDestination.POS,
        MainDestination.INVENTORY,
        MainDestination.ORDERS
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    primaryDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = { Icon(imageVector = destination.icon, contentDescription = if (isPersian) destination.titleFa else destination.title) },
                            label = { Text(if (isPersian) destination.titleFa else destination.title) }
                        )
                    }
                    NavigationBarItem(
                        selected = !primaryDestinations.contains(currentDestination),
                        onClick = { isMoreMenuOpen = true },
                        icon = { Icon(imageVector = Icons.Rounded.MoreHoriz, contentDescription = if (isPersian) "سایر بخش‌ها" else "More") },
                        label = { Text(if (isPersian) "سایر بخش‌ها" else "More") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                when (currentDestination) {
                    MainDestination.POS -> {
                        PosScreen(
                            viewModel = posViewModel,
                            onNavigateToTenantSwitch = { isTenantSwitcherOpen = true }
                        )
                    }
                    MainDestination.INVENTORY -> {
                        InventoryScreen(viewModel = inventoryViewModel)
                    }
                    MainDestination.ORDERS -> {
                        SalesOrdersScreen(viewModel = salesOrdersViewModel)
                    }
                    MainDestination.PURCHASES -> {
                        PurchasesScreen(viewModel = purchasesViewModel)
                    }
                    MainDestination.CUSTOMERS -> {
                        CustomerScreen(viewModel = customerViewModel)
                    }
                    MainDestination.SUPPLIERS -> {
                        SupplierScreen(viewModel = supplierViewModel)
                    }
                    MainDestination.EXPENSES -> {
                        ExpensesScreen(viewModel = expensesViewModel)
                    }
                    MainDestination.MESSAGES -> {
                        MessagesScreen(
                            viewModel = messagesViewModel,
                            initialSharedText = initialSharedText
                        )
                    }
                    MainDestination.ACCOUNTING -> {
                        DashboardScreen(viewModel = dashboardViewModel)
                    }
                }
            }
        }

        // Clean Material 3 Modal Drawer / Sheet for Remaining Modules
        if (isMoreMenuOpen) {
            ModalBottomSheet(
                onDismissRequest = { isMoreMenuOpen = false }
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (isPersian) "منوی سایر بخش‌های کسب‌وکار" else "Resto Business Modules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPersian) "برای دسترسی سریع، ماژول مورد نظر را انتخاب کنید" else "Select a module to navigate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val secondaryDestinations = listOf(
                        MainDestination.PURCHASES,
                        MainDestination.CUSTOMERS,
                        MainDestination.SUPPLIERS,
                        MainDestination.EXPENSES,
                        MainDestination.MESSAGES,
                        MainDestination.ACCOUNTING
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(secondaryDestinations) { dest ->
                            val isSelected = currentDestination == dest
                            Card(
                                onClick = {
                                    currentDestination = dest
                                    isMoreMenuOpen = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isPersian) dest.titleFa else dest.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                onClick = {
                                    isMoreMenuOpen = false
                                    isTenantSwitcherOpen = true
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isPersian) "تغییر شعبه / تننت" else "Switch Tenant",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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
