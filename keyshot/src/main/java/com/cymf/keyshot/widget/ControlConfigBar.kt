package com.cymf.keyshot.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.*
import android.widget.LinearLayout
import android.widget.RadioButton
import com.cymf.keyshot.App
import com.cymf.keyshot.R
import com.cymf.keyshot.constant.Constants
import com.cymf.keyshot.service.AssistsService
import com.cymf.keyshot.utils.DisplayUtil
import com.cymf.keyshot.utils.SPUtil

class ControlConfigBar : View.OnClickListener {

    var mParams: WindowManager.LayoutParams? = null
    private var mSideBarContent: SideBarContent? = null

    @SuppressLint("RtlHardcoded")
    fun getView(
        context: Context,
        left: Boolean,
        tag: Int,
        sideBarContent: SideBarContent
    ): LinearLayout {
        mParams = WindowManager.LayoutParams()
        mSideBarContent = sideBarContent

        mParams!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        mParams!!.format = PixelFormat.RGBA_8888
        mParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mParams!!.width = ViewGroup.LayoutParams.WRAP_CONTENT
        mParams!!.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mParams!!.gravity = Gravity.LEFT or Gravity.TOP
        val inflater = LayoutInflater.from(context)
        @SuppressLint("InflateParams") val layoutConfig =
            inflater.inflate(R.layout.layout_config, null) as LinearLayout
        val w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        layoutConfig.measure(w, h)

        mParams!!.x = DisplayUtil.widthPixels() / 2 - layoutConfig.measuredWidth / 2
        mParams!!.y = DisplayUtil.heightPixels() / 2 - DisplayUtil.dip2px( 50f)
        // left or right show view
        if (left) {
//            mParams!!.x = DisplayUtil.dp2px(context, 120) - layoutConfig.measuredWidth
//            mParams!!.y = DisplayUtil.getScreenHeight(context) - DisplayUtil.dp2px(context, 282)
            mParams!!.windowAnimations = R.style.LeftSeekBarAnim
        } else {
//            mParams!!.x = DisplayUtil.getScreenWidth(context) - DisplayUtil.dp2px(context, 120)
//            mParams!!.y = DisplayUtil.getScreenHeight(context) - DisplayUtil.dp2px(context, 282)
            mParams!!.windowAnimations = R.style.RightSeekBarAnim
        }
        val btn0 = layoutConfig.findViewById<RadioButton>(R.id.btn_0)
        val btn1 = layoutConfig.findViewById<RadioButton>(R.id.btn_gg)
        val btn2 = layoutConfig.findViewById<RadioButton>(R.id.btn_lm)
        btn0.setOnClickListener(this)
        btn1.setOnClickListener(this)
        btn2.setOnClickListener(this)
        when (SPUtil.TOUCH_LOCATION) {
            0 -> {
                btn0.isChecked = true
                btn1.isChecked = false
                btn2.isChecked = false
            }
            1 -> {
                btn0.isChecked = false
                btn1.isChecked = true
                btn2.isChecked = false
            }
            2 -> {
                btn0.isChecked = false
                btn1.isChecked = false
                btn2.isChecked = true
            }
        }
        return layoutConfig
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.btn_0 -> {
                resetBar(0)
            }
            R.id.btn_gg -> {
                resetBar(1)
            }
            R.id.btn_lm -> {
                resetBar(2)
            }
        }
    }

    private fun resetBar(index: Int) {
        val position = SPUtil.TOUCH_LOCATION
        if (position != index) {
            SPUtil.TOUCH_LOCATION = index
            val intent = Intent(App.app, AssistsService::class.java)
            intent.putExtra(Constants.TAG_RESET_TOUCH_BAR, 1)
            App.app.startService(intent)
        } else {
            mSideBarContent?.removeOtherBar()
        }
    }
}