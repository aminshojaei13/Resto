package com.braveboy.resto.ui.inventory

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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.Warehouse
import com.braveboy.resto.ui.components.RestoChip
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoEmptyState
import com.braveboy.resto.ui.components.RestoSearchField
import com.braveboy.resto.ui.components.RestoStatCard
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.guide.GuidePreferences
import com.braveboy.resto.ui.guide.ScreenGuideHost
import com.braveboy.resto.ui.guide.rememberGuideState
import com.braveboy.resto.ui.guide.spotlightTarget
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    guidePreferences: GuidePreferences? = null,
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
    val isAddProductDialogOpen by viewModel.isAddProductDialogOpen.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val isPersian = tenantState.language == "fa"
    val currencyUnit = if (isPersian) "تومان" else "$"
    val snackbarHostState = remember { SnackbarHostState() }

    val guideState = rememberGuideState()

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

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF8FAFC),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                RestoTopBar(
                    title = if (isPersian) "موجودی انبار" else "Inventory",
                    subtitle = if (isPersian) "کنترل انبارها و موجودی کالا" else "Stock levels & catalog",
                    onNavigationClick = onBackClick
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openAddProductDialog() },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier.spotlightTarget("inventory_fab", guideState)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = RestoSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = if (isPersian) "ثبت کالا" else "Add Product"
                        )
                        Text(
                            text = if (isPersian) "ثبت کالای جدید" else "Add Product",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .spotlightTarget("inventory_stats", guideState),
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
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RestoSpacing.sm))

                // SEARCH
                RestoSearchField(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    placeholder = if (isPersian) "جستجوی کالا، بارکد یا کد SKU..." else "Search by name, SKU, barcode...",
                    modifier = Modifier.spotlightTarget("inventory_search", guideState)
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
                        title = if (isPersian) "هنوز کالایی ثبت نشده است" else "No products found",
                        description = if (isPersian)
                            "برای شروع صدور فاکتور و کنترل موجودی انبار، اولین کالای خود را اضافه کنید."
                        else
                            "Add your first product to start inventory tracking and invoicing.",
                        actionText = if (isPersian) "ثبت اولین کالا" else "Add First Product",
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
                                onAdjustStockClick = { viewModel.openStockAdjustment(product) }
                            )
                        }
                    }
                }
            }
        }

        if (guidePreferences != null) {
            ScreenGuideHost(
                screenId = "inventory",
                guideState = guideState,
                guidePreferences = guidePreferences,
                isPersian = isPersian
            )
        }
    }

    // Add Product Dialog
    if (isAddProductDialogOpen) {
        AddProductDialog(
            warehouses = warehouses,
            activeWarehouseId = tenantState.activeWarehouseId,
            onSubmit = { name, sku, barcode, price, costPrice, category, unit, description, initialStock, warehouseId ->
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
                    warehouseId = warehouseId
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
    isPersian: Boolean,
    currencyUnit: String,
    onEditProductClick: () -> Unit,
    onAdjustStockClick: () -> Unit
) {
    val totalStock = product.getTotalStock()
    val isOutOfStock = totalStock == 0
    val isLowStock = totalStock in 1..9

    val stockBg = when {
        isOutOfStock -> Color(0xFFFEE2E2)
        isLowStock -> Color(0xFFFEF3C7)
        else -> Color(0xFFDCFCE7)
    }

    val stockTextColor = when {
        isOutOfStock -> Color(0xFFB91C1C)
        isLowStock -> Color(0xFFD97706)
        else -> Color(0xFF15803D)
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
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
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = product.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (isPersian) "کد:" else "SKU:"} ${product.sku}  •  ${product.category}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Text(
                    text = formattedPrice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
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
                            contentDescription = if (isPersian) "ویرایش کالا" else "Edit",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .clickable { onAdjustStockClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPersian) "تغییر موجودی" else "Adjust Stock",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
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
        warehouseId: String
    ) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true,
    currencyUnit: String = "تومان"
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

    val selectedWarehouseId = remember {
        mutableStateOf(
            if (activeWarehouseId.isNotBlank()) activeWarehouseId
            else warehouses.firstOrNull()?.id ?: ""
        )
    }

    RestoDialog(
        title = if (isPersian) "ثبت کالای جدید" else "Add New Product",
        confirmText = if (isPersian) "ثبت و افزودن به انبار" else "Add Product",
        onConfirm = {
            val priceVal = priceStr.toDoubleOrNull() ?: 0.0
            val costVal = costPriceStr.toDoubleOrNull() ?: 0.0
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
                    selectedWarehouseId.value
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
            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = if (isPersian) "نام کالا *" else "Product Name *"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                RestoTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = if (isPersian) "شناسه SKU *" else "SKU *",
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
                    value = initialStockStr,
                    onValueChange = { initialStockStr = it },
                    label = if (isPersian) "موجودی اولیه" else "Initial Stock",
                    modifier = Modifier.weight(1f)
                )
                RestoTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = if (isPersian) "واحد (عدد، بسته...)" else "Unit",
                    modifier = Modifier.weight(1f)
                )
            }

            RestoTextField(
                value = category,
                onValueChange = { category = it },
                label = if (isPersian) "دسته‌بندی کالا" else "Category"
            )

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
