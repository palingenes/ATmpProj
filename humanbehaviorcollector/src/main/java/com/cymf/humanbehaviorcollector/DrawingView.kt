package com.cymf.humanbehaviorcollector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    data class DrawingStroke(val path: Path, val paint: Paint)

    private val strokes = mutableMapOf<Int, DrawingStroke>()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // 禁用硬件加速以支持抗锯齿
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        strokes.forEach { (_, stroke) ->
            canvas.drawPath(stroke.path, stroke.paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                handlePointerDown(event)
            }

            MotionEvent.ACTION_MOVE -> {
                handleMove(event)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                handlePointerUp(event)
            }

            MotionEvent.ACTION_CANCEL -> {
                strokes.clear()
                invalidate()
            }
        }
        return true
    }

    private fun handlePointerDown(event: MotionEvent) {
        for (i in 0 until event.pointerCount) {
            if (!strokes.containsKey(event.getPointerId(i))) {
                val newPath = Path()
                newPath.moveTo(event.getX(i), event.getY(i))
                val newPaint = Paint().apply {
                    color = getRandomColor() // 使用随机颜色区分不同手指
                    strokeWidth = 5f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                strokes[event.getPointerId(i)] = DrawingStroke(newPath, newPaint)
                invalidate()
            }
        }
    }

    private fun handleMove(event: MotionEvent) {
        for (i in 0 until event.pointerCount) {
            val id = event.getPointerId(i)
            val path = strokes[id]?.path
            if (path != null) {
                if (event.historySize > 0) {
                    for (h in 0 until event.historySize) {
                        val historicalX = event.getHistoricalX(i, h)
                        val historicalY = event.getHistoricalY(i, h)
                        path.lineTo(historicalX, historicalY)
                    }
                }
                path.lineTo(event.getX(i), event.getY(i))
            }
        }
        invalidate()
    }

    private fun handlePointerUp(event: MotionEvent) {
        for (i in 0 until event.pointerCount) {
            if (event.actionIndex == i) {
                strokes.remove(event.getPointerId(i))
                invalidate()
            }
        }
    }

    private fun getRandomColor(): Int {
        val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA
        )
        return colors.random()
    }
}