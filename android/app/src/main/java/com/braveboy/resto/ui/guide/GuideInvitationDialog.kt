package com.braveboy.resto.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.Icon
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
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.theme.RestoTheme

@Composable
fun GuideInvitationDialog(
    screenTitle: String,
    isPersian: Boolean,
    onShowGuide: () -> Unit,
    onMaybeLater: () -> Unit
) {
    val titleText = if (isPersian) "آیا مایل به دریافت راهنمای سریع هستید؟" else "Would you like a quick tour?"
    val descText = if (isPersian)
        "با قابلیت‌ها و نحوه استفاده از بخش $screenTitle آشنا شوید."
    else
        "Discover the features and learn how to use the $screenTitle screen."

    RestoDialog(
        title = titleText,
        confirmText = if (isPersian) "مشاهده راهنما" else "Show Guide",
        dismissText = if (isPersian) "بعداً" else "Maybe Later",
        onConfirm = onShowGuide,
        onDismissRequest = onMaybeLater
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = descText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuideInvitationDialogPreview() {
    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            GuideInvitationDialog(
                screenTitle = "داشبورد",
                isPersian = true,
                onShowGuide = {},
                onMaybeLater = {}
            )
        }
    }
}
