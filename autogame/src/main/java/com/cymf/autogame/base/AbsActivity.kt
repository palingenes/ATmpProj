package com.cymf.autogame.base

import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.viewbinding.ViewBinding
import com.cymf.autogame.R
import com.cymf.autogame.ktx.clickWithLimit

/**
 * 是否在
 */
abstract class AbsActivity<VB : ViewBinding> : BaseActivity<VB>() {

    private var cardView: CardView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTitleBarView()
    }

    private fun initTitleBarView() {
        cardView = find(R.id.card_title_container)
        if (cardView == null) return
        setStatusBarHeight(R.id.view_placeholder, 10)
        find<View>(R.id.ic_back).clickWithLimit { finish() }
    }

    fun setTitleText(titleText: CharSequence?) {
        setText(R.id.tv_title_bar_text, titleText)
    }

//    fun setTitleRightImg(listener: View.OnClickListener?) {
//        if (cardView == null) return
//        find<View>(R.id.iv_right).visibility = View.VISIBLE
//        find<View>(R.id.iv_right).setOnClickListener(listener)
//    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_out)
    }
}
