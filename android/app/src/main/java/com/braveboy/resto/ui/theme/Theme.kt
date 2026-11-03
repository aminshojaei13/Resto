package com.braveboy.resto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
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

/** Luxe Amber & Obsidian Dark Theme */
private val DarkColorScheme = darkColorScheme(
    // Brand
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

    // Canvas & surfaces (Luxe Obsidian)
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceDim = LuxeDarkBackgroundSecondary,
    surfaceBright = LuxeDarkSurfaceHover,
    surfaceContainerLowest = LuxeDarkBackground,
    surfaceContainerLow = LuxeDarkBackgroundSecondary,
    surfaceContainer = LuxeDarkSurface,
    surfaceContainerHigh = LuxeDarkSurfaceElevated,
    surfaceContainerHighest = Color(0xFF33333E),

    // Borders & dividers
    outline = LuxeDarkBorderStrong,
    outlineVariant = LuxeDarkBorder,

    // Extras
    inverseSurface = Color(0xFFFAFAFA),
    inverseOnSurface = Color(0xFF16161A),
    inversePrimary = PrimaryLight,
    scrim = Color(0xFF000000)
)

@Composable
private fun animateColorScheme(targetColorScheme: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme {
    val animSpec = androidx.compose.animation.core.tween<Color>(durationMillis = 200)
    return androidx.compose.material3.ColorScheme(
        primary = androidx.compose.animation.animateColorAsState(targetColorScheme.primary, animSpec, label = "primary").value,
        onPrimary = androidx.compose.animation.animateColorAsState(targetColorScheme.onPrimary, animSpec, label = "onPrimary").value,
        primaryContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.primaryContainer, animSpec, label = "primaryContainer").value,
        onPrimaryContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
        inversePrimary = androidx.compose.animation.animateColorAsState(targetColorScheme.inversePrimary, animSpec, label = "inversePrimary").value,
        secondary = androidx.compose.animation.animateColorAsState(targetColorScheme.secondary, animSpec, label = "secondary").value,
        onSecondary = androidx.compose.animation.animateColorAsState(targetColorScheme.onSecondary, animSpec, label = "onSecondary").value,
        secondaryContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        tertiary = androidx.compose.animation.animateColorAsState(targetColorScheme.tertiary, animSpec, label = "tertiary").value,
        onTertiary = androidx.compose.animation.animateColorAsState(targetColorScheme.onTertiary, animSpec, label = "onTertiary").value,
        tertiaryContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.tertiaryContainer, animSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.onTertiaryContainer, animSpec, label = "onTertiaryContainer").value,
        background = androidx.compose.animation.animateColorAsState(targetColorScheme.background, animSpec, label = "background").value,
        onBackground = androidx.compose.animation.animateColorAsState(targetColorScheme.onBackground, animSpec, label = "onBackground").value,
        surface = androidx.compose.animation.animateColorAsState(targetColorScheme.surface, animSpec, label = "surface").value,
        onSurface = androidx.compose.animation.animateColorAsState(targetColorScheme.onSurface, animSpec, label = "onSurface").value,
        surfaceVariant = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceVariant, animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = androidx.compose.animation.animateColorAsState(targetColorScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
        surfaceTint = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceTint, animSpec, label = "surfaceTint").value,
        inverseSurface = androidx.compose.animation.animateColorAsState(targetColorScheme.inverseSurface, animSpec, label = "inverseSurface").value,
        inverseOnSurface = androidx.compose.animation.animateColorAsState(targetColorScheme.inverseOnSurface, animSpec, label = "inverseOnSurface").value,
        error = androidx.compose.animation.animateColorAsState(targetColorScheme.error, animSpec, label = "error").value,
        onError = androidx.compose.animation.animateColorAsState(targetColorScheme.onError, animSpec, label = "onError").value,
        errorContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.errorContainer, animSpec, label = "errorContainer").value,
        onErrorContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.onErrorContainer, animSpec, label = "onErrorContainer").value,
        outline = androidx.compose.animation.animateColorAsState(targetColorScheme.outline, animSpec, label = "outline").value,
        outlineVariant = androidx.compose.animation.animateColorAsState(targetColorScheme.outlineVariant, animSpec, label = "outlineVariant").value,
        scrim = androidx.compose.animation.animateColorAsState(targetColorScheme.scrim, animSpec, label = "scrim").value,
        surfaceBright = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceBright, animSpec, label = "surfaceBright").value,
        surfaceContainer = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceContainer, animSpec, label = "surfaceContainer").value,
        surfaceContainerHigh = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceContainerHigh, animSpec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceContainerHighest, animSpec, label = "surfaceContainerHighest").value,
        surfaceContainerLow = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceContainerLow, animSpec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceContainerLowest, animSpec, label = "surfaceContainerLowest").value,
        surfaceDim = androidx.compose.animation.animateColorAsState(targetColorScheme.surfaceDim, animSpec, label = "surfaceDim").value
    )
}

@Composable
fun RestoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val targetColorScheme = when {
            dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context)
                else androidx.compose.material3.dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

        val animatedColorScheme = animateColorScheme(targetColorScheme)

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

        val animProgress = remember { androidx.compose.animation.core.Animatable(1f) }
        var lastDarkTheme by remember { mutableStateOf(darkTheme) }

        LaunchedEffect(darkTheme) {
            if (darkTheme != lastDarkTheme) {
                animProgress.snapTo(0f)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 450,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
                lastDarkTheme = darkTheme
            }
        }

        androidx.compose.runtime.CompositionLocalProvider(
            LocalRestoColors provides extendedColors
        ) {
            MaterialTheme(
                colorScheme = animatedColorScheme,
                typography = Typography
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            if (animProgress.value < 1f) {
                                val sweepY = size.height * animProgress.value
                                val sweepColor = if (darkTheme) BackgroundDark else WarmLightBackground
                                clipRect(top = 0f, bottom = sweepY, left = 0f, right = size.width) {
                                    drawRect(
                                        color = sweepColor.copy(alpha = 0.40f * (1f - animProgress.value))
                                    )
                                }
                            }
                        }
                ) {
                    content()
                }
            }
        }
    }
}

object RestoTheme {
    val colors: RestoExtendedColors
        @Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalRestoColors.current
}
