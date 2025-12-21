package com.wzy.testunity

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.wzy.testunity.bean.ChapterBuffer
import com.wzy.testunity.source.ShuShenGe
import com.wzy.testunity.tool.FoxEpubWriter
import com.wzy.testunity.tool.NovelParser
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.math.min


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MainUnitTest {

    @Test
    fun test111() {
        try {
            val chapters = NovelParser.parseNovel("C:\\Users\\wwzy\\Downloads\\黄泉逆行.txt")
            println("共解析 " + chapters.size + " 章节")

            // 打印前两章信息
            for (i in 0..<min(2, chapters.size)) {
                val ch = chapters.get(i)
                println("章节 " + ch.number + ": " + ch.name)
                println("内容行数: " + ch.content.size)
                if (!ch.content.isEmpty()) {
                    println("首行: " + ch.content.get(0))
                }
                println("---")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Test
    fun epub() {
        val name = "黄泉逆行.epub"
        val filePath = "C:\\Users\\wwzy\\Downloads\\$name"
        val foxEpubWriter = FoxEpubWriter(File(filePath), name)
        foxEpubWriter.setEpub(true)
        foxEpubWriter.setBookCreator("踏浪寻舟")
        val coverPath = "C:\\Users\\wwzy\\Downloads\\cover.jpg"
        foxEpubWriter.setCoverImage(File(coverPath))

        val books = NovelParser.parseNovel("C:\\Users\\wwzy\\Downloads\\黄泉逆行.txt")

        books.forEach(Consumer { chapterBuffer: ChapterBuffer? ->
            val content = StringBuilder()
            for (line in chapterBuffer!!.content) {
                content.append("<p>")
                content.append("    ")
                content.append(line)
                content.append("</p>")
            }
            foxEpubWriter.addChapter(chapterBuffer.name, content.toString())
        })
        foxEpubWriter.saveAll()
        println("保存成功 : $filePath")
    }

    @Test
    fun main() {
        //这两个参数请自行更换
        val bookName = "黄泉逆行"
        //存放目录，该目录是mac系统下的，windows需要自行更正“\\”
        val savePath = "C:\\Users\\wwzy\\Downloads\\logs"

        ShuShenGe(bookName, "https://www.shushenge.com/413477/index.html", savePath).let {
            //下载全部内容到一个txt文件里
            it.downloadTXT()
            println("下载完成")
            //下载epub格式，自动生成索引
//            it.downloadEPUB()
            //下载epub并转换为mobi格式
            //it.downloadMOBI();
        }

    }

    @Test
    fun play() {
        // 启动 Playwright（会自动管理浏览器进程）
        try {
            Playwright.create().use { playwright ->
                // 启动 Chromium（无头模式）
                val browser: com.microsoft.playwright.Browser = playwright.chromium().launch(
                    BrowserType.LaunchOptions()
                        .setHeadless(true) // 设为 false 可看到浏览器窗口（调试用）
                )
                // 创建新页面
                val page: Page = browser.newPage()
                // 设置超时（Cloudflare 验证可能需要几秒）
                page.setDefaultTimeout(30000.0) // 30秒
                // 访问目标 URL
                println("正在访问页面...")
                page.navigate("https://www.shushenge.com/413477/index.html")
                // 等待章节列表加载完成（关键！）
                // 选择器：章节目录中的第一个章节链接（如 "/413477/7.html"）
                page.waitForSelector(
                    "a[href^='/413477/']", Page.WaitForSelectorOptions()
                        .setTimeout(20000.0)
                )
                // 获取完整 HTML
                val html: String = page.content()
                // 打印前 500 字符验证是否成功
                println("✅ 成功获取页面！前500字符：")
                println(html.take(min(500, html.length)))
                Files.write(Paths.get("output.html"), html.toByteArray(Charsets.UTF_8))
                browser.close()
            }
        } catch (e: Exception) {
            System.err.println("❌ 抓取失败: " + e.message)
            e.printStackTrace()
        }
    }
}