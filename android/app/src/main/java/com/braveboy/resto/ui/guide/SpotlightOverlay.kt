package com.braveboy.resto.ui.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.util.PersianFormatter

class GuideState {
    var targetBoundsMap by mutableStateOf(mapOf<String, Rect>())
        private set

    fun updateTargetBounds(key: String, bounds: Rect) {
        if (targetBoundsMap[key] != bounds) {
            targetBoundsMap = targetBoundsMap + (key to bounds)
        }
    }
}

@Composable
fun rememberGuideState(): GuideState {
    return remember { GuideState() }
}

fun Modifier.spotlightTarget(key: String, guideState: GuideState): Modifier = this.onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInRoot()
    if (bounds.width > 0 && bounds.height > 0) {
        guideState.updateTargetBounds(key, bounds)
    }
}

@Composable
fun SpotlightOverlay(
    guideConfig: ScreenGuideConfig,
    currentStepIndex: Int,
    guideState: GuideState,
    isPersian: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSteps = guideConfig.steps.size
    if (currentStepIndex !in 0 until totalSteps) return

    val currentStep = guideConfig.steps[currentStepIndex]
    val targetRect = guideState.targetBoundsMap[currentStep.targetKey]

    val density = LocalDensity.current
    val isLastStep = currentStepIndex == totalSteps - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        // Darkened Scrim with Cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw dark overlay
            drawRect(color = Color.Black.copy(alpha = 0.75f))

            // Cutout spotlight target if bounds are measured
            targetRect?.let { bounds ->
                val padPx = with(density) { currentStep.padding.toPx() }
                val vOffsetPx = with(density) { currentStep.verticalOffset.toPx() }
                val cornerRadiusPx = with(density) { currentStep.cornerRadius.toPx() }

                val spotlightRect = Rect(
                    left = bounds.left - padPx,
                    top = bounds.top - padPx + vOffsetPx,
                    right = bounds.right + padPx,
                    bottom = bounds.bottom + padPx + vOffsetPx
                )

                if (currentStep.shapeType == SpotlightShapeType.CIRCLE) {
                    val radius = maxOf(spotlightRect.width, spotlightRect.height) / 2f
                    drawCircle(
                        color = Color.Transparent,
                        radius = radius,
                        center = spotlightRect.center,
                        blendMode = BlendMode.Clear
                    )
                } else {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(spotlightRect.left, spotlightRect.top),
                        size = Size(spotlightRect.width, spotlightRect.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )
                }
            }
        }

        // Tooltip Position Calculation
        val tooltipOffsetY = if (targetRect != null) {
            val screenHeight = with(density) { 700.dp.toPx() }
            if (targetRect.bottom > screenHeight / 2f) {
                // Target in lower screen half -> show tooltip ABOVE
                with(density) { (targetRect.top - 200.dp.toPx()).coerceAtLeast(40.dp.toPx()).toDp() }
            } else {
                // Target in upper screen half -> show tooltip BELOW
                with(density) { (targetRect.bottom + 20.dp.toPx()).toDp() }
            }
        } else {
            180.dp
        }

        // Animated Tooltip Card
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = tooltipOffsetY)
                .padding(horizontal = 20.dp)
        ) {
            SpotlightTooltipCard(
                step = currentStep,
                stepNumber = currentStepIndex + 1,
                totalSteps = totalSteps,
                isLastStep = isLastStep,
                isPersian = isPersian,
                onNext = onNext,
                onPrevious = onPrevious,
                onSkip = onSkip,
                onFinish = onFinish
            )
        }
    }
}

@Composable
fun SpotlightTooltipCard(
    step: GuideStep,
    stepNumber: Int,
    totalSteps: Int,
    isLastStep: Boolean,
    isPersian: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val title = if (isPersian) step.titleFa else step.titleEn
    val description = if (isPersian) step.descriptionFa else step.descriptionEn

    val stepCountText = if (isPersian) {
        "گام ${PersianFormatter.toPersianDigits(stepNumber)} از ${PersianFormatter.toPersianDigits(totalSteps)}"
    } else {
        "Step $stepNumber of $totalSteps"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Step counter & Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stepCountText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Text(
                    text = if (isPersian) "انصراف" else "Skip",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.clickable { onSkip() }
                )
            }

            // Title & Description
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stepNumber > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .clickable { onPrevious() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isPersian) "قبلی" else "Previous",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .clickable { if (isLastStep) onFinish() else onNext() }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isLastStep) {
                            if (isPersian) "پایان راهنما" else "Finish"
                        } else {
                            if (isPersian) "بعدی ➔" else "Next ➔"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
