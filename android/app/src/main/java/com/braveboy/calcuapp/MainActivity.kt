package com.braveboy.calcuapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.calcuapp.data.intake.PendingImportManager
import com.braveboy.calcuapp.ui.MainAppScreen
import com.braveboy.calcuapp.ui.auth.LoginScreen
import com.braveboy.calcuapp.ui.auth.LoginViewModel
import com.braveboy.calcuapp.ui.theme.CalcuappTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as CalcuappApplication).appContainer

        handleIncomingIntent(intent)

        setContent {
            CalcuappTheme {
                val isSignedIn by appContainer.sessionStore.isSignedIn.collectAsState(initial = false)
                val pendingImport by PendingImportManager.pendingImportFlow.collectAsState()

                LaunchedEffect(Unit) {
                    appContainer.authRepository.restoreSession()
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isSignedIn) {
                        MainAppScreen(
                            appContainer = appContainer,
                            initialSharedText = pendingImport?.rawText
                        )
                    } else {
                        val loginViewModel: LoginViewModel = viewModel(
                            factory = LoginViewModel.Factory(appContainer.authRepository)
                        )
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                // Session state observer reacts automatically;
                                // PendingImportManager preserves pending message
                            }
                        )
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

        val sharedText: String? = when {
            action == Intent.ACTION_SEND && type?.startsWith("text/") == true -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            action == Intent.ACTION_PROCESS_TEXT && type?.startsWith("text/") == true -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            else -> null
        }

        if (!sharedText.isNullOrBlank()) {
            val sourceHint = intent.getStringExtra("android.intent.extra.REFERRER_NAME") ?: "android_share"
            PendingImportManager.setPendingImport(
                rawText = sharedText,
                source = sourceHint
            )
        }
    }
}
