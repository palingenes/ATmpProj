package com.cymf.tmp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cymf.tmp.utils.IpScanner
import com.cymf.tmp.utils.ShellAdbUtils
import com.cymf.tmp.utils.ShellExecutor
import com.cymf.tmp.utils.TouchUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity1 : AppCompatActivity(), View.OnClickListener {

    private val tv by lazy { findViewById<TextView>(R.id.tv_text) }
    private var lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)
        findViewById<Button>(R.id.btn_1).setOnClickListener(this)
        findViewById<Button>(R.id.btn_2).setOnClickListener(this)
        findViewById<Button>(R.id.btn_3).setOnClickListener(this)
        findViewById<Button>(R.id.btn_4).setOnClickListener(this)
        findViewById<Button>(R.id.btn_5).setOnClickListener(this)
        findViewById<Button>(R.id.btn_6).setOnClickListener(this)
        findViewById<Button>(R.id.btn_7).setOnClickListener(this)
        findViewById<Button>(R.id.btn_8).setOnClickListener(this)
        findViewById<Button>(R.id.btn_9).setOnClickListener(this)
        findViewById<Button>(R.id.btn_10).setOnClickListener(this)
        findViewById<Button>(R.id.btn_11).setOnClickListener(this)
        findViewById<Button>(R.id.btn_12).setOnClickListener(this)
        findViewById<Button>(R.id.btn_13).setOnClickListener(this)
        findViewById<Button>(R.id.btn_14).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val currentClickTime = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime < 800) {
            return
        }
        when (v?.id) {
            R.id.btn_1 -> {
                IpScanner().execCatForArp {
                    val sb = StringBuilder()
                    it.forEach {
                        sb.append("key=").append(it.key).append("\t").append("value=")
                            .append(it.value).append("\n")
                    }
                    tv.text = sb
                }
            }

            R.id.btn_2 -> execShell("ls /system/xbin/su")
            R.id.btn_3 -> execShell("ls /system/bin/su")
            R.id.btn_4 -> execShell("ls /data/local/xbin/su")
            R.id.btn_5 -> execShell("cat /proc/cpuinfo")
            R.id.btn_6 -> execShell("cat /proc/meminfo")
            R.id.btn_7 -> execShell("cat /proc/version")
            R.id.btn_8 -> {
                lifecycleScope.launch {
                    val exec = ShellExecutor.exec("getprop")
                    tv.text = exec
                }
            }

            R.id.btn_9 -> execShell("/system/xbin/which --help")
            R.id.btn_10 -> execShell("/system/xbin/which su")
            R.id.btn_11 -> execShell("su")
            R.id.btn_12 -> execShell("frida --version")
            R.id.btn_13 -> execShell("python --version")
            R.id.btn_14 -> {
                TouchUtils.checkMultiTouchSupport(this)

                TouchUtils.listenTouchPoint(tv) { count ->
                    Log.d("MyApp", "当前手指数量：$count")
                }
                val maxTouchPoints = TouchUtils.getMaxTouchPointsFromInputDevice()
                Log.d("TouchUtils", "推测最大触控点数: $maxTouchPoints")
            }
        }
    }

    private fun execShell(shell: String) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                ShellAdbUtils.execCommand(shell, false)
            }
            val sb = StringBuilder()
            sb.append("result: ").append(result.result).append("\n")
            sb.append("successMsg: ").append(result.successMsg).append("\n")
            sb.append("errorMsg: ").append(result.errorMsg).append("\n")
            tv.text = sb.toString()
        }
    }
}
