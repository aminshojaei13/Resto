package com.braveboy.calcuapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object RestoSemanticColors {
    val successLight = Color(0xFF10B981)
    val successDark = Color(0xFF35C98A)
    val successContainerLight = Color(0xFFECFDF5)
    val successContainerDark = Color(0xFF123223)
    
    val warningLight = Color(0xFFF59E0B)
    val warningDark = Color(0xFFE7B85A)
    val warningContainerLight = Color(0xFFFFFBEB)
    val warningContainerDark = Color(0xFF382C13)
    
    val infoLight = Color(0xFF3B82F6)
    val infoDark = Color(0xFF5CA9E6)
    val infoContainerLight = Color(0xFFEFF6FF)
    val infoContainerDark = Color(0xFF132B42)
}

data class RestoExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val textMuted: Color,
    val borderSubtle: Color,
)

val LocalRestoColors = staticCompositionLocalOf {
    RestoExtendedColors(
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified,
        warning = Color.Unspecified,
        onWarning = Color.Unspecified,
        warningContainer = Color.Unspecified,
        onWarningContainer = Color.Unspecified,
        info = Color.Unspecified,
        onInfo = Color.Unspecified,
        infoContainer = Color.Unspecified,
        onInfoContainer = Color.Unspecified,
        textMuted = Color.Unspecified,
        borderSubtle = Color.Unspecified
    )
}
