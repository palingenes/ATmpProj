package com.cymf.autogame.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.cymf.autogame.App
import com.cymf.autogame.ktx.exc
import com.cymf.autogame.service.AssistsService
import com.topjohnwu.superuser.Shell
import kotlin.random.Random

object AppUtil {

    fun getInstalledApps(context: Context): List<PackageInfo> {
        val packageManager = context.packageManager
        val installedPackages = packageManager.getInstalledPackages(0)
        val appList = mutableListOf<PackageInfo>()
        for (packageInfo in installedPackages) {
            if (packageInfo.applicationInfo?.isSystemApp() == true) {
                continue // 跳过系统应用
            }
            appList.add(packageInfo)
        }
        return appList
    }

    // 判断是否是系统应用
    fun ApplicationInfo.isSystemApp(): Boolean {
        return this.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }


    /**
     * 判断指定包名是否已安装
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 判断指定包名是否已安装
     */
    fun getAppInstallVersion(context: Context, packageName: String): Long {
        return try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                context.packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
        } catch (_: PackageManager.NameNotFoundException) {
            -1L
        }
    }

    /**
     * 随机修改所有音量通道
     */
    fun randomAdjustAllVolume() {
        val audioManager = App.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 获取每个通道的最大音量
        val streamTypes = listOf(
            AudioManager.STREAM_VOICE_CALL,     // 通话
            AudioManager.STREAM_SYSTEM,         // 系统
            AudioManager.STREAM_RING,           // 铃声
            AudioManager.STREAM_MUSIC,          // 媒体
            AudioManager.STREAM_ALARM,          // 闹钟
            AudioManager.STREAM_NOTIFICATION,   // 通知
            AudioManager.STREAM_DTMF            // DTMF
        )

        streamTypes.forEach { streamType ->
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val randomVolume = Random.nextInt(0, maxVolume + 1)
            try {
                audioManager.setStreamVolume(streamType, randomVolume, 0)
                YLLogger.e("已将通道 [${streamType.toStreamTypeName()}] 音量设为：$randomVolume / $maxVolume")
            } catch (e: Exception) {
                val sb = StringBuilder()
                sb.append("无法修改通道 [${streamType.toStreamTypeName()}] 的音量 , 错误信息如下：\n")
                e.exc(sb)
                YLLogger.e(sb.toString())
            }
        }
    }

    /**
     * 随机修改屏幕亮度
     */
    fun randomAdjustScreenBrightness() {
        val minBrightness = 0
        val maxBrightness = 255
        val randomBrightness = Random.nextInt(minBrightness, maxBrightness + 1)

        try {
            Settings.System.putInt(
                App.context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                randomBrightness
            )
            YLLogger.e("已将屏幕亮度设为：$randomBrightness / 255")
        } catch (e: Exception) {
            val sb = StringBuilder()
            sb.append("无法修改屏幕亮度 , 错误信息如下：\n")
            e.exc(sb)
            YLLogger.e(sb.toString())
        }
    }


    fun Int.toStreamTypeName(): String {
        return when (this) {
            AudioManager.STREAM_VOICE_CALL -> "通话"
            AudioManager.STREAM_SYSTEM -> "系统"
            AudioManager.STREAM_RING -> "铃声"
            AudioManager.STREAM_MUSIC -> "媒体"
            AudioManager.STREAM_ALARM -> "闹钟"
            AudioManager.STREAM_NOTIFICATION -> "通知"
            AudioManager.STREAM_DTMF -> "DTMF"
            else -> "其他未格式化参数($this)"
        }
    }

    /**
     * 通过已有列表查找 指定包名是否已安装
     */
    fun isAppInstalledByList(context: Context, targetPackageName: String): Boolean {
        val installedApps = getInstalledApps(context)
        return installedApps.any { it.packageName == targetPackageName }
    }

    @SuppressLint("SdCardPath")
    fun getAppSize(packageName: String): Long? {
        return try {
            val apkPath = runCatching {
                Shell.cmd("pm path $packageName").exec().out
                    .firstOrNull { it.contains(":") }
                    ?.split(":", limit = 2)
                    ?.getOrNull(1)
                    ?.trim()
            }.getOrNull()
            if (apkPath.isNullOrBlank()) {
                YLLogger.e("未找到 $packageName 应用")
                return null
            }
            // 获取 APK 文件大小
            val apkSize = getDirSize(apkPath)

            // 获取数据目录大小
            val dataDir = "/data/data/$packageName"
            val dataSize = getDirSize(dataDir)

            apkSize + dataSize
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 封装获取目录大小的函数
    private fun getDirSize(path: String): Long {
        return runCatching {
            var tmp = 0L
            Shell.cmd("du -sk '$path'").exec().out.forEach {
                tmp = it.split("\\s+".toRegex())
                    .getOrNull(0)
                    ?.toLongOrNull()
                    ?: 0L
            }
            tmp * 1024
        }.getOrDefault(0L)
    }

      fun startGooglePlay(packageName: String) {
        val playStoreUri = "market://details?id=$packageName".toUri()
        val intent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
            setPackage("com.android.vending") // 确保使用 Google Play 打开
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 非Activity启动就需要此flag
        }
        if (intent.resolveActivity(App.context.packageManager) != null) {
            AssistsService.instance?.startActivity(intent)
        } else {
            // 如果没有安装 Google Play，尝试使用浏览器打开
            val browserUri = "https://play.google.com/store/apps/details?id=$packageName".toUri()
            AssistsService.instance?.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }
}

