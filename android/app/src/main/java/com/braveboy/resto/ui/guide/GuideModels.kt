package com.braveboy.resto.ui.guide

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SpotlightShapeType {
    ROUNDED_RECT,
    CIRCLE
}

data class GuideStep(
    val targetKey: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val shapeType: SpotlightShapeType = SpotlightShapeType.ROUNDED_RECT,
    val cornerRadius: Dp = 16.dp,
    val padding: Dp = 8.dp,
    val verticalOffset: Dp = 0.dp
)

data class ScreenGuideConfig(
    val screenId: String,
    val screenTitleFa: String,
    val screenTitleEn: String,
    val steps: List<GuideStep>
)

data class TargetBounds(
    val key: String,
    val boundsInWindow: Rect
)
