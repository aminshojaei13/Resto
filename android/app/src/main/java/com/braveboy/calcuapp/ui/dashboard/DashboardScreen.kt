package com.braveboy.calcuapp.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoDialog
import com.braveboy.calcuapp.ui.components.RestoEmptyState
import com.braveboy.calcuapp.ui.components.RestoSection
import com.braveboy.calcuapp.ui.components.RestoStatCard
import com.braveboy.calcuapp.ui.components.RestoTextField
import com.braveboy.calcuapp.ui.components.RestoTopBar
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.ui.theme.RestoTheme
import com.braveboy.calcuapp.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val metrics by viewModel.metricsSummary.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val isRecordEntryDialogOpen by viewModel.isRecordEntryDialogOpen.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val isPersian = tenantState.language == "fa"
    val currencyUnit = if (isPersian) "تومان" else "$"
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = if (isPersian) "داشبورد مدیریت کسب‌وکار" else "Business Dashboard",
                subtitle = if (isPersian) "خوش آمدید، ${tenantState.userName}" else "Welcome, ${tenantState.userName}"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openRecordEntryDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = if (isPersian) "ثبت سند مالی" else "Record Entry"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md),
            contentPadding = PaddingValues(top = RestoSpacing.md, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            // Priority Action Banner (What to do right now?)
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

            // PRIMARY FINANCIAL KPIs (Row of 2 key stats)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
                ) {
                    val formattedTodayRevenue = if (isPersian) {
                        "${PersianFormatter.formatTomans(metrics.todayRevenue)} $currencyUnit"
                    } else {
                        "$${String.format("%.2f", metrics.todayRevenue)}"
                    }

                    RestoStatCard(
                        title = if (isPersian) "فروش امروز" else "Today's Sales",
                        value = formattedTodayRevenue,
                        subtitle = if (isPersian) "${PersianFormatter.toPersianDigits(metrics.todaySalesCount)} سفارش ثبت شده" else "${metrics.todaySalesCount} orders",
                        icon = Icons.Rounded.Paid,
                        badgeText = if (isPersian) "امروز" else "Today",
                        badgeVariant = RestoBadgeVariant.Success,
                        modifier = Modifier.weight(1f)
                    )

                    val formattedNetProfit = if (isPersian) {
                        "${PersianFormatter.formatTomans(metrics.netProfit)} $currencyUnit"
                    } else {
                        "$${String.format("%.2f", metrics.netProfit)}"
                    }

                    RestoStatCard(
                        title = if (isPersian) "سود خالص" else "Net Profit",
                        value = formattedNetProfit,
                        subtitle = if (isPersian) "عملیاتی" else "Operating",
                        icon = Icons.Rounded.TrendingUp,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // SECONDARY BUSINESS STATS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
                ) {
                    val formattedInventoryValue = if (isPersian) {
                        "${PersianFormatter.formatTomans(metrics.totalInventoryValue)} $currencyUnit"
                    } else {
                        "$${String.format("%.2f", metrics.totalInventoryValue)}"
                    }

                    RestoStatCard(
                        title = if (isPersian) "ارزش انبار" else "Stock Value",
                        value = formattedInventoryValue,
                        subtitle = if (isPersian) "کل کالاهای موجود" else "Inventory assets",
                        icon = Icons.Rounded.Inventory2,
                        modifier = Modifier.weight(1f)
                    )

                    val formattedExpenses = if (isPersian) {
                        "${PersianFormatter.formatTomans(metrics.totalExpenses)} $currencyUnit"
                    } else {
                        "$${String.format("%.2f", metrics.totalExpenses)}"
                    }

                    RestoStatCard(
                        title = if (isPersian) "هزینه‌های جاری" else "Total Expenses",
                        value = formattedExpenses,
                        subtitle = if (isPersian) "مخارج ثبت شده" else "Operating costs",
                        icon = Icons.Rounded.Receipt,
                        iconTint = MaterialTheme.colorScheme.error,
                        iconBackground = MaterialTheme.colorScheme.errorContainer,
                        badgeVariant = RestoBadgeVariant.Error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // RECENT ACTIVITY / LEDGER SECTION
            item {
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoSection(
                    title = if (isPersian) "گردش مالی و تراکنش‌های اخیر" else "Recent Financial Activity",
                    subtitle = if (isPersian) "ثبت درآمدهای فروش و هزینه‌های عملیاتی" else "Sales income & operational expenses",
                    actionText = if (isPersian) "+ ثبت سند" else "+ New Entry",
                    onActionClick = { viewModel.openRecordEntryDialog() }
                )
            }

            if (ledgerEntries.isEmpty()) {
                item {
                    RestoEmptyState(
                        title = if (isPersian) "هنوز تراکنشی ثبت نشده است" else "No transactions recorded yet",
                        description = if (isPersian)
                            "پس از انجام اولین فروش یا ثبت هزینه، تاریخچه تراکنش‌ها در اینجا نمایش داده می‌شود."
                        else
                            "When you make your first sale or record an expense, activity will appear here.",
                        actionText = if (isPersian) "ثبت اولین سند" else "Record Entry",
                        onActionClick = { viewModel.openRecordEntryDialog() }
                    )
                }
            } else {
                items(ledgerEntries, key = { it.id }) { entry ->
                    ModernLedgerItem(entry = entry, isPersian = isPersian, currencyUnit = currencyUnit)
                }
            }
        }
    }

    // Modern M3 Record Entry Dialog
    if (isRecordEntryDialogOpen) {
        ModernRecordEntryDialog(
            onSubmit = { type, category, amount, description ->
                viewModel.recordLedgerEntry(type, category, amount, description)
            },
            onDismiss = { viewModel.closeRecordEntryDialog() },
            isPersian = isPersian,
            currencyUnit = currencyUnit
        )
    }
}

@Composable
private fun ModernLedgerItem(
    entry: LedgerEntry,
    isPersian: Boolean,
    currencyUnit: String
) {
    val isCredit = entry.type == LedgerType.CREDIT
    val dateFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    val dateStr = if (isPersian) {
        PersianFormatter.toPersianDigits(dateFormat.format(Date(entry.createdAt)))
    } else {
        dateFormat.format(Date(entry.createdAt))
    }

    val formattedAmount = if (isPersian) {
        "${if (isCredit) "+" else "-"}${PersianFormatter.formatTomans(entry.amount)} $currencyUnit"
    } else {
        "${if (isCredit) "+" else "-"}$${String.format("%.2f", entry.amount)}"
    }

    val categoryTitle = when (entry.category) {
        LedgerCategory.SALES -> if (isPersian) "درآمد فروش" else "Sales Income"
        LedgerCategory.INVENTORY_ADJUSTMENT -> if (isPersian) "اصلاح موجودی" else "Stock Adjustment"
        LedgerCategory.EXPENSE -> if (isPersian) "هزینه جاری" else "Operating Expense"
        LedgerCategory.CASH_IN -> if (isPersian) "ورود نقدینگی" else "Cash In"
        LedgerCategory.CASH_OUT -> if (isPersian) "برداشت نقدینگی" else "Cash Out"
        LedgerCategory.REFUND -> if (isPersian) "مرجوعی به مشتری" else "Refund"
    }

    RestoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCredit) RestoTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = if (isCredit) RestoTheme.colors.success else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(RestoSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.entryNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(RestoSpacing.xs))
                    RestoBadge(
                        text = categoryTitle,
                        variant = if (isCredit) RestoBadgeVariant.Success else RestoBadgeVariant.Error
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(RestoSpacing.sm))

            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) RestoTheme.colors.success else MaterialTheme.colorScheme.error
            )
        }
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
