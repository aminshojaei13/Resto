package com.braveboy.calcuapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.ui.theme.RestoDimensions
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing

enum class RestoButtonVariant {
    Primary,
    Secondary,
    Outlined,
    Destructive,
    Text
}

@Composable
fun RestoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RestoButtonVariant = RestoButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    val buttonModifier = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .defaultMinSize(minHeight = RestoDimensions.buttonHeight)

    when (variant) {
        RestoButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = RestoShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = RestoSpacing.lg, vertical = RestoSpacing.sm),
                modifier = buttonModifier
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.onPrimary)
            }
        }
        RestoButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = RestoShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = RestoSpacing.lg, vertical = RestoSpacing.sm),
                modifier = buttonModifier
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.onSurface)
            }
        }
        RestoButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = RestoShapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = RestoSpacing.lg, vertical = RestoSpacing.sm),
                modifier = buttonModifier
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.primary)
            }
        }
        RestoButtonVariant.Destructive -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = RestoShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = RestoSpacing.lg, vertical = RestoSpacing.sm),
                modifier = buttonModifier
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.onError)
            }
        }
        RestoButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = RestoShapes.medium,
                contentPadding = PaddingValues(horizontal = RestoSpacing.md, vertical = RestoSpacing.xs),
                modifier = buttonModifier
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    contentColor: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(RestoDimensions.iconSmall)
                )
                Spacer(modifier = Modifier.width(RestoSpacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(RestoSpacing.xs))
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(RestoDimensions.iconSmall)
                )
            }
        }
    }
}

@Composable
fun RestoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = RestoDimensions.touchTarget, minHeight = RestoDimensions.touchTarget)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(RestoDimensions.iconMedium)
        )
    }
}
