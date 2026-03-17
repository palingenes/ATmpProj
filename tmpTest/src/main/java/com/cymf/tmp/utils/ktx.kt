package com.cymf.tmp.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.toColorInt

fun SpannableStringBuilder.appendTitle(text: String) {
    val ss = SpannableString(text)
    ss.setSpan(
        ForegroundColorSpan("#FF6200".toColorInt()),
        0,
        text.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    this.append(ss)
}

fun SpannableStringBuilder.appendKey(text: String) {
    val ss = SpannableString(text)
    ss.setSpan(
        ForegroundColorSpan("#757575".toColorInt()),
        0,
        text.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    this.append(ss)
}

fun SpannableStringBuilder.appendLine(line: String) {
    val ss = SpannableString(line)
    ss.setSpan(
        ForegroundColorSpan("#AAAAAA".toColorInt()),
        0,
        line.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    this.append(ss)
}

fun SpannableStringBuilder.appendDividerLine(line: String) {
    val ss = SpannableString(line)
    ss.setSpan(
        ForegroundColorSpan("#000000".toColorInt()),
        0,
        line.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    this.append(ss)
}