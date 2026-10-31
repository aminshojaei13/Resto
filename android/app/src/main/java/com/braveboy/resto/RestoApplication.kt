package com.braveboy.resto

import android.app.Application
import com.braveboy.resto.di.AppContainer

class RestoApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
