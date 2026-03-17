package com.cymf.humanbehaviorcollector

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


/**
 * 人类行为采集器
 *
 * 页面1：上下左右滑动的记录
 * 页面2、矩形区域点击模拟（记录宽高比例）1个
 * 页面3、banner多个区域点击模拟（记录宽高比例） 4个
 * 页面4、小按钮（关闭按钮）的点击（记录宽高比例，允许误点） 1个
 * 页面5、多个位置正常比例按钮点击。4行，每行2个
 *
 * --------------------------
 *
 * 规范格式：记录为json文件，文件以屏幕宽高和最小滑动距离命名，每个页面分别存储为一个文件
 * page{number}_720_1080_18.txt
 * [{
 *     "viewRect":"l%,t%,r%,b%", // 目标view边界，如果是滑动则rect：0.00,100.000,0.000,100.000
 *     "downTime":2000,    //  从手指按下到抬起的时间
 *     "touch":"x%,y%|……|x%,y%"// 触摸轨迹：x、y均是按照比例记录，以|分割,单位精确到小数点后三位
 *     "action":"slide_top"    //  根据行为总结行为类型：enum{click,longClick，slide（上下左右）}
 * }]
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_slide).setOnClickListener(this)
        findViewById<Button>(R.id.btn_rect).setOnClickListener(this)
        findViewById<Button>(R.id.btn_banner).setOnClickListener(this)
        findViewById<Button>(R.id.btn_small).setOnClickListener(this)
        findViewById<Button>(R.id.btn_multi).setOnClickListener(this)
        findViewById<Button>(R.id.btn_web).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val currentClickTime = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime < 800) {
            return
        }
        when (v?.id) {
            R.id.btn_slide -> start(SlideActivity::class.java)
            R.id.btn_rect -> start(RectangleActivity::class.java)
            R.id.btn_banner -> start(BannerActivity::class.java)
            R.id.btn_small -> start(SmallActivity::class.java)
            R.id.btn_multi -> start(MultiActivity::class.java)
            R.id.btn_web -> start(WebActivity::class.java)
        }
    }

    private fun start(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }
}
