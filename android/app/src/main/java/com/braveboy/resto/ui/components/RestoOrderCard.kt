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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.braveboy.resto.data.model.FulfillmentStatus
import com.braveboy.resto.data.model.PaymentStatus
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.data.model.SalesOrder
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

    val itemsSummaryStr = if (order.items.isNotEmpty()) {
        val names = order.items.take(2).joinToString("، ") { it.productName }
        val count = order.items.sumOf { it.quantity }
        if (order.items.size > 2) "$names (+${order.items.size - 2} کالا) • مجموع $count عدد" else "$names • $count عدد"
    } else {
        if (isPersian) "بدون اقلام ریز" else "No items listed"
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
                    RestoBadge(text = if (isPersian) channel.titleFa else channel.titleEn, variant = RestoBadgeVariant.Neutral)
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
