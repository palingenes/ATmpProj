package com.cymf.autogame.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.widget.LinearLayout
import com.cymf.autogame.App
import com.cymf.autogame.R
import com.cymf.autogame.ktx.gone
import com.cymf.autogame.service.AssistsService
import com.cymf.autogame.utils.DisplayUtil
import kotlin.math.abs
import kotlin.math.sqrt

class SideBarArrow : View.OnClickListener {
    private var mParams: WindowManager.LayoutParams? = null
    private var mArrowView: LinearLayout? = null
    private var mContext: Context? = null
    private var mWindowManager: WindowManager? = null
    private var mAssistsService: AssistsService? = null
    private var mContentBar: SideBarContent? = null
    private var mContentBarView: LinearLayout? = null


    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val touchSlop by lazy {
        ViewConfiguration.get(mContext ?: return@lazy 8).scaledTouchSlop
    }
    private var isAwaitingDrag = false // 是否处于等待判断状态
    private val longPressTimeout = 300L

    @SuppressLint("InflateParams")
    fun getView(
        context: Context,
        windowManager: WindowManager,
        assistsService: AssistsService
    ): LinearLayout? {
        mContext = context
        mWindowManager = windowManager
        mAssistsService = assistsService
        mParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT

            val centerY = DisplayUtil.heightPixels() / 2
            y = centerY + DisplayUtil.dip2px(50f)

            gravity = Gravity.START or Gravity.TOP
            windowAnimations = R.style.RightSeekBarAnim
        }
        val inflater = LayoutInflater.from(context)
        mArrowView = inflater.inflate(R.layout.layout_arrow, null) as LinearLayout

        mArrowView?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val viewWidth = mArrowView?.measuredWidth ?: DisplayUtil.dip2px(40f)
        mParams?.x = DisplayUtil.widthPixels() - viewWidth - 15

        mArrowView?.findViewById<View>(R.id.arrow)?.apply {
            setOnClickListener(this@SideBarArrow)
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        initialX = mParams?.x ?: 0
                        initialY = mParams?.y ?: 0

                        isAwaitingDrag = true
                        App.runOnUiThread(longPressRunnable, longPressTimeout)
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()

                            mParams?.x = initialX + dx
                            mParams?.y = initialY + dy

                            mWindowManager?.updateViewLayout(mArrowView, mParams)
                            return@setOnTouchListener true
                        }

                        if (isAwaitingDrag) {
                            val dx = abs(event.rawX - initialTouchX)
                            val dy = abs(event.rawY - initialTouchY)
                            val distance = sqrt((dx * dx + dy * dy).toDouble())

                            if (distance > touchSlop) {
                                isDragging = true
                                isAwaitingDrag = false
                                handler.removeCallbacks(longPressRunnable)
                                return@setOnTouchListener true
                            }
                        }
                        false
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        isAwaitingDrag = false

                        if (isDragging) {
                            isDragging = false
                            return@setOnTouchListener true // 消费事件
                        }
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        val distance = sqrt((dx * dx + dy * dy).toDouble())

                        if (distance < touchSlop) {
                            v.performClick()
                        }
                        false
                    }

                    else -> false
                }
            }
        }
        mArrowView?.isClickable = true
        mWindowManager?.addView(mArrowView, mParams)
        return mArrowView
    }

    private val longPressRunnable = Runnable {
        if (isAwaitingDrag) {
            isDragging = true
            isAwaitingDrag = false
        }
    }

    override fun onClick(v: View) {
        if (v.id != R.id.arrow) return

        mArrowView?.gone()
        if (null == mContentBar || null == mContentBarView) {
            mContentBar = SideBarContent()
            mContentBarView = mContentBar?.getView(
                mContext!!, mWindowManager!!, mArrowView!!,
                mAssistsService!!
            )
        } else {
            mContentBarView?.visibility = View.VISIBLE
            mArrowView?.let { mContentBar?.updatePosition(it) }

        }
        mContentBar?.removeOrSendMsg(remove = false, send = true)
    }

    fun clearAll() {
        App.removeCallbacks(longPressRunnable)
        mArrowView?.let { mWindowManager?.removeView(it) }
        mContentBarView?.let { mWindowManager?.removeView(it) }
        mContentBar?.clearCallbacks()
    }
}