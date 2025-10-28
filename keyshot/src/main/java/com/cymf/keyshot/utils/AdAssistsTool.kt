package com.cymf.keyshot.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.cymf.keyshot.App
import com.cymf.keyshot.constant.Constants
import com.cymf.keyshot.constant.MultiLanguageKeywords
import com.cymf.keyshot.constant.NodeClassValue
import com.cymf.keyshot.ktx.random
import com.cymf.keyshot.service.AssistsService
import com.cymf.keyshot.utils.AssistsCore.findByTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random
import kotlin.random.nextInt
import com.cymf.keyshot.utils.AssistsCore.getBoundsInScreen
import com.cymf.keyshot.utils.ShellSwipeUtil.clickInBounds

/**
 * 标准化的广告识别、操作相关函数封装
 */
object AdAssistsTool {
    private const val TAG = "AdAssistsTool"

    /**
     * 点击目标View，主要是按下和抬起不是同一个坐标
     */
    suspend fun AccessibilityNodeInfo.clickInRect() {
        this.getBoundsInScreen().let {
            AssistsCore.gestureClick(it, Random.nextInt(200, 501).toLong())
        }
    }

    /**
     * 点击屏幕中心
     */
    suspend fun clickCenter4Screen() {
        val widthPixels = DisplayUtil.widthPixels()
        val heightPixels = DisplayUtil.heightPixels()
        val random = Random.nextDouble(2.01, 2.2).toFloat()
        AssistsCore.gestureClick(widthPixels / random, heightPixels / random, 100)
    }

    /**
     * LM中寻找书城的bottomBar item的rect区域
     */
    fun AccessibilityService.findTabContainerRect(): Rect? {
        val root = this.rootInActiveWindow ?: return null

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var foundCount = 0 // 找到的 tab_btn_container 计数器
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.viewIdResourceName?.endsWith("tab_btn_container") == true) {
                if (foundCount == 1) {
                    // 找到了第 2 个 RelativeLayout
                    val rect = Rect().apply {
                        node.getBoundsInScreen(this)
                    }
                    return rect
                } else {
                    foundCount++
                }
            }
            // 将子节点加入队列继续查找
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }
        return null // 没找到第二个 tab_btn_container
    }

    /**
     * LM中查找已选中的bottomBar
     */
    fun AssistsService.findSelectedTabIndex(): Int {
        val root = this.rootInActiveWindow ?: return -1
        // 使用 BFS 遍历视图树，寻找 tab_btn_container 并检查其内部是否有 tab_btn_default_sel
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var containerIndex = 0 // 记录当前 RelativeLayout 的索引
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.viewIdResourceName?.endsWith("tab_btn_container") == true) {
                // 当前节点是 tab_btn_container, 检查它的子节点是否包含 tab_btn_default_sel
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null && isTabSelected(child)) {
                        return containerIndex
                    }
                }
                containerIndex++
            } else {
                // 继续向下搜索
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        queue.add(child)
                    }
                }
            }
        }
        return -1 // 如果没有找到符合条件的 ImageView
    }

    /**
     * 辅助函数：检查给定的节点或其任意子节点是否为 tab_btn_default_sel
     */
    private fun isTabSelected(node: AccessibilityNodeInfo): Boolean {
        if (node.viewIdResourceName?.endsWith("tab_btn_default_sel") == true) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && isTabSelected(child)) {
                return true
            }
        }
        return false
    }



    /**
     * 随机在屏幕中心生成一个小rect
     */
    fun generateRandomRect(
        minWidth: Int = 50, minHeight: Int = 50, paddingPercentX: Float = 0.1f, // 左右各 10%
        paddingPercentY: Float = 0.2f  // 上下各 20%
    ): Rect {
        val screenWidth = DisplayUtil.widthPixels()
        val screenHeight = DisplayUtil.heightPixels()

        // 计算可用区域
        val leftBound = (screenWidth * paddingPercentX).toInt()
        val rightBound = screenWidth - leftBound
        val topBound = (screenHeight * paddingPercentY).toInt()
        val bottomBound = screenHeight - topBound

        // 可用宽高
        val effectiveMaxWidth = rightBound - leftBound
        val effectiveMaxHeight = bottomBound - topBound

        // 确保最小尺寸不超过有效区域
        val width = Random.nextInt(minWidth, effectiveMaxWidth.coerceAtLeast(minWidth + 1))
        val height = Random.nextInt(minHeight, effectiveMaxHeight.coerceAtLeast(minHeight + 1))

        // 随机位置（在限定区域内）
        val left = Random.nextInt(leftBound, rightBound - width)
        val top = Random.nextInt(topBound, bottomBound - height)

        val right = left + width
        val bottom = top + height

        return Rect(left, top, right, bottom)
    }

    /**
     * 随机在屏幕右上、右下方
     */
    fun generateRandomRect(): Rect {
        val widthPixels = DisplayUtil.widthPixels()
        val heightPixels = DisplayUtil.heightPixels()
        val safePaddingPx = DisplayUtil.dip2px(10f)
        val bottomPaddingPx = DisplayUtil.dip2px(70f)
        val statusBarHeightPx = DisplayUtil.statusBarHeight()

        return if (Random.nextBoolean()) {
            // 右上方的情况
            val top = statusBarHeightPx + safePaddingPx
            val bottom = (heightPixels * 0.3).toInt() - safePaddingPx
            val left = (widthPixels / 2) + safePaddingPx
            val right = widthPixels - safePaddingPx

            Rect(left, top, right, bottom)
        } else {
            // 右下方的情况
            val top = (heightPixels * 0.73).toInt() + safePaddingPx
            val bottom = heightPixels - bottomPaddingPx - safePaddingPx
            val left = (widthPixels / 2) + safePaddingPx
            val right = widthPixels - safePaddingPx

            Rect(left, top, right, bottom)
        }
    }

    /**
     * 判断当前页面是否为插页广告页面
     * </> 仅限 LM、FT等自研产品判断
     *
     * @param curr 当前页面
     * @return 是否为插屏（全屏）广告页面
     */
    fun checkIsAdPage4Self(curr: String?): Boolean {
        return (curr?.endsWith(Constants.AD_FULL_NAME_U3D) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_INMOBI) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_PANGLE) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_PANGLE2) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_PANGLE3) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_PANGLE4) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_PANGLE5) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_PANGLE6) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_PANGLE7) == true ||
                curr?.endsWith(Constants.AD_FULL_NAME_MAX) == true ||
                curr?.endsWith(Constants.AD_BROWSER) == true ||
                curr?.endsWith(Constants.AD_BROWSER_1) == true)
    }

    /**
     * 根据应用大小返回推荐的首次进入等待时间（毫秒）
     */
    fun calculateDelayByAppSize(appSizeBytes: Long?): Long {
        if (appSizeBytes == null) {
            return Random.nextLong(3000, 6000)
        }
        val sizeInMB = appSizeBytes / 1024 / 1024

        return when {
            sizeInMB < 100 -> 5000L
            sizeInMB < 300 -> 7000L
            sizeInMB < 500 -> 10000L
            sizeInMB < 1000 -> 13000L
            else -> 17000L
        }
    }

    suspend fun findTitle4FullScreen(activeWindow: AccessibilityNodeInfo?): Boolean {
        val webviewNodes = activeWindow?.findByTags(NodeClassValue.WebView)
        var hasTitle = false
        for (nodes in webviewNodes ?: emptyList()) {
            if (nodes.getBoundsInScreen().top > 100) {
                hasTitle = true
                break
            }
        }
        if (hasTitle) {
            scrollWebPage()
            delay(TaskPollingManager.getCurrTaskSpeed(600))
        }
        return hasTitle
    }

    suspend fun outslideEvent(
        delayMultiple: Long,
        realClickHappen: Boolean = false,
        TAG: String = ""
    ) {
        delay(TaskPollingManager.getCurrTaskSpeed(2000))
        val currentActivity = AssistsCore.getForegroundActivity2() ?: "Null"
        // 有可能从广告页面跳转到浏览器
        YLLogger.i("🎾${TAG} 检查广告是否跳到外部${currentActivity}")
        if (Constants.BROWSER_LIST.any { currentActivity.startsWith(it) }) {
            delay(TaskPollingManager.getCurrTaskSpeed(2_000))
            scrollWebPage()
            delay((delayMultiple * (0.5..1.5).random()).toLong())
            val frontPkg = AssistsCore.getForegroundPackageName() ?: "Null"
            if (Constants.BROWSER_LIST.any { frontPkg.startsWith(it) }) {
                AssistsCore.exitAppHuman(frontPkg)
            }
            delay(TaskPollingManager.getCurrTaskSpeed(1_000))
            AssistsCore.recentApps()
            delay(TaskPollingManager.getCurrTaskSpeed(800))
            generateRandomRect(paddingPercentX = 0.35f, paddingPercentY = 0.37f).clickInBounds()
            YLLogger.e("🎾${TAG} 从外部浏览器页面返回")
        } else {
            backGooglePlay(currentActivity, realClickHappen, TAG = TAG)
            delay(1500)
        }
    }

    suspend fun backGooglePlay(
        currentActivity: String,
        realClickHappen: Boolean = false,
        callBack: suspend () -> Unit? = {},
        TAG: String = ""
    ) {
        //  正式环境下有frida拦截，就不需要这一步
        if (currentActivity.startsWith(Constants.PKG_VENDING_MOCK)) {
            val from = if (realClickHappen) 5_000 else 2_500
            val until = if (realClickHappen) 10_000 else 5_000
            val delayTimes =
                TaskPollingManager.getCurrTaskSpeed(Random.nextInt(from, until).toLong())
            delay(delayTimes)
            AssistsCore.back()
            delay(TaskPollingManager.getCurrTaskSpeed(1100))
            YLLogger.e("🎾${TAG} 从MockGoogle Play页面返回 随机的时间=" + delayTimes)
        } else if (currentActivity.startsWith(Constants.PKG_VENDING)) {
            val from = if (realClickHappen) 5_000 else 1_500
            val until = if (realClickHappen) 10_000 else 3_000
            delay(TaskPollingManager.getCurrTaskSpeed(Random.nextInt(from, until).toLong()))
            AssistsCore.back()
            delay(TaskPollingManager.getCurrTaskSpeed(1100))
            YLLogger.e("🎾${TAG} 从Google Play页面返回")
        } else {
            callBack.invoke()
            YLLogger.e("🎾${TAG} 从其他页面返回")
        }
    }

    suspend fun scrollWebPage() {
        val count = Random.nextInt(3..7)
        YLLogger.i("🎾网页广告模拟随便滑动")
        repeat(count) { idx ->
            runCatching {
                YLLogger.e("👇滑动浏览器广告 $idx/$count")
                val nextInt = Random.nextInt(1, 6)
                val end = nextInt / 10f
                val durationMs = TaskPollingManager.getCurrTaskSpeed(nextInt * 120L)

                ShellSwipeUtil.swipeUpFromBottom(App.app, 0.7f, end, durationMs)
                delay(TaskPollingManager.getCurrTaskSpeed(1200))
                val dump = WindowDumpAnalyzer.analyzeWindowDump()
                val priBounds =
                    WindowDumpAnalyzer.findNodesByText(dump, MultiLanguageKeywords.PRi_BUTTON)
                        .find {
                            it.node.clickable
                        }?.node?.bounds
                priBounds?.let { WindowDumpAnalyzer.parseRectFromString(it) }
                    ?.clickInBounds(-1, true)?.let {
                        YLLogger.e("进入到隐私协议页面")
                        ShellSwipeUtil.swipeUpFromBottom(App.app, 0.7f, end, durationMs)
                        AssistsCore.back()
                    }
                delay(TaskPollingManager.getCurrTaskSpeed(2000))
            }.onFailure {
                YLLogger.e("scrollWebPage ${idx}: 滑动浏览器广告失败: ${it.message}")
            }

        }
    }
}
