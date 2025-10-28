package com.cymf.keyshot.widget

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.cymf.keyshot.R
import com.cymf.keyshot.service.AssistsService
import com.cymf.keyshot.tasks.TestClick
import com.cymf.keyshot.utils.PermissionUtil

class SideBarContent : View.OnClickListener {

    private var mContext: Context? = null
    private var mLeft = false
    private var mContentView: LinearLayout? = null
    private var mWindowManager: WindowManager? = null
    private var mArrowView: LinearLayout? = null
    private var mSideBarService: AssistsService? = null

    private var mControlBar: ControlConfigBar? = null
    private var mSeekBarView: LinearLayout? = null
    private var mAnotherArrowView: LinearLayout? = null
    private var mTagTemp = -1

    companion object {
        private const val COUNT_DOWN_TAG = 1
        private const val COUNT_DOWN_TIME = 5000
    }

    private var mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                COUNT_DOWN_TAG -> goNormal()
            }
        }
    }

    @SuppressLint("InflateParams")
    fun getView(
        context: Context,
        left: Boolean,
        windowManager: WindowManager,
        params: WindowManager.LayoutParams?,
        arrowView: LinearLayout,
        sideBarService: AssistsService,
        anotherArrowView: LinearLayout?
    ): LinearLayout? {
        mContext = context
        mLeft = left
        mWindowManager = windowManager
        mArrowView = arrowView
        mSideBarService = sideBarService
        mAnotherArrowView = anotherArrowView
        // get layout
        val inflater = LayoutInflater.from(context)
        mContentView = inflater.inflate(R.layout.layout_content, null) as LinearLayout
        // init click
//        mContentView?.findViewById<View>(R.id.tv_brightness)?.setOnClickListener(this)
        mContentView?.findViewById<View>(R.id.tv_back)?.setOnClickListener(this)
        mContentView?.findViewById<View>(R.id.tv_config)?.setOnClickListener(this)
        mContentView?.findViewById<View>(R.id.tv_force_stop)?.setOnClickListener(this)
        mContentView?.findViewById<View>(R.id.tv_home)?.setOnClickListener(this)
        mContentView?.findViewById<View>(R.id.tv_annotation)?.setOnClickListener(this)
//        mContentView?.findViewById<View>(R.id.tv_volume)?.setOnClickListener(this)
        mContentView?.findViewById<View>(R.id.tv_backstage)?.setOnClickListener(this)
        mContentView?.findViewById<View>(R.id.task_layout)?.setOnClickListener(this)
        val root = mContentView?.findViewById<LinearLayout>(R.id.root)
        if (left) {
            root?.setPadding(15, 0, 0, 0)
        } else {
            root?.setPadding(0, 0, 15, 0)
        }
        mWindowManager?.addView(mContentView, params)
        upDateTaskUI(false)
        return mContentView
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_back -> {
                removeOrSendMsg(remove = true, send = true)
                clearSeekBar()
                mSideBarService!!.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }

            R.id.tv_home -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                mSideBarService!!.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }

            R.id.tv_annotation -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                annotationGo()
            }

            R.id.tv_config -> {
                removeOrSendMsg(remove = true, send = true)
                goNormal()
                TestClick.click()
//                brightnessOrVolume(2)
            }

            R.id.tv_force_stop -> {
                removeOrSendMsg(remove = true, send = true)
                TestClick.forceStop()
//                brightnessOrVolume(2)
            }
//            R.id.tv_volume -> {
//                removeOrSendMsg(remove = true, send = true)
//                brightnessOrVolume(1)
//            }
//            R.id.tv_brightness -> {
//                removeOrSendMsg(remove = true, send = true)
//                brightnessPermissionCheck()
//            }
            R.id.tv_backstage -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                mSideBarService!!.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }

            R.id.task_layout -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                val isPause = false
                if (isPause) {
                    TestClick.forceStart()
                } else {
                    TestClick.forcePause()
                }
                upDateTaskUI(!isPause);
            }
        }
    }

    private fun upDateTaskUI(pauseGetTask: Boolean) {
        mContentView?.findViewById<ImageView>(R.id.iv_task)
            ?.setImageResource(if (pauseGetTask) R.mipmap.task_start_icon else R.mipmap.task_pause_icon)
        mContentView?.findViewById<TextView>(R.id.tv_task)?.text =
            if (pauseGetTask) "开始" else "暂停";
    }

    private fun brightnessOrVolume(tag: Int) {
        if (mTagTemp == tag) {
            if (null != mSeekBarView) {
                removeSeekBarView()
            } else {
                addSeekBarView(tag)
            }
            return
        }
        mTagTemp = tag
        if (null == mControlBar) {
            mControlBar = ControlConfigBar()
        }
        if (null == mSeekBarView) {
            addSeekBarView(tag)
        } else {
            removeSeekBarView()
            addSeekBarView(tag)
        }
    }

    private fun addSeekBarView(tag: Int) {
        mSeekBarView = mControlBar!!.getView(mContext!!, mLeft, tag, this)
        mWindowManager!!.addView(mSeekBarView, mControlBar!!.mParams)
    }

    private fun removeSeekBarView() {
        if (null != mSeekBarView) {
            mWindowManager!!.removeView(mSeekBarView)
            mSeekBarView = null
        }
    }

    private fun arrowsShow() {
        mContentView?.visibility = View.GONE
        mArrowView?.visibility = View.VISIBLE
        mAnotherArrowView?.visibility = View.VISIBLE
    }

    fun clearSeekBar() {
        if (null != mSeekBarView) {
            mWindowManager?.removeView(mSeekBarView)
            mSeekBarView = null
        }
    }

    private fun goNormal() {
        arrowsShow()
        clearSeekBar()
    }

    private fun annotationGo() {
        val intent = Intent()
        intent.component = ComponentName("com.android.notes", "com.android.notes.MainActivity")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            mContext!!.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                mContext,
                "not found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun removeOrSendMsg(remove: Boolean, send: Boolean) {
        if (remove) {
            mHandler.removeMessages(COUNT_DOWN_TAG)
        }
        if (send) {
            mHandler.sendEmptyMessageDelayed(
                COUNT_DOWN_TAG,
                COUNT_DOWN_TIME.toLong()
            )
        }
    }

    fun removeOtherBar() {
        mHandler.removeMessages(COUNT_DOWN_TAG)
        mHandler.sendEmptyMessageDelayed(
            COUNT_DOWN_TAG,
            100
        )
    }

    /**
     * when AccessibilityService is forced closed
     */
    fun clearCallbacks() {
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun brightnessPermissionCheck() {
        if (!PermissionUtil.isSettingsCanWrite(mContext)) {
            goNormal()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = ("package:" + mContext!!.packageName).toUri()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext!!.startActivity(intent)
            Toast.makeText(
                mContext,
                "aaaaa",
                Toast.LENGTH_LONG
            ).show()
        } else {
            brightnessOrVolume(0)
        }
    }
}