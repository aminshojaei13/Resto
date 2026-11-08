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

// ============================================================================
// Option 1: Emerald Neon Slate (زمردی نئون و اسلیت تیره)
// ============================================================================
val EmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF06B6D4),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF151D2A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainer = Color(0xFF151D2A),
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF334155),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF1E293B)
)

// ============================================================================
// Option 2: Deep Indigo / Violet Velvet (مخمل ارغوانی و نیلی مدرن)
// ============================================================================
val VioletColorScheme = darkColorScheme(
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

// ============================================================================
// Option 3: Luxe Amber & Obsidian (طلا و آبسیدین لوکس)
// ============================================================================
val AmberColorScheme = darkColorScheme(
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

// ============================================================================
// Option 4: Electric Cyan & Deep Space (آبی الکتریک و فضای عمیق)
// ============================================================================
val ElectricCyanColorScheme = darkColorScheme(
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

@Preview(name = "Option 1 - Emerald Neon Slate", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewOption1Emerald() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = EmeraldColorScheme, typography = Typography) {
            DashboardRedesignView()
        }
    }
}

@Preview(name = "Option 2 - Violet Velvet", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewOption2Violet() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = VioletColorScheme, typography = Typography) {
            DashboardRedesignView()
        }
    }
}

@Preview(name = "Option 3 - Luxe Amber Obsidian", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewOption3Amber() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = AmberColorScheme, typography = Typography) {
            DashboardRedesignView()
        }
    }
}

@Preview(name = "Option 4 - Electric Cyan Deep Space", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewOption4Cyan() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = ElectricCyanColorScheme, typography = Typography) {
            DashboardRedesignView()
        }
    }
}
