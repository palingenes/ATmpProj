package com.cymf.keyshot.bean

import android.graphics.Rect

data class NodeInfoData(
    val text: CharSequence? = null,
    val contentDescription: CharSequence? = null,
    val viewId: String? = null,
    val className: String? = null,
    val bounds: Rect? = null,
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val depth: Int = 0
)