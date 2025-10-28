package com.cymf.autogame

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.cymf.autogame.bean.LogItem
import com.cymf.autogame.service.AccessibilityServiceMonitor
import com.cymf.autogame.utils.GlobalExceptionHandler
import com.cymf.autogame.utils.YLLogger
import com.hjq.toast.Toaster
import com.topjohnwu.superuser.Shell
import java.util.concurrent.CopyOnWriteArrayList


class App : Application() {

    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create().setInitializers(ExampleInitializer::class.java)
        )
    }


    override fun onCreate() {
        super.onCreate()
        app = this

        Toaster.init(this)
        GlobalExceptionHandler.init()

        // 启动服务监控器
        AccessibilityServiceMonitor.startMonitoring(this)
    }


    companion object {

        private val logList by lazy { CopyOnWriteArrayList<LogItem>() }
        val logLiveData by lazy { MutableLiveData<List<LogItem>>() }

        private val handler = Handler(Looper.getMainLooper())

        @JvmStatic
        lateinit var app: App
            private set


        val context: Context
            get() = app.applicationContext

        @Volatile
        var roundStartTimeNanos: Long = System.nanoTime()
            private set

        fun resetRoundTimer() {
            roundStartTimeNanos = System.nanoTime()
        }

        fun addLog(item: LogItem) {
            val elapsedMs = (System.nanoTime() - roundStartTimeNanos) / 1_000_000
            val timeStr = if (elapsedMs < 1000) {
                "+${elapsedMs}ms"
            } else {
                "+${elapsedMs / 1000}.${(elapsedMs % 1000).toString().padStart(3, '0')}s"
            }
            item.ts = "[$timeStr]"

            logList.add(item)
            if (logList.size > 1000) {
                logList.subList(0, logList.size - 500).clear()
            }
            logLiveData.postValue(logList.takeLast(500))
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

        @JvmStatic
        fun removeCallbacks(runnable: Runnable?) {
            if (runnable == null) {
                handler.removeCallbacksAndMessages(null)
                return
            }
            handler.removeCallbacks(runnable)
        }
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