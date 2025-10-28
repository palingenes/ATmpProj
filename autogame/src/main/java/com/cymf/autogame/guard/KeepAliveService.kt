package com.cymf.autogame.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cymf.autogame.R
import com.cymf.autogame.utils.YLLogger
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class KeepAliveService : Service() {

    private val channelId = "keepalive_foreground"
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        YLLogger.i("KeepAliveService 服务启动……")

        try {
            createNotificationChannel()
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1001, notification)
            }

            scope.launch {
                delay(10_000)
                // 启动 Root 守护逻辑
                if (isDeviceRootedAndPermitted()) {
                    startNativeWatchdogs()
                    disableBatteryOptimization()
                    adjustOOMScorePeriodically()
                }
            }
        } catch (e: Exception) {
            YLLogger.e("KeepAliveService 服务启动失败", e)
            // 如果启动失败，尝试重启
            restartService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        YLLogger.d("KeepAliveService onStartCommand called")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("保活服务运行中")
            .setContentText("应用保活服务正在后台运行")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "保活服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "应用保活服务通知"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * 重启服务
     */
    private fun restartService() {
        scope.launch {
            delay(5000) // 延迟5秒后重启
            try {
                val intent = Intent(this@KeepAliveService, KeepAliveService::class.java)
                startForegroundService(intent)
                YLLogger.d("KeepAliveService 服务重启成功")
            } catch (e: Exception) {
                YLLogger.e("KeepAliveService 服务重启失败", e)
            }
        }
    }

    /**
     * 检查设备是否 Root 并且有权限执行 root 命令
     */
    private fun isDeviceRootedAndPermitted(): Boolean {
        return try {
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            YLLogger.e("Root 检查失败", e)
            false
        }
    }

    private fun startNativeWatchdogs() {
        val binaryDir = "/data/local/tmp"
        val context = this@KeepAliveService

        val watchdogs = listOf(
            Pair(R.raw.watchdog1, "watchdog1"),
            Pair(R.raw.watchdog2, "watchdog2")
        )
        watchdogs.forEach { (rawResId, destName) ->
            scope.launch(Dispatchers.IO) {
                try {
                    val destPath = "$binaryDir/$destName"
                    val file = File(destPath)
                    if (!file.exists()) {
                        context.resources.openRawResource(rawResId).use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    val result = Shell.cmd("chmod 777 $destPath").exec()
                    if (result.isSuccess) {
                        Shell.cmd(destPath).submit { result ->
                            YLLogger.e("$destName 启动 ${if (result.isSuccess) "成功" else "失败"}")
                        }
                    } else {
                        YLLogger.e("$destName 修改权限失败\n$result")
                    }

                } catch (e: Exception) {
                    YLLogger.e("启动 $destName 出错: \n${e.stackTrace}")
                }
            }
        }
    }

    private fun disableBatteryOptimization() {
        Shell.cmd("pm set-app-standby-bypassed com.cymf.autogame.guard.keepalive true").submit {
            YLLogger.e("是否已关闭电池优化：=${it.isSuccess}")
        }
    }

    private fun adjustOOMScorePeriodically(intervalMillis: Long = 10_000) {
        scope.launch {
            while (isActive) {
                try {
                    setOOMScoreOnce()
                    delay(intervalMillis)
                } catch (e: Exception) {
                    YLLogger.e("KeepAliveService 调整OOM分数失败", e)
                    delay(intervalMillis)
                }
            }
        }
    }

    private fun setOOMScoreOnce(): Boolean {
        val pid = android.os.Process.myPid()
        val command = "echo -1000 > /proc/$pid/oom_score_adj"
        return Shell.cmd(command).exec().isSuccess
    }

    override fun onDestroy() {
        super.onDestroy()
        YLLogger.d("KeepAliveService 服务销毁")
        scope.cancel() // 取消所有协程

        // 尝试重启服务
        try {
            val intent = Intent(this, KeepAliveService::class.java)
            startForegroundService(intent)
        } catch (e: Exception) {
            YLLogger.e("KeepAliveService onDestroy 重启服务失败", e)
        }
    }
}
