package com.braveboy.resto.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Resto Design Language — Light + Warm Dark (P8.3)
// ============================================================================
// Conceptual palette shared with web/src/theme/tokens.ts (CSS implementation).
// Warm Dark = warm neutral surfaces (brown-tinted), Resto teal identity,
// restrained semantic colors. NOT a cold blue-gray Material default.
// ============================================================================

// ---- Shared brand -----------------------------------------------------------------
val RestoTeal = Color(0xFF18B7C7)
val RestoTealHover = Color(0xFF25C7D4)
val RestoTealPressed = Color(0xFF0E8E9C)
val RestoTealContainer = Color(0xFF123337)
val OnRestoTealContainer = Color(0xFF4FC3CF)

// ---- Warm Light surfaces ---------------------------------------------------------
val WarmLightBackground = Color(0xFFFAF8F5)
val WarmLightBackgroundSecondary = Color(0xFFF3EFE7)
val WarmLightSurface = Color(0xFFFFFFFF)
val WarmLightSurfaceElevated = Color(0xFFF7F4EE)
val WarmLightSurfaceHover = Color(0xFFEFECE6)
val WarmLightBorder = Color(0xFFE8E2D8)
val WarmLightBorderStrong = Color(0xFFDCD4C9)

// ---- Warm Light text --------------------------------------------------------------
val WarmLightTextPrimary = Color(0xFF231F1C)
val WarmLightTextSecondary = Color(0xFF635C53)
val WarmLightTextMuted = Color(0xFF8C847B)

// ---- Warm Dark surfaces ----------------------------------------------------------
val WarmDarkBackground = Color(0xFF171513)
val WarmDarkBackgroundSecondary = Color(0xFF1D1A17)
val WarmDarkSurface = Color(0xFF24211E)
val WarmDarkSurfaceElevated = Color(0xFF2B2723)
val WarmDarkSurfaceHover = Color(0xFF322D28)
val WarmDarkBorder = Color(0xFF3A342E)
val WarmDarkBorderStrong = Color(0xFF4D453E)

// ---- Warm Dark text --------------------------------------------------------------
val WarmDarkTextPrimary = Color(0xFFF5F1EA)
val WarmDarkTextSecondary = Color(0xFFC4BDB3)
val WarmDarkTextMuted = Color(0xFF958D83)

// ---- Warm Dark semantics ---------------------------------------------------------
val WarmDarkSuccess = Color(0xFF35C98A)
val WarmDarkSuccessContainer = Color(0xFF123223)
val OnWarmDarkSuccessContainer = Color(0xFF9BE8C8)
val WarmDarkWarning = Color(0xFFE7B85A)
val WarmDarkWarningContainer = Color(0xFF382C13)
val OnWarmDarkWarningContainer = Color(0xFFF4DCA8)
val WarmDarkError = Color(0xFFE36A6A)
val WarmDarkErrorContainer = Color(0xFF3B1A1A)
val OnWarmDarkErrorContainer = Color(0xFFF3B8B8)
val WarmDarkInfo = Color(0xFF5CA9E6)
val WarmDarkInfoContainer = Color(0xFF132B42)
val OnWarmDarkInfoContainer = Color(0xFFB4D9F6)

// ---- Black & White Monochrome Primary System -----------------------------------
val PrimaryLight = Color(0xFF0F172A)          // Pure Obsidian Black
val OnPrimaryLight = Color(0xFFFFFFFF)        // Crisp White
val PrimaryContainerLight = Color(0xFF1E293B) // Dark Slate Container
val OnPrimaryContainerLight = Color(0xFFF8FAFC)

val SecondaryLight = Color(0xFF334155)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF1F5F9)
val OnSecondaryContainerLight = Color(0xFF0F172A)

val TertiaryLight = Color(0xFF10B981)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD1FAE5)
val OnTertiaryContainerLight = Color(0xFF065F46)

val ErrorLight = Color(0xFFEF4444)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF991B1B)

val BackgroundLight = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF64748B)

// ---- Dark Theme (Exact Reverse of Light Theme) --------------------------------
val PrimaryDark = Color(0xFFFFFFFF)           // Crisp White
val OnPrimaryDark = Color(0xFF0F172A)         // Pure Obsidian Black
val PrimaryContainerDark = Color(0xFFF8FAFC)  // Light Container
val OnPrimaryContainerDark = Color(0xFF0F172A)

val SecondaryDark = Color(0xFFCBD5E1)
val OnSecondaryDark = Color(0xFF0F172A)
val SecondaryContainerDark = Color(0xFF1E293B)
val OnSecondaryContainerDark = Color(0xFFF8FAFC)

val TertiaryDark = Color(0xFF34D399)
val OnTertiaryDark = Color(0xFF064B33)
val TertiaryContainerDark = Color(0xFF065F46)
val OnTertiaryContainerDark = Color(0xFFD1FAE5)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

val BackgroundDark = Color(0xFF0F172A)        // Deep Obsidian Canvas
val OnBackgroundDark = Color(0xFFF8FAFC)       // Crisp White Text
val SurfaceDark = Color(0xFF1E293B)          // Dark Slate Surface for Cards
val OnSurfaceDark = Color(0xFFF8FAFC)         // Crisp White Text
val SurfaceVariantDark = Color(0xFF334155)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
