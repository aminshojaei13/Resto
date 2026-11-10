package com.braveboy.resto

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.resto.data.intake.PendingImportManager
import com.braveboy.resto.ui.MainAppScreen
import com.braveboy.resto.ui.auth.BusinessSetupScreen
import com.braveboy.resto.ui.auth.BusinessSetupViewModel
import com.braveboy.resto.ui.auth.LoginScreen
import com.braveboy.resto.ui.auth.LoginViewModel
import com.braveboy.resto.ui.components.RestoSplashScreen
import com.braveboy.resto.ui.theme.CircularReveal
import com.braveboy.resto.ui.theme.RestoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as RestoApplication).appContainer

        handleIncomingIntent(intent)

        setContent {
            val isSignedInState =
                appContainer.sessionStore.isSignedIn.collectAsState(initial = null)
            val tenantStateNullable =
                appContainer.tenantPreferences.tenantState.collectAsState(initial = null)
            val isDarkModeState =
                appContainer.tenantPreferences.isDarkMode.collectAsState(initial = null)
            val pendingImport by PendingImportManager.pendingImportFlow.collectAsState()

            var isOnlineLoginRequested by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                appContainer.authRepository.restoreSession()
            }

            val isSignedIn = isSignedInState.value
            val tenantState = tenantStateNullable.value
            val isDarkModeNullable = isDarkModeState.value

            val activeDarkTheme = isDarkModeNullable ?: isSystemInDarkTheme()

            SideEffect {
                val style = if (activeDarkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        Color.WHITE,
                        Color.WHITE
                    )
                }
                enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            }

            CircularReveal(
                expanded = isDarkModeNullable == true,
                animationSpec = tween(durationMillis = 650)
            ) { darkTheme ->
                RestoTheme(darkTheme = darkTheme) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                    ) { paddingValues ->
                        if (isSignedIn == null || tenantState == null || isDarkModeNullable == null) {
                            RestoSplashScreen(
                                modifier = Modifier.padding(paddingValues)
                            )
                        } else {
                            val hasActiveWorkspace =
                                isSignedIn || tenantState.activeOrgId.isNotBlank()

                            if (hasActiveWorkspace && !isOnlineLoginRequested) {
                                MainAppScreen(
                                    modifier = Modifier.padding(paddingValues),
                                    appContainer = appContainer,
                                    initialSharedText = pendingImport?.rawText
                                )
                            } else if (isOnlineLoginRequested) {
                                val loginViewModel: LoginViewModel = viewModel(
                                    factory = LoginViewModel.Factory(appContainer.authRepository)
                                )
                                LoginScreen(
                                    modifier = Modifier.padding(paddingValues),
                                    viewModel = loginViewModel,
                                    onLoginSuccess = {
                                        isOnlineLoginRequested = false
                                    },
                                    onNavigateToBusinessSetup = {
                                        isOnlineLoginRequested = false
                                    }
                                )
                            } else {
                                val setupViewModel: BusinessSetupViewModel = viewModel(
                                    factory = BusinessSetupViewModel.Factory(appContainer.authRepository)
                                )
                                BusinessSetupScreen(
                                    modifier = Modifier.padding(paddingValues),
                                    viewModel = setupViewModel,
                                    onSetupSuccess = {
                                        // Workspace created; reacts automatically
                                    },
                                    onOpenOnlineLogin = {
                                        isOnlineLoginRequested = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        val sharedText: String? = when (action) {
            Intent.ACTION_SEND if type?.startsWith("text/") == true -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }

            Intent.ACTION_PROCESS_TEXT if type?.startsWith("text/") == true -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }

            else -> null
        }

        if (!sharedText.isNullOrBlank()) {
            val sourceHint =
                intent.getStringExtra("android.intent.extra.REFERRER_NAME") ?: "android_share"
            PendingImportManager.setPendingImport(
                rawText = sharedText,
                source = sourceHint
            )
        }
    }
}
