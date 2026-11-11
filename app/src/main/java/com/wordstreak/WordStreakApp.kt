package com.wordstreak

import android.app.Application
import com.wordstreak.di.AppContainer

class WordStreakApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
