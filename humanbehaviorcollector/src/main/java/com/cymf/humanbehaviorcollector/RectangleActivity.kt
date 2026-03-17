package com.cymf.humanbehaviorcollector

import android.view.View
import android.widget.TextView

/**
 * 页面2： 矩形区域点击模拟（记录宽高比例）1个
 */
class RectangleActivity : AbsTapActivity() {

    private lateinit var textView: TextView
    private lateinit var tvMask: TextView

    override fun initDynamicViews(dynamicViews: MutableList<View>) {
        dynamicViews.add(findViewById(R.id.view))
    }

    override fun initView() {
        textView = findViewById(R.id.textView)
        tvMask = findViewById(R.id.tv_mask)
    }

    override fun notifyTapText(text: String) {
        textView.text = text
    }

    override fun notifyMaskText(text: String) {
        tvMask.text = text
    }

    override fun layoutId() = R.layout.activity_rectangle

    override fun suffixName() = "Rectangle"
}