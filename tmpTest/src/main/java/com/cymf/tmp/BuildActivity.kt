package com.cymf.tmp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cymf.tmp.utils.BuildUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@SuppressLint("SetTextI18n")
class BuildActivity : AppCompatActivity(), View.OnClickListener {

    private val tv by lazy { findViewById<TextView>(R.id.tv_text) }
    private var lastClickTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_build)
        lifecycleScope.launch {
            tv.text = "\n\n\n\n\n\n\n             开始加载……"
            delay(500)
            requestInfo()
        }
        findViewById<Button>(R.id.btn_1).setOnClickListener(this)
        findViewById<Button>(R.id.btn_2).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val currentClickTime = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime < 500) {
            return
        }
        lifecycleScope.launch {
            tv.text = "\n\n\n\n\n\n\n             开始加载……"
            delay(500) //  防止感觉没有重新触发该方法
            when (v?.id) {
                R.id.btn_1 -> requestInfo()
                R.id.btn_2 -> tv.text = "\n\n\n\n\n\n\n             什么也没有…"
            }
        }
    }

    private suspend fun requestInfo() {
        tv.text = "Build 信息：\n\n"
        BuildUtils.getBuildInfo().forEach {
            tv.append("${it.key}: ${it.value}\n")
        }
    }
}