package com.braveboy.resto.ui.purchases

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalShipping
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
import androidx.compose.ui.unit.sp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.Purchase
import com.braveboy.resto.data.model.PurchaseItem
import com.braveboy.resto.data.model.Supplier
import com.braveboy.resto.ui.components.RestoBadge
import com.braveboy.resto.ui.components.RestoBadgeVariant
import com.braveboy.resto.ui.components.RestoBottomSheet
import com.braveboy.resto.ui.components.RestoButton
import com.braveboy.resto.ui.components.RestoButtonVariant
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoEmptyState
import com.braveboy.resto.ui.components.RestoLoadingState
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()
    val isCreatePurchaseDialogOpen by viewModel.isCreatePurchaseDialogOpen.collectAsState()
    val selectedPurchaseDetail by viewModel.selectedPurchaseDetail.collectAsState()
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
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = stringResource(R.string.purchases_title),
                onNavigationClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreatePurchaseDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.new_purchase_invoice)
                )
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

            // Informational Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.purchasing_cycle_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.purchasing_cycle_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (isLoading && purchases.isEmpty()) {
                RestoLoadingState(
                    modifier = Modifier.weight(1f),
                    message = stringResource(R.string.loading_purchases)
                )
            } else if (purchases.isEmpty()) {
                RestoEmptyState(
                    title = stringResource(R.string.no_purchases_title),
                    description = stringResource(R.string.no_purchases_desc),
                    actionText = stringResource(R.string.new_purchase_invoice),
                    onActionClick = { viewModel.openCreatePurchaseDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(purchases, key = { it.id }) { purchase ->
                        PurchaseItemCard(
                            purchase = purchase,
                            currencyUnit = currencyUnit,
                            onClick = { viewModel.selectPurchaseDetail(purchase) },
                            onReceive = { viewModel.receiveGoods(purchase) }
                        )
                    }
                }
            }
        }
    }

    // Create Purchase Dialog
    if (isCreatePurchaseDialogOpen) {
        CreatePurchaseDialog(
            suppliers = suppliers,
            products = products,
            currencyUnit = currencyUnit,
            confirmLoading = isSaving,
            onSubmit = { supplierId, supplierName, items ->
                viewModel.createPurchase(
                    supplierId = supplierId,
                    supplierName = supplierName,
                    warehouseId = tenantState.activeWarehouseId,
                    items = items
                )
            },
            onDismiss = { viewModel.closeCreatePurchaseDialog() }
        )
    }

    // Purchase Detail Sheet
    selectedPurchaseDetail?.let { purchase ->
        PurchaseDetailSheet(
            purchase = purchase,
            currencyUnit = currencyUnit,
            onReceive = {
                viewModel.receiveGoods(purchase)
                viewModel.selectPurchaseDetail(null)
            },
            onDismiss = { viewModel.selectPurchaseDetail(null) }
        )
    }
}

@Composable
fun PurchaseItemCard(
    purchase: Purchase,
    currencyUnit: String,
    onClick: () -> Unit,
    onReceive: () -> Unit
) {
    val isReceived = purchase.status == "RECEIVED"
    val statusText = if (isReceived) {
        stringResource(R.string.received_in_warehouse)
    } else {
        stringResource(R.string.pending_delivery)
    }
    val statusBg = if (isReceived) com.braveboy.resto.ui.theme.RestoTheme.colors.successContainer else com.braveboy.resto.ui.theme.RestoTheme.colors.warningContainer
    val statusTextColor = if (isReceived) com.braveboy.resto.ui.theme.RestoTheme.colors.onSuccessContainer else com.braveboy.resto.ui.theme.RestoTheme.colors.onWarningContainer

    val formattedAmount = "${PersianFormatter.formatTomans(purchase.totalAmount)} $currencyUnit"

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = PersianFormatter.toPersianDigits(dateFormat.format(Date(purchase.createdAt)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = purchase.purchaseNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تأمین‌کننده: ${purchase.supplierName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextColor
                    )
                }

                if (!isReceived) {
                    RestoButton(
                        text = stringResource(R.string.receive_stock),
                        onClick = onReceive,
                        variant = RestoButtonVariant.Primary,
                        compact = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseDialog(
    suppliers: List<Supplier>,
    products: List<Product>,
    currencyUnit: String,
    onSubmit: (supplierId: String, supplierName: String, items: List<PurchaseItem>) -> Unit,
    onDismiss: () -> Unit,
    confirmLoading: Boolean = false
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var quantityText by remember { mutableStateOf("10") }
    var unitCostText by remember { mutableStateOf(selectedProduct?.costPrice?.let { it.toLong().toString() } ?: "0") }

    val qty = quantityText.toIntOrNull() ?: 0
    val cost = PersianFormatter.parsePrice(unitCostText)

    RestoDialog(
        title = stringResource(R.string.new_purchase_invoice),
        confirmText = stringResource(R.string.submit_purchase_order),
        confirmLoading = confirmLoading,
        onConfirm = {
            val sup = selectedSupplier ?: return@RestoDialog
            val prod = selectedProduct ?: return@RestoDialog
            if (qty > 0 && cost > 0) {
                val item = PurchaseItem(
                    productId = prod.id,
                    productName = prod.name,
                    quantity = qty,
                    unitCost = cost,
                    totalCost = qty * cost
                )
                onSubmit(sup.id, sup.name, listOf(item))
            }
        },
        confirmEnabled = selectedSupplier != null && selectedProduct != null && qty > 0 && cost > 0,
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
            // Supplier Picker
            ExposedDropdownMenuBox(
                expanded = supplierDropdownExpanded,
                onExpandedChange = { supplierDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSupplier?.name ?: "انتخاب تأمین‌کننده...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.company_supplier_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                    shape = RestoShapes.medium,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = supplierDropdownExpanded,
                    onDismissRequest = { supplierDropdownExpanded = false }
                ) {
                    suppliers.forEach { sup ->
                        DropdownMenuItem(
                            text = { Text(sup.name) },
                            onClick = {
                                selectedSupplier = sup
                                supplierDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Product Picker
            ExposedDropdownMenuBox(
                expanded = productDropdownExpanded,
                onExpandedChange = { productDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedProduct?.name ?: "انتخاب کالا...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.product_name_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                    shape = RestoShapes.medium,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = productDropdownExpanded,
                    onDismissRequest = { productDropdownExpanded = false }
                ) {
                    products.forEach { prod ->
                        DropdownMenuItem(
                            text = { Text("${prod.name} (${prod.sku})") },
                            onClick = {
                                selectedProduct = prod
                                unitCostText = prod.costPrice.toLong().toString()
                                productDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    label = stringResource(R.string.quantity_delta),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                RestoTextField(
                    value = unitCostText,
                    onValueChange = { unitCostText = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                    label = "${stringResource(R.string.cost_price_label)} ($currencyUnit)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailSheet(
    purchase: Purchase,
    currencyUnit: String,
    onReceive: () -> Unit,
    onDismiss: () -> Unit
) {
    val isReceived = purchase.status == "RECEIVED"
    val formattedAmount = "${PersianFormatter.formatTomans(purchase.totalAmount)} $currencyUnit"

    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = purchase.purchaseNumber,
        subtitle = "فاکتور خرید از ${purchase.supplierName}"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RestoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            RestoCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.total_purchase_amount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    RestoBadge(
                        text = if (isReceived) stringResource(R.string.received_in_warehouse) else stringResource(R.string.pending_delivery),
                        variant = if (isReceived) RestoBadgeVariant.Success else RestoBadgeVariant.Warning
                    )
                }
            }

            Text(
                text = stringResource(R.string.purchased_items),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            purchase.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val qtyText = "${PersianFormatter.toPersianDigits(item.quantity)} عدد × ${PersianFormatter.formatTomans(item.unitCost)} $currencyUnit"
                        Text(
                            text = qtyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val totalText = "${PersianFormatter.formatTomans(item.totalCost)} $currencyUnit"

                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (!isReceived) {
                RestoButton(
                    text = stringResource(R.string.confirm_delivery),
                    onClick = onReceive,
                    variant = RestoButtonVariant.Primary,
                    fullWidth = true
                )
            }
        }
    }
}
