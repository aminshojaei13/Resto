package com.braveboy.calcuapp

import android.app.Application
import com.braveboy.calcuapp.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalcuappApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)

        applicationScope.launch {
            // Ensure Room database is completely cleared on launch for clean testing
            appContainer.database.clearAllTables()
            appContainer.tenantRepository.seedInitialData()
        }
    }
}
