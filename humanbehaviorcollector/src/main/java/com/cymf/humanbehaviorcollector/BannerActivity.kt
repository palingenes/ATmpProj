package com.cymf.humanbehaviorcollector

import android.view.View
import android.widget.TextView

/**
 * 页面3、banner多个区域点击模拟（记录宽高比例） 4个
 */
class BannerActivity : AbsTapActivity() {

    private lateinit var textView: TextView
    private lateinit var tvMask: TextView

    override fun initView() {
        textView = findViewById(R.id.textView)
        tvMask = findViewById(R.id.tv_mask)
    }

    override fun initDynamicViews(dynamicViews: MutableList<View>) {
        dynamicViews.add(findViewById(R.id.tv_banner1))
        dynamicViews.add(findViewById(R.id.tv_banner2))
        dynamicViews.add(findViewById(R.id.tv_banner3))
        dynamicViews.add(findViewById(R.id.tv_banner4))
    }
    override fun notifyTapText(text: String) {
        textView.text = text
    }

    override fun notifyMaskText(text: String) {
        tvMask.text = text
    }

    override fun layoutId() = R.layout.activity_banner

    override fun suffixName() = "Banner"

}