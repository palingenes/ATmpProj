package com.cymf.autogame.ktx

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.cymf.autogame.App
import kotlin.random.Random

fun res2Color(colorId: Int): Int {
    return App.app.resources.getColor(colorId, App.app.theme)
}

fun inflate(viewId: Int, root: ViewGroup?): View? {
    return LayoutInflater.from(App.app).inflate(viewId, root, false)
}

fun res2Dimen(@DimenRes dimenId: Int): Int {
    return App.app.resources.getDimensionPixelSize(dimenId)
}

fun res2Drawable(@DrawableRes drawId: Int): Drawable? {
    return ResourcesCompat.getDrawable(App.context.resources, drawId, App.app.theme)
}

fun stringOf(@StringRes id: Int, vararg formatArgs: Any): String = getString(id, *formatArgs)

fun stringOf(@StringRes id: Int): String = getString(id)

fun getString(@StringRes id: Int, vararg formatArgs: Any?): String {
    return App.app.resources.getString(id, *formatArgs)
}

fun <T> List<T?>.randomNonNullElement(): T? {
    if (isEmpty()) return null
    val maxRetries = size / 2
    (0..maxRetries).forEach { i ->
        val index = Random.nextInt(size)
        val element = get(index)
        if (element != null) {
            return element
        }
    }
    return null
}

/**
 * 生成指定范围的随机 Double，并保留两位小数
 */
fun randomFloatInRange(min: Double = 0.0, max: Double = 100.0): Float {
    require(min < max) { "max 必须大于 min" }
    val value = Random.nextDouble(min, max)
    return "%.2f".format(value).toFloat()
}

// Int 范围随机数
fun ClosedRange<Int>.random(): Int {
    return Random.nextInt(endInclusive - start + 1) + start
}

// Float 范围随机数（保留两位小数）
fun ClosedRange<Float>.random(): Float {
    val range = endInclusive - start
    val randomValue = start + Random.nextFloat() * range
    return (randomValue * 100).toInt() / 100f // 四舍五入保留两位小数
}

// Double 范围随机数（保留两位小数）
@SuppressLint("DefaultLocale")
fun ClosedRange<Double>.random(): Double {
    val randomValue = Random.nextDouble(start, endInclusive)
    return String.format("%.2f", randomValue).toDouble() // 保留两位小数
}

fun String.containsAny(keywords: List<String>): Boolean {
    return keywords.any { this.contains(it, ignoreCase = true) }
}