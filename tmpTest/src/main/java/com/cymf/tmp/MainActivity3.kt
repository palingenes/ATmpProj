package com.cymf.tmp

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cymf.tmp.utils.AppInfoUtil
import com.cymf.tmp.utils.InputMethodUtil
import com.cymf.tmp.utils.JsonFormatterAndHighlighter
import com.cymf.tmp.utils.LocaleUtils
import com.cymf.tmp.utils.NetworkUtil
import com.cymf.tmp.utils.WifiUtils
import com.cymf.tmp.utils.appendDividerLine
import com.cymf.tmp.utils.appendKey
import com.cymf.tmp.utils.appendLine
import com.cymf.tmp.utils.appendTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n")
class MainActivity3 : AppCompatActivity(), View.OnClickListener {

    private val tv by lazy { findViewById<TextView>(R.id.tv_text) }
    private var lastClickTime: Long = 0

    private val wifiUtils by lazy { WifiUtils(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
        findViewById<Button>(R.id.btn_1).setOnClickListener(this)
        findViewById<Button>(R.id.btn_2).setOnClickListener(this)
        findViewById<Button>(R.id.btn_3).setOnClickListener(this)
        findViewById<Button>(R.id.btn_4).setOnClickListener(this)
        findViewById<Button>(R.id.btn_5).setOnClickListener(this)
        findViewById<Button>(R.id.btn_6).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val currentClickTime = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime < 500) {
            return
        }
        lifecycleScope.launch {
            tv.text = "\n\n\n\n\n\n\n             开始加载……"
            delay(500) //  防止感觉没有重新触发该方法
            when (v?.id) {
                R.id.btn_1 -> getNetInfo()
                R.id.btn_2 -> requestUrl()
                R.id.btn_3 -> getInputMethodInfo()
                R.id.btn_4 -> getISOLanguageInfo()
                R.id.btn_5 -> getPkgInfo()
                R.id.btn_6 -> getWifiInfo()
            }
        }
    }

    private suspend fun getWifiInfo() {
        val span = SpannableStringBuilder()
        span.append("WiFi是否开启：${wifiUtils.isWifiEnabled()}\n")
        span.appendDividerLine("-----------------\n")
        span.appendTitle("当前连接的 Wi-Fi 信息:\n")
        wifiUtils.getCurrentConnectionInfo().forEach {
            span.append("${it.key} : ${it.value}\n")
        }
        span.appendDividerLine("-----------------\n")
        span.appendKey("正在扫描周围的 Wi-Fi 列表……\n")
        tv.text = span
        wifiUtils.startScanAndGetResults().collect { results ->
            span.appendTitle("扫描的WiFi列表（size=${results.size}）:\n")
            for (result in results) {
                span.appendKey("SSID : ${result.SSID}\n")
                span.appendKey("BSSID : ${result.BSSID}\n")
                span.appendKey("RSSI : ${result.level}\n")
                span.appendLine("~~~~~~~~~~~~~~~~~~~~~~~~~~\n")
            }
            tv.text = span
        }
    }

    private fun getPkgInfo() {
        val packageName = this@MainActivity3.packageName
        tv.text = "查询包名：$packageName\n"
        tv.append("-----------------\n")

        val installTime = AppInfoUtil.getAppInstallTime(this@MainActivity3, packageName)
        tv.append("应用安装时间: ${AppInfoUtil.timestampToReadableDate(installTime)}\n")
        tv.append("应用安装时间: $installTime\n")
        tv.append("-----------------\n")

        val updateTime = AppInfoUtil.getAppUpdateTime(this@MainActivity3, packageName)
        tv.append("应用更新时间: ${AppInfoUtil.timestampToReadableDate(updateTime)}\n")
        tv.append("应用更新时间: $updateTime\n")
        tv.append("-----------------\n")

        val installer = AppInfoUtil.getAppInstaller(this@MainActivity3, packageName)
        tv.append("应用安装来源: $installer\n")
    }

    private fun getISOLanguageInfo() {
        val isoLanguages = LocaleUtils.getISOLanguages()
        val span = SpannableStringBuilder()
        span.appendTitle("ISOLanguages(size=${isoLanguages.size}) ：\n")
        isoLanguages.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                if (item.length == 3) {
                    span.appendKey("$item      |      ")
                } else {
                    span.appendKey("$item       |      ")
                }
            } else {
                span.appendKey("${item}\n")
            }
        }
        this.tv.text = span
    }

    private fun getInputMethodInfo() {
        val span = SpannableStringBuilder()
        span.appendTitle("当前激活的输入法 ID：\n")
        span.append("${InputMethodUtil.getActiveInputMethodId(this)}\n")
        span.appendDividerLine("--------------------------------\n")
        span.appendTitle("所有输入法信息列表：\n")
        InputMethodUtil.getAllInputMethods(this).forEach {
            span.appendKey("[+] id: ${it.id}\n")
            span.appendKey("[+++] pkgName: ${it.packageName}\n")
            span.appendKey("[+++] serviceName: ${it.serviceName}\n")
            span.appendKey("[+++] settingsActivity: ${it.settingsActivity}\n")
            span.appendKey("[+++] 是否已启用: ${it.isEnabled}\n")
            span.appendKey("[+++] 是否是默认输入法: ${it.isDefault}\n")
            span.appendLine("~~~~~~~~~~~~~~~~~~~~~~~~~~\n")
        }
        this.tv.text = span
    }


    private fun getNetInfo() {
        tv.text = "getNetworkInterfaces获取IP:\n"
        NetworkUtil.getAllIPAddresses().forEach {
            tv.append("$it\n")
        }
        tv.append("--------------------------------\n")
        tv.append("LinkProperties获取IP：\n")
        NetworkUtil.getCurrentIPAddress(this).forEach {
            tv.append("$it\n")
        }
        tv.append("--------------------------------\n")
        tv.append("cat net/arp获取IP：\n")
        NetworkUtil.readArpTable().forEach {
            tv.append("${it.key} = ${it.value}\n")
        }
        tv.append("--------------------------------\n")
        tv.append("ip route方式获取IP：\n")
        NetworkUtil.getIPAddressByCommand()?.let {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("ifconfig方式获取IP：\n")
        NetworkUtil.getIPAddressByIfconfig()?.let {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("LinkProperties方式获取DNS：\n")
        NetworkUtil.getDnsServers(this).forEach {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("resolv.conf方式获取DNS：\n")
        NetworkUtil.getDnsServersFromResolvConf().forEach {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("prop方式获取DNS：\n")
        NetworkUtil.getDnsServersByGetProp().forEach {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("LinkProperties方式获取网关：\n")
        NetworkUtil.getDefaultGateway(this)?.let {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("ip route方式获取网关：\n")
        NetworkUtil.getDefaultGatewayByIpRoute()?.let {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("route -n方式获取网关：\n")
        NetworkUtil.getDefaultGatewayByRoute()?.let {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
        tv.append("子网掩码：\n")
        NetworkUtil.getSubnetMask()?.let {
            tv.append("$it  \n")
        }
        tv.append("--------------------------------\n")
    }

    private fun requestUrl() {
        lifecycleScope.launch {
            val result = NetworkUtil.httpRequestGet("https://www.baidu.com/")
            JsonFormatterAndHighlighter.formatAndHighlight(result, tv)
        }
    }
}