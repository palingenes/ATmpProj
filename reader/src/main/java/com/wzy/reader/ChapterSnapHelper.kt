package com.wzy.reader

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

class ChapterSnapHelper : SnapHelper() {

    private var mRecyclerView: RecyclerView? = null

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        mRecyclerView = recyclerView
        super.attachToRecyclerView(recyclerView)
    }

    // 【核心】只返回需要吸附的 View（章首/章末）
    override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
        val lm = layoutManager as LinearLayoutManager
        val firstVisible = lm.findFirstCompletelyVisibleItemPosition() // ✅ 完全可见
        val lastVisible = lm.findLastCompletelyVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return null
        }

        val adapter = mRecyclerView?.adapter as? PageAdapter ?: return null

        // 🟢 情况1：用户正在从 A 章最后一页滑向 B 章
        // 条件：A 章最后一页已完全不可见，B 章第一页部分可见
        if (lastVisible + 1 < adapter.itemCount) {
            val nextPage = adapter.pages[lastVisible + 1]
            if (nextPage.isFirstPage) {
                // 用户已经滑过 A 章最后一页 → 吸附到 B 章第一页
                return lm.findViewByPosition(lastVisible + 1)
            }
        }

        // 🟢 情况2：用户正在从 B 章第一页滑回 A 章
        if (firstVisible - 1 >= 0) {
            val prevPage = adapter.pages[firstVisible - 1]
            if (prevPage.isLastPage) {
                // 用户已经滑过 B 章第一页 → 吸附到 A 章最后一页
                return lm.findViewByPosition(firstVisible - 1)
            }
        }

        return null // 其他情况：自由滚动
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray {
        if (mRecyclerView == null) return intArrayOf(0, 0)
        // 吸附到顶部（ViewPager 效果）
        return intArrayOf(0, targetView.top)
    }

    // 【重要】处理 Fling
    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager?,
        velocityX: Int,
        velocityY: Int
    ): Int {
        // 复用 findSnapView 逻辑，但需预测方向
        val snapView = findSnapView(layoutManager)
        if (snapView != null) {
            return (layoutManager as LinearLayoutManager).getPosition(snapView)
        }
        // 非边界 fling：交给 RecyclerView 默认处理（自由滚动）
        return RecyclerView.NO_POSITION
    }
}