package com.cymf.tmp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.cymf.tmp.RootActivity.ExampleInitializer
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), View.OnClickListener {

    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setInitializers(ExampleInitializer::class.java)
                .setTimeout(10)
        )
    }

    private var lastClickTime: Long = 0
    val text1 by lazy { "IpScanner、sh、触摸点" }
    val text2 by lazy { "StatFs 分区" }
    val text3 by lazy { "网络、wifi、输入法、ISO、包安装/更新时间" }
    val text4 by lazy { "Build信息" }
    val text5 by lazy { "执行Shell命令" }
    val text6 by lazy { "执行Shell命令 2 " }
    val text7 by lazy { "测试文件信息" }
    val text8 by lazy { "WebView 测试插页" }
    val text9 by lazy { "测试0707的日存疑" }
    val text10 by lazy { "测试 Deeplink/ 应用外跳转" }
    val text11 by lazy { "Socket测试" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createView())

        lifecycleScope.launch {
            delay(200)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        LogUtils.e(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onClick(v: View?) {
        val currentClickTime = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime < 500) {
            return
        }
        when (v?.tag) {
            text1 -> startActivity(Intent(this, MainActivity1::class.java))
            text2 -> startActivity(Intent(this, MainActivity2::class.java))
            text3 -> startActivity(Intent(this, MainActivity3::class.java))
            text4 -> startActivity(Intent(this, BuildActivity::class.java))
            text5 -> startActivity(Intent(this, ShellActivity::class.java))
            text6 -> startActivity(Intent(this, RootActivity::class.java))
            text7 -> startActivity(Intent(this, FileActivity::class.java))
            text8 -> startActivity(Intent(this, WebActivity::class.java))
            text9 -> startActivity(Intent(this, DoubtfulActivity::class.java))
            text10 -> startActivity(Intent(this, JumpActivity::class.java))
            text11 -> startActivity(Intent(this, PingNetActivity::class.java))
        }
    }

    private fun createView(): View {
        val parent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            setPadding(20)
        }

        parent.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 60)
        })
        parent.addView(createButton(text1))
        parent.addView(createButton(text2))
        parent.addView(createButton(text3))
        parent.addView(createButton(text4))
        parent.addView(createButton(text5))
//        parent.addView(createButton(text6))
        parent.addView(createButton(text7))
        parent.addView(createButton(text8))
        parent.addView(createButton(text9))
        parent.addView(createButton(text10))
        parent.addView(createButton(text11))
        return parent
    }

    private fun createButton(text: String): Button {
        return Button(this).apply {
            isAllCaps = false
            textSize = 14.0f
            setTextColor(ColorStateList.valueOf(Color.BLACK))
            this.text = text
            this.tag = text
            setOnClickListener(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
    }
}