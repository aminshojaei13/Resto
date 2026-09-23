package com.braveboy.calcuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.braveboy.calcuapp.ui.MainAppScreen
import com.braveboy.calcuapp.ui.theme.CalcuappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as CalcuappApplication).appContainer

        setContent {
            CalcuappTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen(appContainer = appContainer)
                }
            }
        }
    }
}
