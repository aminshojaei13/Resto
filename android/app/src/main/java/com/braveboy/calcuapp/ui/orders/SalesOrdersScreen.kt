package com.braveboy.calcuapp.ui.orders

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoBottomSheet
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoChip
import com.braveboy.calcuapp.ui.components.RestoEmptyState
import com.braveboy.calcuapp.ui.components.RestoOrderCard
import com.braveboy.calcuapp.ui.components.RestoSearchField
import com.braveboy.calcuapp.ui.components.RestoTopBar
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.ui.theme.RestoTheme
import com.braveboy.calcuapp.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrdersScreen(
    viewModel: SalesOrdersViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPaymentStatus by viewModel.selectedPaymentStatus.collectAsState()
    val selectedFulfillmentStatus by viewModel.selectedFulfillmentStatus.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val selectedOrderForDetail by viewModel.selectedOrderForDetail.collectAsState()
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = if (isPersian) "سفارش‌ها و فاکتورهای فروش" else "Orders & Invoices",
                subtitle = if (isPersian) "پیگیری سفارشات آنلاین، فروشگاهی و وضعیت پرداخت" else "Track online and in-store orders"
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

            // SEARCH BAR
            RestoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = if (isPersian) "جستجو با شماره سفارش یا نام مشتری..." else "Search by Order # or Customer..."
            )

            Spacer(modifier = Modifier.height(RestoSpacing.xs))

            // FILTER TABS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    RestoChip(
                        text = if (isPersian) "همه سفارش‌ها" else "All Orders",
                        selected = selectedPaymentStatus == null && selectedFulfillmentStatus == null,
                        onClick = {
                            viewModel.filterPaymentStatus(null)
                            viewModel.filterFulfillmentStatus(null)
                        }
                    )
                }

                item {
                    RestoChip(
                        text = if (isPersian) "پرداخت شده" else "Paid",
                        selected = selectedPaymentStatus == PaymentStatus.PAID,
                        onClick = { viewModel.filterPaymentStatus(PaymentStatus.PAID) }
                    )
                }

                item {
                    RestoChip(
                        text = if (isPersian) "در انتظار پرداخت" else "Pending Payment",
                        selected = selectedPaymentStatus == PaymentStatus.PENDING,
                        onClick = { viewModel.filterPaymentStatus(PaymentStatus.PENDING) }
                    )
                }

                item {
                    RestoChip(
                        text = if (isPersian) "تکمیل شده" else "Completed",
                        selected = selectedFulfillmentStatus == FulfillmentStatus.COMPLETED,
                        onClick = { viewModel.filterFulfillmentStatus(FulfillmentStatus.COMPLETED) }
                    )
                }

                item {
                    RestoChip(
                        text = if (isPersian) "در صف ارسال" else "Processing",
                        selected = selectedFulfillmentStatus == FulfillmentStatus.PROCESSING,
                        onClick = { viewModel.filterFulfillmentStatus(FulfillmentStatus.PROCESSING) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            // ORDERS LIST
            if (orders.isEmpty()) {
                RestoEmptyState(
                    title = if (isPersian) "هنوز سفارشی ثبت نشده است" else "No orders found",
                    description = if (isPersian)
                        "وقتی مشتریان سفارش ثبت کنند یا از بخش فروش ثبت سفارش کنید، اینجا نمایش داده می‌شود."
                    else
                        "Orders created through the store or imported from social media will appear here.",
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
                            isPersian = isPersian,
                            currencySymbol = currencyUnit,
                            onClick = { viewModel.selectOrderForDetail(order) }
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
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            onUpdateStatus = { orderId, payStatus, fulStatus ->
                viewModel.updateOrderStatus(orderId, payStatus, fulStatus)
            },
            onDismiss = { viewModel.selectOrderForDetail(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailSheet(
    order: SalesOrder,
    isPersian: Boolean,
    currencyUnit: String,
    onUpdateStatus: (orderId: String, paymentStatus: PaymentStatus?, fulfillmentStatus: FulfillmentStatus?) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    val dateStr = if (isPersian) {
        PersianFormatter.toPersianDigits(dateFormat.format(Date(order.createdAt)))
    } else {
        dateFormat.format(Date(order.createdAt))
    }

    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isPersian) "جزئیات سفارش ${order.orderNumber}" else "Order Details ${order.orderNumber}",
        subtitle = dateStr
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RestoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            // Customer Info Box
            RestoCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = if (isPersian) "مشتری:" else "Customer:",
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
            }

            // Products list
            Text(
                text = if (isPersian) "اقلام سفارش" else "Order Items",
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
                        val qtyText = if (isPersian)
                            "${PersianFormatter.toPersianDigits(item.quantity)} عدد × ${PersianFormatter.formatTomans(item.unitPrice)} $currencyUnit"
                        else
                            "${item.quantity}x @ $${String.format("%.2f", item.unitPrice)}"

                        Text(
                            text = qtyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val itemTotalText = if (isPersian) {
                        "${PersianFormatter.formatTomans(item.totalPrice)} $currencyUnit"
                    } else {
                        "$${String.format("%.2f", item.totalPrice)}"
                    }

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
                    label = if (isPersian) "جمع اقلام:" else "Subtotal:",
                    value = if (isPersian) "${PersianFormatter.formatTomans(order.subtotal)} $currencyUnit" else "$${String.format("%.2f", order.subtotal)}"
                )
                if (order.discountAmount > 0) {
                    FinancialRow(
                        label = if (isPersian) "تخفیف:" else "Discount:",
                        value = if (isPersian) "-${PersianFormatter.formatTomans(order.discountAmount)} $currencyUnit" else "-$${String.format("%.2f", order.discountAmount)}",
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
                if (order.taxAmount > 0) {
                    FinancialRow(
                        label = if (isPersian) "مالیات بر ارزش افزوده:" else "Tax:",
                        value = if (isPersian) "${PersianFormatter.formatTomans(order.taxAmount)} $currencyUnit" else "$${String.format("%.2f", order.taxAmount)}"
                    )
                }
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                FinancialRow(
                    label = if (isPersian) "مبلغ کل قابل پرداخت:" else "Total Amount:",
                    value = if (isPersian) "${PersianFormatter.formatTomans(order.totalAmount)} $currencyUnit" else "$${String.format("%.2f", order.totalAmount)}",
                    isBold = true,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Order Workflow Actions
            Text(
                text = if (isPersian) "مراحل آماده‌سازی و پردازش" else "Order Fulfillment & Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.xs)) {
                if (order.fulfillmentStatus == FulfillmentStatus.PENDING) {
                    RestoButton(
                        text = if (isPersian) "شروع آماده‌سازی سفارش (پردازش)" else "Start Processing",
                        onClick = {
                            onUpdateStatus(order.id, null, FulfillmentStatus.PROCESSING)
                        },
                        fullWidth = true
                    )
                } else if (order.fulfillmentStatus == FulfillmentStatus.PROCESSING) {
                    RestoButton(
                        text = if (isPersian) "تکمیل و ارسال سفارش به مشتری" else "Complete & Ship Order",
                        onClick = {
                            onUpdateStatus(order.id, null, FulfillmentStatus.COMPLETED)
                        },
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
                                text = if (isPersian) "این سفارش با موفقیت ارسال و تکمیل شده است." else "This order has been completed.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = RestoTheme.colors.onSuccessContainer
                            )
                        }
                    }
                }

                if (order.paymentStatus != PaymentStatus.PAID) {
                    RestoButton(
                        text = if (isPersian) "ثبت تسویه حساب (پرداخت شد)" else "Mark as Paid",
                        onClick = {
                            onUpdateStatus(order.id, PaymentStatus.PAID, null)
                        },
                        variant = RestoButtonVariant.Outlined,
                        fullWidth = true
                    )
                }
            }

            // Close button
            Spacer(modifier = Modifier.height(RestoSpacing.sm))
            RestoButton(
                text = if (isPersian) "بستن جزئیات" else "Close",
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
