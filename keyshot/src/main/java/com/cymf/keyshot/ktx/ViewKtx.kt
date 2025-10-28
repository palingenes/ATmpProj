package com.cymf.keyshot.ktx

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.IntEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.StaticLayout
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import com.cymf.keyshot.App
import com.cymf.keyshot.utils.DisplayUtil
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Locale

fun View.clickWithLimit(intervalMill: Int = 500, block: ((v: View) -> Unit)) {
    setOnClickListener(object : View.OnClickListener {
        var last = 0L
        override fun onClick(v: View) {
            if (System.currentTimeMillis() - last > intervalMill) {
                block(v)
                last = System.currentTimeMillis()
            }
        }
    })
}

/***
 * 防止快速点击-并且添加按下变暗效果
 */
@SuppressLint("ClickableViewAccessibility")
fun View.click(minTime: Int = 500, listener: (view: View) -> Unit) {
    var lastTime = 0L
    this.setOnClickListener {
        val tmpTime = System.currentTimeMillis()
        if (tmpTime - lastTime > minTime) {
            lastTime = tmpTime
            listener.invoke(this)
        } else {
            Log.d(this::class.java.simpleName, "点击过快，取消触发")
        }
    }
    this.setOnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                view.alpha = 0.7f   //这里改变前，可以存储原view.alpha值，这样不会影响设置了alpha的view
            }

            MotionEvent.ACTION_UP -> {
                view.alpha = 1f//存储了alpha，取出值
            }

            MotionEvent.ACTION_CANCEL -> {
                view.alpha = 1f
            }
        }
        return@setOnTouchListener false
    }
}

/**
 * @param during 防抖时间间隔
 * @param combine 一个接口中的多个回调方法是否共用防抖时间
 */
@SuppressLint("ALL")
@Suppress("UNCHECKED_CAST")
fun <T> T.throttle(during: Long = 2000L, combine: Boolean = true): T {
    return Proxy.newProxyInstance(
        this!!::class.java.classLoader, this::class.java.interfaces,
        object : InvocationHandler {
            private val map = HashMap<Method?, Long>()

            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
                val current = System.currentTimeMillis()
                return if (current - (map[if (combine) null else method] ?: 0) > during) {
                    map[if (combine) null else method] = current
                    method.invoke(this@throttle, *args.orEmpty())
                } else {
                    resolveDefaultReturnValue(method)
                }

            }

        }
    ) as T
}

private fun resolveDefaultReturnValue(method: Method): Any? {
    return when (method.returnType.name.lowercase(Locale.US)) {
        Void::class.java.simpleName.lowercase(Locale.US) -> null
        else -> throw kotlin.IllegalArgumentException("无法正确对返回值不为空的回调进行节流")
    }
}

/**
 * 纯色背景过度
 */
fun View.animChangeBackground(start: Int, end: Int, duration: Long = 400) {
    try {
        val colorAnim: ValueAnimator = ObjectAnimator.ofInt(this, "backgroundColor", start, end)
        colorAnim.duration = duration
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.repeatCount = 0
        colorAnim.repeatMode = ValueAnimator.REVERSE
        colorAnim.start()
    } catch (e: Exception) {
        this.setBackgroundColor(end)
    }
}

//呼吸动画
fun View.breathAnim(repeatCount: Int = ValueAnimator.INFINITE, duration: Long = 800): AnimatorSet {
    val anim1 = ObjectAnimator.ofFloat(this, "scaleX", 1.1f, 0.9f)
    val anim2 = ObjectAnimator.ofFloat(this, "scaleY", 1.1f, 0.9f)
    val sets = AnimatorSet()
    sets.playTogether(anim1, anim2)
    sets.interpolator = LinearInterpolator()
    sets.duration = duration
    anim1.repeatMode = ValueAnimator.REVERSE
    anim2.repeatMode = ValueAnimator.REVERSE
    anim1.repeatCount = repeatCount
    anim2.repeatCount = repeatCount
    sets.start()
    return sets
}

//摇晃动画
fun View.shake(): AnimatorSet {
    val animatorX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.08f, 1f)
    val animatorY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.08f, 1f)
    val rotation = ObjectAnimator.ofFloat(
        this,
        "rotation",
        0f,
        15f,
        0f,
        -15f,
        0f,
        12f,
        0f,
        -12f,
        0f,
        9f,
        0f,
        -9f,
        0f,
        6f,
        0f,
        -6f,
        0f,
        3f,
        0f,
        -3f,
        0f
    )
    val animatorSet = AnimatorSet()
    animatorSet.playTogether(animatorX, animatorY, rotation)
    animatorSet.duration = 600
    animatorSet.interpolator = LinearInterpolator()
    animatorSet.startDelay = 1200
    var cancel = false
    animatorSet.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            if (!cancel) {
                animatorSet.start()
            }
        }

        override fun onAnimationCancel(animation: Animator) {
            super.onAnimationCancel(animation)
            cancel = true
        }
    })
    animatorSet.start()
    return animatorSet
}


/**
 * 设置View的margin
 * @param leftMargin 默认保留原来的
 * @param topMargin 默认是保留原来的
 * @param rightMargin 默认是保留原来的
 * @param bottomMargin 默认是保留原来的
 */
fun View.margin(
    leftMargin: Int = Int.MAX_VALUE,
    topMargin: Int = Int.MAX_VALUE,
    rightMargin: Int = Int.MAX_VALUE,
    bottomMargin: Int = Int.MAX_VALUE
): View {
    val params = layoutParams as ViewGroup.MarginLayoutParams

    if (leftMargin != Int.MAX_VALUE) params.leftMargin = leftMargin
    if (topMargin != Int.MAX_VALUE) params.topMargin = topMargin
    if (rightMargin != Int.MAX_VALUE) params.rightMargin = rightMargin
    if (bottomMargin != Int.MAX_VALUE) params.bottomMargin = bottomMargin
    layoutParams = params

    return this
}


/**
 * 设置宽度，带有过渡动画
 * @param targetValue 目标宽度
 * @param duration 时长
 * @param action 可选行为
 */
fun View.animateWidth(
    targetValue: Int, duration: Long = 400, listener: Animator.AnimatorListener? = null,
    action: ((Float) -> Unit)? = null
) {
    post {
        ValueAnimator.ofInt(width, targetValue).apply {
            addUpdateListener {
                width(it.animatedValue as Int)
                action?.invoke((it.animatedFraction))
            }
            if (listener != null) addListener(listener)
            setDuration(duration)
            start()
        }
    }
}


/**
 * 设置View的宽度
 */
fun View.width(width: Int): View {
    val params = layoutParams ?: ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    params.width = width
    layoutParams = params
    return this
}

/**
 * 设置View的高度
 */
fun View.height(height: Int): View {
    val params = layoutParams ?: ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    params.height = height
    layoutParams = params
    return this
}

/**
 * 设置View的高度
 */
fun View.maxHeight(height: Int): View {
    postDelayed({
        val params = layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        if (this.measuredHeight > height) {
            params.height = height
        }
        layoutParams = params
    }, 1)
    return this
}

/**
 * 设置宽度和高度，带有过渡动画
 * @param targetWidth 目标宽度
 * @param targetHeight 目标高度
 * @param duration 时长
 * @param action 可选行为
 */
fun View.animateWidthAndHeight(
    targetWidth: Int,
    targetHeight: Int,
    duration: Long = 400,
    listener: Animator.AnimatorListener? = null,
    action: ((Float) -> Unit)? = null
) {
    post {
        val startHeight = height
        val evaluator = IntEvaluator()
        ValueAnimator.ofInt(width, targetWidth).apply {
            addUpdateListener {
                widthAndHeight(
                    it.animatedValue as Int,
                    evaluator.evaluate(it.animatedFraction, startHeight, targetHeight)
                )
                action?.invoke((it.animatedFraction))
            }
            if (listener != null) addListener(listener)
            setDuration(duration)
            start()
        }
    }
}

/**
 * 设置View的宽度和高度
 * @param width 要设置的宽度
 * @param height 要设置的高度
 */
fun View.widthAndHeight(width: Int, height: Int): View {
    val params = layoutParams ?: ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    params.width = width
    params.height = height
    layoutParams = params
    return this
}

fun Float.dip2px(): Int {
    val scale = App.app.resources.displayMetrics.density
    return (this * scale + 0.5f).toInt()
}

/*** 可见性相关 ****/
fun View.gone(delay: Long = 0) {
    if (delay > 0) {
        postDelayed({ visibility = View.GONE }, delay)
        return
    }
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

val View.isGone: Boolean
    get() {
        return visibility == View.GONE
    }

val View.isVisible: Boolean
    get() {
        return visibility == View.VISIBLE
    }
val View.isNotVisible: Boolean
    get() {
        return visibility != View.VISIBLE
    }

val View.isInvisible: Boolean
    get() {
        return visibility == View.INVISIBLE
    }

/**
 * 单纯禁用view事件
 */
fun View.setOnTouchEnable(enable: Boolean, autoRemove: Boolean = false) {
    this.setOnTouchListener(object : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(
            v: View?,
            event: MotionEvent?
        ): Boolean {
            if (autoRemove) {
                this@setOnTouchEnable.setOnTouchListener(null)
            }
            return enable
        }
    })
}

/**
 * 切换View的可见性
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
}

/**
 * 获取View的截图, 支持获取整个RecyclerView列表的长截图
 * 注意：调用该方法时，请确保View已经测量完毕，如果宽高为0，则将抛出异常
 */
fun View.toBitmap(): Bitmap {
    if (measuredWidth == 0 || measuredHeight == 0) {
        throw kotlin.RuntimeException("调用该方法时，请确保View已经测量完毕，如果宽高为0，则抛出异常以提醒！")
    }
    return when (this) {
        is RecyclerView -> {
            this.scrollToPosition(0)
            this.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val bmp = Bitmap.createBitmap(width, measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            //draw default bg, otherwise will be black
            if (background != null) {
                background.setBounds(0, 0, width, measuredHeight)
                background.draw(canvas)
            } else {
                canvas.drawColor(Color.WHITE)
            }
            this.draw(canvas)
            //恢复高度
            this.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
            )
            bmp //return
        }

        else -> {
            val screenshot =
                Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(screenshot)
            if (background != null) {
                background.setBounds(0, 0, width, measuredHeight)
                background.draw(canvas)
            } else {
                canvas.drawColor(Color.WHITE)
            }
            draw(canvas)// 将 view 画到画布上
            screenshot //return
        }
    }
}

/**
 * 是否是从右到左的布局
 *
 * @since 1.0.2
 */
fun View.isLayoutDirectionRTL(): Boolean {
    return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
}

/**
 * 设置背景颜色
 *
 * @since 1.0.2
 */
fun View.setBackgroundColor(colorStr: String) {
    this.setBackgroundColor(Color.parseColor(colorStr))
}


/**
 * 设置paddingTop
 *
 * @since 1.0.2
 */
fun View.setPaddingTop(value: Int) {
    this.setPadding(0, value, 0, 0)
}

/**
 * 设置paddingStart
 *
 * @since 1.0.2
 */
fun View.setPaddingStart(value: Int) {
    if (isLayoutDirectionRTL()) {
        this.setPadding(0, 0, value, 0)
    } else {
        this.setPadding(value, 0, 0, 0)
    }
}

/**
 * 设置paddingEnd
 *
 * @since 1.0.2
 */
fun View.setPaddingEnd(value: Int) {
    if (isLayoutDirectionRTL()) {
        this.setPadding(value, 0, 0, 0)
    } else {
        this.setPadding(0, 0, value, 0)
    }
}

/**
 * 设置paddingBottom
 *
 * @since 1.0.2
 */
fun View.setPaddingBottom(value: Int) {
    this.setPadding(0, 0, 0, value)
}

/**
 * 从父布局里面删除自己
 *
 * @since 1.0.4
 */
fun View.removeFromParent() {
    this.parent?.let {
        (it as ViewGroup).removeView(this)
    }
}

fun TextView.lineMaxNumber(text: String?): Int {
    try {
        if (null == text || "" == text) {
            return 0
        }
        val staticLayout = this.let {
            StaticLayout.Builder.obtain(
                text, 0, text.length, it.paint, it.measuredWidth - it.marginStart - it.marginEnd
            ).build()
        }
        return staticLayout.getLineEnd(0)
    } catch (e: IllegalArgumentException) {
        return 0
    }
}

fun View.addBorder(borderColor: Int, borderWidth: Int = 3) {
    val gradientDrawable: GradientDrawable

    if (this.background is GradientDrawable) {
        gradientDrawable = this.background as GradientDrawable
    } else {
        gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(0x00000000) // 透明色
    }
    gradientDrawable.setStroke(DisplayUtil.dip2px(borderWidth.toFloat()), borderColor)
    this.background = gradientDrawable
}