package com.cymf.keyshot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import com.cymf.keyshot.constant.Constants
import com.cymf.keyshot.ktx.exc
import com.cymf.keyshot.tasks.TestClick.printNodeInfo
import com.cymf.keyshot.utils.AdAssistsTool
import com.cymf.keyshot.utils.AssistsCore
import com.cymf.keyshot.utils.CoroutineWrapper
import com.cymf.keyshot.utils.OverlayManager
import com.cymf.keyshot.utils.PermissionUtil
import com.cymf.keyshot.utils.SPUtil
import com.cymf.keyshot.utils.TouchEventLogger
import com.cymf.keyshot.utils.YLLogger
import com.cymf.keyshot.widget.SideBarArrow
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 优化版无障碍服务核心类
 * 增强了服务稳定性和内存管理
 */
open class AssistsService : AccessibilityService() {

    //    private var mReceiver: SideBarHideReceiver? = null
    private var mRightArrowBar: SideBarArrow? = null
    private var mLeftArrowBar: SideBarArrow? = null
    private var overlayManager: OverlayManager? = null

    private var touchLogger: TouchEventLogger? = null
    private val eventLog = mutableListOf<String>()

    companion object {
        // 服务重启计数器
        val restartCount = AtomicLong(0)
        val lastRestartTime = AtomicLong(0)

        // 内存优化：使用弱引用避免内存泄漏
        var isServiceActive = AtomicBoolean(false)

        var isTargetAppRunning = true
        val _startTime by lazy { AtomicInteger(0) }
        val _isOnTask by lazy { AtomicBoolean(false) }
        var lastPageFingerprint: String? = null
        var lastPage: String? = null
        var pageEnterTime: Long = -1L
        val debounceMs = 1000L
        var lastUpdateTime: Long = 0L
        val maxTimeout = 90_000L
        val handler by lazy { Handler(Looper.getMainLooper()) }


        /**
         * 全局服务实例
         */
        @Volatile
        var instance: AssistsService? = null
            private set

        /**
         * 服务监听器列表 - 使用线程安全集合
         */
        private val listeners: MutableList<AssistsServiceListener> =
            Collections.synchronizedList(arrayListOf<AssistsServiceListener>())

        var isAdActivityForeground = false

        /**
         * 检查服务是否活跃
         */
        fun isServiceActive(): Boolean {
            return isServiceActive.get()
        }
    }

    /**
     * 检查辅助功能服务是否启用
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val serviceName = "${packageName}/${AssistsService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            enabledServices?.contains(serviceName) == true
        } catch (e: Exception) {
            YLLogger.e("AccessibilityServiceMonitor 检查服务状态失败", e)
            false
        }
    }

    fun postLogData(data: String?) {
        data?.apply {
            mRightArrowBar?.addLogText(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val currentTime = System.currentTimeMillis()
        val count = restartCount.incrementAndGet()

        YLLogger.w("AssistsService onCreate - 重启次数: $count")

        lastRestartTime.set(currentTime)
        instance = this
        isServiceActive.set(true)

        try {
            val info = serviceInfo ?: return
            @Suppress("DEPRECATION")
            info.flags =
                info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            serviceInfo = info
//            TaskPollingManager.setTapBannerAdView(false)

            // 重置状态
            resetServiceState()
        } catch (e: Exception) {
            YLLogger.e("AssistsService onCreate 异常", e)
        }
    }

    /**
     * 重置服务状态
     */
    private fun resetServiceState() {
        _isOnTask.set(false)
        _startTime.set(0)
        lastPageFingerprint = null
        lastPage = null
        pageEnterTime = -1L
        lastUpdateTime = 0L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        YLLogger.d("AssistsService onStartCommand")
        if (intent?.getIntExtra(Constants.TAG_RESET_TOUCH_BAR, 0) == 1) {
            try {
                showSlideView()
            } catch (e: Exception) {
                YLLogger.e("showSlideView 异常", e)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun showSlideView() {
        if (!isServiceActive.get()) return
        release()
        createTouchWeight()
    }


    var targetAppRunningCheckingJob: Job? = null
    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                if (!isServiceActive.get()) {
                    YLLogger.w("服务已停止，停止检查任务")
                    return
                }

                checkTouchWeight()

                if (!_isOnTask.get()) {
//                    YLLogger.w("++++++++++ 任务未执行 跳过 ++++++++++ ")
                    handler.postDelayed(this, 1000)
                    return
                }

                val currentTime = System.currentTimeMillis()
                if (pageEnterTime > 0) {
                    val duration = currentTime - pageEnterTime
                    if (duration >= maxTimeout) {
                        lastPageFingerprint?.let {
                            YLLogger.d("页面卡住超过 $maxTimeout ms，准备回调: $lastPage")
                            instance?.rootInActiveWindow.printNodeInfo()
                            pageEnterTime = currentTime
                            lastPageFingerprint = null

                            // 安全地通知监听器
                            notifyListenersSafely { listener ->
                                listener.onStuckPage(lastPage)
                            }
                        }
                    }
                }

                val times = _startTime.incrementAndGet()
                targetAppRunningCheckingJob?.cancel()

                var isGooglePlayCheck = false
                targetAppRunningCheckingJob = CoroutineWrapper.launch {
                    if (times % 11 == 0) {
                        var packageName = ""
                        if (isGooglePlayCheck) {
                            packageName = Constants.PLAY_PACKAGE
                        }
                        isTargetAppRunning =
                            AssistsCore.isTargetAppRunning(packageName)
                    }
                }

                notifyListenersSafely { listener ->
                    listener.onTaskTime(times)
                }

                handler.postDelayed(this, 1000)
            } catch (e: Exception) {
                YLLogger.e("checkRunnable 异常", e)
                handler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * 安全地通知监听器，避免异常导致服务崩溃
     */
    private inline fun notifyListenersSafely(action: (AssistsServiceListener) -> Unit) {
        try {
            synchronized(listeners) {
                listeners.forEach { listener ->
                    try {
                        action(listener)
                    } catch (e: Exception) {
                        YLLogger.e("监听器回调异常", e)
                    }
                }
            }
        } catch (e: Exception) {
            YLLogger.e("通知监听器异常", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
            || event?.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
            || event?.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED
            || event?.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
            || event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            val source = event.source
            if (source != null) {
                val bounds = Rect()
                source.getBoundsInScreen(bounds)
                val clickX = bounds.centerX()
                val clickY = bounds.centerY()

                YLLogger.d(
                    "事件类型: " + event.eventType +
                            ", 坐标: (" + clickX + ", " + clickY + ")" +
                            ", 控件文本: " + event.text
                )
            } else {
                YLLogger.d(
                    "事件类型: " + event.eventType +
                            ", node=: " + event.toString()
                )
            }
            if (!isServiceActive.get()) return

            pageEnterTime = System.currentTimeMillis()

            instance = this

            runCatching {
                if (!_isOnTask.get()) return@runCatching

                if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    return@runCatching
                }

                if (event.packageName == this.packageName || event.className == Constants.READER_LM) {
                    return@runCatching
                }

                if (AssistsCore.isCurrentScreenLauncher(event.packageName?.toString())) {
                    return@runCatching
                }

                val currentFingerprint = extractPageFingerprint(event)

                if (currentFingerprint?.contains("AppErrorDialog", ignoreCase = true) == true) {
                    YLLogger.d("跳过系统错误弹窗页面: $currentFingerprint")
                    return@runCatching
                }

                if (System.currentTimeMillis() - lastUpdateTime < debounceMs) return
                lastUpdateTime = System.currentTimeMillis()

                if (currentFingerprint == lastPageFingerprint) {
                    return@runCatching
                }

                YLLogger.d("页面变化: $currentFingerprint")
                lastPage = "${event.packageName}/${event.className}"
                lastPageFingerprint = currentFingerprint
                pageEnterTime = System.currentTimeMillis()

            }.onFailure {
                val sb = StringBuilder()
                sb.append("${this::class.java.simpleName} 回调 onStuckPage 出现异常:\n")
                it.exc(sb)
                YLLogger.e(sb.toString())
            }

            // 安全地通知监听器
            runCatching {
                event?.let { evt ->
                    notifyListenersSafely { listener ->
                        listener.onAccessibilityEvent(evt)
                    }
                }
            }

            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val className = event.className?.toString()
                isAdActivityForeground = AdAssistsTool.checkIsAdPage4Self(className)
            }
        }
    }

    @SuppressLint("RtlHardcoded", "InflateParams", "UnspecifiedRegisterReceiverFlag")
    private fun createTouchWeight() {
        try {
            val position = SPUtil.TOUCH_LOCATION
            val windowManager = application.getSystemService(WINDOW_SERVICE) as WindowManager

            if (position == 0 || position == 2) {
                mRightArrowBar = SideBarArrow()
                val mArrowRight: LinearLayout? =
                    mRightArrowBar?.getView(this, false, windowManager, this)
                mLeftArrowBar?.setAnotherArrowBar(mArrowRight!!)
            }

            if (position == 0 || position == 1) {
                mLeftArrowBar = SideBarArrow()
                val mArrowLeft: LinearLayout =
                    mLeftArrowBar!!.getView(this, true, windowManager, this)!!
                mRightArrowBar?.setAnotherArrowBar(mArrowLeft)
            }
        } catch (e: Exception) {
            YLLogger.e("createTouchWeight 异常", e)
        }
    }

    @SuppressLint("RtlHardcoded", "InflateParams", "UnspecifiedRegisterReceiverFlag")
    private fun checkTouchWeight() {
        try {
            if (!PermissionUtil.isCanDrawOverlays(this)) {
                return
            }
            touchLogger?.start()
            overlayManager?.showOverlay()

            val position = SPUtil.TOUCH_LOCATION

            if (position == 0 || position == 2) {
                if (mRightArrowBar == null) {
                    val windowManager =
                        application.getSystemService(WINDOW_SERVICE) as WindowManager
                    mRightArrowBar = SideBarArrow()
                    val mArrowRight: LinearLayout? =
                        mRightArrowBar?.getView(this, false, windowManager, this)
                    mLeftArrowBar?.setAnotherArrowBar(mArrowRight!!)
                }
            }

            if (position == 0 || position == 1) {
                if (mLeftArrowBar == null) {
                    val windowManager =
                        application.getSystemService(WINDOW_SERVICE) as WindowManager
                    mLeftArrowBar = SideBarArrow()
                    val mArrowLeft: LinearLayout =
                        mLeftArrowBar!!.getView(this, true, windowManager, this)!!
                    mRightArrowBar?.setAnotherArrowBar(mArrowLeft)
                }
            }

            if (overlayManager == null) {
                overlayManager = OverlayManager(this)
            }
//            if (touchLogger == null) {
//                touchLogger = TouchEventLogger(object : TouchEventLogger.TouchCallback {
//                    override fun onDown(x: Float, y: Float, timestamp: Long) {
//                        logEvent("DOWN: ($x, $y)")
//                    }
//
//                    override fun onMove(x: Float, y: Float, timestamp: Long) {
//                        logEvent("MOVE: ($x, $y)")
//                    }
//
//                    override fun onUp(timestamp: Long) {
//                        logEvent("UP")
//                    }
//
//                    override fun onLog(message: String) {
//                        YLLogger.d( message)
//                    }
//
//                    override fun onError(error: String) {
//                        Toaster.showShort("错误: $error")
//                    }
//                })
//            }
        } catch (e: Exception) {
            YLLogger.e("checkTouchWeight 异常", e)
        }
    }

    private fun logEvent(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val log = "[$time] $msg"
        eventLog.add(log)
        YLLogger.d(log)
    }

    override fun onDestroy() {
        isServiceActive.set(false)

        try {
            release()
            handler.removeCallbacks(checkRunnable)

            notifyListenersSafely { listener ->
                listener.onServiceDestroy(this)
            }

        } catch (e: Exception) {
            YLLogger.e("onDestroy 异常", e)
        } finally {
            super.onDestroy()
        }
    }

    private fun release() {
        try {
            mRightArrowBar?.clearAll()
            mRightArrowBar = null

            mLeftArrowBar?.clearAll()
            mLeftArrowBar = null

//            mReceiver?.let {
//                try {
//                    unregisterReceiver(it)
//                } catch (e: Exception) {
//                    YLLogger.e("unregisterReceiver 异常", e)
//                }
//                mReceiver = null
//            }
            overlayManager?.let {
                it.hideOverlay()
                overlayManager = null
            }
            touchLogger?.let {
                it.stop()
                eventLog.clear()
                touchLogger = null
            }
        } catch (e: Exception) {
            YLLogger.e("release 异常", e)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        YLLogger.w("AssistsService onUnbind")

        isServiceActive.set(false)
        instance = null

        notifyListenersSafely { listener ->
            listener.onUnbind()
        }

        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        YLLogger.w("无障碍服务 onInterrupt")

        notifyListenersSafely { listener ->
            listener.onInterrupt()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        YLLogger.w("AssistsService onServiceConnected - 重启次数: ${restartCount.get()}")

        instance = this
        isServiceActive.set(true)

        try {
            handler.post(checkRunnable)

            notifyListenersSafely { listener ->
                listener.onServiceConnected(this)
            }
        } catch (e: Exception) {
            YLLogger.e("onServiceConnected 异常", e)
        }
    }

    // 页面指纹识别相关方法保持不变...
    private fun extractPageFingerprint(event: AccessibilityEvent): String? {
        val packageName = event.packageName?.toString() ?: return null
        val stableClassName = findStableClassName(event.source) ?: event.className?.toString()
        val keyTexts = getKeyTexts(event.source).take(5)
        val layoutFeatures = getLayoutFeatures(event.source)

        if (!stableClassName.isNullOrEmpty()) {
            val simpleCls = getSimpleName(stableClassName)
            val base = "$packageName.$simpleCls"

            val parts = mutableListOf<String>()
            parts.add(base)

            if (keyTexts.isNotEmpty()) {
                parts.add("keys:${TextUtils.join(",", keyTexts)}")
            }

            if (layoutFeatures.isNotEmpty()) {
                parts.add("layout:$layoutFeatures")
            }

            return TextUtils.join("|", parts)
        }

        if (keyTexts.isNotEmpty()) {
            return "$packageName.CustomWindow|keys:${TextUtils.join(",", keyTexts)}"
        }

        return "$packageName.Window"
    }

    private fun findStableClassName(node: AccessibilityNodeInfo?): String? {
        var current = node
        while (current != null) {
            val cls = current.className?.toString()
            if (!cls.isNullOrEmpty() && !isKnownViewClass(cls)) {
                return cls
            }
            current = current.parent
        }
        return null
    }

    private fun isKnownViewClass(className: String): Boolean {
        return className.startsWith("android.view.") ||
                className.startsWith("android.widget.") ||
                className.startsWith("androidx.core.widget.") ||
                className.startsWith("com.google.android.material.")
    }

    private fun getSimpleName(className: String): String {
        return className.split("\\$")[0].split("\\.").last()
    }

    private fun getKeyTexts(node: AccessibilityNodeInfo?): List<String> {
        if (node == null) return emptyList()
        val texts = mutableListOf<String>()
        collectKeyTexts(node, texts)
        return texts.distinct().sortedByDescending { it.length }.take(10)
    }

    private fun collectKeyTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        val text = node.text?.toString()
        if (!text.isNullOrEmpty() && text.isNotBlank() && text.length in 2..30) {
            texts.add(text)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectKeyTexts(it, texts) }
        }
    }

    private fun getLayoutFeatures(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""

        val features = mutableSetOf<String>()
        detectBottomTabs(node, features)
        detectNavigationDrawer(node, features)

        return TextUtils.join(",", features)
    }

    private fun detectBottomTabs(node: AccessibilityNodeInfo, features: MutableSet<String>) {
        if (node.className?.toString()?.contains("BottomNavigationView", true) == true ||
            node.className?.toString()
                ?.contains("LinearLayout", true) == true && node.childCount > 3
        ) {
            features.add("bottom_tabs")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { detectBottomTabs(it, features) }
        }
    }

    private fun detectNavigationDrawer(
        node: AccessibilityNodeInfo,
        features: MutableSet<String>
    ) {
        if (node.className?.toString()?.contains("DrawerLayout", true) == true) {
            features.add("drawer")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { detectNavigationDrawer(it, features) }
        }
    }


}
