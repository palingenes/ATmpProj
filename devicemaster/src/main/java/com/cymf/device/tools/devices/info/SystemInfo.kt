package com.cymf.device.tools.devices.info

import android.app.Activity
import android.os.Build
import androidx.core.util.Pair
import com.cymf.device.R
import com.cymf.device.tools.devices.utils.DensityUtils
import com.cymf.device.tools.devices.utils.SocUtils
import com.cymf.device.tools.devices.utils.SystemConfigUtils
import com.cymf.device.tools.devices.utils.CommandUtils

/**
 * 系统信息
 */
object SystemInfo {
    fun getSystemInfo(activity: Activity): MutableList<Pair<String?, String?>?> {
        val list = ArrayList<Pair<String?, String?>?>()
        val context = activity.applicationContext
        list.add(Pair(context.getString(R.string.system_manufacture), Build.MANUFACTURER))
        list.add(Pair(context.getString(R.string.system_model), Build.MODEL))
        list.add(Pair(context.getString(R.string.system_brand), Build.BRAND))
        list.add(Pair(context.getString(R.string.system_release), Build.VERSION.RELEASE))
        list.add(Pair(context.getString(R.string.system_api), Build.VERSION.SDK_INT.toString()))
        list.add(Pair(context.getString(R.string.system_code_name), Build.VERSION.CODENAME))
        list.add(
            Pair(
                context.getString(R.string.system_density),
                DensityUtils.getDensityDpi(context).toString() + " (" + DensityUtils.getDensityId(
                    context
                ) + ")"
            )
        )
        list.add(
            Pair(
                context.getString(R.string.system_refresh_rate),
                DensityUtils.getRefreshRate(activity).toString() + " Hz"
            )
        )
        Build.VERSION.SDK
        list.add(Pair(context.getString(R.string.system_device), Build.DEVICE))
        list.add(Pair(context.getString(R.string.system_product), Build.PRODUCT))
        list.add(Pair(context.getString(R.string.system_board), Build.BOARD))
        list.add(Pair(context.getString(R.string.system_platform), SocUtils.getSocInfo()))
        list.add(Pair<String?, String?>(context.getString(R.string.system_build), Build.ID))
        list.add(Pair(context.getString(R.string.system_vm), System.getProperty("java.vm.version")))
        list.add(
            Pair(
                context.getString(R.string.system_security),
                CommandUtils.getProperty("ro.build.version.security_patch")
            )
        )
        list.add(
            Pair(
                context.getString(R.string.system_baseband),
                CommandUtils.getProperty("gsm.version.baseband")
            )
        )
        list.add(Pair<String?, String?>(context.getString(R.string.system_build_type), Build.TYPE))
        list.add(Pair<String?, String?>(context.getString(R.string.system_tags), Build.TAGS))
        list.add(
            Pair<String?, String?>(
                context.getString(R.string.system_incremental),
                CommandUtils.getProperty("ro.build.version.incremental")
            )
        )
        list.add(
            Pair<String?, String?>(
                context.getString(R.string.system_description),
                CommandUtils.getProperty("ro.build.description")
            )
        )
        list.add(
            Pair<String?, String?>(
                context.getString(R.string.system_fingerprint),
                Build.FINGERPRINT
            )
        )
        list.add(Pair<String?, String?>(context.getString(R.string.system_device_features), "68"))
        list.add(
            Pair<String?, String?>(
                context.getString(R.string.system_builder),
                "builder@" + CommandUtils.getProperty("ro.build.host")
            )
        )
        list.add(
            Pair<String?, String?>(
                context.getString(R.string.system_language),
                SystemConfigUtils.getCurrentLanguage(context)
            )
        )
        list.add(
            Pair<String?, String?>(
                context.getString(R.string.system_timezone),
                SystemConfigUtils.getCurrentTimeZone()
            )
        )
        list.add(
            Pair<String?, String?>(
                context.getString(R.string.system_uptime),
                SystemConfigUtils.getSystemUpdate()
            )
        )
        return list
    }
}
