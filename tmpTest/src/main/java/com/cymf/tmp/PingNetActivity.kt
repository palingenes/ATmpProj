package com.cymf.tmp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cymf.tmp.utils.InputMethodUtil
import com.cymf.tmp.utils.Socks5TestRequest

class PingNetActivity : AppCompatActivity() {

    private lateinit var commandInput: EditText
    private lateinit var executeBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var outputAdapter: OutputAdapter

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ping_net)

        commandInput = findViewById(R.id.commandInput)
        executeBtn = findViewById(R.id.executeBtn)
        clearBtn = findViewById(R.id.clearBtn)
        recyclerView = findViewById(R.id.recyclerView)

        // 初始化 RecyclerView
        outputAdapter = OutputAdapter()
        recyclerView.adapter = outputAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 设置点击事件
        executeBtn.setOnClickListener {
            executeCommand()
        }
        clearBtn.setOnClickListener {
            outputAdapter.clearData()
        }
        // 上下键导航历史命令
        commandInput.setOnKeyListener { _, keyCode, event ->
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

        commandInput.setOnEditorActionListener { _, keyCode, _ ->
            if (keyCode == EditorInfo.IME_ACTION_DONE) {
                executeCommand()
                return@setOnEditorActionListener true
            }
            false
        }
        recyclerView.setMaxFlingVelocity()
    }

    private fun executeCommand() {
        val command = commandInput.text.toString().trim()
        if (command.isEmpty()) return
        InputMethodUtil.hideKeyboard(this)

        val request = Socks5TestRequest(command)
        request.setListener {
            if (isFinishing || isDestroyed) return@setListener
            runOnUiThread {
                appendColoredText(it)
            }
        }
        request.start()
    }

    private fun appendColoredText(text: String, defaultColor: Int = Color.BLACK) {
        if (text.isNotBlank()) {
            Log.e("PPPPPP", text)
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