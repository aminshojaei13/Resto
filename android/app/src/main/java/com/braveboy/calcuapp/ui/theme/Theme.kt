package com.braveboy.calcuapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB)
)

/** Warm Dark — mirrors web tokens (warm neutral surfaces, teal primary). */
private val DarkColorScheme = darkColorScheme(
    // Brand
    primary = RestoTeal,
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

    // Canvas & surfaces (warm, layered, NOT pure black)
    background = WarmDarkBackground,
    onBackground = WarmDarkTextPrimary,
    surface = WarmDarkSurface,
    onSurface = WarmDarkTextPrimary,
    surfaceVariant = WarmDarkSurfaceElevated,
    onSurfaceVariant = WarmDarkTextSecondary,
    surfaceDim = WarmDarkBackgroundSecondary,
    surfaceBright = WarmDarkSurfaceHover,
    surfaceContainerLowest = WarmDarkBackground,
    surfaceContainerLow = WarmDarkBackgroundSecondary,
    surfaceContainer = WarmDarkSurface,
    surfaceContainerHigh = WarmDarkSurfaceElevated,
    surfaceContainerHighest = WarmDarkSurfaceHover,

    // Borders & dividers
    outline = WarmDarkBorderStrong,
    outlineVariant = WarmDarkBorder,

    // Extras
    inverseSurface = Color(0xFFF5F1EA),
    inverseOnSurface = Color(0xFF1D1A17),
    inversePrimary = PrimaryLight,
    scrim = Color(0xFF0A0806)
)

@Composable
fun CalcuappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
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
            textMuted = Color(0xFFA1A1AA),
            borderSubtle = Color(0xFF27272A)
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
            textMuted = Color(0xFF71717A),
            borderSubtle = Color(0xFFE4E4E7)
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

object RestoTheme {
    val colors: RestoExtendedColors
        @Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalRestoColors.current
}
