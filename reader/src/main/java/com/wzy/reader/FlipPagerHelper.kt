package com.wzy.reader

import android.app.Activity
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


object FlipPagerHelper {
    fun attachToRecyclerView(rv: RecyclerView, activity: Activity) {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        rv.layoutManager = layoutManager

        // ✅ 关键1：增大 View 缓存池（避免频繁 create/bind）
        rv.setItemViewCacheSize(3) // 缓存当前页 + 左右各1页
        // ✅ 关键2：预取相邻 Item（API 21+）
        layoutManager.initialPrefetchItemCount = 2 // 滚动前预加载2个

        // 添加仿真层
        val simulationView = SimulationView(activity)
        (rv.parent as ViewGroup).addView(
            simulationView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                simulationView.onScrolled(recyclerView, dx)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    simulationView.onScrollIdle()
                }
            }
        })
    }
}