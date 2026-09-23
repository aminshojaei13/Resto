package com.braveboy.calcuapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.di.AppContainer
import com.braveboy.calcuapp.ui.customer.CustomerScreen
import com.braveboy.calcuapp.ui.customer.CustomerViewModel
import com.braveboy.calcuapp.ui.dashboard.DashboardScreen
import com.braveboy.calcuapp.ui.dashboard.DashboardViewModel
import com.braveboy.calcuapp.ui.inventory.InventoryScreen
import com.braveboy.calcuapp.ui.inventory.InventoryViewModel
import com.braveboy.calcuapp.ui.orders.SalesOrdersScreen
import com.braveboy.calcuapp.ui.orders.SalesOrdersViewModel
import com.braveboy.calcuapp.ui.pos.PosScreen
import com.braveboy.calcuapp.ui.pos.PosViewModel
import com.braveboy.calcuapp.ui.tenant.TenantSwitcherModal

enum class MainDestination(
    val title: String,
    val titleFa: String,
    val icon: ImageVector
) {
    POS("POS", "فروشگاه", Icons.Rounded.PointOfSale),
    INVENTORY("Inventory", "انبار", Icons.Rounded.Inventory),
    ORDERS("Orders", "سفارشات", Icons.Rounded.ReceiptLong),
    CUSTOMERS("Customers", "مشتریان", Icons.Rounded.People),
    ACCOUNTING("Accounting", "حسابداری", Icons.Rounded.AccountBalance)
}

@Composable
fun MainAppScreen(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(MainDestination.POS) }
    var isTenantSwitcherOpen by remember { mutableStateOf(false) }

    val tenantState by appContainer.tenantRepository.getTenantState().collectAsState(initial = TenantState())
    val isPersian = tenantState.language == "fa"
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

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

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    MainDestination.values().forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = { Icon(imageVector = destination.icon, contentDescription = if (isPersian) destination.titleFa else destination.title) },
                            label = { Text(if (isPersian) destination.titleFa else destination.title) }
                        )
                    }
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
                    MainDestination.CUSTOMERS -> {
                        CustomerScreen(viewModel = customerViewModel)
                    }
                    MainDestination.ACCOUNTING -> {
                        DashboardScreen(viewModel = dashboardViewModel)
                    }
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
