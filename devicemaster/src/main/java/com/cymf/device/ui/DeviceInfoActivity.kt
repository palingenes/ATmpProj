package com.cymf.device.ui

import android.view.View
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.cymf.device.App
import com.cymf.device.R
import com.cymf.device.base.BaseActivity
import com.cymf.device.base.factory.YLSimpleAdapter
import com.cymf.device.databinding.ActivityDeviceInfoBinding
import com.cymf.device.ktx.gone
import com.cymf.device.ktx.invisible
import com.cymf.device.ktx.visible
import com.cymf.device.tools.devices.info.AppInfo
import com.cymf.device.tools.devices.info.BatteryInfo
import com.cymf.device.tools.devices.info.BluetoothInfo
import com.cymf.device.tools.devices.info.BuildInfo
import com.cymf.device.tools.devices.info.CameraInfo
import com.cymf.device.tools.devices.info.DebugInfo
import com.cymf.device.tools.devices.info.DeviceInfo
import com.cymf.device.tools.devices.info.EmulatorInfo
import com.cymf.device.tools.devices.info.HardwareInfo
import com.cymf.device.tools.devices.info.HookInfo
import com.cymf.device.tools.devices.info.MediaCodecInfo
import com.cymf.device.tools.devices.info.NetWorkInfo
import com.cymf.device.tools.devices.info.OthersInfo
import com.cymf.device.tools.devices.info.RootInfo
import com.cymf.device.tools.devices.info.SOCInfo
import com.cymf.device.tools.devices.info.SystemInfo
import com.cymf.device.tools.devices.info.ThermalInfo
import com.cymf.device.tools.devices.info.USBInfo
import com.cymf.device.tools.devices.info.VirtualAppInfo
import com.cymf.device.tools.devices.info.WifiInfo
import com.cymf.device.ui.adapter.DeviceInfoAdapter
import com.cymf.device.ui.dialog.DevicesGroupDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * @time 2025/04/17 15:15
 * @version V1.0.1
 * @author wzy
 * </>
 * @desc 设备信息
 */
class DeviceInfoActivity : BaseActivity<ActivityDeviceInfoBinding>() {

    private var devicesGroupDialog: DevicesGroupDialog? = null
    private val adapter by lazy { DeviceInfoAdapter(this) }

    init {
        System.loadLibrary("native-lib")
    }

    override fun initView() {
        setTitleText("设备信息")
        find<View>(R.id.ic_back).invisible()

        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = adapter
        adapter.setOnClickItemListener(object : YLSimpleAdapter.OnClickItemListener {
            override fun onClick(view: View?, position: Int) {
                ToastUtils.showShort(adapter.dataList?.get(position)?.second)
            }
        })
        viewBinding.tvMore.setOnClickListener {
            showGenderFragment()
        }
    }

    override fun initData() {
        viewBinding.tvEmpty.visible()
        updateDataInfo(R.id.tv_sys)
    }

    private fun showGenderFragment() {
        lifecycleScope.launch {
            withResumed {
                if (devicesGroupDialog == null) {
                    devicesGroupDialog = DevicesGroupDialog()
                }
                devicesGroupDialog?.setOnChangedListener(object :
                    DevicesGroupDialog.OnChangedListener {
                    override fun onChanged(id: Int, text: String) {
                        updateDataInfo(id)
                        viewBinding.tvConfig.text = text
                    }
                })
                if (devicesGroupDialog?.isResumed == false) {
                    devicesGroupDialog?.showNow(
                        supportFragmentManager,
                        this.javaClass.name
                    )
                }
            }
        }
    }

    fun updateDataInfo(id: Int) {
        val flow = flow {
            when (id) {
                R.id.tv_yingjian -> emit(HardwareInfo.getHardwareInfo(this@DeviceInfoActivity))
                R.id.tv_net -> emit(NetWorkInfo.getNetWorkInfo(this@DeviceInfoActivity))
                R.id.tv_wendu -> emit(ThermalInfo.thermalInfo)
                R.id.tv_dianchi -> emit(BatteryInfo.getBatteryInfo(this@DeviceInfoActivity))
                R.id.tv_sys -> emit(SystemInfo.getSystemInfo(this@DeviceInfoActivity))
                R.id.tv_build -> emit(BuildInfo.getBuildInfo())
//                R.id.tv_fenqu -> emit(PartitionsInfo.partitionsInfo)
//                R.id.tv_neicun -> emit(StoreInfo.getStoreInfo(this@DeviceInfoActivity))
                R.id.tv_xiangji -> emit(CameraInfo.getCameraInfo(App.app))
//                R.id.tv_app_list -> emit(AppListInfo.getAppListInfo(this@DeviceInfoActivity))
                R.id.tv_bianyi -> emit(MediaCodecInfo.codeCInfo)
//                R.id.tv_output -> emit(InputInfo.getInputInfo())
                R.id.tv_usb -> emit(USBInfo.getUSBInfo(this@DeviceInfoActivity))
                R.id.tv_xinpian -> emit(SOCInfo.getSOCInfo(this@DeviceInfoActivity))
                R.id.tv_moniqi -> emit(EmulatorInfo.getEmulatorInfo(this@DeviceInfoActivity))
                R.id.tv_duokai -> emit(VirtualAppInfo.getVirtualAppInfo(this@DeviceInfoActivity))
                R.id.tv_tiaoshi -> emit(DebugInfo.getDebugInfo(this@DeviceInfoActivity))
                R.id.tv_root -> emit(RootInfo.getRootInfo(this@DeviceInfoActivity))
                R.id.tv_hook -> emit(HookInfo.getHookInfo(this@DeviceInfoActivity))
                R.id.tv_info -> emit(DeviceInfo.getDeviceInfo(this@DeviceInfoActivity))
                R.id.tv_app -> emit(AppInfo.getAppInfo(this@DeviceInfoActivity))
                R.id.tv_wifi -> emit(WifiInfo.getWifiInfo(this@DeviceInfoActivity))
                R.id.tv_lanya -> emit(BluetoothInfo.getBluetoothInfo(this@DeviceInfoActivity))
//                R.id.tv_maps -> emit(MapsInfo.getMapsInfo(this@DeviceInfoActivity))
                R.id.tv_others -> emit(OthersInfo.getOthersInfo(this@DeviceInfoActivity))
                else -> emit(null)
            }
        }.flowOn(Dispatchers.IO).catch {
            val list = ArrayList<Pair<String?, String?>?>()
            list.add(Pair(it.javaClass.toString(), it.message))
            emit(list)
        }
        lifecycleScope.launch(Dispatchers.Main) {
            flow.onStart {
                adapter.clear()
                viewBinding.tvEmpty.text = "加载中…"
                viewBinding.tvEmpty.visible()
            }.collect {
                if (it?.isNotEmpty() == true) {
                    viewBinding.tvEmpty.gone()
                } else {
                    viewBinding.tvEmpty.text = "暂无数据/未开发此功能"
                    viewBinding.tvEmpty.visible()
                }
                adapter.setNewData(it)
            }
        }
    }

    override fun onDestroy() {
        try {
            devicesGroupDialog?.let {
                if (it.isResumed) {
                    it.dismissAllowingStateLoss()
                }
                devicesGroupDialog = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}