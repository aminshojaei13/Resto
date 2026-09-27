package com.braveboy.calcuapp.ui.purchases

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.ReceiptLong
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
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.Purchase
import com.braveboy.calcuapp.data.model.PurchaseItem
import com.braveboy.calcuapp.data.model.Supplier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()
    val isCreatePurchaseDialogOpen by viewModel.isCreatePurchaseDialogOpen.collectAsState()
    val selectedPurchaseDetail by viewModel.selectedPurchaseDetail.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

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
                            text = "Purchasing & Goods Receiving",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Org: ${tenantState.activeOrgId}",
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
                onClick = { viewModel.openCreatePurchaseDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Rounded.AddShoppingCart, contentDescription = "New Purchase Order")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (purchases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No purchase orders found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(purchases, key = { it.id }) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            onClick = { viewModel.selectPurchaseDetail(purchase) },
                            onReceiveClick = { viewModel.receiveGoods(purchase) }
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
            activeWarehouseId = tenantState.activeWarehouseId,
            onSubmit = { supplierId, supplierName, warehouseId, items ->
                viewModel.createPurchase(supplierId, supplierName, warehouseId, items)
            },
            onDismiss = { viewModel.closeCreatePurchaseDialog() }
        )
    }

    // Purchase Detail Dialog
    selectedPurchaseDetail?.let { purchase ->
        PurchaseDetailDialog(
            purchase = purchase,
            onReceiveGoods = { viewModel.receiveGoods(purchase) },
            onPaySupplier = { purchaseId, amount, method ->
                viewModel.paySupplier(purchaseId, amount, method)
            },
            onDismiss = { viewModel.selectPurchaseDetail(null) }
        )
    }
}

@Composable
fun PurchaseCard(
    purchase: Purchase,
    onClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    val isReceived = purchase.status == "RECEIVED"
    val isPaid = purchase.paymentStatus == "PAID"

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = purchase.purchaseNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Supplier: ${purchase.supplierName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Total amount
                Text(
                    text = "$${String.format("%.2f", purchase.totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Receiving Status Pill
                    Surface(
                        shape = CircleShape,
                        color = if (isReceived) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = if (isReceived) "RECEIVED" else "ORDERED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isReceived) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Payment Status Pill
                    Surface(
                        shape = CircleShape,
                        color = if (isPaid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = purchase.paymentStatus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!isReceived) {
                    Button(
                        onClick = onReceiveClick,
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Receive Goods", style = MaterialTheme.typography.labelMedium)
                    }
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
    activeWarehouseId: String,
    onSubmit: (supplierId: String, supplierName: String, warehouseId: String, items: List<PurchaseItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var selectedWarehouse by remember { mutableStateOf(activeWarehouseId) }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var qtyStr by remember { mutableStateOf("1") }
    var unitCostStr by remember { mutableStateOf("100.00") }

    val items = remember { mutableStateListOf<PurchaseItem>() }

    var supplierDropdownExpanded by remember { mutableStateOf(false) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    val totalCost = items.sumOf { it.totalCost }

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
                    text = "New Purchase Order",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Supplier Selector
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: "Select Supplier *",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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

                Spacer(modifier = Modifier.height(12.dp))

                // Item Addition Builder
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Add Product Item",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = productDropdownExpanded,
                            onExpandedChange = { productDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedProduct?.name ?: "Select Product",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
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
                                            unitCostStr = prod.costPrice.toString()
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qtyStr,
                                onValueChange = { qtyStr = it },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = unitCostStr,
                                onValueChange = { unitCostStr = it },
                                label = { Text("Unit Cost") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val prod = selectedProduct
                                val qty = qtyStr.toIntOrNull() ?: 1
                                val cost = unitCostStr.toDoubleOrNull() ?: 100.0
                                if (prod != null) {
                                    items.add(
                                        PurchaseItem(
                                            productId = prod.id,
                                            productName = prod.name,
                                            quantity = qty,
                                            unitCost = cost,
                                            totalCost = qty * cost
                                        )
                                    )
                                    selectedProduct = null
                                    qtyStr = "1"
                                }
                            },
                            enabled = selectedProduct != null,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Added Items List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Qty: ${item.quantity} x $${String.format("%.2f", item.unitCost)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("$${String.format("%.2f", item.totalCost)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { items.remove(item) }) {
                                Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Order Amount:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$${String.format("%.2f", totalCost)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val sup = selectedSupplier
                            if (sup != null && items.isNotEmpty()) {
                                onSubmit(sup.id, sup.name, selectedWarehouse, items.toList())
                            }
                        },
                        enabled = selectedSupplier != null && items.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Submit Order")
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseDetailDialog(
    purchase: Purchase,
    onReceiveGoods: () -> Unit,
    onPaySupplier: (id: String, amount: Double, method: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isPaying by remember { mutableStateOf(false) }
    var payAmountStr by remember { mutableStateOf(purchase.totalAmount.toString()) }

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
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = purchase.purchaseNumber,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Supplier: ${purchase.supplierName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Order Items Breakdown:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        purchase.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.productName.ifBlank { item.productId }} (x${item.quantity})", style = MaterialTheme.typography.bodyMedium)
                                Text("$${String.format("%.2f", item.totalCost)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Section
                if (purchase.status == "ORDERED") {
                    Button(
                        onClick = onReceiveGoods,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.LocalShipping, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Receive Goods Into Warehouse")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (purchase.paymentStatus != "PAID") {
                    if (!isPaying) {
                        OutlinedButton(
                            onClick = { isPaying = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Rounded.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record Supplier Payment")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
                                .padding(12.dp)
                        ) {
                            OutlinedTextField(
                                value = payAmountStr,
                                onValueChange = { payAmountStr = it },
                                label = { Text("Payment Amount ($)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val amt = payAmountStr.toDoubleOrNull() ?: purchase.totalAmount
                                    onPaySupplier(purchase.id, amt, "BANK_TRANSFER")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirm Payment")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}
