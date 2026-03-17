package com.cymf.humanbehaviorcollector

import android.view.View
import android.widget.TextView

/**
 * 页面1： 上下左右滑动的记录
 */
class SlideActivity : AbsTapActivity() {

    private lateinit var textView: TextView
    private lateinit var tvMask: TextView

    override fun initDynamicViews(dynamicViews: MutableList<View>) = Unit

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

    override fun layoutId() = R.layout.activity_slide

    override fun suffixName() = "Slide"
}