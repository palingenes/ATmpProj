package com.cymf.keyshot.ktx

import java.io.PrintWriter
import java.io.StringWriter

inline fun <T> Collection<T>?.onEmptyOrNot(block: () -> Unit, onNotEmpty: (Collection<T>) -> Unit) {
    if (this.isNullOrEmpty()) {
        block()
    } else {
        onNotEmpty(this)
    }
}

/**
 * 模糊子串匹配（忽略大小写 + 忽略空格）
 */
fun CharSequence?.containsAnyIgnoreCaseAndSpace(keywords: List<String>): Boolean {
    val text = this?.toString() ?: return false

    // 去除空格、转小写
    val normalizedText = text.replace(" ", "", ignoreCase = true).lowercase()

    for (keyword in keywords) {
        val normalizedKeyword = keyword.replace(" ", "", ignoreCase = true).lowercase()
        if (normalizedText.contains(normalizedKeyword)) {
            return true
        }
    }

    return false
}

/**
 * 格式化输出错误/异常 栈信息
 */
fun Throwable.exc(sb: Appendable) {
    val writer = StringWriter()
    val printWriter = PrintWriter(writer)
    this.printStackTrace(printWriter)
    writer.flush()
    sb.append(writer.toString()).append("\n")
}