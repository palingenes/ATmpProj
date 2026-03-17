package com.cymf.tmp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.cymf.tmp.web.JsBridge
import com.cymf.tmp.web.MyWebChromeClient
import com.cymf.tmp.web.MyWebView
import com.cymf.tmp.web.MyWebViewClient

@SuppressLint("SetJavaScriptEnabled")
class WebActivity : AppCompatActivity() {

    private val container by lazy { findViewById<LinearLayout>(R.id.ll_container) }

    private val webView by lazy {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        MyWebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            clearHistory()
            clearCache(true)
            settings.javaScriptEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = MyWebViewClient()
            webChromeClient = MyWebChromeClient(this@WebActivity)

            addJavascriptInterface(JsBridge(this), "NativeBridge")
//            loadUrl("file:///android_asset/xxl.html")
//            loadUrl("https://m.bi65.cc/")
//            loadUrl("https://m.autohome.com.cn/")
//            loadUrl("https://juejin.cn/")
//            loadUrl("https://www.zhihu.com")
//            loadUrl("https://www.douban.com")
//            loadUrl("https://music.163.com")
//            loadUrl("https://www.ted.com")
//            loadUrl("https://www.twitch.tv")
            loadUrl("https://stackoverflow.com")
//            loadUrl("http://test.ua1.cn/touch.html")
//            loadUrl("http://test.ua1.cn/touch-details.html")
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)

        container.addView(webView)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (webView.parent != null) {
            (webView.parent as ViewGroup).removeView(webView)
        }
        webView.apply {
            stopLoading()
            settings.javaScriptEnabled = false
            clearHistory()
            clearCache(true)
            loadUrl("about:blank")
            onPause()
            removeAllViews()
            destroy()
        }
    }
}