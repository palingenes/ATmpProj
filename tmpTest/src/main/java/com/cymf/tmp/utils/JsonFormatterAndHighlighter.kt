package com.cymf.tmp.utils

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

object JsonFormatterAndHighlighter {

    // 颜色定义
    private val COLORS = mapOf(
        "key" to -0x3f3f40,      // 绿色 #228B22
        "string" to -0xff01,     // 蓝色 #0000FF
        "number" to -0x5500ff01, // 橙色 #FF8C00
        "boolean" to -0x7f0001,  // 紫色 #800080
        "null" to -0x7f000001    // 深红 #800000
    )

    /**
     * 格式化并高亮 JSON 字符串，最终设置到 TextView 显示
     */
    @SuppressLint("SetTextI18n")
    fun formatAndHighlight(jsonInput: String?, textView: TextView) {
        if (jsonInput.isNullOrBlank()) {
            textView.text = "Empty or invalid JSON\n原内容：$jsonInput"
            return
        }

        try {
            val formattedJson = when {
                jsonInput.trim().startsWith("{") -> JSONObject(jsonInput).toString(4)
                jsonInput.trim().startsWith("[") -> JSONArray(jsonInput).toString(4)
                else -> throw IllegalArgumentException("Not a valid JSON")
            }

            val spannable = SpannableString(formattedJson)
            var index = 0

            while (index < formattedJson.length) {
                when {
                    formattedJson.startsWith("\"", index) -> {
                        val isKey = index > 0 && formattedJson[index - 1] == '"'
                        val endQuote = findEndQuote(formattedJson, index + 1)
                        if (endQuote != -1) {
                            val color = if (isKey) COLORS["key"]!! else COLORS["string"]!!
                            spannable.setSpan(
                                ForegroundColorSpan(color),
                                index,
                                endQuote + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            index = endQuote + 1
                        } else break
                    }

                    formattedJson.startsWith("true", index) || formattedJson.startsWith(
                        "false",
                        index
                    ) -> {
                        spannable.setSpan(
                            ForegroundColorSpan(COLORS["boolean"]!!),
                            index,
                            index + if (formattedJson.startsWith("true", index)) 4 else 5,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        index += if (formattedJson.startsWith("true", index)) 4 else 5
                    }

                    formattedJson.startsWith("null", index) -> {
                        spannable.setSpan(
                            ForegroundColorSpan(COLORS["null"]!!),
                            index,
                            index + 4,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        index += 4
                    }

                    formattedJson[index].isDigit() || formattedJson[index] == '-' -> {
                        val end = findEndOfNumber(formattedJson, index)
                        if (end > index) {
                            spannable.setSpan(
                                ForegroundColorSpan(COLORS["number"]!!),
                                index,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            index = end
                        } else index++
                    }

                    else -> index++
                }
            }

            textView.text = spannable

        } catch (_: Exception) {
            textView.text = jsonInput
//            textView.text = "Invalid JSON: ${e.message}"
        }
    }

    private fun findEndQuote(str: String, start: Int): Int {
        var index = start
        while (index < str.length) {
            if (str[index] == '"' && (index <= 0 || str[index - 1] != '\\')) {
                return index
            }
            index++
        }
        return -1
    }

    private fun findEndOfNumber(str: String, start: Int): Int {
        var index = start
        while (index < str.length && "+-0123456789.eE".contains(str[index])) {
            index++
        }
        return index
    }
}