package com.braveboy.calcuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.braveboy.calcuapp.ui.MainDestination
import com.braveboy.calcuapp.ui.theme.CalcuappTheme
import com.braveboy.calcuapp.util.PersianFormatter

@Composable
fun RestoBottomNav(
    currentDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    onCenterFabClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPersian: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = (0.5).dp, color = Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Dashboard / Home
                RestoNavItem(
                    title = if (isPersian) "داشبورد" else "Dashboard",
                    icon = Icons.Rounded.Home,
                    isSelected = currentDestination == MainDestination.ACCOUNTING,
                    onClick = { onDestinationSelected(MainDestination.ACCOUNTING) }
                )

                // Tab 2: Orders
                RestoNavItem(
                    title = if (isPersian) "سفارش‌ها" else "Orders",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    isSelected = currentDestination == MainDestination.ORDERS,
                    onClick = { onDestinationSelected(MainDestination.ORDERS) }
                )

                // Tab 3: Center FAB (+) Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .clickable { onCenterFabClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = if (isPersian) "ثبت جدید" else "New Entry",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tab 4: Inventory / Messages
                RestoNavItem(
                    title = if (isPersian) "موجودی" else "Inventory",
                    icon = Icons.Rounded.Inventory,
                    isSelected = currentDestination == MainDestination.INVENTORY,
                    onClick = { onDestinationSelected(MainDestination.INVENTORY) }
                )

                // Tab 5: More
                val isMoreSelected = !listOf(
                    MainDestination.ACCOUNTING,
                    MainDestination.ORDERS,
                    MainDestination.INVENTORY
                ).contains(currentDestination)

                RestoNavItem(
                    title = if (isPersian) "بیشتر" else "More",
                    icon = Icons.Rounded.MoreHoriz,
                    isSelected = isMoreSelected,
                    onClick = onMoreClick
                )
            }
        }
    }
}

@Composable
private fun RestoNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    isPersian: Boolean = true
) {
    val activeColor = Color(0xFF0F172A)
    val inactiveColor = Color(0xFF64748B)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(22.dp)
            )

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPersian) PersianFormatter.toPersianDigits(badgeCount) else "$badgeCount",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor,
            maxLines = 1
        )
    }
}

@Preview(name = "Resto Bottom Nav RTL Preview", showBackground = true, widthDp = 360)
@Composable
fun RestoBottomNavPreview() {
    CalcuappTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            RestoBottomNav(
                currentDestination = MainDestination.ACCOUNTING,
                onDestinationSelected = {},
                onCenterFabClick = {},
                onMoreClick = {},
                isPersian = true
            )
        }
    }
}
