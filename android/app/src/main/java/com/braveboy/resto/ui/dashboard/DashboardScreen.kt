package com.braveboy.resto.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.braveboy.resto.ui.dashboard.components.DashboardOrderItem
import com.braveboy.resto.ui.dashboard.components.DashboardQuickActions
import com.braveboy.resto.ui.dashboard.components.DashboardRecentOrders
import com.braveboy.resto.ui.dashboard.components.DashboardRevenueCard
import com.braveboy.resto.ui.dashboard.components.DashboardStatCardsRow
import com.braveboy.resto.ui.dashboard.components.OnboardingChecklistCard
import com.braveboy.resto.ui.dashboard.components.OnboardingWelcomeDialog
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (destination: MainDestination, openAddProduct: Boolean) -> Unit = { _, _ -> }
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val metrics by viewModel.metricsSummary.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val onboardingState by viewModel.onboardingState.collectAsState()
    val showWelcomeDialog by viewModel.showWelcomeDialog.collectAsState()
    val isRecordEntryDialogOpen by viewModel.isRecordEntryDialogOpen.collectAsState()
    val snackBarMessage by viewModel.snackbarMessage.collectAsState()

    DashboardScreenContent(
        tenantState = tenantState,
        metrics = metrics,
        ledgerEntries = ledgerEntries,
        onboardingState = onboardingState,
        showWelcomeDialog = showWelcomeDialog,
        isRecordEntryDialogOpen = isRecordEntryDialogOpen,
        snackBarMessage = snackBarMessage,
        modifier = modifier,
        onNavigateToDestination = onNavigateToDestination,
        onClearSnackbarMessage = { viewModel.clearSnackbarMessage() },
        onOpenRecordEntryDialog = { viewModel.openRecordEntryDialog() },
        onCloseRecordEntryDialog = { viewModel.closeRecordEntryDialog() },
        onRecordLedgerEntry = { type, category, amount, description ->
            viewModel.recordLedgerEntry(type, category, amount, description)
        },
        onSkipSupplierStep = { viewModel.skipSupplierStep() },
        onDismissOnboarding = { viewModel.dismissOnboarding() },
        onOpenWelcomeDialog = { viewModel.openWelcomeDialog() },
        onDismissWelcomeDialog = { permanently -> viewModel.dismissWelcomeDialog(permanently) }
    )
}

@SuppressLint("DefaultLocale")
@Composable
fun DashboardScreenContent(
    tenantState: TenantState,
    metrics: MetricsSummary,
    ledgerEntries: List<LedgerEntry>,
    onboardingState: OnboardingState,
    showWelcomeDialog: Boolean,
    isRecordEntryDialogOpen: Boolean,
    snackBarMessage: String?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (destination: MainDestination, openAddProduct: Boolean) -> Unit = { _, _ -> },
    onClearSnackbarMessage: () -> Unit = {},
    onOpenRecordEntryDialog: () -> Unit = {},
    onCloseRecordEntryDialog: () -> Unit = {},
    onRecordLedgerEntry: (type: LedgerType, category: LedgerCategory, amount: Double, description: String) -> Unit = { _, _, _, _ -> },
    onSkipSupplierStep: () -> Unit = {},
    onDismissOnboarding: () -> Unit = {},
    onOpenWelcomeDialog: () -> Unit = {},
    onDismissWelcomeDialog: (permanently: Boolean) -> Unit = {}
) {
    val isPersian = tenantState.language == "fa"
    val currencyUnit = if (isPersian) "تومان" else "$"
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackBarMessage) {
        snackBarMessage?.let { msg ->
            snackBarHostState.showSnackbar(msg)
            onClearSnackbarMessage()
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenRecordEntryDialog,
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = if (isPersian) "ثبت سند مالی" else "Record Entry"
                )
            }
        }
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
                    notificationCount = metrics.lowStockCount,
                    isPersian = isPersian,
                    onNotificationClick = {
                        if (metrics.lowStockCount > 0) {
                            onNavigateToDestination(MainDestination.INVENTORY, false)
                        } else {
                            onNavigateToDestination(MainDestination.MESSAGES, false)
                        }
                    },
                    onProfileClick = {
                        onNavigateToDestination(MainDestination.SETTINGS, false)
                    }
                )
            }

            item {
                if (!onboardingState.isDismissed) {
                    OnboardingChecklistCard(
                        state = onboardingState,
                        isPersian = isPersian,
                        onNavigate = onNavigateToDestination,
                        onSkipSupplierStep = onSkipSupplierStep,
                        onDismiss = onDismissOnboarding,
                        onShowWelcomeDialog = onOpenWelcomeDialog
                    )
                }
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
                                    text = if (isPersian) "هشدار موجودی کالا" else "Low Stock Alert",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RestoTheme.colors.warning
                                )
                                Text(
                                    text = if (isPersian)
                                        "${PersianFormatter.toPersianDigits(metrics.lowStockCount)} کالا رو به اتمام است؛ نیاز به شارژ موجودی دارید."
                                    else
                                        "${metrics.lowStockCount} products are running low on stock.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Stat Cards Row (Net Profit & Total Orders)
            item {
                val formattedNetProfit = if (isPersian) {
                    "${PersianFormatter.formatTomans(metrics.netProfit)} $currencyUnit"
                } else {
                    "$${String.format("%.2f", metrics.netProfit)}"
                }

                val formattedOrdersCount = if (isPersian) {
                    PersianFormatter.toPersianDigits(metrics.todaySalesCount)
                } else {
                    metrics.todaySalesCount.toString()
                }

                DashboardStatCardsRow(
                    netProfitValue = formattedNetProfit,
                    netProfitTrend = if (metrics.netProfit > 0) "+12%" else "۰٪",
                    totalOrdersValue = formattedOrdersCount,
                    totalOrdersTrend = if (metrics.todaySalesCount > 0) "+5%" else "۰٪",
                    isPersian = isPersian
                )
            }

            // 3. Total Revenue Card (Dark Forest Green)
            item {
                val formattedTodayRevenue = if (isPersian) {
                    "${PersianFormatter.formatTomans(metrics.todayRevenue)} $currencyUnit"
                } else {
                    "$${String.format("%.2f", metrics.todayRevenue)}"
                }

                DashboardRevenueCard(
                    revenueValue = formattedTodayRevenue,
                    trendText = if (metrics.todayRevenue > 0) "+8%" else "۰٪",
                    isPersian = isPersian
                )
            }

            // 4. Quick Actions
            item {
                DashboardQuickActions(
                    isPersian = isPersian,
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
                val recentOrdersList = if (ledgerEntries.isNotEmpty()) {
                    ledgerEntries.take(3).map { entry ->
                        val isCredit = entry.type == LedgerType.CREDIT
                        val amtStr = if (isPersian) {
                            "${PersianFormatter.formatTomans(entry.amount)} $currencyUnit"
                        } else {
                            "$${String.format("%.2f", entry.amount)}"
                        }
                        DashboardOrderItem(
                            orderNumber = entry.entryNumber,
                            amount = amtStr,
                            isLikelyFake = entry.category == LedgerCategory.REFUND,
                            paymentStatus = if (isCredit) "Paid" else "Unpaid",
                            orderTag = if (isCredit) "New" else "Expense"
                        )
                    }
                } else {
                    emptyList()
                }

                DashboardRecentOrders(
                    orders = recentOrdersList,
                    isPersian = isPersian,
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
                    isPersian = isPersian,
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
            isPersian = isPersian,
            currencyUnit = currencyUnit
        )
    }

    if (showWelcomeDialog) {
        OnboardingWelcomeDialog(
            userName = tenantState.userName,
            isPersian = isPersian,
            onStartOnboarding = {
                onDismissWelcomeDialog(true)
                onNavigateToDestination(MainDestination.INVENTORY, true)
            },
            onDismiss = { onDismissWelcomeDialog(true) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernRecordEntryDialog(
    onSubmit: (type: LedgerType, category: LedgerCategory, amount: Double, description: String) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean,
    currencyUnit: String
) {
    var selectedType by remember { mutableStateOf(LedgerType.DEBIT) }
    var selectedCategory by remember { mutableStateOf(LedgerCategory.EXPENSE) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    val categories = LedgerCategory.entries

    RestoDialog(
        title = if (isPersian) "ثبت سند مالی جدید" else "Record New Transaction",
        confirmText = if (isPersian) "ثبت نهایی" else "Record",
        onConfirm = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0 && descriptionText.isNotBlank()) {
                onSubmit(selectedType, selectedCategory, amt, descriptionText.trim())
            }
        },
        confirmEnabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && descriptionText.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)) {
            // Type Segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoButton(
                    text = if (isPersian) "+ درآمد / دریافتی" else "+ Income / Inflow",
                    onClick = {
                        selectedType = LedgerType.CREDIT
                        selectedCategory = LedgerCategory.CASH_IN
                    },
                    variant = if (selectedType == LedgerType.CREDIT) RestoButtonVariant.Primary else RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = if (isPersian) "- هزینه / پرداختی" else "- Expense / Outflow",
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
                    LedgerCategory.SALES -> if (isPersian) "درآمد فروش" else "Sales Income"
                    LedgerCategory.INVENTORY_ADJUSTMENT -> if (isPersian) "اصلاح موجودی" else "Stock Adjustment"
                    LedgerCategory.EXPENSE -> if (isPersian) "هزینه‌های جاری" else "Operating Expense"
                    LedgerCategory.CASH_IN -> if (isPersian) "ورود نقدینگی" else "Cash In"
                    LedgerCategory.CASH_OUT -> if (isPersian) "برداشت نقدینگی" else "Cash Out"
                    LedgerCategory.REFUND -> if (isPersian) "مرجوعی مشتری" else "Customer Refund"
                }

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isPersian) "دسته‌بندی سند" else "Category") },
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
                            LedgerCategory.SALES -> if (isPersian) "درآمد فروش" else "Sales Income"
                            LedgerCategory.INVENTORY_ADJUSTMENT -> if (isPersian) "اصلاح موجودی" else "Stock Adjustment"
                            LedgerCategory.EXPENSE -> if (isPersian) "هزینه‌های جاری" else "Operating Expense"
                            LedgerCategory.CASH_IN -> if (isPersian) "ورود نقدینگی" else "Cash In"
                            LedgerCategory.CASH_OUT -> if (isPersian) "برداشت نقدینگی" else "Cash Out"
                            LedgerCategory.REFUND -> if (isPersian) "مرجوعی مشتری" else "Customer Refund"
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
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = if (isPersian) "مبلغ ($currencyUnit)" else "Amount ($currencyUnit)",
                placeholder = "0"
            )

            // Description Input
            RestoTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = if (isPersian) "شرح سند / بابت" else "Description / Notes",
                placeholder = if (isPersian) "مثال: پرداخت قبض اینترنت دفتر" else "e.g. Office internet bill",
                singleLine = false,
                maxLines = 3
            )
        }
    }
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
            onboardingState = OnboardingState(
                isDismissed = false,
                hasProducts = false,
                hasSuppliers = false,
                isSupplierSkipped = false,
                hasOrders = false,
                hasCompletedOrders = false
            ),
            showWelcomeDialog = false,
            isRecordEntryDialogOpen = false,
            snackBarMessage = null
        )
    }
}
