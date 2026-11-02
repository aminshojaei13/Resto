package com.braveboy.resto.ui.messages

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.resto.data.intake.ParsedCustomer
import com.braveboy.resto.data.intake.ParsedItem
import com.braveboy.resto.data.intake.ParsedOrderDraft
import com.braveboy.resto.data.model.MessageTemplate
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.model.Warehouse
import com.braveboy.resto.ui.components.RestoBadge
import com.braveboy.resto.ui.components.RestoBadgeVariant
import com.braveboy.resto.ui.components.RestoButton
import com.braveboy.resto.ui.components.RestoButtonVariant
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoCardVariant
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier,
    initialSharedText: String? = null,
    onBackClick: () -> Unit = {}
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val catalogProducts by viewModel.catalogProducts.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val customTemplates by viewModel.customTemplates.collectAsState()

    MessagesScreenContent(
        tenantState = tenantState,
        uiState = uiState,
        catalogProducts = catalogProducts,
        warehouses = warehouses,
        customTemplates = customTemplates,
        modifier = modifier,
        initialSharedText = initialSharedText,
        onBackClick = onBackClick,
        onSetRawText = { viewModel.setRawText(it) },
        onSaveCustomTemplate = { title, text -> viewModel.saveCustomTemplate(title, text) },
        onDeleteCustomTemplate = { viewModel.deleteCustomTemplate(it) },
        onParseMessage = { text, source ->
            if (text != null && source != null) {
                viewModel.parseMessage(text, source)
            } else {
                viewModel.parseMessage()
            }
        },
        onCheckAndConsumePendingImport = { viewModel.checkAndConsumePendingImport() },
        onClearErrorMessage = { viewModel.clearErrorMessage() },
        onSetSource = { viewModel.setSource(it) },
        onPasteFromClipboard = { viewModel.pasteFromClipboard(it) },
        onClearDraft = { viewModel.clearDraft() },
        onUpdateCustomerName = { viewModel.updateCustomerName(it) },
        onUpdateCustomerPhone = { viewModel.updateCustomerPhone(it) },
        onUpdateCustomerAddress = { viewModel.updateCustomerAddress(it) },
        onUpdateItemQuantity = { index, qty -> viewModel.updateItemQuantity(index, qty) },
        onRemoveItem = { viewModel.removeItem(it) },
        onOpenProductMatchingDialog = { viewModel.openProductMatchingDialog(it) },
        onSetWarehouseId = { viewModel.setWarehouseId(it) },
        onSetPaymentMethod = { viewModel.setPaymentMethod(it) },
        onConfirmAndCheckout = { viewModel.confirmAndCheckout() },
        onCloseProductMatchingDialog = { viewModel.closeProductMatchingDialog() },
        onMatchProductToItem = { index, product -> viewModel.matchProductToItem(index, product) },
        onAddProductToDraft = { viewModel.addProductToDraft(it) },
        onDismissSuccess = { viewModel.dismissSuccess() }
    )
}

@Composable
fun MessagesScreenContent(
    tenantState: TenantState,
    uiState: MessagesUiState,
    catalogProducts: List<Product>,
    warehouses: List<Warehouse>,
    customTemplates: List<MessageTemplate> = emptyList(),
    modifier: Modifier = Modifier,
    initialSharedText: String? = null,
    onBackClick: () -> Unit = {},
    onSetRawText: (String) -> Unit = {},
    onSaveCustomTemplate: (title: String, text: String) -> Unit = { _, _ -> },
    onDeleteCustomTemplate: (id: String) -> Unit = {},
    onParseMessage: (text: String?, source: String?) -> Unit = { _, _ -> },
    onCheckAndConsumePendingImport: () -> Unit = {},
    onClearErrorMessage: () -> Unit = {},
    onSetSource: (String) -> Unit = {},
    onPasteFromClipboard: (String) -> Unit = {},
    onClearDraft: () -> Unit = {},
    onUpdateCustomerName: (String) -> Unit = {},
    onUpdateCustomerPhone: (String) -> Unit = {},
    onUpdateCustomerAddress: (String) -> Unit = {},
    onUpdateItemQuantity: (index: Int, quantity: Int) -> Unit = { _, _ -> },
    onRemoveItem: (index: Int) -> Unit = {},
    onOpenProductMatchingDialog: (index: Int?) -> Unit = {},
    onSetWarehouseId: (String) -> Unit = {},
    onSetPaymentMethod: (PaymentMethod) -> Unit = {},
    onConfirmAndCheckout: () -> Unit = {},
    onCloseProductMatchingDialog: () -> Unit = {},
    onMatchProductToItem: (index: Int, product: Product) -> Unit = { _, _ -> },
    onAddProductToDraft: (product: Product) -> Unit = {},
    onDismissSuccess: () -> Unit = {}
) {
    val isPersian = tenantState.language == "fa"
    val currencyUnit = if (isPersian) "تومان" else "$"

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isTemplatePickerOpen by remember { mutableStateOf(false) }
    var isCreateTemplateOpen by remember { mutableStateOf(false) }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            onSetRawText(initialSharedText)
            onParseMessage(initialSharedText, "android_share")
        } else {
            onCheckAndConsumePendingImport()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearErrorMessage()
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
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                RestoTopBar(
                    title = if (isPersian) "سفارش پیامکی" else "Social Orders",
                    onNavigationClick = onBackClick
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
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
                                onClick = { onSetSource(key) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(RestoSpacing.sm))

                    // Text Field
                    OutlinedTextField(
                        value = uiState.rawText,
                        onValueChange = { onSetRawText(it) },
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

                    // Quick action buttons: Paste (full width), Sample & Clear (secondary row)
                    RestoButton(
                        text = if (isPersian) "چسباندن از حافظه" else "Paste from Clipboard",
                        leadingIcon = Icons.Rounded.ContentPaste,
                        variant = RestoButtonVariant.Outlined,
                        compact = true,
                        fullWidth = true,
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                onPasteFromClipboard(clip)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                    ) {
                        RestoButton(
                            text = if (isPersian) "قالب‌های نمونه پیامک" else "SMS Templates",
                            leadingIcon = Icons.Rounded.Description,
                            variant = RestoButtonVariant.Outlined,
                            compact = true,
                            onClick = { isTemplatePickerOpen = true },
                            modifier = Modifier.weight(1.2f)
                        )

                        RestoButton(
                            text = if (isPersian) "+ قالب دلخواه" else "+ Custom",
                            variant = RestoButtonVariant.Outlined,
                            compact = true,
                            onClick = { isCreateTemplateOpen = true },
                            modifier = Modifier.weight(1f)
                        )

                        if (uiState.rawText.isNotBlank()) {
                            RestoButton(
                                text = if (isPersian) "پاک‌سازی" else "Clear",
                                leadingIcon = Icons.Rounded.Clear,
                                variant = RestoButtonVariant.Text,
                                compact = true,
                                onClick = { onClearDraft() },
                                modifier = Modifier.weight(0.8f)
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
                        onClick = { onParseMessage(null, null) }
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
                                RestoBadge(
                                    text = "مشتری ثبت‌شده",
                                    variant = RestoBadgeVariant.Success
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        OutlinedTextField(
                            value = draft.customer.name ?: "",
                            onValueChange = { onUpdateCustomerName(it) },
                            label = { Text("نام مشتری") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RestoShapes.small,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.xs))

                        OutlinedTextField(
                            value = draft.customer.phone ?: "",
                            onValueChange = { onUpdateCustomerPhone(it) },
                            label = { Text("شماره تماس") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RestoShapes.small,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.xs))

                        OutlinedTextField(
                            value = draft.customer.address ?: "",
                            onValueChange = { onUpdateCustomerAddress(it) },
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
                                    onQuantityChange = { q -> onUpdateItemQuantity(index, q) },
                                    onDelete = { onRemoveItem(index) },
                                    onMatchClick = { onOpenProductMatchingDialog(index) }
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
                            onClick = { onOpenProductMatchingDialog(null) }
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

                        // Warehouse Selection (Only for Multi-Warehouse Businesses)
                        if (warehouses.size > 1) {
                            WarehouseDropdown(
                                warehouses = warehouses,
                                selectedId = uiState.selectedWarehouseId,
                                onSelect = { onSetWarehouseId(it) }
                            )
                            Spacer(modifier = Modifier.height(RestoSpacing.sm))
                        }

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
                                    onClick = { onSetPaymentMethod(method) },
                                    label = {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
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
                                    Text(
                                        "تخفیف:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
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
                                    Text(
                                        "مالیات (${draft.taxRate.toInt()}%):",
                                        style = MaterialTheme.typography.bodySmall
                                    )
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
                            onClick = { onConfirmAndCheckout() }
                        )

                        Spacer(modifier = Modifier.height(RestoSpacing.xs))

                        RestoButton(
                            text = "انصراف و لغو پیش‌نویس",
                            variant = RestoButtonVariant.Text,
                            fullWidth = true,
                            onClick = { onClearDraft() }
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
                onDismiss = { onCloseProductMatchingDialog() },
                onSelectProduct = { selectedProd ->
                    val idx = uiState.matchingItemIndex
                    if (idx != null) {
                        onMatchProductToItem(idx, selectedProd)
                    } else {
                        onAddProductToDraft(selectedProd)
                        onCloseProductMatchingDialog()
                    }
                }
            )
        }

        // Order Success Dialog
        uiState.orderSuccessNumber?.let { orderNum ->
            AlertDialog(
                onDismissRequest = { onDismissSuccess() },
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
                    TextButton(onClick = { onDismissSuccess() }) {
                        Text("متوجه شدم")
                    }
                }
            )
        }

        if (isTemplatePickerOpen) {
            MessageTemplatePickerSheet(
                builtInTemplates = DefaultMessageTemplates.builtInTemplates,
                customTemplates = customTemplates,
                onSelectTemplate = { template ->
                    onSetRawText(template.text)
                },
                onDeleteCustomTemplate = onDeleteCustomTemplate,
                onOpenCreateDialog = { isCreateTemplateOpen = true },
                onDismiss = { isTemplatePickerOpen = false },
                isPersian = isPersian
            )
        }

        if (isCreateTemplateOpen) {
            CreateCustomTemplateDialog(
                onSaveTemplate = { title, text ->
                    onSaveCustomTemplate(title, text)
                    onSetRawText(text)
                },
                onDismiss = { isCreateTemplateOpen = false },
                isPersian = isPersian
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
    warehouses: List<Warehouse>,
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
                            val stockInWh = prod.getStockForWarehouse(warehouseId)
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

object DefaultMessageTemplates {
    val builtInTemplates = listOf(
        MessageTemplate(
            id = "template_structured",
            title = "فاکتور ساختاریافته (اینستاگرام/تلگرام)",
            category = "فاکتوری",
            text = """
                سلام وقت بخیر، سفارش جدید دارم:
                نام و نام خانوادگی: علی رضایی
                شماره تماس: 09121234567
                آدرس ارسال: تهران، خیابان آزادی، پلاک ۱۲، واحد ۴
                اقلام سفارش:
                ۲ عدد قهوه اسپرسو
                ۱ بسته چای لاهیجان
                ۳ عدد کیک شکلاتی
                توضیحات: پرداخت کارت به کارت انجام شد
            """.trimIndent()
        ),
        MessageTemplate(
            id = "template_chat",
            title = "چت صمیمانه (واتس‌اپ/دایرکت)",
            category = "چت آنلاین",
            text = """
                سلام وقتتون بخیر
                لطفاً ۳ تا پیتزا مخصوص و ۲ تا نوشابه مشکی بفرستید برای آقای احمدی
                شماره 09181112233
                آدرس: کرج، عظیمیه، میدان اسبی، خیابان دوم پلاک ۱۵
                ممنون از شما
            """.trimIndent()
        ),
        MessageTemplate(
            id = "template_short_sms",
            title = "پیامک کوتاه و سریع",
            category = "پیامک کوتاه",
            text = """
                سفارش:
                ۲ کیلو پسته اکبری
                ۱ کیلو بادام درختی
                گیرنده: خانم علوی 09359876543
                آدرس: شیراز، خیابان زند، کوچه ۸
            """.trimIndent()
        ),
        MessageTemplate(
            id = "template_corporate",
            title = "سفارش عمده / شرکتی",
            category = "عمده و شرکتی",
            text = """
                ثبت سفارش خرید عمده:
                مشتری: شرکت بازرگانی پارس (مدیریت: آقای محمدی)
                تلفن: 02188889999 / 09120001122
                آدرس: اصفهان، شهرک صنعتی جی، خیابان چهارم
                اقلام:
                ۱۰ عدد کابل VGA
                ۵ عدد ماوس بی‌سیم
                نحوه پرداخت: نسیه / حساب دفتری
            """.trimIndent()
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageTemplatePickerSheet(
    builtInTemplates: List<MessageTemplate>,
    customTemplates: List<MessageTemplate>,
    onSelectTemplate: (MessageTemplate) -> Unit,
    onDeleteCustomTemplate: (String) -> Unit,
    onOpenCreateDialog: () -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true
) {
    var selectedCategory by remember { mutableStateOf("همه") }
    val categories =
        listOf("همه", "فاکتوری", "چت آنلاین", "پیامک کوتاه", "عمده و شرکتی", "سفارشی من")

    val allTemplates = (builtInTemplates + customTemplates)
    val filteredTemplates = allTemplates.filter {
        selectedCategory == "همه" || it.category.contains(selectedCategory, ignoreCase = true) ||
                (selectedCategory == "سفارشی من" && it.isUserCustom)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPersian) "قالب‌ها و الگوی نمونه پیامک" else "SMS Message Templates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories.size) { idx ->
                    val cat = categories[idx]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPersian) "قالبی در این دسته‌بندی وجود ندارد." else "No templates in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredTemplates.forEach { template ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (template.isUserCustom) MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.4f
                                ) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = template.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (template.isUserCustom) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isPersian) "قالب شخصی شما" else "Custom",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteCustomTemplate(template.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = template.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = template.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        onSelectTemplate(template)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isPersian) "درج و استفاده از این قالب" else "Use This Template")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenCreateDialog()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPersian) "+ ثبت و تعریف قالب پیامک دلخواه جدید" else "+ Create Custom Template")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CreateCustomTemplateDialog(
    onSaveTemplate: (title: String, text: String) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true
) {
    var titleText by remember { mutableStateOf("") }
    var templateText by remember { mutableStateOf("") }

    RestoDialog(
        title = if (isPersian) "ثبت قالب پیامک دلخواه جدید" else "Create Custom SMS Template",
        confirmText = if (isPersian) "ذخیره قالب" else "Save Template",
        onConfirm = {
            if (titleText.isNotBlank() && templateText.isNotBlank()) {
                onSaveTemplate(titleText.trim(), templateText.trim())
                onDismiss()
            }
        },
        confirmEnabled = titleText.isNotBlank() && templateText.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
            Text(
                text = if (isPersian)
                    "فرمت یا الگوی دلخواه پیامک مشتریان خود را وارد کنید تا همیشه در دسترس شما باشد."
                else
                    "Enter your custom SMS template pattern for quick reuse.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RestoTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = if (isPersian) "عنوان قالب (مثال: الگو واتس‌اپ مغازه)" else "Template Title",
                placeholder = "مثلاً: قالب اختصاصی فروشگاه"
            )

            OutlinedTextField(
                value = templateText,
                onValueChange = { templateText = it },
                label = { Text(if (isPersian) "متن نمونه یا الگوی پیامک" else "Template Text") },
                placeholder = {
                    Text(
                        if (isPersian)
                            "مثال:\nسفارش جدید:\nنام: ...\nتلفن: ...\nاقلام: ...\nآدرس: ..."
                        else
                            "Enter template text..."
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RestoShapes.medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessagesScreenPreview() {
    RestoTheme {
        MessagesScreenContent(
            tenantState = TenantState(
                userName = "علی رضایی",
                language = "fa"
            ),
            uiState = MessagesUiState(
                rawText = "سلام، سفارش ۲ عدد قهوه اسپرسو دارم.",
                source = "INSTAGRAM",
                selectedWarehouseId = "wh_1",
                selectedPaymentMethod = PaymentMethod.CARD,
                parsedDraft = ParsedOrderDraft(
                    source = "INSTAGRAM",
                    rawText = "سلام، سفارش ۲ عدد قهوه اسپرسو دارم.",
                    customer = ParsedCustomer(
                        name = "علی رضایی",
                        phone = "09121234567",
                        address = "تهران، خیابان آزادی، پلاک ۱۲",
                        isNew = true
                    ),
                    items = listOf(
                        ParsedItem(
                            productId = "prod_1",
                            productName = "قهوه اسپرسو",
                            rawText = "۲ عدد قهوه اسپرسو",
                            sku = "ESP-01",
                            quantity = 2,
                            unitLabel = "عدد",
                            catalogPrice = 45000.0,
                            isMatched = true
                        )
                    ),
                    paymentMethod = PaymentMethod.CARD,
                    warehouseId = "wh_1"
                )
            ),
            catalogProducts = listOf(
                Product(
                    id = "prod_1",
                    orgId = "org_1",
                    sku = "ESP-01",
                    barcode = "123456",
                    name = "قهوه اسپرسو",
                    price = 45000.0,
                    costPrice = 30000.0,
                    category = "نوشیدنی",
                    unit = "عدد",
                    stockQuantityByWarehouse = mapOf("wh_1" to 50)
                )
            ),
            warehouses = listOf(
                Warehouse(
                    id = "wh_1",
                    storeId = "store_1",
                    orgId = "org_1",
                    name = "انبار مرکزی",
                    code = "WH01",
                    address = "تهران"
                )
            )
        )
    }
}
