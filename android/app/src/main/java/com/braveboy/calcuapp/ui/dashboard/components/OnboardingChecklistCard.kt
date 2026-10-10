package com.braveboy.calcuapp.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.ui.MainDestination
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.dashboard.OnboardingState
import com.braveboy.calcuapp.ui.dashboard.OnboardingStepItem
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.ui.theme.RestoTheme
import com.braveboy.calcuapp.util.PersianFormatter

@Composable
fun OnboardingChecklistCard(
    state: OnboardingState,
    isPersian: Boolean,
    onNavigate: (destination: MainDestination, openAddProduct: Boolean) -> Unit,
    onSkipSupplierStep: () -> Unit,
    onDismiss: () -> Unit,
    onShowWelcomeDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (state.isDismissed) return

    var isExpanded by remember { mutableStateOf(true) }

    val completedCountStr = if (isPersian) {
        "${PersianFormatter.toPersianDigits(state.completedStepsCount)} از ${PersianFormatter.toPersianDigits(state.totalSteps)}"
    } else {
        "${state.completedStepsCount} of ${state.totalSteps}"
    }

    val percentage = (state.progress * 100).toInt()
    val percentStr = if (isPersian) "${PersianFormatter.toPersianDigits(percentage)}٪" else "$percentage%"

    RestoCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(RestoSpacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
                    ) {
                        Text(
                            text = if (isPersian) "راهنمای شروع به کار با سیستم" else "Getting Started Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.isAllCompleted) {
                            RestoBadge(
                                text = if (isPersian) "تکمیل شد" else "Completed",
                                variant = RestoBadgeVariant.Success
                            )
                        }
                    }
                    Text(
                        text = if (isPersian)
                            "پیشرفت شما: $completedCountStr مرحله ($percentStr)"
                        else
                            "Progress: $completedCountStr steps ($percentStr)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onShowWelcomeDialog != null) {
                    IconButton(
                        onClick = onShowWelcomeDialog,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = if (isPersian) "راهنمای سناریوها" else "Roadmap Guide",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "جمع کردن" else "باز کردن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = if (isPersian) "بستن راهنما" else "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RestoShapes.full),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Step items
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RestoSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
                ) {
                    state.steps.forEach { step ->
                        StepItemCard(
                            step = step,
                            isPersian = isPersian,
                            onNavigate = onNavigate,
                            onSkipSupplierStep = onSkipSupplierStep
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItemCard(
    step: OnboardingStepItem,
    isPersian: Boolean,
    onNavigate: (destination: MainDestination, openAddProduct: Boolean) -> Unit,
    onSkipSupplierStep: () -> Unit
) {
    val stepTitle = if (isPersian) step.titleFa else step.titleEn
    val stepSubtitle = if (isPersian) step.subtitleFa else step.subtitleEn

    val containerColor = if (step.isCompleted) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RestoShapes.medium)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (step.isCompleted) {
                    RestoTheme.colors.borderSubtle
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RestoShapes.medium
            )
            .padding(RestoSpacing.sm)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (step.isCompleted) RestoTheme.colors.success
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (step.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = RestoTheme.colors.onSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = if (isPersian) PersianFormatter.toPersianDigits(step.stepIndex) else "${step.stepIndex}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(RestoSpacing.xs))

                Text(
                    text = stepTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (step.isCompleted) {
                        RestoTheme.colors.textMuted
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )

                if (step.isCompleted) {
                    RestoBadge(
                        text = if (isPersian) "انجام شد" else "Done",
                        variant = RestoBadgeVariant.Success
                    )
                } else if (step.isOptional) {
                    RestoBadge(
                        text = if (isPersian) "اختیاری" else "Optional",
                        variant = RestoBadgeVariant.Neutral
                    )
                }
            }

            if (!step.isCompleted) {
                Text(
                    text = stepSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = RestoSpacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(RestoSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RestoButton(
                        text = if (isPersian) step.primaryActionTextFa else step.primaryActionTextEn,
                        onClick = { onNavigate(step.primaryDestination, step.openAddProduct) }
                    )

                    if (step.secondaryDestination != null && step.secondaryActionTextFa != null) {
                        RestoButton(
                            text = if (isPersian) step.secondaryActionTextFa else (step.secondaryActionTextEn ?: ""),
                            onClick = { onNavigate(step.secondaryDestination, false) },
                            variant = RestoButtonVariant.Outlined
                        )
                    }

                    if (step.canSkip) {
                        TextButton(onClick = onSkipSupplierStep) {
                            Text(
                                text = if (isPersian) "رد شدن" else "Skip",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
