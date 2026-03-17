package com.cymf.tmp

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val it = "mCurrentFocus=Window{48c4bf u0 com.cymf.tmp/com.cymf.tmp.RootActivity}"
        val it2 = "mFocusedApp=AppWindowToken{e1d0df4 token=Token{71123c7 ActivityRecord{ee17106 u0 com.cymf.tmp/.RootActivity t4}}}"
        val it3 = "mObscuringWindow=Window{5ad4f92 u0 com.qtonz.wifiapp.scanwifi/com.wifianalyzer.showwifipassword.wifiqr.wifimaster.speedtest.activity.WelcomeBackActivity}"
        val name = parseComponentName(it)
        val name2 = parseComponentName(it2)
        val name3 = parseComponentName(it3)
        println("key=${name} ")
        println("key=${name2}  ")
        println("key=${name3}  ")
    }

    fun parseComponentName(input: String): String? {
        val startIndex = input.indexOf("u0 ") + 3
        val endIndex = input.indexOf("}", startIndex)
        if (startIndex < endIndex && endIndex != -1) {
            val text = input.substring(startIndex, endIndex).trim()
            if (text.contains(" ")) {
                val indexOf = text.indexOf(" ")
                return text.substring(0, indexOf)
            }
            return text
        }
        return null
    }
}