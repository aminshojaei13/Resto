package com.braveboy.calcuapp.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.intake.ParsedItem
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoCardVariant
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.ui.theme.RestoTheme
import com.braveboy.calcuapp.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier,
    initialSharedText: String? = null
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val catalogProducts by viewModel.catalogProducts.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()

    val isPersian = tenantState.language == "fa"
    val currencyUnit = if (isPersian) "تومان" else "$"

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            viewModel.setRawText(initialSharedText)
            viewModel.parseMessage(initialSharedText, source = "android_share")
        } else {
            viewModel.checkAndConsumePendingImport()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    val sampleOrderText = """
        سلام وقت بخیر، سفارش جدید دارم:
        نام: علی رضایی
        همراه: 09121234567
        آدرس: تهران، خیابان آزادی، پلاک ۱۲، واحد ۴
        اقلام:
        ۲ عدد قهوه اسپرسو
        ۱ بسته چای لاهیجان
        پرداخت: کارت به کارت انجام شد
    """.trimIndent()

    CompositionLocalProvider(LocalLayoutDirection provides if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "ثبت سفارش از شبکه‌های اجتماعی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "شعبه: ${tenantState.activeStoreId.ifBlank { "پیش‌فرض" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(RestoSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
            ) {
                // Section 1: Message Input Card
                RestoCard(
                    variant = RestoCardVariant.Filled,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "ورود متن پیام سفارش",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(RestoSpacing.xs))
                    Text(
                        text = "متن پیام دریافتی از اینستاگرام، تلگرام یا واتس‌اپ را اینجا قرار دهید تا به فاکتور فروش تبدیل شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(RestoSpacing.sm))

                    // Source Selection Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                    ) {
                        val sources = listOf(
                            "INSTAGRAM" to "اینستاگرام",
                            "TELEGRAM" to "تلگرام",
                            "WHATSAPP" to "واتس‌اپ",
                            "MANUAL_PASTE" to "دستی"
                        )
                        sources.forEach { (key, label) ->
                            FilterChip(
                                selected = uiState.source.equals(key, ignoreCase = true),
                                onClick = { viewModel.setSource(key) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(RestoSpacing.sm))

                    // Text Field
                    OutlinedTextField(
                        value = uiState.rawText,
                        onValueChange = { viewModel.setRawText(it) },
                        placeholder = {
                            Text(
                                "متن پیام مشتری را اینجا بچسبانید یا تایپ کنید...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RestoShapes.medium
                    )

                    Spacer(modifier = Modifier.height(RestoSpacing.sm))

                    // Quick action bar: Paste, Sample, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                    ) {
                        RestoButton(
                            text = "چسباندن از حافظه",
                            leadingIcon = Icons.Rounded.ContentPaste,
                            variant = RestoButtonVariant.Outlined,
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    viewModel.pasteFromClipboard(clip)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        RestoButton(
                            text = "نمونه پیام",
                            variant = RestoButtonVariant.Outlined,
                            onClick = { viewModel.setRawText(sampleOrderText) },
                            modifier = Modifier.weight(0.8f)
                        )

                        if (uiState.rawText.isNotBlank()) {
                            RestoButton(
                                text = "پاک‌سازی",
                                leadingIcon = Icons.Rounded.Clear,
                                variant = RestoButtonVariant.Text,
                                onClick = { viewModel.clearDraft() },
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(RestoSpacing.sm))

                    // Parse Action Button
                    RestoButton(
                        text = if (uiState.isParsing) "در حال پردازش و استخراج اقلام..." else "استخراج و تحلیل سفارش",
                        leadingIcon = Icons.Rounded.Search,
                        variant = RestoButtonVariant.Primary,
                        isLoading = uiState.isParsing,
                        enabled = !uiState.isParsing && uiState.rawText.isNotBlank(),
                        fullWidth = true,
                        onClick = { viewModel.parseMessage() }
                    )
                }

                // Section 2: Order Preview & Editing
                val draft = uiState.parsedDraft
                if (draft != null) {
                    // Customer Card
                    RestoCard(
                        variant = RestoCardVariant.Elevated,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(RestoSpacing.xs))
                                Text(
                                    text = "اطلاعات مشتری",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (draft.customer.isNew) {
                                RestoBadge(text = "مشتری جدید", variant = RestoBadgeVariant.Info)
                            } else {
                                RestoBadge(text = "مشتری ثبت‌شده", variant = RestoBadgeVariant.Success)
                            }
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        OutlinedTextField(
                            value = draft.customer.name ?: "",
                            onValueChange = { viewModel.updateCustomerName(it) },
                            label = { Text("نام مشتری") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RestoShapes.small,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.xs))

                        OutlinedTextField(
                            value = draft.customer.phone ?: "",
                            onValueChange = { viewModel.updateCustomerPhone(it) },
                            label = { Text("شماره تماس") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RestoShapes.small,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.xs))

                        OutlinedTextField(
                            value = draft.customer.address ?: "",
                            onValueChange = { viewModel.updateCustomerAddress(it) },
                            label = { Text("آدرس تحویل") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RestoShapes.small,
                            maxLines = 2
                        )
                    }

                    // Extracted Items Card
                    RestoCard(
                        variant = RestoCardVariant.Elevated,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(RestoSpacing.xs))
                                Text(
                                    text = "اقلام استخراج‌شده (${draft.items.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (draft.hasUnmatchedItems) {
                                RestoBadge(
                                    text = "نیاز به تطبیق کالا",
                                    variant = RestoBadgeVariant.Warning,
                                    icon = Icons.Rounded.Warning
                                )
                            } else {
                                RestoBadge(
                                    text = "تمام اقلام تطبیق‌یافته",
                                    variant = RestoBadgeVariant.Success,
                                    icon = Icons.Rounded.CheckCircle
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        if (draft.items.isEmpty()) {
                            Text(
                                text = "هیچ کالایی استخراج نشد. می‌توانید کالاها را دستی اضافه کنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            draft.items.forEachIndexed { index, item ->
                                ItemRowView(
                                    item = item,
                                    currency = currencyUnit,
                                    onQuantityChange = { q -> viewModel.updateItemQuantity(index, q) },
                                    onDelete = { viewModel.removeItem(index) },
                                    onMatchClick = { viewModel.openProductMatchingDialog(index) }
                                )
                                if (index < draft.items.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = RestoSpacing.xs),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        // Button to add item manually from catalog
                        RestoButton(
                            text = "افزودن کالای دیگر از کاتالوگ",
                            leadingIcon = Icons.Rounded.Add,
                            variant = RestoButtonVariant.Outlined,
                            fullWidth = true,
                            onClick = { viewModel.openProductMatchingDialog(null) }
                        )
                    }

                    // Order Summary & Fulfillment Card
                    RestoCard(
                        variant = RestoCardVariant.Elevated,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "تنظیمات فاکتور و انبار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        // Warehouse Selection
                        WarehouseDropdown(
                            warehouses = warehouses,
                            selectedId = uiState.selectedWarehouseId,
                            onSelect = { viewModel.setWarehouseId(it) }
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        // Payment Method Selection Chips
                        Text(
                            text = "روش پرداخت:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                        ) {
                            listOf(
                                PaymentMethod.CASH to "نقدی",
                                PaymentMethod.CARD to "کارتخوان",
                                PaymentMethod.MOBILE_PAYMENT to "کارت به کارت / QR",
                                PaymentMethod.STORE_CREDIT to "نسیه"
                            ).forEach { (method, label) ->
                                FilterChip(
                                    selected = uiState.selectedPaymentMethod == method,
                                    onClick = { viewModel.setPaymentMethod(method) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        // Price Calculation Breakdown
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RestoShapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(RestoSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(RestoSpacing.xxs)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("جمع جزء اقلام:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${PersianFormatter.formatTomans(draft.subtotal)} $currencyUnit",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (draft.discountAmount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("تخفیف:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    Text(
                                        "-${PersianFormatter.formatTomans(draft.discountAmount)} $currencyUnit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (draft.taxAmount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("مالیات (${draft.taxRate.toInt()}%):", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "+${PersianFormatter.formatTomans(draft.taxAmount)} $currencyUnit",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = RestoSpacing.xxs),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "مبلغ نهایی فاکتور:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${PersianFormatter.formatTomans(draft.grandTotal)} $currencyUnit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.md))

                        // Unmatched Warning Banner if any
                        if (draft.hasUnmatchedItems) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RestoShapes.small)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(RestoSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(RestoSpacing.xs))
                                Text(
                                    text = "تطبیق تمام اقلام با کاتالوگ انبار قبل از ثبت فاکتور الزامی است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(RestoSpacing.sm))
                        }

                        // Final Confirmation Button
                        RestoButton(
                            text = if (uiState.isCheckingOut) "در حال ثبت فاکتور در سیستم..." else "تأیید نهایی و صدور فاکتور فروش",
                            leadingIcon = Icons.Rounded.CheckCircle,
                            variant = RestoButtonVariant.Primary,
                            isLoading = uiState.isCheckingOut,
                            enabled = !uiState.isCheckingOut && !draft.hasUnmatchedItems && draft.items.isNotEmpty(),
                            fullWidth = true,
                            onClick = { viewModel.confirmAndCheckout() }
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.xs))

                        RestoButton(
                            text = "انصراف و لغو پیش‌نویس",
                            variant = RestoButtonVariant.Text,
                            fullWidth = true,
                            onClick = { viewModel.clearDraft() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RestoSpacing.xl))
            }
        }

        // Product Catalog Matching Dialog
        if (uiState.isProductMatchingDialogOpen) {
            ProductCatalogPickerDialog(
                products = catalogProducts,
                warehouseId = uiState.selectedWarehouseId,
                onDismiss = { viewModel.closeProductMatchingDialog() },
                onSelectProduct = { selectedProd ->
                    val idx = uiState.matchingItemIndex
                    if (idx != null) {
                        viewModel.matchProductToItem(idx, selectedProd)
                    } else {
                        viewModel.addProductToDraft(selectedProd)
                        viewModel.closeProductMatchingDialog()
                    }
                }
            )
        }

        // Order Success Dialog
        uiState.orderSuccessNumber?.let { orderNum ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccess() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = RestoTheme.colors.success
                        )
                        Spacer(modifier = Modifier.width(RestoSpacing.xs))
                        Text("سفارش با موفقیت ثبت شد")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                        Text("شماره سفارش صادر شده:")
                        Text(
                            text = orderNum,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "سفارش به سیستم فروشگاه متصل و در صف آماده‌سازی و ارسال قرار گرفت.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissSuccess() }) {
                        Text("متوجه شدم")
                    }
                }
            )
        }
    }
}

@Composable
private fun ItemRowView(
    item: ParsedItem,
    currency: String,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onMatchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RestoSpacing.xxs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!item.sku.isNullOrBlank()) {
                    Text(
                        text = "کد کالا: ${item.sku}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.catalogPrice > 0) {
                    Text(
                        text = "فی: ${PersianFormatter.formatTomans(item.catalogPrice)} $currency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quantity stepper & delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Rounded.Delete else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "کاهش",
                        tint = if (item.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${item.quantity} ${item.unitLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = RestoSpacing.xs)
                )

                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "افزایش",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(RestoSpacing.xxs))

        // Match status / trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isMatched) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RestoBadge(
                        text = "تطبیق با کاتالوگ انبار",
                        variant = RestoBadgeVariant.Success,
                        icon = Icons.Rounded.CheckCircle
                    )
                    Spacer(modifier = Modifier.width(RestoSpacing.xs))
                    TextButton(
                        onClick = onMatchClick,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("تغییر کالا", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RestoBadge(
                        text = "تطبیق‌نیافته",
                        variant = RestoBadgeVariant.Warning,
                        icon = Icons.Rounded.Warning
                    )
                    Spacer(modifier = Modifier.width(RestoSpacing.xs))
                    RestoButton(
                        text = "انتخاب از کاتالوگ",
                        leadingIcon = Icons.Rounded.Link,
                        variant = RestoButtonVariant.Outlined,
                        onClick = onMatchClick
                    )
                }
            }

            val lineTotal = (item.catalogPrice * item.quantity)
            if (lineTotal > 0) {
                Text(
                    text = "${PersianFormatter.formatTomans(lineTotal)} $currency",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WarehouseDropdown(
    warehouses: List<com.braveboy.calcuapp.data.model.Warehouse>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentWh = warehouses.find { it.id == selectedId } ?: warehouses.firstOrNull()

    Column {
        Text("انبار تحویل سفارش:", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(RestoSpacing.xxs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RestoShapes.small)
                .border(1.dp, MaterialTheme.colorScheme.outline, RestoShapes.small)
                .clickable { expanded = true }
                .padding(RestoSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentWh?.name ?: "انتخاب انبار...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                warehouses.forEach { wh ->
                    DropdownMenuItem(
                        text = { Text(wh.name) },
                        onClick = {
                            onSelect(wh.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCatalogPickerDialog(
    products: List<Product>,
    warehouseId: String,
    onDismiss: () -> Unit,
    onSelectProduct: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("انتخاب کالا از کاتالوگ فروشگاه", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجوی نام کالا یا کد SKU...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RestoShapes.medium,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(RestoSpacing.sm))

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "کالایی با این مشخصات در فروشگاه یافت نشد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                    ) {
                        items(filteredProducts) { prod ->
                            val stockInWh = prod.stockQuantityByWarehouse[warehouseId] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RestoShapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onSelectProduct(prod) }
                                    .padding(RestoSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "کد: ${prod.sku} | واحد: ${prod.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${PersianFormatter.formatTomans(prod.price)} تومان",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "موجودی: $stockInWh",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (stockInWh > 0) RestoTheme.colors.success else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}
