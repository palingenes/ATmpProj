package com.wzy.reader

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PageAdapter(val pages: List<TextPage>, private val layoutPx: TextLayoutPx) :
    RecyclerView.Adapter<PageViewHolder>() {

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_FIRST = 1
        const val TYPE_LAST = 2
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val textView = TextView(parent.context)
        return PageViewHolder(textView, layoutPx)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.textView.text = pages[position].content
        // 标记页面类型（关键！）
        holder.itemView.tag = when {
            pages[position].isFirstPage -> TYPE_FIRST
            pages[position].isLastPage -> TYPE_LAST
            else -> TYPE_NORMAL
        }
    }

    override fun getItemCount() = pages.size
}

class PageViewHolder(val textView: TextView, layoutPx: TextLayoutPx) :
    RecyclerView.ViewHolder(textView) {
    init {
        layoutPx.applyToTextView(textView)
    }
}