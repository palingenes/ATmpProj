package com.cymf.humanbehaviorcollector

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

/**
 * 点击相关行为收集
 */
class TapActivity : AppCompatActivity() {

    private lateinit var relativeLayout: RelativeLayout
    private lateinit var textView: TextView
    private lateinit var tvMask: TextView
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
//        tvMask.text = "长按"
    }

    private var currentGesture: MutableList<String>? = null
    private var direction: SwipeDirection = SwipeDirection.NONE
    private var isEnd = false
    private var gestureStartAction: Int = -1

    private val random = Random.Default
    private var screenWidth = 0
    private var screenHeight = 0
    private val dynamicViews = mutableListOf<View>() // 保存所有动态添加的 View 及其边界信息
    private var hitView: View? = null   // 查找是否点击/滑动在某个动态 View 上

    private var clazzType: Int = -1


    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tap)

        intent?.let {
            clazzType = it.getIntExtra(TAG_INT, -1)
        }
        if (clazzType < 0) {
            ToastUtils.showShort("出现边界！")
            finish()
            return
        }
        relativeLayout = findViewById(R.id.relative_layout)
        textView = findViewById(R.id.textView)
        tvMask = findViewById(R.id.tv_mask)
        drawingView = findViewById(R.id.drawingView)


        when (clazzType) {
            0 -> textView.text = "点击随机出现的按钮"
            1 -> textView.text = "向左滑动"
            2 -> textView.text = "向右滑动"
            3 -> textView.text = "向上滑动"
            4 -> textView.text = "向下滑动"
        }
        textView.append("\n系统会自动记录每次行为")

        drawingView.setOnTouchListener { _, event ->
            handleTouchEvent(event)
            drawingView.onTouchEvent(event)
            true
        }

        when (clazzType) {
            0 -> addRandomClickView()
            1 -> tvMask.text = "← ← ←"
            2 -> tvMask.text = "→ → →"
            3 -> tvMask.text = "↑\n↑\n↑"
            4 -> tvMask.text = "↓\n↓\n↓"
        }
        tvMask.visibility = View.VISIBLE

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                //  其实没啥用，设置true后就拦截back键，false不触发回调
                ToastUtils.showShort("handleOnBackPressed")
            }
        })
    }

    /**
     * 添加随机位置的view（以供点击）
     */
    fun addRandomClickView() {
        getScreenSize().let {
            screenWidth = it.x
            screenHeight = it.y
        }

        // 随机选择添加 Button 或 ImageView
        val randomView = if (random.nextBoolean()) {
            createButton()
        } else {
            createImageView()
        }
        addRandomView(randomView)
        // 添加到列表中以便后续判断
        dynamicViews.add(randomView)
    }

    private fun addRandomView(createView: View) {
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        val position = random.nextInt(4) // 0: 左上, 1: 右上, 2: 左下, 3: 右下
        when (position) {
            0 -> {
                params.addRule(RelativeLayout.ALIGN_PARENT_START)
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }

            1 -> {
                params.addRule(RelativeLayout.ALIGN_PARENT_END)
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }

            2 -> {
                params.addRule(RelativeLayout.ALIGN_PARENT_START)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }

            3 -> {
                params.addRule(RelativeLayout.ALIGN_PARENT_END)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }
        }
        relativeLayout.addView(createView)
        createView.post {
            adjustLayoutParams(createView, params, position)
        }
    }

    private fun adjustLayoutParams(view: View, params: RelativeLayout.LayoutParams, position: Int) {
        val offsetX = random.nextInt(view.width)
        val offsetY = random.nextInt(view.height)

        var adjustedOffsetX = offsetX
        var adjustedOffsetY = offsetY

        // 根据位置调整偏移方向
        if (position == 1 || position == 3) adjustedOffsetX = -offsetX
        if (position == 2 || position == 3) adjustedOffsetY = -offsetY

        // 保证不越界
        ensureWithinBounds(params, view, adjustedOffsetX, adjustedOffsetY)

        params.leftMargin += adjustedOffsetX
        params.topMargin += adjustedOffsetY
        view.layoutParams = params
    }

    private fun ensureWithinBounds(
        params: RelativeLayout.LayoutParams,
        view: View,
        initialOffsetX: Int,
        initialOffsetY: Int
    ) {
        var adjustedOffsetX = initialOffsetX
        var adjustedOffsetY = initialOffsetY

        if (params.getRule(RelativeLayout.ALIGN_PARENT_START) != 0) {
            if (params.leftMargin + adjustedOffsetX < 0) adjustedOffsetX = 0
            if (params.leftMargin + view.width + adjustedOffsetX > screenWidth) {
                adjustedOffsetX = screenWidth - view.width - params.leftMargin
            }
        }
        if (params.getRule(RelativeLayout.ALIGN_PARENT_END) != 0) {
            if (params.leftMargin - adjustedOffsetX < 0) adjustedOffsetX = 0
            if (params.leftMargin - view.width + adjustedOffsetX > screenWidth) {
                adjustedOffsetX = screenWidth - view.width - params.leftMargin
            }
        }
        if (params.getRule(RelativeLayout.ALIGN_PARENT_TOP) != 0) {
            if (params.topMargin + adjustedOffsetY < 0) adjustedOffsetY = 0
            if (params.topMargin + view.height + adjustedOffsetY > screenHeight) {
                adjustedOffsetY = screenHeight - view.height - params.topMargin
            }
        }
        if (params.getRule(RelativeLayout.ALIGN_PARENT_BOTTOM) != 0) {
            if (params.topMargin - adjustedOffsetY < 0) adjustedOffsetY = 0
            if (params.topMargin - view.height + adjustedOffsetY > screenHeight) {
                adjustedOffsetY = screenHeight - view.height - params.topMargin
            }
        }
    }

    private fun createButton(): View {
        return Button(this).apply {
            text = "关闭按钮"
            isClickable = false
        }
    }

    private fun createImageView(): View {
        val src = arrayOf(R.mipmap.ic_next, R.mipmap.ic_close)
        return ImageView(this).apply {
            setImageResource(src[random.nextInt(src.size - 1)])
            setBackgroundResource(R.drawable.bg_img)
            layoutParams = RelativeLayout.LayoutParams(dpToPx(80), dpToPx(50))
            isClickable = false
        }
    }


    private val startPositions = SparseArray<PointF>() // 保存每个 pointerId 的起始坐标 (x, y)
    private val movePoints = SparseArray<MutableList<PointF>>() // 记录每个触点的所有移动点

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
                    val pointerId = event.getPointerId(i)
                    val startPoint = startPositions[pointerId]
                    if (startPoint != null) {
                        direction = SwipeDirection.NONE
                        startPositions.remove(pointerId) // 移除已处理的手指数据
                    }
                    if (actionMasked == MotionEvent.ACTION_POINTER_UP && i == actionIndex ||
                        actionMasked == MotionEvent.ACTION_UP ||
                        actionMasked == MotionEvent.ACTION_CANCEL
                    ) {
                        endCurrentGesture(event, i, actionMasked)
                    }
                }
            }
        }
        // 更新 TextView 显示最新的手势记录
        updateTextView()
    }

    private fun startNewGesture(event: MotionEvent, pointerIndex: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        startPositions.put(pointerId, PointF(x, y))
        movePoints.put(pointerId, mutableListOf(PointF(x, y))) // 初始化轨迹列表

        resetStateAndDownTime()//   记录按下的时间和重置状态
        val actionStr =
            if (event.actionMasked == MotionEvent.ACTION_DOWN) "ACTION_DOWN" else "ACTION_POINTER_DOWN"
        currentGesture = mutableListOf("$actionStr,${x.toInt()},${y.toInt()}")
        gestureStartAction = event.actionMasked

        handler.postDelayed(longPressRunnable, 500) // 500ms 为长按判定时间
    }

    private fun recordMove(event: MotionEvent, pointerIndex: Int) {
        val currentX = event.getX(pointerIndex)
        val currentY = event.getY(pointerIndex)
        currentGesture?.add("${currentX.toInt()},${currentY.toInt()}")

        val pointerId = event.getPointerId(pointerIndex)
        val startPoint = startPositions[pointerId]
        val deltaX = currentX - startPoint.x
        val deltaY = currentY - startPoint.y
        direction = calculateSwipeDirection(deltaX, deltaY)

        // 更新累计位移
        totalDeltaX += abs(deltaX)
        totalDeltaY += abs(deltaY)

        // 如果位移超过系统默认滑动阈值，则标记为“已滑动”
        if (totalDeltaX > touchSlop || totalDeltaY > touchSlop) {
            hasMoved = true
            handler.removeCallbacks(longPressRunnable) // 移除长按检测
        }

        startPoint?.set(currentX, currentY)
        movePoints[pointerId]?.add(PointF(currentX, currentY)) // 添加到轨迹中
    }


    private fun endCurrentGesture(event: MotionEvent, pointerIndex: Int, actionMasked: Int) {
        currentGesture?.let { gestureList ->
            val x = event.getX(pointerIndex).toInt()
            val y = event.getY(pointerIndex).toInt()

            val endActionStr = when (actionMasked) {
                MotionEvent.ACTION_UP -> "ACTION_UP"
                MotionEvent.ACTION_POINTER_UP -> "ACTION_POINTER_UP"
                else -> "ACTION_CANCEL"
            }

            gestureList.add("$endActionStr,$x,$y")

            // 拼接成最终格式
            val line = gestureList.joinToString(" | ")

            // 更新 TextView 显示这一行完整记录
            textView.text = line
            // 写入本地文件
            appendToFile(line)
            // 清空当前记录
            currentGesture = null


            // 获取该触点的完整移动轨迹
            val pointerId = event.getPointerId(pointerIndex)
            val path = movePoints[pointerId]

            if (!path.isNullOrEmpty()) {
                val startX = path[0].x
                val endX = path[path.size - 1].x
                val totalDeltaX = abs(endX - startX)
                val totalDeltaY = abs(path[path.size - 1].y - path[0].y)

                isSwipe = totalDeltaX > touchSlop || totalDeltaY > touchSlop

                // 使用累计位移判断最终方向
                direction = calculateSwipeDirection(
                    path[path.size - 1].x - path[0].x,
                    path[path.size - 1].y - path[0].y
                )
                isEnd = true
            }
            // 获取最终触点坐标
            val finalX = event.rawX
            val finalY = event.rawY
            for (view in dynamicViews) {
                if (isTouchInView(view, finalX, finalY)) {
                    hitView = view
                    break
                }
            }
            movePoints.remove(pointerId) // 清除轨迹数据
        }

        handler.removeCallbacks(longPressRunnable)
    }

    enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT, NONE
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

    @SuppressLint("SetTextI18n")
    private fun updateTextView() {
        // 如果有正在进行的手势，则显示它；否则显示最新完成的手势
        val currentText = if (currentGesture != null) {
            currentGesture!!.joinToString(" | ")
        } else {
            textView.text.toString()
        }
        textView.text = currentText

        val string = when (direction) {
            SwipeDirection.UP -> "向上滑动"
            SwipeDirection.DOWN -> "向下滑动"
            SwipeDirection.LEFT -> "向左滑动"
            SwipeDirection.RIGHT -> "向右滑动"
            SwipeDirection.NONE -> " "
        }
        tvMask.text = if (isEnd) {
            isEnd = false
            val text = when {
                isLongPress -> {
                    if (hitView != null) {
                        "长按在控件上"
                    } else {
                        "长按在空白处"
                    }
                }

                isSwipe -> {
                    if (hitView != null) {
                        "从控件上滑动"
                    } else {
                        "$string(从空白处滑动)"
                    }
                }

                else -> if (hitView != null) {
                    removeView(hitView!!) // 点击后移除控件
                    "点击了控件"
                } else {
                    "点击了空白区域"
                }
            }
            "最终方向： $text"
        } else string
    }


    private suspend fun appendToFileSuspend(line: String) {
        withContext(Dispatchers.IO) {
            try {
                val suffix = when (clazzType) {
                    0 -> "click"
                    1 -> "left"
                    2 -> "right"
                    3 -> "top"
                    4 -> "bottom"
                    else -> "unknown"
                }
                val fileName = "gesture_records_${suffix}.txt"
                val filePath = File(filesDir, fileName)

                if (!filePath.exists()) {
                    filePath.parentFile?.mkdirs() // 确保父目录存在
                    filePath.createNewFile()
                }
                val fos = openFileOutput(fileName, MODE_PRIVATE or MODE_APPEND)
                fos.write(line.toByteArray())
                fos.write("\n".toByteArray())
                fos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun appendToFile(line: String) {
        lifecycleScope.launch {
            appendToFileSuspend(line)
        }
    }

    private fun resetStateAndDownTime() {
        downTime = System.currentTimeMillis()// 记录按下时间

        // 重置状态
        isLongPress = false
        isSwipe = false
        hasMoved = false
        totalDeltaX = 0f
        totalDeltaY = 0f
    }

    /**
     * 检测坐标是否在某个 View 的显示区域内
     */
    private fun isTouchInView(view: View, x: Float, y: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        return x >= left && x <= right && y >= top && y <= bottom
    }

    /**
     * 移除指定 View 并延迟添加新的控件
     */
    private fun removeView(view: View) {
        runOnUiThread {
            handler.postDelayed({
                addRandomClickView()
                ToastUtils.showShort("已创建新的随机控件！")
            }, 3000)
            relativeLayout.removeView(view)
            dynamicViews.remove(view)
            ToastUtils.showShort("已移除当前控件")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ToastUtils.showShort("KEYCODE_BACK")
        }
        return super.onKeyDown(keyCode, event)
    }

    // dp 转 px
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    @Suppress("DEPRECATION")
    private fun getScreenSize(): Point {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val size = Point()
            windowManager.defaultDisplay.getSize(size)
            size
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}