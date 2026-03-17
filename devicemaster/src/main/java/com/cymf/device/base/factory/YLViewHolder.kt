package com.cymf.device.base.factory

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView

/**
 * ViewHolder基类
 */
class YLViewHolder(itemView: View?, private val mContext: Context) :
    RecyclerView.ViewHolder(itemView!!) {

    private val mViews: SparseArray<View?> = SparseArray()

    @Suppress("UNCHECKED_CAST")
    fun <T : View?> getView(viewId: Int): T? {
        var view = mViews[viewId]
        if (view == null) {
            view = itemView.findViewById(viewId)
            mViews.put(viewId, view)
        }
        return view as T?
    }

    fun setText(viewId: Int, value: CharSequence?): YLViewHolder {
        setText(viewId, value, "")
        return this
    }

    fun setTextUseFallbackLineSpacing(viewId: Int, value: CharSequence?): YLViewHolder {
        getView<TextView>(viewId)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isFallbackLineSpacing = false
            }
            text = if (TextUtils.isEmpty(value)) "" else value
        }
        return this
    }

    fun setText(viewId: Int, value: CharSequence?, def: CharSequence?): YLViewHolder {
        val textView = getView<TextView>(viewId)
        textView?.text = if (TextUtils.isEmpty(value)) def else value
        return this
    }

    fun setImageViewByBitmap(viewId: Int, bmp: Bitmap?): YLViewHolder {
        val view = getView<ImageView>(viewId)
        view?.setImageBitmap(bmp)
        return this
    }

    fun setTextColor(viewId: Int, textColor: Int): YLViewHolder {
        val view = getView<TextView>(viewId)
        view?.setTextColor(textColor)
        return this
    }

    fun setTextColorRes(viewId: Int, @ColorRes textColorRes: Int): YLViewHolder {
        val view = getView<TextView>(viewId)
        view?.setTextColor(mContext.resources.getColor(textColorRes, mContext.theme))
        return this
    }

    fun setTextColor(@ColorRes textColorRes: Int, vararg viewIds: Int) {
        for (viewId in viewIds) {
            val view = getView<TextView>(viewId)
            view?.setTextColor(mContext.resources.getColor(textColorRes, mContext.theme))
        }
    }

    fun setTextColor(@ColorRes textColorRes: Int, vararg views: TextView) {
        for (view in views) {
            view.setTextColor(mContext.resources.getColor(textColorRes, mContext.theme))
        }
    }

    fun setTextSize(txtSize: Float, vararg viewIds: Int) {
        for (viewId in viewIds) {
            val view = getView<TextView>(viewId)
            view?.textSize = txtSize
        }
    }

    fun setLineSpacing(add: Float, multiple: Float, vararg viewIds: Int) {
        for (viewId in viewIds) {
            val view = getView<TextView>(viewId)
            view?.setLineSpacing(add, multiple)
        }
    }

    fun setImageResource(viewId: Int, imageResId: Int): YLViewHolder {
        val view = getView<ImageView>(viewId)
        view?.setImageResource(imageResId)
        return this
    }

    fun removeImageResource(viewId: Int): YLViewHolder {
        val view = getView<ImageView>(viewId)
        view?.setImageDrawable(null)
        return this
    }

    fun setBackgroundColor(viewId: Int, color: Int) {
        val view = getView<View>(viewId)
        view?.setBackgroundColor(color)
    }

    fun setBackgroundResource(viewId: Int, backgroundRes: Int): YLViewHolder {
        return setBackgroundResource(viewId, backgroundRes, backgroundRes)
    }

    fun setBackgroundResource(viewId: Int, backgroundRes: Int, tag: Any): YLViewHolder {
        val tag1 = getTag(viewId)
        if (tag1 == tag) {
            return this //  防止重复设置
        }
        val view = getView<View>(viewId)
        view?.setBackgroundResource(backgroundRes)
        view?.tag = tag
        return this
    }

    fun backgroundTintList(viewId: Int, tintColor: String): YLViewHolder {
        val tag1 = getTag(viewId)
        if (tag1 == tintColor) {
            return this //  防止重复设置
        }
        getView<View>(viewId)?.apply {
            backgroundTintList = ColorStateList.valueOf(Color.parseColor(tintColor))
            tag = tintColor
        }
        return this
    }

    fun backgroundTintList(viewId: Int): YLViewHolder {
        getView<View>(viewId)?.apply {
            backgroundTintList = null
        }
        return this
    }

    fun setVisible(viewId: Int, visible: Boolean) {
        getView<View>(viewId)?.apply {
            visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    fun setTypeface(typeface: Typeface?, vararg viewIds: Int) {
        for (viewId in viewIds) {
            getView<TextView>(viewId)?.apply {
                this.typeface = typeface
                this.paintFlags = paintFlags or Paint.SUBPIXEL_TEXT_FLAG
            }
        }
    }

    fun setOnClickListener(viewId: Int, listener: View.OnClickListener?): YLViewHolder {
        val view = getView<View>(viewId)
        view?.setOnClickListener(listener)
        return this
    }

    fun setOnTouchListener(viewId: Int, listener: OnTouchListener?): YLViewHolder {
        getView<View>(viewId)?.setOnTouchListener(listener)
        return this
    }

    fun setOnLongClickListener(viewId: Int, listener: OnLongClickListener?): YLViewHolder {
        getView<View>(viewId)?.setOnLongClickListener(listener)
        return this
    }

    fun setTag(viewId: Int, tag: Any?): YLViewHolder {
        val view = getView<View>(viewId)
        view?.tag = tag
        return this
    }

    fun getTag(viewId: Int): Any? {
        val view = getView<View>(viewId)
        return view?.tag
    }
}
