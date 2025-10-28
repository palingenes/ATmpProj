@file:Suppress("DEPRECATION")

package com.cymf.autogame.dialog

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.app.ProgressDialog
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.WindowManager
import com.cymf.autogame.R
import com.cymf.autogame.App
import com.cymf.autogame.ktx.checkIsDestroyed


class LoadingDialog : ProgressDialog {

    private var theme = THEME_LIGHT
    private val mActivity: Activity?
    private val frameAnim: AnimationDrawable? = null

    /**
     * 原生Android风格
     *
     * @param activity 上下文对象
     */
    constructor(activity: Activity?) : super(activity, THEME_LIGHT) {
        mActivity = activity
    }

    /**
     * IOS 风格
     *
     * @param activity 依附窗体
     * @param theme    主题
     */
    constructor(activity: Activity?, theme: Int) : super(activity, theme) {
        mActivity = activity
        this.theme = theme
    }

    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        if (mActivity == null) return
        App.app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private val activityLifecycleCallbacks: ActivityLifecycleCallbacks =
        object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                dismiss()
            }
        }

    private fun init() {
        //    设置不可取消，点击其他区域不能取消，实际中可以抽出去封装供外包设置
//        setCancelable(false);
        setCanceledOnTouchOutside(false)
        setContentView(if (theme == THEME_IOS) R.layout.view_loading_ios else R.layout.view_loading)

        window?.attributes?.apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
    }

    override fun dismiss() {
        if (mActivity == null || mActivity.isDestroyed || mActivity.isFinishing) return
        App. app.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        stop()
        if (isShowing) super.dismiss()
    }

    @Deprecated("Deprecated in Java")
    override fun onStop() {
        super.onStop()
        if (mActivity == null || mActivity.checkIsDestroyed()) return
        if (isShowing) dismiss()
        stop()
    }

    override fun show() {
        if (mActivity == null || mActivity.checkIsDestroyed()) return
        try {
            if (!isShowing) super.show()
            App.runOnUiThread({
                dismiss()
            }, 30_000)
        } catch (ignored: Error) {
        }
    }

    /**
     * 停止播放
     */
    private fun stop() {
        if (frameAnim != null && frameAnim.isRunning) {
            frameAnim.stop()
        }
    }

    companion object {
        val THEME_LIGHT = R.style.CustomDialog
        val THEME_DARK = R.style.CustomDialogBg
        val THEME_IOS = R.style.dialog_ios
    }
}