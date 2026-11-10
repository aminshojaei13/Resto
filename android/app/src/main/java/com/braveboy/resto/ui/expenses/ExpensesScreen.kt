package com.braveboy.resto.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Expense
import com.braveboy.resto.ui.components.RestoBadge
import com.braveboy.resto.ui.components.RestoBadgeVariant
import com.braveboy.resto.ui.components.RestoBottomSheet
import com.braveboy.resto.ui.components.RestoButton
import com.braveboy.resto.ui.components.RestoButtonVariant
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoEmptyState
import com.braveboy.resto.ui.components.RestoLoadingState
import com.braveboy.resto.ui.components.RestoSearchField
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter

@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel,
    onBackClick: () -> Unit = {}
) {
    val expenses by viewModel.expenses.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isCreateExpenseDialogOpen by viewModel.isCreateExpenseDialogOpen.collectAsState()
    val selectedExpenseForEdit by viewModel.selectedExpenseForEdit.collectAsState()
    val selectedExpenseDetail by viewModel.selectedExpenseDetail.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val currencyUnit = stringResource(R.string.currency_unit)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = stringResource(R.string.expenses_title),
                onNavigationClick = onBackClick
            )
        },
        floatingActionButton = {
            if (expenses.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.openCreateExpenseDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddCard,
                        contentDescription = stringResource(R.string.record_new_expense)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // Explanation Banner
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
                            text = stringResource(R.string.operating_expenses_def),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.operating_expenses_desc),
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
                placeholder = stringResource(R.string.search_expenses_placeholder)
            )

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (isLoading && expenses.isEmpty()) {
                RestoLoadingState(
                    modifier = Modifier.weight(1f),
                    message = stringResource(R.string.loading_expenses)
                )
            } else if (expenses.isEmpty()) {
                RestoEmptyState(
                    title = stringResource(R.string.no_expenses_title),
                    description = stringResource(R.string.no_expenses_desc),
                    actionText = stringResource(R.string.record_new_expense),
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
                            currencyUnit = currencyUnit,
                            onClick = { viewModel.selectExpenseDetail(expense) },
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Expense Dialog
    if (isCreateExpenseDialogOpen) {
        ExpenseFormDialog(
            title = stringResource(R.string.record_new_expense),
            initialExpense = null,
            currencyUnit = currencyUnit,
            confirmLoading = isSaving,
            onSubmit = { category, amount, method, date, notes ->
                viewModel.addExpense(category, amount, date, method, notes)
            },
            onDismiss = { viewModel.closeCreateExpenseDialog() }
        )
    }

    selectedExpenseForEdit?.let { expense ->
        ExpenseFormDialog(
            title = stringResource(R.string.edit_expense_title),
            initialExpense = expense,
            currencyUnit = currencyUnit,
            confirmLoading = isSaving,
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
    currencyUnit: String,
    onClick: () -> Unit,
) {
    val formattedAmount = "${PersianFormatter.formatTomans(expense.amount)} $currencyUnit"

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
    currencyUnit: String,
    onSubmit: (category: String, amount: Double, method: String, date: String, notes: String) -> Unit,
    onDismiss: () -> Unit,
    confirmLoading: Boolean = false
) {
    val defaultCategories = listOf(
        "اجاره دفتر و انبار",
        "تبلیغات و بازاریابی",
        "هزینه بسته‌بندی و پاکت",
        "هزینه پست و حمل‌ونقل",
        "اینترنت، هاست و نرم‌افزار",
        "حقوق و دستمزد پرسنل",
        "تعمیرات و نگهداری",
        "سایر مخارج اداری"
    )

    var category by remember { mutableStateOf(initialExpense?.category ?: defaultCategories.first()) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf(initialExpense?.amount?.toLong()?.toString() ?: "") }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod ?: "نقد / کارت") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }

    val amountVal = PersianFormatter.parsePrice(amountText)

    RestoDialog(
        title = title,
        confirmText = stringResource(R.string.save_expense),
        confirmLoading = confirmLoading,
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
                    label = { Text("${stringResource(R.string.category_label)} *") },
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
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                label = "${stringResource(R.string.expense_amount_label)} ($currencyUnit)",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                placeholder = "0"
            )

            // Payment method input
            RestoTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = stringResource(R.string.payment_method_label)
            )

            // Notes
            RestoTextField(
                value = notes,
                onValueChange = { notes = it },
                label = stringResource(R.string.notes_description_label),
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
    currencyUnit: String,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedAmount = "${PersianFormatter.formatTomans(expense.amount)} $currencyUnit"

    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = expense.category,
        subtitle = stringResource(R.string.expense_record_subtitle)
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
                            text = stringResource(R.string.expense_amount_label),
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
                    text = "${stringResource(R.string.notes_description_label)}:",
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
                    text = stringResource(R.string.edit),
                    onClick = onEditClick,
                    variant = RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss,
                    variant = RestoButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
