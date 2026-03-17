package com.cymf.tmp.web

import android.content.Context
import android.view.MotionEvent
import android.webkit.WebView
import com.blankj.utilcode.util.ToastUtils
import com.cymf.tmp.utils.YLLogger

class MyWebView(context: Context) : WebView(context) {

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ToastUtils.showLong(ev.toString())
        return super.dispatchTouchEvent(ev)
    }
}