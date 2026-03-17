package com.cymf.humanbehaviorcollector

import android.view.View
import android.widget.TextView

/**
 * 页面5、多个位置正常比例按钮点击。4行，每行2个
 */
class MultiActivity : AbsTapActivity() {

    private lateinit var textView: TextView
    private lateinit var tvMask: TextView

    override fun initDynamicViews(dynamicViews: MutableList<View>) {
        dynamicViews.add(findViewById(R.id.btn_1))
        dynamicViews.add(findViewById(R.id.btn_2))
        dynamicViews.add(findViewById(R.id.btn_3))
        dynamicViews.add(findViewById(R.id.btn_4))
        dynamicViews.add(findViewById(R.id.btn_5))
        dynamicViews.add(findViewById(R.id.btn_6))
        dynamicViews.add(findViewById(R.id.btn_7))
        dynamicViews.add(findViewById(R.id.btn_8))
    }

    override fun initView() {
        textView = findViewById(R.id.textView)
        tvMask = findViewById(R.id.tv_mask)

//        findViewById<Button>(R.id.btn_1).post {
//            val view: View = findViewById<Button>(R.id.btn_1)
//            val location = IntArray(2)
//            view.getLocationOnScreen(location)
//
//            val left = location[0]
//            val top = location[1]
//            val right = left + view.width
//            val bottom = top + view.height
//
//            val displayMetrics = resources.displayMetrics
//            val screenWidth = displayMetrics.widthPixels
//            val screenHeight = displayMetrics.heightPixels
//
//            val percentLeft = (left.toFloat() / screenWidth * 100).toInt()
//            val percentTop = (top.toFloat() / screenHeight * 100).toInt()
//            val percentRight = (right.toFloat() / screenWidth * 100).toInt()
//            val percentBottom = (bottom.toFloat() / screenHeight * 100).toInt()
//            LogUtils.e("$percentLeft,$percentTop,$percentRight,$percentBottom")
//        }
    }

    override fun notifyTapText(text: String) {
        textView.text = text
    }

    override fun notifyMaskText(text: String) {
        tvMask.text = text
    }

    override fun layoutId() = R.layout.activity_multi

    override fun suffixName() = "Multi"
}