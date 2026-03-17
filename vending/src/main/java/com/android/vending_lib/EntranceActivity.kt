package com.android.vending_lib

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 伪装的 Google play页面，只是用来接收跳转，之后就可以关闭
 */
class EntranceActivity : AppCompatActivity() {

    private val tv by lazy { findViewById<TextView>(R.id.tv) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entrance)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val sb = StringBuilder()

        sb.append("<p>🟢 <b>收到新Intent</b></p>")
        sb.append("<p><font color='#6c757d'>───────────────────────────────────</font></p>")

        val action = intent.action ?: "null"
        sb.append("<p>📌 <b>Action:</b> <code>$action</code></p>")

        val data: Uri? = intent.data
        sb.append("<p>🔗 <b>Data:</b> ")
        if (data != null) {
            sb.append("<font color='#0056b3'>$data</font>")
        } else {
            sb.append("<font color='#6c757d'>&lt;null&gt;</font>")
        }
        sb.append("</p>")

        if (Intent.ACTION_VIEW == action && data != null) {
            sb.append("<p>✅ <b>Source:</b> <font color='#28a745'>DeepLink / URL Scheme</font></p>")
            sb.append("<p>• Scheme: <b>${data.scheme}</b><br>")
            sb.append("• Host: <b>${data.host}</b><br>")
            sb.append("• Path: <code>${data.path}</code><br>")
            sb.append("• Query: <code>${data.query ?: "null"}</code></p>")
        } else {
            sb.append("<p>ℹ️  <b>Source:</b> <font color='#fd7e14'>Internal Launch</font></p>")
        }

        val extras = intent.extras
        if (extras != null && extras.keySet().isNotEmpty()) {
            sb.append("<p>📦 <b>Extras:</b></p>")
            for (key in extras.keySet()) {
                val value = extras.get(key)?.toString()?.replace("<", "&lt;")?.replace(">", "&gt;") ?: "null"
                sb.append("<p style='margin-left: 20dp'>• <b>$key</b> = <code>$value</code></p>")
            }
        } else {
            sb.append("<p>📦 <b>Extras:</b> <font color='#6c757d'>&lt;empty&gt;</font></p>")
        }

        sb.append("<p>⚙️  <b>Flags:</b> <code>0x${intent.flags.toString(16)}</code></p>")
        sb.append("<p><font color='#6c757d'>🕒 ${System.currentTimeMillis()}</font></p>")
        sb.append("<p><font color='#6c757d'>───────────────────────────────────</font></p>")

        tv.append(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY))
    }
}
