package com.braveboy.calcuapp.ui.dashboard.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.calcuapp.ui.theme.CalcuappTheme

@Composable
fun DashboardQuickActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPersian: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
        }

        Icon(
            imageVector = if (isPersian) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DashboardQuickActions(
    modifier: Modifier = Modifier,
    isPersian: Boolean = false,
    onAddProductClick: () -> Unit = {},
    onImportDropshippingClick: () -> Unit = {},
    onViewOrdersClick: () -> Unit = {},
    onWithdrawMoneyClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
    ) {
        Column {
            DashboardQuickActionRow(
                title = if (isPersian) "افزودن محصول" else "Add Product",
                icon = Icons.Rounded.Add,
                onClick = onAddProductClick,
                isPersian = isPersian
            )

            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

            DashboardQuickActionRow(
                title = if (isPersian) "وارد کردن دراپ‌شیپینگ" else "Import Dropshipping",
                icon = Icons.Rounded.Upload,
                onClick = onImportDropshippingClick,
                isPersian = isPersian
            )

            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

            DashboardQuickActionRow(
                title = if (isPersian) "مشاهده سفارشات" else "View Orders",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                onClick = onViewOrdersClick,
                isPersian = isPersian
            )

            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

            DashboardQuickActionRow(
                title = if (isPersian) "برداشت وجه" else "Withdraw Money",
                icon = Icons.Rounded.NorthEast,
                onClick = onWithdrawMoneyClick,
                isPersian = isPersian
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardQuickActionsPreview() {
    CalcuappTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFF9FAFB))
                    .padding(16.dp)
            ) {
                DashboardQuickActions(isPersian = true)
            }
        }
    }
}
