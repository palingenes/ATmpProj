package com.cymf.humanbehaviorcollector

import android.view.View
import android.widget.TextView

/**
 * 页面4、小按钮（关闭按钮）的点击（记录宽高比例，允许误点） 1个
 */
class SmallActivity : AbsTapActivity() {

    private lateinit var textView: TextView
    private lateinit var tvMask: TextView

    override fun initDynamicViews(dynamicViews: MutableList<View>) {
        dynamicViews.add(findViewById(R.id.iv_next))
        dynamicViews.add(findViewById(R.id.iv_next_2))
        dynamicViews.add(findViewById(R.id.iv_close))
        dynamicViews.add(findViewById(R.id.iv_close_2))
    }

    override fun initView() {
        textView = findViewById(R.id.textView)
        tvMask = findViewById(R.id.tv_mask)
        setAdjacentHitConfig(true, dpToPx(20).toFloat())
    }

    override fun notifyTapText(text: String) {
        textView.text = text
    }

    override fun notifyMaskText(text: String) {
        tvMask.text = text
    }

    override fun layoutId() = R.layout.activity_small

    override fun suffixName() = "Small"
}