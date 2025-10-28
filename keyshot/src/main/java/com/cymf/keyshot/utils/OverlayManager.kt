package com.cymf.keyshot.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView
import com.cymf.keyshot.bean.NodeInfoData
import com.cymf.keyshot.ktx.addBorder
import com.cymf.keyshot.service.AssistsService
import com.cymf.keyshot.utils.AssistsCore.collectAllNodeInfo
import com.hjq.toast.Toaster
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

class OverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: AppCompatTextView? = null
    private val eventLog = mutableListOf<TouchSequence>()
    private val eventList = JSONArray()
    private var currentSequence: TouchSequence? = null

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay() {
        if (overlayView != null) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayView = object : AppCompatTextView(context) {
            init {
                isFocusable = false
                isClickable = false
                isLongClickable = false
                isFocusableInTouchMode = false
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                isSoundEffectsEnabled = false
                isHapticFeedbackEnabled = false

                gravity = Gravity.CENTER
                textSize = 22f
                setTextColor(Color.RED)
            }

            override fun onTouchEvent(event: MotionEvent?): Boolean {
                event?.let { ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            overlayView?.addBorder(Color.RED)

                            currentSequence = TouchSequence()
                            currentSequence?.addPoint(
                                ev.rawX, ev.rawY, ev.downTime, ev.actionMasked
                            )
                            YLLogger.d("开始触摸: (${ev.rawX}, ${ev.rawY})")
                        }

                        MotionEvent.ACTION_MOVE -> {
                            currentSequence?.addPoint(
                                ev.rawX, ev.rawY, ev.downTime, ev.actionMasked
                            )
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            currentSequence?.let { sequence ->
                                sequence.addPoint(ev.rawX, ev.rawY, ev.downTime, ev.actionMasked)
                                eventLog.add(sequence.copy())
                                YLLogger.d("手势完成: $sequence")
                                replayLastGestureSmart()
                            }
                            currentSequence = null
                        }

                        else -> {
                            // 多指触控等其他情况
                        }
                    }
                }
                return true
            }
        }.apply {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setEmpty()
                }
            }
            this.addBorder(Color.RED)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN /*or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE*/,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        try {
            windowManager?.addView(overlayView, params)
            YLLogger.d("悬浮窗已成功添加")
        } catch (e: Exception) {
            Toaster.showLong("添加悬浮窗失败!")
            YLLogger.e("添加悬浮窗失败", e)
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                YLLogger.d("悬浮窗已移除")
            } catch (e: Exception) {
                Toaster.showLong("移除悬浮窗失败!")
                YLLogger.e("移除悬浮窗失败", e)
            }
        }
        overlayView = null
    }

    fun getEventLog(): List<TouchSequence> = eventLog.toList()

    fun clearLog() = eventLog.clear()


    fun replayLastGestureSmart() {
        val sequences = getEventLog()
        if (sequences.isEmpty()) {
            YLLogger.d("无手势可回放")
            return
        }

        val lastSequence = sequences.last()
//        val duration = lastSequence.getDuration() // 手势总时长
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    val currActivity = AssistsCore.getCurrentActivity() ?: "null"
                    val collectAllNodeInfo = lastSequence.getEndPoint()?.let {
                        val findNodeAt = AssistsCore.findNodeAt(it.x, it.y)
                        findNodeAt.collectAllNodeInfo()
                    }
                    val singleEventJson = createGestureNodeJson(
                        lastSequence.toString(),
                        currActivity,
                        collectAllNodeInfo
                    )
                    eventList.put(singleEventJson)  //  增加到一起
                    YLLogger.e(singleEventJson.toString())
                    // TODO: 最后再集中上传并重置！
                }
                YLLogger.d("--------------开始回放动作--------------")
                withOverlayDisabled {
//                    if (duration <= 1000L) {
//                        replayWithGestureDescription(lastSequence)
//                    } else {
                    replayWithInputSwipe(lastSequence)
//                    }
                }
            } catch (e: Exception) {
                "回放过程中发生异常\n请联系开发人员".also { overlayView?.text = it }

                setOverlayTouchTransparent(true)    //  取消蒙层拦截效果
                //  之后再进行其他状态恢复或者排查问题
                overlayView?.addBorder(Color.LTGRAY)
                YLLogger.e("回放过程中发生异常", e)
            }
        }
    }

    private suspend fun replayWithGestureDescription(sequence: TouchSequence) {
        val stroke = sequence.toGestureStroke() ?: throw IllegalArgumentException("无法生成 Stroke")
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val service = AssistsService.instance
        if (service != null && service.isAccessibilityServiceEnabled()) {
            suspendCancellableCoroutine<Unit> { cont ->
                val callback = object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        YLLogger.d("GestureDescription 回放完成")
                        cont.resume(Unit) { _, _, _ -> }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        YLLogger.w("GestureDescription 回放被取消")
                        cont.resume(Unit) { _, _, _ -> }
                    }
                }
                if (service.dispatchGesture(gesture, callback, null)) {
                    cont.invokeOnCancellation {
                        // 取消时清理
                    }
                } else {
                    cont.resumeWithException(RuntimeException("dispatchGesture 失败"))
                }
            }
        } else {
            throw IllegalStateException("辅助功能未启用")
        }
    }

    // TODO: 暂时无法区分长按还是拖动
//    private suspend fun replayWithMultiSwipe(sequence: TouchSequence) {
//        val points = TouchSampling.optimizePath(sequence, epsilon = 10f, maxPoints = 15)
//        if (points.size < 2) return
//
//        val cmds = mutableListOf<String>()
//
//        val first = points[0]
//
//        if (sequence.getDuration() > 300) {
//            cmds.add("input swipe ${first.x} ${first.y} ${first.x + 1} ${first.y + 1} 50")
//            cmds.add("sleep 0.6") // 等待长按触发
//        }
//
//        for (i in 0 until points.size - 1) {
//            val from = points[i]
//            val to = points[i + 1]
//
//            val duration = to.delayAfterMs.coerceAtLeast(80).toLong()
//
//            cmds.add("input swipe ${from.x} ${from.y} ${to.x} ${to.y} $duration")
//
//            if (duration < 100) {
//                cmds.add("sleep ${duration / 2000.0}")
//            }
//        }
//
//        val command = cmds.joinToString("; ")
//        YLLogger.d("执行多段 swipe: $command")
//
//        withContext(Dispatchers.IO) {
//            val result = Shell.cmd(command).enqueue().get()
//            if (!result.isSuccess) {
//                throw RuntimeException("多段 swipe 执行失败: ${result.out}")
//            } else {
//                YLLogger.d("input swipe 执行成功")
//            }
//        }
//    }

    private suspend fun replayWithInputSwipe(sequence: TouchSequence) {
        if (!sequence.isValid()) throw IllegalArgumentException("无效手势")

        val start = sequence.getStartPoint() ?: throw NullPointerException("起始点为空")
        val end = sequence.getEndPoint() ?: throw NullPointerException("终点为空")

        val startX = start.x.toInt()
        val startY = start.y.toInt()
        val endX = end.x.toInt()
        val endY = end.y.toInt()

        val command = "input swipe $startX $startY $endX $endY ${sequence.getDuration()}"
        YLLogger.d("执行长手势: $command")

        withContext(Dispatchers.IO) {
            try {
                val exec = Shell.cmd(command).enqueue()
                val result = exec.get()
                if (!result.isSuccess) {
                    throw RuntimeException("input swipe 执行失败，退出码: ${result.code}")
                } else {
                    YLLogger.d("input swipe 执行成功")
                }
            } catch (e: Exception) {
                YLLogger.e("执行 input swipe 失败", e)
                throw e
            }
        }
    }

    /**
     * 表示一次完整的触摸操作：从 DOWN 到 UP/CANCEL
     */
    data class TouchSequence(
        private val points: MutableList<Point> = mutableListOf(),
        private var startTime: Long = 0L,
        private var recordedDownTime: Long = 0L // 保存原始 downTime
    ) {
        data class Point(
            val x: Float, val y: Float, val timeOffset: Long, // 相对于 startTime
            val action: Int
        )

        fun addPoint(x: Float, y: Float, downTime: Long, action: Int) {
            if (points.isEmpty()) {
                startTime = System.currentTimeMillis()
                recordedDownTime = downTime
            }
            val timeOffset = System.currentTimeMillis() - startTime
            points.add(Point(x, y, timeOffset, action))
        }

        fun getStartPoint(): Point? = points.firstOrNull()
        fun getEndPoint(): Point? = points.lastOrNull()

        fun getPointSize(): Int = points.size
        fun getPointList(): MutableList<Point> = points

        fun getDuration(): Long {
            return if (points.size >= 2) {
                points.last().timeOffset - points.first().timeOffset
            } else {
                0
            }
        }

        fun isValid(): Boolean = points.size >= 2

        @SuppressLint("NewApi")
        fun toGestureStroke(): GestureDescription.StrokeDescription? {
            if (points.isEmpty()) return null

            val path = Path()
            var first = true

            for (point in points) {
                if (first) {
                    path.moveTo(point.x, point.y)
                    first = false
                } else {
                    path.lineTo(point.x, point.y)
                }
            }

            // 计算持续时间（从第一个点到最后一个点）
            val duration = if (points.size > 1) {
                max(1L, points.last().timeOffset) // 至少为 1ms
            } else {
                50 // 单点手势，设为 50ms
            }

            // 确保最终 duration 至少为 1，且不超过 1000
            val finalDuration = min(max(1L, duration), 1000L)

            if (duration > 1000) {
                YLLogger.w("手势过长：$duration ms，已截断为 1000ms")
            }

            return try {
                GestureDescription.StrokeDescription(
                    path, 0,      // 延迟 0ms 开始
                    finalDuration, true    // 允许中断
                )
            } catch (e: IllegalArgumentException) {
                YLLogger.e("创建 StrokeDescription 失败: ${e.message}")
                null
            }
        }

        override fun toString(): String {
            val actionNames = mapOf(
                MotionEvent.ACTION_DOWN to "DOWN",
                MotionEvent.ACTION_MOVE to "MOVE",
                MotionEvent.ACTION_UP to "UP",
                MotionEvent.ACTION_CANCEL to "CANCEL"
            )
            val steps = points.joinToString(" -> ") { p ->
                "${actionNames[p.action] ?: "UNKNOWN"}[${p.timeOffset}](${p.x.toInt()},${p.y.toInt()})"
            }
            return "Gesture: $steps"
        }

        fun copy(): TouchSequence {
            return TouchSequence(points.toMutableList(), startTime, recordedDownTime)
        }
    }

    private suspend fun <T> withOverlayDisabled(
        delayRestoreMs: Long = 200, block: suspend () -> T
    ): T {
        overlayView?.addBorder(Color.BLUE)
        setOverlayTouchTransparent(true)
        return try {
            block()
        } finally {
            withContext(Dispatchers.Main) {
                delay(delayRestoreMs)
                setOverlayTouchTransparent(false)
                overlayView?.addBorder(Color.CYAN)
            }
        }
    }

    fun setOverlayTouchTransparent(isTransparent: Boolean) {
        val params = overlayView?.layoutParams as? WindowManager.LayoutParams ?: return
        if (isTransparent) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
        windowManager?.updateViewLayout(overlayView, params)
    }


    /**
     * 将一次手势事件和其对应的 UI 节点信息打包为 JSONObject
     */
    fun createGestureNodeJson(
        gestureLog: String,
        activityName: String,
        nodeInfoList: List<NodeInfoData>?
    ): JSONObject {
        val json = JSONObject()

        json.put("gesture", gestureLog.trim())
        json.put("activity", activityName.trim())
        json.put("timestamp", System.currentTimeMillis())

        val nodesJson = JSONArray()
        if (nodeInfoList != null) {
            for (node in nodeInfoList) {
                val nodeJson = JSONObject().apply {
                    put("text", node.text?.toString() ?: "")
                    put("contentDescription", node.contentDescription?.toString() ?: "")
                    put("viewId", node.viewId ?: "")
                    put("className", node.className ?: "")
                    put("isClickable", node.isClickable)
                    put("isLongClickable", node.isLongClickable)
                    put("depth", node.depth)

                    val bounds = node.bounds
                    if (bounds != null) {
                        put(
                            "bounds",
                            "[${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}]"
                        )
                    } else {
                        put("bounds", "[]")
                    }
                }
                nodesJson.put(nodeJson)
            }
        }
        json.put("nodes", nodesJson)
        return json
    }
}