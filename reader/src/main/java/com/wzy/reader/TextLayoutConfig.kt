package com.wzy.reader

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.text.TextPaint
import android.view.Gravity
import android.widget.TextView

data class TextLayoutConfig(
    val fontSizeSp: Float = 18f,
    val lineSpacingExtraDp: Float = 4f,
    val lineSpacingMultiplier: Float = 1.2f,
    val paddingLeftDp: Int = 32,
    val paddingTopDp: Int = 32,
    val paddingRightDp: Int = 32,
    val paddingBottomDp: Int = 32,
    val typeface: Typeface = Typeface.DEFAULT
) {
    companion object {
        // 默认配置（可全局替换）
        val DEFAULT = TextLayoutConfig()

        // 工厂方法：从 Context 构建带 px 的辅助对象
        fun createWithPx(context: Context, config: TextLayoutConfig = DEFAULT): TextLayoutPx {
            val dm = context.resources.displayMetrics
            return TextLayoutPx(
                config = config,
                fontSizePx = (config.fontSizeSp * dm.density).toInt(),
                paddingLeftPx = (config.paddingLeftDp * dm.density).toInt(),
                paddingTopPx = (config.paddingTopDp * dm.density).toInt(),
                paddingRightPx = (config.paddingRightDp * dm.density).toInt(),
                paddingBottomPx = (config.paddingBottomDp * dm.density).toInt(),
                lineSpacingExtraPx = config.lineSpacingExtraDp * dm.density
            )
        }
    }
}

// 辅助类：包含已转换的像素值（用于分页计算）
data class TextLayoutPx(
    val config: TextLayoutConfig,
    val fontSizePx: Int,
    val paddingLeftPx: Int,
    val paddingTopPx: Int,
    val paddingRightPx: Int,
    val paddingBottomPx: Int,
    val lineSpacingExtraPx: Float
) {
    val contentWidthPx: Int
        get() = Resources.getSystem().displayMetrics.widthPixels - paddingLeftPx - paddingRightPx

    fun createTextPaint(): TextPaint {
        return TextPaint().apply {
            textSize = fontSizePx.toFloat()
            typeface = config.typeface
            isAntiAlias = true
        }
    }

    fun applyToTextView(textView: TextView) {
        textView.apply {
            setPadding(paddingLeftPx, paddingTopPx, paddingRightPx, paddingBottomPx)
            textSize = config.fontSizeSp
            setLineSpacing(lineSpacingExtraPx, config.lineSpacingMultiplier)
            typeface = config.typeface
            gravity = Gravity.TOP or Gravity.START
            includeFontPadding = false
            movementMethod = null
        }
    }
}