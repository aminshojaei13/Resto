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

// ---- Luxe Gold & Amber Brand ---------------------------------------------------
val RestoGold = Color(0xFFF59E0B)
val RestoGoldHover = Color(0xFFFBBF24)
val RestoGoldPressed = Color(0xFFD97706)
val RestoGoldContainer = Color(0xFF78350F)
val OnRestoGoldContainer = Color(0xFFFEF3C7)

// ---- Luxe Obsidian Dark surfaces -------------------------------------------------
val LuxeDarkBackground = Color(0xFF0A0A0C)
val LuxeDarkBackgroundSecondary = Color(0xFF101014)
val LuxeDarkSurface = Color(0xFF16161A)
val LuxeDarkSurfaceElevated = Color(0xFF222228)
val LuxeDarkSurfaceHover = Color(0xFF2A2A32)
val LuxeDarkBorder = Color(0xFF27272A)
val LuxeDarkBorderStrong = Color(0xFF52525B)

// ---- Luxe Obsidian Dark text -----------------------------------------------------
val LuxeDarkTextPrimary = Color(0xFFFAFAFA)
val LuxeDarkTextSecondary = Color(0xFFA1A1AA)
val LuxeDarkTextMuted = Color(0xFF71717A)

// ---- Warm Dark surfaces ----------------------------------------------------------
val WarmDarkBackground = Color(0xFF0A0A0C)
val WarmDarkBackgroundSecondary = Color(0xFF101014)
val WarmDarkSurface = Color(0xFF16161A)
val WarmDarkSurfaceElevated = Color(0xFF222228)
val WarmDarkSurfaceHover = Color(0xFF2A2A32)
val WarmDarkBorder = Color(0xFF27272A)
val WarmDarkBorderStrong = Color(0xFF52525B)

// ---- Warm Dark text --------------------------------------------------------------
val WarmDarkTextPrimary = Color(0xFFFAFAFA)
val WarmDarkTextSecondary = Color(0xFFA1A1AA)
val WarmDarkTextMuted = Color(0xFF71717A)

// ---- Warm Dark semantics ---------------------------------------------------------
val WarmDarkSuccess = Color(0xFF10B981)
val WarmDarkSuccessContainer = Color(0xFF064E3B)
val OnWarmDarkSuccessContainer = Color(0xFFA7F3D0)
val WarmDarkWarning = Color(0xFFF59E0B)
val WarmDarkWarningContainer = Color(0xFF78350F)
val OnWarmDarkWarningContainer = Color(0xFFFEF3C7)
val WarmDarkError = Color(0xFFEF4444)
val WarmDarkErrorContainer = Color(0xFF7F1D1D)
val OnWarmDarkErrorContainer = Color(0xFFFEE2E2)
val WarmDarkInfo = Color(0xFF3B82F6)
val WarmDarkInfoContainer = Color(0xFF1E3A8A)
val OnWarmDarkInfoContainer = Color(0xFFBFDBFE)

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

// ---- Dark Theme (Luxe Amber & Obsidian Gold) ------------------------------------
val PrimaryDark = Color(0xFFF59E0B)           // Amber Champagne Gold
val OnPrimaryDark = Color(0xFF451A03)         // Deep Amber Brown
val PrimaryContainerDark = Color(0xFFEA970F)  // Warm Gold Container
val OnPrimaryContainerDark = Color(0xFF504D42)

val SecondaryDark = Color(0xFFFBBF24)
val OnSecondaryDark = Color(0xFF451A03)
val SecondaryContainerDark = Color(0xFF92400E)
val OnSecondaryContainerDark = Color(0xFFFDE68A)

val TertiaryDark = Color(0xFF34D399)
val OnTertiaryDark = Color(0xFF064B33)
val TertiaryContainerDark = Color(0xFF065F46)
val OnTertiaryContainerDark = Color(0xFFD1FAE5)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

val BackgroundDark = Color(0xFF0A0A0C)        // Deep Dark Obsidian Canvas
val OnBackgroundDark = Color(0xFFFAFAFA)       // Crisp Light Text
val SurfaceDark = Color(0xFF16161A)          // Dark Surface for Cards
val OnSurfaceDark = Color(0xFFFAFAFA)         // Crisp Light Text
val SurfaceVariantDark = Color(0xFF222228)
val OnSurfaceVariantDark = Color(0xFFA1A1AA)
