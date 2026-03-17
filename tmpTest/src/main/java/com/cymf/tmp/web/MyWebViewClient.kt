package com.cymf.tmp.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class MyWebViewClient : WebViewClient() {

    // region ==== 页面加载控制 ====

    /**
     * 控制页面加载方式，比如是否在 WebView 中打开链接。
     * @return true 表示当前请求被处理；false 表示交给 WebView 默认处理。
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        Log.d("MyWebViewClient", "尝试加载 URL: $url")

        if (url.startsWith("http://") || url.startsWith("https://")) {
            // 继续由 WebView 加载
            return false
        }

        if (url.startsWith("myapp://")) {
            // 拦截自定义协议，执行原生逻辑
            handleCustomProtocol(url)
            return true
        }

        // 其他情况也返回 false，默认行为
        return super.shouldOverrideUrlLoading(view, request)
    }

    // endregion

    // region ==== 页面生命周期回调 ====

    /**
     * 页面开始加载时调用
     */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("MyWebViewClient", "页面开始加载: $url")
        // 可以在这里显示进度条或加载动画
    }

    /**
     * 页面加载完成时调用
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d("MyWebViewClient", "页面加载完成: $url")
        // 可以注入 JS、隐藏进度条等操作
        injectJavaScript(view)
        GlobalScope.launch(Dispatchers.Main) {
            delay(300)
            view?.evaluateJavascript("getCookie('root')") { value -> LogUtils.e(value) }
        }
    }

    // endregion

    // region ==== 资源加载拦截与错误处理 ====

    /**
     * 加载网页资源时触发，可拦截特定资源（如图片、JS 文件）
     */
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
//        val url = request?.url.toString()
//
//        if (   url.contains(".js?v")
//            || url.startsWith("https://cdn")
//            || url.endsWith(".css")
//            || url.endsWith(".png")
//            || url.endsWith(".jpg")
//            || url.endsWith(".js")
//            || url.endsWith(".php")
//            || url.endsWith(".ico")
//        ) {
//            return super.shouldInterceptRequest(view, request)
//        }
//        LogUtils.e(url)
//
//
//        val client = OkHttpClient.Builder()
//            .connectTimeout(10, TimeUnit.SECONDS)
//            .writeTimeout(10, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .build()
//
//        // 构建 OkHttp 请求，注意判空
//        val originalRequest = Request.Builder()
//            .url(url)
//            .method(request?.method ?: "GET", null)
//
//        // 添加请求头（只在非空时添加）
//        request?.requestHeaders?.let {
//            originalRequest.headers(it.toHeaders())
//        }
//
//        // 构建最终请求
//        val builtRequest = originalRequest.build()//        return super.shouldInterceptRequest(view, request)
//        val response = client.newCall(builtRequest).execute()
//        val statusCode = response.code
//        val reasonPhrase = response.message.ifBlank {
//            "OK"
//        }
//
//        if (statusCode in 300..399) {
//            return super.shouldInterceptRequest(view, request)
//        }
//
//        // 解析 Content-Type 和编码
//        val contentType = response.header("Content-Type", "text/html")
//        var mimeType = "text/html"
//        var encoding = "UTF-8"
//
//        if (contentType != null) {
//            val semicolonIndex = contentType.indexOf(';')
//            if (semicolonIndex > 0) {
//                mimeType = contentType.substring(0, semicolonIndex).trim()
//                val charsetPart = contentType.substring(semicolonIndex + 1).trim()
//                if (charsetPart.startsWith("charset=", true)) {
//                    encoding = charsetPart.removePrefix("charset=").trim()
//                }
//            } else {
//                mimeType = contentType.trim()
//            }
//        }
//
//        val sourceBytes = response.body?.bytes() ?: ByteArray(0)
//
//        // 插入 JS 字节流到 <head>
//        val headTag = "<head>".toByteArray(Charsets.ISO_8859_1)
//        val injectedJS = "<script>alert('页面已加载！');</script>\n".toByteArray(Charset.forName(encoding))
//
//        val modifiedBytes = insertBytesBefore(sourceBytes, headTag, injectedJS)
//
//        val modifiedInputStream = ByteArrayInputStream(modifiedBytes)
//
//    return  WebResourceResponse(
//            mimeType,
//            encoding,
//            statusCode,
//            reasonPhrase ?: "OK",
//            mutableMapOf(), // headers 简化处理，如有需要可补充
//            modifiedInputStream
//        )
        return super.shouldInterceptRequest(view, request)
        //        return createEmptyResource()
    }

    /**
     * 创建一个空的资源响应（用于屏蔽某些请求）
     */
    private fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", 200, "OK", null, ByteArrayInputStream(ByteArray(0)))
    }

    private fun insertBytesBefore(data: ByteArray, search: ByteArray, insert: ByteArray): ByteArray {
        val index = indexOf(data, search)
        if (index == -1) {
            return insert + data
        }
        val result = ByteArray(data.size + insert.size)
        System.arraycopy(insert, 0, result, 0, insert.size)
        System.arraycopy(data, 0, result, insert.size, index)
        System.arraycopy(data, index, result, insert.size + index, data.size - index)
        return result
    }

    // 查找子数组位置
    private fun indexOf(data: ByteArray, toFind: ByteArray): Int {
        for (i in data.indices) {
            var match = true
            for (j in toFind.indices) {
                if (i + j >= data.size || data[i + j] != toFind[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    /**
     * 网络加载出错时调用（如 404、500）
     */
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
//        showCustomErrorPage(view, request, error)
    }

    private fun showCustomErrorPage(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        val title = "加载失败"
        var message = when (error?.errorCode) {
            ERROR_HOST_LOOKUP -> "无法解析服务器地址，请检查网络连接。"
            ERROR_CONNECT -> "无法连接到服务器，请稍后再试。"
            ERROR_TIMEOUT -> "请求超时，请检查网络或重试。"
            else -> "其他错误：${error?.errorCode}"
        }
        message += "\nRequest=${request?.url}"

        val encodedTitle = Uri.encode(title)
        val encodedMessage = Uri.encode(message)

        val url = "file:///android_asset/error.html?title=$encodedTitle&message=$encodedMessage"
        view?.loadUrl(url)
    }


    /**
     * 接收 SSL 证书错误
     * ⚠️ 注意：仅用于测试环境，生产环境应根据实际情况处理
     */
    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        Log.w("MyWebViewClient", "SSL 错误: ${error?.primaryError}")
        handler?.proceed() // 接受证书继续加载（不推荐用于正式环境）
    }

    // endregion

    // region ==== 权限与内容拦截 ====

    /**
     * 接收到服务器认证请求（如 Basic Auth）
     */
    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        // 示例：自动填写用户名密码
        handler?.proceed("username", "password")
    }

    /**
     * 接收到重定向时调用
     */
    override fun onFormResubmission(
        view: WebView?,
        dontResend: Message?,
        resend: Message?
    ) {
        Log.d("MyWebViewClient", "表单重复提交")
        super.onFormResubmission(view, dontResend, resend)
    }

    // endregion

    // region ==== 实用方法实现 ====

    /**
     * 自定义协议处理（如 myapp://openSettings）
     */
    private fun handleCustomProtocol(url: String) {
        val uri = url.toUri()
        LogUtils.e(uri)
        when (uri.scheme) {
            "myapp" -> {
                when (uri.host) {
                    "openSettings" -> {
                        // 启动 SettingsActivity
                        // context.startActivity(Intent(context, SettingsActivity::class.java))
                        ToastUtils.showShort("打开设置页面")
                    }

                    "share" -> {
                        val text = uri.getQueryParameter("text")
                        text?.let {
                            ToastUtils.showShort("分享内容: $it")
                        }
                    }
                }
            }
        }
    }

    /**
     * 注入 JS 脚本（例如修改页面样式）
     */
    private fun injectJavaScript(webView: WebView?) {
//        webView?.evaluateJavascript(
//            "(function() { " +
//                    "document.body.style.backgroundColor = 'lightblue'; " +
//                    "})()"
//        ) { result ->
//            Log.d("JSResult", "脚本执行结果: $result")
//        }
    }

    // endregion

}