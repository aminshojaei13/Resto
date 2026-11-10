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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.model.Warehouse
import com.braveboy.resto.ui.components.RestoChip
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoDropdownField
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
import com.braveboy.resto.util.ProductErrorMapper

@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel,
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

    val availableUnits by viewModel.availableUnits.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()

    InventoryScreenContent(
        tenantState = tenantState,
        searchQuery = searchQuery,
        selectedCategory = selectedCategory,
        products = products,
        warehouses = warehouses,
        selectedProductForAdjustment = selectedProductForAdjustment,
        selectedProductForEdit = selectedProductForEdit,
        selectedProductForDelete = selectedProductForDelete,
        isAddProductDialogOpen = isAddProductDialogOpen,
        snackbarMessage = snackbarMessage,
        isLoading = isLoading,
        isSaving = isSaving,
        availableUnits = availableUnits,
        availableCategories = availableCategories,
        defaultUnits = viewModel.defaultUnits,
        defaultCategories = viewModel.defaultCategories,
        onAddCustomUnit = { viewModel.addCustomUnit(it) },
        onDeleteUnit = { viewModel.deleteUnit(it) },
        onAddCustomCategory = { viewModel.addCustomCategory(it) },
        onDeleteCategory = { viewModel.deleteCategory(it) },
        onBackClick = onBackClick,
        onClearSnackbarMessage = { viewModel.clearSnackbarMessage() },
        onOpenAddProductDialog = { viewModel.openAddProductDialog() },
        onCloseAddProductDialog = { viewModel.closeAddProductDialog() },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onSelectCategory = { viewModel.selectCategory(it) },
        onOpenEditProduct = { viewModel.openEditProduct(it) },
        onCloseEditProduct = { viewModel.closeEditProduct() },
        onOpenDeleteProduct = { viewModel.openDeleteProduct(it) },
        onCloseDeleteProduct = { viewModel.closeDeleteProduct() },
        onOpenStockAdjustment = { viewModel.openStockAdjustment(it) },
        onCloseStockAdjustment = { viewModel.closeStockAdjustment() },
        onAddProduct = { name, sku, barcode, price, costPrice, category, unit, description, initialStock, warehouseId, imageUrl ->
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
        onSubmitStockAdjustment = { productId, warehouseId, delta, reason ->
            viewModel.submitStockAdjustment(productId, warehouseId, delta, reason)
        },
        onUpdateProduct = { viewModel.updateProduct(it) },
        onDeleteProduct = { viewModel.deleteProduct(it) },
        modifier = modifier
    )
}

@Composable
fun InventoryScreenContent(
    modifier: Modifier = Modifier,
    tenantState: TenantState,
    searchQuery: String,
    selectedCategory: String?,
    products: List<Product>,
    warehouses: List<Warehouse>,
    selectedProductForAdjustment: Product?,
    selectedProductForEdit: Product?,
    selectedProductForDelete: Product?,
    isAddProductDialogOpen: Boolean,
    snackbarMessage: String?,
    isLoading: Boolean,
    isSaving: Boolean,
    availableUnits: List<String> = emptyList(),
    availableCategories: List<String> = emptyList(),
    defaultUnits: List<String> = emptyList(),
    defaultCategories: List<String> = emptyList(),
    onAddCustomUnit: (String) -> Unit = {},
    onDeleteUnit: (String) -> Unit = {},
    onAddCustomCategory: (String) -> Unit = {},
    onDeleteCategory: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onClearSnackbarMessage: () -> Unit = {},
    onOpenAddProductDialog: () -> Unit = {},
    onCloseAddProductDialog: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSelectCategory: (String?) -> Unit = {},
    onOpenEditProduct: (Product) -> Unit = {},
    onCloseEditProduct: () -> Unit = {},
    onOpenDeleteProduct: (Product) -> Unit = {},
    onCloseDeleteProduct: () -> Unit = {},
    onOpenStockAdjustment: (Product) -> Unit = {},
    onCloseStockAdjustment: () -> Unit = {},
    onAddProduct: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onSubmitStockAdjustment: (productId: String, warehouseId: String, delta: Int, reason: String) -> Unit = { _, _, _, _ -> },
    onUpdateProduct: (Product) -> Unit = {},
    onDeleteProduct: (Product) -> Unit = {}
) {
    val currencyUnit = stringResource(R.string.currency_unit)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearSnackbarMessage()
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
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = stringResource(R.string.inventory_title),
                onNavigationClick = onBackClick
            )
        },
        floatingActionButton = {
            if (products.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onOpenAddProductDialog,
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // STATS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoStatCard(
                    title = stringResource(R.string.total_skus),
                    value = PersianFormatter.toPersianDigits(totalSkus),
                    subtitle = stringResource(R.string.active_items),
                    icon = Icons.Rounded.Inventory,
                    modifier = Modifier.weight(1f)
                )

                RestoStatCard(
                    title = stringResource(R.string.units_in_stock),
                    value = PersianFormatter.toPersianDigits(totalStockUnits),
                    subtitle = stringResource(R.string.units_in_stock),
                    icon = Icons.Rounded.Inventory2,
                    modifier = Modifier.weight(1f)
                )

                if (lowStockCount > 0 || outOfStockCount > 0) {
                    val alertTotal = lowStockCount + outOfStockCount
                    RestoStatCard(
                        title = stringResource(R.string.low_stock_warning),
                        value = PersianFormatter.toPersianDigits(alertTotal),
                        subtitle = "$outOfStockCount ${stringResource(R.string.empty_items)}",
                        icon = Icons.Rounded.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // SEARCH
            RestoSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
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
                        onClick = { onSelectCategory(null) }
                    )
                }
                items(categories) { category ->
                    RestoChip(
                        text = category,
                        selected = selectedCategory == category,
                        onClick = { onSelectCategory(category) }
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
                    onActionClick = onOpenAddProductDialog,
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
                            currencyUnit = currencyUnit,
                            onEditProductClick = { onOpenEditProduct(product) },
                            onDeleteProductClick = { onOpenDeleteProduct(product) },
                            onAdjustStockClick = { onOpenStockAdjustment(product) }
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
            onSubmit = onAddProduct,
            onDismiss = onCloseAddProductDialog,
            currencyUnit = currencyUnit,
            availableUnits = availableUnits,
            availableCategories = availableCategories,
            defaultUnits = defaultUnits,
            defaultCategories = defaultCategories,
            onAddCustomUnit = onAddCustomUnit,
            onDeleteUnit = onDeleteUnit,
            onAddCustomCategory = onAddCustomCategory,
            onDeleteCategory = onDeleteCategory
        )
    }

    // Stock Adjustment Dialog
    selectedProductForAdjustment?.let { product ->
        StockAdjustmentDialog(
            product = product,
            warehouses = warehouses,
            activeWarehouseId = tenantState.activeWarehouseId,
            confirmLoading = isSaving,
            onSubmitAdjustment = onSubmitStockAdjustment,
            onDismiss = onCloseStockAdjustment
        )
    }

    // Edit Product Dialog
    selectedProductForEdit?.let { product ->
        EditProductDialog(
            product = product,
            confirmLoading = isSaving,
            onSubmit = onUpdateProduct,
            onDismiss = onCloseEditProduct,
            currencyUnit = currencyUnit,
            availableUnits = availableUnits,
            availableCategories = availableCategories,
            defaultUnits = defaultUnits,
            defaultCategories = defaultCategories,
            onAddCustomUnit = onAddCustomUnit,
            onDeleteUnit = onDeleteUnit,
            onAddCustomCategory = onAddCustomCategory,
            onDeleteCategory = onDeleteCategory
        )
    }

    // Delete Product Confirmation Dialog
    selectedProductForDelete?.let { product ->
        RestoDialog(
            title = stringResource(R.string.delete_product_title),
            confirmText = stringResource(R.string.delete),
            confirmLoading = isSaving,
            onConfirm = { onDeleteProduct(product) },
            onDismissRequest = onCloseDeleteProduct
        ) {
            Text(
                text = stringResource(R.string.delete_product_confirm_message, product.name),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InventoryItemCard(
    product: Product,
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
        isLowStock -> "کمبود موجودی (${PersianFormatter.toPersianDigits(totalStock)})"
        else -> "${PersianFormatter.toPersianDigits(totalStock)} ${product.unit}"
    }

    val formattedPrice = "${PersianFormatter.formatTomans(product.price)} $currencyUnit"

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
    currencyUnit: String = stringResource(R.string.currency_unit),
    confirmLoading: Boolean = false,
    availableUnits: List<String> = emptyList(),
    availableCategories: List<String> = emptyList(),
    defaultUnits: List<String> = emptyList(),
    defaultCategories: List<String> = emptyList(),
    onAddCustomUnit: (String) -> Unit = {},
    onDeleteUnit: (String) -> Unit = {},
    onAddCustomCategory: (String) -> Unit = {},
    onDeleteCategory: (String) -> Unit = {}
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
    var showErrors by remember { mutableStateOf(false) }

    var isAddNewUnitPromptOpen by remember { mutableStateOf(false) }
    var newUnitInput by remember { mutableStateOf("") }

    var isAddNewCategoryPromptOpen by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val errors = ProductErrorMapper.validate(
        name = name,
        sku = sku,
        priceStr = priceStr,
        costPriceStr = costPriceStr,
        category = category,
        unit = unit
    )

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
            if (!errors.hasError) {
                val priceVal = PersianFormatter.parsePrice(priceStr)
                val costVal = PersianFormatter.parsePrice(costPriceStr)
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
            } else {
                showErrors = true
            }
        },
        confirmEnabled = true,
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
                        text = if (imageUrl.isNotBlank()) stringResource(R.string.image_selected) else "افزودن تصویر کالا",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.tap_to_change_photo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.product_name_label),
                errorText = if (showErrors) errors.nameError else null
            )

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = stringResource(R.string.sku_label),
                    errorText = if (showErrors) errors.skuError else null,
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
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                    label = "${stringResource(R.string.selling_price_label)} ($currencyUnit)",
                    errorText = if (showErrors) errors.priceError else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                    label = "${stringResource(R.string.cost_price_label)} ($currencyUnit)",
                    errorText = if (showErrors) errors.costPriceError else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoDropdownField(
                    selectedOption = category,
                    onOptionSelected = { category = it },
                    options = availableCategories,
                    label = stringResource(R.string.category_label),
                    errorText = if (showErrors) errors.categoryError else null,
                    addNewLabel = "+ افزودن دسته‌بندی جدید",
                    onAddNewOption = { isAddNewCategoryPromptOpen = true },
                    onDeleteOption = onDeleteCategory,
                    modifier = Modifier.weight(1f)
                )
                RestoDropdownField(
                    selectedOption = unit,
                    onOptionSelected = { unit = it },
                    options = availableUnits,
                    label = stringResource(R.string.unit_label),
                    errorText = if (showErrors) errors.unitError else null,
                    addNewLabel = "+ افزودن واحد جدید",
                    onAddNewOption = { isAddNewUnitPromptOpen = true },
                    onDeleteOption = onDeleteUnit,
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

    if (isAddNewCategoryPromptOpen) {
        RestoDialog(
            title = "افزودن دسته‌بندی جدید",
            confirmText = "افزودن",
            onConfirm = {
                if (newCategoryInput.isNotBlank()) {
                    onAddCustomCategory(newCategoryInput)
                    category = newCategoryInput.trim()
                    isAddNewCategoryPromptOpen = false
                    newCategoryInput = ""
                }
            },
            confirmEnabled = newCategoryInput.isNotBlank(),
            onDismissRequest = { isAddNewCategoryPromptOpen = false }
        ) {
            RestoTextField(
                value = newCategoryInput,
                onValueChange = { newCategoryInput = it },
                label = "نام دسته‌بندی جدید"
            )
        }
    }

    if (isAddNewUnitPromptOpen) {
        RestoDialog(
            title = "افزودن واحد سنجش جدید",
            confirmText = "افزودن",
            onConfirm = {
                if (newUnitInput.isNotBlank()) {
                    onAddCustomUnit(newUnitInput)
                    unit = newUnitInput.trim()
                    isAddNewUnitPromptOpen = false
                    newUnitInput = ""
                }
            },
            confirmEnabled = newUnitInput.isNotBlank(),
            onDismissRequest = { isAddNewUnitPromptOpen = false }
        ) {
            RestoTextField(
                value = newUnitInput,
                onValueChange = { newUnitInput = it },
                label = "نام واحد سنجش جدید"
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
    currencyUnit: String = stringResource(R.string.currency_unit),
    confirmLoading: Boolean = false,
    availableUnits: List<String> = emptyList(),
    availableCategories: List<String> = emptyList(),
    defaultUnits: List<String> = emptyList(),
    defaultCategories: List<String> = emptyList(),
    onAddCustomUnit: (String) -> Unit = {},
    onDeleteUnit: (String) -> Unit = {},
    onAddCustomCategory: (String) -> Unit = {},
    onDeleteCategory: (String) -> Unit = {}
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
    var showErrors by remember { mutableStateOf(false) }

    var isAddNewUnitPromptOpen by remember { mutableStateOf(false) }
    var newUnitInput by remember { mutableStateOf("") }

    var isAddNewCategoryPromptOpen by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val errors = ProductErrorMapper.validate(
        name = name,
        sku = sku,
        priceStr = priceStr,
        costPriceStr = costPriceStr,
        category = category,
        unit = unit,
        stockStr = initialStockStr
    )

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
            activeWarehouseId.ifBlank { warehouses.firstOrNull()?.id ?: "" }
        )
    }

    RestoDialog(
        title = stringResource(R.string.add_product_title),
        confirmText = stringResource(R.string.add_product_to_warehouse),
        confirmLoading = confirmLoading,
        onConfirm = {
            if (!errors.hasError) {
                val priceVal = PersianFormatter.parsePrice(priceStr)
                val costVal = PersianFormatter.parsePrice(costPriceStr)
                val stockVal = initialStockStr.trim().toIntOrNull() ?: 0
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
            } else {
                showErrors = true
            }
        },
        confirmEnabled = true,
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
                        text = if (imageUrl.isNotBlank()) stringResource(R.string.image_selected) else "افزودن تصویر کالا",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.tap_to_choose_photo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.product_name_label),
                errorText = if (showErrors) errors.nameError else null
            )

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = stringResource(R.string.sku_label),
                    errorText = if (showErrors) errors.skuError else null,
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
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                    label = "${stringResource(R.string.selling_price_label)} ($currencyUnit)",
                    errorText = if (showErrors) errors.priceError else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                    label = "${stringResource(R.string.cost_price_label)} ($currencyUnit)",
                    errorText = if (showErrors) errors.costPriceError else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = initialStockStr,
                    onValueChange = { initialStockStr = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    label = stringResource(R.string.initial_stock_label),
                    errorText = if (showErrors) errors.stockError else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                RestoDropdownField(
                    selectedOption = unit,
                    onOptionSelected = { unit = it },
                    options = availableUnits,
                    label = stringResource(R.string.unit_label),
                    errorText = if (showErrors) errors.unitError else null,
                    addNewLabel = "+ افزودن واحد جدید",
                    onAddNewOption = { isAddNewUnitPromptOpen = true },
                    onDeleteOption = onDeleteUnit,
                    modifier = Modifier.weight(1f)
                )
            }

            RestoDropdownField(
                selectedOption = category,
                onOptionSelected = { category = it },
                options = availableCategories,
                label = stringResource(R.string.category_label),
                errorText = if (showErrors) errors.categoryError else null,
                addNewLabel = "+ افزودن دسته‌بندی جدید",
                onAddNewOption = { isAddNewCategoryPromptOpen = true },
                onDeleteOption = onDeleteCategory
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

    if (isAddNewCategoryPromptOpen) {
        RestoDialog(
            title = "افزودن دسته‌بندی جدید",
            confirmText = "افزودن",
            onConfirm = {
                if (newCategoryInput.isNotBlank()) {
                    onAddCustomCategory(newCategoryInput)
                    category = newCategoryInput.trim()
                    isAddNewCategoryPromptOpen = false
                    newCategoryInput = ""
                }
            },
            confirmEnabled = newCategoryInput.isNotBlank(),
            onDismissRequest = { isAddNewCategoryPromptOpen = false }
        ) {
            RestoTextField(
                value = newCategoryInput,
                onValueChange = { newCategoryInput = it },
                label = "نام دسته‌بندی جدید"
            )
        }
    }

    if (isAddNewUnitPromptOpen) {
        RestoDialog(
            title = "افزودن واحد سنجش جدید",
            confirmText = "افزودن",
            onConfirm = {
                if (newUnitInput.isNotBlank()) {
                    onAddCustomUnit(newUnitInput)
                    unit = newUnitInput.trim()
                    isAddNewUnitPromptOpen = false
                    newUnitInput = ""
                }
            },
            confirmEnabled = newUnitInput.isNotBlank(),
            onDismissRequest = { isAddNewUnitPromptOpen = false }
        ) {
            RestoTextField(
                value = newUnitInput,
                onValueChange = { newUnitInput = it },
                label = "نام واحد سنجش جدید"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InventoryItemCardPreview() {
    RestoTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            InventoryItemCard(
                product = Product(
                    id = "1",
                    orgId = "org1",
                    sku = "PRD-1001",
                    barcode = "123456789",
                    name = "اسپرسو سینگل",
                    description = "قهوه اسپرسو ۱۰۰٪ عربیکا",
                    price = 45000.0,
                    costPrice = 20000.0,
                    category = "نوشیدنی",
                    unit = "عدد",
                    stockQuantityByWarehouse = mapOf("w1" to 15)
                ),
                currencyUnit = "تومان",
                onEditProductClick = {},
                onDeleteProductClick = {},
                onAdjustStockClick = {}
            )
        }
    }
}

@Preview(name = "Edit Product Dialog Preview", showBackground = true)
@Composable
fun EditProductDialogPreview() {
    RestoTheme {
        EditProductDialog(
            product = Product(
                id = "1",
                orgId = "org1",
                sku = "PRD-1001",
                barcode = "123456789",
                name = "اسپرسو سینگل",
                description = "قهوه اسپرسو ۱۰۰٪ عربیکا",
                price = 45000.0,
                costPrice = 20000.0,
                category = "نوشیدنی",
                unit = "عدد",
                imageUrl = "",
                stockQuantityByWarehouse = mapOf("w1" to 15)
            ),
            onSubmit = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Add Product Dialog Preview", showBackground = true)
@Composable
fun AddProductDialogPreview() {
    RestoTheme {
        AddProductDialog(
            warehouses = listOf(
                Warehouse(
                    id = "wh-1",
                    storeId = "store-1",
                    orgId = "org-1",
                    name = "انبار مرکزی",
                    code = "WH-01",
                    address = "تهران، خیابان اصلی"
                ),
                Warehouse(
                    id = "wh-2",
                    storeId = "store-1",
                    orgId = "org-1",
                    name = "انبار شماره ۲",
                    code = "WH-02",
                    address = "تهران، خیابان دوم"
                )
            ),
            activeWarehouseId = "wh-1",
            onSubmit = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview(name = "Inventory Screen Preview", showBackground = true)
@Composable
fun InventoryScreenPreview() {
    val sampleWarehouse = Warehouse(
        id = "w1",
        storeId = "s1",
        orgId = "o1",
        name = "انبار مرکزی",
        code = "WH-01",
        address = "تهران، خیابان اصلی"
    )
    val sampleProducts = listOf(
        Product(
            id = "p1",
            orgId = "o1",
            sku = "PRD-1001",
            barcode = "626000000001",
            name = "پیتزا پپرونی",
            description = "پیتزا پپرونی تند با پنیر موزارلا",
            price = 280000.0,
            costPrice = 180000.0,
            category = "پیتزا",
            unit = "عدد",
            stockQuantityByWarehouse = mapOf("w1" to 25)
        ),
        Product(
            id = "p2",
            orgId = "o1",
            sku = "PRD-1002",
            barcode = "626000000002",
            name = "همبرگر مخصوص",
            description = "برگر گوشت تازه به همراه قارچ و پنیر",
            price = 220000.0,
            costPrice = 140000.0,
            category = "برگر",
            unit = "عدد",
            stockQuantityByWarehouse = mapOf("w1" to 5)
        ),
        Product(
            id = "p3",
            orgId = "o1",
            sku = "PRD-1003",
            barcode = "626000000003",
            name = "نوشابه قوطی",
            description = "نوشابه کوکاکولا ۳۳۰ میلی‌لیتر",
            price = 30000.0,
            costPrice = 20000.0,
            category = "نوشیدنی",
            unit = "عدد",
            stockQuantityByWarehouse = mapOf("w1" to 0)
        )
    )

    RestoTheme {
        InventoryScreenContent(
            tenantState = TenantState(
                activeOrgId = "o1",
                activeWarehouseId = "w1"
            ),
            searchQuery = "",
            selectedCategory = null,
            products = sampleProducts,
            warehouses = listOf(sampleWarehouse),
            selectedProductForAdjustment = null,
            selectedProductForEdit = null,
            selectedProductForDelete = null,
            isAddProductDialogOpen = false,
            snackbarMessage = null,
            isLoading = false,
            isSaving = false
        )
    }
}

