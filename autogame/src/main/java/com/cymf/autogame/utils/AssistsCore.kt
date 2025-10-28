package com.cymf.autogame.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.os.bundleOf
import com.cymf.autogame.App
import com.cymf.autogame.BuildConfig
import com.cymf.autogame.constant.Constants
import com.cymf.autogame.constant.NodeClassValue
import com.cymf.autogame.service.AssistsService
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.LinkedList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 无障碍服务核心类
 * 提供对AccessibilityService的封装和扩展功能
 */
object AssistsCore {

    private val touchSlop by lazy { ViewConfiguration.get(App.app).scaledTouchSlop }

    /** 当前应用在屏幕中的位置信息缓存 */
    private var appRectInScreen: Rect? = null

    /**
     * 以下是一系列用于快速判断元素类型的扩展函数
     * 通过比对元素的className来判断元素类型
     */

    /**
     * 判断是否为继承viewGroup的布局（如LinearLayout）
     */
    fun AccessibilityNodeInfo.isViewGroupClazz(): Boolean {
        val className = this.className?.toString() ?: return false
        return NodeClassValue.viewGroups.values.contains(className)
    }

    /**
     * 判断是否为单纯的view（如ImageView）
     */
    fun AccessibilityNodeInfo.isViewClazz(): Boolean {
        val className = this.className?.toString() ?: return false
        return !isViewGroup() && (className == NodeClassValue.View || NodeClassValue.views.values.contains(
            className
        ))
    }

    fun AccessibilityNodeInfo.getNodeTypeSimpleName(): String {
        val className = this.className?.toString() ?: return "Unknown"

        if (isViewGroupClazz()) {
            val name = NodeClassValue.viewGroups.entries.find { it.value == className }?.key
                ?: "UnknownViewGroup"
            return name
        }
        val name = NodeClassValue.views.entries.find { it.value == className }?.key ?: "UnknownView"
        return name
    }

    /** 判断元素是否是FrameLayout */
    fun AccessibilityNodeInfo.isFrameLayout(): Boolean {
        return className == NodeClassValue.FrameLayout
    }

    /** 判断元素是否是ViewGroup */
    fun AccessibilityNodeInfo.isViewGroup(): Boolean {
        return className == NodeClassValue.ViewGroup
    }

    /** 判断元素是否是View */
    fun AccessibilityNodeInfo.isView(): Boolean {
        return className == NodeClassValue.View
    }

    /** 判断元素是否是ImageView */
    fun AccessibilityNodeInfo.isImageView(): Boolean {
        return className == NodeClassValue.ImageView
    }

    /** 判断元素是否是TextView */
    fun AccessibilityNodeInfo?.isTextView(): Boolean {
        return this?.className == NodeClassValue.TextView
    }

    /** 判断元素是否是LinearLayout */
    fun AccessibilityNodeInfo.isLinearLayout(): Boolean {
        return className == NodeClassValue.LinearLayout
    }

    /** 判断元素是否是RelativeLayout */
    fun AccessibilityNodeInfo.isRelativeLayout(): Boolean {
        return className == NodeClassValue.RelativeLayout
    }

    fun AccessibilityNodeInfo.isViewPager(): Boolean {
        return className == NodeClassValue.ViewPager
    }

    fun AccessibilityNodeInfo.isViewPager2(): Boolean {
        return className == NodeClassValue.ViewPager2
    }

    fun AccessibilityNodeInfo.isRecyclerView(): Boolean {
        return className == NodeClassValue.RecyclerView || className == NodeClassValue.RecyclerView2
    }

    fun AccessibilityNodeInfo.isListView(): Boolean {
        return className == NodeClassValue.ListView || className == NodeClassValue.AbsListView || className == NodeClassValue.ExpandableListView
    }

    fun AccessibilityNodeInfo.isScrollView(): Boolean {
        return className == NodeClassValue.ScrollView || className == NodeClassValue.NestedScrollView || className == NodeClassValue.NestedScrollView2
    }

    fun AccessibilityNodeInfo.isHorizontalScrollView(): Boolean {
        return className == NodeClassValue.HorizontalScrollView
    }

    /** 判断元素是否是Button */
    fun AccessibilityNodeInfo.isButton(): Boolean {
        return className == NodeClassValue.Button
    }

    /** 判断元素是否是ImageButton */
    fun AccessibilityNodeInfo.isImageButton(): Boolean {
        return className == NodeClassValue.ImageButton
    }

    /** 判断元素是否是EditText */
    fun AccessibilityNodeInfo.isEditText(): Boolean {
        return className == NodeClassValue.EditText
    }

    /**
     * 获取元素的文本内容
     * @return 元素的text属性值，如果为空则返回空字符串
     */
    fun AccessibilityNodeInfo.txt(): String {
        return text?.toString() ?: ""
    }

    /**
     * 获取元素的描述内容
     * @return 元素的contentDescription属性值，如果为空则返回空字符串
     */
    fun AccessibilityNodeInfo.des(): String {
        return contentDescription?.toString() ?: ""
    }

    object PatternConstants {
        val HASH_PATTERN = Regex("^[a-zA-Z0-9]+_[a-fA-F0-9]{40}(_[a-zA-Z0-9_]+)?$")
        val TIMESTAMP_SIZE_PATTERN = Regex("^\\d{13}_\\d+x\\d+$")
        val HEX_COLOR_REGEX = Regex("^[a-fA-F0-9]{6}$")
    }

    /**
     * 打开系统的无障碍服务设置页面
     * 用于引导用户开启无障碍服务
     */
    fun openAccessibilitySetting() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        ActivityUtils.startActivity(intent)
    }

    /**
     * TODO:  检查无障碍服务是否已开启
     * @return true表示服务已开启，false表示服务未开启
     */
//    fun isAccessibilityServiceEnabled(): Boolean {
//        return AssistsService.instance != null
//    }

    /**
     * @return 当前窗口的包名，如果获取失败则返回空字符串
     */
    fun getPackageName(): String {
        return AssistsService.instance?.rootInActiveWindow?.packageName?.toString() ?: ""
    }

    fun getCurrentActivity(): String? {
        val foundLine = Shell.cmd("dumpsys activity top | grep -E 'ACTIVITY'").exec()
            .takeIf { it.isSuccess }?.out?.last()

        val parts = foundLine?.trim()?.split("\\s+".toRegex()) // 按空白符分割
        val activityName = parts?.getOrNull(1) // 第二个部分
        return activityName
    }

    fun findNodeAt(
        x: Float,
        y: Float,
        requireClickable: Boolean = true,
        node: AccessibilityNodeInfo? = AssistsService.instance?.rootInActiveWindow
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        // 基础检查
        if (!node.isVisibleToUser || !node.isEnabled) return null

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // 判断坐标是否在该节点范围内
        if (!bounds.contains(x.toInt(), y.toInt())) {
            return null
        }

        // 先递归查找子节点（子元素优先）
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeAt(x, y, requireClickable, child)
                if (result != null) return result
            }
        }

        // 子节点没找到，检查当前节点
        if (requireClickable) {
            if (node.isClickable
                || node.isLongClickable
                || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
            ) {
                return node
            }
        } else {
            return node
        }
        return null
    }

    /**
     * 通过id查找所有符合条件的元素
     * @param id 元素的资源id
     * @param text 可选的文本过滤条件
     * @return 符合条件的元素列表
     */
    fun findById(id: String, text: String? = null): List<AccessibilityNodeInfo> {
        var nodeInfos = AssistsService.instance?.rootInActiveWindow?.findById(id) ?: arrayListOf()

        nodeInfos = text?.let {
            nodeInfos.filter {
                return@filter it.txt() == text
            }
        } ?: let { nodeInfos }

        return nodeInfos
    }

    /**
     * 在指定元素范围内通过id查找所有符合条件的元素
     * @param id 元素的资源id
     * @return 符合条件的元素列表
     */
    fun AccessibilityNodeInfo?.findById(id: String): List<AccessibilityNodeInfo> {
        if (this == null) return arrayListOf()
        findAccessibilityNodeInfosByViewId(id)?.let {
            return it
        }
        return arrayListOf()
    }

    /**
     * 通过文本内容查找所有符合条件的元素
     * @param text 要查找的文本内容
     * @return 符合条件的元素列表
     */
    fun findByText(text: String): List<AccessibilityNodeInfo> {
        return AssistsService.instance?.rootInActiveWindow?.findByText(text) ?: arrayListOf()
    }


    fun AccessibilityNodeInfo.findTargetImageRect(screenRect: Rect, maxY: Float = 0.15f): Rect? {
        return findTargetImageRectPair(screenRect, maxY).second
    }

    fun AccessibilityNodeInfo.findTargetImageRectPair(
        screenRect: Rect, maxY: Float = 0.15f
    ): Pair<Rect?, Rect?> {
        val rectList = findTargetImageRectList(screenRect, maxY)
        if (rectList.size > 1) {
            return Pair(rectList.first(), rectList.last())
        }
        return Pair(null, rectList.firstOrNull())
    }

    /**
     * 在给定的 AccessibilityNodeInfo 中查找符合条件的 Image 控件
     * 配合插页广告使用
     *
     * @param screenRect 屏幕尺寸 Rect（如 Rect(0, 0, 1920, 1080)）
     * @return 符合条件的 Rect，或 null
     */
    fun AccessibilityNodeInfo.findTargetImageRectList(
        screenRect: Rect, maxY: Float = 0.15f
    ): List<Rect?> {
        val targetYMax = (screenRect.height() * maxY).toInt()
        val candidate: Rect? = null
        val leftFirst: Rect? = null
        val rightmostX = -1

        YLLogger.d("开始查找目标图像区域，屏幕尺寸：$screenRect，Y轴上限：$targetYMax")
        val maybeCloseRectList = mutableListOf<Rect?>()

        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(this)

        // 关闭按钮不会太宽 太高
        val limitWidth = screenRect.width() / 10
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()

            val bounds = Rect().apply { node.getBoundsInScreen(this) }

            if (bounds.left >= 0 && bounds.right <= screenRect.width() && bounds.bottom < targetYMax) {
                YLLogger.v("rect::: $className, $bounds <-> [0, 0, ${screenRect.width()}, $targetYMax]")
            }

            // 判断是否启用
            if (!node.isEnabled) {
                YLLogger.v("跳过未启用的节点：${node.className}, $bounds, ${node.text}")
                node.text?.let { text ->
                    if (text.endsWith("s")) {
                        text.substring(0, text.length - 1).toIntOrNull()?.let { second ->
                            YLLogger.w("⏳可能找到倒计时 等待 $second 秒")
                            return listOf(bounds)
                        }

                    }
                }
                continue
            }

            // 检查 bounds 是否有效
            if (bounds.isEmpty || bounds.width() <= 0 || bounds.height() <= 0) {
                YLLogger.v("无效 bounds，跳过：${node.className} $bounds text=${node.text}")
                continue
            }

            val className = node.className?.toString() ?: "unknown"

            // 类型检查
            if (className == "android.widget.Image" || className == "android.widget.ImageView" || className == "android.view.View") {
                if (bounds.left >= 0 && bounds.right <= screenRect.width() && bounds.bottom < targetYMax) {
                    YLLogger.v("找到候选图像：$className, bounds=$bounds")

                    // 保留最左侧的候选
//                    if (rightmostX == -1) {
//                        leftFirst = bounds
//                    }
                    // 更新最右的候选
                    if (bounds.right > rightmostX && bounds.width() <= limitWidth) {
//                        YLLogger.i("更新为当前最右候选：right=${bounds.right}")
//                        rightmostX = bounds.right
//                        candidate = bounds
                        if (!maybeCloseRectList.contains(bounds)) {
                            maybeCloseRectList.add(bounds)
                        }
                    }
                } else {
//                    bounds.show(MaskView.TYPE_ERROR)
                    YLLogger.v("不在范围内, $className, bounds=$bounds")
                }
            }

            // 将子节点压入栈中（逆序处理，保证顺序一致）
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let { child ->
                    YLLogger.v("index=$i, 发现子节点: ${child.className}, $bounds, ${child.viewIdResourceName}")
                    stack.addLast(child)
                }
            }
        }
        return maybeCloseRectList

//        candidate.also {
//            if (it != null) {
//                it.show(MaskView.TYPE_AREA)
//                YLLogger.d("✅ 成功找到目标图像区域：$it")
//            } else {
//                YLLogger.d("❌ 未找到符合条件的目标图像区域")
//            }
//        }
//        return Pair(leftFirst, candidate)
    }

    fun searchNodeByRect(node: AccessibilityNodeInfo, targetRect: Rect): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // 比较两个矩形是否相同或包含关系，具体逻辑可以根据需求调整
        if (bounds == targetRect || bounds.contains(targetRect)) {
            return node
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = searchNodeByRect(child, targetRect)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * 查找屏幕 Y 轴 70%-100% 区域中的 FrameLayout，
     * 并检查其子 View 中是否至少有 6 个符合要求的 android.view.ViewGroup，
     * 且它们的 content-desc 符合 Hex 颜色格式。
     *
     * @param rootNode 根节点 AccessibilityNodeInfo
     * @param screenHeight 屏幕高度（如 1080）
     * @return 符合条件的 FrameLayout AccessibilityNodeInfo，否则 null
     */
    fun findQualifiedFrameLayoutParent(
        rootNode: AccessibilityNodeInfo?, screenHeight: Int
    ): AccessibilityNodeInfo? {
        val yMin = (screenHeight * 0.7).toInt()
        val yMax = screenHeight

        // 常见的 ViewGroup 类型集合（缓存避免重复判断）
        val viewGroupClasses = setOf(
            "android.view.ViewGroup",
            "android.widget.LinearLayout",
            "android.widget.RelativeLayout",
            "android.widget.FrameLayout",
            "androidx.constraintlayout.widget.ConstraintLayout"
        )

        fun AccessibilityNodeInfo.isInTargetYArea(): Boolean {
            val bounds = Rect().apply { getBoundsInScreen(this) }
            val centerY = (bounds.top + bounds.bottom) / 2
            return centerY in yMin..yMax
        }

        // 遍历查找所有 FrameLayout
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node?.className == "android.widget.FrameLayout" && node.isInTargetYArea()) {
                candidates.add(node)
                return  // 找到 FrameLayout 后不再继续深入遍历子节点
            }
            for (i in 0 until (node?.childCount ?: 0)) {
                node?.getChild(i)?.let { child ->
                    traverse(child)
                }
            }
        }
        traverse(rootNode)

        // 遍历候选的 FrameLayout，检查子 View 是否满足条件
        for (frameLayout in candidates) {
            var count = 0
            for (i in 0 until frameLayout.childCount) {
                val child = frameLayout.getChild(i) ?: continue
                // 必须是 ViewGroup 类型
                if (child.className !in viewGroupClasses) continue
                // 必须有 content-desc 且匹配颜色格式
                val contentDesc = child.contentDescription?.toString()?.trim() ?: continue
                if (!PatternConstants.HEX_COLOR_REGEX.matches(contentDesc)) continue
                count++
                if (count >= 6) break // 达到目标数量即可停止
            }
            if (count >= 6) {
                return frameLayout  // 返回当前 FrameLayout 父节点
            }
        }
        return null
    }

    /**
     * 递归查找符合条件的文本，并根据匹配情况返回相应的整数值
     * 哈希格式则返回1，时间戳+尺寸格式返回2
     * 没找到返回-1
     */
    fun AccessibilityNodeInfo.findAndClassifyTextNodes(): Int {
        // 检查当前节点是否是 Image 或 ImageView 类型
        if (this.className == "android.widget.Image" || this.className == "android.widget.ImageView") {
            val text = this.text?.toString() ?: this.contentDescription?.toString()
            text?.let {
                val result = classifyStringToCode(it)
                if (result != -1) return result // 如果找到匹配项，立即返回
            }
        }

        // 递归检查子节点
        for (i in 0 until childCount) {
            getChild(i)?.findAndClassifyTextNodes()?.let { result ->
                if (result != -1) return result // 如果子节点中有匹配项，立即返回
            }
        }

        return -1 // 没有找到匹配项
    }

    /**
     * 尝试找出当前页面的可点击节点。
     * 如果当前页面有 scrollable 节点但无 clickable 节点，会尝试启用 skipScrollView=false 查找。
     *
     * @param maxPollingCount 最大轮询次数
     * @return 找到的可点击节点列表
     */
    suspend fun tryFindClickableNodes(maxPollingCount: Int = 6): List<AccessibilityNodeInfo> {
        var pollingCount = 0
        var result = listOf<AccessibilityNodeInfo>()
        val rootNode = AssistsService.instance?.rootInActiveWindow ?: return emptyList()
        // 先判断是否当前页面存在 scrollable 节点
        val hasScrollableView = rootNode.findAllScrollableNodes().isNotEmpty()
        while (pollingCount < maxPollingCount) {
            delay(1000)
            pollingCount++
            // 先尝试使用 skipScrollView = true 查找
            result = rootNode.findAllClickableNodes(skipScrollView = true)
            if (result.isNotEmpty()) {
                YLLogger.d("找到可点击节点，继续执行")
                break
            }
            // 如果没找到，并且当前页面有 scrollable 容器，尝试使用 skipScrollView = false
            if (hasScrollableView) {
                YLLogger.d("未找到可点击节点，但发现当前页面有可滚动容器，尝试启用 skipScrollView=false")
                result = rootNode.findAllClickableNodes(skipScrollView = false)
                if (result.isNotEmpty()) {
                    YLLogger.d("使用 skipScrollView=false 找到可点击节点")
                    break
                }
            }
        }
        if (result.isEmpty()) {
            YLLogger.d("经过 $pollingCount 次轮询仍未找到可点击节点")
        }
        return result
    }

    /**
     * 搜索当前节点及其子节点中所有 clickable = true 的节点
     * @param skipScrollView 是否跳过所有可滑动的节点（默认为 true）
     */
    suspend fun AccessibilityNodeInfo?.findAllClickableNodes(skipScrollView: Boolean = true): List<AccessibilityNodeInfo> =
        suspendCancellableCoroutine { cont ->

            val result = mutableListOf<AccessibilityNodeInfo>()
            if (this == null) {
                cont.resume(emptyList()) { cause, _, _ -> null }
                return@suspendCancellableCoroutine
            }
            val queue = LinkedList<AccessibilityNodeInfo>()
            queue.add(this)

            while (queue.isNotEmpty()) {
                val current = queue.poll() ?: continue

                if (current.isClickable && current.isVisibleToUser) {
                    if (skipScrollView && current.isScrollable) {
                        continue
                    } else if (!skipScrollView) {
                        result.add(current)
                    } else {
                        result.add(current)
                    }
                }
                // 将可见子节点加入队列
                for (i in 0 until current.childCount) {
                    current.getChild(i)?.let { child ->
                        // 子节点必须可见
                        if (!child.isVisibleToUser) return@let
                        // 如果 skipScrollView 为 true 且子节点是 scrollable，则不加入队列
                        if (skipScrollView && child.isScrollable) return@let
                        // 所有条件通过，才入队
                        queue.add(child)
                    }
                }
            }
            cont.resume(result) { cause, _, _ -> null }
        }

    /**
     * 查找当前节点及其子节点中所有 isScrollable == true 的节点
     */
    suspend fun AccessibilityNodeInfo?.findAllScrollableNodes(): List<AccessibilityNodeInfo> =
        suspendCancellableCoroutine { cont ->
            val result = mutableListOf<AccessibilityNodeInfo>()
            if (this == null) {
                cont.resume(emptyList()) { cause, _, _ -> null }
                return@suspendCancellableCoroutine
            }
            val queue = LinkedList<AccessibilityNodeInfo>()
            queue.add(this)

            while (queue.isNotEmpty()) {
                val current = queue.poll() ?: continue
                if (current.isVisibleToUser && current.isScrollable) {
                    result.add(current)
                }
                for (i in 0 until current.childCount) {
                    current.getChild(i)?.let { child ->
                        if (child.isVisibleToUser) {
                            queue.add(child)
                        }
                    }
                }
            }
            cont.resume(result) { cause, _, _ -> null }
        }

    /**
     * 挂起函数：查找可滚动容器下的所有非空 TextView 节点
     */
    suspend fun AccessibilityService.findNonEmptyTextViewsInScrollableView(): List<AccessibilityNodeInfo> =
        suspendCancellableCoroutine { continuation ->

            val root = rootInActiveWindow ?: return@suspendCancellableCoroutine continuation.resume(
                emptyList()
            ) { cause, _, _ -> null }

            val result = mutableListOf<AccessibilityNodeInfo>()
            val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()

            queue.add(root)

            while (queue.isNotEmpty()) {
                val node = queue.poll() ?: continue

                // 如果是可滚动容器，继续深入其子节点
                if (node.isScrollable) {
                    for (i in 0 until node.childCount) {
                        val child = node.getChild(i) ?: continue
                        queue.add(child)
                    }
                    node.recycle()
                    continue
                }
                // 判断是否为 TextView 并且文本不为空
                if ("android.widget.TextView" == node.className.toString()) {
                    val text = node.text?.toString()?.trim()
                    if (!text.isNullOrEmpty()) {
                        result.add(node) // 加入结果（注意不要 recycle，因为要返回）
                    } else {
                        node.recycle()
                    }
                } else {
                    // 非 TextView 继续深入查找
                    for (i in 0 until node.childCount) {
                        val child = node.getChild(i) ?: continue
                        queue.add(child)
                    }
                    node.recycle()
                }
            }
            continuation.resume(result) { cause, _, _ -> null }
        }

    /**
     * 根据字符串内容分类并返回对应的代码
     */
    private fun classifyStringToCode(input: String): Int {
        return when {
            PatternConstants.HASH_PATTERN.matches(input) -> 1
            PatternConstants.TIMESTAMP_SIZE_PATTERN.matches(input) -> 2
            else -> -1
        }
    }

    fun findByTextMultiLangOpt(
        keywords: List<String>, isFindFirst: Boolean = true
    ): List<AccessibilityNodeInfo> {
        return AssistsService.instance?.rootInActiveWindow?.findByTextMultiLangOpt(
            keywords, isFindFirst
        ) ?: emptyList()
    }

    /**
     * 在指定元素范围内通过多语言关键词查找所有符合条件的元素（高性能优化版）
     * @param keywords 多语言关键词列表
     * @return 符合条件的元素列表
     */
    fun AccessibilityNodeInfo?.findByTextMultiLangOpt(
        keywords: List<String>, isFindFirst: Boolean = true
    ): List<AccessibilityNodeInfo> {
        if (this == null) return emptyList()

        val matchedNodes = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(this)
        // 复用对象，减少 GC 压力
        val tempKeywords = keywords.map { it.trim().lowercase() }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            try {
                // 检查是否有效节点
                if (!current.isVisibleToUser) {
                    current.recycle()
                    continue
                }
                val text = current.text?.toString()?.trim() ?: ""
                // 匹配逻辑：忽略大小写 + 白空格处理
                if (text.isNotBlank() && tempKeywords.any { it == text.lowercase() }) {
                    matchedNodes.add(current)
                    if (isFindFirst) break
                }
                // 将子节点加入队列继续遍历
                for (i in 0 until current.childCount) {
                    current.getChild(i)?.let { queue.add(it) }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 若非最终结果，及时回收节点资源（视设备而定）
                // 注意：有些设备不建议主动 recycle，慎用
//            if (!matchedNodes.contains(current)) {
//                current.recycle()
//            }
            }
        }
        return matchedNodes
    }

    fun findByTextMultiLangOptByText(
        keywords: List<String>, isFindFirst: Boolean = true
    ): List<AccessibilityNodeInfo> {
        return AssistsService.instance?.rootInActiveWindow?.findByTextMultiLangOpt(
            keywords, isFindFirst
        ) ?: emptyList()
    }

    fun AccessibilityNodeInfo?.findByTextMultiLangOptByText(
        keywords: List<String>, isFindFirst: Boolean = true
    ): List<AccessibilityNodeInfo> {
        if (this == null) return emptyList()

        val matchedNodes = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(this)
        // 复用对象，减少 GC 压力
        val tempKeywords = keywords.map { it.trim().lowercase() }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            try {
                // 检查是否有效节点
                if (!current.isVisibleToUser) {
                    current.recycle()
                    continue
                }
                if (!NodeClassValue.TextView.equals(current.className)) {
                    continue
                }
                val text = current.text?.toString()?.trim() ?: ""
                // 匹配逻辑：忽略大小写 + 白空格处理
                if (text.isNotBlank() && tempKeywords.any { it == text.lowercase() }) {
                    matchedNodes.add(current)
                    if (isFindFirst) break
                }
                // 将子节点加入队列继续遍历
                for (i in 0 until current.childCount) {
                    current.getChild(i)?.let { queue.add(it) }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
            }
        }
        return matchedNodes
    }

    /**
     * 查找所有文本完全匹配的元素
     * @param text 要匹配的文本内容
     * @return 文本完全匹配的元素列表
     */
    fun findByTextAllMatch(text: String): List<AccessibilityNodeInfo> {
        val listResult = arrayListOf<AccessibilityNodeInfo>()
        val list = AssistsService.instance?.rootInActiveWindow?.findByText(text)
        list?.let {
            it.forEach {
                if (TextUtils.equals(it.text, text)) {
                    listResult.add(it)
                }
            }
        }
        return listResult
    }

    /**
     * 在指定元素范围内通过文本查找所有符合条件的元素
     * @param text 要查找的文本内容
     * @return 符合条件的元素列表
     */
    fun AccessibilityNodeInfo?.findByText(text: String): List<AccessibilityNodeInfo> {
        if (this == null) return arrayListOf()
        findAccessibilityNodeInfosByText(text)?.let {
            return it
        }
        return arrayListOf()
    }

    /**
     * 判断元素是否包含指定文本
     * @param text 要检查的文本内容
     * @return true表示包含指定文本，false表示不包含
     */
    fun AccessibilityNodeInfo?.containsText(text: String): Boolean {
        if (this == null) return false
        getText()?.let {
            if (it.contains(text)) return true
        }
        contentDescription?.let {
            if (it.contains(text)) return true
        }
        return false
    }

    /**
     * 获取元素的所有文本内容（包括text和contentDescription）
     * @return 包含所有文本内容的列表
     */
    fun AccessibilityNodeInfo?.getAllText(): ArrayList<String> {
        if (this == null) return arrayListOf()
        val texts = arrayListOf<String>()
        getText()?.let {
            texts.add(it.toString())
        }
        contentDescription?.let {
            texts.add(it.toString())
        }
        return texts
    }

    /**
     * 根据多个条件查找元素
     * @param className 元素的类名
     * @param viewId 可选的资源id过滤条件
     * @param text 可选的文本过滤条件
     * @param des 可选的描述文本过滤条件
     * @return 符合所有条件的元素列表
     */
    fun findByTags(
        className: String, viewId: String? = null, text: String? = null, des: String? = null
    ): List<AccessibilityNodeInfo> {
        var nodeList = arrayListOf<AccessibilityNodeInfo>()
        getAllNodes().forEach {
            if (TextUtils.equals(className, it.className)) {
                nodeList.add(it)
            }
        }
        nodeList = viewId?.let {
            return@let arrayListOf<AccessibilityNodeInfo>().apply {
                addAll(nodeList.filter {
                    return@filter it.viewIdResourceName == viewId
                })
            }
        } ?: let {
            return@let nodeList
        }

        nodeList = text?.let {
            return@let arrayListOf<AccessibilityNodeInfo>().apply {
                addAll(nodeList.filter {
                    return@filter it.txt() == text
                })
            }
        } ?: let { return@let nodeList }
        nodeList = des?.let {
            return@let arrayListOf<AccessibilityNodeInfo>().apply {
                addAll(nodeList.filter {
                    return@filter it.des() == des
                })
            }
        } ?: let { return@let nodeList }
        return nodeList
    }

    /**
     * 在指定元素范围内根据多个条件查找元素
     * @param className 元素的类名
     * @param viewId 可选的资源id过滤条件
     * @param text 可选的文本过滤条件
     * @param des 可选的描述文本过滤条件
     * @return 符合所有条件的元素列表
     */
    fun AccessibilityNodeInfo.findByTags(
        className: String, viewId: String? = null, text: String? = null, des: String? = null
    ): List<AccessibilityNodeInfo> {
        var nodeList = arrayListOf<AccessibilityNodeInfo>()
        getNodes().forEach {
            if (TextUtils.equals(className, it.className)) {
                nodeList.add(it)
            }
        }
        nodeList = viewId?.let {
            return@let arrayListOf<AccessibilityNodeInfo>().apply {
                addAll(nodeList.filter {
                    return@filter it.viewIdResourceName == viewId
                })
            }
        } ?: let {
            return@let nodeList
        }

        nodeList = text?.let {
            return@let arrayListOf<AccessibilityNodeInfo>().apply {
                addAll(nodeList.filter {
                    return@filter it.txt() == text
                })
            }
        } ?: let { return@let nodeList }
        nodeList = des?.let {
            return@let arrayListOf<AccessibilityNodeInfo>().apply {
                addAll(nodeList.filter {
                    return@filter it.des() == des
                })
            }
        } ?: let { return@let nodeList }

        return nodeList
    }

    /**
     * 查找第一个符合指定类型的父元素
     * @param className 要查找的父元素类名
     * @return 找到的父元素，如果未找到则返回null
     */
    fun AccessibilityNodeInfo.findFirstParentByTags(className: String): AccessibilityNodeInfo? {
        val nodeList = arrayListOf<AccessibilityNodeInfo>()
        findFirstParentByTags(className, nodeList)
        return nodeList.firstOrNull()
    }

    /**
     * 递归查找符合指定类型的父元素
     * @param className 要查找的父元素类名
     * @param container 用于存储查找结果的列表
     */
    fun AccessibilityNodeInfo.findFirstParentByTags(
        className: String, container: ArrayList<AccessibilityNodeInfo>
    ) {
        getParent()?.let {
            if (TextUtils.equals(className, it.className)) {
                container.add(it)
            } else {
                it.findFirstParentByTags(className, container)
            }
        }
    }

    /**
     * 获取当前窗口中的所有元素
     * @return 包含所有元素的列表
     */
    fun getAllNodes(): ArrayList<AccessibilityNodeInfo> {
        val nodeList = arrayListOf<AccessibilityNodeInfo>()
        AssistsService.instance?.rootInActiveWindow?.getNodes(nodeList)
        return nodeList
    }

    /**
     * 获取指定元素下的所有子元素
     * @return 包含所有子元素的列表
     */
    fun AccessibilityNodeInfo.getNodes(): ArrayList<AccessibilityNodeInfo> {
        val nodeList = arrayListOf<AccessibilityNodeInfo>()
        this.getNodes(nodeList)
        return nodeList
    }

    /**
     * 递归获取元素的所有子元素
     * @param nodeList 用于存储子元素的列表
     */
    private fun AccessibilityNodeInfo.getNodes(nodeList: ArrayList<AccessibilityNodeInfo>) {
        nodeList.add(this)
        if (nodeList.size > 10000) return // 防止无限递归
        for (index in 0 until this.childCount) {
            getChild(index)?.getNodes(nodeList)
        }
    }

    /**
     * 查找元素的第一个可点击的父元素
     * @return 找到的可点击父元素，如果未找到则返回null
     */
    fun AccessibilityNodeInfo.findFirstParentClickable(): AccessibilityNodeInfo? {
        arrayOfNulls<AccessibilityNodeInfo>(1).apply {
            findFirstParentClickable(this)
            return this[0]
        }
    }

    /**
     * 递归查找可点击的父元素
     * @param nodeInfo 用于存储查找结果的数组
     */
    private fun AccessibilityNodeInfo.findFirstParentClickable(nodeInfo: Array<AccessibilityNodeInfo?>) {
        if (parent?.isClickable == true) {
            nodeInfo[0] = parent
            return
        } else {
            parent?.findFirstParentClickable(nodeInfo)
        }
    }

    /**
     * 获取元素的直接子元素（不包括子元素的子元素）
     * @return 包含直接子元素的列表
     */
    fun AccessibilityNodeInfo.getChildren(): ArrayList<AccessibilityNodeInfo> {
        val nodes = arrayListOf<AccessibilityNodeInfo>()
        for (i in 0 until this.childCount) {
            val child = getChild(i)
            nodes.add(child)
        }
        return nodes
    }

    /**
     *   执行手势操作
     * @param gesture 手势描述对象
     * @param nonTouchableWindowDelay 窗口变为不可触摸后的延迟时间
     * @return 手势是否执行成功
     */
    suspend fun dispatchGesture(
        gesture: GestureDescription,
        nonTouchableWindowDelay: Long = 100,
    ): Boolean {
        val completableDeferred = CompletableDeferred<Boolean>()

        val gestureResultCallback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
//                CoroutineWrapper.launch { AssistsWindowManager.touchableByAll() }
                completableDeferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
//                CoroutineWrapper.launch { AssistsWindowManager.touchableByAll() }
                completableDeferred.complete(false)
            }
        }
        val runResult = AssistsService.instance?.let {
//            AssistsWindowManager.nonTouchableByAll()
            delay(nonTouchableWindowDelay)
            runMain { it.dispatchGesture(gesture, gestureResultCallback, null) }
        } ?: let {
            return false
        }
        if (!runResult) return false
        return completableDeferred.await()
    }

    /**
     * 执行点击或滑动手势
     * @param startLocation 起始位置坐标
     * @param endLocation 结束位置坐标
     * @param startTime 开始延迟时间
     * @param duration 手势持续时间
     * @return 手势是否执行成功
     */
    suspend fun gesture(
        startLocation: FloatArray,
        endLocation: FloatArray,
        startTime: Long,
        duration: Long,
    ): Boolean {
        val path = Path()
        path.moveTo(startLocation[0], startLocation[1])
        path.lineTo(endLocation[0], endLocation[1])
        Rect(
            startLocation[0].toInt(),
            startLocation[1].toInt(),
            endLocation[0].toInt(),
            endLocation[1].toInt()
        )/*.show(
            MaskView.TYPE_AREA
        )*/
        return gesture(path, startTime, duration)
    }

    /**
     * 执行自定义路径的手势
     * @param path 手势路径
     * @param startTime 开始延迟时间
     * @param duration 手势持续时间
     * @return 手势是否执行成功
     */
    suspend fun gesture(
        path: Path,
        startTime: Long,
        duration: Long,
    ): Boolean {
        val builder = GestureDescription.Builder().apply {
            // 第一段：正常线性滑动
            addStroke(StrokeDescription(path, startTime, duration))
        }.build()
        val deferred = CompletableDeferred<Boolean>()
        val runResult = runMain {
            return@runMain AssistsService.instance?.dispatchGesture(
                builder, object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        deferred.complete(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        deferred.complete(false)
                    }
                }, null
            ) ?: let {
                return@runMain false
            }
        }
        if (!runResult) return false
        val result = deferred.await()
        return result
    }

    /**
     * 获取元素在屏幕中的位置信息
     * @return 包含元素位置信息的Rect对象
     */
    fun AccessibilityNodeInfo.getBoundsInScreen(): Rect {
        val boundsInScreen = Rect()
        getBoundsInScreen(boundsInScreen)
        return boundsInScreen
    }


    /**
     * 在指定坐标位置执行点击手势
     * @param x 横坐标
     * @param y 纵坐标
     * @param duration 点击持续时间
     * @return 手势是否执行成功
     */
    suspend fun gestureClick(
        x: Float, y: Float, duration: Long = 10
    ): Boolean {
        return gesture(
            floatArrayOf(x, y), floatArrayOf(x, y),
            0,
            duration,
        )
    }

    /**
     * 在指定坐标位置执行点击手势，且按下和抬起不是同一个坐标
     * @param rect 目标应用的bounds
     * @param duration 点击持续时间
     * @return 手势是否执行成功
     */
    suspend fun gestureClick(
        rect: Rect, duration: Long = 10
    ): Boolean {
        val pointInView = generateRandomPointInView(rect)
        if (pointInView == null) return false

        val startLocation = floatArrayOf(pointInView.first, pointInView.second)
        val point = generateNearbyPoint(pointInView.first, pointInView.second, rect)
        var endLocation = if (point == null) {
            floatArrayOf(pointInView.first, pointInView.second)
        } else {
            floatArrayOf(point.first, point.second)
        }
        Rect(
            startLocation[0].toInt(),
            startLocation[1].toInt(),
            endLocation[0].toInt(),
            endLocation[1].toInt()
        )/*.show(
            MaskView.TYPE_AREA
        )*/
        return gesture(startLocation, endLocation, 0, duration)
    }

    /**
     * 在元素位置执行点击手势
     * @param offsetX X轴偏移量
     * @param offsetY Y轴偏移量
     * @param switchWindowIntervalDelay 窗口切换延迟时间
     * @param duration 点击持续时间
     * @return 手势是否执行成功
     */
    suspend fun AccessibilityNodeInfo.nodeGestureClick(
        offsetX: Float = DisplayUtil.widthPixels() * 0.01953f,
        offsetY: Float = DisplayUtil.widthPixels() * 0.01953f,
        switchWindowIntervalDelay: Long = 250,
        duration: Long = 25
    ): Boolean {
//        runMain { AssistsWindowManager.nonTouchableByAll() }
        delay(switchWindowIntervalDelay)
        val rect = getBoundsInScreen()
        val result = gesture(
            floatArrayOf(rect.left.toFloat() + offsetX, rect.top.toFloat() + offsetY),
            floatArrayOf(rect.left.toFloat() + offsetX, rect.top.toFloat() + offsetY),
            0,
            duration,
        )
        delay(switchWindowIntervalDelay)
//        runMain { AssistsWindowManager.touchableByAll() }
        return result
    }

    /**
     * 执行返回操作
     * @return 返回操作是否成功
     */
    fun back(): Boolean {
        Thread.currentThread().stackTrace.joinToString("\n") {
            return@joinToString "${it.className}.${it.methodName} (${it.fileName}:${it.lineNumber})"
        }.let { stack ->
            YLLogger.v("...执行返回操作 $stack")
        }

        return AssistsService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) == true
    }

    /**
     * 返回主屏幕
     * @return 返回主屏幕操作是否成功
     */
    fun home(): Boolean {
        YLLogger.v("...执行Home操作")
        return AssistsService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) == true
    }

    /**
     * 打开通知栏
     * @return 打开通知栏操作是否成功
     */
    fun notifications(): Boolean {
        return AssistsService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true
    }

    /**
     * 显示最近任务
     * @return 显示最近任务操作是否成功
     */
    fun recentApps(): Boolean {
        return AssistsService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) == true
    }

    /**
     * 向元素粘贴文本
     * @param text 要粘贴的文本
     * @return 粘贴操作是否成功
     */
    fun AccessibilityNodeInfo.paste(text: String?): Boolean {
        performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        AssistsService.instance?.let {
            val clip = ClipData.newPlainText("${System.currentTimeMillis()}", text)
            val clipboardManager =
                (it.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            clipboardManager.setPrimaryClip(clip)
            clipboardManager.primaryClip
            return performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
        return false
    }

    /**
     * 选择元素中的文本
     * @param selectionStart 选择起始位置
     * @param selectionEnd 选择结束位置
     * @return 文本选择操作是否成功
     */
    fun AccessibilityNodeInfo.selectionText(selectionStart: Int, selectionEnd: Int): Boolean {
        val selectionArgs = Bundle()
        selectionArgs.putInt(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionStart
        )
        selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionEnd)
        return performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
    }

    /**
     * 设置元素的文本内容
     * @param text 要设置的文本
     * @return 设置文本操作是否成功
     */
    fun AccessibilityNodeInfo.setNodeText(text: String?): Boolean {
        text ?: return false
        return performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundleOf().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
            )
        })
    }

    /**
     * 根据基准宽度计算实际X坐标
     * @param baseWidth 基准宽度
     * @param x 原始X坐标
     * @return 计算后的实际X坐标
     */
    fun getX(baseWidth: Int, x: Int): Int {
        val screenWidth = DisplayUtil.widthPixels()
        return (x / baseWidth.toFloat() * screenWidth).toInt()
    }

    /**
     * 根据基准高度计算实际Y坐标
     * @param baseHeight 基准高度
     * @param y 原始Y坐标
     * @return 计算后的实际Y坐标
     */
    fun getY(baseHeight: Int, y: Int): Int {
        var screenHeight = DisplayUtil.heightPixels()
        if (screenHeight > baseHeight) {
            screenHeight = baseHeight
        }
        return (y.toFloat() / baseHeight * screenHeight).toInt()
    }

    /**
     * 获取当前应用在屏幕中的位置
     * @return 应用窗口的位置信息，如果未找到则返回null
     */
    fun getAppBoundsInScreen(): Rect? {
        return AssistsService.instance?.let {
            return@let findById("android:id/content").firstOrNull()?.getBoundsInScreen()
        }
    }

    /**
     * 初始化并缓存当前应用在屏幕中的位置
     * @return 应用窗口的位置信息
     */
    fun initAppBoundsInScreen(): Rect? {
        return getAppBoundsInScreen().apply {
            appRectInScreen = this
        }
    }

    /**
     * 获取当前应用在屏幕中的宽度
     * @return 应用窗口的宽度
     */
    fun getAppWidthInScreen(): Int {
        return appRectInScreen?.let {
            return@let it.right - it.left
        } ?: DisplayUtil.widthPixels()
    }

    /**
     * 获取当前应用在屏幕中的高度
     * @return 应用窗口的高度
     */
    fun getAppHeightInScreen(): Int {
        return appRectInScreen?.let {
            return@let it.bottom - it.top
        } ?: DisplayUtil.heightPixels()
    }

    /**
     * 向前滚动可滚动元素
     * @return 滚动操作是否成功
     */
    fun AccessibilityNodeInfo.scrollForward(): Boolean {
        return performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    /**
     * 向后滚动可滚动元素
     * @return 滚动操作是否成功
     */
    fun AccessibilityNodeInfo.scrollBackward(): Boolean {
        return performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    /**
     * 滚动到当前界面中第一个可滚动容器的顶端
     */
    fun AccessibilityNodeInfo.scrollToTop() {
        // 持续向上滚动，直到不能滚动
        while (this.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
            // 加个短延迟，避免过快导致失败
            Thread.sleep(100)
        }
    }

    /**
     * 在日志中输出元素的详细信息
     */
    fun AccessibilityNodeInfo.logNode() {
        StringBuilder().apply {
            val rect = getBoundsInScreen()
            append("-------------------------------------\n")
            append("位置:left=${rect.left}, top=${rect.top}, right=${rect.right}, bottom=${rect.bottom} \n")
            append("文本:$text \n")
            append("内容描述:$contentDescription \n")
            append("id:$viewIdResourceName \n")
            append("类型:${className} \n")
            append("是否已经获取到到焦点:$isFocused \n")
            append("是否可滚动:$isScrollable \n")
            append("是否可点击:$isClickable \n")
            append("是否可用:$isEnabled \n")
            YLLogger.d(toString())
        }
    }

    private val knownLaunchers = listOf(
        "com.miui.home", // 小米
        "com.android.launcher3", // AOSP Launcher
        "com.google.android.launcher", // Google Now Launcher
        "com.huawei.android.launcher", // 华为
        "com.sec.android.app.launcher", // 三星
        "com.lge.launcher2", // LG
        "com.oppo.launcher", // Oppo
        "com.ldmnq.launcher3" // 模拟器
        // 可以根据需要添加其他厂商的Launcher包名
    )

    /**
     * 判断当前屏幕是否为桌面
     */
    fun isCurrentScreenLauncher(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false

        //  直接检查已知的Launcher包名
        if (knownLaunchers.any { it.equals(packageName, ignoreCase = true) }) {
            return true
        }
        //  获取系统默认Launcher并进行比较
        val defaultLauncher = getDefaultLauncherPackageName()
        return packageName.equals(defaultLauncher, ignoreCase = true)
    }

    /**
     * 获取默认Launcher包名
     */
    private fun getDefaultLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo =
            App.app.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * 从一个 Rect 中扣除边界（10% 宽度、5% 高度）
     * 在剩余区域内生成一个随机点 (x, y)
     */
    fun generateRandomPointInView(rect: Rect): Pair<Float, Float>? {
        val viewWidth = rect.right - rect.left
        val viewHeight = rect.bottom - rect.top
        // 计算预留边界的数值（基于 View 自身宽高）
        val horizontalPadding = viewWidth * 0.1f // 宽度方向预留10%
        val verticalPadding = viewHeight * 0.05f // 高度方向预留5%
        // 在View内部，并预留边界
        val innerLeft = rect.left + horizontalPadding
        val innerRight = rect.right - horizontalPadding
        val innerTop = rect.top + verticalPadding
        val innerBottom = rect.bottom - verticalPadding
        // 确保有效范围
        if (innerLeft >= innerRight || innerTop >= innerBottom) {
            println("无效区域：预留边界后可用区域为空")
            return null
        }
        // 生成 [min, max) 范围内的随机 Float
        val randomX = rect.left.toFloat().let {
            Random.nextFloat() * (innerRight - innerLeft) + innerLeft
        }
        val randomY = rect.top.toFloat().let {
            Random.nextFloat() * (innerBottom - innerTop) + innerTop
        }
        return Pair(randomX, randomY)
    }

    /**
     * 根据传入的x、y，结合最小滑动距离生成一个新的坐标点
     */
    fun generateNearbyPoint(
        x: Float, y: Float, rect: Rect, minSlideDistance: Float = touchSlop.toFloat()
    ): Pair<Float, Float>? {
        (1..10).forEach { _ ->
            // 循环尝试指定次数
            // 随机生成角度和距离
            val angle = Random.nextDouble() * 2 * Math.PI // 角度范围在0到2π之间
            // 距离最大值是minSlideDistance
            val distance = Random.nextDouble() * minSlideDistance
            // 根据极坐标计算新的x和y坐标
            val deltaX = cos(angle) * distance
            val deltaY = sin(angle) * distance
            // 计算新点的坐标并保持为浮点数
            val newX = (x + deltaX).toFloat()
            val newY = (y + deltaY).toFloat()

            // 检查新点是否在rect范围内
            if (newX >= rect.left && newX <= rect.right && newY >= rect.top && newY <= rect.bottom) {
                return Pair(newX, newY)
            }
        }
        // 如果尝试了maxAttempts次后仍未能找到合适的点，则返回null
        return null
    }




    suspend fun isTargetAppRunning(packageName: String?): Boolean = withContext(Dispatchers.IO) {
        if (packageName == null) return@withContext false

        return@withContext Shell.cmd("dumpsys activity activities | grep mActivityComponent")
            .exec().out.any { line ->
                line.let {
//                    YLLogger.d("?? $line contains: \n $packageName")
                    it
                }.contains(packageName)
            }
    }

    /**
     * 获取当前 Android 设备上正在显示（resumed）的 Activity 包名和类名。
     * 该函数通过执行 adb shell 命令来获取 dumpsys activity 的输出并进行解析。
     *
     * @return 如果成功获取并解析，则返回 "packageName/fullyQualifiedClassName" 格式的字符串；否则返回 null。
     */
    suspend fun getForegroundActivity2(): String? = withContext(Dispatchers.IO) {

        var foundLine = Shell.cmd("dumpsys activity activities | grep mActivityComponent")
            .exec().out.firstOrNull()?.let {
//                YLLogger.d("getForegroundActivity2 mActivityComponent:\n $it")
                parseComponentName3(it)
            }

        if (foundLine?.isNotBlank() == true) {
//            YLLogger.d("getForegroundActivity2 0[dumpsys activity activities]\n$foundLine")
//            YLLogger.i("🚚dumpsys activity activities | grep mActivityComponent\n $foundLine")
            return@withContext foundLine
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            foundLine = Shell.cmd("dumpsys activity top | grep -E 'ACTIVITY'").exec()
                .takeIf { it.isSuccess }?.out?.last {
                    (it.trim().startsWith("ACTIVITY ")) && it.trim().endsWith("(type=INTERNAL)")
                }?.let { parseComponentName2(it) }

            if (foundLine?.isNotBlank() == true) {
//                YLLogger.d("getForegroundActivity2 1[dumpsys activity top | grep -E 'ACTIVITY']\n$foundLine")
//                YLLogger.i("🚚dumpsys activity top | grep -E 'ACTIVITY'\n $foundLine")
                return@withContext foundLine
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val exec = Shell.cmd("dumpsys window windows").exec()
            foundLine = exec.takeIf { it.isSuccess }?.out?.find {
                (it.contains("topResumedActivity") || it.contains("mResumedActivity") || it.contains(
                    "mObscuringWindow"
                )) && it.contains("u0 ")
            }?.let { parseComponentName(it) }
            if (foundLine?.isNotBlank() == true) {
//                YLLogger.d("getForegroundActivity2 2[dumpsys window windows]\n$foundLine")
//                YLLogger.i("🚚dumpsys window windows\n $foundLine")
                return@withContext foundLine
            }
        }
        foundLine = Shell.cmd("dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp'").exec()
            .takeIf { it.isSuccess }?.out?.find {
                (it.contains("mCurrentFocus") || it.contains("mFocusedApp")) && it.contains("u0 ")
            }?.let { parseComponentName(it) }

        if (foundLine?.isNotBlank() == true) {
//            YLLogger.d("getForegroundActivity2 3[dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp']\n$foundLine")
//            YLLogger.i("🚚dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp'\n $foundLine")
            return@withContext foundLine
        }
        YLLogger.d("getForegroundActivity2 4[NULL]")
        YLLogger.i("🚚oh no no no no top ACTIVITY found")
        return@withContext null
    }

    private fun parseComponentName(it: String): String? {
        return runCatching {
            var comp: String? = null
            val startIndex = it.indexOf("u0 ") + 3
            val endIndex = it.indexOf("}", startIndex)
            if (startIndex < endIndex && endIndex != -1) {
                val text = it.substring(startIndex, endIndex).trim()
                if (text.contains(" ")) {
                    val indexOf = text.indexOf(" ")
                    comp = text.substring(0, indexOf)
                }
            }
            comp
        }.onFailure { null }.getOrNull()
    }

    private fun parseComponentName3(string: String): String? {
        //       mActivityComponent=com.opera.browser/com.opera.android.BrowserActivity
        return runCatching {
            var comp: String? = null
            val startIndex = string.indexOf("mActivityComponent=") + "mActivityComponent=".length
            if (startIndex > -1) {
                comp = string.substring(startIndex).trim()
            }
            comp
        }.onFailure { null }.getOrNull()
    }

    private fun parseComponentName2(string: String): String? {
        val startIndex = string.indexOf("ACTIVITY ") // 注意这里 ACTIVITY 后面有一个空格
        return runCatching {
            var comp: String? = null
            if (startIndex != -1) {
                val actualStartIndex = startIndex + "ACTIVITY ".length
                val endIndex = string.indexOf(" ", actualStartIndex)

                comp = if (endIndex != -1) {
                    string.substring(actualStartIndex, endIndex).trim()
                } else {
                    string.substring(actualStartIndex).trim()
                }
            }
            comp
        }.onFailure { null }.getOrNull()
    }

    /**
     * @return 如果成功获取并解析，则返回 "packageName" 格式的字符串；否则返回 null。
     */
    suspend fun getForegroundPackageName(): String? {
        return runCatching {
            var pkgName: String? = null
            try {
                val foregroundActivity = getForegroundActivity2()
                pkgName = foregroundActivity?.split("/")[0]
                YLLogger.i("💡getForegroundPackageName foregroundActivity=$foregroundActivity, pkgName=$pkgName")
            } catch (e: Exception) {
                YLLogger.e("💡getForegroundPackageName", e)
            }
            if (pkgName == null) {
                pkgName = getForegroundApp(App.context)
                YLLogger.i("💡getForegroundApp[SystemService] pkgName=$pkgName")
            }
            pkgName?.trim()
        }.onFailure { null }.getOrNull()
    }

    /**
     * @return 如果成功获取并解析，则返回 "fullyQualifiedClassName" 格式的字符串；否则返回 null。
     */
    suspend fun getForegroundActivityName(): String? {
        return runCatching {
            val foregroundActivity = getForegroundActivity2()
            return foregroundActivity?.split("/")[1]
        }.onFailure { null }.getOrNull()
    }

    @SuppressLint("WrongConstant")
    fun getForegroundApp(context: Context): String? {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, now - 1000 * 10, now
        )

        var foregroundPackage: String? = null
        var lastActiveTime = 0L

        for (usageStats in stats ?: emptyList()) {
            if (usageStats.lastTimeUsed > lastActiveTime) {
                foregroundPackage = usageStats.packageName
                lastActiveTime = usageStats.lastTimeUsed
            }
        }

        return foregroundPackage
    }

    fun isSquareViewAtCenter(
        screenWidth: Int, screenHeight: Int, targetClassName: String
    ): Boolean {
        val rootNode = AssistsService.instance?.rootInActiveWindow ?: return false
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        fun isPointInsideBounds(node: AccessibilityNodeInfo, x: Int, y: Int): Boolean {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            return bounds.contains(x, y)
        }

        fun isSquare(node: AccessibilityNodeInfo): Boolean {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            return bounds.width() == bounds.height() && bounds.width() > 0
        }

        fun searchInViewTree(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            if (isPointInsideBounds(node, centerX, centerY)) {
                if (node.className == targetClassName && isSquare(node)) {
                    return true
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (searchInViewTree(child)) {
                    return true
                }
            }
            return false
        }
        return searchInViewTree(rootNode)
    }

    fun findMatchedKeywordInWebView(
        webViewNode: AccessibilityNodeInfo?, keywordList: List<String>
    ): String? {
        // 先检查当前节点是否满足条件
        val text = webViewNode?.text?.toString()
        if (!text.isNullOrEmpty()) {
            for (keyword in keywordList) {
                if (text.contains(keyword, ignoreCase = true)) {
                    return keyword
                }
            }
        }

        val resourceId = webViewNode?.viewIdResourceName?.toString()
        if (!resourceId.isNullOrEmpty()) {
            for (keyword in keywordList) {
                if (resourceId.contains(keyword, ignoreCase = true)) {
                    return keyword
                }
            }
        }
        // 递归检查子节点
        for (i in 0 until (webViewNode?.childCount ?: 0)) {
            val child = webViewNode?.getChild(i) ?: continue
            val result = findMatchedKeywordInWebView(child, keywordList)
            if (result != null) {
                return result
            }
        }
        return null
    }


    /**
     * 判断该节点是否应递归其子节点
     * - 只有 Layout 容器类才继续深入
     * - TextView、ImageView、Button 等叶子节点不再递归
     */
    private fun shouldTraverseChildren(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: return false

        when (className) {
            "android.widget.TextView",
            "android.widget.Button",
            "android.widget.ImageButton",
            "android.widget.ImageView",
            "android.widget.EditText",
            "android.widget.CheckBox",
            "android.widget.Switch",
            "android.widget.RadioButton",
            "android.widget.RatingBar",
            "android.widget.ProgressBar",
            "android.widget.Space" -> return false
        }

        return when {
            className == "android.widget.LinearLayout" -> true
            className == "android.widget.RelativeLayout" -> true
            className == "android.widget.FrameLayout" -> true
            className == "android.widget.TableLayout" -> true
            className == "android.widget.CoordinatorLayout" -> true
            className == "androidx.recyclerview.widget.RecyclerView" -> true
            className == "android.widget.ScrollView" -> true
            className == "android.widget.HorizontalScrollView" -> true
            className == "androidx.constraintlayout.widget.ConstraintLayout" -> true

            className.startsWith("androidx.") || className.startsWith("com.google.android.material") -> {
                className.contains("Layout") ||
                        className.contains("RecyclerView") ||
                        className.contains("ScrollView")
            }

            className.contains("Layout") -> true
            else -> true
        }
    }

    /**
     * 是否为正方形
     */
    fun AccessibilityNodeInfo.isSquare(): Boolean {
        val rect = this.getBoundsInScreen()
        return rect.width() == rect.height()

    }

    fun AccessibilityNodeInfo.scaleInY(): Float {
        val heightPixels = DisplayUtil.heightPixels()
        val rect = this.getBoundsInScreen()
        if (heightPixels == 0) return 0f
        val ratio = rect.bottom.toFloat() / heightPixels
        val percentage = ratio * 100
        return "%.2f".format(percentage).toFloat()
    }

    val AccessibilityNodeInfo.isCheckableOrClickable: Boolean
        get() = isCheckable || isClickable || (className == "android.widget.Button")

    suspend fun canGoBackTargetApp() {
        val foregroundPackageName = getForegroundPackageName() ?: "UNDEFINED"
        if ("com.android.settings" == foregroundPackageName) {
            back()
        }
    }

    /**
     * 判断当前页面是否属于目标条件下
     * 不符合条件的就退出当前任务
     */
    suspend fun checkWithRules(targetName: String, rules: Array<String> = defaultRules()): Boolean {
        val foregroundPackageName = getForegroundPackageName() ?: "UNDEFINED"
        YLLogger.i("💡checkWithRules foregroundPackageName=$foregroundPackageName -- targetName=$targetName -- rules=${rules.joinToString { it }}")
        if (targetName == foregroundPackageName) {
            return true
        }
        if (foregroundPackageName in rules) return true
        return false
    }

    fun defaultRules(): Array<String> {
        return arrayOf(
            "com.android.permissioncontroller", //  云手机的权限包名
            "com.android.packageinstaller"  //  雷电模拟器的权限授权包名
        )
    }

    fun defaultRules2(): Array<String> {
        val arrayOf = arrayOf(
            "com.android.vending",
        ).plus(Constants.BROWSER_LIST)
        return arrayOf
    }
}