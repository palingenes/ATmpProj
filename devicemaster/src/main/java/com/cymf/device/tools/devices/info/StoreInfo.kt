package com.cymf.device.tools.devices.info

import android.content.Context
import com.cymf.device.tools.devices.beans.StorageBean
import com.cymf.device.tools.devices.utils.MemoryUtils
import com.cymf.device.tools.devices.utils.SdUtils

/**
 * 内存信息
 */
object StoreInfo {
    /**
     * 获取内存信息
     *
     */
    fun getStoreInfo(context: Context?): MutableList<StorageBean?> {
        val list: MutableList<StorageBean?> = ArrayList<StorageBean?>()
        val bean = StorageBean()
        SdUtils.getStoreInfo(context, bean)
        MemoryUtils.getMemoryInfo(context, bean)
        list.add(bean)
        return list
    }
}
