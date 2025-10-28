package com.cymf.keyshot.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View


class MaskView(context: Context): View(context) {

    private val rect by lazy { Rect() }
    private val path by lazy { mutableListOf<PointF>() }
    private var text: String = ""
    private var type = TYPE_NORMAL

    private val paint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
    }

    fun setRect(l: Int, t: Int, r: Int, b: Int, type: Int) {
        this.rect.set(l, t, r, b)
        this.type = type
        postInvalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                path.clear()
                path.add(PointF(event.x, event.y))
            }
            MotionEvent.ACTION_MOVE -> {
                path.add(PointF(event.x, event.y))
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                path.add(PointF(event.x, event.y))
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    fun setText(text: String) {
        this.text = text
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when(type) {
            TYPE_NORMAL -> {
                paint.color = Color.BLUE
            }
            TYPE_POSITIVE -> {
                paint.color = Color.GREEN
            }
            TYPE_NEGATIVE -> {
                paint.color = Color.CYAN
            }
            TYPE_AREA -> {
                paint.color = Color.YELLOW
            }
            TYPE_ERROR -> {
                paint.color = Color.RED
            }
            TYPE_CLICK_AREA -> {
                paint.color = clickColor
            }
        }
        if (rect.width() < 10 && rect.height() < 10) {
            // 向外拓展10像素
            rect.inset(-10, -10)
        }
        paint.style = Paint.Style.STROKE
        canvas.drawRect(rect, paint)

        for (i in 0 until path.size - 1) {
            canvas.drawLine(path[i].x, path[i].y, path[i + 1].x, path[i + 1].y, paint)
        }

        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setLineSpacing(0.0f, 1.0f)
            .setIncludePad(false)
            .build().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    this.computeDrawingBoundingBox().apply {
                        paint.style = Paint.Style.FILL
                        paint.color = consoleBackgroundColor
                        canvas.drawRect(left, top, right, bottom, paint)
                    }
                }
                paint.textSize = 30f
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.CENTER
                draw(canvas)
            }
    }

    companion object {
        val clickColor = Color.parseColor("#9400D3")
        val consoleBackgroundColor = Color.parseColor("#55000000")
        const val TYPE_NORMAL = 0
        const val TYPE_POSITIVE = 1
        const val TYPE_NEGATIVE = 2
        const val TYPE_AREA = 3
        const val TYPE_ERROR = 4
        const val TYPE_CLICK_AREA = 5
    }
}