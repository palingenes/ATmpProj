package com.cymf.tmp.utils

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

object AnsiColorizer {

    private val ansiRegex = Regex("\u001B\$$[0-9;]*m")

    fun applyAnsiColors(text: String): SpannableString {
        val spannable = SpannableString(text)
        val matcher = ansiRegex.toPattern().matcher(text)

        var currentIndex = 0
        while (matcher.find(currentIndex)) {
            val start = matcher.start()
            val end = matcher.end()
            val code = text.substring(start + 2, end - 1)

            val color = when (code) {
                "0" -> Color.BLACK
                "31" -> Color.RED
                "32" -> Color.GREEN
                "33" -> Color.YELLOW
                "34" -> Color.BLUE
                "35" -> Color.MAGENTA
                "36" -> Color.CYAN
                "37" -> Color.WHITE
                else -> Color.BLACK
            }

            spannable.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            currentIndex = end
        }
        return spannable
    }
}