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

val CyanThemeColors = darkColorScheme(
    primary = Color(0xFF00D2FF),
    onPrimary = Color(0xFF003545),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFB8F3FF),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color(0xFF172554),
    secondaryContainer = Color(0xFF1E40AF),
    onSecondaryContainer = Color(0xFFBFDBFE),
    background = Color(0xFF070A12),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111726),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1C253B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLow = Color(0xFF0C111C),
    surfaceContainer = Color(0xFF111726),
    surfaceContainerHigh = Color(0xFF1C253B),
    surfaceContainerHighest = Color(0xFF2A3654),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF1C253B)
)

@Preview(name = "Option 4 - Electric Cyan", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewCyanScreen() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = CyanThemeColors, typography = Typography) {
            DashboardRedesignView()
        }
    }
}
