package com.braveboy.resto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// ============================================================================
// Resto Compose Theme — Warm Dark design language (P8.3)
// ============================================================================
// Dynamic (Material You) color is intentionally disabled by default so Resto's
// warm-dark + teal identity stays consistent across devices, matching Web.
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    // Warm Light Canvas & Surfaces
    background = WarmLightBackground,
    onBackground = WarmLightTextPrimary,
    surface = WarmLightSurface,
    onSurface = WarmLightTextPrimary,
    surfaceVariant = WarmLightSurfaceElevated,
    onSurfaceVariant = WarmLightTextSecondary,
    surfaceDim = WarmLightBackgroundSecondary,
    surfaceBright = WarmLightSurfaceHover,
    surfaceContainerLowest = WarmLightBackground,
    surfaceContainerLow = WarmLightBackgroundSecondary,
    surfaceContainer = WarmLightSurface,
    surfaceContainerHigh = WarmLightSurfaceElevated,
    surfaceContainerHighest = WarmLightSurfaceHover,

    // Borders & Dividers
    outline = WarmLightBorderStrong,
    outlineVariant = WarmLightBorder
)

/** Dark Theme — Exact Reverse of Light Theme (White primary on Black canvas). */
private val DarkColorScheme = darkColorScheme(
    // Brand (White Primary for dark mode contrast)
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    // Canvas & surfaces
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceDim = BackgroundDark,
    surfaceBright = SurfaceVariantDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = BackgroundDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerHighest = SurfaceVariantDark,

    // Borders & dividers
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),

    // Extras
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = PrimaryLight,
    scrim = Color(0xFF0A0806)
)

@Composable
fun RestoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val colorScheme = when {
            dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context)
                else androidx.compose.material3.dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

        val extendedColors = if (darkTheme) {
            RestoExtendedColors(
                success = RestoSemanticColors.successDark,
                onSuccess = Color(0xFF000000),
                successContainer = RestoSemanticColors.successContainerDark,
                onSuccessContainer = Color(0xFFFFFFFF),
                warning = RestoSemanticColors.warningDark,
                onWarning = Color(0xFF000000),
                warningContainer = RestoSemanticColors.warningContainerDark,
                onWarningContainer = Color(0xFFFFFFFF),
                info = RestoSemanticColors.infoDark,
                onInfo = Color(0xFF000000),
                infoContainer = RestoSemanticColors.infoContainerDark,
                onInfoContainer = Color(0xFFFFFFFF),
                textMuted = WarmDarkTextMuted,
                borderSubtle = WarmDarkBorder
            )
        } else {
            RestoExtendedColors(
                success = RestoSemanticColors.successLight,
                onSuccess = Color(0xFFFFFFFF),
                successContainer = RestoSemanticColors.successContainerLight,
                onSuccessContainer = Color(0xFF000000),
                warning = RestoSemanticColors.warningLight,
                onWarning = Color(0xFFFFFFFF),
                warningContainer = RestoSemanticColors.warningContainerLight,
                onWarningContainer = Color(0xFF000000),
                info = RestoSemanticColors.infoLight,
                onInfo = Color(0xFFFFFFFF),
                infoContainer = RestoSemanticColors.infoContainerLight,
                onInfoContainer = Color(0xFF000000),
                textMuted = WarmLightTextMuted,
                borderSubtle = WarmLightBorder
            )
        }

        androidx.compose.runtime.CompositionLocalProvider(
            LocalRestoColors provides extendedColors
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}

object RestoTheme {
    val colors: RestoExtendedColors
        @Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalRestoColors.current
}
