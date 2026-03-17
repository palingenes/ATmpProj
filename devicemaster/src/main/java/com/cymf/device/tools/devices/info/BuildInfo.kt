package com.cymf.device.tools.devices.info

import android.text.TextUtils
import androidx.core.util.Pair
import com.cymf.device.tools.devices.utils.CommandUtils

/**
 * 编译信息
 */
object BuildInfo {
    fun getBuildInfo( ): MutableList<Pair<String?, String?>?> {
        val list: MutableList<Pair<String?, String?>?> = ArrayList<Pair<String?, String?>?>()
        val array = CommandUtils.exec("getprop")
        for (item in array) {
            if (!TextUtils.isEmpty(item)) {
                try {
                    val split: Array<String?> =
                        item!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (split.size == 1) {
                        list.add(
                            Pair<String?, String?>(
                                split[0]!!.trim { it <= ' ' },
                                "UNKNOWN"
                            )
                        )
                    } else if (split.size == 2) {
                        list.add(
                            Pair<String?, String?>(
                                split[0]!!.trim { it <= ' ' },
                                split[1]!!.trim { it <= ' ' })
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }
}