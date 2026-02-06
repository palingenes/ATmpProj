package com.wzy.reader

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReaderActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PageAdapter
    private lateinit var layoutPx: TextLayoutPx


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        recyclerView = findViewById(R.id.recyclerView)

        // ✅ 全局唯一配置
        val config = TextLayoutConfig(
            fontSizeSp = 18f,
            lineSpacingExtraDp = 4f,
            lineSpacingMultiplier = 1.2f,
            paddingLeftDp = 32,
            paddingTopDp = 32,
            paddingRightDp = 32,
            paddingBottomDp = 32
        )
        layoutPx = TextLayoutConfig.createWithPx(this, config)


        // 加载 TXT 并构建分页
        val text = assets.open("test.txt").bufferedReader().readText()
        val pages = TextPage.buildPagesFromText(
            text = text,
            context = this,
            config = config
        )
        Log.d("PAGINATION", "${pages[0].content}\n---")
        Log.d("PAGINATION", "${pages[1].content}\n---")

        // 设置 Adapter
        adapter = PageAdapter(pages, layoutPx)
        recyclerView.adapter = adapter

        // 启用 Scroll 模式（含章节吸附）
        setScrollMode()
    }

    private fun setSlideMode() {
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        SlidePagerHelper.attachToRecyclerView(recyclerView)
    }

    private fun setScrollMode() {
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.isNestedScrollingEnabled = true

        recyclerView.onFlingListener = null
        ScrollModeController(recyclerView, adapter).attach()
    }

    private fun setFlipMode() {
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        FlipPagerHelper.attachToRecyclerView(recyclerView, this)
    }

    // 可通过按钮切换模式
    fun onModeChanged(mode: String) {
        when (mode) {
            "slide" -> setSlideMode()
            "scroll" -> setScrollMode()
            "flip" -> setFlipMode()
        }
    }
}
