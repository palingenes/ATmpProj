package com.cymf.humanbehaviorcollector

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * 点击、滑动等触摸事件封装基类
 * @author wzy
 */
abstract class AbsTapActivity : AppCompatActivity() {

    private lateinit var relativeLayout: RelativeLayout
    private lateinit var drawingView: DrawingView

    private var isLongPress = false
    private var isSwipe = false
    private var downTime = 0L
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop } // 触发滑动的最小距离
    private var totalDeltaX = 0f
    private var totalDeltaY = 0f
    private var hasMoved = false
    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        isLongPress = true
    }

    private val startPositions = SparseArray<PointF>() // 起始坐标
    private val movePoints = SparseArray<MutableList<PointF>>() // 移动轨迹
    private val activeGestures = SparseArray<CompleteGesture>() // 完整手势
    private val currentGestureSteps = mutableListOf<GestureStep>() // 临时 UI 显示
    private var direction: SwipeDirection = SwipeDirection.NONE

    private var hitView: View? = null     // 查找是否点击/滑动在某个动态 View 上
    private val dynamicViews = mutableListOf<View>()// 保存所有动态添加的 View 及其边界信息

    private var allowAdjacentHit: Boolean = false
    private var adjacentHitRange: Float = 20f // 默认允许周围 20px 内也算命中

    private val screenWidth by lazy { resources.displayMetrics.widthPixels }
    private val screenHeight by lazy { resources.displayMetrics.heightPixels }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // 隐藏导航栏
                or View.SYSTEM_UI_FLAG_FULLSCREEN // 隐藏状态栏
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) // 滑动时暂时显示系统UI

        setContentView(layoutId())
        addDrawingView()
        initDynamicViews(dynamicViews)
        initView()
        dynamicViews.forEach { saveOriginalBackground(it) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addDrawingView() {
        drawingView = DrawingView(this, null).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setOnTouchListener { _, event ->
                handleTouchEvent(event)
                drawingView.onTouchEvent(event)
                true
            }
        }
        relativeLayout = findViewById(R.id.relative_layout)
        relativeLayout.addView(drawingView)
    }

    /**
     * 触摸监听及数据打印/保存
     */
    private fun handleTouchEvent(event: MotionEvent) {
        val pointerCount = event.pointerCount
        val actionMasked = event.actionMasked
        val actionIndex = event.actionIndex

        when (actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                currentGestureSteps.clear() // 清除上一次的步骤
                // 对于每一个触点进行处理
                for (i in 0 until pointerCount) {
                    startNewGesture(event, i)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // 移动时记录每个触点的位置
                for (i in 0 until pointerCount) {
                    recordMove(event, i)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                // 处理手指抬起或取消的情况
                for (i in 0 until pointerCount) {
                    if (actionMasked == MotionEvent.ACTION_POINTER_UP && i == actionIndex ||
                        actionMasked == MotionEvent.ACTION_UP ||
                        actionMasked == MotionEvent.ACTION_CANCEL
                    ) {
                        endCurrentGesture(event, i, actionMasked)
                    }
                }
                handler.removeCallbacks(longPressRunnable)
            }
        }
        // 更新 TextView 显示最新的手势记录
        updateTextView()
    }

    private fun startNewGesture(event: MotionEvent, pointerIndex: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        // 初始化位置和轨迹
        startPositions.put(pointerId, PointF(x, y))
        movePoints.put(pointerId, mutableListOf(PointF(x, y)))

        checkInDynamicView(x, y)
        // 初始化完整手势记录
        val step = GestureStep("ACTION_DOWN", pointerId, x, y, event.pressure)
        currentGestureSteps.add(step)

        val gesture = CompleteGesture(startAction = "ACTION_DOWN", pointerId = pointerId)
        gesture.downView = hitView
        gesture.steps.add(step)
        activeGestures.put(pointerId, gesture)

        resetStateAndDownTime()
        handler.postDelayed(longPressRunnable, 500)
    }

    private fun recordMove(event: MotionEvent, pointerIndex: Int) {
//        for (h in 0 until event.historySize) {
//            val historicalX = event.getHistoricalX(pointerIndex, h)
//            val historicalY = event.getHistoricalY(pointerIndex, h)
//            LogUtils.e("Pointer $pointerIndex historical ($historicalX, $historicalY)")
//        }

        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        val gesture = activeGestures[pointerId] ?: return
        val downView = gesture.downView

        // 更新轨迹记录
        movePoints[pointerId]?.add(PointF(x, y))
        currentGestureSteps.add(GestureStep("", pointerId, x, y, event.pressure).apply {
            timestamp = System.currentTimeMillis()
        })
        activeGestures[pointerId]?.steps?.add(GestureStep("", pointerId, x, y, event.pressure))

        // 滑动方向与位移判断
        val startPoint = startPositions[pointerId]
        val deltaX = x - startPoint.x
        val deltaY = y - startPoint.y
        direction = calculateSwipeDirection(deltaX, deltaY)

        totalDeltaX += abs(deltaX)
        totalDeltaY += abs(deltaY)

        if (totalDeltaX > touchSlop || totalDeltaY > touchSlop) {
            hasMoved = true
            handler.removeCallbacks(longPressRunnable)
        }

        checkInDynamicView(x, y)

        // 检查是否滑出原始 View
        if (downView != null && !isTouchInView(downView, x, y)) {
            gesture.movedOutOfView = true
        }
        startPoint?.set(x, y)
    }


    private fun endCurrentGesture(event: MotionEvent, pointerIndex: Int, actionMasked: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        val step = GestureStep(
            when (actionMasked) {
                MotionEvent.ACTION_UP -> "ACTION_UP"
                MotionEvent.ACTION_POINTER_UP -> "ACTION_POINTER_UP"
                else -> "ACTION_CANCEL"
            },
            pointerId,
            x,
            y, event.pressure
        ).apply { timestamp = System.currentTimeMillis() }
        currentGestureSteps.add(step)

        activeGestures[pointerId]?.let { gesture ->
            gesture.steps.add(step)
            gesture.isEnded = true

            // 获取最终抬起时的 hitView
            val upView = findHitView(x, y)

            // 判断是否全程都在原始 View 内（包括所有 move 没有移出）
            val downView = gesture.downView
            val stillInDownView = downView != null && isTouchInView(downView, x, y)
            val allInSameView = downView != null && stillInDownView && !gesture.movedOutOfView
            gesture.allInSameView = allInSameView

            // 是否 swipe
            gesture.isSwipe = this.hasMoved && !allInSameView
            gesture.isLongPress = this.isLongPress

            //  将 upView 作为参数传入 onGestureCompleted
            onGestureCompleted(gesture, upView)
        }

        hitView?.let { restoreHitViewBackground(it) }
        hitView = null

        // 清理状态
        startPositions.remove(pointerId)
        movePoints.remove(pointerId)
        activeGestures.remove(pointerId)

        // 重置状态变量
        resetStateAndDownTime()
    }

    /**
     * 手势完成后回调
     */
    private fun onGestureCompleted(gesture: CompleteGesture, upView: View?) {
        val steps = gesture.steps
        if (steps.isEmpty()) return

        val first = steps.first()
        val last = steps.last()

        val duration = last.timestamp - first.timestamp
        val path = movePoints[gesture.pointerId]
        var size = 0
        val touchPoints = path?.joinToString("|") { point ->
            val xPercent = (point.x / screenWidth * 100).format(3)
            val yPercent = (point.y / screenHeight * 100).format(3)
            size++
            "$xPercent,$yPercent(${steps[size-1].pressure})"
        } ?: ""
//        LogUtils.e(path)
//        LogUtils.e(touchPoints)

        val downView = gesture.downView
        val allInSameView = gesture.allInSameView

        val actionType = when {
            allInSameView -> when {
                gesture.isLongPress -> "longClick"
                else -> "click"
            }

            downView == null -> when {
                gesture.isLongPress -> "longClick"
                hasMoved -> "slide_${direction.name.lowercase()}"
                else -> "click"
            }

            else -> when {
                gesture.isLongPress -> "longClick"
                hasMoved -> "slide_${direction.name.lowercase()}"
                else -> "click"
            }
        }

        val rectStr = when {
            allInSameView -> getPercentRect(downView!!)
            upView != null -> getPercentRect(upView)
            downView != null -> getPercentRect(downView)
            else -> "-1,-1,-1,-1"
        }

        val record = GestureRecord(
            viewRect = rectStr,
            downTime = duration,
            touch = touchPoints,
            action = actionType
        )
        writeGestureToJson(record)
    }

    /**
     * 检查手指是否处于目标view中
     */
    private fun checkInDynamicView(x: Float, y: Float) {
        // 检查是否处于某个 dynamicView 内部
        var newHitView: View? = null
        for (view in dynamicViews) {
            if (isTouchInView(view, x, y)) {
                newHitView = view
                break
            }
        }
        if (newHitView != hitView) {
            hitView?.let { restoreHitViewBackground(it) }
            hitView = newHitView
            hitView?.let { updateHitViewBackground(it) }
        } else hitView?.let { updateHitViewBackground(it) }
    }


    private fun writeGestureToJson(record: GestureRecord) {
        lifecycleScope.launch {
            val fileName = "page${suffixName()}_${screenWidth}_${screenHeight}_${touchSlop}.json"
            GestureRecordManager.appendRecord(this@AbsTapActivity, fileName, record)
        }
    }

    private fun resetStateAndDownTime() {
        downTime = 0L
        isLongPress = false
        isSwipe = false
        hasMoved = false
        totalDeltaX = 0f
        totalDeltaY = 0f
    }

    // 辅助函数：获取 View 百分比坐标
    private fun getPercentRect(view: View): String {
        val location = IntArray(2).apply { view.getLocationOnScreen(this) }
        val l = (location[0].toFloat() / screenWidth * 100).format(3)
        val t = (location[1].toFloat() / screenHeight * 100).format(3)
        val r = ((location[0] + view.width).toFloat() / screenWidth * 100).format(3)
        val b = ((location[1] + view.height).toFloat() / screenHeight * 100).format(3)
        return "$l,$t,$r,$b"
    }

    private fun isTouchInView(view: View, x: Float, y: Float): Boolean {
        if (!view.isShown) return false // 忽略不可见 View

        val location = IntArray(2)
        view.getLocationOnScreen(location)

        val left = location[0].toFloat()
        val top = location[1].toFloat()
        val right = (location[0] + view.width).toFloat()
        val bottom = (location[1] + view.height).toFloat()

        return x in left..right && y >= top && y <= bottom
    }

    private fun calculateSwipeDirection(deltaX: Float, deltaY: Float): SwipeDirection {
        return when {
            abs(deltaY) > abs(deltaX) -> {
                if (deltaY < 0) SwipeDirection.UP else SwipeDirection.DOWN
            }

            else -> {
                if (deltaX < 0) SwipeDirection.LEFT else SwipeDirection.RIGHT
            }
        }
    }

    private fun findHitView(x: Float, y: Float): View? {
        return dynamicViews.find { view ->
            isTouchInView(view, x, y)
        }
    }

    private fun updateTextView() {
        val displayText = currentGestureSteps.joinToString(" | ") {
            "${it.action},${it.x},${it.y}(${it.pressure})"
        }
        notifyTapText(displayText)

        val text = when (direction) {
            SwipeDirection.UP -> "向上滑动"
            SwipeDirection.DOWN -> "向下滑动"
            SwipeDirection.LEFT -> "向左滑动"
            SwipeDirection.RIGHT -> "向右滑动"
            SwipeDirection.NONE -> " "
        }
        notifyMaskText(text)
    }


    data class GestureStep(
        val action: String,
        val pointerId: Int,
        val x: Float,
        val y: Float,
        val pressure: Float,
        var timestamp: Long = System.currentTimeMillis()
    )

    data class CompleteGesture(
        val startAction: String,
        val pointerId: Int,
        val steps: MutableList<GestureStep> = mutableListOf(),
        var isEnded: Boolean = false,
        var downView: View? = null,
        var isSwipe: Boolean = false,
        var isLongPress: Boolean = false,

        var allInSameView: Boolean = false,   // 全程是否都在初始 View 内
        var movedOutOfView: Boolean = false   // 是否中途滑出 View
    )

    enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT, NONE
    }

    private fun Float.format(decimals: Int): String {
        val factor = 10.0.pow(decimals.toDouble()).toInt()
        return (this * factor).toInt().div(factor.toDouble()).toString()
    }

    private fun saveOriginalBackground(view: View) {
        val background = view.backgroundTintList
        view.setTag(R.id.tag_original_background, background)
    }

    private fun updateHitViewBackground(view: View) {
        if (view.background == null) {
            view.setBackgroundColor(Color.WHITE)
        }
        val color = "#88FF0000".toColorInt()
        view.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun restoreHitViewBackground(view: View) {
        view.backgroundTintList = view.getTag(R.id.tag_original_background) as ColorStateList?
    }

    fun setAdjacentHitConfig(enabled: Boolean, range: Float = 20f) {
        allowAdjacentHit = enabled
        adjacentHitRange = range
    }

    protected abstract fun initDynamicViews(dynamicViews: MutableList<View>)
    protected abstract fun initView()
    protected abstract fun layoutId(): Int
    protected abstract fun notifyTapText(text: String)
    protected abstract fun notifyMaskText(text: String)
    protected abstract fun suffixName(): String


    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}