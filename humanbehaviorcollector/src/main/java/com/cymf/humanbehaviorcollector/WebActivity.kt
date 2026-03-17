package com.cymf.humanbehaviorcollector

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils


class WebActivity : AppCompatActivity() {

    private var webView: WebView? = null
        get() {
            return findViewById(R.id.webView)
        }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        webView?.settings?.javaScriptEnabled = true

        webView?.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)
        webView?.loadUrl("https://www.baidu.com")
        webView?.setOnTouchListener { _, event ->
            LogUtils.e("Action: ${actionToString(event.action)}, x=${event.x},rawX=${event.rawX}, y=${event.y}, rawY=${event.rawY}")
            false // 不拦截
        }
    }

    private fun actionToString(action: Int): String {
        return when (action) {
            MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
            MotionEvent.ACTION_UP -> "ACTION_UP"
            MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
            MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
            MotionEvent.ACTION_POINTER_DOWN -> "ACTION_POINTER_DOWN"
            MotionEvent.ACTION_POINTER_UP -> "ACTION_POINTER_UP"
            else -> "UNKNOWN_ACTION_$action"
        }
    }

    override fun onDestroy() {
        webView?.let {
            it.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            it.clearHistory()

            (it.parent as ViewGroup).removeView(it)
            it.destroy()
            webView = null
        }
        super.onDestroy()
    }

}