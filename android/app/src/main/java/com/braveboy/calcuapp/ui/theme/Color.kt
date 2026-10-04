package com.braveboy.calcuapp.ui.theme

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

// ---- Light theme (legacy values preserved) ----------------------------------------
val PrimaryLight = Color(0xFF12AFC0)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDDF7F9)
val OnPrimaryContainerLight = Color(0xFF087F8C)

val SecondaryLight = Color(0xFF087F8C)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE2F8FA)
val OnSecondaryContainerLight = Color(0xFF05525B)

val TertiaryLight = Color(0xFF10B981)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD1FAE5)
val OnTertiaryContainerLight = Color(0xFF065F46)

val ErrorLight = Color(0xFFEF4444)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF991B1B)

val BackgroundLight = Color(0xFFF4F5F7)
val OnBackgroundLight = Color(0xFF1F2937)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1F2937)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF6B7280)

// Legacy aliases kept so any external references keep compiling.
val PrimaryDark = RestoTeal
val OnPrimaryDark = Color(0xFF003237)
val PrimaryContainerDark = RestoTealContainer
val OnPrimaryContainerDark = OnRestoTealContainer

val SecondaryDark = Color(0xFF9FB4AC)
val OnSecondaryDark = Color(0xFF16211D)
val SecondaryContainerDark = Color(0xFF2C3733)
val OnSecondaryContainerDark = Color(0xFFD3E5DD)

val TertiaryDark = WarmDarkSuccess
val OnTertiaryDark = Color(0xFF003922)
val TertiaryContainerDark = WarmDarkSuccessContainer
val OnTertiaryContainerDark = OnWarmDarkSuccessContainer

val ErrorDark = WarmDarkError
val OnErrorDark = Color(0xFF4A1010)
val ErrorContainerDark = WarmDarkErrorContainer
val OnErrorContainerDark = OnWarmDarkErrorContainer

val BackgroundDark = WarmDarkBackground
val OnBackgroundDark = WarmDarkTextPrimary
val SurfaceDark = WarmDarkSurface
val OnSurfaceDark = WarmDarkTextPrimary
val SurfaceVariantDark = WarmDarkSurfaceElevated
val OnSurfaceVariantDark = WarmDarkTextSecondary
