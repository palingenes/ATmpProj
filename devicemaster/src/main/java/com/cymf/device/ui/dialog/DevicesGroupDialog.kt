package com.cymf.device.ui.dialog

import android.view.View
import android.widget.TextView
import com.cymf.device.R
import com.cymf.device.base.BaseDialogFragment
import com.cymf.device.databinding.DialogDeviceGroupBinding
import com.cymf.device.ktx.clickWithLimit

/**
 * @author wzy
 * >
 * @version V1.0.1
 * @time 2023/09/06 9:40
 * @desc 设备组
 */
class DevicesGroupDialog : BaseDialogFragment<DialogDeviceGroupBinding>() {

    private var onChangedListener: OnChangedListener? = null
    private var currId: Int = R.id.tv_sys

    override fun initView() {

        viewBinding.tvYingjian.clickWithLimit { callback(it) }
        viewBinding.tvNet.clickWithLimit { callback(it) }
        viewBinding.tvWendu.clickWithLimit { callback(it) }
        viewBinding.tvDianchi.clickWithLimit { callback(it) }
        viewBinding.tvSys.clickWithLimit { callback(it) }
        viewBinding.tvBuild.clickWithLimit { callback(it) }
        viewBinding.tvFenqu.clickWithLimit { callback(it) }
        viewBinding.tvNeicun.clickWithLimit { callback(it) }
        viewBinding.tvXiangji.clickWithLimit { callback(it) }
        viewBinding.tvAppList.clickWithLimit { callback(it) }
        viewBinding.tvBianyi.clickWithLimit { callback(it) }
        viewBinding.tvOutput.clickWithLimit { callback(it) }
        viewBinding.tvUsb.clickWithLimit { callback(it) }
        viewBinding.tvXinpian.clickWithLimit { callback(it) }
        viewBinding.tvMoniqi.clickWithLimit { callback(it) }
        viewBinding.tvDuokai.clickWithLimit { callback(it) }
        viewBinding.tvTiaoshi.clickWithLimit { callback(it) }
        viewBinding.tvRoot.clickWithLimit { callback(it) }
        viewBinding.tvHook.clickWithLimit { callback(it) }
        viewBinding.tvInfo.clickWithLimit { callback(it) }
        viewBinding.tvApp.clickWithLimit { callback(it) }
        viewBinding.tvWifi.clickWithLimit { callback(it) }
        viewBinding.tvMaps.clickWithLimit { callback(it) }
        viewBinding.tvLanya.clickWithLimit { callback(it) }
        viewBinding.tvOthers.clickWithLimit { callback(it) }

        for (i in 0..viewBinding.flex.childCount - 1) {
            val childAt = viewBinding.flex.getChildAt(i)
            if (!childAt.isEnabled) continue
            if (childAt.id == currId) {
                childAt.setBackgroundResource(R.drawable.shape_pink_border)
            } else {
                childAt.setBackgroundResource(R.drawable.bg_button_normal)
            }
        }
    }

    fun callback(view: View) {
        for (i in 0..viewBinding.flex.childCount - 1) {
            val childAt = viewBinding.flex.getChildAt(i)
            if (!childAt.isEnabled) continue
            if (childAt == view) {
                currId = view.id
                childAt.setBackgroundResource(R.drawable.shape_pink_border)
            } else {
                childAt.setBackgroundResource(R.drawable.bg_button_normal)
            }
        }
        onChangedListener?.onChanged(
            view.id, (if (view is TextView) view.text else "UNKNOWN").toString()
        )
        dismissAllowingStateLoss()
    }

    fun setOnChangedListener(listener: OnChangedListener) {
        this.onChangedListener = listener
    }

    interface OnChangedListener {
        fun onChanged(id: Int, text: String)
    }

    override fun theme() = 0
}