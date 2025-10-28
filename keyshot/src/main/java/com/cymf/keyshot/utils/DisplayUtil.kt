package com.cymf.keyshot.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.ColorInt
import com.cymf.keyshot.App
import kotlin.jvm.java
import kotlin.ranges.until
import kotlin.text.toInt

@Suppress("DEPRECATION")
@SuppressLint("DiscouragedApi", "InternalInsetResource")
object DisplayUtil {
    /**
     * 利用反射获取状态栏高度
     */

    @JvmStatic
    fun statusBarHeight(): Int {
        var result = 0
        //获取状态栏高度的资源id
        try {
            val resources = App.app.resources
            val resourceId =
                resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId)
            }
            if (result <= 0) // 双重校验
            {
                @SuppressLint("PrivateApi") val c = Class.forName("com.android.internal.R\$dimen")
                val obj = c.newInstance()
                val field = c.getField("status_bar_height")
                val o = field[obj]
                if (o != null) {
                    val x = o.toString().toInt()
                    result = App.app.resources.getDimensionPixelSize(x)
                }
            }
        } catch (ignored: Exception) {
        }
        return result
    }

    /**
     * 获取 虚拟按键的高度
     */
    fun bottomStatusHeight(context: Context): Int {
        return try {
            if (checkNavigationBarShow(context)) {
                val totalHeight = dpi
                val dm = context.resources.displayMetrics
                val contentHeight = dm.heightPixels
                totalHeight - contentHeight
            } else {
                val resourceId =
                    context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
                context.resources.getDimensionPixelSize(resourceId)
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 该方法需要在View完全被绘制出来之后调用，否则判断不了
     * 在比如 onWindowFocusChanged（）方法中可以得到正确的结果
     */
    fun isNavigationBarExist(activity: Activity?): Boolean {
        val decorView: View = activity?.window?.decorView ?: return false
        val vp = decorView as ViewGroup
        for (i in 0 until vp.childCount) {
            vp.getChildAt(i).context.packageName
            if (vp.getChildAt(i).id != View.NO_ID && "navigationBarBackground" == activity.resources.getResourceEntryName(
                    vp.getChildAt(i).id
                )
            ) {
                return true
            }
        }
        return false
    }

    /**
     * 获取屏幕宽高
     */
    private fun metrics(): DisplayMetrics {
        val resources =App. app.resources
        return resources.displayMetrics
    }

    /**
     * @return 屏幕宽度
     */
    fun metricsWidth(): Int {
        val metrics = metrics()
        return metrics.widthPixels
    }

    /**
     * @return 屏幕高度
     */
    fun metricsHeight(): Int {
        val metrics = metrics()
        return metrics.heightPixels
    }

    @JvmStatic
    fun widthPixels(): Int {
        return try {
            displayMetrics().widthPixels
        } catch (e: Exception) {
            750
        }
    }

    @JvmStatic
    fun heightPixels(): Int {
        return try {
            displayMetrics().heightPixels
        } catch (e: Exception) {
            1334
        }
    }

    private fun displayMetrics(): DisplayMetrics {
        val windowManager = App.app
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        return outMetrics
    }

    /**
     * convert px to its equivalent dp
     *
     *
     * 将px转换为与之相等的dp
     */
    @JvmStatic
    fun px2dp(pxValue: Float): Int {
        val scale = App.app.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * convert dp to its equivalent px
     *
     *
     * 将dp转换为与之相等的px
     */
    @JvmStatic
    fun dip2px(dipValue: Float): Int {
        val scale =App. app.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    /**
     * convert px to its equivalent sp
     *
     *
     * 将px转换为sp
     */
    fun px2sp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    /**
     * convert sp to its equivalent px
     *
     *
     * 将sp转换为px
     */
    @JvmStatic
    fun sp2px(spValue: Float): Float {
        val fontScale =App. app.resources.displayMetrics.scaledDensity
        return spValue * fontScale + 0.5f
    }

    private val dpi: Int
        /**
         * 获取屏幕原始尺寸高度，包括虚拟功能键高度
         */
        get() {
            var dpi = 0
            val windowManager = App.app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val displayMetrics = DisplayMetrics()
            val c: Class<*>
            try {
                c = Class.forName("android.view.Display")
                val method = c.getMethod("getRealMetrics", DisplayMetrics::class.java)
                method.invoke(display, displayMetrics)
                dpi = displayMetrics.heightPixels
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return dpi
        }

    /**
     * 判断虚拟导航栏是否显示
     *
     * @return true(显示虚拟导航栏)，false(不显示或不支持虚拟导航栏)
     */
    private fun checkNavigationBarShow(context: Context): Boolean {
        var hasNavigationBar = false
        val rs = context.resources
        val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id)
        }
        try {
            @SuppressLint("PrivateApi") val systemPropertiesClass =
                Class.forName("android.os.SystemProperties")
            val m = systemPropertiesClass.getMethod("get", String::class.java)
            val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
            // 判断是否隐藏了底部虚拟导航
            val navigationBarIsMin = Settings.Global.getInt(
                context.contentResolver,
                "navigationbar_is_min", 0
            )
            if ("1" == navBarOverride || 1 == navigationBarIsMin) {
                hasNavigationBar = false
            } else if ("0" == navBarOverride) {
                hasNavigationBar = true
            }
        } catch (ignored: Exception) {
        }
        return hasNavigationBar
    }

    /**
     * 创建背景颜色
     *
     * @param color       填充色
     * @param strokeColor 线条颜色
     * @param strokeWidth 线条宽度  单位px
     * @param radius      角度  px
     */
    fun createRectangleDrawable(
        @ColorInt color: IntArray?,
        @ColorInt strokeColor: Int,
        strokeWidth: Int,
        radius: Float
    ): GradientDrawable {
        return try {
            val radiusBg = GradientDrawable()
            //设置Shape类型
            radiusBg.shape = GradientDrawable.RECTANGLE
            //设置填充颜色
            radiusBg.colors = color
            if (strokeWidth > 0) {
                //设置线条粗心和颜色,px
                radiusBg.setStroke(strokeWidth, strokeColor)
            }
            //设置圆角角度,如果每个角度都一样,则使用此方法
            radiusBg.cornerRadius = radius
            radiusBg
        } catch (_: Exception) {
            GradientDrawable()
        }
    }
}