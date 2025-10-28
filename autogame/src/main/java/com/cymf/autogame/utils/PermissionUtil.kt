package com.cymf.autogame.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object PermissionUtil {
    /**
     * @param context
     * @return
     * AccessibilityService permission check
     */
    fun isAccessibilityServiceEnable(context: Context): Boolean {
        val accessibilityManager =
            (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
        val accessibilityServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        for (info in accessibilityServices) {
            if (info.id.contains(context.packageName)) {
                return true
            }
        }
        return false
    }

    fun isSettingsCanWrite(context: Context?): Boolean {
        return Settings.System.canWrite(context)
    }

    fun isCanDrawOverlays(context: Context?): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isUsageAccessPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION") appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // 如果没有权限，可以引导用户去设置页面打开
    fun requestUsageAccessPermission(activity: Activity) {
        if (!isUsageAccessPermissionGranted(activity)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            activity.startActivity(intent)
        }
    }
}