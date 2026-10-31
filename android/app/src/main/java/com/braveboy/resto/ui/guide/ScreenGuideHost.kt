package com.braveboy.resto.ui.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun ScreenGuideHost(
    screenId: String,
    guideState: GuideState,
    guidePreferences: GuidePreferences,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    isReplayRequested: Boolean = false,
    onReplayHandled: () -> Unit = {}
) {
    val config = remember(screenId) { ScreenGuideRegistry.getGuideForScreen(screenId) } ?: return
    val scope = rememberCoroutineScope()

    val hasSeenInvitation by guidePreferences.hasSeenInvitation(screenId).collectAsState(initial = true)
    var isInvitationDialogOpen by remember { mutableStateOf(false) }
    var isGuideActive by remember { mutableStateOf(false) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Handle initial first-time screen entrance
    LaunchedEffect(hasSeenInvitation) {
        if (!hasSeenInvitation && !isGuideActive) {
            isInvitationDialogOpen = true
        }
    }

    // Handle replay request
    LaunchedEffect(isReplayRequested) {
        if (isReplayRequested) {
            currentStepIndex = 0
            isGuideActive = true
            isInvitationDialogOpen = false
            onReplayHandled()
        }
    }

    // Invitation Dialog
    if (isInvitationDialogOpen) {
        val title = if (isPersian) config.screenTitleFa else config.screenTitleEn
        GuideInvitationDialog(
            screenTitle = title,
            isPersian = isPersian,
            onShowGuide = {
                isInvitationDialogOpen = false
                currentStepIndex = 0
                isGuideActive = true
                scope.launch {
                    guidePreferences.setInvitationSeen(screenId, true)
                }
            },
            onMaybeLater = {
                isInvitationDialogOpen = false
                scope.launch {
                    guidePreferences.setInvitationSeen(screenId, true)
                }
            }
        )
    }

    // Interactive Spotlight Overlay
    if (isGuideActive) {
        SpotlightOverlay(
            guideConfig = config,
            currentStepIndex = currentStepIndex,
            guideState = guideState,
            isPersian = isPersian,
            onNext = {
                if (currentStepIndex < config.steps.size - 1) {
                    currentStepIndex++
                }
            },
            onPrevious = {
                if (currentStepIndex > 0) {
                    currentStepIndex--
                }
            },
            onSkip = {
                isGuideActive = false
                scope.launch {
                    guidePreferences.setGuideCompleted(screenId, false)
                }
            },
            onFinish = {
                isGuideActive = false
                scope.launch {
                    guidePreferences.setGuideCompleted(screenId, true)
                }
            },
            modifier = modifier
        )
    }
}
