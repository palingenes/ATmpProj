package com.cymf.device.tools.devices.info

import android.text.TextUtils
import androidx.core.util.Pair
import com.cymf.device.tools.devices.utils.FileUtils
import java.io.File
import java.io.FilenameFilter

/**
 * 温度信息
 */
object ThermalInfo {
    val thermalInfo: MutableList<Pair<String?, String?>?>
        get() {
            val list: MutableList<Pair<String?, String?>?> =
                ArrayList<Pair<String?, String?>?>()
            val files =
                filter("/sys/class/thermal/", ThermalFilter("thermal_zone"))
            if (files != null) {
                for (file in files) {
                    val type =
                        FileUtils.readFile(
                            File(file, "type")
                        )
                    if (!TextUtils.isEmpty(type)) {
                        val temp =
                            FileUtils.readFile(
                                File(file, "temp")
                            )
                        if (!TextUtils.isEmpty(temp)) {
                            list.add(
                                Pair<String?, String?>(
                                    type!!.trim { it <= ' ' },
                                    temp!!.trim { it <= ' ' })
                            )
                        }
                    }
                }
            }
            return list
        }

    private fun filter(path: String, filter: FilenameFilter?): Array<File?>? {
        val file = File(path)
        if (file.exists()) {
            return file.listFiles(filter)
        }
        return null
    }

    private class ThermalFilter(private val condition: String) : FilenameFilter {
        override fun accept(dir: File?, name: String): Boolean {
            return name.startsWith(condition)
        }
    }
}
