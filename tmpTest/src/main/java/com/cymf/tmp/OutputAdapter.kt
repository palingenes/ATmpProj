package com.cymf.tmp

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OutputAdapter : RecyclerView.Adapter<OutputAdapter.ViewHolder>() {

    private val items = mutableListOf<OutputItem>()

    fun addOutput(item: OutputItem) {
        items.add(item)
        if (items.size > MAX_LINES) {
            items.removeAt(0)
            notifyItemRangeRemoved(0, 1)
            notifyItemRangeChanged(0, items.size)
        } else {
            notifyItemInserted(items.size - 1)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearData() {
        try {
            notifyItemRangeRemoved(0, items.size)
            items.clear()
        } catch (_: Exception) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_output, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val spannable = SpannableString(item.text).apply {
            setSpan(
                ForegroundColorSpan(item.color),
                0,
                item.text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        holder.textView.text = spannable
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.outputItemText)
    }

    companion object {
        const val MAX_LINES = 1000 // 控制最大行数，防止内存溢出
    }
}

data class OutputItem(val text: String, val color: Int = Color.BLACK)