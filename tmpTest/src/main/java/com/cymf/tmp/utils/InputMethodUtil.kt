package com.cymf.tmp.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import java.lang.reflect.Method


object InputMethodUtil {


    data class InputMethodInfoModel(
        val id: String,
        val packageName: String,
        val serviceName: String,
        val settingsActivity: String?,
        val isDefault: Boolean = false,
        val isEnabled: Boolean = false
    )

    /**
     * 获取所有输入法信息列表（包括未启用的），并标记是否启用状态
     */
    fun getAllInputMethods(context: Context): List<InputMethodInfoModel> {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                ?: return emptyList()

        // 所有输入法（包括未启用）
        val allInputMethods =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inputMethodManager.inputMethodList
            } else {
                @Suppress("DEPRECATION")
                inputMethodManager.inputMethodList
            }

        // 已启用的输入法 ID 集合
        val enabledInputMethodInfos =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inputMethodManager.enabledInputMethodList
            } else {
                @Suppress("DEPRECATION")
                inputMethodManager.enabledInputMethodList
            }
        val enabledIds = HashSet<String>().apply {
            enabledInputMethodInfos.forEach { add(it.id) }
        }

        // 默认输入法 ID
        val defaultImeId = getDefaultInputMethodId(context)

        return allInputMethods.map { info ->
            val serviceInfo = info.serviceInfo
            val settingsActivity = info.settingsActivity

            val isDefault = info.id == defaultImeId
            val isEnabled = enabledIds.contains(info.id)

            InputMethodInfoModel(
                id = info.id,
                packageName = serviceInfo.packageName,
                serviceName = serviceInfo.name,
                settingsActivity = settingsActivity,
                isDefault = isDefault,
                isEnabled = isEnabled
            )
        }
    }

    /**
     * 获取当前默认输入法 ID（兼容 API 21+）
     */
    private fun getDefaultInputMethodId(context: Context): String? {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                "default_input_method"
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取当前激活的输入法 ID（适用于高版本，可选）
     */
    fun getActiveInputMethodId(context: Context): String? {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                ?: return null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+
                val method: Method =
                    InputMethodManager::class.java.getMethod("getActiveInputMethodId")
                method.invoke(inputMethodManager) as? String
            } else {
                getDefaultInputMethodId(context)
            }
        } catch (_: Exception) {
            getDefaultInputMethodId(context)
        }
    }

    /**
     * 关闭软键盘
     */
    fun hideKeyboard(activity: Activity) {
        try {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // 传入当前获得焦点的 View（通常是 EditText）
        imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }catch (_: Exception){}
    }
}