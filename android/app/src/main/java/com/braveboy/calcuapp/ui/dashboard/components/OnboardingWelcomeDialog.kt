package com.braveboy.calcuapp.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoDialog
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing

@Composable
fun OnboardingWelcomeDialog(
    userName: String,
    isPersian: Boolean,
    onStartOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    RestoDialog(
        title = if (isPersian) "به نرم‌افزار رستو (Resto) خوش آمدید" else "Welcome to Resto",
        confirmText = if (isPersian) "شروع مراحل راه‌اندازی" else "Get Started",
        dismissText = if (isPersian) "بعداً یادآوری کن" else "Remind Later",
        onConfirm = onStartOnboarding,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
        ) {
            val welcomeText = if (isPersian) {
                if (userName.isNotBlank()) "سلام $userName گرامی، برای شروع کار با سیستم مدیریت فروش و انبارداری، ۴ مرحله ساده زیر طراحی شده تا به راحتی بر نرم‌افزار مسلط شوید:"
                else "برای شروع کار با سیستم مدیریت فروش و انبارداری، ۴ مرحله ساده زیر طراحی شده تا به راحتی بر نرم‌افزار مسلط شوید:"
            } else {
                "Follow these 4 simple steps to set up your business and learn the workflow:"
            }

            Text(
                text = welcomeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(RestoSpacing.xxs))

            RoadmapStepPreview(
                icon = Icons.Rounded.Inventory2,
                title = if (isPersian) "۱. ثبت اولین کالا و موجودی انبار" else "1. Products & Inventory",
                description = if (isPersian) "تعریف کالاها با قیمت فروش و تخصیص موجودی اولیه" else "Define products and add initial stock levels"
            )

            RoadmapStepPreview(
                icon = Icons.Rounded.People,
                title = if (isPersian) "۲. تأمین‌کنندگان و خرید (اختیاری)" else "2. Suppliers & Purchases",
                description = if (isPersian) "ثبت طرف‌های تجاری تأمین برای مدیریت فاکتورهای خرید" else "Register suppliers for tracking purchase bills"
            )

            RoadmapStepPreview(
                icon = Icons.Rounded.PointOfSale,
                title = if (isPersian) "۳. ثبت اولین فروش و صدور فاکتور" else "3. First Sales Order",
                description = if (isPersian) "ثبت فروش حضوری در صندوق POS یا ثبت هوشمند پیام شبکه‌های اجتماعی" else "Checkout via POS or automatically parse customer chats"
            )

            RoadmapStepPreview(
                icon = Icons.Rounded.LocalShipping,
                title = if (isPersian) "۴. مراحل آماده‌سازی، ارسال و تکمیل" else "4. Order Fulfillment",
                description = if (isPersian) "پیگیری وضعیت سفارشات، شروع آماده‌سازی، ارسال و ثبت تسویه حساب" else "Manage order lifecycle from processing to delivery"
            )
        }
    }
}

@Composable
private fun RoadmapStepPreview(
    icon: ImageVector,
    title: String,
    description: String
) {
    RestoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RestoShapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(RestoSpacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
