package com.cymf.tmp

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class ShellActivity : AppCompatActivity() {

    private lateinit var commandInput: EditText
    private lateinit var executeBtn: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var outputAdapter: OutputAdapter

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    private var interactiveSession: InteractiveShellSession? = null
    private var isInteractiveMode = false
    private var currentUseRoot = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shell)

        commandInput = findViewById(R.id.commandInput)
        executeBtn = findViewById(R.id.executeBtn)
        recyclerView = findViewById(R.id.recyclerView)

        // 初始化 RecyclerView
        outputAdapter = OutputAdapter()
        recyclerView.adapter = outputAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 设置点击事件
        executeBtn.setOnClickListener {
            executeCommand()
        }

        // 上下键导航历史命令
        commandInput.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        navigateHistoryBackward()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        navigateHistoryForward()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }

        commandInput.setOnEditorActionListener { v, keyCode, event ->
            if (keyCode == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                executeCommand()
                return@setOnEditorActionListener true
            }
            false
        }
        recyclerView.setMaxFlingVelocity()
    }

    private fun appendColoredText(text: String, defaultColor: Int = Color.BLACK) {
        if (text.isNotBlank()) {
            if (outputAdapter.itemCount > 0)  //  加一条分割线，方便阅读
            {
                outputAdapter.addOutput(OutputItem("-------------------------------", Color.DKGRAY))
            }
            outputAdapter.addOutput(OutputItem(text, defaultColor))

            // 自动滚到底部（仅当用户未手动上滑）
            if (isAutoScrollEnabled()) {
                recyclerView.post {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastPosition = outputAdapter.itemCount - 1
                    layoutManager.scrollToPositionWithOffset(lastPosition, 0)
                }
            }
        }
    }

    private fun isAutoScrollEnabled(): Boolean {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val lastVisiblePos = layoutManager.findLastCompletelyVisibleItemPosition()
        return lastVisiblePos == outputAdapter.itemCount - 1 || outputAdapter.itemCount <= 50
    }

    private suspend fun runShellCommand(command: String, useRoot: Boolean = false): ShellResult =
        withContext(Dispatchers.IO) {
            val shellCommand = if (useRoot) "su" else "sh"
            val process = Runtime.getRuntime().exec(shellCommand)

            val stdin = DataOutputStream(process.outputStream)
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            stdin.writeBytes("$command\nexit\n")
            stdin.flush()

            val stdoutJob = async {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stdout.append(line).append("\n")
                }
            }

            val stderrJob = async {
                val reader = BufferedReader(InputStreamReader(process.errorStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stderr.append(line).append("\n")
                }
            }

            awaitAll(stdoutJob, stderrJob)
            process.waitFor()

            ShellResult(stdout.toString(), stderr.toString())
        }

    private fun navigateHistoryBackward() {
        if (commandHistory.isEmpty()) return

        if (historyIndex >= 0) {
            historyIndex--
            if (historyIndex < 0) historyIndex = -1
        } else {
            historyIndex = commandHistory.size - 1
        }
        updateInputWithHistory()
    }

    private fun navigateHistoryForward() {
        if (commandHistory.isEmpty()) return

        if (historyIndex >= 0 && historyIndex < commandHistory.size - 1) {
            historyIndex++
            updateInputWithHistory()
        } else {
            historyIndex = -1
            commandInput.setText("")
        }
    }

    private fun updateInputWithHistory() {
        if (historyIndex >= 0 && historyIndex < commandHistory.size) {
            val cmd = commandHistory[historyIndex]
            commandInput.setText(cmd)
            commandInput.setSelection(cmd.length)
        }
    }

    private fun endInteractiveSession() {
        interactiveSession?.close()
        interactiveSession = null
        isInteractiveMode = false
        appendColoredText("\n🔚 已退出交互式模式。\n")
    }

    private fun executeCommand() {
        val command = commandInput.text.toString().trim()
        if (command.isEmpty()) return

        if (command.lowercase() == "clear" || command.lowercase() == "cls") {
            outputAdapter.clearData()
            return
        }

        lifecycleScope.launch {
            try {
                if (!isInteractiveMode) { // 非交互模式下的逻辑
                    if (command == "su") {
                        isInteractiveMode = true
                        currentUseRoot = true
                        interactiveSession = InteractiveShellSession(currentUseRoot).apply {
                            start()
                        }
                        appendColoredText("✅ 已进入 root 模式，请继续输入命令...\n")
                    } else {
                        val result = runShellCommand(command)
                        appendColoredText(result.output)
                        if (result.error.isNotBlank()) {
                            appendColoredText("\n[错误]\n${result.error}", Color.RED)
                        }
                    }
                } else {
                    // 异步执行命令
                    val session = interactiveSession!!
                    val finalCommand = if (shouldRunInBackground(command)) "$command &" else command

                    session.executeNext(finalCommand) { line ->
                        appendColoredText(line)
                    }
                }
                commandHistory.add(command)
                historyIndex = -1

            } catch (e: Exception) {
                appendColoredText("\n执行异常:\n ${e.message}", Color.RED)
                endInteractiveSession()
            } finally {
                commandInput.setText("")
                commandInput.requestFocus()

                lifecycleScope.launch {
                    executeBtn.isEnabled = true
                    delay(500)
                    executeBtn.text = "执行命令"
                }
            }
        }
    }

    private fun shouldRunInBackground(command: String): Boolean {
        return command.startsWith("am start", true) ||
                command.startsWith("monkey", true) ||
                command.contains("sleep") ||
                command.startsWith("ping", true)
    }


    fun RecyclerView.setMaxFlingVelocity(maxFlingVelocity: Int = 6000) {
        try {
            val field = this.javaClass.getDeclaredField("mMaxFlingVelocity")
            field.isAccessible = true
            field.set(this, maxFlingVelocity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


data class ShellResult(val output: String, val error: String)

class InteractiveShellSession(private val useRoot: Boolean = false) {

    private lateinit var process: Process
    private lateinit var stdin: DataOutputStream
    private lateinit var stdoutReader: BufferedReader
    private lateinit var stderrReader: BufferedReader

    private var isClosed = false

    fun start() {
        process = Runtime.getRuntime().exec(if (useRoot) "su" else "sh")
        stdin = DataOutputStream(process.outputStream)

        stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
        stderrReader = BufferedReader(InputStreamReader(process.errorStream))

        isClosed = false
    }

    suspend fun executeNext(command: String, callback: (String) -> Unit): Job = coroutineScope {
        require(!isClosed) { "Shell session 已关闭" }

        stdin.writeBytes("$command\n")
        stdin.flush()

        val stdoutJob = launch(Dispatchers.IO) {
            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                callback.invoke(line!!)
            }
        }

        val stderrJob = launch(Dispatchers.IO) {
            var line: String?
            while (stderrReader.readLine().also { line = it } != null) {
                callback.invoke("❌ [错误] ${line!!}")
            }
        }

        // 设置最大等待时间（比如 5 秒），避免无限等待
        val timeoutJob = launch {
            withTimeoutOrNull(5000) {
                stdoutJob.join()
                stderrJob.join()
                callback.invoke("🔚 命令执行完毕。")
            }
        }
        timeoutJob
    }

    fun close() {
        if (!isClosed) {
            stdin.writeBytes("exit\n")
            stdin.flush()
            stdin.close()
            stdoutReader.close()
            stderrReader.close()
            process.destroy()
            isClosed = true
        }
    }
}