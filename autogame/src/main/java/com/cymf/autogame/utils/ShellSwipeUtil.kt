package com.cymf.autogame.utils

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.cymf.autogame.utils.AssistsCore.getBoundsInScreen
import com.topjohnwu.superuser.Shell
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.random.Random

object ShellSwipeUtil {

    // dp 转 px
    private fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }


    fun AccessibilityNodeInfo.clickInBounds(isWebview: Boolean = false): Boolean {
        val durationMs = TaskPollingManager.getCurrTaskSpeed()
        return this.getBoundsInScreen().clickInBounds(durationMs, isWebview)
    }

    fun Rect.clickInBounds(durationMs: Long = -1, isWebview: Boolean = false): Boolean {
        val du = if (durationMs == -1L) {
            TaskPollingManager.getCurrTaskSpeed()
        } else durationMs

        val random = Random.Default
        val clickDownUpDiffHappen = TaskPollingManager.getCurrTask()?.let { task ->
            random.nextFloat() <= ((if (isWebview) task.webviewDownUpDiffRate else task.activityDownUpDiffRate)
                ?: 0.0)
        } ?: false

        // 计算安全区域（留出边距）
        val safeLeft = this.left + 7
        val safeTop = this.top + 7
        val safeRight = this.right - 7
        val safeBottom = this.bottom - 7

        if (safeRight <= safeLeft || safeBottom <= safeTop) {
            return false
        }

        // 生成起始坐标（浮点数）
        val startX = generateRandomFloatInRange(
            safeLeft.toFloat(),
            safeRight.toFloat()
        )
        val startY = ShellSwipeUtil.generateRandomFloatInRange(
            safeTop.toFloat(),
            safeBottom.toFloat()
        )

        val command = if (clickDownUpDiffHappen) {
            // 添加轻微抖动（-0.3 ~ +0.3 之间）
            val jitterX = Random.nextFloat() * 0.6f - 0.3f
            val jitterY = Random.nextFloat() * 0.6f - 0.3f

            val endX = max(safeLeft.toFloat(), startX + jitterX)
            val endY = max(safeTop.toFloat(), startY + jitterY)

            YLLogger.e("startX=$startX , startY=$startY , endX=$endX , endY=$endY , durationMs=$du")

            // 构造 input 滑动命令（支持浮点数）
            "input swipe $startX $startY $endX $endY $du"
        } else {
            "input tap $startX $startY"
        }
        return Shell.cmd(command).exec().isSuccess
    }

    fun randomSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long = 100) {
        val offsetX = (Math.random() * 20 - 10).toInt()
        val offsetY = (Math.random() * 20 - 10).toInt()

        swipe(startX + offsetX, startY + offsetY, endX + offsetX, endY + offsetY, durationMs)
    }

    /**
     * 基础滑动方法（使用像素坐标）
     */
    fun swipe(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long = 100
    ) {
        val sX = formatToRandom4Or5Decimal(startX.toFloat())
        val sY = formatToRandom4Or5Decimal(startY.toFloat())
        val eX = formatToRandom4Or5Decimal(endX.toFloat())
        val eY = formatToRandom4Or5Decimal(endY.toFloat())

        val command = "input swipe $sX $sY $eX $eY $durationMs\nexit\n"
        Shell.cmd(command).submit()
    }

    /**
     * 支持 dp 的滑动方法
     */
    fun swipeDp(
        context: Context,
        startXDp: Float,
        startYDp: Float,
        endXDp: Float,
        endYDp: Float,
        durationMs: Long = 100
    ) {
        try {
            val startX = dp2px(context, startXDp)
            val startY = dp2px(context, startYDp)
            val endX = dp2px(context, endXDp)
            val endY = dp2px(context, endYDp)

            swipe(startX, startY, endX, endY, durationMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取屏幕尺寸（px）
     */
    private fun getScreenWidthPx(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    private fun getScreenHeightPx(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * 获取屏幕尺寸（dp）
     */
    private fun getScreenWidthDp(context: Context): Float {
        return getScreenWidthPx(context) / context.resources.displayMetrics.density
    }

    private fun getScreenHeightDp(context: Context): Float {
        return getScreenHeightPx(context) / context.resources.displayMetrics.density
    }

    // ===================== 方向识别滑动 ===================== //

    /**
     * 从底部向上滑动（常用于任务栏关闭应用）
     */
    fun swipeUpFromBottom(
        context: Context,
        startYPercent: Float = 0.8f,
        endYPercent: Float = 0.8f,
        durationMs: Long = 100
    ) {
        val centerX = formatToRandom4Or5Decimal(
            (getScreenWidthDp(context) / Random.nextDouble(
                1.8,
                2.2
            )).toFloat()
        ).toFloat()
        val screenHeightDp = getScreenHeightDp(context)

        val startY = formatToRandom4Or5Decimal(screenHeightDp * startYPercent).toFloat()
        val endY = formatToRandom4Or5Decimal(screenHeightDp * endYPercent).toFloat()

        swipeDp(context, centerX, startY, centerX, endY, durationMs)
    }

    /**
     * 从顶部向下滑动
     */
    fun swipeDownFromTop(
        context: Context,
        percent: Float = 0.6f,
        durationMs: Long = 100
    ) {
        val screenHeightDp = getScreenHeightDp(context)
        val centerX = getScreenWidthDp(context) / 2f

        val startY = screenHeightDp * 0.2f
        val endY = startY + screenHeightDp * percent

        swipeDp(context, centerX, startY, centerX, endY, durationMs)
    }

    /**
     * 从右侧向左滑动（如返回手势）
     */
    fun swipeLeftFromRight(
        context: Context,
        percent: Float = 0.6f,
        durationMs: Long = 100
    ) {
        val screenWidthDp = getScreenWidthDp(context)
        val centerY = getScreenHeightDp(context) / 2f

        val startX = screenWidthDp * 0.8f
        val endX = startX - screenWidthDp * percent

        swipeDp(context, startX, centerY, endX, centerY, durationMs)
    }

    /**
     * 从左侧向右滑动（如打开抽屉菜单）
     */
    fun swipeRightFromLeft(
        context: Context,
        percent: Float = 0.6f,
        durationMs: Long = 100
    ) {
        val screenWidthDp = getScreenWidthDp(context)
        val centerY = getScreenHeightDp(context) / 2f

        val startX = screenWidthDp * 0.2f
        val endX = startX + screenWidthDp * percent

        swipeDp(context, startX, centerY, endX, centerY, durationMs)
    }

    // 生成指定范围内的浮点数，保留 4~5 位小数（5位概率90%）
    private fun generateRandomFloatInRange(min: Float, max: Float): Float {
        val value = min + Random.nextFloat() * (max - min)
        val decimalPlaces = if (Random.nextFloat() < 0.9f) 5 else 4
        return roundToDecimalPlaces(value, decimalPlaces)
    }

    // 四舍五入到指定位数的小数
    private fun roundToDecimalPlaces(value: Float, places: Int): Float {
        if (places < 0) throw IllegalArgumentException()

        var bd = BigDecimal(value.toDouble())
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toFloat()
    }

    /**
     * 随机生成4-5位小数
     */
    fun formatToRandom4Or5Decimal(value: Float): String {
        val str = value.toString()
        val dotIndex = str.indexOf('.')
        val integerPart = if (dotIndex == -1) str else str.substring(0, dotIndex)
        val decimalPart = if (dotIndex == -1) "" else str.substring(dotIndex + 1)

        val targetLength = if (Math.random() < 0.9) 5 else 4

        val sb = StringBuilder()
        var i = 0
        while (i < targetLength && i < decimalPart.length) {
            sb.append(decimalPart[i])
            i++
        }
        while (i < targetLength) {
            sb.append((1..9).random())
            i++
        }
        return "$integerPart.$sb"
    }
}