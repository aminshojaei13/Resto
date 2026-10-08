package com.braveboy.calcuapp.ui.inventory

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.Warehouse
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoChip
import com.braveboy.calcuapp.ui.components.RestoDialog
import com.braveboy.calcuapp.ui.components.RestoEmptyState
import com.braveboy.calcuapp.ui.components.RestoSearchField
import com.braveboy.calcuapp.ui.components.RestoStatCard
import com.braveboy.calcuapp.ui.components.RestoTextField
import com.braveboy.calcuapp.ui.components.RestoTopBar
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.ui.theme.RestoTheme
import com.braveboy.calcuapp.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val products by viewModel.products.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val selectedProductForAdjustment by viewModel.selectedProductForAdjustment.collectAsState()
    val selectedProductForEdit by viewModel.selectedProductForEdit.collectAsState()
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

    val totalSkus = products.size
    val totalStockUnits = products.sumOf { it.getTotalStock() }
    val lowStockCount = products.count { it.getTotalStock() in 1..9 }
    val outOfStockCount = products.count { it.getTotalStock() == 0 }

    val categories = remember(products) {
        products.map { it.category }.distinct().sorted()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = if (isPersian) "مدیریت کالا و موجودی انبار" else "Inventory & Products",
                subtitle = if (isPersian) "کنترل انبارها و ورود و خروج موجودی" else "Stock levels & catalog"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // STATS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoStatCard(
                    title = if (isPersian) "تعداد اقلام" else "Total SKUs",
                    value = if (isPersian) PersianFormatter.toPersianDigits(totalSkus) else "$totalSkus",
                    subtitle = if (isPersian) "کالای فعال" else "Active items",
                    icon = Icons.Rounded.Inventory,
                    modifier = Modifier.weight(1f)
                )

                RestoStatCard(
                    title = if (isPersian) "موجودی کل" else "Total Units",
                    value = if (isPersian) PersianFormatter.toPersianDigits(totalStockUnits) else "$totalStockUnits",
                    subtitle = if (isPersian) "عدد در انبارها" else "Units in stock",
                    icon = Icons.Rounded.Inventory2,
                    modifier = Modifier.weight(1f)
                )

                if (lowStockCount > 0 || outOfStockCount > 0) {
                    val alertTotal = lowStockCount + outOfStockCount
                    RestoStatCard(
                        title = if (isPersian) "هشدار کسری" else "Low Stock",
                        value = if (isPersian) PersianFormatter.toPersianDigits(alertTotal) else "$alertTotal",
                        subtitle = if (isPersian) "$outOfStockCount ناموجود" else "$outOfStockCount empty",
                        icon = Icons.Rounded.Warning,
                        iconTint = MaterialTheme.colorScheme.error,
                        iconBackground = MaterialTheme.colorScheme.errorContainer,
                        badgeVariant = RestoBadgeVariant.Error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // SEARCH
            RestoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = if (isPersian) "جستجوی کالا، بارکد یا کد SKU..." else "Search by name, SKU, barcode..."
            )

            Spacer(modifier = Modifier.height(RestoSpacing.xs))

            // CATEGORY CHIPS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    RestoChip(
                        text = if (isPersian) "همه دسته‌ها" else "All",
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(categories) { category ->
                    RestoChip(
                        text = category,
                        selected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // PRODUCT LIST
            if (products.isEmpty()) {
                RestoEmptyState(
                    title = if (isPersian) "کالایی یافت نشد" else "No products found",
                    description = if (isPersian)
                        "با عبارت جستجوی فعلی یا این دسته‌بندی، هیچ کالایی در انبار ثبت نشده است."
                    else
                        "No products match your current search query or selected category.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products, key = { it.id }) { product ->
                        InventoryItemCard(
                            product = product,
                            warehouses = warehouses,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit,
                            onEditProductClick = { viewModel.openEditProduct(product) },
                            onAdjustStockClick = { viewModel.openStockAdjustment(product) }
                        )
                    }
                }
            }
        }
    }

    // Stock Adjustment Dialog
    selectedProductForAdjustment?.let { product ->
        StockAdjustmentDialog(
            product = product,
            warehouses = warehouses,
            activeWarehouseId = tenantState.activeWarehouseId,
            onSubmitAdjustment = { productId, warehouseId, delta, reason ->
                viewModel.submitStockAdjustment(productId, warehouseId, delta, reason)
            },
            onDismiss = { viewModel.closeStockAdjustment() },
            isPersian = isPersian
        )
    }

    // Edit Product Dialog
    selectedProductForEdit?.let { product ->
        EditProductDialog(
            product = product,
            onSubmit = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
            },
            onDismiss = { viewModel.closeEditProduct() },
            isPersian = isPersian,
            currencyUnit = currencyUnit
        )
    }
}

@Composable
fun InventoryItemCard(
    product: Product,
    warehouses: List<Warehouse>,
    isPersian: Boolean,
    currencyUnit: String,
    onEditProductClick: () -> Unit,
    onAdjustStockClick: () -> Unit
) {
    val totalStock = product.getTotalStock()
    val isOutOfStock = totalStock == 0
    val isLowStock = totalStock in 1..9

    val stockBadgeVariant = when {
        isOutOfStock -> RestoBadgeVariant.Error
        isLowStock -> RestoBadgeVariant.Warning
        else -> RestoBadgeVariant.Success
    }

    val stockBadgeText = when {
        isOutOfStock -> if (isPersian) "اتمام موجودی" else "Out of stock"
        isLowStock -> if (isPersian) "کمبود موجودی (${PersianFormatter.toPersianDigits(totalStock)})" else "Low ($totalStock)"
        else -> if (isPersian) "${PersianFormatter.toPersianDigits(totalStock)} ${product.unit}" else "$totalStock ${product.unit}"
    }

    val formattedPrice = if (isPersian) {
        "${PersianFormatter.formatTomans(product.price)} $currencyUnit"
    } else {
        "$${String.format("%.2f", product.price)}"
    }

    RestoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RestoShapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(RestoSpacing.md))

                    Column {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (isPersian) "کد:" else "SKU:"} ${product.sku}  •  ${product.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // Stock badge & Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RestoBadge(text = stockBadgeText, variant = stockBadgeVariant)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEditProductClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = if (isPersian) "ویرایش کالا" else "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    RestoButton(
                        text = if (isPersian) "افزایش / تغییر موجودی" else "Adjust Stock",
                        onClick = onAdjustStockClick,
                        variant = RestoButtonVariant.Outlined
                    )
                }
            }
        }
    }
}

@Composable
fun EditProductDialog(
    product: Product,
    onSubmit: (updatedProduct: Product) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true,
    currencyUnit: String = "تومان"
) {
    var name by remember { mutableStateOf(product.name) }
    var sku by remember { mutableStateOf(product.sku) }
    var barcode by remember { mutableStateOf(product.barcode) }
    var description by remember { mutableStateOf(product.description) }
    var priceStr by remember { mutableStateOf(product.price.toString()) }
    var costPriceStr by remember { mutableStateOf(product.costPrice.toString()) }
    var category by remember { mutableStateOf(product.category) }
    var unit by remember { mutableStateOf(product.unit) }

    RestoDialog(
        title = if (isPersian) "ویرایش اطلاعات کالا" else "Edit Product Details",
        confirmText = if (isPersian) "ذخیره تغییرات" else "Save Changes",
        onConfirm = {
            val priceVal = priceStr.toDoubleOrNull() ?: product.price
            val costVal = costPriceStr.toDoubleOrNull() ?: product.costPrice
            if (name.isNotBlank() && sku.isNotBlank()) {
                onSubmit(
                    product.copy(
                        name = name.trim(),
                        sku = sku.trim(),
                        barcode = barcode.trim(),
                        description = description.trim(),
                        price = priceVal,
                        costPrice = costVal,
                        category = category.trim(),
                        unit = unit.trim()
                    )
                )
            }
        },
        confirmEnabled = name.isNotBlank() && sku.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = if (isPersian) "نام کالا *" else "Product Name *"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = if (isPersian) "کد شناسایی SKU *" else "SKU *",
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = if (isPersian) "بارکد" else "Barcode",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = if (isPersian) "قیمت فروش ($currencyUnit) *" else "Selling Price *",
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it },
                    label = if (isPersian) "قیمت خرید ($currencyUnit)" else "Cost Price",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = if (isPersian) "دسته‌بندی" else "Category",
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = if (isPersian) "واحد (عدد، کیلوگرم...)" else "Unit",
                    modifier = Modifier.weight(1f)
                )
            }

            RestoTextField(
                value = description,
                onValueChange = { description = it },
                label = if (isPersian) "توضیحات اختیاری" else "Description",
                singleLine = false,
                maxLines = 2
            )
        }
    }
}
