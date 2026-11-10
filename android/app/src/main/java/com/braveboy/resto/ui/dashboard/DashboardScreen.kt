package com.braveboy.resto.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.LedgerCategory
import com.braveboy.resto.data.model.LedgerEntry
import com.braveboy.resto.data.model.LedgerType
import com.braveboy.resto.data.model.MetricsSummary
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.ui.MainDestination
import com.braveboy.resto.ui.components.RestoButton
import com.braveboy.resto.ui.components.RestoButtonVariant
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.dashboard.components.DashboardFinancialActivity
import com.braveboy.resto.ui.dashboard.components.DashboardHeader
import com.braveboy.resto.ui.dashboard.components.DashboardQuickActions
import com.braveboy.resto.ui.dashboard.components.DashboardRecentOrders
import com.braveboy.resto.ui.dashboard.components.DashboardRevenueCard
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.ui.dashboard.components.DashboardStatCardsRow
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

import com.braveboy.resto.data.local.datastore.TenantPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    tenantPreferences: TenantPreferences? = null,
    onNavigateToDestination: (destination: MainDestination, openAddProduct: Boolean) -> Unit = { _, _ -> }
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val metrics by viewModel.metricsSummary.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val recentSalesOrders by viewModel.recentSalesOrders.collectAsState()
    val isRecordEntryDialogOpen by viewModel.isRecordEntryDialogOpen.collectAsState()
    val snackBarMessage by viewModel.snackbarMessage.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val isDarkModeState = tenantPreferences?.isDarkMode?.collectAsState(initial = null)
    val isDarkMode = isDarkModeState?.value ?: isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    DashboardScreenContent(
        tenantState = tenantState,
        metrics = metrics,
        ledgerEntries = ledgerEntries,
        recentSalesOrders = recentSalesOrders,
        isRecordEntryDialogOpen = isRecordEntryDialogOpen,
        snackBarMessage = snackBarMessage,
        isSaving = isSaving,
        isDarkMode = isDarkMode,
        onToggleTheme = {
            scope.launch {
                tenantPreferences?.setDarkMode(!isDarkMode)
            }
        },
        modifier = modifier,
        onNavigateToDestination = onNavigateToDestination,
        onClearSnackbarMessage = { viewModel.clearSnackbarMessage() },
        onOpenRecordEntryDialog = { viewModel.openRecordEntryDialog() },
        onCloseRecordEntryDialog = { viewModel.closeRecordEntryDialog() },
        onRecordLedgerEntry = { type, category, amount, description ->
            viewModel.recordLedgerEntry(type, category, amount, description)
        }
    )
}

@SuppressLint("DefaultLocale")
@Composable
fun DashboardScreenContent(
    modifier: Modifier = Modifier,
    tenantState: TenantState,
    metrics: MetricsSummary,
    ledgerEntries: List<LedgerEntry>,
    recentSalesOrders: List<SalesOrder> = emptyList(),
    isRecordEntryDialogOpen: Boolean,
    snackBarMessage: String?,
    isSaving: Boolean = false,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onNavigateToDestination: (destination: MainDestination, openAddProduct: Boolean) -> Unit = { _, _ -> },
    onClearSnackbarMessage: () -> Unit = {},
    onOpenRecordEntryDialog: () -> Unit = {},
    onCloseRecordEntryDialog: () -> Unit = {},
    onRecordLedgerEntry: (type: LedgerType, category: LedgerCategory, amount: Double, description: String) -> Unit = { _, _, _, _ -> }
) {
    val currencyUnit = stringResource(R.string.currency_unit)
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackBarMessage) {
        snackBarMessage?.let { msg ->
            snackBarHostState.showSnackbar(msg)
            onClearSnackbarMessage()
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackBarHostState) },
    ) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardHeader(
                    userName = tenantState.userName,
                    userAvatarUrl = tenantState.userAvatarUrl,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme,
                    onProfileClick = {
                        onNavigateToDestination(MainDestination.SETTINGS, false)
                    }
                )
            }

            item {
                if (metrics.lowStockCount > 0) {
                    RestoCard(
                        containerColor = RestoTheme.colors.warningContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = RestoTheme.colors.warning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(RestoSpacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.low_stock_warning_banner),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RestoTheme.colors.warning
                                )
                                Text(
                                    text = "${PersianFormatter.toPersianDigits(metrics.lowStockCount)} کالا رو به اتمام است؛ نیاز به شارژ موجودی دارید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                val formattedNetProfit = "${PersianFormatter.formatTomans(metrics.netProfit)} $currencyUnit"
                val displayOrdersCount = if (metrics.todaySalesCount > 0) metrics.todaySalesCount else metrics.totalSalesCount
                val formattedOrdersCount = PersianFormatter.toPersianDigits(displayOrdersCount)

                val isNetProfitPositive = metrics.netProfitTrendPercent >= 0
                val isTotalOrdersPositive = metrics.ordersTrendPercent >= 0

                val ordersTrendStr = formatTrendPercentage(metrics.ordersTrendPercent)
                val netProfitTrendStr = formatTrendPercentage(metrics.netProfitTrendPercent)

                DashboardStatCardsRow(
                    netProfitValue = formattedNetProfit,
                    netProfitTrend = netProfitTrendStr,
                    isNetProfitPositive = isNetProfitPositive,
                    totalOrdersValue = formattedOrdersCount,
                    totalOrdersTrend = ordersTrendStr,
                    isTotalOrdersPositive = isTotalOrdersPositive
                )
            }

            // 3. Total Revenue Card (Dark Forest Green)
            item {
                val displayRevenue = if (metrics.totalRevenue > 0) metrics.totalRevenue else metrics.todayRevenue
                val formattedRevenue = "${PersianFormatter.formatTomans(displayRevenue)} $currencyUnit"
                val revenueTrendStr = formatTrendPercentage(metrics.revenueTrendPercent)

                DashboardRevenueCard(
                    revenueValue = formattedRevenue,
                    trendText = revenueTrendStr,
                    isPositiveTrend = metrics.revenueTrendPercent >= 0
                )
            }

            // 4. Quick Actions
            item {
                DashboardQuickActions(
                    onNewManualOrderClick = {
                        onNavigateToDestination(MainDestination.POS, false)
                    },
                    onAddProductClick = {
                        onNavigateToDestination(MainDestination.INVENTORY, true)
                    },
                    onImportDropshippingClick = {
                        onNavigateToDestination(MainDestination.MESSAGES, false)
                    },
                    onViewOrdersClick = {
                        onNavigateToDestination(MainDestination.ORDERS, false)
                    },
                    onWithdrawMoneyClick = onOpenRecordEntryDialog
                )
            }

            // 5. Recent Orders List
            item {
                DashboardRecentOrders(
                    orders = recentSalesOrders.take(5),
                    currencySymbol = currencyUnit,
                    onSeeAllClick = {
                        onNavigateToDestination(MainDestination.ORDERS, false)
                    },
                    onOrderClick = {
                        onNavigateToDestination(MainDestination.ORDERS, false)
                    }
                )
            }

            // 6. Financial Activity Section
            item {
                DashboardFinancialActivity(
                    ledgerEntries = ledgerEntries,
                    currencyUnit = currencyUnit,
                    onNewEntryClick = onOpenRecordEntryDialog
                )
            }
        }
    }

    // Modern M3 Record Entry Dialog
    if (isRecordEntryDialogOpen) {
        ModernRecordEntryDialog(
            onSubmit = onRecordLedgerEntry,
            onDismiss = onCloseRecordEntryDialog,
            currencyUnit = currencyUnit,
            confirmLoading = isSaving
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernRecordEntryDialog(
    onSubmit: (type: LedgerType, category: LedgerCategory, amount: Double, description: String) -> Unit,
    onDismiss: () -> Unit,
    currencyUnit: String,
    confirmLoading: Boolean = false
) {
    var selectedType by remember { mutableStateOf(LedgerType.DEBIT) }
    var selectedCategory by remember { mutableStateOf(LedgerCategory.EXPENSE) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    val categories = LedgerCategory.entries

    RestoDialog(
        title = stringResource(R.string.record_transaction_title),
        confirmText = stringResource(R.string.record_final_btn),
        confirmLoading = confirmLoading,
        onConfirm = {
            val amt = PersianFormatter.parsePrice(amountText)
            if (amt > 0 && descriptionText.isNotBlank()) {
                onSubmit(selectedType, selectedCategory, amt, descriptionText.trim())
            }
        },
        confirmEnabled = PersianFormatter.parsePrice(amountText) > 0 && descriptionText.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)) {
            // Type Segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoButton(
                    text = stringResource(R.string.income_inflow),
                    onClick = {
                        selectedType = LedgerType.CREDIT
                        selectedCategory = LedgerCategory.CASH_IN
                    },
                    variant = if (selectedType == LedgerType.CREDIT) RestoButtonVariant.Primary else RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = stringResource(R.string.expense_outflow),
                    onClick = {
                        selectedType = LedgerType.DEBIT
                        selectedCategory = LedgerCategory.EXPENSE
                    },
                    variant = if (selectedType == LedgerType.DEBIT) RestoButtonVariant.Destructive else RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )
            }

            // Category Picker
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                val categoryName = when (selectedCategory) {
                    LedgerCategory.SALES -> stringResource(R.string.sales_income_cat)
                    LedgerCategory.INITIAL_STOCK -> stringResource(R.string.initial_stock_cat)
                    LedgerCategory.INVENTORY_ADJUSTMENT -> stringResource(R.string.stock_adjustment_cat)
                    LedgerCategory.EXPENSE -> stringResource(R.string.operating_expense_cat)
                    LedgerCategory.CASH_IN -> stringResource(R.string.cash_in_cat)
                    LedgerCategory.CASH_OUT -> stringResource(R.string.cash_out_cat)
                    LedgerCategory.REFUND -> stringResource(R.string.customer_refund_cat)
                }

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    shape = RestoShapes.medium,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        val catLabel = when (cat) {
                            LedgerCategory.SALES -> stringResource(R.string.sales_income_cat)
                            LedgerCategory.INITIAL_STOCK -> stringResource(R.string.initial_stock_cat)
                            LedgerCategory.INVENTORY_ADJUSTMENT -> stringResource(R.string.stock_adjustment_cat)
                            LedgerCategory.EXPENSE -> stringResource(R.string.operating_expense_cat)
                            LedgerCategory.CASH_IN -> stringResource(R.string.cash_in_cat)
                            LedgerCategory.CASH_OUT -> stringResource(R.string.cash_out_cat)
                            LedgerCategory.REFUND -> stringResource(R.string.customer_refund_cat)
                        }
                        DropdownMenuItem(
                            text = { Text(catLabel) },
                            onClick = {
                                selectedCategory = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount Input
            RestoTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                label = "${stringResource(R.string.expense_amount_label)} ($currencyUnit)",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                placeholder = "0"
            )

            // Description Input
            RestoTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = stringResource(R.string.notes_description_label),
                placeholder = stringResource(R.string.office_bill_placeholder),
                singleLine = false,
                maxLines = 3
            )
        }
    }
}

private fun formatTrendPercentage(percent: Double): String {
    val rounded = Math.round(percent)
    val absVal = PersianFormatter.toPersianDigits(Math.abs(rounded).toInt())
    return if (rounded > 0) "$absVal٪" else if (rounded < 0) "$absVal٪" else "۰٪"
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    RestoTheme {
        DashboardScreenContent(
            tenantState = TenantState(
                userName = "ایران علوی",
                language = "fa"
            ),
            metrics = MetricsSummary(
                todayRevenue = 0.0,
                todaySalesCount = 0,
                netProfit = 0.0,
                lowStockCount = 0
            ),
            ledgerEntries = emptyList(),
            isRecordEntryDialogOpen = false,
            snackBarMessage = null
        )
    }
}
