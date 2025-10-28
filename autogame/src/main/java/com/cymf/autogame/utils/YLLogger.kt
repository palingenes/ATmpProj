package com.cymf.autogame.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object YLLogger {

    private var logFileName: String = "log_def.log"
    private var writeToFile: Boolean = true

    private var logFile: File? = null
    private val loggerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @SuppressLint("ConstantLocale")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private const val LOG_TAG = "_YL"

    // 获取日志级别字符串
    private fun getLevelString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "U"
        }
    }

    // 设置日志文件名
    fun setLogFileName(name: String) {
        val lastDotIndex = name.lastIndexOf('.')
        val oldLogFileName = logFileName
        logFileName = if (lastDotIndex > 0) name else {
            "${name}.log"
        }
        if (oldLogFileName != logFileName) {
            w("日志文件已切换为 $logFileName")
        }
        resetLogFile()
    }

    // 设置是否写入文件
    fun setWriteToFile(enabled: Boolean) {
        writeToFile = enabled
    }

    // 自动识别调用者的 TAG：完整类名.方法名(文件名:行号)
    private fun generateTag(): String {
        val stackTrace = Thread.currentThread().stackTrace

        // 找出第一个非 YLLogger 和非系统类的堆栈项
        var targetIndex = -1
        for (i in stackTrace.indices) {
            val className = stackTrace[i].className
            if (!className.startsWith("com.cymf.autogame.utils.YLLogger") &&
                !className.startsWith("java.lang.Thread") &&
                !className.startsWith("dalvik.system")
            ) {
                targetIndex = i
                break
            }
        }

        val element = stackTrace.getOrNull(targetIndex) ?: return "UnknownTag"

        val className = element.className
        val methodName = element.methodName
        val fileName = element.fileName ?: "UnknownFile"
        val lineNumber = element.lineNumber

        return "$className.$methodName($fileName:$lineNumber)"
    }

    // 重置日志文件路径
    private fun resetLogFile() {
        try {
            val context = getApplicationContext()
            val folder = File(context.filesDir, "logs")
            if (!folder.exists()) {
                folder.mkdirs()
            }
            logFile = File(folder, logFileName)
        } catch (e: Exception) {
            Log.e("MyLogger", "设置日志文件失败", e)
        }
    }

    // 获取全局上下文（通过反射）
    @SuppressLint("PrivateApi")
    private fun getApplicationContext(): Context {
        return Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Context
    }

    // 初始化日志文件（首次调用时自动初始化）
    private fun initLogFile() {
        if (logFile == null) {
            resetLogFile()
        }
    }

    // 日志级别接口（带 Throwable）
    fun v(message: String, throwable: Throwable? = null) =
        log(Log.VERBOSE, message, throwable)

    fun d(message: String, throwable: Throwable? = null) =
        log(Log.DEBUG, message, throwable)

    fun i(message: String, throwable: Throwable? = null) =
        log(Log.INFO, message, throwable)

    fun w(message: String, throwable: Throwable? = null) =
        log(Log.WARN, message, throwable)

    fun e(message: String?, throwable: Throwable? = null) =
        log(Log.ERROR, message, throwable)

    fun wtf(message: String, throwable: Throwable? = null) =
        log(Log.ASSERT, message, throwable)

    // 支持 String.format 风格的变参
    fun v(format: String, vararg args: Any?) =
        log(Log.VERBOSE, format.format(*args))

    fun d(format: String, vararg args: Any?) =
        log(Log.DEBUG, format.format(*args))

    fun i(format: String, vararg args: Any?) =
        log(Log.INFO, format.format(*args))

    fun w(format: String, vararg args: Any?) =
        log(Log.WARN, format.format(*args))

    fun e(format: String?, vararg args: Any?) =
        log(Log.ERROR, format?.format(*args))

    fun wtf(format: String, vararg args: Any?) =
        log(Log.ASSERT, format.format(*args))

    val pool = mutableListOf<String>()

    // 核心日志处理函数
    private fun log(priority: Int, message: String?, throwable: Throwable? = null) {
        initLogFile()
        val tag = generateTag() // 保留原始 TAG，用于文件写入

        val location = try {
            val stack = Thread.currentThread().stackTrace
            var element: StackTraceElement? = null
            for (i in stack.indices) {
                val className = stack[i].className
                if (!className.startsWith("com.cymf.autogame.utils.YLLogger") &&
                    !className.startsWith("java.lang.Thread") &&
                    !className.startsWith("dalvik.system")
                ) {
                    element = stack[i]
                    break
                }
            }
            element?.let {
                val fileName = it.fileName ?: "UnknownFile"
                val className = it.className.substringAfterLast('.')
                val methodName = it.methodName
                val lineNumber = it.lineNumber
                "[$className.$methodName($fileName:$lineNumber)]"
            } ?: "[Unknown]"
        } catch (_: Exception) {
            "[Unknown]"
        }
        val finalMessage = if (message.isNullOrBlank()) {
            "null"
        } else {
            "$location\n$message"
        }

        // 原始 TAG 输出（Logcat 中可能被截断）
        when (priority) {
            Log.VERBOSE -> Log.v(LOG_TAG, finalMessage)
            Log.DEBUG -> Log.d(LOG_TAG, finalMessage)
            Log.INFO -> Log.i(LOG_TAG, finalMessage)
            Log.WARN -> Log.w(LOG_TAG, finalMessage)
            Log.ERROR -> Log.e(LOG_TAG, finalMessage)
            Log.ASSERT -> Log.wtf(LOG_TAG, finalMessage)
        }

        // 异步写入文件
        if (writeToFile) {
            loggerScope.launch {
                asyncWriteToFile(priority, tag, finalMessage, throwable)
            }
        }
    }

    fun log2(message: String) {
        initLogFile()
        Log.e(LOG_TAG, message)
        if (writeToFile) {
            loggerScope.launch {
                asyncWriteToFile(Log.ERROR, null, message, null)
            }
        }
    }

    fun writeToFile(fileName: String, message: String) {
        loggerScope.launch {
            withContext(Dispatchers.IO) {
                val context = getApplicationContext()
                val folder =
                    File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "nodetree")
                if (!folder.exists()) {
                    folder.mkdirs()
                }
                println("write to $folder")
                FileOutputStream(File(folder, fileName), true).use { fos ->
                    OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                        writer.write(message)
                    }
                }
            }
        }
    }

    // 异步写入日志到文件
    private suspend fun asyncWriteToFile(
        priority: Int,
        tag: String?,
        message: String,  // 已包含 location 前缀
        throwable: Throwable?
    ) {
        val file = logFile ?: return
        try {
            val timestamp = dateFormatter.format(Date())
            val level = getLevelString(priority)
            val logMessage = buildString {
                append("$timestamp [$level] $tag\n")  // 原始 TAG 行
                append(message)                       // 内容（已含 [Class.method(File:line)] 前缀）
                if (throwable != null) {
                    append("\n")
                    append(throwable.stackTraceToString())
                }
                append("\n\n")
            }
            withContext(Dispatchers.IO) {
                FileOutputStream(file, true).use { fos ->
                    OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                        writer.write(logMessage)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MyLogger", "异步写入日志失败", e)
        }
    }

    // 关闭协程作用域（建议在 Application 中调用）
    fun close() {
        loggerScope.cancel()
    }
}
