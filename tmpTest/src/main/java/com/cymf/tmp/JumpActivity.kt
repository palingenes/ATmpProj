package com.cymf.tmp

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.blankj.utilcode.util.ToastUtils

class JumpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jump)


        // 1. 显式跳转（假设你要跳转到 com.example.targetapp.TargetActivity）
        findViewById<Button>(R.id.btnExplicit).setOnClickListener {
            try {
                val intent = Intent()
                intent.component = ComponentName(
                    "com.android.vending_lib", // 目标包名
                    "com.android.vending_lib.EntranceActivity" // 完整类名
                )
                startActivity(intent)
            } catch (_: Exception) {
                showToast("目标应用未安装或 Activity 不存在")
            }
        }

        // 2. DeepLink - URL Scheme (如 myapp://profile/123)
        findViewById<Button>(R.id.btnDeepLinkScheme).setOnClickListener {
            val uri = "myapp://profile/123".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            launchIntent(intent)
        }

        // 3. App Link - HTTPS (如 https://example.com/profile/123)
        findViewById<Button>(R.id.btnDeepLinkHttp).setOnClickListener {
            val uri = "https://example.com/profile/123".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            launchIntent(intent)
        }

        // 4. 打开网页
        findViewById<Button>(R.id.btnViewWeb).setOnClickListener {
            val uri = "https://www.google.com".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            launchIntent(intent)
        }

        // 5. 跳转到应用商店（当前 App 的详情页）
        findViewById<Button>(R.id.btnAppStore).setOnClickListener {
            val packageName = this.packageName
            val uri = "market://details?id=$packageName".toUri()

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 尝试通过 market:// 协议跳转（最佳体验）
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                return@setOnClickListener
            }

            // Fallback 1: Google Play 网页版
            val googlePlayUrl = "https://play.google.com/store/apps/details?id=$packageName"
            val browserIntent = Intent(Intent.ACTION_VIEW, googlePlayUrl.toUri())
            if (browserIntent.resolveActivity(packageManager) != null) {
                startActivity(browserIntent)
                return@setOnClickListener
            }
            // Fallback 2: 国内常用商店（可选弹出选择器）
            ToastUtils.showLong("可选择其他应用商店，代码我没写~")
        }

        // 6. 打开地图
        findViewById<Button>(R.id.btnMap).setOnClickListener {
            val uri = "geo:39.9,116.4?q=北京".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            launchIntent(intent)
        }

        // 7. 分享文本
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "这是一条分享测试消息")
                putExtra(Intent.EXTRA_SUBJECT, "分享标题")
            }
            startActivity(intent)
        }

        // 8. 弹出选择器（带标题）
        findViewById<Button>(R.id.btnChooser).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "通过选择器分享")
            }
            val chooser = Intent.createChooser(intent, "选择分享方式")
            startActivity(chooser)
        }

        // 9. 打开邮箱
        findViewById<Button>(R.id.btnEmail).setOnClickListener {
            val uri = "mailto:support@example.com?subject=反馈&body=内容".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            launchIntent(intent)
        }

        // 10. 打开系统设置
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }

        // 11. 打开相机（隐式）
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                showToast("未找到相机应用")
            }
        }

        // 12. NEW_TASK（常用于通知栏跳转）
        findViewById<Button>(R.id.btnNewTask).setOnClickListener {
            val uri = "myapp://home".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            launchIntent(intent)
        }

        // 13. CLEAR_TOP + SINGLE_TOP（用于栈内复用）
        findViewById<Button>(R.id.btnClearTop).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        // 14. 带多个 Extras 的跳转（测试参数传递）
        findViewById<Button>(R.id.btnMultipleExtras).setOnClickListener {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.android.vending_lib", // 目标包名
                    "com.android.vending_lib.EntranceActivity" // 完整类名
                )
                putExtra("userId", 12345)
                putExtra("userName", "张三")
                putExtra("isLoggedIn", true)
                putExtra("score", 95.5f)
                putExtra("tags", arrayOf("vip", "premium"))
            }
            launchIntent(intent)
        }
    }

    // 安全启动 Intent（处理未安装情况）
    private fun launchIntent(intent: Intent) {
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("未找到可处理该 Intent 的应用")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}