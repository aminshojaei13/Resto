package com.braveboy.resto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.braveboy.resto.data.model.FulfillmentStatus
import com.braveboy.resto.data.model.PaymentStatus
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RestoOrderCard(
    order: SalesOrder,
    modifier: Modifier = Modifier,
    isPersian: Boolean = true,
    currencySymbol: String = "تومان",
    onClick: (() -> Unit)? = null
) {
    val paymentBadgeVariant = when (order.paymentStatus) {
        PaymentStatus.PAID -> RestoBadgeVariant.Success
        PaymentStatus.PENDING -> RestoBadgeVariant.Warning
        PaymentStatus.PARTIAL -> RestoBadgeVariant.Info
        PaymentStatus.REFUNDED -> RestoBadgeVariant.Error
    }

    val paymentBadgeText = when (order.paymentStatus) {
        PaymentStatus.PAID -> if (isPersian) "پرداخت شده" else "Paid"
        PaymentStatus.PENDING -> if (isPersian) "در انتظار پرداخت" else "Pending"
        PaymentStatus.PARTIAL -> if (isPersian) "پرداخت ناقص" else "Partial"
        PaymentStatus.REFUNDED -> if (isPersian) "مرجوع شده" else "Refunded"
    }

    val fulfillmentBadgeVariant = when (order.fulfillmentStatus) {
        FulfillmentStatus.COMPLETED -> RestoBadgeVariant.Success
        FulfillmentStatus.PROCESSING -> RestoBadgeVariant.Info
        FulfillmentStatus.PENDING -> RestoBadgeVariant.Warning
        FulfillmentStatus.CANCELLED -> RestoBadgeVariant.Error
    }

    val fulfillmentBadgeText = when (order.fulfillmentStatus) {
        FulfillmentStatus.COMPLETED -> if (isPersian) "تکمیل شده" else "Completed"
        FulfillmentStatus.PROCESSING -> if (isPersian) "در حال پردازش" else "Processing"
        FulfillmentStatus.PENDING -> if (isPersian) "در صف ارسال" else "Pending"
        FulfillmentStatus.CANCELLED -> if (isPersian) "لغو شده" else "Cancelled"
    }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    val dateStr = if (isPersian) {
        PersianFormatter.toPersianDigits(dateFormat.format(Date(order.createdAt)))
    } else {
        dateFormat.format(Date(order.createdAt))
    }

    val formattedTotal = if (isPersian) {
        "${PersianFormatter.formatTomans(order.totalAmount)} $currencySymbol"
    } else {
        "$${String.format("%.2f", order.totalAmount)}"
    }

    RestoCard(
        modifier = modifier,
        variant = RestoCardVariant.Filled,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

            Spacer(modifier = Modifier.height(RestoSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (isPersian) "مشتری:" else "Customer:"} ${order.customerName}",
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

            Spacer(modifier = Modifier.height(RestoSpacing.xs))

            // DEFINED PRODUCTS / ITEMS IN THIS ORDER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isPersian) "اقلام سفارش (کالاهای تعریف‌شده):" else "Order Defined Items:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (order.items.isEmpty()) {
                        Text(
                            text = if (isPersian) "• (بدون جزئیات اقلام)" else "• (No item details)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        order.items.take(3).forEach { item ->
                            val itemTotalText = if (isPersian) {
                                "${PersianFormatter.formatTomans(item.totalPrice)} $currencySymbol"
                            } else {
                                "$${String.format("%.2f", item.totalPrice)}"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${item.productName} (${if (isPersian) PersianFormatter.toPersianDigits(item.quantity) else item.quantity} عدد)",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = itemTotalText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (order.items.size > 3) {
                            Text(
                                text = if (isPersian) "+ ${PersianFormatter.toPersianDigits(order.items.size - 3)} قلم دیگر..." else "+ ${order.items.size - 3} more items...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val channel = SalesChannel.fromCode(order.source)
                RestoBadge(text = if (isPersian) channel.titleFa else channel.titleEn, variant = RestoBadgeVariant.Neutral)
                RestoBadge(text = paymentBadgeText, variant = paymentBadgeVariant)
                RestoBadge(text = fulfillmentBadgeText, variant = fulfillmentBadgeVariant)
            }
        }
    }
}
