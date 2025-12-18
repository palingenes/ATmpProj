package com.wzy.testunity

import android.app.Application
import android.content.Context
import com.topjohnwu.superuser.Shell

class App : Application() {

    init {
        Shell.enableVerboseLogging = false

        Shell.setDefaultBuilder(
            Shell.Builder.create()
//                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setInitializers(ExampleInitializer::class.java)
                .setTimeout(10)
        )
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onTerminate() {
        super.onTerminate()
        YLLogger.close()
    }

    class ExampleInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            val bashrc = context.resources.openRawResource(R.raw.bashrc)
            shell.newJob().add(bashrc).exec()
            return true
        }
    }
}