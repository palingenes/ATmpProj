package com.cymf.keyshot.utils

import android.graphics.Rect
import android.util.Xml
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.LinkedList

/**
 *  viewModelScope.launch {
 *     // 1. 获取并解析 window_dump
 *     val nodes = WindowDumpAnalyzer.analyzeWindowDump()
 *
 *     // 2. 查找包含目标文本的控件
 *     val matchedNodes = WindowDumpAnalyzer.findNodesByText(nodes, targetText)
 *
 *     // 3. 打印匹配的控件信息
 *     for (node in matchedNodes) {
 *         println("匹配控件：文本='${node.text}', 坐标='${node.bounds}', 可点击=${node.clickable}")
 *         // 如果需要点击，可调用如下 shell 命令
 *         // tapOnBounds(node.bounds)
 *     }
 * }
 */
object WindowDumpAnalyzer {

    // 原始UI节点
    data class UiNode(
        val index: String,
        val text: String,
        val resourceId: String,
        val className: String,
        val packageName: String,
        val contentDesc: String,
        val clickable: Boolean,
        val scrollable: Boolean,
        val longClickable: Boolean,
        val checkable: Boolean,
        val checked: Boolean,
        val enabled: Boolean,
        val bounds: String
    )

    // 树形结构节点
    data class UiNodeTree(
        val node: UiNode, val children: MutableList<UiNodeTree> = mutableListOf()
    )

    // 主方法：获取并解析 window_dump，保留层级结构并过滤 enabled != true 的节点
    suspend fun analyzeWindowDump(): List<UiNodeTree> = withContext(Dispatchers.IO) {
        try {
            // FIXME 会导致AssistsService 重启
//            val xmlContent = runIO {  executeShellCommandAndGetOutput() }
//            if (xmlContent.isBlank()) {
//                YLLogger.e("XML content is empty")
//                emptyList()
//            } else {
//                val root = parseXmlStringToTree(xmlContent)
//                val filteredRoot = filterEnabledNodes(root)
//                filteredRoot?.children ?: emptyList()
//            }
            emptyList()
        } catch (e: Exception) {
            YLLogger.e("analyzeWindowDump", e)
            emptyList()
        }
    }

    private fun executeShellCommandAndGetOutput(): String {
        val dumpPath = "/data/local/tmp/window_dump.xml"
        val string = "uiautomator dump $dumpPath"
        val string1 = "chmod 666 $dumpPath"
        val string2 = "cat $dumpPath"

        val result = Shell.cmd(string, string1, string2).exec()
        val output = StringBuilder()
        if (result.isSuccess) {
            for (str in result.out) {
                if (str.startsWith("UI hierchary dumped to: $dumpPath")) continue
                output.append("$str\n")
            }
        } else {
            val sb = StringBuilder()
            sb.append("报错code=${result.code}\n")
            result.err.forEach { sb.append("$it\n") }
            YLLogger.e(sb.toString())
        }
        return output.toString()
    }

   fun excShellCommandAndGetOutput(): String {
        val dumpPath = "/data/local/tmp/window_dump.xml"
        val string = "uiautomator dump $dumpPath"
        val string2 = "cat $dumpPath"

        val result = Shell.cmd(string,  string2).exec()
        val output = StringBuilder()
        if (result.isSuccess) {
            for (str in result.out) {
                if (str.startsWith("UI hierchary dumped to: $dumpPath")) continue
                output.append("$str\n")
            }
        } else {
            val sb = StringBuilder()
            sb.append("报错code=${result.code}\n")
            result.err.forEach { sb.append("$it\n") }
            YLLogger.e(sb.toString())
        }
        return output.toString()
    }

    private fun parseXmlStringToTree(xmlContent: String): UiNodeTree {
        val rootNode = UiNodeTree(createDummyNode())
        val stack = LinkedList<UiNodeTree>().apply { addLast(rootNode) }

        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(xmlContent.reader())
            nextTag()
        }

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when {
                eventType == XmlPullParser.START_TAG && parser.name == "node" -> {
                    val node = parseNode(parser)
                    val newNode = UiNodeTree(node)
                    stack.last().children.add(newNode)
                    stack.addLast(newNode)
                }

                eventType == XmlPullParser.END_TAG && parser.name == "node" -> {
                    stack.removeLast()
                }
            }
            eventType = parser.next()
        }

        return rootNode
    }


    // 创建一个虚拟节点，用于根节点
    private fun createDummyNode() = UiNode(
        index = "0",
        text = "",
        resourceId = "",
        className = "",
        packageName = "",
        contentDesc = "",
        clickable = false,
        scrollable = false,
        longClickable = false,
        checkable = false,
        checked = false,
        enabled = true,
        bounds = ""
    )

    // 解析单个 <node> 标签
    private fun parseNode(parser: XmlPullParser): UiNode {
        var index = ""
        var text = ""
        var resourceId = ""
        var className = ""
        var packageName = ""
        var contentDesc = ""
        var clickable = false
        var scrollable = false
        var longClickable = false
        var checkable = false
        var checked = false
        var enabled = false
        var bounds = ""

        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i)
            val attrValue = parser.getAttributeValue(i)

            when (attrName) {
                "index" -> index = attrValue
                "text" -> text = attrValue
                "resource-id" -> resourceId = attrValue
                "class" -> className = attrValue
                "package" -> packageName = attrValue
                "content-desc" -> contentDesc = attrValue
                "clickable" -> clickable = attrValue == "true"
                "scrollable" -> scrollable = attrValue == "true"
                "long-clickable" -> longClickable = attrValue == "true"
                "checkable" -> checkable = attrValue == "true"
                "checked" -> checked = attrValue == "true"
                "enabled" -> enabled = attrValue == "true"
                "bounds" -> bounds = attrValue
            }
        }

        return UiNode(
            index = index,
            text = text,
            resourceId = resourceId,
            className = className,
            packageName = packageName,
            contentDesc = contentDesc,
            clickable = clickable,
            scrollable = scrollable,
            longClickable = longClickable,
            checkable = checkable,
            checked = checked,
            enabled = enabled,
            bounds = bounds
        )
    }

    // 递归过滤 enabled 为 false 的节点及其子节点
    private fun filterEnabledNodes(node: UiNodeTree): UiNodeTree? {
        if (!node.node.enabled) {
            return null
        }
        val filtered = UiNodeTree(node.node)
        for (child in node.children) {
            val filteredChild = filterEnabledNodes(child)
            if (filteredChild != null) {
                filtered.children.add(filteredChild)
            }
        }
        return filtered
    }

    // 根据 text 查找控件（可模糊匹配）
    fun findNodesByText(
        nodes: List<UiNodeTree>,
        targetTexts: Collection<String>,
        ignoreCase: Boolean = true
    ): List<UiNodeTree> {

        val result = mutableListOf<UiNodeTree>()
        val normalizedTargets = if (ignoreCase) {
            targetTexts.map { it.lowercase() }.toSet()
        } else {
            targetTexts.toSet()
        }

        fun match(text: String): Boolean {
            return if (ignoreCase) {
                val lowerText = text.lowercase()
                normalizedTargets.any { lowerText.contains(it) }
            } else {
                normalizedTargets.contains(text)
            }
        }

        val stack = ArrayDeque<List<UiNodeTree>>()
        stack.addLast(nodes)

        while (stack.isNotEmpty()) {
            val currentLevel = stack.removeLast()
            for (node in currentLevel) {
                if (match(node.node.text)) {
                    result.add(node)
                }
                if (node.children.isNotEmpty()) {
                    stack.addLast(node.children)
                }
            }
        }
        return result
    }

    // 根据 content-desc 查找控件（可模糊匹配）
    fun findNodesByContentDesc(nodes: List<UiNodeTree>, targetDesc: String, ignoreCase: Boolean = true): List<UiNodeTree> {
        val result = mutableListOf<UiNodeTree>()
        for (node in nodes) {
            if (ignoreCase) {
                if (node.node.contentDesc.contains(targetDesc, ignoreCase = true)) {
                    result.add(node)
                }
            } else {
                if (node.node.contentDesc == targetDesc) {
                    result.add(node)
                }
            }
            result.addAll(findNodesByContentDesc(node.children, targetDesc, ignoreCase))
        }
        return result
    }

    /**
     * 根据依赖关系打印出树状节点信息
     */
    fun List<UiNodeTree>.printNodeTree() {
        this.forEachIndexed { index, node ->
            val isLast = index == this.lastIndex
            node.printNodeTree("", isLast)
        }
    }

    fun UiNodeTree.printNodeTree(prefix: String = "", isLast: Boolean = false) {
        val node = this.node

        // 构建节点信息（对齐格式）
        val sb = StringBuilder()
        sb.append("class = ${node.className}")
        if (node.className == "android.webkit.WebView") {
            sb.append(" (child: ${children.size})")
        }
        sb.append("\n${prefix}  text       = '${node.text}'")
        sb.append("\n${prefix}  res-id     = '${node.resourceId}'")
        sb.append("\n${prefix}  package    = '${node.packageName}'")
        sb.append("\n${prefix}  desc       = '${node.contentDesc}'")
        sb.append("\n${prefix}  clickable  = ${node.clickable}")
        sb.append("\n${prefix}  scrollable  = ${node.scrollable}")
        sb.append("\n${prefix}  long-click = ${node.longClickable}")
        sb.append("\n${prefix}  checkable  = ${node.checkable}")
        sb.append("\n${prefix}  checked    = ${node.checked}")
        sb.append("\n${prefix}  enabled    = ${node.enabled}")
        sb.append("\n${prefix}  bounds     = ${node.bounds}")

        // 树形结构符号
        val marker = if (isLast) "└── " else "├── "
        val currentPrefix = "$prefix$marker"

        // 打印当前节点
        YLLogger.log2("$currentPrefix$sb")

        // 递归打印子节点
        val childPrefix = prefix + if (isLast) "    " else "│   "
        children.forEachIndexed { index, child ->
            child.printNodeTree(childPrefix, index == children.lastIndex)
        }
    }

    /**
     * input [558,1764][1008,1884] 字符串转成 rect 矩形坐标
     */
    fun parseRectFromString(input: String): Rect? {
        return try {
            val str = input.trim()
            if (!str.startsWith("[") || !str.endsWith("]")) return null

            val parts = str.split("][")
            if (parts.size != 2) return null
            val first = parts[0].removePrefix("[").split(",").map { it.trim().toInt() }
            val second = parts[1].removeSuffix("]").split(",").map { it.trim().toInt() }

            if (first.size != 2 || second.size != 2) return null
            val (x1, y1) = first
            val (x2, y2) = second
            Rect(x1, y1, x2, y2)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}