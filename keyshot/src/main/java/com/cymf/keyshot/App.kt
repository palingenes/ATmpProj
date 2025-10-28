package com.cymf.keyshot

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.cymf.keyshot.service.AccessibilityServiceMonitor
import com.cymf.keyshot.utils.GlobalExceptionHandler
import com.hjq.toast.Toaster


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this

        Toaster.init(this)
        GlobalExceptionHandler.init()

        // 启动服务监控器
        AccessibilityServiceMonitor.startMonitoring(this)
    }


    companion object {
        private val handler = Handler(Looper.getMainLooper())

        @JvmStatic
        lateinit var app: App
            private set


        val context: Context
            get() = app.applicationContext


        /**
         * 主线程执行
         */
        @JvmStatic
        fun runOnUiThread(runnable: Runnable) {
            handler.post(runnable)
        }

        @JvmStatic
        fun postAtFrontOfQueue(runnable: Runnable) {
            handler.postAtFrontOfQueue(runnable)
        }

        @JvmStatic
        fun runOnUiThread(runnable: Runnable, delay: Long) {
            handler.postDelayed(runnable, delay)
        }
    }
}