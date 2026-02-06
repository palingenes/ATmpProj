package com.wzy.reader

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import com.wzy.reader.utils.DisplayUtil

/**
 * 表示阅读器中的一页文本内容
 *
 * @property content        本页显示的纯文本内容（已按屏幕尺寸分好行）
 * @property chapterIndex   所属章节索引（从0开始）
 * @property pageIndex      在当前章节中的页码（从0开始）
 * @property globalPageIndex 在全文中的全局页码（用于RecyclerView position）
 * @property isFirstPage    是否是章节的第一页（关键！用于吸附）
 * @property isLastPage     是否是章节的最后一页（关键！用于吸附）
 * @property chapterTitle   所属章节标题（如“第一章 初遇”）
 */
data class TextPage(
    val content: String,
    val chapterIndex: Int,
    val pageIndex: Int,
    val globalPageIndex: Int,
    val isFirstPage: Boolean = false,
    val isLastPage: Boolean = false,
    val chapterTitle: String = ""
) {
    companion object {
        /**
         * 从原始 TXT 文本构建分页列表（含章节识别）
         *
         * 支持的章节标记格式（可自定义正则）：
         *   - # 第一章
         *   - 第1章
         *   - CHAPTER ONE
         *   - ====== 分隔线 ======
         */
        fun buildPagesFromText(
            text: String,
            context: Context,
            config: TextLayoutConfig = TextLayoutConfig.DEFAULT,
            chapterRegex: Regex = Regex(
                """第[零一二三四五六七八九十百千\d]+[章节回卷集部篇]\s*.+""",
                RegexOption.IGNORE_CASE
            )
        ): List<TextPage> {
            val layoutPx = TextLayoutConfig.createWithPx(context, config)
            val usableHeight =
                getUsableScreenHeight(context) - layoutPx.paddingTopPx - layoutPx.paddingBottomPx

            // 分章
            val chapters = splitIntoChapters(text, chapterRegex)

            // 分页
            val allPages = mutableListOf<TextPage>()
            var globalIndex = 0

            for ((idx, chapter) in chapters.withIndex()) {
                val pagesInChapter = paginateChapter(
                    content = chapter.content,
                    chapterTitle = chapter.title,
                    chapterIndex = idx,
                    layoutPx = layoutPx,
                    usableHeight = usableHeight
                )

                // 标记首尾页
                if (pagesInChapter.isNotEmpty()) {
                    pagesInChapter[0] = pagesInChapter[0].copy(isFirstPage = true)
                    pagesInChapter[pagesInChapter.lastIndex] =
                        pagesInChapter.last().copy(isLastPage = true)
                }

                for (page in pagesInChapter) {
                    allPages.add(page.copy(globalPageIndex = globalIndex++))
                }
            }

            return allPages
        }

        private fun paginateChapter(
            content: String,
            chapterTitle: String,
            chapterIndex: Int,
            layoutPx: TextLayoutPx,
            usableHeight: Int
        ): MutableList<TextPage> {
            val paint = layoutPx.createTextPaint()
            val width = layoutPx.contentWidthPx

            val staticLayout = StaticLayout.Builder
                .obtain(content, 0, content.length, paint, width)
                .setIncludePad(false)
                .setLineSpacing(
                    layoutPx.config.lineSpacingExtraDp * Resources.getSystem().displayMetrics.density,
                    layoutPx.config.lineSpacingMultiplier
                )
                .build()

            val lineHeight = if (staticLayout.lineCount > 0) {
                staticLayout.getLineBottom(0) - staticLayout.getLineTop(0)
            } else {
                paint.fontSpacing.toInt()
            }

            val maxLinesPerPage = (usableHeight / lineHeight).coerceAtLeast(1)
            val pages = mutableListOf<TextPage>()
            var lineStart = 0

            while (lineStart < staticLayout.lineCount) {
                val lineEnd = minOf(lineStart + maxLinesPerPage, staticLayout.lineCount)
                val pageLines = StringBuilder()

                for (i in lineStart until lineEnd) {
                    val start = staticLayout.getLineStart(i)
                    val end = staticLayout.getLineEnd(i)
                    pageLines.append(content.substring(start, end))
                    if (i < lineEnd - 1) pageLines.append('\n')
                }

                pages.add(
                    TextPage(
                        content = pageLines.toString(),
                        chapterIndex = chapterIndex,
                        pageIndex = pages.size,
                        globalPageIndex = 0,
                        chapterTitle = chapterTitle
                    )
                )

                lineStart = lineEnd
            }

            return pages
        }

        private fun getUsableScreenHeight(context: Context): Int {
            val dm = context.resources.displayMetrics
            return dm.heightPixels - DisplayUtil.statusBarHeight() - DisplayUtil.bottomStatusHeight(
                context
            )
        }

        private fun splitIntoChapters(text: String, regex: Regex): List<Chapter> {
            val chapters = mutableListOf<Chapter>()
            var lastEnd = 0

            for (match in regex.findAll(text)) {
                // 添加上一章内容
                if (lastEnd < match.range.first) {
                    val prevContent = text.substring(lastEnd, match.range.first).trim()
                    if (prevContent.isNotEmpty()) {
                        chapters.add(Chapter("前言", prevContent))
                    }
                }

                // 当前章节标题和内容
                val title = match.value.trim()
                val start = match.range.last + 1
                val nextMatch = regex.find(text, start)
                val end = nextMatch?.range?.first ?: text.length

                val content = text.substring(start, end).trim()
                chapters.add(Chapter(title, content))

                lastEnd = end
            }

            // 处理最后一段（无标题）
            if (lastEnd < text.length) {
                val tail = text.substring(lastEnd).trim()
                if (tail.isNotEmpty()) {
                    chapters.add(Chapter("尾声", tail))
                }
            }

            return chapters.ifEmpty { listOf(Chapter("全文", text)) }
        }

        private fun paginateChapter(
            content: String,
            chapterTitle: String,
            chapterIndex: Int,
            width: Int,
            height: Int,
            context: Context,
            fontSizeSp: Float = 18f
        ): MutableList<TextPage> {
            val density = context.resources.displayMetrics.density
            val textSizePx = (fontSizeSp * density)

            val paint = TextPaint().apply {
                textSize = textSizePx
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            // ✅ 构建 StaticLayout
            val staticLayout = StaticLayout.Builder.obtain(content, 0, content.length, paint, width)
                .setIncludePad(false).setLineSpacing(0f, 1.0f).build()

            // ✅ 计算每页最大行数
            val lineHeight = if (staticLayout.lineCount > 0) {
                staticLayout.getLineBottom(0) - staticLayout.getLineTop(0)
            } else {
                paint.fontSpacing.toInt()
            }
            val maxLinesPerPage = (height / lineHeight).toInt().coerceAtLeast(1)

            val pages = mutableListOf<TextPage>()
            var lineStart = 0

            while (lineStart < staticLayout.lineCount) {
                val lineEnd = minOf(lineStart + maxLinesPerPage, staticLayout.lineCount)
                val pageLines = StringBuilder()

                // ✅ 逐行提取，保留原始换行语义
                for (i in lineStart until lineEnd) {
                    val start = staticLayout.getLineStart(i)
                    val end = staticLayout.getLineEnd(i) // 不包含行尾空白
                    pageLines.append(content, start, end)

                    // 只有在非最后一行时才加换行
                    if (i < lineEnd - 1) {
                        pageLines.append('\n')
                    }
                }

                pages.add(
                    TextPage(
                        content = pageLines.toString(),
                        chapterIndex = chapterIndex,
                        pageIndex = pages.size,
                        globalPageIndex = 0,
                        chapterTitle = chapterTitle
                    )
                )

                lineStart = lineEnd
            }
            return pages
        }
    }

    // 辅助类：章节
    private data class Chapter(val title: String, val content: String)
}