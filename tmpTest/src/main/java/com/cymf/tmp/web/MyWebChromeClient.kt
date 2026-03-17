package com.cymf.tmp.web

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils

class MyWebChromeClient(private val activity: Activity) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var videoProgressView: View? = null
    private var progressBar: ProgressBar? = null

    // region ==== 进度条相关 ====

    /**
     * 页面加载进度变化时回调（可用于更新进度条）
     */
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        Log.d("MyWebChromeClient", "页面加载进度: $newProgress%")

        progressBar?.progress = newProgress

        if (newProgress == 100) {
            // 加载完成，隐藏进度条
            progressBar?.visibility = View.GONE
        } else {
            // 显示进度条
            progressBar?.visibility = View.VISIBLE
        }
    }

    // endregion

    // region ==== 标题与图标 ====

    /**
     * 页面标题改变时调用
     */
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        Log.d("MyWebChromeClient", "页面标题: $title")
        activity.title = title ?: "WebView"
    }

    /**
     * 页面图标（favicon）改变时调用
     */
    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        Log.d("MyWebChromeClient", "收到新的 favicon")
        // 可将 icon 设置到 ImageView 上显示
    }

    // endregion

    // region ==== 输入框、弹窗相关 ====

    /**
     * 网页请求显示 JavaScript 警告框
     */
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        ToastUtils.showShort(message)
        result?.confirm()
        return true // 表示我们已经处理了这个 alert
    }

    /**
     * 网页请求显示确认框（有“确定”和“取消”按钮）
     */
    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d("MyWebChromeClient", "JS Confirm: $message")
        // 示例：自动选择“确定”
        result?.confirm()
        return true
    }

    /**
     * 网页请求显示输入框（prompt）
     */
    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        Log.d("MyWebChromeClient", "JS Prompt: $message")
        // 示例：自动填写默认值
        result?.confirm("自动回复")
        return true
    }

    // endregion

    // region ==== 文件上传与视频全屏 ====

    /**
     * 网页请求打开文件选择器（如 <input type="file">）
     */
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Log.d("MyWebChromeClient", "网页请求上传文件")
        // 启动系统文件选择器
        // 示例：
        // val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        //     addCategory(Intent.CATEGORY_OPENABLE)
        //     type = "*/*"
        // }
        // activity.startActivityForResult(intent, REQUEST_CODE_FILE_PICKER)
        return true // 表示我们已处理该请求
    }

    /**
     * 全屏视图请求（如 HTML5 视频播放）
     */
    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        Log.d("MyWebChromeClient", "请求进入全屏模式")
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }

        // 保存当前视图，并切换为全屏
        customView = view
        customViewCallback = callback
        // 将 view 添加到你的容器中，例如 FrameLayout
        // containerView.addView(customView)
        // 隐藏 WebView
    }

    /**
     * 退出全屏模式
     */
    override fun onHideCustomView() {
        Log.d("MyWebChromeClient", "退出全屏模式")
        if (customView == null) return

        customView?.visibility = View.GONE
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
        // 显示 WebView 或恢复原有布局
    }

    // endregion

    // region ==== 视频预览与悬浮窗 ====

    /**
     * 获取视频的预览视图（用于视频加载前占位）
     */
    override fun getVideoLoadingProgressView(): View {
        if (videoProgressView == null) {
            val progress = ProgressBar(activity).apply {
                layoutParams = FrameLayout.LayoutParams(-1, -1)
            }
            videoProgressView = progress
        }
        return videoProgressView!!
    }

    // endregion

    // region ==== 实用工具方法 ====

    /**
     * 模拟点击“确定”或“取消”的 JS 弹窗
     */
    private fun autoHandleJsDialog(result: JsResult?, confirm: Boolean = true) {
        if (confirm) {
            result?.confirm()
        } else {
            result?.cancel()
        }
    }

    // endregion

}