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

val AmberThemeColors = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF0A0A0C),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF16161A),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF222228),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLow = Color(0xFF101014),
    surfaceContainer = Color(0xFF16161A),
    surfaceContainerHigh = Color(0xFF222228),
    surfaceContainerHighest = Color(0xFF33333E),
    outline = Color(0xFF52525B),
    outlineVariant = Color(0xFF27272A)
)

@Preview(name = "Option 3 - Amber Obsidian", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewAmberScreen() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = AmberThemeColors, typography = Typography) {
            DashboardRedesignView()
        }
    }
}
