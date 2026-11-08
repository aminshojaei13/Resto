package com.braveboy.resto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.braveboy.resto.ui.dashboard.components.DashboardRedesignView

val VioletThemeColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFDDD6FE),
    secondary = Color(0xFFEC4899),
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFBCFE8),
    background = Color(0xFF0F0C20),
    onBackground = Color(0xFFF3F0FF),
    surface = Color(0xFF191530),
    onSurface = Color(0xFFF3F0FF),
    surfaceVariant = Color(0xFF251F45),
    onSurfaceVariant = Color(0xFFA78BFA),
    surfaceContainerLow = Color(0xFF141029),
    surfaceContainer = Color(0xFF191530),
    surfaceContainerHigh = Color(0xFF251F45),
    surfaceContainerHighest = Color(0xFF362F5A),
    outline = Color(0xFF4C3F7A),
    outlineVariant = Color(0xFF251F45)
)

@Preview(name = "Option 2 - Violet Velvet", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewVioletScreen() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = VioletThemeColors, typography = Typography) {
            DashboardRedesignView()
        }
    }
}
