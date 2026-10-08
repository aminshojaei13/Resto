package com.braveboy.calcuapp.ui.expenses

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
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.Expense
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoBottomSheet
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoDialog
import com.braveboy.calcuapp.ui.components.RestoEmptyState
import com.braveboy.calcuapp.ui.components.RestoSearchField
import com.braveboy.calcuapp.ui.components.RestoTextField
import com.braveboy.calcuapp.ui.components.RestoTopBar
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val isCreateExpenseDialogOpen by viewModel.isCreateExpenseDialogOpen.collectAsState()
    val selectedExpenseDetail by viewModel.selectedExpenseDetail.collectAsState()
    val selectedExpenseForEdit by viewModel.selectedExpenseForEdit.collectAsState()
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
                title = if (isPersian) "هزینه‌های جاری کسب‌وکار" else "Operating Expenses",
                subtitle = if (isPersian) "مخارج دفتری، اجاره، تبلیغات، حمل‌ونقل و حقوق" else "Rent, utilities, ads & other operating costs"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateExpenseDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCard,
                    contentDescription = if (isPersian) "ثبت هزینه جدید" else "Add Expense"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // Explanation Banner: clarifies difference between Purchase and Expense
            RestoCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    Column {
                        Text(
                            text = if (isPersian) "تعریف هزینه‌های جاری:" else "What are Operating Expenses?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPersian)
                                "مخارجی مثل اجاره، بسته بندی، تبلیغات، اینترنت، حقوق و ارسال. (توجه: خرید کالای انبار در بخش «خرید و تامین» ثبت می‌شود)."
                            else
                                "Non-inventory costs such as rent, ads, internet, packaging and salaries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            RestoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = if (isPersian) "جستجو در شرح یا دسته‌بندی هزینه‌ها..." else "Search expenses..."
            )

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (expenses.isEmpty()) {
                RestoEmptyState(
                    title = if (isPersian) "هیچ هزینه‌ای ثبت نشده است" else "No expenses recorded",
                    description = if (isPersian)
                        "برای محاسبه دقیق سود و زیان کسب‌وکار، مخارج و هزینه‌های روزمره را ثبت کنید."
                    else
                        "Record your ongoing business expenses to keep your operating profit accurate.",
                    actionText = if (isPersian) "+ ثبت هزینه جدید" else "+ Record Expense",
                    onActionClick = { viewModel.openCreateExpenseDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit,
                            onClick = { viewModel.selectExpenseDetail(expense) },
                            onEditClick = { viewModel.openEditExpense(expense) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Expense Dialog
    if (isCreateExpenseDialogOpen) {
        ExpenseFormDialog(
            title = if (isPersian) "ثبت هزینه جدید" else "Record New Expense",
            initialExpense = null,
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            onSubmit = { category, amount, method, date, notes ->
                viewModel.addExpense(category, amount, date, method, notes)
            },
            onDismiss = { viewModel.closeCreateExpenseDialog() }
        )
    }

    selectedExpenseForEdit?.let { expense ->
        ExpenseFormDialog(
            title = if (isPersian) "ویرایش اطلاعات هزینه" else "Edit Expense",
            initialExpense = expense,
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            onSubmit = { category, amount, method, date, notes ->
                viewModel.updateExpense(
                    expense.copy(category = category, amount = amount, paymentMethod = method, date = date, notes = notes)
                )
            },
            onDismiss = { viewModel.closeEditExpense() }
        )
    }

    // Detail Sheet
    selectedExpenseDetail?.let { expense ->
        ExpenseDetailSheet(
            expense = expense,
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            onEditClick = {
                viewModel.selectExpenseDetail(null)
                viewModel.openEditExpense(expense)
            },
            onDismiss = { viewModel.selectExpenseDetail(null) }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    isPersian: Boolean,
    currencyUnit: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val formattedAmount = if (isPersian) {
        "${PersianFormatter.formatTomans(expense.amount)} $currencyUnit"
    } else {
        "$${String.format("%.2f", expense.amount)}"
    }

    RestoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(RestoSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(RestoSpacing.xs))
                    RestoBadge(
                        text = expense.paymentMethod,
                        variant = RestoBadgeVariant.Neutral
                    )
                }

                if (expense.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    title: String,
    initialExpense: Expense?,
    isPersian: Boolean,
    currencyUnit: String,
    onSubmit: (category: String, amount: Double, method: String, date: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultCategories = if (isPersian) {
        listOf(
            "اجاره دفتر و انبار",
            "تبلیغات و بازاریابی",
            "هزینه بسته‌بندی و پاکت",
            "هزینه پست و حمل‌ونقل",
            "اینترنت، هاست و نرم‌افزار",
            "حقوق و دستمزد پرسنل",
            "تعمیرات و نگهداری",
            "سایر مخارج اداری"
        )
    } else {
        listOf(
            "Rent & Facility",
            "Marketing & Ads",
            "Packaging Materials",
            "Shipping & Courier",
            "Internet & SaaS",
            "Payroll & Wages",
            "Repairs & Maintenance",
            "General Office Expenses"
        )
    }

    var category by remember { mutableStateOf(initialExpense?.category ?: defaultCategories.first()) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod ?: "نقد / کارت") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }

    val amountVal = amountText.toDoubleOrNull() ?: 0.0

    RestoDialog(
        title = title,
        confirmText = if (isPersian) "ثبت هزینه" else "Save Expense",
        onConfirm = {
            if (amountVal > 0) {
                onSubmit(category, amountVal, paymentMethod, "", notes.trim())
            }
        },
        confirmEnabled = amountVal > 0,
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isPersian) "دسته‌بندی هزینه *" else "Category *") },
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
                    defaultCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount input
            RestoTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = if (isPersian) "مبلغ هزینه ($currencyUnit) *" else "Amount ($currencyUnit) *",
                placeholder = "0"
            )

            // Payment method input
            RestoTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = if (isPersian) "روش پرداخت (کارت، نقد، شبا...)" else "Payment Method"
            )

            // Notes
            RestoTextField(
                value = notes,
                onValueChange = { notes = it },
                label = if (isPersian) "توضیحات و بابت" else "Notes / Description",
                singleLine = false,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailSheet(
    expense: Expense,
    isPersian: Boolean,
    currencyUnit: String,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedAmount = if (isPersian) {
        "${PersianFormatter.formatTomans(expense.amount)} $currencyUnit"
    } else {
        "$${String.format("%.2f", expense.amount)}"
    }

    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = expense.category,
        subtitle = if (isPersian) "رسید سند هزینه جاری" else "Operating Expense Record"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RestoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            RestoCard(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPersian) "مبلغ هزینه" else "Expense Amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    RestoBadge(
                        text = expense.paymentMethod,
                        variant = RestoBadgeVariant.Neutral
                    )
                }
            }

            if (expense.notes.isNotBlank()) {
                Text(
                    text = if (isPersian) "توضیحات:" else "Notes:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = expense.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoButton(
                    text = if (isPersian) "ویرایش" else "Edit",
                    onClick = onEditClick,
                    variant = RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = if (isPersian) "بستن" else "Close",
                    onClick = onDismiss,
                    variant = RestoButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
