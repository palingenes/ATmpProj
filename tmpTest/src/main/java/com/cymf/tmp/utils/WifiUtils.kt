package com.cymf.tmp.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiUtils(private val context: Context) {

    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    // MARK: - 获取当前连接的 Wi-Fi 信息

    /**
     * 获取当前连接的 Wi-Fi 信息（SSID, BSSID, RSSI）
     */
    @SuppressLint("MissingPermission")
    fun getCurrentConnectionInfo(): Map<String, Any?> {
        val info = wifiManager.connectionInfo ?: return emptyMap()

        return mapOf(
            "ssid" to formatSSID(info.ssid),
            "bssid" to info.bssid,
            "rssi" to info.rssi
        )
    }

    private fun formatSSID(ssid: String): String {
        return if (ssid == "<unknown ssid>") "" else ssid.replace("\"", "")
    }

    // MARK: - 开始扫描 Wi-Fi 并获取结果 Flow

    /**
     * 获取周围的 Wi-Fi 列表（Flow 形式）
     * 需要 ACCESS_FINE_LOCATION 权限
     */
    fun startScanAndGetResults(): Flow<List<ScanResult>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val results = wifiManager.scanResults
                    trySend(results).isSuccess
                }
            }
        }
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, filter)
        wifiManager.startScan()
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    // MARK: - 工具方法

    /**
     * 检查是否具有定位权限（扫描 Wi-Fi 需要）
     */
    fun hasLocationPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 请求定位权限（用于扫描 Wi-Fi）
     */
    fun requestLocationPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
        }
    }

    /**
     * 是否开启了 Wi-Fi
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * 强制开启 Wi-Fi
     */
    fun enableWifi(): Boolean {
        return if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled
        } else {
            true
        }
    }
}