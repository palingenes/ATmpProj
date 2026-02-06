package com.wzy.reader

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    companion object {
        private val eventList by lazy { mutableListOf<String>() }
        val eventLiveData by lazy { MutableLiveData<List<String>>(eventList) }
        private val handler = Handler(Looper.getMainLooper())

        @JvmStatic
        lateinit var app: App
            private set
        val context: Context
            get() = app.applicationContext

        fun addState(state: String) {
            runOnUiThread {
                eventList.add(0, state)
                eventLiveData.value = eventList
            }
        }

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