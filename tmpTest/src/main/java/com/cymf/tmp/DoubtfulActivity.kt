package com.cymf.tmp

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetAddress
import java.net.NetworkInterface

@SuppressLint("SetTextI18n")
class DoubtfulActivity : AppCompatActivity(), View.OnClickListener {

    private val tv by lazy { findViewById<TextView>(R.id.tv_text) }
    private var lastClickTime: Long = 0

    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private val bluetoothManager by lazy { getSystemService(BLUETOOTH_SERVICE) as BluetoothManager }
    private val activityManager by lazy { getSystemService(ACTIVITY_SERVICE) as ActivityManager }
    private val cameraManager by lazy { getSystemService(CAMERA_SERVICE) as CameraManager }
    val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }
    private var sb1 = StringBuffer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doubtful)
        findViewById<Button>(R.id.btn_1).setOnClickListener(this)
        findViewById<Button>(R.id.btn_2).setOnClickListener(this)
        findViewById<Button>(R.id.btn_3).setOnClickListener(this)
        findViewById<Button>(R.id.btn_4).setOnClickListener(this)
        findViewById<Button>(R.id.btn_5).setOnClickListener(this)
        findViewById<Button>(R.id.btn_6).setOnClickListener(this)
        findViewById<Button>(R.id.btn_7).setOnClickListener(this)
        findViewById<Button>(R.id.btn_8).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val currentClickTime = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime < 500) {
            return
        }
        lifecycleScope.launch {
            tv.text = " 开始加载……"
            delay(500) //  防止感觉没有重新触发该方法
            when (v?.id) {
                R.id.btn_1 -> getAndDisplayAllAudioDevices()
                R.id.btn_2 -> getBluetoothAdapter()
                R.id.btn_3 -> getMyApplicationInfo()
                R.id.btn_4 -> printAllNetworkInterfaces()
                R.id.btn_5 -> getAndPrintDeviceConfigInfo()
                R.id.btn_6 -> printAllCameraInfo()
                R.id.btn_7 -> getThermal()
                R.id.btn_8 -> tv.text = " 啥也没有……"
            }
        }
    }

    val onThermalStatusChangedListener = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager.OnThermalStatusChangedListener { status ->
                sb1.append("热区信息= ")
                sb1.append(thermalText(status)).append("\n")
            }
        } else {
            null
        }
    } catch (e: Exception) {
        e
    } catch (e: Error) {
        e
    }

    private fun thermalText(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "无(NONE)"
        PowerManager.THERMAL_STATUS_LIGHT -> "轻度(LIGHT)"
        PowerManager.THERMAL_STATUS_MODERATE -> "中等(MODERATE)"
        PowerManager.THERMAL_STATUS_SEVERE -> "严重(SEVERE)"
        PowerManager.THERMAL_STATUS_CRITICAL -> "危急(CRITICAL)"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "紧急(EMERGENCY)"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "关闭(SHUTDOWN)"
        else -> "未知($status)"
    }

    private fun getThermal() {
        sb1 = StringBuffer()
        try {
            sb1.append("THERMAL_SERVICE 已被隐藏").append("\n")
            sb1.append("源码中没有ThermalManager").append("\n")
            // https://developer.android.com/games/optimize/adpf/thermal?hl=zh-cn#java
            sb1.append("Android 官方文档也是通过powerManager获取，但是有c++获取方式").append("\n")
            sb1.append("使用powerManager.addThermalStatusListener获取热区信息").append("\n")

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                sb1.append("当前版本小于SDK_INT 29").append("\n")
            } else {
                val thermalHeadroom = powerManager.getThermalHeadroom(10)
                sb1.append("提前十秒获取温度：${thermalHeadroom}").append("\n")

                val thermalStatus = powerManager.currentThermalStatus
                sb1.append("当前热状态：${thermalText(thermalStatus)}").append("\n")

                onThermalStatusChangedListener?.let {
                    when (onThermalStatusChangedListener) {
                        is Error -> exc(onThermalStatusChangedListener, sb1)
                        is Exception -> exc(onThermalStatusChangedListener, sb1)
                        else -> powerManager.addThermalStatusListener(it as PowerManager.OnThermalStatusChangedListener)
                    }
                }
            }
        } catch (e: Exception) {
            exc(e, sb1)
        }
        tv.text = sb1.toString()
    }


    fun printAllCameraInfo() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sb = StringBuilder()
            try {
                val cameraIdList = cameraManager.cameraIdList
                if (cameraIdList.isEmpty()) {
                    sb.append("没有检测到摄像头").append("\n")
                } else {
                    cameraIdList.forEach {
                        sb.append("摄像头 ID: $it").append("\n")
                        val characteristics = cameraManager.getCameraCharacteristics(it)
                        val facing = when (characteristics[CameraCharacteristics.LENS_FACING]) {
                            CameraCharacteristics.LENS_FACING_FRONT -> "前置"
                            CameraCharacteristics.LENS_FACING_BACK -> "后置"
                            CameraCharacteristics.LENS_FACING_EXTERNAL -> "外置"
                            else -> "未知"
                        }
                        sb.append("  方向: $facing").append("\n")

                        val orientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION]
                        sb.append("  传感器旋转角度: $orientation°").append("\n")

                        val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                        sb.append("  是否有闪光灯: ${flashAvailable ?: false}").append("\n")

                        val controlModes = characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]
                        sb.append("  支持的 AE 模式: ${controlModes?.contentToString() ?: "无"}").append("\n")

                        val effects = characteristics[CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS]
                        sb.append("  支持的效果模式: ${effects?.contentToString() ?: "无"}").append("\n")

                        val resolutions = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        if (resolutions != null) {
                            val sizes = resolutions.getOutputSizes(SurfaceTexture::class.java)
                            sb.append("  支持的最大预览分辨率: ${sizes?.maxByOrNull { it.width * it.height } ?: "未知"}").append("\n")
                        }
                        sb.append("----------------------------------------\n")
                    }
                }
            } catch (e: Exception) {
                exc(e, sb)
            }
            withContext(Dispatchers.Main) {
                tv.text = sb.toString()
            }
        }

    }

    fun getAndPrintDeviceConfigInfo() {
        val sb = StringBuilder()
        try {
            val configInfo = activityManager.deviceConfigurationInfo
            sb.append("OpenGL ES 版本: ${configInfo.glEsVersion}\n")
            sb.append("SDK 版本: ${configInfo.reqGlEsVersion}\n")
            sb.append("reqNavigation: ${configInfo.reqNavigation}\n")
            sb.append("reqInputFeatures: ${configInfo.reqInputFeatures}\n")
            sb.append("reqKeyboardType: ${configInfo.reqKeyboardType}\n")
            sb.append("reqTouchScreen: ${configInfo.reqTouchScreen}\n")
        } catch (e: Exception) {
            exc(e, sb)
        }
        tv.text = sb.toString()
    }


    fun printAllNetworkInterfaces() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sb = StringBuilder()
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                if (networkInterfaces == null) {
                    sb.append("无法获取网络接口信息\n")
                } else {
                    for (networkInterface in networkInterfaces) {
                        sb.append("接口名称: ${networkInterface.name}\n")
                        sb.append("显示名称: ${networkInterface.displayName}\n")
                        sb.append("是否启用: ${networkInterface.isUp}\n")
                        sb.append("是否是虚拟接口: ${networkInterface.isVirtual}\n")
                        sb.append("是否支持多播: ${networkInterface.supportsMulticast()}\n")
                        sb.append("MTU（最大传输单元）: ${networkInterface.mtu}\n")

                        // 输出绑定的 IP 地址
                        sb.append("绑定的 IP 地址:\n")
                        for (inetAddress in networkInterface.inetAddresses.asSequence()) {
                            sb.append("  - ${formatInetAddress(inetAddress)}\n")
                        }

                        // 输出子接口（如果有）
                        val subInterfaces = networkInterface.subInterfaces.toList()
                        if (subInterfaces.isNotEmpty()) {
                            sb.append("子接口:\n")
                            for (subIf in subInterfaces) {
                                sb.append("  - ${subIf.name}: ${subIf.displayName}\n")
                            }
                        }
                        sb.append("\n----------------------------------------\n\n")
                    }
                }

            } catch (e: Exception) {
                exc(e, sb)
            }
            withContext(Dispatchers.Main) {
                tv.text = sb.toString()
            }
        }
    }

    // 格式化 InetAddress（显示 IP + 主机名）
    private fun formatInetAddress(address: InetAddress): String {
        return "${address.hostAddress} (${address.hostName})"
    }

    @Suppress("DEPRECATION")
    private fun getMyApplicationInfo() {
        showInputDialog { input ->
            val flags = PackageManager.GET_META_DATA or
                    PackageManager.GET_SHARED_LIBRARY_FILES or
                    PackageManager.MATCH_UNINSTALLED_PACKAGES or
                    PackageManager.GET_CONFIGURATIONS or
                    PackageManager.GET_DISABLED_COMPONENTS or
                    PackageManager.GET_PROVIDERS or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_PERMISSIONS
            getAndDisplayApplicationInfo(input, flags)
        }
    }

    fun getAndDisplayApplicationInfo(packageName: String, flags: Int) {
        val pm = packageManager
        try {
            val appInfo = pm.getApplicationInfo(packageName, flags)

            val info = """
            Package Name: ${appInfo.packageName}
            Source Dir: ${appInfo.sourceDir}
            Public Source Dir: ${appInfo.publicSourceDir}
            Data Dir: ${appInfo.dataDir}
            UID: ${appInfo.uid}
            Flags: ${appInfo.flags}
            taskAffinity: ${appInfo.taskAffinity}
            processName: ${appInfo.processName}
            descriptionRes: ${appInfo.descriptionRes}
            manageSpaceActivityName: ${appInfo.manageSpaceActivityName}
            className: ${appInfo.className}
            backupAgentName: ${appInfo.backupAgentName}
            Theme: ${appInfo.theme}
            NativeLibraryDir: ${appInfo.nativeLibraryDir ?: "null"}
            nativeHeapZeroInitialized: ${
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    appInfo.nativeHeapZeroInitialized
                } else "null"
            }
            enabled: ${appInfo.enabled}
            SeInfo: ${
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    appInfo.areAttributionsUserVisible()
                } else "null"
            }
            permission：${appInfo.permission}
        """.trimIndent()
            tv.text = info
        } catch (e: PackageManager.NameNotFoundException) {
            val sb = StringBuilder("找不到包：$packageName\n")
            exc(e, sb)
            tv.text = sb.toString()
        }
    }

    private fun exc(e: Exception, sb: StringBuffer) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        stringWriter.append(stringWriter.toString()).append("\n")
        sb.append(stringWriter)
    }

    private fun exc(e: Error, sb: StringBuffer) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        stringWriter.append(stringWriter.toString()).append("\n")
        sb.append(stringWriter)
    }

    private fun exc(e: Exception, sb: StringBuilder) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        stringWriter.append(stringWriter.toString()).append("\n")
        sb.append(stringWriter)
    }

    private fun getBluetoothAdapter() {
        val sb = StringBuilder()
        sb.append("仅在清单文件配置以下权限(未主动申请)\n")
        sb.append("ACCESS_FINE_LOCATION\n")
        sb.append("BLUETOOTH\n")
        try {
            sb.append("=== 方式一：BluetoothAdapter.getDefaultAdapter() ===\n")
            getBluetoothInfoUsingLegacy(sb)
            sb.append("\n")
            sb.append("=== 方式二：BluetoothManager.adapter ===\n")
            getBluetoothInfoUsingModern(sb)
        } catch (e: Exception) {
            exc(e, sb)
        }
        tv.text = sb.toString()
    }

    @Suppress("DEPRECATION")
    private fun getBluetoothInfoUsingLegacy(sb: StringBuilder) {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            sb.append("设备不支持蓝牙（getDefaultAdapter 返回 null）\n")
            return
        }
        appendBasicInfo(sb, bluetoothAdapter)
    }

    private fun getBluetoothInfoUsingModern(sb: StringBuilder) {
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            sb.append("设备不支持蓝牙（BluetoothManager.adapter 为 null）\n")
            return
        }
        appendBasicInfo(sb, bluetoothAdapter)
    }

    // 公共方法：打印蓝牙基本信息
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun appendBasicInfo(sb: StringBuilder, adapter: BluetoothAdapter) {
        sb.append("是否启用: ${adapter.isEnabled}\n")
        sb.append("蓝牙名称: ${adapter.name}\n")
        sb.append("蓝牙地址 (MAC): ${adapter.address}\n")
        sb.append("蓝牙状态: ${getStateDescription(adapter.state)}\n")
        sb.append("可被发现模式: ${getScanModeDescription(adapter.scanMode)}\n")
        sb.append("已配对设备数量: ${adapter.bondedDevices.size}\n")

        if (adapter.bondedDevices.isNotEmpty()) {
            sb.append("已配对设备:\n")
            for (device in adapter.bondedDevices) {
                sb.append("  - ${device.name} - ${device.address}\n")
            }
        } else {
            sb.append("无已配对设备\n")
        }
    }

    // 蓝牙状态描述
    private fun getStateDescription(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_OFF -> "关闭"
            BluetoothAdapter.STATE_TURNING_ON -> "正在开启"
            BluetoothAdapter.STATE_ON -> "已开启"
            BluetoothAdapter.STATE_TURNING_OFF -> "正在关闭"
            else -> "未知状态"
        }
    }

    // 可被发现模式描述
    private fun getScanModeDescription(scanMode: Int): String {
        return when (scanMode) {
            BluetoothAdapter.SCAN_MODE_NONE -> "不可被发现"
            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> "可连接，不可被发现"
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> "可连接且可被发现"
            else -> "未知模式"
        }
    }

    // ------------------------------------------------------------------------------------

    private fun getAndDisplayAllAudioDevices() {
        val devices: Array<AudioDeviceInfo> = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS or AudioManager.GET_DEVICES_OUTPUTS)
        val sb = StringBuilder()

        val outputDevices = mutableListOf<AudioDeviceInfo>()
        val inputDevices = mutableListOf<AudioDeviceInfo>()

        for (device in devices) {
            if (device.isSink) outputDevices.add(device)
            if (device.isSource) inputDevices.add(device)
        }

        // 显示输出设备
        sb.append("== 输出设备 (${outputDevices.size}) ==\n\n")
        if (outputDevices.isEmpty()) {
            sb.append("无输出设备\n\n")
        } else {
            for (device in outputDevices) {
                sb.append("名称: ${device.productName}\n")
                sb.append("类型: ${getDeviceTypeName(device.type)}\n")
                sb.append("支持采样率: ${device.sampleRates.joinToString(", ")}\n")
                sb.append("通道掩码: ${device.channelMasks.joinToString(", ")}\n")
                sb.append("编码格式: ${device.encodings.joinToString(", ")}\n")
                sb.append("\n")
            }
        }

        // 显示输入设备
        sb.append("== 输入设备 (${inputDevices.size}) ==\n\n")
        if (inputDevices.isEmpty()) {
            sb.append("无输入设备\n\n")
        } else {
            for (device in inputDevices) {
                sb.append("名称: ${device.productName}\n")
                sb.append("类型: ${getDeviceTypeName(device.type)}\n")
                sb.append("支持采样率: ${device.sampleRates.joinToString(", ")}\n")
                sb.append("通道掩码: ${device.channelMasks.joinToString(", ")}\n")
                sb.append("编码格式: ${device.encodings.joinToString(", ")}\n")
                sb.append("\n")
            }
        }
        tv.text = sb.toString()
    }

    // 根据 AudioDeviceInfo.type 返回可读字符串
    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "听筒 {原类型：$type}"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "(内置)扬声器 {原类型：$type}"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "有线耳机（带麦克风） {原类型：$type}"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "有线耳机（无麦克风） {原类型：$type}"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "蓝牙 SCO 设备（通话） {原类型：$type}"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "蓝牙 A2DP 设备（音乐） {原类型：$type}"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI 输出 {原类型：$type}"
            AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC {原类型：$type}"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB 音频设备（输入） {原类型：$type}"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory 音频设备 {原类型：$type}"
            AudioDeviceInfo.TYPE_DOCK -> "Dock 站 {原类型：$type}"
            AudioDeviceInfo.TYPE_FM -> "FM 收音机 {原类型：$type}"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "(内置)麦克风 {原类型：$type}"
            AudioDeviceInfo.TYPE_BUS -> "音频总线设备 {原类型：$type}"
            AudioDeviceInfo.TYPE_IP -> "网络音频设备 {原类型：$type}"
            else -> "其他类型 ($type)"
        }
    }

    private fun showInputDialog(onInputReceived: (String) -> Unit) {
        val input = android.widget.EditText(this)
        input.hint = "请输入内容"
        input.setText("com.feilong.zaitian")

        AlertDialog.Builder(this)
            .setTitle("输入预期包名")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    onInputReceived(text)
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        onThermalStatusChangedListener?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it is PowerManager.OnThermalStatusChangedListener) {
                powerManager.removeThermalStatusListener(it)
            }
        }
    }
}