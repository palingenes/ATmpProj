package com.cymf.device.base.factory

import android.annotation.SuppressLint
import android.content.Context
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.cymf.device.ktx.clickWithLimit

/**
 * 多个item类型使用
 *
 * @param <T>
</T> */
abstract class YLBaseMultiAdapter<T>(protected var mContext: Context) :
    RecyclerView.Adapter<YLViewHolder>() {

    private val mInflater =
        mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    /**
     * layouts indexed with their types
     */
    private var layouts: SparseIntArray? = null
    protected var mDataList: MutableList<T>? = ArrayList()
    private var mOnClickItemListener: OnClickItemListener? = null
    private var mOnLongClickItemListener: OnLongClickItemListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YLViewHolder {
        val itemView = mInflater.inflate(getLayoutId(viewType), parent, false)
        return YLViewHolder(itemView, mContext)
    }

    override fun onBindViewHolder(holder: YLViewHolder, position: Int) {
        if (position < 0 || position >= itemCount) return
        holder.itemView.clickWithLimit(10) {
            mOnClickItemListener?.onClick(holder.itemView, position)
        }
        holder.itemView.setOnLongClickListener {
            if (mOnLongClickItemListener != null) {
                return@setOnLongClickListener mOnLongClickItemListener!!.onClick(
                    holder.itemView,
                    position
                )
            }
            false
        }
        onBindItemHolder(holder, position)
    }

    //局部刷新关键：带payload的这个onBindViewHolder方法必须实现
    override fun onBindViewHolder(holder: YLViewHolder, position: Int, payloads: List<Any>) {
        holder.itemView.clickWithLimit(10) {
            mOnClickItemListener?.onClick(holder.itemView, position)
        }
        holder.itemView.setOnLongClickListener {
            return@setOnLongClickListener mOnLongClickItemListener?.onClick(
                holder.itemView,
                position
            ) == true
            @Suppress("UNREACHABLE_CODE")
            return@setOnLongClickListener false
        }
        if (payloads.isEmpty()) {
            onBindItemHolder(holder, position)
        } else {
            onBindItemHolder(holder, position, payloads)
        }
    }

    //根据ViewType获取LayoutId
    fun getLayoutId(viewType: Int): Int {
        return layouts!![viewType]
    }

    protected fun addItemType(type: Int, @LayoutRes layoutResId: Int) {
        if (layouts == null) {
            layouts = SparseIntArray()
        }
        layouts!!.put(type, layoutResId)
    }

    abstract fun onBindItemHolder(holder: YLViewHolder, position: Int)

    @Suppress("UNUSED_PARAMETER")
    fun onBindItemHolder(holder: YLViewHolder?, position: Int, payloads: List<Any>?) {
    }

    override fun getItemCount(): Int {
        return mDataList?.size ?: 0
    }

    @set:SuppressLint("NotifyDataSetChanged")
    open var dataList: MutableList<T>?
        get() = mDataList
        set(list) {
            mDataList?.clear()
            list?.let { mDataList?.addAll(it) }
            notifyDataSetChanged()
        }

    fun addAll(list: Collection<T>?) {
        if (list.isNullOrEmpty()) return
        val lastIndex = mDataList!!.size
        if (mDataList!!.addAll(list)) {
            notifyItemRangeInserted(lastIndex, list.size)
        }
    }

    fun addData(data: T) {
        mDataList?.apply {
            val lastIndex = size
            if (add(data)) {
                notifyItemRangeInserted(lastIndex, 1)
            }
        }
    }

    fun addData(index: Int, data: T) {
        try {
            mDataList?.add(index, data)
            notifyItemRangeInserted(index, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun moveData(index: Int, data: T) {
        try {
            mDataList?.apply {
                this@YLBaseMultiAdapter.remove(data)
                if (index < 0) {
                    this@YLBaseMultiAdapter.addData(data)
                } else {
                    this@YLBaseMultiAdapter.addData(index, data)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun remove(position: Int) {
        mDataList?.removeAt(position)
        notifyItemRemoved(position)
        if (position != dataList?.size) { // 如果移除的是最后一个，忽略
            mDataList?.size?.let {
                notifyItemRangeChanged(position, it - position)
            }
        }
    }

    fun remove(data: T) {
        mDataList?.indexOf(data)?.let {
            mDataList?.removeAt(it)
            notifyItemRemoved(it)
            if (it != dataList?.size) { // 如果移除的是最后一个，忽略
                mDataList?.size?.apply {
                    notifyItemRangeChanged(it, this - it)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        mDataList?.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    @JvmOverloads
    fun expand(
        @IntRange(from = 0) index: Int,
        animate: Boolean = true,
        shouldNotify: Boolean = true
    ): Int {
        var position = index
        position -= headerLayoutCount
        val expandable = getExpandableItem(position) ?: return 0
        if (!hasSubItems(expandable)) {
            expandable.isExpanded = false
            return 0
        }
        var subItemCount = 0
        if (!expandable.isExpanded) {
            val list = expandable.subItems
            list?.let {
                mDataList!!.addAll(position + 1, it)
                subItemCount += recursiveExpand(position + 1, it)
                expandable.isExpanded = true
                subItemCount += it.size
            }
        }
        val parentPos = position + headerLayoutCount
        if (shouldNotify) {
            if (animate) {
                notifyItemChanged(parentPos)
                notifyItemRangeInserted(parentPos + 1, subItemCount)
            } else {
                notifyDataSetChanged()
            }
        }
        return subItemCount
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun expandAll(index: Int, animate: Boolean, notify: Boolean): Int {
        var position = index
        position -= headerLayoutCount
        var endItem: T? = null
        if (position + 1 < mDataList!!.size) {
            endItem = getItem(position + 1)
        }
        val expandable = getExpandableItem(position)
        if (!hasSubItems(expandable)) {
            return 0
        }
        var count = expand(position + headerLayoutCount, animate = false, shouldNotify = false)
        for (i in position + 1 until mDataList!!.size) {
            val item = getItem(i)
            if (item === endItem) {
                break
            }
            if (isExpandable(item)) {
                count += expand(i + headerLayoutCount, animate = false, shouldNotify = false)
            }
        }
        if (notify) {
            if (animate) {
                notifyItemRangeInserted(position + headerLayoutCount + 1, count)
            } else {
                notifyDataSetChanged()
            }
        }
        return count
    }

    /**
     * expand the item and all its subItems
     *
     * @param position position of the item, which includes the header layouts count.
     * @param init     whether you are initializing the recyclerView or not.
     * if **true**, it won't notify recyclerView to redraw UI.
     * @return the number of items that have been added to the adapter.
     */
    fun expandAll(position: Int, init: Boolean): Int {
        return expandAll(position, true, !init)
    }

    @Suppress("UNCHECKED_CAST")
    private fun recursiveCollapse(@IntRange(from = 0) position: Int): Int {
        val item = getItem(position)
        if (!isExpandable(item)) {
            return 0
        }
        val expandable = item as IExpandable<T>
        var subItemCount = 0
        if (expandable.isExpanded) {
            val subItems = expandable.subItems
            for (i in subItems!!.indices.reversed()) {
                val subItem = subItems[i]
                val pos = getItemPosition(subItem)
                if (pos < 0) {
                    continue
                }
                if (subItem is IExpandable<*>) {
                    subItemCount += recursiveCollapse(pos)
                }
                mDataList!!.removeAt(pos)
                subItemCount++
            }
        }
        return subItemCount
    }

    @Suppress("UNCHECKED_CAST")
    private fun recursiveExpand(position: Int, list: MutableList<T>): Int {
        var count = 0
        var pos = position + list.size - 1
        var i = list.size - 1
        while (i >= 0) {
            if (list[i] is IExpandable<*>) {
                val item: IExpandable<T> = list[i] as IExpandable<T>
                if (item.isExpanded && hasSubItems(item)) {
                    val subList = item.subItems
                    mDataList!!.addAll(pos + 1, subList!!)
                    val subItemCount = recursiveExpand(pos + 1, subList)
                    count += subItemCount
                }
            }
            i--
            pos--
        }
        return count
    }

    private fun hasSubItems(item: IExpandable<T>?): Boolean {
        val list = item!!.subItems
        return list != null && list.size > 0
    }

    private fun isExpandable(item: T): Boolean {
        return item is IExpandable<*>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getExpandableItem(position: Int): IExpandable<T>? {
        val item = getItem(position)
        return if (isExpandable(item)) {
            item as IExpandable<T>
        } else {
            null
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @JvmOverloads
    fun collapse(
        @IntRange(from = 0) index: Int,
        animate: Boolean = true,
        notify: Boolean = true
    ): Int {
        var position = index
        position -= headerLayoutCount
        val expandable = getExpandableItem(position) ?: return 0
        val subItemCount = recursiveCollapse(position)
        expandable.isExpanded = false
        val parentPos = position + headerLayoutCount
        if (notify) {
            if (animate) {
                notifyItemChanged(parentPos)
                notifyItemRangeRemoved(parentPos + 1, subItemCount)
            } else {
                notifyDataSetChanged()
            }
        }
        return subItemCount
    }

    private fun getItemPosition(item: T?): Int {
        return if (item != null && mDataList != null && mDataList!!.isNotEmpty()) mDataList!!.indexOf(
            item
        ) else -1
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     * data set.
     * @return The data at the specified position.
     */
    fun getItem(position: Int): T {
        return mDataList!![position]
    }

    private val headerLayoutCount: Int
        /**
         * if addHeaderView will be return 1, if not will be return 0
         */
        get() = 0

    interface OnClickItemListener {
        fun onClick(view: View?, position: Int)
    }

    interface OnLongClickItemListener {
        fun onClick(view: View?, position: Int): Boolean
    }

    open fun setOnClickItemListener(itemListener: OnClickItemListener?) {
        this.mOnClickItemListener = itemListener
    }

    fun setOnLongClickItemListener(onLongClickItemListener: OnLongClickItemListener?) {
        mOnLongClickItemListener = onLongClickItemListener
    }
}
