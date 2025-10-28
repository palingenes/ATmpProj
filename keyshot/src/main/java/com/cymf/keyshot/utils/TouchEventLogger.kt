package com.cymf.keyshot.utils

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class TouchEventLogger(private val callback: TouchCallback?) {

    private var process: Process? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var isRunning = false

    // 屏幕尺寸
    private val DISPLAY_WIDTH = DisplayUtil.widthPixels()
    private val DISPLAY_HEIGHT = DisplayUtil.heightPixels()

    // 动态获取的输入设备范围
    private var xMax: Int = 4095 // 默认值
    private var yMax: Int = 4095 // 默认值

    interface TouchCallback {
        fun onDown(x: Float, y: Float, timestamp: Long)
        fun onMove(x: Float, y: Float, timestamp: Long)
        fun onUp(timestamp: Long)
        fun onLog(message: String)
        fun onError(error: String)
    }

    /**
     * 启动监听
     */
    fun start() {
        if (isRunning) return
        isRunning = true

        executor.execute {
            try {
                // 先尝试获取设备范围
                val ranges = getTouchDeviceRanges()
                if (ranges.xMax > 0 && ranges.yMax > 0) {
                    xMax = ranges.xMax
                    yMax = ranges.yMax
                    callback?.onLog("✅ 获取到设备坐标范围: X=0~$xMax, Y=0~$yMax")
                } else {
                    callback?.onLog("⚠️ 未获取到设备范围，使用默认值: X=0~$xMax, Y=0~$yMax")
                }

                // 请求 root 并启动 getevent
                process = Runtime.getRuntime().exec("su")
                val stdin = process!!.outputStream
                val stdout = BufferedReader(InputStreamReader(process!!.inputStream))

                stdin.write("getevent -t -l\n".toByteArray())
                stdin.flush()

                callback?.onLog("✅ getevent 监听已启动")

                var lastX = 0f
                var lastY = 0f
                var isTouching = false

                var line: String? = null
                while (isRunning && (stdout.readLine().also { line = it }) != null) {
                    val trimmed = line!!.trim()
//                    callback?.onLog("Raw: $trimmed")

                    val parsed = parseLine(trimmed) ?: continue

                    val timestamp = parsed.timestamp
                    val device = parsed.device
                    val eventCode = parsed.eventCode
                    val value = parsed.value

                    // 只处理触摸设备
                    if (!device.contains("/dev/input/event")) continue

                    when (eventCode) {
                        "ABS_MT_POSITION_X" -> {
                            lastX = mapCoordinate(value, xMax, DISPLAY_WIDTH)
                        }
                        "ABS_MT_POSITION_Y" -> {
                            lastY = mapCoordinate(value, yMax, DISPLAY_HEIGHT)
                        }
                        "ABS_MT_TRACKING_ID" -> {
                            if (value >= 0) {
                                callback?.onDown(lastX, lastY, timestamp)
                                isTouching = true
                            } else if (isTouching) {
                                callback?.onUp(timestamp)
                                isTouching = false
                            }
                        }
                        "BTN_TOUCH" -> {
                            if (value == 1 && !isTouching) {
                                callback?.onDown(lastX, lastY, timestamp)
                                isTouching = true
                            } else if (value == 0 && isTouching) {
                                callback?.onUp(timestamp)
                                isTouching = false
                            }
                        }
                    }

                    // MOVE 事件
                    if (isTouching && (eventCode == "ABS_MT_POSITION_X" || eventCode == "ABS_MT_POSITION_Y")) {
                        callback?.onMove(lastX, lastY, timestamp)
                    }
                }

                close()
            } catch (e: Exception) {
                if (isRunning) {
                    val errorMsg = "❌ getevent 启动失败: ${e.message}"
                    Log.e("TouchEventLogger", errorMsg, e)
                    callback?.onError(errorMsg)
                }
            }
        }
    }

    /**
     * 停止监听
     */
    fun stop() {
        isRunning = false
        process?.destroy()
        process = null
        callback?.onLog("🛑 getevent 监听已停止")
    }

    /**
     * 映射坐标
     */
    private fun mapCoordinate(value: Int, inputMax: Int, outputMax: Int): Float {
        return if (inputMax > 0) {
            (value.coerceIn(0, inputMax) * outputMax / inputMax).toFloat()
        } else {
            0f
        }
    }

    /**
     * 关闭资源
     */
    private fun close() {
        try {
            process?.destroy()
        } catch (e: Exception) {
            Log.e("TouchEventLogger", "关闭 process 失败", e)
        }
    }

    private fun parseLine(line: String): ParsedEvent? {
        var index = 0
        val length = line.length

        // 1. 跳过空格，期望 [ 开头
        while (index < length && line[index] == ' ') index++
        if (index >= length || line[index] != '[') return null
        index++

        // 2. 找到 ] 结束时间戳
        val endBracket = line.indexOf(']', index)
        if (endBracket == -1) return null
        val timeStr = line.substring(index, endBracket).trim()
        val timestamp = try {
            (timeStr.toDouble() * 1000).toLong()
        } catch (e: NumberFormatException) {
            return null
        }
        index = endBracket + 1

        // 3. 跳空格，找设备路径（/dev/input/eventX:）
        while (index < length && line[index] == ' ') index++
        val deviceStart = index
        val deviceColon = line.indexOf(':', deviceStart)
        if (deviceColon == -1) return null
        val device = line.substring(deviceStart, deviceColon)
        index = deviceColon + 1

        // 4. 跳空格，找事件类型（EV_ABS, EV_KEY, EV_SYN）
        while (index < length && line[index] == ' ') index++
        val typeStart = index
        if (typeStart >= length) return null
        var typeEnd = typeStart
        while (typeEnd < length && line[typeEnd] != ' ' && line[typeEnd] != '\t') typeEnd++
        val eventType = line.substring(typeStart, typeEnd)
        index = typeEnd

        // 5. 跳空格，找事件码（ABS_MT_POSITION_X, BTN_TOUCH 等）
        while (index < length && (line[index] == ' ' || line[index] == '\t')) index++
        val codeStart = index
        if (codeStart >= length) return null
        var codeEnd = codeStart
        while (codeEnd < length && line[codeEnd] != ' ' && line[codeEnd] != '\t') codeEnd++
        val eventCode = line.substring(codeStart, codeEnd)
        index = codeEnd

        // 6. 跳空格，找值（十六进制字符串）
        while (index < length && (line[index] == ' ' || line[index] == '\t')) index++
        val valueStart = index
        if (valueStart >= length) return null
        var valueEnd = valueStart
        while (valueEnd < length && line[valueEnd] != ' ' && line[valueEnd] != '\t' && line[valueEnd] != '\n') valueEnd++
        val valueHex = line.substring(valueStart, valueEnd)

        val value = try {
            if (valueHex == "00000000" || valueHex == "ffffffff") 0
            else Integer.parseInt(valueHex, 16)
        } catch (e: NumberFormatException) {
            return null
        }

        return ParsedEvent(timestamp, device, eventType, eventCode, value)
    }

    // 数据类用于传递解析结果
    private data class ParsedEvent(
        val timestamp: Long,
        val device: String,
        val eventType: String,
        val eventCode: String,
        val value: Int
    )
    // ====================================================================================
    // 动态获取设备范围（使用 getevent -lp）
    // ====================================================================================

    data class DeviceRanges(val xMax: Int, val yMax: Int)

    private fun getTouchDeviceRanges(): DeviceRanges {
        var xMax = 0
        var yMax = 0

        try {
            val process = Runtime.getRuntime().exec("getevent -lp")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            var inAbsSection = false

            while ((reader.readLine().also { line = it }) != null) {
                val trimmed = line?.trim() ?: continue

                // 检查是否进入 ABS 段
                if (trimmed.contains("ABS (0003):")) {
                    inAbsSection = true
                    continue
                }

                // 新设备开始（重置状态）
                if (trimmed.startsWith("add device")) {
                    inAbsSection = false
                    continue
                }

                if (inAbsSection) {
                    // 查找 ABS_MT_POSITION_X 或 Y
                    if (trimmed.contains("ABS_MT_POSITION_X") && trimmed.contains("max")) {
                        val max = parseMaxValue(trimmed)
                        if (max > 0) xMax = max
                    } else if (trimmed.contains("ABS_MT_POSITION_Y") && trimmed.contains("max")) {
                        val max = parseMaxValue(trimmed)
                        if (max > 0) yMax = max
                    }
                }
            }

            reader.close()
            process.destroy()

        } catch (e: Exception) {
            Log.e("TouchEventLogger", "获取设备范围失败", e)
        }

        // 如果没获取到，返回 0，调用方会使用默认值
        return DeviceRanges(if (xMax > 0) xMax else 4095, if (yMax > 0) yMax else 4095)
    }

    /**
     * 从一行文本中提取 "max XXX" 的值
     */
    private fun parseMaxValue(line: String): Int {
        val maxIndex = line.indexOf("max")
        if (maxIndex == -1) return 0
        var start = maxIndex + 3 // 跳过 "max"
        while (start < line.length && line[start] == ' ') start++
        if (start >= line.length) return 0

        var end = start
        while (end < line.length && line[end] in '0'..'9') end++
        if (end == start) return 0

        return try {
            line.substring(start, end).toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }
}