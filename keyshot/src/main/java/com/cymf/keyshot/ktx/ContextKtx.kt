package com.cymf.keyshot.ktx

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment


fun Activity.checkIsDestroyed(): Boolean {
    return isFinishing || isDestroyed
}

@Suppress("DEPRECATION", "DEPRECATION")
@RequiresApi(api = Build.VERSION_CODES.P)
fun Activity.fullScreen(): Activity {
    try {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        ) // 隐藏android系统的状态栏
        val lp = window.attributes
        lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = lp
        // 设置页面全屏显示
        val decorView = window.decorView
        decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    } catch (_: Error) {
    } catch (_: Exception) {
    }
    return this
}

fun Activity.screenOn(): Activity {
    try {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } catch (_: Error) {
    } catch (_: Exception) {
    }
    return this
}


fun isDialogFragmentShowing(dialog: DialogFragment?): Boolean {
    return dialog?.dialog != null && dialog.dialog?.isShowing == true
}
