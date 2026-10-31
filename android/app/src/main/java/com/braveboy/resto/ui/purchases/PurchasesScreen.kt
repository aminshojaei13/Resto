package com.braveboy.resto.ui.purchases

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()
    val isCreatePurchaseDialogOpen by viewModel.isCreatePurchaseDialogOpen.collectAsState()
    val selectedPurchaseDetail by viewModel.selectedPurchaseDetail.collectAsState()
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
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = if (isPersian) "خرید و تأمین" else "Purchases",
                subtitle = if (isPersian) "ثبت فاکتورهای ورود کالا به انبار" else "Purchase orders & receiving",
                onNavigationClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreatePurchaseDialog() },
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = if (isPersian) "ثبت خرید جدید" else "New Purchase"
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

            // Informational Card clarifying Purchase vs Receiving
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isPersian) "چرخه خرید و تامین کالا" else "Purchasing & Receiving Cycle",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isPersian)
                                "خرید = فاکتور سفارش | ورود به انبار = دریافت فیزیکی کالا و شارژ موجودی."
                            else
                                "Purchase = Order placed | Receiving = Stock physically arrives.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (purchases.isEmpty()) {
                RestoEmptyState(
                    title = if (isPersian) "هنوز خریدی ثبت نشده است" else "No purchases recorded",
                    description = if (isPersian)
                        "با ثبت فاکتور خرید از تامین‌کننده، کالاهای سفارش‌داده‌شده را دریافت کرده و موجودی انبار را شارژ کنید."
                    else
                        "When you purchase products from suppliers, purchase orders and receiving records will appear here.",
                    actionText = if (isPersian) "+ ثبت خرید جدید" else "+ New Purchase",
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
                            isPersian = isPersian,
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
            isPersian = isPersian,
            currencyUnit = currencyUnit,
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
            isPersian = isPersian,
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
    isPersian: Boolean,
    currencyUnit: String,
    onClick: () -> Unit,
    onReceive: () -> Unit
) {
    val isReceived = purchase.status == "RECEIVED"
    val statusText = if (isReceived) {
        if (isPersian) "دریافت شده در انبار" else "Received"
    } else {
        if (isPersian) "در انتظار تحویل" else "Pending Delivery"
    }
    val statusBg = if (isReceived) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
    val statusTextColor = if (isReceived) Color(0xFF15803D) else Color(0xFFD97706)

    val formattedAmount = if (isPersian) {
        "${PersianFormatter.formatTomans(purchase.totalAmount)} $currencyUnit"
    } else {
        "$${String.format("%.2f", purchase.totalAmount)}"
    }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = if (isPersian) {
        PersianFormatter.toPersianDigits(dateFormat.format(Date(purchase.createdAt)))
    } else {
        dateFormat.format(Date(purchase.createdAt))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = purchase.purchaseNumber,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = formattedAmount,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (isPersian) "تأمین‌کننده:" else "Supplier:"} ${purchase.supplierName}",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .clickable { onReceive() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPersian) "ورود به انبار" else "Receive Stock",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
    isPersian: Boolean,
    currencyUnit: String,
    onSubmit: (supplierId: String, supplierName: String, items: List<PurchaseItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var quantityText by remember { mutableStateOf("10") }
    var unitCostText by remember { mutableStateOf(selectedProduct?.costPrice?.toString() ?: "0") }

    val qty = quantityText.toIntOrNull() ?: 0
    val cost = unitCostText.toDoubleOrNull() ?: 0.0

    RestoDialog(
        title = if (isPersian) "ثبت فاکتور خرید کالا" else "New Purchase Order",
        confirmText = if (isPersian) "ثبت سفارش خرید" else "Submit Order",
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
                    value = selectedSupplier?.name ?: (if (isPersian) "انتخاب تأمین‌کننده..." else "Select Supplier..."),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isPersian) "تأمین‌کننده طرف حساب *" else "Supplier *") },
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
                    value = selectedProduct?.name ?: (if (isPersian) "انتخاب کالا..." else "Select Product..."),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isPersian) "کالای خریداری‌شده *" else "Product *") },
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
                                unitCostText = prod.costPrice.toString()
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
                    label = if (isPersian) "تعداد خرید *" else "Quantity *",
                    modifier = Modifier.weight(1f)
                )

                RestoTextField(
                    value = unitCostText,
                    onValueChange = { unitCostText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = if (isPersian) "قیمت خرید واحد ($currencyUnit) *" else "Unit Cost *",
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
    isPersian: Boolean,
    currencyUnit: String,
    onReceive: () -> Unit,
    onDismiss: () -> Unit
) {
    val isReceived = purchase.status == "RECEIVED"
    val formattedAmount = if (isPersian) {
        "${PersianFormatter.formatTomans(purchase.totalAmount)} $currencyUnit"
    } else {
        "$${String.format("%.2f", purchase.totalAmount)}"
    }

    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = purchase.purchaseNumber,
        subtitle = "${if (isPersian) "فاکتور خرید از" else "From"} ${purchase.supplierName}"
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
                            text = if (isPersian) "مبلغ کل فاکتور خرید" else "Total Purchase Amount",
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
                        text = if (isReceived) (if (isPersian) "تحویل شده" else "Received") else (if (isPersian) "در انتظار ورود" else "Pending"),
                        variant = if (isReceived) RestoBadgeVariant.Success else RestoBadgeVariant.Warning
                    )
                }
            }

            Text(
                text = if (isPersian) "کالاهای این فاکتور" else "Purchased Items",
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
                        val qtyText = if (isPersian)
                            "${PersianFormatter.toPersianDigits(item.quantity)} عدد × ${PersianFormatter.formatTomans(item.unitCost)} $currencyUnit"
                        else
                            "${item.quantity}x @ $${String.format("%.2f", item.unitCost)}"
                        Text(
                            text = qtyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val totalText = if (isPersian) {
                        "${PersianFormatter.formatTomans(item.totalCost)} $currencyUnit"
                    } else {
                        "$${String.format("%.2f", item.totalCost)}"
                    }

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
                    text = if (isPersian) "تأیید تحویل و افزایش موجودی انبار" else "Confirm Delivery & Update Stock",
                    onClick = onReceive,
                    variant = RestoButtonVariant.Primary,
                    fullWidth = true
                )
            }

            RestoButton(
                text = if (isPersian) "بستن" else "Close",
                onClick = onDismiss,
                variant = RestoButtonVariant.Outlined,
                fullWidth = true
            )
        }
    }
}
