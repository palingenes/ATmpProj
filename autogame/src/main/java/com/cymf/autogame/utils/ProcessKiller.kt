package com.cymf.autogame.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProcessKiller {

    // 获取指定包名的 Linux 用户名（如 u0_a69）
    private fun getAppUserName(context: Context, packageName: String): String? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val uid = appInfo.uid

            val userId = uid / 100000
            val appId = uid % 10000

            "u${userId}_a$appId"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    // Root 模式下 kill 用户下所有进程
    private fun killAllProcessesByUser(userName: String): Boolean {
        return try {
            // 获取所有属于该用户的进程
            val psCommand = "ps -A -o user,pid,comm"
            val result = Shell.cmd("su", "-c", "sh", "-c", psCommand).enqueue().get()
            if (result.isSuccess && result.out.isNotEmpty()) {
                val pids = mutableListOf<Int>()
                result.out.forEach { it ->
                    if (!it.isNullOrBlank() && it.contains(userName)) {
                        val parts = it.trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            parts[1].toIntOrNull()?.let { pids.add(it) }
                        }
                    }
                }
                if (pids.isEmpty()) {
                    return false
                }
                // 逐一 kill
                for (pid in pids) {
                    val killCmd = "kill -9 $pid"
                    val isSuccess =
                        Shell.cmd("su", "-c", "sh", "-c", killCmd).enqueue().get().isSuccess

                    if (isSuccess) {
                        YLLogger.d("ProcessKiller 成功 kill pid=$pid")
                    } else {
                        YLLogger.e("ProcessKiller 无法 kill pid=$pid")
                    }
                }
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 非 Root 模式下尝试终止后台进程
    private fun killBackgroundProcesses(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 统一入口：杀死指定包名的应用及其所有相关进程
     * 目前确定微信杀不掉！！！！！！！！！！！！！！！！！！！！
     */
    suspend fun killApp(context: Context, packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            val userName = getAppUserName(context, packageName)
            if (userName == null) {
                YLLogger.e("ProcessKiller -> 无法获取包名对应的用户名: $packageName")
                return@withContext false
            }
            YLLogger.d("ProcessKiller -> 准备结束应用: $packageName，用户名: $userName")

            // 强制停止应用(一些流氓软件真不上手段真杀不掉)
            try {
                Shell.cmd("su -c am force-stop $packageName").submit {
                    if (it.isSuccess) {
                        YLLogger.d("ProcessKiller -> 强制停止应用成功，包名=${packageName}")
                    } else {
                        YLLogger.e(
                            "ProcessKiller -> 强制停止应用失败，包名=${packageName}，\n"
                                    + "code=${it.code}, "
                                    + "info=${it.err.joinToString("\n")}"
                        )
                    }
                }
            } catch (e: Exception) {
                YLLogger.e("ProcessKiller -> 强制停止应用失败", e)
            }

            if (Shell.isAppGrantedRoot() == true) {
                YLLogger.d("ProcessKiller -> 正在以 Root 模式结束所有相关进程...")
                if (!killAllProcessesByUser(userName)) {
                    YLLogger.w("ProcessKiller -> 未能完全杀死进程")
                }
            } else {
                YLLogger.d("ProcessKiller", "非 Root 模式，尝试使用标准 API 结束后台进程...")
                if (!killBackgroundProcesses(context, packageName)) {
                    YLLogger.w("ProcessKiller -> 未能完全杀死进程")
                }
            }
            true
        }
}