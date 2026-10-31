package com.braveboy.resto.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.ui.theme.RestoTheme

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    trendText: String,
    isPositiveTrend: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPersian: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Value Text (Responsive size to fit cleanly on narrow screens)
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Trend Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val trendColor = if (isPositiveTrend) Color(0xFF16A34A) else Color(0xFFDC2626)
                val arrow = if (isPositiveTrend) "↑" else "↓"

                Text(
                    text = "$arrow $trendText",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = trendColor,
                    maxLines = 1
                )

                Text(
                    text = if (isPersian) "نسبت به ماه قبل" else "vs last month",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DashboardStatCardsRow(
    modifier: Modifier = Modifier,
    netProfitValue: String = "۰ تومان",
    netProfitTrend: String = "۰٪",
    totalOrdersValue: String = "۰",
    totalOrdersTrend: String = "۰٪",
    isPersian: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DashboardStatCard(
            title = if (isPersian) "سود خالص" else "Net Profit",
            value = netProfitValue,
            trendText = netProfitTrend,
            isPositiveTrend = true,
            icon = Icons.Rounded.Payments,
            modifier = Modifier.weight(1f),
            isPersian = isPersian
        )

        DashboardStatCard(
            title = if (isPersian) "کل سفارشات" else "Total Orders",
            value = totalOrdersValue,
            trendText = totalOrdersTrend,
            isPositiveTrend = false,
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            modifier = Modifier.weight(1f),
            isPersian = isPersian
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun DashboardStatCardsRowPreview() {
    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(modifier = Modifier.padding(12.dp)) {
                DashboardStatCardsRow(
                    netProfitValue = "۰ تومان",
                    totalOrdersValue = "۰",
                    isPersian = true
                )
            }
        }
    }
}
