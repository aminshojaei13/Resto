package com.braveboy.resto.ui.reports

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.ComprehensiveReportData
import com.braveboy.resto.data.model.CustomersReportData
import com.braveboy.resto.data.model.ExpensesReportData
import com.braveboy.resto.data.model.InventoryReportData
import com.braveboy.resto.data.model.PurchasesReportData
import com.braveboy.resto.data.model.ReportTab
import com.braveboy.resto.data.model.ReportTimeFrame
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.data.model.SalesReportData
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter
import com.braveboy.resto.util.ReportExportManager

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val tenantState by viewModel.tenantState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedTimeFrame by viewModel.selectedTimeFrame.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val comprehensiveData by viewModel.comprehensiveData.collectAsState()
    val salesData by viewModel.salesData.collectAsState()
    val inventoryData by viewModel.inventoryData.collectAsState()
    val expensesData by viewModel.expensesData.collectAsState()
    val purchasesData by viewModel.purchasesData.collectAsState()
    val customersData by viewModel.customersData.collectAsState()

    val isPersian = tenantState.language == "fa"
    val currencyUnit = stringResource(R.string.currency_unit)
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = if (isPersian) "گزارش‌ها" else "Reports & Analytics",
                onNavigationClick = onBackClick
            )
        }
    ) { innerPadding ->
        ReportsScreenContent(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            selectedTab = selectedTab,
            selectedTimeFrame = selectedTimeFrame,
            comprehensiveData = comprehensiveData,
            salesData = salesData,
            inventoryData = inventoryData,
            expensesData = expensesData,
            purchasesData = purchasesData,
            customersData = customersData,
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            onSelectTab = { viewModel.selectTab(it) },
            onSelectTimeFrame = { viewModel.selectTimeFrame(it) },
            onExportPdf = {
                viewModel.exportReportToPdf(context) { file ->
                    ReportExportManager.shareFile(context, file, "application/pdf")
                }
            },
            onExportCsv = {
                viewModel.exportReportToCsv(context) { file ->
                    ReportExportManager.shareFile(context, file, "text/csv")
                }
            }
        )
    }
}

@Composable
fun ReportsScreenContent(
    modifier: Modifier = Modifier,
    selectedTab: ReportTab,
    selectedTimeFrame: ReportTimeFrame,
    comprehensiveData: ComprehensiveReportData,
    salesData: SalesReportData,
    inventoryData: InventoryReportData,
    expensesData: ExpensesReportData,
    purchasesData: PurchasesReportData,
    customersData: CustomersReportData,
    isPersian: Boolean = true,
    currencyUnit: String = "تومان",
    onSelectTab: (ReportTab) -> Unit = {},
    onSelectTimeFrame: (ReportTimeFrame) -> Unit = {},
    onExportPdf: () -> Unit = {},
    onExportCsv: () -> Unit = {}
) {
    var timeFrameDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = RestoSpacing.md)
    ) {
        Spacer(modifier = Modifier.height(RestoSpacing.xs))

        // CLEAN COMPACT HEADER: Compact Date Dropdown Pill + Export Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Date Range Dropdown Pill
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { timeFrameDropdownExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) selectedTimeFrame.labelFa else selectedTimeFrame.labelEn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = timeFrameDropdownExpanded,
                    onDismissRequest = { timeFrameDropdownExpanded = false }
                ) {
                    ReportTimeFrame.entries.forEach { tf ->
                        DropdownMenuItem(
                            text = { Text(if (isPersian) tf.labelFa else tf.labelEn) },
                            onClick = {
                                onSelectTimeFrame(tf)
                                timeFrameDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Compact Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onExportPdf() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPersian) "PDF" else "PDF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onExportCsv() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPersian) "اکسل" else "Excel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // UNCLUTTERED SINGLE SEGMENTED TABS BAR
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ReportTab.entries.size) { idx ->
                val tab = ReportTab.entries[idx]
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isPersian) tab.labelFa else tab.labelEn,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CONTENT TAB VIEW
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            modifier = Modifier.weight(1f)
        ) {
            when (selectedTab) {
                ReportTab.COMPREHENSIVE -> {
                    item {
                        ComprehensiveReportView(
                            data = comprehensiveData,
                            salesData = salesData,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit
                        )
                    }
                }

                ReportTab.SALES -> {
                    item {
                        SalesReportView(
                            data = salesData,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit
                        )
                    }
                }

                ReportTab.INVENTORY -> {
                    item {
                        InventoryReportView(
                            data = inventoryData,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit
                        )
                    }
                }

                ReportTab.EXPENSES -> {
                    item {
                        ExpensesReportView(
                            data = expensesData,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit
                        )
                    }
                }

                ReportTab.PURCHASES -> {
                    item {
                        PurchasesReportView(
                            data = purchasesData,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit
                        )
                    }
                }

                ReportTab.CUSTOMERS -> {
                    item {
                        CustomersReportView(
                            data = customersData,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ComprehensiveReportView(
    data: ComprehensiveReportData,
    salesData: SalesReportData = SalesReportData(),
    isPersian: Boolean,
    currencyUnit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Net Profit Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPersian) "سود خالص عملیاتی" else "Net Operating Profit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(RestoTheme.colors.successContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = RestoTheme.colors.success,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPersian) "سوددهی مثبت" else "+18%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = RestoTheme.colors.onSuccessContainer
                            )
                        }
                    }
                }

                Text(
                    text = if (isPersian) "${PersianFormatter.formatTomans(data.netProfit)} $currencyUnit" else "$${
                        String.format(
                            "%.2f",
                            data.netProfit
                        )
                    }",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${if (isPersian) "تعداد کل سفارشات:" else "Total Orders:"} ${
                            if (isPersian) PersianFormatter.toPersianDigits(
                                data.totalOrdersCount
                            ) else data.totalOrdersCount
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${if (isPersian) "میانگین فاکتور:" else "Avg Factor:"} ${
                            if (isPersian) PersianFormatter.formatTomans(
                                data.averageOrderValue
                            ) else String.format("%.2f", data.averageOrderValue)
                        } $currencyUnit",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Metrics Grid Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = if (isPersian) "فروش کل" else "Total Sales",
                value = if (isPersian) "${PersianFormatter.formatTomans(data.totalRevenue)} $currencyUnit" else "$${
                    String.format(
                        "%.2f",
                        data.totalRevenue
                    )
                }",
                icon = Icons.Rounded.ArrowUpward,
                iconBgColor = RestoTheme.colors.successContainer,
                iconTint = RestoTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (isPersian) "سود ناخالص" else "Gross Profit",
                value = if (isPersian) "${PersianFormatter.formatTomans(data.grossProfit)} $currencyUnit" else "$${
                    String.format(
                        "%.2f",
                        data.grossProfit
                    )
                }",
                icon = Icons.Rounded.PieChart,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = if (isPersian) "قیمت تمام‌شده" else "Total COGS",
                value = if (isPersian) "${PersianFormatter.formatTomans(data.totalCogs)} $currencyUnit" else "$${
                    String.format(
                        "%.2f",
                        data.totalCogs
                    )
                }",
                icon = Icons.Rounded.Inventory2,
                iconBgColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (isPersian) "هزینه‌های جاری" else "Expenses",
                value = if (isPersian) "${PersianFormatter.formatTomans(data.totalExpenses)} $currencyUnit" else "$${
                    String.format(
                        "%.2f",
                        data.totalExpenses
                    )
                }",
                icon = Icons.Rounded.ArrowDownward,
                iconBgColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        // VISUAL DISTRIBUTION BARS FOR SALES CHANNELS (COMBINED FROM PREVIEW B)
        if (salesData.channelBreakdown.isNotEmpty()) {
            Text(
                if (isPersian) "سهم کانال‌های فروش (توزیع بصری درصدی)" else "Sales By Channel Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            RestoCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val totalRevenue = salesData.totalRevenue.coerceAtLeast(1.0)
                    salesData.channelBreakdown.forEach { (ch, amt) ->
                        val ratio = (amt / totalRevenue).toFloat().coerceIn(0f, 1f)
                        val percentInt = (ratio * 100).toInt()

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$ch (${
                                        if (isPersian) PersianFormatter.toPersianDigits(
                                            percentInt
                                        ) else percentInt
                                    }٪)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isPersian) "${PersianFormatter.formatTomans(amt)} $currencyUnit" else "$${
                                        String.format(
                                            "%.2f",
                                            amt
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }
                }
            }
        }

        // UNCLUTTERED FINANCIAL CARDS (2-Line Separated Layout)
        Text(
            text = if (isPersian) "محاسبات خلوت و مجزای سود و زیان (P&L)" else "P&L Statement Calculation Cards",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SpaciousFinancialCard(
                title = "۱. درآمد کل حاصل از فروش",
                amount = data.totalRevenue,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                tagColor = RestoTheme.colors.success
            )

            SpaciousFinancialCard(
                title = "۲. قیمت تمام‌شده کالاهای فروخته‌شده",
                amount = -data.totalCogs,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isNegative = true
            )

            SpaciousFinancialCard(
                title = "۳. سود ناخالص محاسبه‌شده",
                amount = data.grossProfit,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isHighlight = true
            )

            SpaciousFinancialCard(
                title = "۴. مجموع هزینه‌های جاری و عملیاتی",
                amount = -data.totalExpenses,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isNegative = true
            )

            SpaciousFinancialCard(
                title = "۵. سود خالص نهایی عملیاتی",
                amount = data.netProfit,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isHighlight = true,
                isFinalResult = true
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun SpaciousFinancialCard(
    title: String,
    amount: Double,
    isPersian: Boolean,
    currencyUnit: String,
    isNegative: Boolean = false,
    isHighlight: Boolean = false,
    isFinalResult: Boolean = false,
    tagColor: Color? = null
) {
    val bgColor = when {
        isFinalResult -> MaterialTheme.colorScheme.primaryContainer
        isHighlight -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isFinalResult) 2.dp else 1.dp,
                color = if (isFinalResult) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFinalResult || isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isFinalResult) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val amtStr = if (isPersian) {
                    "${if (isNegative && amount < 0) "-" else ""}${
                        PersianFormatter.formatTomans(
                            kotlin.math.abs(amount)
                        )
                    } $currencyUnit"
                } else {
                    "${if (isNegative && amount < 0) "-" else ""}$${
                        String.format(
                            "%.2f",
                            kotlin.math.abs(amount)
                        )
                    }"
                }

                Text(
                    text = amtStr,
                    style = if (isFinalResult) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isFinalResult -> MaterialTheme.colorScheme.primary
                        isNegative -> MaterialTheme.colorScheme.error
                        tagColor != null -> tagColor
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (isFinalResult) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPersian) "نتیجه نهایی" else "Final Net",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun SalesReportView(
    data: SalesReportData,
    isPersian: Boolean,
    currencyUnit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = if (isPersian) "فروش کل" else "Total Sales",
                value = if (isPersian) "${PersianFormatter.formatTomans(data.totalRevenue)} $currencyUnit" else "$${
                    String.format(
                        "%.2f",
                        data.totalRevenue
                    )
                }",
                icon = Icons.Rounded.ArrowUpward,
                iconBgColor = RestoTheme.colors.successContainer,
                iconTint = RestoTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (isPersian) "تعداد فاکتورها" else "Total Orders",
                value = if (isPersian) PersianFormatter.toPersianDigits(data.totalOrdersCount) else "${data.totalOrdersCount}",
                icon = Icons.Rounded.AccountBalanceWallet,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        // VISUAL DISTRIBUTION BARS FOR SALES CHANNELS
        if (data.channelBreakdown.isNotEmpty()) {
            Text(
                if (isPersian) "سهم کانال‌های فروش (توزیع درصدی)" else "Sales By Channel Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            RestoCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val totalRevenue = data.totalRevenue.coerceAtLeast(1.0)
                    data.channelBreakdown.forEach { (ch, amt) ->
                        val ratio = (amt / totalRevenue).toFloat().coerceIn(0f, 1f)
                        val percentInt = (ratio * 100).toInt()

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$ch (${
                                        if (isPersian) PersianFormatter.toPersianDigits(
                                            percentInt
                                        ) else percentInt
                                    }٪)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isPersian) "${PersianFormatter.formatTomans(amt)} $currencyUnit" else "$${
                                        String.format(
                                            "%.2f",
                                            amt
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }
                }
            }
        }

        // Table List
        Text(
            if (isPersian) "لیست فاکتورهای اخیر فروش" else "Orders List",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (data.orders.isEmpty()) {
            Text(
                if (isPersian) "سفارشی در این بازه یافت نشد" else "No orders in this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.orders.take(10).forEach { o ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    o.orderNumber,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "${o.customerName} • ${SalesChannel.fromCode(o.source).titleFa}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                if (isPersian) "${PersianFormatter.formatTomans(o.totalAmount)} $currencyUnit" else "$${
                                    String.format(
                                        "%.2f",
                                        o.totalAmount
                                    )
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun InventoryReportView(
    data: InventoryReportData,
    isPersian: Boolean,
    currencyUnit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = if (isPersian) "ارزش دارایی انبار" else "Total Asset",
                value = if (isPersian) "${PersianFormatter.formatTomans(data.totalInventoryValue)} $currencyUnit" else "$${
                    String.format(
                        "%.2f",
                        data.totalInventoryValue
                    )
                }",
                icon = Icons.Rounded.Inventory2,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (isPersian) "تعداد فیزیکی موجودی" else "Total Units",
                value = if (isPersian) PersianFormatter.toPersianDigits(data.totalPhysicalStockUnits) else "${data.totalPhysicalStockUnits}",
                icon = Icons.Rounded.PieChart,
                iconBgColor = RestoTheme.colors.successContainer,
                iconTint = RestoTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            if (isPersian) "ارزش ریالی موجودی کالاها" else "Inventory Items Asset Value",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.products.take(10).forEach { p ->
                val stock = p.getTotalStock()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "کد: ${p.sku} | موجودی: ${
                                    if (isPersian) PersianFormatter.toPersianDigits(
                                        stock
                                    ) else stock
                                } ${p.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (isPersian) "${PersianFormatter.formatTomans(stock * p.costPrice)} $currencyUnit" else "$${
                                String.format(
                                    "%.2f",
                                    stock * p.costPrice
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ExpensesReportView(
    data: ExpensesReportData,
    isPersian: Boolean,
    currencyUnit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MetricCard(
            title = if (isPersian) "مجموع هزینه‌های جاری" else "Total Expenses",
            value = if (isPersian) "${PersianFormatter.formatTomans(data.totalExpenses)} $currencyUnit" else "$${
                String.format(
                    "%.2f",
                    data.totalExpenses
                )
            }",
            icon = Icons.Rounded.ArrowDownward,
            iconBgColor = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.error
        )

        Text(
            if (isPersian) "لیست هزینه‌ها" else "Expenses List",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.expenses.take(10).forEach { e ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                e.category,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                e.notes.ifBlank { "-" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (isPersian) "${PersianFormatter.formatTomans(e.amount)} $currencyUnit" else "$${
                                String.format(
                                    "%.2f",
                                    e.amount
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun PurchasesReportView(
    data: PurchasesReportData,
    isPersian: Boolean,
    currencyUnit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MetricCard(
            title = if (isPersian) "مجموع خریدهای انبار" else "Total Purchases",
            value = if (isPersian) "${PersianFormatter.formatTomans(data.totalPurchasesValue)} $currencyUnit" else "$${
                String.format(
                    "%.2f",
                    data.totalPurchasesValue
                )
            }",
            icon = Icons.Rounded.Inventory2,
            iconBgColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.primary
        )

        Text(
            if (isPersian) "فاکتورهای خرید" else "Purchase Orders",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.purchases.take(10).forEach { p ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                p.purchaseNumber,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "تامین‌کننده: ${p.supplierName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (isPersian) "${PersianFormatter.formatTomans(p.totalAmount)} $currencyUnit" else "$${
                                String.format(
                                    "%.2f",
                                    p.totalAmount
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun CustomersReportView(
    data: CustomersReportData,
    isPersian: Boolean,
    currencyUnit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = if (isPersian) "حجم خرید مشتریان" else "Total Customer Spend",
                value = if (isPersian) "${PersianFormatter.formatTomans(data.totalCustomerSpentValue)} $currencyUnit" else "$${
                    String.format(
                        "%.2f",
                        data.totalCustomerSpentValue
                    )
                }",
                icon = Icons.Rounded.AccountBalanceWallet,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (isPersian) "تعداد کل مشتریان" else "Total Customers",
                value = if (isPersian) PersianFormatter.toPersianDigits(data.totalCustomersCount) else "${data.totalCustomersCount}",
                icon = Icons.Rounded.PieChart,
                iconBgColor = RestoTheme.colors.infoContainer,
                iconTint = RestoTheme.colors.info,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            if (isPersian) "پرفروش‌ترین مشتریان" else "Top Customers",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.customers.sortedByDescending { it.totalPurchases }.take(10).forEach { c ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                c.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                c.phone.ifBlank { "-" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (isPersian) "${PersianFormatter.formatTomans(c.totalPurchases)} $currencyUnit" else "$${
                                String.format(
                                    "%.2f",
                                    c.totalPurchases
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "Combined Perfect Preview", showBackground = true, widthDp = 380, heightDp = 950)
@Composable
fun ReportsCombinedPreview() {
    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ReportsScreenContent(
                selectedTab = ReportTab.COMPREHENSIVE,
                selectedTimeFrame = ReportTimeFrame.THIS_MONTH,
                comprehensiveData = ComprehensiveReportData(
                    totalRevenue = 325000000.0,
                    totalCogs = 180000000.0,
                    grossProfit = 145000000.0,
                    totalExpenses = 25000000.0,
                    netProfit = 120000000.0,
                    totalOrdersCount = 142,
                    averageOrderValue = 2288732.0
                ),
                salesData = SalesReportData(
                    totalRevenue = 325000000.0,
                    totalOrdersCount = 142,
                    averageOrderValue = 2288732.0,
                    channelBreakdown = mapOf(
                        "حضوری (صندوق)" to 180000000.0,
                        "اینستاگرام" to 95000000.0,
                        "سفارش تلفنی" to 50000000.0
                    )
                ),
                inventoryData = InventoryReportData(),
                expensesData = ExpensesReportData(),
                purchasesData = PurchasesReportData(),
                customersData = CustomersReportData(),
                isPersian = true,
                currencyUnit = "تومان"
            )
        }
    }
}
