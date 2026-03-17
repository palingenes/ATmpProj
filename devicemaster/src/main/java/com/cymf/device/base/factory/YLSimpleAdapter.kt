package com.cymf.device.base.factory

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cymf.device.ktx.clickWithLimit

/**
 * 封装adapter linear
 *
 * @param <T>
</T> */
abstract class YLSimpleAdapter<T> : RecyclerView.Adapter<YLViewHolder> {

    private var mOnClickItemListener: OnClickItemListener? = null
    private var mOnLongClickItemListener: OnLongClickItemListener? = null
    private var mDataList: MutableList<T>? = ArrayList()

    @JvmField
    protected var mContext: Context
    private val mInflater: LayoutInflater

    constructor(context: Context) {
        mContext = context
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    constructor(context: Context, data: MutableList<T>?) {
        mContext = context
        mDataList = data
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YLViewHolder {
        val itemView = mInflater.inflate(layoutId, parent, false)
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
            if (mOnClickItemListener != null) {
                mOnClickItemListener!!.onClick(holder.itemView, position)
            }
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
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            onBindItemHolder(holder, position, payloads)
        }
    }

    abstract val layoutId: Int
    abstract fun onBindItemHolder(holder: YLViewHolder, position: Int)

    @Suppress("UNUSED_PARAMETER")
    fun onBindItemHolder(holder: YLViewHolder?, position: Int, payloads: List<Any>?) {
        //注意：payloads的size总是1
//        String payload = (String)payloads.get(0);
//        TLog.error("payload = " + payload);
//
//        TextView textView = holder.getView(R.id.info_text);
//        //需要更新的控件
//        ItemModel itemModel = mDataList.get(position);
//        textView.setText(itemModel.title);
    }

    override fun getItemCount(): Int {
        return if (mDataList == null) 0 else mDataList!!.size
    }

    @set:SuppressLint("NotifyDataSetChanged")
    var dataList: MutableList<T>?
        get() = mDataList
        set(list) {
            mDataList?.clear()
            if (list?.isNotEmpty() == true) {
                mDataList?.addAll(list)
            }
            notifyDataSetChanged()
        }

    fun addAll(list: Collection<T>) {
        val lastIndex = mDataList?.size ?: 0
        if (mDataList?.addAll(list) == true) {
            notifyItemRangeInserted(lastIndex, list.size)
        }
    }

    fun setNewData(list: MutableList<T>?) {
        if (list == null) return
        mDataList = list
        notifyItemRangeChanged(0, list.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun remove(position: Int) {
        mDataList!!.removeAt(position)
        notifyItemRemoved(position)
        if (position != dataList!!.size) { // 如果移除的是最后一个，忽略
            notifyItemRangeChanged(position, mDataList!!.size - position)
        }
        //Scrapped or attached views may not be recycled. isScrap:false isAttached:true
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        if (mDataList != null) {
            mDataList!!.clear()
        }
        mDataList = ArrayList()
        notifyDataSetChanged()
    }

    interface OnClickItemListener {
        fun onClick(view: View?, position: Int)
    }

    interface OnLongClickItemListener {
        fun onClick(view: View?, position: Int): Boolean
    }

    fun setOnClickItemListener(mOnClickItemListener: OnClickItemListener?) {
        this.mOnClickItemListener = mOnClickItemListener
    }

    fun setOnLongClickItemListener(onLongClickItemListener: OnLongClickItemListener?) {
        mOnLongClickItemListener = onLongClickItemListener
    }
}
