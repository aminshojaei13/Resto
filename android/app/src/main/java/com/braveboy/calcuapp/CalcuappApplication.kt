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

        // Seed initial mock SaaS tenant data if DB is empty
        applicationScope.launch {
            appContainer.tenantRepository.seedInitialData()
        }
    }
}
