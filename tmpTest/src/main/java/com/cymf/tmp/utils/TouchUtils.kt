package com.cymf.tmp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View

/**
 * 多点触控及触摸点数量统计
 */
object TouchUtils {

    private const val TAG = "TouchUtils"

    /**
     * 检查设备是否支持多点触控，并打印支持的类型
     */
    fun checkMultiTouchSupport(context: Context) {
        val pm = context.packageManager

        if (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)) {
            Log.d(TAG, "设备支持多点触控")

            if (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)) {
                Log.d(TAG, "支持区分轻按和重按的多点触控")
            }

            if (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND)) {
                Log.d(TAG, "支持至少5个触控点的高级多点触控（JazzHand）")
            }
        } else {
            Log.d(TAG, "设备不支持多点触控")
        }
    }

    /**
     * 获取当前 MotionEvent 中的触摸点数量
     */
    fun getPointerCount(event: MotionEvent): Int {
        return event.pointerCount
    }

    /**
     * 注册一个 OnTouchListener 到指定 View 上，用于监听并打印当前触摸点数
     */
    @SuppressLint("ClickableViewAccessibility")
    fun listenTouchPoint(view: View, listener: ((Int) -> Unit)? = null) {
        view.setOnTouchListener { _, event ->
            val pointerCount = getPointerCount(event)
            Log.d(TAG, "当前触摸点数: $pointerCount")
            listener?.invoke(pointerCount)
            true
        }
    }

    /**
     * 获取设备支持的最大触控点数（通过遍历 InputDevice）
     * 注意：这个方法不能精确获取设备的最大触控点数，只能作为参考。
     */
    fun getMaxTouchPointsFromInputDevice(): Int {
        var maxPoints = 0
        val deviceIds = InputDevice.getDeviceIds()

        for (id in deviceIds) {
            val device = InputDevice.getDevice(id)
            if (device == null) continue
            if ((device.sources and InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) {
                val touchPoints = device.motionRanges.size
                if (touchPoints > maxPoints) {
                    maxPoints = touchPoints
                }
            }
        }

        return maxPoints.takeIf { it > 0 } ?: -1
    }
}