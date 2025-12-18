package com.wzy.testunity

import com.wzy.testunity.source.Bishenge
import org.junit.Test


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MainUnitTest {


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