package com.cymf.autogame.bean

import android.graphics.Color

data class LogItem(
    val level: LogLevel,
    val emoji: String,
    val message: String,
    val pkgName: String? = null,
    var ts: String? = null
)

enum class LogLevel {
    NORMAL(Color.BLACK, Color.TRANSPARENT),
    LV_1(Color.RED, 0xFFffdd00.toInt()),
    LV_2(Color.BLUE, 0xFFea00ff.toInt());

    val color: Int
    val borderColor: Int

    constructor(color: Int, borderColor: Int) {
        this.color = color
        this.borderColor = borderColor
    }
}