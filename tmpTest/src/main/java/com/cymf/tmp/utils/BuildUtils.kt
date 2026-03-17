package com.cymf.tmp.utils

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Modifier

object BuildUtils {

    /**
     * 挂起函数获取 Build 所有字段信息
     */
    suspend fun getBuildInfo(): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()

        // 黑名单：不需要处理的字段名或类名
        val BLACKLIST_CLASSES = setOf(
            "VERSION_CODES"
        )

        val BLACKLIST_FIELDS = setOf(
            "classLoader", "Companion", "\$VALUES"
        )

        fun processClass(clazz: Class<*>, prefix: String = "") {
            // 如果当前类在黑名单中，则跳过
            if (BLACKLIST_CLASSES.contains(clazz.simpleName)) return

            for (field in clazz.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) {
                    // 跳过字段黑名单
                    if (BLACKLIST_FIELDS.contains(field.name)) continue

                    try {
                        field.isAccessible = true
                        val value = field.get(null)
                        val key = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"

                        val strValue = when (value) {
                            is Array<*> -> value.contentToString()
                            null -> "null"
                            is String -> if (value.isEmpty()) "null" else value
                            else -> value.toString()
                        }

                        result[key] = strValue
                    } catch (e: Exception) {
                        result["Error: ${field.name}"] = " ${e.message}"
                    }
                }
            }

            // 递归处理嵌套类，但跳过黑名单中的类
            for (innerClass in clazz.classes) {
                if (BLACKLIST_CLASSES.contains(innerClass.simpleName)) continue
                processClass(innerClass, "${clazz.simpleName}.${innerClass.simpleName}")
            }
        }

        processClass(Build::class.java)
        result
    }
}