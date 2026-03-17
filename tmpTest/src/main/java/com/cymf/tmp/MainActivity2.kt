package com.cymf.tmp

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.StatFs
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity2 : AppCompatActivity(), View.OnClickListener {

    private val tv by lazy { findViewById<TextView>(R.id.tv_text) }
    private var lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        findViewById<Button>(R.id.btn_1).setOnClickListener(this)
        findViewById<Button>(R.id.btn_2).setOnClickListener(this)
        findViewById<Button>(R.id.btn_3).setOnClickListener(this)
        findViewById<Button>(R.id.btn_4).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val currentClickTime = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime < 500) {
            return
        }
        tv.text = null

        val aa = when (v?.id) {
            R.id.btn_1 -> getStorageInfo("/data")
            R.id.btn_2 -> getStorageInfo(noBackupFilesDir)
            R.id.btn_3 -> getStorageInfo("/storage/emulated/0")
            R.id.btn_4 -> getStorageInfo(getExternalFilesDir(null))
            else -> null
        }
        val sb = StringBuilder()
        aa?.forEach {
            sb.append(it.key).append(" : ").append(it.value).append("\n")
        }
        tv.text = sb
    }

    /**
     * 获取指定路径的存储信息（重载方法 1：接受 String 类型路径）
     */
    fun getStorageInfo(path: String): Map<String, String> {
        return getStorageInfo(File(path))
    }

    /**
     * 获取指定路径的存储信息（重载方法 2：接受 File 类型对象）
     */
    fun getStorageInfo(file: File?): Map<String, String> {
        try {
            if (file == null) return emptyMap()
            if (!file.exists()) {
                throw IllegalArgumentException("路径不存在：${file.absolutePath}")
            }
            val stat = StatFs(file.absolutePath)

            val blockSize: Long = stat.blockSizeLong
            val totalBlocks: Long = stat.blockCountLong
            val freeBlocks: Long = stat.freeBlocksLong
            val availableBlocks: Long = stat.availableBlocksLong

            val totalSpace = totalBlocks * blockSize
            val freeSpace = freeBlocks * blockSize
            val availableSpace = availableBlocks * blockSize
            val usedSpace = totalSpace - availableSpace

            return mapOf(
                "PATH" to file.absolutePath,
                "blockSize" to "$blockSize(bytes)",
                "totalSpace" to formatFileSize(totalSpace),
                "freeSpace" to formatFileSize(freeSpace),
                "availableSpace" to formatFileSize(availableSpace),
                "usedSpace" to formatFileSize(usedSpace)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyMap()
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var index = 0
        var bytes = size.toDouble()

        while (bytes >= 1024 && index < units.size - 1) {
            bytes /= 1024
            index++
        }

        return String.format("%.2f %s", bytes, units[index])
    }
}