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
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
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
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isPersian) "مرکز مدیریت کسب‌وکار" else "Business Control Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPersian) "سازمان: ${tenantState.activeOrgId} | شعبه: ${tenantState.activeStoreId}" else "Org: ${tenantState.activeOrgId} | Store: ${tenantState.activeStoreId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openRecordEntryDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Record Entry")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // THREE CORE JOBS HIGHLIGHT SECTION
            item {
                Text(
                    text = if (isPersian) "وظایف اصلی کسب‌وکار شما" else "Core Business Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Core Need 1: Inventory & Resources
                    CoreJobCard(
                        title = if (isPersian) "۱. موجودی و کالاها" else "1. Inventory & Stock Control",
                        subtitle = if (isPersian) "کنترل انبارها، کالاها و ورود خروج" else "Warehouse stock levels & catalog",
                        metricValue = "$${String.format("%.2f", metrics.totalInventoryValue)}",
                        metricLabel = if (isPersian) "ارزش کل موجودی (${metrics.lowStockCount} کمبود)" else "Stock Value (${metrics.lowStockCount} low stock)",
                        icon = Icons.Rounded.Inventory2,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )

                    // Core Need 2: Online & Social Orders
                    CoreJobCard(
                        title = if (isPersian) "۲. ثبت سفارش‌های آنلاین" else "2. Online & Social Orders",
                        subtitle = if (isPersian) "ثبت مستقیم یا از طریق پیام شبکه‌های اجتماعی" else "Fast checkout & social message import",
                        metricValue = "$${String.format("%.2f", metrics.todayRevenue)}",
                        metricLabel = if (isPersian) "فروش امروز (${metrics.todaySalesCount} سفارش)" else "Today's Sales (${metrics.todaySalesCount} orders)",
                        icon = Icons.AutoMirrored.Rounded.Message,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )

                    // Core Need 3: Centralized Business Workspace
                    CoreJobCard(
                        title = if (isPersian) "۳. مرکز متمرکز داده‌ها" else "3. Centralized Business Workspace",
                        subtitle = if (isPersian) "اطلاعات متمرکز کالاها، مشتریان و مالی بدون نیاز به اکسل" else "Centralized sales, customers, expenses & ledger",
                        metricValue = "$${String.format("%.2f", metrics.netProfit)}",
                        metricLabel = if (isPersian) "سود خالص عملیاتی" else "Net Operating Profit",
                        icon = Icons.Rounded.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            // Ledger Entries Activity Log
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPersian) "تراکنش‌های اخیر دفتر کل" else "General Ledger Activity Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = { viewModel.openRecordEntryDialog() }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPersian) "سند جدید" else "New Entry")
                    }
                }
            }

            if (ledgerEntries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isPersian) "هیچ تراکنشی ثبت نشده است." else "No general ledger entries recorded yet.")
                        }
                    }
                }
            } else {
                items(ledgerEntries, key = { it.id }) { entry ->
                    LedgerEntryCard(entry = entry, isPersian = isPersian)
                }
            }
        }
    }

    // Manual Entry Dialog
    if (isRecordEntryDialogOpen) {
        RecordLedgerEntryDialog(
            onSubmit = { type, category, amount, description ->
                viewModel.recordLedgerEntry(type, category, amount, description)
            },
            onDismiss = { viewModel.closeRecordEntryDialog() },
            isPersian = isPersian
        )
    }
}

@Composable
fun CoreJobCard(
    title: String,
    subtitle: String,
    metricValue: String,
    metricLabel: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = metricValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  $metricLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerEntryCard(entry: LedgerEntry, isPersian: Boolean = false) {
    val isCredit = entry.type == LedgerType.CREDIT
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.createdAt))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isCredit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = if (isCredit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.entryNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (isCredit) "+" else "-"}$${String.format("%.2f", entry.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordLedgerEntryDialog(
    onSubmit: (type: LedgerType, category: LedgerCategory, amount: Double, description: String) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = false
) {
    var selectedType by remember { mutableStateOf(LedgerType.DEBIT) }
    val categories = LedgerCategory.values().toList()
    var selectedCategory by remember { mutableStateOf(LedgerCategory.EXPENSE) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isPersian) "ثبت سند دوطرفه حسابداری" else "Record Financial Ledger Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Segmented
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedType = LedgerType.CREDIT
                            selectedCategory = LedgerCategory.CASH_IN
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedType == LedgerType.CREDIT) androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(if (isPersian) "+ بستانکار / درآمد" else "+ Credit / Income")
                    }

                    Button(
                        onClick = {
                            selectedType = LedgerType.DEBIT
                            selectedCategory = LedgerCategory.EXPENSE
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedType == LedgerType.DEBIT) androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(if (isPersian) "- بدهکار / هزینه" else "- Debit / Expense")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isPersian) "دسته‌بندی" else "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isPersian) "مبلغ (تومان / $)" else "Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text(if (isPersian) "شرح سند / بابت" else "Description / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && descriptionText.isNotBlank()) {
                                onSubmit(
                                    selectedType,
                                    selectedCategory,
                                    amt,
                                    descriptionText.trim()
                                )
                            }
                        },
                        enabled = (amountText.toDoubleOrNull()
                            ?: 0.0) > 0 && descriptionText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPersian) "ثبت سند" else "Record Entry")
                    }
                }
            }
        }
    }
}
