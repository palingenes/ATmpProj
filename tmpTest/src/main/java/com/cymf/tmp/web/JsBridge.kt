package com.cymf.tmp.web

import android.os.Build
import android.webkit.JavascriptInterface
import android.widget.Toast


import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.os.Handler
import android.webkit.WebView

class JsBridge(private val webView: WebView) {

    // 可以访问上下文用于 Toast 等操作
    private val context: Context
        get() = webView.context

    @JavascriptInterface
    fun onRetry(){
        Handler(Looper.getMainLooper()).post {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        // 显示 Toast（必须在主线程）
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 可以添加更多方法，例如：
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return android.os.Build.MODEL + " - " + android.os.Build.VERSION.RELEASE
    }
}