package com.cymf.autogame

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cymf.autogame.adapter.LogAdapter
import com.cymf.autogame.base.AbsActivity
import com.cymf.autogame.constant.Constants
import com.cymf.autogame.databinding.ActivityFrameBinding
import com.cymf.autogame.ktx.clickWithLimit
import com.cymf.autogame.ktx.gone
import com.cymf.autogame.ktx.invisible
import com.cymf.autogame.ktx.isNotVisible
import com.cymf.autogame.ktx.visible
import com.cymf.autogame.service.AssistsService
import com.cymf.autogame.service.AssistsServiceListener
import com.cymf.autogame.utils.AssistsCore
import com.cymf.autogame.utils.CoroutineWrapper
import com.cymf.autogame.utils.PermissionUtil
import com.cymf.autogame.utils.ProcessKiller
import com.cymf.autogame.utils.SPUtil
import com.cymf.autogame.utils.TaskPollingManager
import com.cymf.autogame.utils.YLLogger
import com.hjq.toast.Toaster
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FrameActivity : AbsActivity<ActivityFrameBinding>(), AssistsServiceListener {

    private val isActivityResumed by lazy { AtomicBoolean(false) }
    private val loopCount by lazy { AtomicInteger(0) }
    private val logAdapter by lazy { LogAdapter() }
    private var isUserScrolling = false  // 用户是否正在手动滚动
    private var wasAutoScrollEnabled = true  // 是否允许自动滚动（上次是否在底部）


    override fun bindMergeView() {
        super.bindMergeView()
        if (!PermissionUtil.isAccessibilityServiceEnable(this)) {
            val fullPath = "${BuildConfig.APPLICATION_ID}/${AssistsService::class.java.name}"
            Shell.cmd("settings put secure enabled_accessibility_services $fullPath").exec()
        }
    }

    override fun initView() {
        find<View>(R.id.ic_back).invisible()

        lifecycleScope.launch { applyRootPermissions() }

        viewBinding.btnAccessibility.clickWithLimit {
            try {
                val accessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibleIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(accessibleIntent)
            } catch (_: Exception) {
            }
        }
        viewBinding.btnFloatWindow.clickWithLimit {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:$packageName".toUri()
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            lifecycleScope.launch {
                while (isActivityResumed.get() && !PermissionUtil.isCanDrawOverlays(this@FrameActivity)) {
                    delay(500)
                }
                if (PermissionUtil.isCanDrawOverlays(this@FrameActivity)) {
                    AssistsCore.back()
                    updateUI()
                }
            }
        }
        viewBinding.btnList.clickWithLimit {
            PermissionUtil.requestUsageAccessPermission(this)
            lifecycleScope.launch {
                while (isActivityResumed.get() && !PermissionUtil.isUsageAccessPermissionGranted(
                        this@FrameActivity
                    )
                ) {
                    delay(500)
                }
                if (PermissionUtil.isUsageAccessPermissionGranted(this@FrameActivity)) {
                    AssistsCore.back()
                    updateUI()
                }
            }
        }
        viewBinding.btnWriteSettings.clickWithLimit {
            val writeSettingsIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(writeSettingsIntent)
            lifecycleScope.launch {
                while (isActivityResumed.get() && !PermissionUtil.isSettingsCanWrite(this@FrameActivity)) {
                    delay(500)
                }
                if (PermissionUtil.isSettingsCanWrite(this@FrameActivity)) {
                    AssistsCore.back()
                    updateUI()
                }
            }
        }
        viewBinding.btnSavePhoneId.clickWithLimit {
            if ("111" != it.tag) return@clickWithLimit

            if (viewBinding.editPhoneId.text?.trim().isNullOrBlank()) {
                Toaster.showShort("请输入当前设备ID！")
                return@clickWithLimit
            }
            if (SPUtil.PHONE_ID.isNotBlank()) {
                Toaster.showShort("当前已绑定设备ID，如需更换请联系技术人员")
                return@clickWithLimit
            }
            val phoneId = viewBinding.editPhoneId.text?.trim().toString()

            if (phoneId.isBlank() || phoneId.length < 4 || phoneId.length > 6) {
                visibleBtnAndRunTask(false)  //  输入的phoneID不符合要求。
                Toaster.showShort("当前设备格式不符合要求！")
                return@clickWithLimit
            }
            SPUtil.PHONE_ID = phoneId
            saveIDToFile(SPUtil.PHONE_ID)
            bindPhoneIdAndStartTask()
        }

        viewBinding.consoleRecyclerView.apply {
            adapter = logAdapter
            layoutManager = LinearLayoutManager(this@FrameActivity).apply {
                reverseLayout = true
                stackFromEnd = true
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    when (newState) {
                        RecyclerView.SCROLL_STATE_DRAGGING -> {
                            isUserScrolling = true
                            wasAutoScrollEnabled = false
                        }

                        RecyclerView.SCROLL_STATE_IDLE -> {
                            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                            val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                            wasAutoScrollEnabled = firstVisibleItem == 0
                            isUserScrolling = false
                        }

                        RecyclerView.SCROLL_STATE_SETTLING -> {
                            isUserScrolling = true
                        }
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                    if (dy > 0 && firstVisibleItem > 0) {
                        isUserScrolling = true
                        wasAutoScrollEnabled = false
                    }
                }
            })
            itemAnimator = null
        }
        App.logLiveData.observe(this) { logs ->
            logAdapter.updateLogs(logs)
            if (!isUserScrolling || wasAutoScrollEnabled) {
                viewBinding.consoleRecyclerView.post {
                    viewBinding.consoleRecyclerView.scrollToPosition(logAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun bindPhoneIdAndStartTask() {
        viewBinding.editPhoneId.setText(SPUtil.PHONE_ID)
        viewBinding.editPhoneId.setTextColor(Color.GREEN)
        viewBinding.editPhoneId.isFocusable = false
        viewBinding.editPhoneId.isClickable = false
        viewBinding.editPhoneId.isLongClickable = false

        visibleBtnAndRunTask(true)  //  默认已经获取到PhoneID
        viewBinding.btnSavePhoneId.text = "获取任务"
        viewBinding.btnSavePhoneId.tag = "222"
        viewBinding.btnSavePhoneId.isEnabled = true
    }

    @SuppressLint("SetTextI18n")
    private suspend fun applyRootPermissions() {
        if (Shell.isAppGrantedRoot() == true) {
            canEnableToButton()
            viewBinding.tvBody.setTextColor(Color.GREEN)
            viewBinding.tvBody.text = "当前应用已获得root权限！"
            return
        }
        if (loopCount.andIncrement >= 10) {
            viewBinding.tvBody.setTextColor(Color.RED)
            viewBinding.tvBody.text =
                "请在右下角【更多操作】-> 【root】中手动打开本应用授权\n打开授权后建议重启本应用！"
            return
        }
        val result = Shell.cmd("su").exec().apply {
            YLLogger.d("command su: $code")
        }
        if (!result.isSuccess) {
            delay(200)
            applyRootPermissions()
        } else {
            canEnableToButton()
            viewBinding.tvBody.setTextColor(Color.GREEN)
            viewBinding.tvBody.text = "当前应用已获得root权限！"
        }
    }

    private fun canEnableToButton() {
        for (view in viewBinding.llContainer.children) {
            if (view !is Button || view !is AppCompatButton) continue
            if (view.isNotVisible) continue
            view.isEnabled = true
        }
    }


    override fun initData() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 101
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveIDToFile(text: String) {
        CoroutineWrapper.launch(false) {
            if (!TextUtils.isEmpty(text)) {
                Shell.cmd(
                    "mkdir -p ${
                        ID_FILE_PATH.substring(
                            0, ID_FILE_PATH.lastIndexOf('/') + 1
                        )
                    } && echo $text > $ID_FILE_PATH"
                ).exec()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed.set(true)
        updateUI()  //  onResume
        loadIDByFile()
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed.set(false)
    }

    private fun updateUI() {
        if (!isActivityResumed.get()) return

        if (!PermissionUtil.isAccessibilityServiceEnable(this)) {
            viewBinding.btnAccessibility.visible()
            return
        }
        viewBinding.btnAccessibility.gone()
        if (!PermissionUtil.isCanDrawOverlays(this)) {
            viewBinding.btnFloatWindow.visible()
            return
        }
        viewBinding.btnFloatWindow.gone()
        AssistsService.instance?.showSlideView()

        if (!PermissionUtil.isUsageAccessPermissionGranted(this)) {
            viewBinding.btnList.visible()
            return
        }
        viewBinding.btnList.gone()
        if (!Settings.System.canWrite(this)) {
            viewBinding.btnWriteSettings.visible()
            return
        }
        viewBinding.btnWriteSettings.gone()
        viewBinding.editPhoneId.visible()

        if (SPUtil.PHONE_ID.isBlank() || SPUtil.PHONE_ID.length < 4 || SPUtil.PHONE_ID.length > 6) {
            visibleBtnAndRunTask(false)  //  更新后校验
        } else {
            bindPhoneIdAndStartTask()
        }
    }

    /**
     * 所有权限都开启的情况下才会轮询请求任务
     */
    private fun visibleBtnAndRunTask(isStartTask: Boolean) {
        if (isStartTask) {
            CoroutineWrapper.launch {
                ProcessKiller.killApp(App.context, Constants.PKG_AUTO_APP)
            }
            TaskPollingManager.startPolling()  //  修改成所有权限都赋予之后再进行任务请求
        }
        viewBinding.btnSavePhoneId.visible()
    }

    override fun onServiceConnected(service: AssistsService) {
        updateUI()  //  onServiceConnected
    }

    fun loadIDByFile() {
        if (!TextUtils.isEmpty(SPUtil.PHONE_ID)) {
            return
        }
        CoroutineWrapper.launch(false) {
            val result = Shell.cmd("cat $ID_FILE_PATH").exec()
            if (!result.isSuccess || result.out.isEmpty()) {
                return@launch
            }
            val text = result.out[0]
            if (text.isNullOrBlank()) {
                return@launch
            }
            SPUtil.PHONE_ID = text
            launch(Dispatchers.Main) { updateUI() }
        }
    }

    companion object {
        private const val ID_FILE_PATH =
            "/storage/emulated/0/Download/${BuildConfig.PATH_M}/tmp/data"
    }
}