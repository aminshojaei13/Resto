package com.braveboy.calcuapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.util.PersianFormatter
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
                    text = order.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RestoBadge(text = paymentBadgeText, variant = paymentBadgeVariant)
                RestoBadge(text = fulfillmentBadgeText, variant = fulfillmentBadgeVariant)
            }
        }
    }
}
