package com.cymf.device.tools

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi

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
}