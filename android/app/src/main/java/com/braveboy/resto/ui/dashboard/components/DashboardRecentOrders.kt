package com.braveboy.resto.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.FulfillmentStatus
import com.braveboy.resto.data.model.OrderItem
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.PaymentStatus
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.ui.components.RestoBadge
import com.braveboy.resto.ui.components.RestoBadgeVariant
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardRecentOrders(
    orders: List<SalesOrder>,
    modifier: Modifier = Modifier,
    currencySymbol: String = stringResource(R.string.currency_unit),
    onSeeAllClick: () -> Unit = {},
    onOrderClick: (SalesOrder) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_orders_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = stringResource(R.string.see_all),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_orders_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                orders.forEach { order ->
                    DashboardSalesOrderCard(
                        order = order,
                        currencySymbol = currencySymbol,
                        onClick = { onOrderClick(order) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSalesOrderCard(
    order: SalesOrder,
    modifier: Modifier = Modifier,
    currencySymbol: String = stringResource(R.string.currency_unit),
    onClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    val dateStr = PersianFormatter.toPersianDigits(dateFormat.format(Date(order.createdAt)))

    val formattedTotal = "${PersianFormatter.formatTomans(order.totalAmount)} $currencySymbol"

    val paymentBadgeVariant = when (order.paymentStatus) {
        PaymentStatus.PAID -> RestoBadgeVariant.Success
        PaymentStatus.PENDING -> RestoBadgeVariant.Warning
        PaymentStatus.PARTIAL -> RestoBadgeVariant.Info
        PaymentStatus.REFUNDED -> RestoBadgeVariant.Error
    }

    val paymentBadgeText = when (order.paymentStatus) {
        PaymentStatus.PAID -> stringResource(R.string.payment_paid)
        PaymentStatus.PENDING -> stringResource(R.string.payment_pending)
        PaymentStatus.PARTIAL -> stringResource(R.string.payment_partial)
        PaymentStatus.REFUNDED -> stringResource(R.string.payment_refunded)
    }

    val fulfillmentBadgeVariant = when (order.fulfillmentStatus) {
        FulfillmentStatus.COMPLETED -> RestoBadgeVariant.Success
        FulfillmentStatus.PROCESSING -> RestoBadgeVariant.Info
        FulfillmentStatus.PENDING -> RestoBadgeVariant.Warning
        FulfillmentStatus.CANCELLED -> RestoBadgeVariant.Error
    }

    val fulfillmentBadgeText = when (order.fulfillmentStatus) {
        FulfillmentStatus.COMPLETED -> stringResource(R.string.fulfillment_completed)
        FulfillmentStatus.PROCESSING -> stringResource(R.string.fulfillment_processing)
        FulfillmentStatus.PENDING -> stringResource(R.string.fulfillment_pending)
        FulfillmentStatus.CANCELLED -> stringResource(R.string.fulfillment_cancelled)
    }

    val channel = SalesChannel.fromCode(order.source)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Order Number & Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Customer Name & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.customer_label, order.customerName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // DEFINED PRODUCTS / ITEMS IN THIS ORDER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.order_defined_items),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (order.items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.items_summary_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    order.items.forEach { item ->
                        val itemFormattedTotal = "${PersianFormatter.formatTomans(item.totalPrice)} $currencySymbol"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${item.productName} (${PersianFormatter.toPersianDigits(item.quantity)} عدد)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = itemFormattedTotal,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RestoBadge(
                    text = channel.titleFa,
                    variant = RestoBadgeVariant.Neutral
                )
                RestoBadge(
                    text = paymentBadgeText,
                    variant = paymentBadgeVariant
                )
                RestoBadge(
                    text = fulfillmentBadgeText,
                    variant = fulfillmentBadgeVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardRecentOrdersPreview() {
    RestoTheme {
        DashboardRecentOrders(
            orders = listOf(
                SalesOrder(
                    id = "ord_1001",
                    orderNumber = "ORD-2026-1001",
                    orgId = "org_1",
                    storeId = "store_1",
                    warehouseId = "wh_1",
                    customerId = "cust_1",
                    customerName = "رضا احمدی",
                    items = listOf(
                        OrderItem(
                            productId = "p1",
                            productName = "پیتزا مخصوص",
                            sku = "SKU-1",
                            unitPrice = 120000.0,
                            quantity = 2,
                            discountPercent = 0.0,
                            taxAmount = 0.0,
                            totalPrice = 240000.0
                        ),
                        OrderItem(
                            productId = "p2",
                            productName = "نوشابه قوطی",
                            sku = "SKU-2",
                            unitPrice = 25000.0,
                            quantity = 2,
                            discountPercent = 0.0,
                            taxAmount = 0.0,
                            totalPrice = 50000.0
                        )
                    ),
                    subtotal = 290000.0,
                    discountAmount = 0.0,
                    taxAmount = 0.0,
                    totalAmount = 290000.0,
                    paymentMethod = PaymentMethod.CARD,
                    paymentStatus = PaymentStatus.PAID,
                    fulfillmentStatus = FulfillmentStatus.COMPLETED,
                    source = SalesChannel.WALK_IN.code,
                    createdAt = System.currentTimeMillis() - 3600000L
                ),
                SalesOrder(
                    id = "ord_1002",
                    orderNumber = "ORD-2026-1002",
                    orgId = "org_1",
                    storeId = "store_1",
                    warehouseId = "wh_1",
                    customerId = "cust_2",
                    customerName = "سارا حسینی",
                    items = listOf(
                        OrderItem(
                            productId = "p3",
                            productName = "همبرگر مخصوص",
                            sku = "SKU-3",
                            unitPrice = 180000.0,
                            quantity = 1,
                            discountPercent = 0.0,
                            taxAmount = 0.0,
                            totalPrice = 180000.0
                        )
                    ),
                    subtotal = 180000.0,
                    discountAmount = 0.0,
                    taxAmount = 0.0,
                    totalAmount = 180000.0,
                    paymentMethod = PaymentMethod.CASH,
                    paymentStatus = PaymentStatus.PENDING,
                    fulfillmentStatus = FulfillmentStatus.PROCESSING,
                    source = SalesChannel.ONLINE.code,
                    createdAt = System.currentTimeMillis() - 7200000L
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardSalesOrderCardPreview() {
    RestoTheme {
        DashboardSalesOrderCard(
            order = SalesOrder(
                id = "ord_1001",
                orderNumber = "ORD-2026-1001",
                orgId = "org_1",
                storeId = "store_1",
                warehouseId = "wh_1",
                customerId = "cust_1",
                customerName = "رضا احمدی",
                items = listOf(
                    OrderItem(
                        productId = "p1",
                        productName = "پیتزا مخصوص",
                        sku = "SKU-1",
                        unitPrice = 120000.0,
                        quantity = 2,
                        discountPercent = 0.0,
                        taxAmount = 0.0,
                        totalPrice = 240000.0
                    ),
                    OrderItem(
                        productId = "p2",
                        productName = "نوشابه قوطی",
                        sku = "SKU-2",
                        unitPrice = 25000.0,
                        quantity = 2,
                        discountPercent = 0.0,
                        taxAmount = 0.0,
                        totalPrice = 50000.0
                    )
                ),
                subtotal = 290000.0,
                discountAmount = 0.0,
                taxAmount = 0.0,
                totalAmount = 290000.0,
                paymentMethod = PaymentMethod.CARD,
                paymentStatus = PaymentStatus.PAID,
                fulfillmentStatus = FulfillmentStatus.COMPLETED,
                source = SalesChannel.WALK_IN.code,
                createdAt = System.currentTimeMillis() - 3600000L
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

