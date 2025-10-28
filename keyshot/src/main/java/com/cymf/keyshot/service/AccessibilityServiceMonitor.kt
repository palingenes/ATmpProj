package com.cymf.keyshot.service

import android.content.Context
import android.provider.Settings
import com.cymf.keyshot.utils.YLLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 辅助功能服务健康监控器
 * 监控服务状态，记录重启频率，提供服务健康度分析
 */
object AccessibilityServiceMonitor {
    
    private var isMonitoring = false
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 统计数据
    private var totalRestarts = 0
    private var lastRestartTime = 0L
    private var serviceUpTime = 0L
    private var lastConnectedTime = 0L
    
    // 健康度阈值
    private const val HEALTHY_RESTART_INTERVAL = 300_000L // 5分钟内重启超过3次认为不健康
    private const val MAX_RESTARTS_PER_INTERVAL = 3
    
    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        
        isMonitoring = true
        YLLogger.d("AccessibilityServiceMonitor 开始监控辅助功能服务")
        
        scope.launch {
            while (isActive && isMonitoring) {
                try {
                    checkServiceHealth(context)
                    delay(10_000) // 每10秒检查一次
                } catch (e: Exception) {
                    YLLogger.e("AccessibilityServiceMonitor 监控服务时出错", e)
                    delay(10_000)
                }
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        YLLogger.d("AccessibilityServiceMonitor 停止监控辅助功能服务")
    }
    
    /**
     * 记录服务重启
     */
    fun recordServiceRestart() {
        totalRestarts++
        lastRestartTime = System.currentTimeMillis()
        
        YLLogger.w("AccessibilityServiceMonitor 记录服务重启 - 总次数: $totalRestarts")
        
        // 分析重启频率
        analyzeRestartPattern()
    }
    
    /**
     * 记录服务连接
     */
    fun recordServiceConnected() {
        lastConnectedTime = System.currentTimeMillis()
        serviceUpTime = lastConnectedTime
        
        YLLogger.i("AccessibilityServiceMonitor 记录服务连接")
    }
    
    /**
     * 检查服务健康状态
     */
    private fun checkServiceHealth(context: Context) {
        val isServiceEnabled = isAccessibilityServiceEnabled(context)
        val isServiceRunning = AssistsService.isServiceActive()
        
        if (isServiceEnabled && !isServiceRunning) {
            YLLogger.w("AccessibilityServiceMonitor 服务已启用但未运行，可能存在问题")
        }
        
        if (!isServiceEnabled) {
            YLLogger.w("AccessibilityServiceMonitor 辅助功能服务未启用")
        }
        
        // 计算服务运行时间
        if (lastConnectedTime > 0) {
            val upTime = System.currentTimeMillis() - lastConnectedTime
            YLLogger.d("AccessibilityServiceMonitor 服务运行时间: ${upTime / 1000}秒")
        }
    }
    
    /**
     * 分析重启模式
     */
    private fun analyzeRestartPattern() {
        val currentTime = System.currentTimeMillis()
        
        // 检查是否频繁重启
        if (lastRestartTime > 0) {
            val timeSinceLastRestart = currentTime - lastRestartTime
            
            if (timeSinceLastRestart < HEALTHY_RESTART_INTERVAL && 
                totalRestarts % MAX_RESTARTS_PER_INTERVAL == 0) {
                
                YLLogger.e("AccessibilityServiceMonitor",
                    "检测到频繁重启！${HEALTHY_RESTART_INTERVAL/1000}秒内重启${MAX_RESTARTS_PER_INTERVAL}次")
                
                // 可以在这里添加处理逻辑，比如：
                // 1. 通知用户
                // 2. 调整服务配置
                // 3. 暂时停止某些功能
                handleFrequentRestarts()
            }
        }
    }
    
    /**
     * 处理频繁重启情况
     */
    private fun handleFrequentRestarts() {
        YLLogger.w("AccessibilityServiceMonitor 处理频繁重启 - 建议检查:")
        YLLogger.w("AccessibilityServiceMonitor 1. 内存使用情况")
        YLLogger.w("AccessibilityServiceMonitor 2. 是否有内存泄漏")
        YLLogger.w("AccessibilityServiceMonitor 3. 系统资源压力")
        YLLogger.w("AccessibilityServiceMonitor 4. 服务配置是否合理")
        
        // 可以添加自动优化逻辑
        // 比如清理缓存、减少监听器等
    }
    
    /**
     * 检查辅助功能服务是否启用
     */
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val serviceName = "${context.packageName}/${AssistsService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            enabledServices?.contains(serviceName) == true
        } catch (e: Exception) {
            YLLogger.e("AccessibilityServiceMonitor 检查服务状态失败", e)
            false
        }
    }
    
    /**
     * 获取服务健康报告
     */
    fun getHealthReport(): String {
        val currentTime = System.currentTimeMillis()
        val upTime = if (lastConnectedTime > 0) currentTime - lastConnectedTime else 0
        
        return buildString {
            appendLine("=== 辅助功能服务健康报告 ===")
            appendLine("总重启次数: $totalRestarts")
            appendLine("最后重启时间: ${if (lastRestartTime > 0) Date(lastRestartTime) else "无"}")
            appendLine("当前运行时间: ${upTime / 1000}秒")
            appendLine("服务状态: ${if (AssistsService.isServiceActive()) "运行中" else "未运行"}")
            appendLine("监控状态: ${if (isMonitoring) "监控中" else "未监控"}")
        }
    }
}
