package com.cymf.keyshot

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.cymf.keyshot.base.AbsActivity
import com.cymf.keyshot.databinding.ActivityFrameBinding
import com.cymf.keyshot.ktx.gone
import com.cymf.keyshot.ktx.invisible
import com.cymf.keyshot.ktx.isNotVisible
import com.cymf.keyshot.ktx.visible
import com.cymf.keyshot.service.AssistsService
import com.cymf.keyshot.service.AssistsServiceListener
import com.cymf.keyshot.utils.AssistsCore
import com.cymf.keyshot.utils.CoroutineWrapper
import com.cymf.keyshot.utils.PermissionUtil
import com.cymf.keyshot.utils.SPUtil
import com.cymf.keyshot.utils.YLLogger
import com.hjq.toast.Toaster
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FrameActivity : AbsActivity<ActivityFrameBinding>(), AssistsServiceListener {

    private val loopCount by lazy { AtomicInteger(0) }
    private val isActivityResumed by lazy { AtomicBoolean(false) }

    override fun bindMergeView() {
        super.bindMergeView()
        if (!PermissionUtil.isAccessibilityServiceEnable(this)) {
            Shell.cmd("settings put secure enabled_accessibility_services com.cymf.automatic/com.cymf.automatic.service.AssistsService")
                .exec()
        }
    }

    override fun initView() {
        find<View>(R.id.ic_back).invisible()

        lifecycleScope.launch {
            applyRootPermissions()
        }

        viewBinding.btnAccessibility.setOnClickListener {
            try {
                val accessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibleIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(accessibleIntent)
            } catch (_: Exception) {
            }
        }
        viewBinding.btnFloatWindow.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:$packageName".toUri()
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            while (!PermissionUtil.isCanDrawOverlays(this)) {
                if (PermissionUtil.isCanDrawOverlays(this)) {
                    AssistsCore.back()
                }
            }
        }
        viewBinding.btnList.setOnClickListener {
            PermissionUtil.requestUsageAccessPermission(this)
            while (!PermissionUtil.isUsageAccessPermissionGranted(this)) {
                if (PermissionUtil.isUsageAccessPermissionGranted(this)) {
                    AssistsCore.back()
                }
            }
        }
        viewBinding.btnWriteSettings.setOnClickListener {
            val writeSettingsIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(writeSettingsIntent)
            while (!PermissionUtil.isSettingsCanWrite(this)) {
                if (PermissionUtil.isSettingsCanWrite(this)) {
                    AssistsCore.back()
                }
            }
        }
        viewBinding.btnSavePhoneId.setOnClickListener {
            if (viewBinding.editPhoneId.text.trim().isBlank()) {
                Toaster.showShort("请输入当前设备ID！")
                return@setOnClickListener
            }
            SPUtil.PHONE_ID = viewBinding.editPhoneId.text.trim().toString()
            saveIDToFile(SPUtil.PHONE_ID)
            bindPhoneIdAndStartTask()
        }
    }

    private fun bindPhoneIdAndStartTask() {
        viewBinding.editPhoneId.setText(SPUtil.PHONE_ID)
        viewBinding.editPhoneId.setTextColor(Color.GREEN)
        viewBinding.editPhoneId.isFocusable = false
        viewBinding.editPhoneId.isClickable = false
        viewBinding.editPhoneId.isLongClickable = false

        viewBinding.btnSavePhoneId.visible()
        viewBinding.btnSavePhoneId.text = "获取任务"
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
                "建议杀死当前应用，并重新进入\n如尝试重启多次应用依然无法获取root权限，\n请联系开发人员！"
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
                            0,
                            ID_FILE_PATH.lastIndexOf('/') + 1
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
//        startActivity(Intent(this, TestTaskActivity::class.java))
//        finish()
//
        if (SPUtil.PHONE_ID.isBlank() || SPUtil.PHONE_ID.length < 4 || SPUtil.PHONE_ID.length > 6) {
            viewBinding.btnSavePhoneId.visible()
            return
        }
        bindPhoneIdAndStartTask()
    }

    override fun onServiceConnected(service: AssistsService) {
//        onBackApp()
        updateUI()  //  onServiceConnected
    }


    fun loadIDByFile() {
        if (TextUtils.isEmpty(SPUtil.PHONE_ID)) {
            CoroutineWrapper.launch(false) {
                val result = Shell.cmd("cat $ID_FILE_PATH").exec()
                if (result.isSuccess && result.out.isNotEmpty()) {
                    val text = result.out[0]
                    Log.e("why===", "text ==${text}")
                    if (!TextUtils.isEmpty(text)) {
                        SPUtil.PHONE_ID = text
                        launch(Dispatchers.Main) {
                            updateUI()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val ID_FILE_PATH =
            "/storage/emulated/0/Download/${BuildConfig.PATH_M}/tmp/data"
    }
}