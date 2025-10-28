package com.cymf.autogame.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cymf.autogame.R
import com.cymf.autogame.bean.LogItem
import com.cymf.autogame.utils.DisplayUtil

class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val strokeDp by lazy { DisplayUtil.dip2px(2f) }
    private val logs = mutableListOf<LogItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val logItem = logs[position]

        holder.tvEmoji.text = logItem.emoji
        holder.tvTime.text = logItem.ts
        holder.tvMessage.text = logItem.message
        logItem.pkgName?.let {
            holder.tvPkgName.visibility = View.VISIBLE
            holder.tvPkgName.text = "包名：${it}"
        } ?: let {
            holder.tvPkgName.visibility = View.GONE
        }
        holder.tvMessage.setTextColor(logItem.level.color)
        holder.background?.setStroke(strokeDp, logItem.level.borderColor)
    }

    override fun getItemCount(): Int = logs.size

    fun updateLogs(newLogs: List<LogItem>) {
        val diffCallback = LogDiffCallback(logs, newLogs.takeLast(500))
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        logs.clear()
        logs.addAll(newLogs.takeLast(500))
        diffResult.dispatchUpdatesTo(this)
    }

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bindings = mutableMapOf<Int, View>()

        @Suppress("UNCHECKED_CAST")
        fun <T : View> bind(@IdRes id: Int): Lazy<T> {
            return lazy {
                bindings.getOrPut(id) { itemView.findViewById(id) } as T
            }
        }

        val background: GradientDrawable? by lazy {
            val bind by bind<View>(R.id.container)
            bind.background as? GradientDrawable
        }
        val tvEmoji: TextView by bind(R.id.textViewEmoji)
        val tvTime: TextView by bind(R.id.textViewTime)
        val tvMessage: TextView by bind(R.id.textViewMessage)
        val tvPkgName: TextView by bind(R.id.textViewTag)
    }
}

class LogDiffCallback(
    private val oldList: List<LogItem>,
    private val newList: List<LogItem>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] === newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem
    }
}