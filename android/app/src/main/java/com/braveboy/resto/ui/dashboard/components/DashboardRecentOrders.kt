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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

data class DashboardOrderItem(
    val orderNumber: String,
    val amount: String,
    val isLikelyFake: Boolean = false,
    val paymentStatus: String = "Unpaid", // Unpaid, Paid, Pending
    val orderTag: String = "New" // New, Processing, Completed
)

@Composable
fun DashboardRecentOrders(
    modifier: Modifier = Modifier,
    orders: List<DashboardOrderItem> = emptyList(),
    isPersian: Boolean = false,
    onSeeAllClick: () -> Unit = {},
    onOrderClick: (DashboardOrderItem) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPersian) "سفارش‌های اخیر" else "Recent Orders",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            if (orders.isNotEmpty()) {
                Text(
                    text = if (isPersian) "مشاهده همه" else "See All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.clickable { onSeeAllClick() }
                )
            }
        }

        // Orders List or Empty State
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isPersian) "هنوز سفارشی ثبت نشده است" else "No orders recorded yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (isPersian) "پس از انجام ثبت سفارش جدید یا دریافت فاکتور، سفارشات در این بخش قرار می‌گیرند." else "Your recent sales orders will appear here.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                orders.forEach { order ->
                    DashboardOrderItemCard(
                        order = order,
                        isPersian = isPersian,
                        onClick = { onOrderClick(order) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardOrderItemCard(
    order: DashboardOrderItem,
    modifier: Modifier = Modifier,
    isPersian: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (order.isLikelyFake) {
                    // Likely Fake Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                            Text(
                                text = if (isPersian) "احتمالاً فیک" else "Likely Fake",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }

                if (order.paymentStatus.isNotBlank()) {
                    // Unpaid Pill
                    val isUnpaid = order.paymentStatus.equals("Unpaid", ignoreCase = true)
                    val bg = if (isUnpaid) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                    val textClr = if (isUnpaid) Color(0xFFDC2626) else Color(0xFF059669)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPersian) {
                                if (isUnpaid) "پرداخت نشده" else "پرداخت شده"
                            } else order.paymentStatus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textClr
                        )
                    }
                }

                if (order.orderTag.isNotBlank()) {
                    // New Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPersian && order.orderTag == "New") "جدید" else order.orderTag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            // Order title & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPersian) PersianFormatter.toPersianDigits(order.orderNumber) else order.orderNumber,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Text(
                    text = order.amount,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardRecentOrdersPreview() {
    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFF9FAFB))
                    .padding(16.dp)
            ) {
                DashboardRecentOrders(
                    orders = emptyList(),
                    isPersian = true
                )
            }
        }
    }
}
