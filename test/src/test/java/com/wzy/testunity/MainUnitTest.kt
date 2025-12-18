package com.wzy.testunity

import com.wzy.testunity.bean.ChapterBuffer
import com.wzy.testunity.source.Bishenge
import com.wzy.testunity.tool.FoxEpubWriter
import com.wzy.testunity.tool.NovelParser
import org.junit.Test
import java.io.File
import java.io.IOException
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
        foxEpubWriter.setCoverImage( File(coverPath))

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

        Bishenge(bookName, "https://www.shushenge.com/413477/index.html", savePath).let {
            //下载全部内容到一个txt文件里
            it.downloadTXT()
            println("下载完成")
            //下载epub格式，自动生成索引
//            it.downloadEPUB()
            //下载epub并转换为mobi格式
            //it.downloadMOBI();
        }

    }
}