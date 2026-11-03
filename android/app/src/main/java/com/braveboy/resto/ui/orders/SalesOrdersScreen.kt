package com.braveboy.resto.ui.orders

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.resto.R
import com.braveboy.resto.data.mock.MockDataArchive
import com.braveboy.resto.data.model.FulfillmentStatus
import com.braveboy.resto.data.model.OrderItem
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.PaymentStatus
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.ui.components.RestoBadge
import com.braveboy.resto.ui.components.RestoBadgeVariant
import com.braveboy.resto.ui.components.RestoBottomSheet
import com.braveboy.resto.ui.components.RestoButton
import com.braveboy.resto.ui.components.RestoButtonVariant
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoChip
import com.braveboy.resto.ui.components.RestoEmptyState
import com.braveboy.resto.ui.components.RestoLoadingState
import com.braveboy.resto.ui.components.RestoOrderCard
import com.braveboy.resto.ui.components.RestoSearchField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesOrdersScreen(
    viewModel: SalesOrdersViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPaymentStatus by viewModel.selectedPaymentStatus.collectAsState()
    val selectedFulfillmentStatus by viewModel.selectedFulfillmentStatus.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val selectedOrderForDetail by viewModel.selectedOrderForDetail.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    SalesOrdersScreenContent(
        searchQuery = searchQuery,
        selectedPaymentStatus = selectedPaymentStatus,
        selectedFulfillmentStatus = selectedFulfillmentStatus,
        orders = orders,
        selectedOrderForDetail = selectedOrderForDetail,
        snackbarMessage = snackbarMessage,
        isLoading = isLoading,
        isSaving = isSaving,
        onSearchQueryChange = viewModel::setSearchQuery,
        onStatusFilterChange = viewModel::setStatusFilter,
        onPaymentStatusFilterChange = viewModel::filterPaymentStatus,
        onFulfillmentStatusFilterChange = viewModel::filterFulfillmentStatus,
        onSelectOrderForDetail = viewModel::selectOrderForDetail,
        onUpdateOrderStatus = viewModel::updateOrderStatus,
        onClearSnackbarMessage = viewModel::clearSnackbarMessage,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
fun SalesOrdersScreenContent(
    searchQuery: String,
    selectedPaymentStatus: PaymentStatus?,
    selectedFulfillmentStatus: FulfillmentStatus?,
    orders: List<SalesOrder>,
    selectedOrderForDetail: SalesOrder?,
    snackbarMessage: String?,
    isLoading: Boolean,
    isSaving: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (PaymentStatus?, FulfillmentStatus?) -> Unit,
    onPaymentStatusFilterChange: (PaymentStatus) -> Unit,
    onFulfillmentStatusFilterChange: (FulfillmentStatus) -> Unit,
    onSelectOrderForDetail: (SalesOrder?) -> Unit,
    onUpdateOrderStatus: (orderId: String, paymentStatus: PaymentStatus?, fulfillmentStatus: FulfillmentStatus?) -> Unit,
    onClearSnackbarMessage: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currencyUnit = stringResource(R.string.currency_unit)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = stringResource(R.string.orders_screen_title),
                onNavigationClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // SEARCH BAR
            RestoSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = stringResource(R.string.search_orders_placeholder)
            )

            Spacer(modifier = Modifier.height(RestoSpacing.xs))

            // FILTER TABS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    RestoChip(
                        text = stringResource(R.string.all_orders_chip),
                        selected = selectedPaymentStatus == null && selectedFulfillmentStatus == null,
                        onClick = { onStatusFilterChange(null, null) }
                    )
                }

                item {
                    RestoChip(
                        text = stringResource(R.string.pending_fulfillment_chip),
                        selected = selectedFulfillmentStatus == FulfillmentStatus.PENDING,
                        onClick = { onFulfillmentStatusFilterChange(FulfillmentStatus.PENDING) }
                    )
                }

                item {
                    RestoChip(
                        text = stringResource(R.string.processing_chip),
                        selected = selectedFulfillmentStatus == FulfillmentStatus.PROCESSING,
                        onClick = { onFulfillmentStatusFilterChange(FulfillmentStatus.PROCESSING) }
                    )
                }

                item {
                    RestoChip(
                        text = stringResource(R.string.completed_chip),
                        selected = selectedFulfillmentStatus == FulfillmentStatus.COMPLETED,
                        onClick = { onFulfillmentStatusFilterChange(FulfillmentStatus.COMPLETED) }
                    )
                }

                item {
                    RestoChip(
                        text = stringResource(R.string.paid_chip),
                        selected = selectedPaymentStatus == PaymentStatus.PAID,
                        onClick = { onPaymentStatusFilterChange(PaymentStatus.PAID) }
                    )
                }

                item {
                    RestoChip(
                        text = stringResource(R.string.pending_payment_chip),
                        selected = selectedPaymentStatus == PaymentStatus.PENDING,
                        onClick = { onPaymentStatusFilterChange(PaymentStatus.PENDING) }
                    )
                }

                item {
                    RestoChip(
                        text = stringResource(R.string.fulfillment_cancelled),
                        selected = selectedFulfillmentStatus == FulfillmentStatus.CANCELLED,
                        onClick = { onFulfillmentStatusFilterChange(FulfillmentStatus.CANCELLED) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // ORDERS LIST
            if (isLoading && orders.isEmpty()) {
                RestoLoadingState(
                    modifier = Modifier.weight(1f),
                    message = stringResource(R.string.loading_orders)
                )
            } else if (orders.isEmpty()) {
                RestoEmptyState(
                    title = stringResource(R.string.no_orders_title),
                    description = stringResource(R.string.no_orders_desc),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(orders, key = { it.id }) { order ->
                        RestoOrderCard(
                            order = order,
                            currencySymbol = currencyUnit,
                            onClick = { onSelectOrderForDetail(order) }
                        )
                    }
                }
            }
        }
    }

    // ORDER DETAIL BOTTOM SHEET
    selectedOrderForDetail?.let { order ->
        OrderDetailSheet(
            order = order,
            currencyUnit = currencyUnit,
            isSaving = isSaving,
            onUpdateStatus = onUpdateOrderStatus,
            onDismiss = { onSelectOrderForDetail(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailSheet(
    order: SalesOrder,
    currencyUnit: String,
    isSaving: Boolean = false,
    onUpdateStatus: (orderId: String, paymentStatus: PaymentStatus?, fulfillmentStatus: FulfillmentStatus?) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    val dateStr = PersianFormatter.toPersianDigits(dateFormat.format(Date(order.createdAt)))

    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = "${stringResource(R.string.order_details_title)} ${order.orderNumber}",
        subtitle = dateStr
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RestoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            // Customer & Sales Channel Info Box
            RestoCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(RestoSpacing.sm))
                        Column {
                            Text(
                                text = "نام مشتری:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = order.customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val channel = com.braveboy.resto.data.model.SalesChannel.fromCode(order.source)
                    RestoBadge(
                        text = channel.titleFa,
                        variant = RestoBadgeVariant.Neutral
                    )
                }
            }

            // Products list
            Text(
                text = stringResource(R.string.order_items),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val qtyText = "${PersianFormatter.toPersianDigits(item.quantity)} عدد × ${PersianFormatter.formatTomans(item.unitPrice)} $currencyUnit"

                        Text(
                            text = qtyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val itemTotalText = "${PersianFormatter.formatTomans(item.totalPrice)} $currencyUnit"

                    Text(
                        text = itemTotalText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Financial Summary
            Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.xxs)) {
                FinancialRow(
                    label = stringResource(R.string.subtotal_label),
                    value = "${PersianFormatter.formatTomans(order.subtotal)} $currencyUnit"
                )
                if (order.discountAmount > 0) {
                    FinancialRow(
                        label = stringResource(R.string.discount_label),
                        value = "-${PersianFormatter.formatTomans(order.discountAmount)} $currencyUnit",
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
                if (order.taxAmount > 0) {
                    FinancialRow(
                        label = stringResource(R.string.tax_label),
                        value = "${PersianFormatter.formatTomans(order.taxAmount)} $currencyUnit"
                    )
                }
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                FinancialRow(
                    label = stringResource(R.string.total_amount_payable),
                    value = "${PersianFormatter.formatTomans(order.totalAmount)} $currencyUnit",
                    isBold = true,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Order Workflow Actions
            Text(
                text = stringResource(R.string.fulfillment_workflow),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                if (order.fulfillmentStatus == FulfillmentStatus.PENDING) {
                    RestoButton(
                        text = stringResource(R.string.start_processing),
                        onClick = {
                            onUpdateStatus(order.id, null, FulfillmentStatus.PROCESSING)
                        },
                        isLoading = isSaving,
                        fullWidth = true
                    )
                } else if (order.fulfillmentStatus == FulfillmentStatus.PROCESSING) {
                    RestoButton(
                        text = stringResource(R.string.complete_and_ship),
                        onClick = {
                            onUpdateStatus(order.id, null, FulfillmentStatus.COMPLETED)
                        },
                        isLoading = isSaving,
                        fullWidth = true
                    )
                } else if (order.fulfillmentStatus == FulfillmentStatus.COMPLETED) {
                    RestoCard(
                        containerColor = RestoTheme.colors.successContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = RestoTheme.colors.success,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(RestoSpacing.xs))
                            Text(
                                text = stringResource(R.string.order_completed_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = RestoTheme.colors.onSuccessContainer
                            )
                        }
                    }
                }

                if (order.paymentStatus != PaymentStatus.PAID) {
                    RestoButton(
                        text = stringResource(R.string.mark_as_paid),
                        onClick = {
                            onUpdateStatus(order.id, PaymentStatus.PAID, null)
                        },
                        isLoading = isSaving,
                        variant = RestoButtonVariant.Outlined,
                        fullWidth = true
                    )
                }
            }

            // Close button
            Spacer(modifier = Modifier.height(RestoSpacing.sm))
            RestoButton(
                text = stringResource(R.string.close_details),
                onClick = onDismiss,
                variant = RestoButtonVariant.Secondary,
                fullWidth = true
            )
        }
    }
}

@Composable
private fun FinancialRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Preview(name = "Financial Row Preview", showBackground = true)
@Composable
private fun FinancialRowPreview() {
    RestoTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RestoSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
            ) {
                FinancialRow(
                    label = "جمع کل",
                    value = "۲۵۰,۰۰۰ تومان"
                )
                FinancialRow(
                    label = "تخفیف",
                    value = "-۲۵,۰۰۰ تومان",
                    valueColor = MaterialTheme.colorScheme.error
                )
                FinancialRow(
                    label = "مالیات",
                    value = "۲۲,۵۰۰ تومان"
                )
                FinancialRow(
                    label = "مبلغ قابل پرداخت",
                    value = "۲۴۷,۵۰۰ تومان",
                    isBold = true,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(name = "Sales Orders Screen Preview", showBackground = true)
@Composable
fun SalesOrdersScreenPreview() {
    RestoTheme {
        SalesOrdersScreenContent(
            searchQuery = "",
            selectedPaymentStatus = null,
            selectedFulfillmentStatus = null,
            orders = MockDataArchive.salesOrders,
            selectedOrderForDetail = null,
            snackbarMessage = null,
            isLoading = false,
            isSaving = false,
            onSearchQueryChange = {},
            onStatusFilterChange = { _, _ -> },
            onPaymentStatusFilterChange = {},
            onFulfillmentStatusFilterChange = {},
            onSelectOrderForDetail = {},
            onUpdateOrderStatus = { _, _, _ -> },
            onClearSnackbarMessage = {}
        )
    }
}

@Preview(name = "Order Detail Sheet Preview", showBackground = true)
@Composable
private fun OrderDetailSheetPreview() {
    RestoTheme {
        OrderDetailSheet(
            order = SalesOrder(
                id = "ord_1001",
                orderNumber = "ORD-2026-1001",
                orgId = "org_1",
                storeId = "store_1",
                warehouseId = "wh_1",
                customerId = "cust_1",
                customerName = "رضا احمدی",
                source = "POS",
                items = listOf(
                    OrderItem(
                        productId = "p1",
                        productName = "پیتزا مخصوص",
                        sku = "SKU-1",
                        unitPrice = 250000.0,
                        quantity = 2,
                        discountPercent = 0.0,
                        taxAmount = 0.0,
                        totalPrice = 500000.0
                    ),
                    OrderItem(
                        productId = "p2",
                        productName = "نوشابه قوطی",
                        sku = "SKU-2",
                        unitPrice = 30000.0,
                        quantity = 2,
                        discountPercent = 0.0,
                        taxAmount = 0.0,
                        totalPrice = 60000.0
                    )
                ),
                subtotal = 560000.0,
                discountAmount = 20000.0,
                taxAmount = 50000.0,
                totalAmount = 590000.0,
                paymentMethod = PaymentMethod.CARD,
                paymentStatus = PaymentStatus.PENDING,
                fulfillmentStatus = FulfillmentStatus.PROCESSING,
                notes = "تحویل حضوری",
                createdAt = System.currentTimeMillis()
            ),
            currencyUnit = "تومان",
            onDismiss = {}
        )
    }
}
