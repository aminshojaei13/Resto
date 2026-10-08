package com.braveboy.calcuapp

import android.app.Application
import com.braveboy.calcuapp.di.AppContainer

class CalcuappApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
