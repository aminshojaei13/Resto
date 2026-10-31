package com.braveboy.calcuapp.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.ui.theme.CalcuappTheme

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
    recentOrders: List<DashboardOrderItem> = listOf(
        DashboardOrderItem(
            orderNumber = "Order-12567",
            amount = "۱۶۹,۰۰۰ تومان",
            isLikelyFake = true,
            paymentStatus = "Unpaid",
            orderTag = "New"
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
    onOrderClick: (DashboardOrderItem) -> Unit = {},
    onNewEntryClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
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
    CalcuappTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            DashboardRedesignView()
        }
    }
}
