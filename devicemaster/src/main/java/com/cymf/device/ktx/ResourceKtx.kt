package com.cymf.device.ktx

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.cymf.device.App

fun res2Color(colorId: Int): Int {
    return App.app.resources.getColor(colorId, App.app.theme)
}

fun inflate(viewId: Int, root: ViewGroup?): View? {
    return LayoutInflater.from(App.app).inflate(viewId, root, false)
}

fun res2Dimen(@DimenRes dimenId: Int): Int {
    return App.app.resources.getDimensionPixelSize(dimenId)
}

fun res2Drawable(@DrawableRes drawId: Int): Drawable? {
    return ResourcesCompat.getDrawable(App.context.resources, drawId, App.app.theme)
}

fun stringOf(@StringRes id: Int, vararg formatArgs: Any): String = getString(id, *formatArgs)

fun stringOf(@StringRes id: Int): String = getString(id)

fun getString(@StringRes id: Int, vararg formatArgs: Any?): String {
    return App.app.resources.getString(id, *formatArgs)
}