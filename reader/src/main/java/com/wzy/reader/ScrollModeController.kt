package com.wzy.reader

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScrollModeController(
    private val recyclerView: RecyclerView,
    private val adapter: PageAdapter
) {

    private var isFlinging = false
    private var isBlocking = false

    fun attach() {
        ChapterSnapHelper().attachToRecyclerView(recyclerView)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy == 0) return

                val lm = recyclerView.layoutManager as LinearLayoutManager
                val first = lm.findFirstVisibleItemPosition()
                val last = lm.findLastVisibleItemPosition()
                val adapter = recyclerView.adapter as PageAdapter

                // 向下滑（内容向上）：检查是否到达章末
                if (dy > 0 && last != RecyclerView.NO_POSITION) {
                    if (adapter.pages[last].isLastPage) {
                        // 如果继续滑，会进入下一章 → 阻断
                        if (last + 1 < adapter.itemCount && !adapter.pages[last + 1].isFirstPage.not()) {
                            // 实际判断：下一页是否是新章开头
                            if (adapter.pages[last + 1].isFirstPage) {
                                // 阻断 fling
                                recyclerView.fling(0, 0)
                                return
                            }
                        }
                    }
                }

                // 向上滑（内容向下）：检查是否到达章首
                if (dy < 0 && first != RecyclerView.NO_POSITION) {
                    if (adapter.pages[first].isFirstPage) {
                        if (first - 1 >= 0 && adapter.pages[first - 1].isLastPage) {
                            recyclerView.fling(0, 0)
                            return
                        }
                    }
                }
            }
        })
    }
}