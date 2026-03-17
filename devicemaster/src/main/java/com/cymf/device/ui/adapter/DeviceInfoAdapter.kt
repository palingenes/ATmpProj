package com.cymf.device.ui.adapter

import android.content.Context
import androidx.core.util.Pair
import com.cymf.device.R
import com.cymf.device.base.factory.YLSimpleAdapter
import com.cymf.device.base.factory.YLViewHolder
import com.cymf.device.ktx.res2Color

class DeviceInfoAdapter(context: Context) : YLSimpleAdapter<Pair<String?, String?>?>(context) {

    override val layoutId = R.layout.item_normal

    override fun onBindItemHolder(holder: YLViewHolder, position: Int) {
        val item = dataList?.get(position) ?: return
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(res2Color(R.color.color_999))
        } else {
            holder.itemView.setBackgroundColor(res2Color(R.color.color_777))
        }
        holder.setText(R.id.tv_key, item.first)
        holder.setText(R.id.tv_value, item.second)
    }
}