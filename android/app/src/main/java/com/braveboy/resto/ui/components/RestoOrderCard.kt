package com.braveboy.resto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.braveboy.resto.R
import com.braveboy.resto.data.model.FulfillmentStatus
import com.braveboy.resto.data.model.OrderItem
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.PaymentStatus
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RestoOrderCard(
    order: SalesOrder,
    modifier: Modifier = Modifier,
    currencySymbol: String = stringResource(R.string.currency_unit),
    onClick: (() -> Unit)? = null
) {
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

    val dateFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    val dateStr = PersianFormatter.toPersianDigits(dateFormat.format(Date(order.createdAt)))

    val formattedTotal = "${PersianFormatter.formatTomans(order.totalAmount)} $currencySymbol"

    val itemsSummaryStr = if (order.items.isNotEmpty()) {
        val names = order.items.take(2).joinToString("، ") { "${it.productName} (${PersianFormatter.toPersianDigits(it.quantity)} عدد)" }
        val count = order.items.sumOf { it.quantity }
        if (order.items.size > 2) "$names (+${PersianFormatter.toPersianDigits(order.items.size - 2)} کالا) • مجموع ${PersianFormatter.toPersianDigits(count)} عدد" else names
    } else {
        stringResource(R.string.no_items_listed)
    }

    val channel = SalesChannel.fromCode(order.source)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onClick?.invoke() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Order Number + Channel Badge + Total Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RestoBadge(text = channel.titleFa, variant = RestoBadgeVariant.Neutral)
                }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Clean 1-Line Items Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "📦 $itemsSummaryStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Footer Row: Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RestoBadge(text = paymentBadgeText, variant = paymentBadgeVariant)
                RestoBadge(text = fulfillmentBadgeText, variant = fulfillmentBadgeVariant)
            }
        }
    }
}

@Preview(name = "Resto Order Card Preview", showBackground = true)
@Composable
fun RestoOrderCardPreview() {
    val sampleOrder = SalesOrder(
        orderNumber = "ORD-2026-1001",
        orgId = "org_1",
        storeId = "store_1",
        warehouseId = "wh_1",
        customerName = "رضا احمدی",
        source = "POS",
        items = listOf(
            OrderItem(
                productId = "p1",
                productName = "پیتزا مخصوص",
                sku = "PIZZA-01",
                unitPrice = 180000.0,
                quantity = 2,
                discountPercent = 0.0,
                taxAmount = 0.0,
                totalPrice = 360000.0
            ),
            OrderItem(
                productId = "p2",
                productName = "نوشابه قوطی",
                sku = "DRINK-01",
                unitPrice = 25000.0,
                quantity = 2,
                discountPercent = 0.0,
                taxAmount = 0.0,
                totalPrice = 50000.0
            )
        ),
        subtotal = 410000.0,
        discountAmount = 0.0,
        taxAmount = 0.0,
        totalAmount = 410000.0,
        paymentMethod = PaymentMethod.CASH,
        paymentStatus = PaymentStatus.PAID,
        fulfillmentStatus = FulfillmentStatus.COMPLETED
    )

    RestoTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RestoOrderCard(
                order = sampleOrder
            )
        }
    }
}

