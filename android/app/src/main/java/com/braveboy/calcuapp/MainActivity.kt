package com.braveboy.calcuapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.calcuapp.ui.MainAppScreen
import com.braveboy.calcuapp.ui.auth.LoginScreen
import com.braveboy.calcuapp.ui.auth.LoginViewModel
import com.braveboy.calcuapp.ui.theme.CalcuappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as CalcuappApplication).appContainer

        var sharedText: String? = null
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        setContent {
            CalcuappTheme {
                val isSignedIn by appContainer.sessionStore.isSignedIn.collectAsState(initial = false)

                LaunchedEffect(Unit) {
                    appContainer.authRepository.restoreSession()
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isSignedIn) {
                        MainAppScreen(
                            appContainer = appContainer,
                            initialSharedText = sharedText
                        )
                    } else {
                        val loginViewModel: LoginViewModel = viewModel(
                            factory = LoginViewModel.Factory(appContainer.authRepository)
                        )
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                // Session state observer reacts automatically
                            }
                        )
                    }
                }
            }
        }
    }
}
