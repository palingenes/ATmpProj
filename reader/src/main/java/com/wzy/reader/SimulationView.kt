package com.wzy.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SimulationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null
    private var scrollOffset = 0f
    private var isForward = true

    fun onScrolled(rv: RecyclerView, dx: Int) {
        scrollOffset += dx
        isForward = dx > 0
        invalidate()
    }

    fun onScrollIdle() {
        scrollOffset = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scrollOffset == 0f) return

        // 此处应生成当前页和下一页截图（略）
        // 实际需从 RecyclerView 获取 View 并 draw 到 Bitmap

        canvas.drawColor(Color.GRAY) // 模拟背景
        // TODO: 绘制弯曲纸张（贝塞尔曲线 + clipPath）
    }
}