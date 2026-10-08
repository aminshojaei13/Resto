package com.braveboy.calcuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.ui.theme.RestoTheme

enum class RestoBadgeVariant {
    Success,
    Warning,
    Error,
    Info,
    Neutral
}

@Composable
fun RestoBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: RestoBadgeVariant = RestoBadgeVariant.Info,
    icon: ImageVector? = null
) {
    val (backgroundColor, textColor) = when (variant) {
        RestoBadgeVariant.Success -> RestoTheme.colors.successContainer to RestoTheme.colors.success
        RestoBadgeVariant.Warning -> RestoTheme.colors.warningContainer to RestoTheme.colors.warning
        RestoBadgeVariant.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        RestoBadgeVariant.Info -> RestoTheme.colors.infoContainer to RestoTheme.colors.info
        RestoBadgeVariant.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RestoShapes.small)
            .background(backgroundColor)
            .padding(horizontal = RestoSpacing.sm, vertical = RestoSpacing.xxs),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(RestoSpacing.xxs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun RestoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(RestoShapes.full)
            .border(1.dp, borderColor, RestoShapes.full)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = RestoSpacing.md, vertical = RestoSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(RestoSpacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
