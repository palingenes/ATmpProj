package com.wzy.reader

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Slide 模式（横向覆盖）
 */
object SlidePagerHelper {
    fun attachToRecyclerView(rv: RecyclerView) {
        rv.setChildDrawingOrderCallback { childCount, i ->
            val first = (rv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val last = (rv.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return@setChildDrawingOrderCallback i

            val center = (first + last) / 2
            val targetPos = if (rv.scrollState == RecyclerView.SCROLL_STATE_IDLE) center else first

            val diff = i - targetPos
            if (diff == 0) i
            else if (diff > 0) i - 1
            else i + 1
        }

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val first = lm.findFirstVisibleItemPosition()
                val view = lm.findViewByPosition(first)
                view?.translationX = if (dx > 0) -dx.toFloat() else -dx.toFloat()
            }
        })
    }
}