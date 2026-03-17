package com.cymf.device.base

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.cymf.device.R
import java.lang.reflect.ParameterizedType

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {

    lateinit var viewBinding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeDef = theme()
        if (themeDef == 0) {
            themeDef = R.style.DialogFullScreen
        }
        setStyle(STYLE_NO_TITLE, themeDef)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setWindowConfig()
        viewBinding = getViewBindingForFragment(inflater, container)
        initView()
        return viewBinding.root
    }

    protected open fun setWindowConfig() {
        try {
            dialog?.setCanceledOnTouchOutside(true)
            val window = dialog?.window
            window?.setBackgroundDrawableResource(R.color.transparent)
            window?.decorView?.setPadding(0, 0, 0, 0)
            val wlp = window?.attributes
            wlp?.gravity = Gravity.BOTTOM
            wlp?.width = WindowManager.LayoutParams.MATCH_PARENT
            wlp?.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = wlp
        } catch (_: Exception) {
        }
    }


    override fun dismiss() {
        super.dismiss()
        dismissCallback()
    }

    override fun dismissAllowingStateLoss() {
        super.dismissAllowingStateLoss()
        dismissCallback()
    }

    private fun dismissCallback() {}

    protected abstract fun initView()

    @StyleRes
    protected abstract fun theme(): Int
    override fun onDestroy() {
//        dismissLoading()
        super.onDestroy()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getViewBindingForFragment(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): VB {
        val type = javaClass.genericSuperclass as ParameterizedType
        val aClass = type.actualTypeArguments[0] as Class<*>
        val method = aClass.getDeclaredMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        return method.invoke(null, layoutInflater, container, false) as VB
    }
}
