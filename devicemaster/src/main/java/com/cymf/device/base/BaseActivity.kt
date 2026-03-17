package com.cymf.device.base

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
import androidx.cardview.widget.CardView
import androidx.viewbinding.ViewBinding
import com.cymf.device.R
import com.cymf.device.ktx.clickWithLimit
import com.cymf.device.tools.DisplayUtil
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var mDisposable: CompositeDisposable? = null
    lateinit var viewBinding: VB

    private var cardView: CardView? = null

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
        initTitleBarView()
        initView()
        initData()
    }

    open fun bindMergeView() {

    }

    protected abstract fun initView()
    protected abstract fun initData()

    private fun initTitleBarView() {
        cardView = find(R.id.card_title_container)
        if (cardView == null) return
        setStatusBarHeight(R.id.view_placeholder, 10)
        find<View>(R.id.ic_back).clickWithLimit { finish() }
    }

    fun setTitleText(titleText: CharSequence?) {
        setText(R.id.tv_title_bar_text, titleText)
    }

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

    protected fun addDisposable(d: Disposable?) {
        if (mDisposable == null) {
            mDisposable = CompositeDisposable()
        }
        d?.let { mDisposable?.add(it) }
    }

    protected fun cancelDisposable() {
        mDisposable?.clear()
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

    @Suppress("UNCHECKED_CAST")
    private fun getViewBindingForActivity(layoutInflater: LayoutInflater): VB {
        val type = javaClass.genericSuperclass as ParameterizedType
        val aClass = type.actualTypeArguments[0] as Class<*>
        val method = aClass.getDeclaredMethod("inflate", LayoutInflater::class.java)
        return method.invoke(null, layoutInflater) as VB
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelDisposable()
    }
}
