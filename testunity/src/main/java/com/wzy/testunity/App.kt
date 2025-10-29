package com.wzy.testunity

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        PlayGamesSdk.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        YLLogger.close()
    }
}