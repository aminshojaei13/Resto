package com.braveboy.resto.ui.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.Warehouse
import com.braveboy.resto.ui.components.RestoChip
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoEmptyState
import com.braveboy.resto.ui.components.RestoLoadingState
import com.braveboy.resto.ui.components.RestoSearchField
import com.braveboy.resto.ui.components.RestoStatCard
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.ImageStorageManager
import com.braveboy.resto.util.PersianFormatter

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val products by viewModel.products.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val selectedProductForAdjustment by viewModel.selectedProductForAdjustment.collectAsState()
    val selectedProductForEdit by viewModel.selectedProductForEdit.collectAsState()
    val selectedProductForDelete by viewModel.selectedProductForDelete.collectAsState()
    val isAddProductDialogOpen by viewModel.isAddProductDialogOpen.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val isPersian = tenantState.language == "fa"
    val currencyUnit = stringResource(R.string.currency_unit)
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = stringResource(R.string.inventory_title),
                onNavigationClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddProductDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = RestoSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_product_title)
                    )
                    Text(
                        text = stringResource(R.string.add_product_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
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

            // STATS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoStatCard(
                    title = stringResource(R.string.total_skus),
                    value = if (isPersian) PersianFormatter.toPersianDigits(totalSkus) else "$totalSkus",
                    subtitle = stringResource(R.string.active_items),
                    icon = Icons.Rounded.Inventory,
                    modifier = Modifier.weight(1f)
                )

                RestoStatCard(
                    title = stringResource(R.string.units_in_stock),
                    value = if (isPersian) PersianFormatter.toPersianDigits(totalStockUnits) else "$totalStockUnits",
                    subtitle = stringResource(R.string.units_in_stock),
                    icon = Icons.Rounded.Inventory2,
                    modifier = Modifier.weight(1f)
                )

                if (lowStockCount > 0 || outOfStockCount > 0) {
                    val alertTotal = lowStockCount + outOfStockCount
                    RestoStatCard(
                        title = stringResource(R.string.low_stock_warning),
                        value = if (isPersian) PersianFormatter.toPersianDigits(alertTotal) else "$alertTotal",
                        subtitle = if (isPersian) "$outOfStockCount ${stringResource(R.string.empty_items)}" else "$outOfStockCount empty",
                        icon = Icons.Rounded.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // SEARCH
            RestoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = stringResource(R.string.search_product_hint)
            )

            Spacer(modifier = Modifier.height(RestoSpacing.xs))

            // CATEGORY CHIPS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    RestoChip(
                        text = stringResource(R.string.all_categories_chip),
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

            // PRODUCT LIST / LOADING / EMPTY STATE
            if (isLoading && products.isEmpty()) {
                RestoLoadingState(
                    modifier = Modifier.weight(1f),
                    message = stringResource(R.string.loading_inventory)
                )
            } else if (products.isEmpty()) {
                RestoEmptyState(
                    title = stringResource(R.string.no_products_title),
                    description = stringResource(R.string.no_products_desc),
                    actionText = stringResource(R.string.add_first_product),
                    onActionClick = { viewModel.openAddProductDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products, key = { it.id }) { product ->
                        InventoryItemCard(
                            product = product,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit,
                            onEditProductClick = { viewModel.openEditProduct(product) },
                            onDeleteProductClick = { viewModel.openDeleteProduct(product) },
                            onAdjustStockClick = { viewModel.openStockAdjustment(product) }
                        )
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (isAddProductDialogOpen) {
        AddProductDialog(
            warehouses = warehouses,
            activeWarehouseId = tenantState.activeWarehouseId,
            confirmLoading = isSaving,
            onSubmit = { name, sku, barcode, price, costPrice, category, unit, description, initialStock, warehouseId, imageUrl ->
                viewModel.addProduct(
                    name = name,
                    sku = sku,
                    barcode = barcode,
                    price = price,
                    costPrice = costPrice,
                    category = category,
                    unit = unit,
                    description = description,
                    initialStock = initialStock,
                    warehouseId = warehouseId,
                    imageUrl = imageUrl
                )
            },
            onDismiss = { viewModel.closeAddProductDialog() },
            isPersian = isPersian,
            currencyUnit = currencyUnit
        )
    }

    // Stock Adjustment Dialog
    selectedProductForAdjustment?.let { product ->
        StockAdjustmentDialog(
            product = product,
            warehouses = warehouses,
            activeWarehouseId = tenantState.activeWarehouseId,
            confirmLoading = isSaving,
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
            confirmLoading = isSaving,
            onSubmit = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
            },
            onDismiss = { viewModel.closeEditProduct() },
            isPersian = isPersian,
            currencyUnit = currencyUnit
        )
    }

    // Delete Product Confirmation Dialog
    selectedProductForDelete?.let { product ->
        RestoDialog(
            title = stringResource(R.string.delete_product_title),
            confirmText = stringResource(R.string.delete),
            confirmLoading = isSaving,
            onConfirm = {
                viewModel.deleteProduct(product)
            },
            onDismissRequest = { viewModel.closeDeleteProduct() }
        ) {
            Text(
                text = String.format(stringResource(R.string.delete_product_confirm_message), product.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InventoryItemCard(
    product: Product,
    isPersian: Boolean,
    currencyUnit: String,
    onEditProductClick: () -> Unit,
    onDeleteProductClick: () -> Unit,
    onAdjustStockClick: () -> Unit
) {
    val totalStock = product.getTotalStock()
    val isOutOfStock = totalStock == 0
    val isLowStock = totalStock in 1..9

    val stockBg = when {
        isOutOfStock -> MaterialTheme.colorScheme.errorContainer
        isLowStock -> RestoTheme.colors.warningContainer
        else -> RestoTheme.colors.successContainer
    }

    val stockTextColor = when {
        isOutOfStock -> MaterialTheme.colorScheme.onErrorContainer
        isLowStock -> RestoTheme.colors.onWarningContainer
        else -> RestoTheme.colors.onSuccessContainer
    }

    val stockBadgeText = when {
        isOutOfStock -> stringResource(R.string.out_of_stock)
        isLowStock -> if (isPersian) "کمبود موجودی (${PersianFormatter.toPersianDigits(totalStock)})" else "Low ($totalStock)"
        else -> if (isPersian) "${PersianFormatter.toPersianDigits(totalStock)} ${product.unit}" else "$totalStock ${product.unit}"
    }

    val formattedPrice = if (isPersian) {
        "${PersianFormatter.formatTomans(product.price)} $currencyUnit"
    } else {
        "$${String.format("%.2f", product.price)}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(RestoTheme.colors.successContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Inventory2,
                                contentDescription = null,
                                tint = RestoTheme.colors.success,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = product.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${stringResource(R.string.stock_code)} ${product.sku}  •  ${product.category}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = formattedPrice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Stock badge & Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(stockBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stockBadgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = stockTextColor
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEditProductClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDeleteProductClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable { onAdjustStockClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.change_stock),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
    currencyUnit: String = stringResource(R.string.currency_unit),
    confirmLoading: Boolean = false
) {
    var name by remember { mutableStateOf(product.name) }
    var sku by remember { mutableStateOf(product.sku) }
    var barcode by remember { mutableStateOf(product.barcode) }
    var description by remember { mutableStateOf(product.description) }
    var priceStr by remember { mutableStateOf(if (product.price > 0) product.price.toLong().toString() else "") }
    var costPriceStr by remember { mutableStateOf(if (product.costPrice > 0) product.costPrice.toLong().toString() else "") }
    var category by remember { mutableStateOf(product.category) }
    var unit by remember { mutableStateOf(product.unit) }
    var imageUrl by remember { mutableStateOf(product.imageUrl) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageManager.saveImageLocally(
                context = context,
                uri = uri,
                folderName = "product_images",
                fileName = "prod_${System.currentTimeMillis()}"
            )
            if (!savedPath.isNullOrBlank()) {
                imageUrl = savedPath
            }
        }
    }

    RestoDialog(
        title = stringResource(R.string.edit_product_title),
        confirmText = stringResource(R.string.save_changes),
        confirmLoading = confirmLoading,
        onConfirm = {
            val priceVal = PersianFormatter.parsePrice(priceStr)
            val costVal = PersianFormatter.parsePrice(costPriceStr)
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
                        unit = unit.trim(),
                        imageUrl = imageUrl.trim()
                    )
                )
            }
        },
        confirmEnabled = name.isNotBlank() && sku.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
            // Product Image Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (imageUrl.isNotBlank()) (if (isPersian) "تصویر کالا انتخاب شد" else "Image Selected") else (if (isPersian) "افزودن تصویر کالا" else "Add Product Image"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPersian) "لمس کنید تا عکس کالا را تغییر دهید" else "Tap to change photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.product_name_label)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = stringResource(R.string.sku_label),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = stringResource(R.string.barcode_label),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(isPersian),
                    label = "${stringResource(R.string.selling_price_label)} ($currencyUnit)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(isPersian),
                    label = "${stringResource(R.string.cost_price_label)} ($currencyUnit)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = stringResource(R.string.category_label),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = stringResource(R.string.unit_label),
                    modifier = Modifier.weight(1f)
                )
            }

            RestoTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.description_optional_label),
                singleLine = false,
                maxLines = 2
            )
        }
    }
}

@Composable
fun AddProductDialog(
    warehouses: List<Warehouse>,
    activeWarehouseId: String,
    onSubmit: (
        name: String,
        sku: String,
        barcode: String,
        price: Double,
        costPrice: Double,
        category: String,
        unit: String,
        description: String,
        initialStock: Int,
        warehouseId: String,
        imageUrl: String
    ) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true,
    currencyUnit: String = stringResource(R.string.currency_unit),
    confirmLoading: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("PRD-${System.currentTimeMillis().toString().takeLast(5)}") }
    var barcode by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var costPriceStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عمومی") }
    var unit by remember { mutableStateOf("عدد") }
    var description by remember { mutableStateOf("") }
    var initialStockStr by remember { mutableStateOf("10") }
    var imageUrl by remember { mutableStateOf("") }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageManager.saveImageLocally(
                context = context,
                uri = uri,
                folderName = "product_images",
                fileName = "prod_${System.currentTimeMillis()}"
            )
            if (!savedPath.isNullOrBlank()) {
                imageUrl = savedPath
            }
        }
    }

    val selectedWarehouseId = remember {
        mutableStateOf(
            if (activeWarehouseId.isNotBlank()) activeWarehouseId
            else warehouses.firstOrNull()?.id ?: ""
        )
    }

    RestoDialog(
        title = stringResource(R.string.add_product_title),
        confirmText = stringResource(R.string.add_product_to_warehouse),
        confirmLoading = confirmLoading,
        onConfirm = {
            val priceVal = PersianFormatter.parsePrice(priceStr)
            val costVal = PersianFormatter.parsePrice(costPriceStr)
            val stockVal = initialStockStr.toIntOrNull() ?: 0
            if (name.isNotBlank() && sku.isNotBlank()) {
                onSubmit(
                    name.trim(),
                    sku.trim(),
                    barcode.trim(),
                    priceVal,
                    costVal,
                    category.trim(),
                    unit.trim(),
                    description.trim(),
                    stockVal,
                    selectedWarehouseId.value,
                    imageUrl.trim()
                )
            }
        },
        confirmEnabled = name.isNotBlank() && sku.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
        ) {
            // Product Image Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (imageUrl.isNotBlank()) (if (isPersian) "تصویر کالا انتخاب شد" else "Image Selected") else (if (isPersian) "افزودن تصویر کالا" else "Add Product Image"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPersian) "لمس کنید تا عکس کالا را انتخاب کنید" else "Tap to choose photo from gallery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.product_name_label)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = stringResource(R.string.sku_label),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = stringResource(R.string.barcode_label),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(isPersian),
                    label = "${stringResource(R.string.selling_price_label)} ($currencyUnit)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(isPersian),
                    label = "${stringResource(R.string.cost_price_label)} ($currencyUnit)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = initialStockStr,
                    onValueChange = { initialStockStr = it },
                    label = stringResource(R.string.initial_stock_label),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = stringResource(R.string.unit_label),
                    modifier = Modifier.weight(1f)
                )
            }

            RestoTextField(
                value = category,
                onValueChange = { category = it },
                label = stringResource(R.string.category_label)
            )

            RestoTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.description_optional_label),
                singleLine = false,
                maxLines = 2
            )
        }
    }
}
