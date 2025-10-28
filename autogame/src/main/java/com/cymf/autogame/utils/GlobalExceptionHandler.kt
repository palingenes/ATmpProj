package com.cymf.autogame.utils

import android.util.Log
import com.cymf.autogame.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GlobalExceptionHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "GlobalExceptionHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private val loggerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // 自己处理异常
        handleUncaughtException(t, e)

        // 延迟一段时间再退出线程，防止协程来不及写文件
        try {
            Thread.sleep(2000)
        } catch (_: InterruptedException) {
        }

        // 把自己也干掉 😢
        defaultHandler?.uncaughtException(t, e)
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        // 输出到 Logcat
        Log.e(TAG, "未捕获异常: 线程 ${thread.name}", throwable)

        // 写入到文件
        loggerScope.launch {
            asyncWriteToFile(throwable)
        }
    }

    private fun asyncWriteToFile(throwable: Throwable) {
        try {
            val folder = File(App.context.filesDir, "crash_logs")
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "crash-$timeStamp.log"
            val file = File(folder, fileName)

            val writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)
            writer.use { out ->
                out.write("发生时间: $timeStamp\n")
                out.write("线程: ${Thread.currentThread().name}\n")
                out.write("异常信息:\n")
                out.write(throwable.stackTraceToString())
            }

            Log.d(TAG, "异常日志已保存至: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "写入异常日志失败", e)
        }
    }
}