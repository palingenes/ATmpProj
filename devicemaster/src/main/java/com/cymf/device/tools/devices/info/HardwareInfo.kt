package com.cymf.device.tools.devices.info

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.util.Pair

/**
 * 硬件信息
 */
object HardwareInfo {

    fun getHardwareInfo(context: Context): MutableList<Pair<String?, String?>?> {
        val list: MutableList<Pair<String?, String?>?> = ArrayList<Pair<String?, String?>?>()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        list.add(Pair("GYROSCOPE", gyroscope?.name ?: "UNKNOWN"))
        list.add(Pair("MAGNETIC", magnetic?.name ?: "UNKNOWN"))
        list.add(Pair("ACCELEROMETER", accelerometer?.name ?: "UNKNOWN"))
        return list
    }
}
