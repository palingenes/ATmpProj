package com.cymf.keyshot.base

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.cymf.keyshot.App
import com.cymf.keyshot.dialog.LoadingDialog
import com.cymf.keyshot.ktx.checkIsDestroyed
import com.cymf.keyshot.utils.DisplayUtil
import java.lang.reflect.ParameterizedType
import kotlin.also
import kotlin.jvm.java
import kotlin.jvm.javaClass
import kotlin.takeIf

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    lateinit var viewBinding: VB

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        fullScreenAndImmersiveView(true)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        viewBinding = getViewBindingForActivity(layoutInflater)
        bindMergeView()
        setContentView(viewBinding.root)
        initView()
        initData()
    }

    open fun bindMergeView() {

    }

    protected abstract fun initView()
    protected abstract fun initData()

    fun <T : View?> find(@IdRes id: Int): T {
        return findViewById(id)
    }

    fun setText(@IdRes id: Int, @StringRes resId: Int) {
        val view = find<View>(id)
        if (view is TextView) {
            view.setText(resId)
        }
    }

    fun setText(@IdRes id: Int, charSequence: CharSequence?) {
        val view = find<View>(id)
        if (view is TextView) {
            if (TextUtils.isEmpty(charSequence)) {
                "".also { view.text = it }
            } else {
                view.text = charSequence
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (isShouldHideInput(v, ev)) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                assert(v != null)
                imm.hideSoftInputFromWindow(v!!.windowToken, 0)
            }
            return super.dispatchTouchEvent(ev)
        }
        try {
            if (window.superDispatchTouchEvent(ev)) {
                return true
            }
        } catch (ignored: IllegalArgumentException) {
        }
        return onTouchEvent(ev)
    }

    private fun isShouldHideInput(v: View?, event: MotionEvent): Boolean {
        if (v is EditText) {
            val leftTop = intArrayOf(0, 0)
            v.getLocationOnScreen(leftTop)
            val left = leftTop[0]
            val top = leftTop[1]
            val bottom = top + v.getHeight()
            val right = left + v.getWidth()
            return (!(event.rawX > left) || !(event.rawX < right)
                    || !(event.rawY > top) || !(event.rawY < bottom))
        }
        return false
    }

    protected fun setStatusBarHeight(@IdRes ids: Int) {
        this.setStatusBarHeight(ids, 20)
    }

    protected open fun setStatusBarHeight(@IdRes ids: Int, dip: Int) {
        try {
            if (find<View>(ids).layoutParams is LinearLayout.LayoutParams) {
                val layoutParams = find<View>(ids).layoutParams as LinearLayout.LayoutParams
                layoutParams.topMargin =
                    DisplayUtil.statusBarHeight() + DisplayUtil.dip2px(dip.toFloat())
            } else if (find<View>(ids).layoutParams is MarginLayoutParams) {
                val layoutParams = find<View>(ids).layoutParams as MarginLayoutParams
                layoutParams.topMargin =
                    DisplayUtil.statusBarHeight() + DisplayUtil.dip2px(dip.toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 沉浸式显示
     *
     * @param isDark4StatusBarColor 状态栏字体颜色是否是深色
     */
    @Suppress("DEPRECATION")
    private fun fullScreenAndImmersiveView(isDark4StatusBarColor: Boolean) {
        val window = this.window
        val decorView = window.decorView
        if (isDark4StatusBarColor) //  深色
        {
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else  //  浅色
        {
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_VISIBLE
        }
        window.statusBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }

    private var loadingDialog: LoadingDialog? = null

    fun dialog(): LoadingDialog {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(this)
        }
        return loadingDialog!!
    }

    fun dialogIsShowing(): Boolean {
        return loadingDialog?.isShowing == true
    }

    fun showLoading() {
        showLoading(LoadingDialog.THEME_LIGHT)
    }

    protected fun showLoading(theme: Int) {
        if (checkIsDestroyed()) return

        loadingDialog?.takeIf { it.isShowing }?.dismiss()
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(this, theme)
        }
        loadingDialog?.show()
    }

    fun dismissLoading() {
        App.runOnUiThread({
            try {
                loadingDialog?.takeIf { it.isShowing }?.dismiss()
            } catch (_: Exception) {
            }
        }, 1)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getViewBindingForActivity(layoutInflater: LayoutInflater): VB {
        val type = javaClass.genericSuperclass as ParameterizedType
        val aClass = type.actualTypeArguments[0] as Class<*>
        val method = aClass.getDeclaredMethod("inflate", LayoutInflater::class.java)
        return method.invoke(null, layoutInflater) as VB
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissLoading()
    }
}
