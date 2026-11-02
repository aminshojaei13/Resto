package com.braveboy.resto.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.resto.data.model.LedgerEntry
import com.braveboy.resto.data.model.OrderItem
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.ui.theme.RestoTheme

@Composable
fun DashboardRedesignView(
    userName: String = "ایران علوی",
    notificationCount: Int = 1,
    netProfitValue: String = "۰ تومان",
    netProfitTrend: String = "+12%",
    totalOrdersValue: String = "۱,۵۰۰",
    totalOrdersTrend: String = "-5%",
    revenueValue: String = "۳۲,۴۵۰,۰۰۰ تومان",
    revenueTrend: String = "+8%",
    recentOrders: List<SalesOrder> = listOf(
        SalesOrder(
            orderNumber = "ORD-2026-1001",
            orgId = "org1",
            storeId = "store1",
            warehouseId = "wh1",
            customerName = "رضا احمدی",
            items = listOf(
                OrderItem(
                    productId = "p1",
                    productName = "پیتزا مخصوص",
                    sku = "SKU-1",
                    unitPrice = 120000.0,
                    quantity = 2,
                    discountPercent = 0.0,
                    taxAmount = 0.0,
                    totalPrice = 240000.0
                )
            ),
            subtotal = 240000.0,
            discountAmount = 0.0,
            taxAmount = 0.0,
            totalAmount = 240000.0,
            paymentMethod = PaymentMethod.CASH
        )
    ),
    ledgerEntries: List<LedgerEntry> = emptyList(),
    isPersian: Boolean = true,
    currencyUnit: String = "تومان",
    onAddProductClick: () -> Unit = {},
    onImportDropshippingClick: () -> Unit = {},
    onViewOrdersClick: () -> Unit = {},
    onWithdrawMoneyClick: () -> Unit = {},
    onSeeAllOrdersClick: () -> Unit = {},
    onOrderClick: (SalesOrder) -> Unit = {},
    onNewEntryClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            DashboardHeader(
                userName = userName,
                notificationCount = notificationCount,
                isPersian = isPersian,
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick
            )

            // Top Stat Cards (Net Profit & Total Orders)
            DashboardStatCardsRow(
                netProfitValue = netProfitValue,
                netProfitTrend = netProfitTrend,
                totalOrdersValue = totalOrdersValue,
                totalOrdersTrend = totalOrdersTrend,
                isPersian = isPersian
            )

            // Total Revenue Dark Green Card
            DashboardRevenueCard(
                revenueValue = revenueValue,
                trendText = revenueTrend,
                isPersian = isPersian
            )

            // Quick Actions List
            DashboardQuickActions(
                isPersian = isPersian,
                onAddProductClick = onAddProductClick,
                onImportDropshippingClick = onImportDropshippingClick,
                onViewOrdersClick = onViewOrdersClick,
                onWithdrawMoneyClick = onWithdrawMoneyClick
            )

            // Recent Orders List
            DashboardRecentOrders(
                orders = recentOrders,
                isPersian = isPersian,
                onSeeAllClick = onSeeAllOrdersClick,
                onOrderClick = onOrderClick
            )

            // Redesigned Financial Activity Section
            DashboardFinancialActivity(
                ledgerEntries = ledgerEntries,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                onNewEntryClick = onNewEntryClick
            )
        }
    }
}

@Preview(name = "Dashboard Redesign Persian RTL Preview", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun DashboardRedesignPreview() {
    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            DashboardRedesignView()
        }
    }
}
