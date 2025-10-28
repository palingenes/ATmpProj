package com.cymf.keyshot.ktx

import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import com.cymf.keyshot.App
import com.cymf.keyshot.BuildConfig
import com.cymf.keyshot.utils.CoroutineWrapper
import com.cymf.keyshot.widget.MaskView

object MaskHolder {
    var maskView: MaskView? = null
}

fun Rect.show(maskType: Int = MaskView.TYPE_NORMAL) {
    if (!BuildConfig.DEBUG) return
    val windowManager = App.context.getSystemService(WINDOW_SERVICE) as WindowManager
    val params = WindowManager.LayoutParams().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        x = 0
        y = 0
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    CoroutineWrapper.launch(true) {
        MaskHolder.maskView?.let {
            windowManager.removeView(it)
        }
        val maskView = MaskHolder.maskView ?: MaskView(App.context).apply {
            MaskHolder.maskView = this
        }
        maskView.setRect(left, top, right, bottom, maskType)
        windowManager.addView(maskView, params)
    }
}
