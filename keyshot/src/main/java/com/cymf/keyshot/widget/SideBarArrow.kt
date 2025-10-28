package com.cymf.keyshot.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import com.cymf.keyshot.R
import com.cymf.keyshot.service.AssistsService

class SideBarArrow : View.OnClickListener {
    private var mParams: WindowManager.LayoutParams? = null
    private var mArrowView: LinearLayout? = null
    private var mContext: Context? = null
    private var mLeft = false
    private var mWindowManager: WindowManager? = null
    private var mAssistsService: AssistsService? = null
    private var mContentBar: SideBarContent? = null
    private var mContentBarView: LinearLayout? = null
    private var mAnotherArrowView: LinearLayout? = null


    @SuppressLint("InflateParams")
    fun getView(
        context: Context,
        left: Boolean,
        windowManager: WindowManager,
        assistsService: AssistsService
    ): LinearLayout? {
        mContext = context
        mLeft = left
        mWindowManager = windowManager
        mAssistsService = assistsService
        mParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            x = 0
            y = 0
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        val inflater = LayoutInflater.from(context)
        mArrowView = inflater.inflate(R.layout.layout_arrow, null) as LinearLayout
        val arrow: View = mArrowView!!.findViewById(R.id.arrow)
        arrow.setOnClickListener(this)
        if (left) {
            arrow.rotation = 180f
            mParams?.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            mParams?.windowAnimations = R.style.LeftSeekBarAnim
        } else {
            arrow.rotation = 0F
            mParams?.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            mParams?.windowAnimations = R.style.RightSeekBarAnim
        }
        val text: TextView = mArrowView!!.findViewById<TextView>(R.id.info);
        text.isClickable = false
        text.isEnabled = false
        mArrowView!!.isClickable = false
        mWindowManager?.addView(mArrowView, mParams)
        return mArrowView
    }


    private val stringBuilder = StringBuilder();
    fun addLogText(logData: String) {
        mArrowView?.let {
            val text: TextView = it.findViewById<TextView>(R.id.info);
            if (text.isShown) {
                stringBuilder.appendLine(logData)
                var stringLines = stringBuilder.lines();
                if (stringLines.size > 10) {
                    stringLines = stringLines.subList(stringLines.size - 10, stringLines.size)
                    stringBuilder.clear()
                    stringLines.forEach<String> {
                        stringBuilder.appendLine(it)
                    }
                }
                text.text = stringBuilder
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.arrow -> {
                mArrowView?.visibility = View.GONE
                mAnotherArrowView?.visibility = View.GONE
                if (null == mContentBar || null == mContentBarView) {
                    mContentBar = SideBarContent()
                    mContentBarView = mContentBar?.getView(
                        mContext!!, mLeft,
                        mWindowManager!!, mParams, mArrowView!!,
                        mAssistsService!!, mAnotherArrowView
                    )
                } else {
                    mContentBarView?.visibility = View.VISIBLE
                }
                mContentBar?.removeOrSendMsg(remove = false, send = true)
            }
        }
    }

    fun setAnotherArrowBar(anotherArrowBar: LinearLayout) {
        mAnotherArrowView = anotherArrowBar
    }

    fun launcherInvisibleSideBar() {
        mArrowView?.visibility = View.VISIBLE
        mContentBarView?.let { view ->
            mContentBar?.let {
                view.visibility = View.GONE
                it.removeOrSendMsg(remove = true, send = false)
                it.clearSeekBar()
            }
        }

    }

    /**
     * when AccessibilityService is forced closed
     */
    fun clearAll() {
        mWindowManager!!.removeView(mArrowView)
        if (null != mContentBar || null != mContentBarView) {
            mWindowManager!!.removeView(mContentBarView)
            mContentBar!!.clearSeekBar()
            mContentBar!!.clearCallbacks()
        }
    }
}