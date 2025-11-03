package com.cymf.autogame.widget

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Process
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import com.cymf.autogame.App
import com.cymf.autogame.FrameActivity
import com.cymf.autogame.R
import com.cymf.autogame.ktx.gone
import com.cymf.autogame.ktx.isVisible
import com.cymf.autogame.ktx.visible
import com.cymf.autogame.service.AssistsService
import com.cymf.autogame.utils.CoroutineWrapper
import com.cymf.autogame.utils.DisplayUtil
import com.cymf.autogame.utils.GlobalTimer
import com.cymf.autogame.utils.ProcessKiller
import com.cymf.autogame.utils.TaskPollingManager
import com.cymf.autogame.utils.YLLogger
import com.hjq.toast.Toaster
import com.topjohnwu.superuser.Shell
import kotlin.math.min


class SideBarContent : View.OnClickListener {

    private var mContext: Context? = null
    private var mContentView: LinearLayout? = null
    private var mWindowManager: WindowManager? = null
    private var mArrowView: LinearLayout? = null
    private var mSideBarService: AssistsService? = null

    private var lastClickTime = 0L

    private var killTaskAndSuccess: LinearLayout? = null
    private var taskSuccess: LinearLayout? = null
    private var hasTask = false

    private var timeTextView: TextView? = null
    private var lastTime = ""
    private var tmp: Long = 0

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

    private val timerObserver = Observer<String> { time ->
        val displayTime = if (time.length >= 5) time.substring(0, 5) else "00:00"
        if (timeTextView == null) {
            timeTextView = mContentView?.findViewById(R.id.task_time)
        }
        timeTextView?.takeIf { it.isVisible }?.let { tv ->
            if (lastTime != displayTime) {
                tv.text = displayTime
                lastTime = displayTime

                if (tmp % 3L == 0L) {
                    checkTaskTimeAndBtnStyle()
                }
                tmp++
            }
        }
    }

    @SuppressLint("InflateParams")
    fun getView(
        context: Context,
        windowManager: WindowManager,
        arrowView: LinearLayout,
        sideBarService: AssistsService,
    ): LinearLayout? {
        this.mContext = context
        this.mWindowManager = windowManager
        this.mArrowView = arrowView
        this.mSideBarService = sideBarService
        (LayoutInflater.from(context).inflate(R.layout.layout_content, null) as LinearLayout).let {
            mContentView = it
        }

        findAndClick(
            R.id.tv_back,
            R.id.tv_config,
            R.id.force_stop_fail,
            R.id.force_stop_success,
            R.id.tv_home,
            R.id.tv_annotation,
            R.id.tv_backstage,
            R.id.task_layout,
            R.id.report_task_success,
            R.id.report_task_fail,
            R.id.copy_mail,
            R.id.copy_pwd,
            R.id.copy_code,
        )
        val root = mContentView?.findViewById<LinearLayout>(R.id.root)
        root?.setPadding(15, 15, 15, 15)

        val initialParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = 300
            height = 400
            x = 0
            y = 0
            gravity = Gravity.START or Gravity.TOP
            windowAnimations = R.style.ContentCoverAnim
        }
        mWindowManager?.addView(mContentView, initialParams)
        updatePosition(arrowView)
//        upDateTaskUI(TaskPollingManager.isPauseGetTask())

        GlobalTimer.getInstance().currentTime.removeObserver(timerObserver)
        GlobalTimer.getInstance().currentTime.observeForever(timerObserver)
        return mContentView
    }

    override fun onClick(v: View?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500L) {
            return
        }
        lastClickTime = currentTime

        when (v?.id) {
            R.id.tv_back -> {
                removeOrSendMsg(remove = true, send = true)
                mSideBarService!!.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }

            R.id.tv_home -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                mSideBarService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }

            R.id.tv_annotation -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                annotationGo()
            }

            R.id.tv_config -> {
                removeOrSendMsg(remove = true, send = true)
                goNormal()
            }

            R.id.force_stop_success, R.id.force_stop_fail -> {
                removeOrSendMsg(remove = true, send = true)
                val isSuccess: Boolean = v.id == R.id.force_stop_success
                if (isSuccess && !hasTask) {
                    Toaster.showShort("未满足最短任务时间！")
                    goNormal()
                    return
                }
                val pkgName = TaskPollingManager.getCurrTask()?.packageName
                TaskPollingManager.reportTaskComp(isSuccess)    //  上报并退出

                App.runOnUiThread({
                    killTaskApp(pkgName, false)
                    mSideBarService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                }, 500)

                App.runOnUiThread({
                    if (Shell.isAppGrantedRoot() == true) {
                        Shell.cmd("su -c am force-stop ${App.context.packageName}").submit {
                            if (it.isSuccess) {
                                YLLogger.d("通过am force-stop 杀死自身应用")
                            } else {
                                YLLogger.w("am force-stop 出错：：${it.err}")
                                YLLogger.d("（尝试） 通过 killProcess 杀死自身应用")
                                Process.killProcess(Process.myPid())
                            }
                        }
                    } else {
                        YLLogger.d("通过 killProcess 杀死自身应用")
                        Process.killProcess(Process.myPid())
                    }
                }, 1500)
            }

            R.id.tv_backstage -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                mSideBarService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }

//            R.id.task_layout -> {
//                removeOrSendMsg(remove = true, send = false)
//                goNormal()
//                val isPause = TaskPollingManager.isPauseGetTask()
//                if (isPause) {
//                    CoroutineWrapper.launch {
//                        TaskPollingManager.setIsPauseGetTask(false)
//                        delay(1000)
//                        TaskPollingManager.startPolling()
//                    }
//                } else {
//                    CoroutineWrapper.launch {
//                        TaskPollingManager.setIsPauseGetTask(true)
//                        TaskPollingManager.reportTaskComp(false)//已废弃
//                        TaskPollingManager.stopPolling()
//                        delay(1000)
//                    }
//                }
//                upDateTaskUI(!isPause)
//            }

            R.id.report_task_success, R.id.report_task_fail -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()

                var isSuccess: Boolean
                var text: String
                if (v.id == R.id.report_task_success) {
                    text = "已上报本次任务为成功状态！"
                    isSuccess = true
                } else {
                    text = "已上报本次任务为失败状态！"
                    isSuccess = false
                }
                if (isSuccess && !hasTask) {
                    Toaster.showShort("未满足最短任务时间！")
                    return
                }

                val pkgName = TaskPollingManager.getCurrTask()?.packageName
                TaskPollingManager.reportTaskComp(isSuccess) // 上报任务状态并进入下一轮
                Toaster.showShort(text)
                killTaskApp(pkgName, true)
            }

            R.id.copy_mail, R.id.copy_pwd, R.id.copy_code -> {
                removeOrSendMsg(remove = true, send = false)
                goNormal()
                Toaster.showShort("功能开发中……")
            }
        }
    }

//    private fun upDateTaskUI(pauseGetTask: Boolean) {
//        mContentView?.findViewById<ImageView>(R.id.iv_task)
//            ?.setImageResource(if (pauseGetTask) R.mipmap.task_start_icon else R.mipmap.task_pause_icon)
//        mContentView?.findViewById<TextView>(R.id.tv_task)?.text =
//            if (pauseGetTask) "  开始  " else "  暂停  "
//    }

    private fun arrowsShow() {
        mContentView?.gone()
        mArrowView?.visible()
    }

    private fun goNormal() {
        arrowsShow()
    }

    private fun annotationGo() {
        val intent = Intent()
        intent.component = ComponentName("com.android.notes", "com.android.notes.MainActivity")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            mContext!!.startActivity(intent)
        } catch (_: Exception) {
            Toaster.showShort("not found")
        }
    }

    fun removeOrSendMsg(remove: Boolean, send: Boolean) {
        if (remove) {
            mHandler.removeMessages(COUNT_DOWN_TAG)
        }
        if (send) {
            mHandler.sendEmptyMessageDelayed(
                COUNT_DOWN_TAG, COUNT_DOWN_TIME.toLong()
            )
        }
    }

    private fun killTaskApp(pkgName: String?, isStartMe: Boolean) {
        CoroutineWrapper.launch(false) {
            if (pkgName.isNullOrBlank()) {
                YLLogger.d("*** Task PkgName is Null")
            } else {
                YLLogger.d("# start kill Task App --> $pkgName")
                ProcessKiller.killApp(App.context, pkgName)
            }
            runCatching {
                GlobalTimer.getInstance().currentTime.removeObserver(timerObserver)
                GlobalTimer.getInstance().reset()
            }

            if (!isStartMe) return@launch

            val selfPkgName = App.context.packageName
            val context = App.context
            val intent = (context.packageManager.getLaunchIntentForPackage(selfPkgName) ?: Intent(
                Intent.ACTION_MAIN
            ).apply {
                component = ComponentName(selfPkgName, FrameActivity::class.java.name)
                addCategory(Intent.CATEGORY_LAUNCHER)
            }).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)

            App.runOnUiThread({
                val command = "monkey -p $selfPkgName -c android.intent.category.LAUNCHER 1"
                Shell.cmd(command).submit()
            }, 800)
        }
    }

    /**
     * when AccessibilityService is forced closed
     */
    fun clearCallbacks() {
        hasTask = false
        tmp = 0
        mHandler.removeCallbacksAndMessages(null)
        GlobalTimer.getInstance().currentTime.removeObserver(timerObserver)
    }

    fun findAndClick(vararg ids: Int) {
        ids.forEach {
            mContentView?.findViewById<View>(it)?.setOnClickListener(this)
        }
    }

    fun updatePosition(arrowView: View) {
        val contentView = mContentView ?: return
        if (contentView.parent == null || mContext == null || mWindowManager == null) return

        val wm = mWindowManager!!

        val arrowLocation = IntArray(2)
        arrowView.getLocationOnScreen(arrowLocation)
        val arrowX = arrowLocation[0]
        val arrowY = arrowLocation[1]
        val arrowWidth = arrowView.width
        val arrowHeight = arrowView.height

        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val contentWidth = contentView.measuredWidth
        val contentHeight = contentView.measuredHeight

        var contentX = arrowX + (arrowWidth - contentWidth) / 2
        var contentY = arrowY + (arrowHeight - contentHeight) / 2

        val screenWidth = DisplayUtil.widthPixels()
        val screenHeight = DisplayUtil.heightPixels()

        val margin11 = DisplayUtil.dip2px(8f)

        if (contentX < margin11) {
            contentX = margin11
        } else if (contentX + contentWidth > screenWidth - margin11) {
            contentX = screenWidth - contentWidth - margin11
        }

        if (contentY < margin11) {
            contentY = margin11
        } else if (contentY + contentHeight > screenHeight - margin11) {
            contentY = screenHeight - contentHeight - margin11
        }

        val finalWidth = min(contentWidth, screenWidth - margin11 * 2)
        val finalHeight = min(contentHeight, screenHeight - margin11 * 2)

        val params =
            contentView.layoutParams as? WindowManager.LayoutParams ?: WindowManager.LayoutParams()
                .apply {
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    format = PixelFormat.RGBA_8888
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    windowAnimations = R.style.ContentCoverAnim
                    gravity = Gravity.START or Gravity.TOP
                }

        params.width = finalWidth
        params.height = finalHeight
        params.x = contentX
        params.y = contentY

        if (contentView.parent == null) {
            wm.addView(contentView, params)
        } else {
            wm.updateViewLayout(contentView, params)
        }

        checkTaskTimeAndBtnStyle()
    }

    /**
     *  以下时间内"成功"、"成功并退出"的灰色的
     *  游戏-首次激活：最少15分钟
     *  游戏-留存：最少5分钟
     *  应用-首次激活：最少5分钟
     *  应用-留存：最少2分钟
     */
    private fun checkTaskTimeAndBtnStyle() {
        val currTask = TaskPollingManager.getCurrTask()
        val isGame = currTask?.isGameApp == true

        hasTask = when (currTask?.modeID) {
            2, 6 -> {    //  激活
                GlobalTimer.getInstance().checkTimed(if (isGame) 15 else 5)
            }

            3, 7 -> {   //  留存
                GlobalTimer.getInstance().checkTimed(if (isGame) 5 else 2)
            }

            else -> true    // 除了激活和留存其他都是true
        }

        if (killTaskAndSuccess == null) {
            killTaskAndSuccess = mContentView?.findViewById(R.id.force_stop_success)
        }
        if (taskSuccess == null) {
            taskSuccess = mContentView?.findViewById(R.id.report_task_success)
        }
//        YLLogger.e("hasTask--->$hasTask,\ntask$currTask")
        killTaskAndSuccess?.setViewVisualClickable(hasTask)
        taskSuccess?.setViewVisualClickable(hasTask)
    }

    fun LinearLayout.setViewVisualClickable(enabled: Boolean) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            when (child) {
                is ImageView -> {
                    val matrix = ColorMatrix()
                    matrix.setSaturation(if (enabled) 1f else 0f)
                    child.colorFilter = ColorMatrixColorFilter(matrix)
                }

                is TextView -> {
                    if (child.tag !is Int) {
                        child.tag = child.currentTextColor
                    }
                    val originalColor = child.tag as Int

                    val hsv = FloatArray(3)
                    Color.RGBToHSV(
                        Color.red(originalColor),
                        Color.green(originalColor),
                        Color.blue(originalColor),
                        hsv
                    )
                    hsv[1] = if (enabled) hsv[1] else 0f
                    val grayColor = Color.HSVToColor(Color.alpha(originalColor), hsv)
                    child.setTextColor(grayColor)
                }
            }
        }

        val background = background
        if (background != null) {
            background.colorFilter = if (enabled) {
                null
            } else {
                PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN)
            }
        }
    }

}