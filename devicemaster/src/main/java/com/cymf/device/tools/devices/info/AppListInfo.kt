package com.cymf.device.tools.devices.info

import android.content.Context
import android.content.pm.ApplicationInfo
import com.cymf.device.tools.devices.beans.ApplicationBean

/**
 * 应用列表
 */
object AppListInfo {
    /**
     * 获取应用列表
     *
     */
    fun getAppListInfo(context: Context): MutableList<ApplicationBean?> {
        val list: MutableList<ApplicationBean?> = ArrayList<ApplicationBean?>()
        val packageManager = context.applicationContext.packageManager
        val installedPackages = packageManager.getInstalledPackages(0)
        for (info in installedPackages) {
            val bean = ApplicationBean()
            checkNotNull(info.applicationInfo)
            bean.name = info.applicationInfo!!.loadLabel(packageManager).toString()
            bean.packageName = info.packageName
            bean.version = info.versionName
            bean.icon = info.applicationInfo!!.loadIcon(packageManager)
            bean.buildVersion = info.applicationInfo!!.targetSdkVersion
            bean.isSystemApp = (ApplicationInfo.FLAG_SYSTEM and info.applicationInfo!!.flags) != 0
            list.add(bean)
        }
        return list
    }
}
