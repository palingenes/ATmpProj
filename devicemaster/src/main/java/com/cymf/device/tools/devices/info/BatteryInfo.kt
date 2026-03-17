package com.cymf.device.tools.devices.info

import android.content.Context
import androidx.core.util.Pair
import com.cymf.device.tools.devices.utils.BatteryUtils

/**
 * 电池信息
 */
object BatteryInfo {
    /**
     * 获取电池信息
     *
     * @return 电池JSON
     */
    fun getBatteryInfo(context: Context?): MutableList<Pair<String?, String?>?> {
        val list = ArrayList<Pair<String?, String?>?>()
        BatteryUtils.getBatteryInfo(context, list)
        return list
    }
}
