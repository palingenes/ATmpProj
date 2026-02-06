package com.wzy.reader

import android.content.Context
import android.graphics.Typeface
import android.text.TextPaint

/**
 *  TXT 分页器
 */
class TxtPageLoader(private val context: Context, private val text: String) {

    fun loadPages(width: Int, height: Int): List<String> {
        val pages = mutableListOf<String>()
        val paint = TextPaint().apply {
            textSize = 18f * context.resources.displayMetrics.density
            typeface = Typeface.DEFAULT
        }

        val words = text.split("".toRegex()).dropLastWhile { it.isEmpty() }
        var currentLine = StringBuilder()
        var currentPage = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine$word"
            if (paint.measureText(testLine) <= width) {
                currentLine = StringBuilder(testLine)
            } else {
                // 换行
                if ((currentPage.lines().size + 1) * paint.fontSpacing > height) {
                    pages.add(currentPage.toString())
                    currentPage = StringBuilder("$currentLine\n")
                } else {
                    currentPage.append("$currentLine\n")
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            currentPage.append(currentLine)
        }
        if (currentPage.isNotEmpty()) {
            pages.add(currentPage.toString())
        }

        return pages
    }
}