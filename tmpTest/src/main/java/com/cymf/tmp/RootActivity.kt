package com.cymf.tmp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RootActivity : AppCompatActivity() {

    private val commandInput: EditText by lazy { findViewById(R.id.commandInput) }
    private val executeBtn: Button by lazy { findViewById(R.id.executeBtn) }
    private val tvView: TextView by lazy { findViewById(R.id.tv_text) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)

        executeBtn.setOnClickListener {
            lifecycleScope.launch {
                val text = commandInput.text.toString().trim()
                if (text.isBlank()) return@launch
                tvView.text = "执行中……"
                executeBtn.isEnabled = false
                delay(500)
                Shell.cmd(text).submit { it ->
                    executeBtn.isEnabled = true
                    val sb = StringBuilder()
                    sb.append("isSuccess: ${it.isSuccess}").append("\n")
                    sb.append("code: ${it.code}").append("\n")
                    sb.append("Output:").append("\n")
                    it.out.forEach {
                        sb.append(it).append("\n")
                    }
                    sb.append("--------------------").append("\n")
                    sb.append("Error:").append("\n")
                    it.err.forEach {
                        sb.append(it).append("\n")
                    }
                    sb.append("--------------------").append("\n")
                    tvView.text = sb
                }
            }
        }
    }


    inner class ExampleInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            // Load our init script
            val bashrc = context.resources.openRawResource(R.raw.bashrc)
            shell.newJob().add(bashrc).exec()
            return true
        }
    }
}