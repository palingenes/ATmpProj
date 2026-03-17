package com.cymf.tmp.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

object AppInfoUtil {

    /**
     * 获取应用的首次安装时间。
     */
    fun getAppInstallTime(context: Context, packageName: String = context.packageName): Long {
        return getPackageInfo(context, packageName).firstInstallTime
    }

    /**
     * 获取应用的最后一次更新时间。
     */
    fun getAppUpdateTime(context: Context, packageName: String = context.packageName): Long {
        return getPackageInfo(context, packageName).lastUpdateTime
    }

    /**
     * 获取应用的安装来源(package installer)。
     * 需要 API level 29 及以上。
     */
    @Suppress("DEPRECATION")
    fun getAppInstaller(context: Context, packageName: String = context.packageName): String? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11 and above
            val packageManager = context.packageManager
            try {
                packageManager.getInstallSourceInfo(packageName).installingPackageName ?: "未知来源"
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                "未知来源"
            }
        } else {
            // For Android versions below 11
            context.packageManager.getInstallerPackageName(packageName)
        }
    }

    private fun getPackageInfo(context: Context, packageName: String): PackageInfo {
        val packageManager = context.packageManager
        return packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
        )
    }

    /**
     * 将时间戳转换为可读格式（包含毫秒）
     */
    fun timestampToReadableDate(time: Long): String {
        val date = java.util.Date(time)
        val format =
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())
        return format.format(date)
    }
}