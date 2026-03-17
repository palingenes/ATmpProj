package com.cymf.tmp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetTextI18n")
class FileActivity : AppCompatActivity() {

    private val btn1 by lazy { findViewById<Button>(R.id.btn_1) }
    private val tvView by lazy { findViewById<TextView>(R.id.tv_text) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)
        btn1.setOnClickListener {
            loadData()
        }
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            tvView.text = "加载/刷新中……"
            delay(500)
            writeToFile("hello world！")
        }
    }

    private suspend fun writeToFile(content: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(applicationContext.filesDir, "test.txt")

            if (file.exists()) {
                withContext(Dispatchers.Main) {
                    tvView.text = "⚠️ 文件已存在：${file.name}\n"
                    tvView.append("path: ${file.path}\n")
                    tvView.append("absolutePath: ${file.absolutePath}\n")
                    tvView.append("exists(): ${file.exists()}\n")
                    tvView.append("canRead(): ${file.canRead()}\n")
                    tvView.append("canWrite(): ${file.canWrite()}\n")
                    tvView.append("canExecute(): ${file.canExecute()}\n")
                    tvView.append("length(): ${file.length()} bytes\n")
                    tvView.append("lastModified(): ${formatTime(file.lastModified())}\n")
                    tvView.append("--------------------------------------")
                }
            } else {
                withContext(Dispatchers.Main) {
                    tvView.text = ("🆕 文件不存在，即将创建并写入……\n")
                }
                file.writeText(content)
                delay(200)
                withContext(Dispatchers.Main) {
                    tvView.append("文件<${file.name}>写入成功\n")
                    tvView.append("path: ${file.path}\n")
                    tvView.append("absolutePath: ${file.absolutePath}\n")
                    tvView.append("exists(): ${file.exists()}\n")
                    tvView.append("canRead(): ${file.canRead()}\n")
                    tvView.append("canWrite(): ${file.canWrite()}\n")
                    tvView.append("canExecute(): ${file.canExecute()}\n")
                    tvView.append("length(): ${file.length()} bytes\n")
                    tvView.append("lastModified(): ${formatTime(file.lastModified())}\n")
                    tvView.append("--------------------------------------")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                tvView.text = "出现异常：\n"
                tvView.append(e.message)
            }
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timeInMillis))
    }
}