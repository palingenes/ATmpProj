package com.cymf.autogame.guard

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cymf.autogame.utils.YLLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 服务监控器，用于监控和重启 KeepAliveService
 */
object ServiceWatchdog {
    
    private var isMonitoring = false
    private val scope = CoroutineScope(Dispatchers.IO)
    
    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        
        isMonitoring = true
        YLLogger.d("开始监控服务")
        
        scope.launch {
            while (isActive && isMonitoring) {
                try {
                    if (!isServiceRunning(context, KeepAliveService::class.java)) {
                        YLLogger.w("检测到服务已停止，尝试重启")
                        restartService(context)
                    }
                    delay(30_000) // 每30秒检查一次
                } catch (e: Exception) {
                    YLLogger.e("监控服务时出错", e)
                    delay(30_000)
                }
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        YLLogger.d("停止监控服务")
    }
    
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val services = manager.getRunningServices(Integer.MAX_VALUE)
            services.any { it.service.className == serviceClass.name }
        } catch (e: Exception) {
            YLLogger.e("检查服务状态失败", e)
            false
        }
    }
    
    private fun restartService(context: Context) {
        try {
            val intent = Intent(context, KeepAliveService::class.java)
            context.startForegroundService(intent)
            YLLogger.d("服务重启成功")
        } catch (e: Exception) {
            YLLogger.e("重启服务失败", e)
        }
    }
}
