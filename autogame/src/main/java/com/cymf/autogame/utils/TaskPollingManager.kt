package com.cymf.autogame.utils

import com.cymf.autogame.App
import com.cymf.autogame.bean.AdClicks
import com.cymf.autogame.bean.AdConfig
import com.cymf.autogame.bean.LogItem
import com.cymf.autogame.bean.LogLevel
import com.cymf.autogame.bean.Task
import com.cymf.autogame.bean.TaskBean
import com.cymf.autogame.constant.Constants
import com.cymf.autogame.service.AssistsService
import com.cymf.autogame.utils.net.NetworkClient
import com.hjq.toast.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

/**
 * 网络轮询请求
 */
object TaskPollingManager {

    private const val DEFAULT_DELAY = 3000L
    private val scope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + Job()
    }
    private var isPauseGetTask = false
    private val formatter by lazy {
        SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
    }
    private val isTasking = AtomicBoolean(false)
    private val pollingJob = AtomicReference<Job?>(null)
    private val currentTask = AtomicReference<Task?>(null)
    private val currentAdConfigs = AtomicReference<List<AdConfig>>(null)

    private val mAdClicks = AtomicReference<List<AdClicks>>(emptyList())
    private val clicksCount by lazy { AtomicInteger(0) }

    private val tapBannerAdView by lazy { AtomicBoolean(false) }

//    fun togglePolling() {
//        if (pollingJob.get()?.isActive == true) {
//            stopPolling()
//        } else {
//            startPolling()
//        }
//    }

//    fun resumePolling() {
//        if (AssistsService.instance?.isAccessibilityServiceEnabled() != true) {
//            YLLogger.i("resumePolling AccessibilityServiceDisabled")
//            startPolling()  // AssistsService.instance?.isAccessibilityServiceEnabled()
//        } else if (!AssistsService.isServiceActive()) {
//            YLLogger.i("resumePolling AccessibilityServiceInActive")
//        } else if (currentTask.get() == null) {
//            YLLogger.i("resumePolling startPolling...")
//            startPolling()  //  currentTask.get() == null
//        } else {
//            if (pollingJob.get()?.isActive == true) return
//            YLLogger.w("resumePolling task is running")
//            reportTaskFatal("恢复获取任务", "任务中断")
//        }
//    }

    // 启动轮询（可手动调用）
    fun startPolling() {
        if (isPauseGetTask) {
            pollingJob.get()?.cancel()
            YLLogger.e("关闭 协程=====" + pollingJob.get()?.isActive)
            return
        }
        if (isTasking.get()) {
            YLLogger.d("当前正在执行其他任务，跳过本次轮询~")
            return
        }
        if (pollingJob.get()?.isActive == true) return
        YLLogger.d("start Polling ...")

        CoroutineWrapper.launch {
            ProcessKiller.killApp(App.context, Constants.PKG_AUTO_APP)
        }

        val newJob = scope.launch {
            var delayTime: Long
            // 初始等待 3 秒
            delay(DEFAULT_DELAY)

            while (isActive) {
                if (SPUtil.PHONE_ID.isBlank()) {
                    YLLogger.d("⚠️ startPolling...phone ID empty ！")
                    App.addLog(LogItem(LogLevel.NORMAL, "⚠️", "找不到设备ID!"))
                    delay(DEFAULT_DELAY)
                    continue
                }
                App.resetRoundTimer()
                App.addLog(LogItem(LogLevel.NORMAL, "☕", "获取任务中……"))
                val (data, _) = NetworkClient.get(SPUtil.PHONE_ID)
                val modeID = data?.task?.modeID
                App.addLog(LogItem(LogLevel.NORMAL, "🍵", "当前任务ID=$modeID"))
                YLLogger.d("startPolling...modeID= $modeID")
                when {
                    modeID == 11 -> {   //  等待下次轮询，时间由 waitTime 提供
                        val waitTime = data.task.waitTime ?: DEFAULT_DELAY
                        val text = when (waitTime) {
                            3000L -> "任务处理中"
                            23000L -> "等待新任务"
                            60000L -> "云手机异常"
                            else -> "未知情况，请等待或联系开发人员"
                        }
                        isTasking.set(false)
                        App.addLog(LogItem(LogLevel.NORMAL, "⌛️", text))
                        delayTime = waitTime
                        delay(delayTime)
                    }

                    data == null || data.task == null -> {  //  出错，等3秒
                        App.addLog(
                            LogItem(
                                LogLevel.NORMAL,
                                "🖤",
                                "出现错误，请等待${DEFAULT_DELAY / 1000}秒"
                            )
                        )
                        isTasking.set(false)
                        delay(DEFAULT_DELAY)
                    }

                    else -> {
                        App.addLog(LogItem(LogLevel.NORMAL, "👉", "准备执行任务……"))
                        data.task.let { task ->
                            currentTask.set(task.copy())
                        }
                        data.adConfig?.takeIf { data.task.modeID in listOf(2, 3) }?.let {
                            currentAdConfigs.set(it.toList())
                        }
                        stopPolling()
                        assigningTasks()
                    }
                }
            }
        }
        App.addLog(LogItem(LogLevel.NORMAL, "😉", "开始获取任务"))
        // 原子设置：如果当前为 null，才设置新值
        YLLogger.d("startPolling...pollingJob...")
        pollingJob.compareAndSet(null, newJob)
    }

    /**
     * 分配任务
     */
    private suspend fun assigningTasks() {
        val task = currentTask.get()
        YLLogger.d("assigningTasks... ${task?.modeID}")
        AssistsService.instance?.setStartTaskValue(true)
        when (task?.modeID) {
            1, 5 -> {    //  去Google Play安装SAPP | 去Google Play安装AAPP
                val text = if (1 == task.modeID) {
                    "去Google Play安装SAPP"
                } else {
                    "去Google Play安装AAPP"
                }
                App.addLog(
                    LogItem(
                        LogLevel.NORMAL,
                        "🚴",
                        "任务{${task.modeID}} $text",
                        task.packageName
                    )
                )
                isTasking.set(true)
                val pkgName = task.packageName ?: return startPolling()
                YLLogger.setLogFileName("log_gg_${suffix()}")
                Toaster.showShort("即将跳转到Google Play")
                AppUtil.startGooglePlay(pkgName)
                GlobalTimer.getInstance().start()
            }

            2, 3 -> {    //  SAPP首次激活使用 | SAPP后续留存使用 (目前只有LM这一个App)
                val text = if (2 == task.modeID) {
                    "SAPP首次激活使用"
                } else {
                    "SAPP后续留存使用"
                }
                App.addLog(
                    LogItem(
                        LogLevel.NORMAL,
                        "🚴",
                        "任务{${task.modeID}} $text",
                        task.packageName
                    )
                )
                isTasking.set(true)
                GlobalTimer.getInstance().start()
            }

            4, 8 -> {    //  去Google Play更新SAPP | 去Google Play更新AAPP (暂时未做此任务！)
                val text = if (4 == task.modeID) {
                    "去Google Play更新SAPP"
                } else {
                    "去Google Play更新AAPP"
                }
                App.addLog(
                    LogItem(
                        LogLevel.NORMAL,
                        "🚴",
                        "任务{${task.modeID}} $text",
                        task.packageName, task.packageName
                    )
                )
                isTasking.set(true)
                val pkgName = task.packageName ?: return startPolling()
                Toaster.showShort("即将跳转到Google Play")
                AppUtil.startGooglePlay(pkgName)
                GlobalTimer.getInstance().start()
            }

            6, 7 -> {  //  AAPP首次激活使用 | AAPP后续留存使用 （儿童操作）
                if (6 == task.modeID) {
                    App.addLog(
                        LogItem(
                            LogLevel.LV_2,
                            "🚴",
                            "任务{${task.modeID}} AAPP首次激活使用",
                            task.packageName
                        )
                    )
                } else {
                    App.addLog(
                        LogItem(
                            LogLevel.LV_1,
                            "🚴",
                            "任务{${task.modeID}} AAPP后续留存使用",
                            task.packageName
                        )
                    )
                }
                isTasking.set(true)
                GlobalTimer.getInstance().start()
            }

            10 -> {   //  默认浏览器首次使用处理引导页
                App.addLog(
                    LogItem(
                        LogLevel.NORMAL,
                        "🚴",
                        "任务{${task.modeID}} 默认浏览器首次使用处理引导页"
                    )
                )
                YLLogger.setLogFileName("log_browser_${suffix()}")
                isTasking.set(true)
                GlobalTimer.getInstance().start()
            }

            9 -> {    //  更新无障碍应用
                App.addLog(
                    LogItem(
                        LogLevel.NORMAL,
                        "🚴",
                        "任务{${task.modeID}} 更新无障碍应用(可忽略)", task.packageName
                    )
                )
                reportTaskComp(true)   //   更新任务，暂无
                delay(700)
                isTasking.set(false)
                GlobalTimer.getInstance().reset()
                startPolling()  //  开启下一轮任务请求
            }

            else -> {
                App.addLog(
                    LogItem(
                        LogLevel.NORMAL,
                        "🌚",
                        "未处理任务,id=${task?.modeID}(可忽略)",
                        task?.packageName ?: "Null"
                    )
                )
                reportTaskComp(true)    //  一般modeID=11
                delay(700)
                isTasking.set(false)
                GlobalTimer.getInstance().reset()
                startPolling()  //  开启下一轮任务请求
            }
        }
    }

    /**
     * 上报任务完成
     * task.modeId == 2|3时 上传 adClicks
     */
    fun reportTaskComp(isSuccess: Boolean) {
        AssistsService.instance?.setStartTaskValue(false)
        val task = currentTask.get()
        if (task == null) {
            YLLogger.d("task == null")
            GlobalTimer.getInstance().reset()
            return
        }
        currentTask.set(null)
        val text = if (isSuccess) "完成" else "失败"
        App.addLog(LogItem(LogLevel.NORMAL, "🙇", "即将上报任务 {$text} 状态"))
        val taskBody = JsonConvert.buildTaskBody(isSuccess, task, mAdClicks.get())
        YLLogger.d("reportTaskComp... ${task.modeID} $isSuccess, ${mAdClicks.get()}")
        NetworkClient.complete<TaskBean>(taskBody) { data, error ->
            if (data?.code == 200) {
                App.addLog(LogItem(LogLevel.NORMAL, "✅", "上报任务接口回调 $data"))
            } else {
                App.addLog(LogItem(LogLevel.LV_1, "🐞", "上报任务失败！ \n🐞$error"))
            }
            YLLogger.e(data.toString(), error)
            currentAdConfigs.set(null)
            mAdClicks.set(emptyList())
            setClickCount(0)
            isTasking.set(false)
            GlobalTimer.getInstance().reset()
            startPolling()  //  上报任务完成后再进行新任务请求
        }
    }

    /**
     * 上报任务严重错误！
     */
//    fun reportTaskFatal(title: String, details: String) {
//        val task = currentTask.get() ?: return
//        currentTask.set(null)
//        App.addLog(LogItem(LogLevel.NORMAL, "🙇", "即将上报任务错误信息 \n$title \n$details"))
//        val taskBody = JsonConvert.buildFatalBody(title, details, task)
//        NetworkClient.fatal<TaskBean>(taskBody) { data, error ->
//            if (data?.code == 200) {
//                App.addLog(LogItem(LogLevel.NORMAL, "✅", "任务错误接口回调 $data"))
//            } else {
//                App.addLog(LogItem(LogLevel.LV_1, "🐞", "上传任务错误失败！ \n🐞$error"))
//            }
//            YLLogger.e(data.toString(), error)
//            currentAdConfigs.set(null)
//            mAdClicks.set(emptyList())
//            setClickCount(0)
//            startPolling()  //  上报任务错误后再进行新任务请求
//        }
//    }

    // 停止轮询
    fun stopPolling() {
        runCatching {
            val currentJob = pollingJob.getAndSet(null)
            currentJob?.cancel()
        }
    }

//    fun getClickCount(): Int {
//        return clicksCount.get()
//    }

    fun getCurrTaskSpeed(duration: Long = -1): Long {
        val baseDuration = if (duration == -1L) {
            Random.nextInt(50, 200).toLong()
        } else duration

        return (baseDuration * (currentTask.get()?.tapSpeedIncRate ?: 1.0)).toLong()
    }

    fun getCurrTask(): Task? {
        return currentTask.get()
    }

//    fun getAdNetwork(type: String): String? {
//        return getAdLoadInfo()[type]
//    }

//    fun getAdConfigByType(ad: Pair<String, String>?): AdConfig? {
//        return getAdConfigs().firstOrNull { it.adType == ad?.first && it.adNetwork == ad.second }
//    }

//    fun getAdConfigs(): List<AdConfig> {
//        return currentAdConfigs.get() ?: emptyList()
//    }

    fun getTapBannerAdView(): Boolean {
        return tapBannerAdView.get()
    }

    fun setTapBannerAdView(tapView: Boolean) {
        tapBannerAdView.set(tapView)
    }

//    fun addAdClicks(adClicks: AdClicks) {
//        mAdClicks.updateAndGet { it + adClicks }
//    }

    fun setClickCount(count: Int = -1) {
        if (count == -1) {
            clicksCount.incrementAndGet()
        } else {
            clicksCount.set(count)
        }
    }

    private fun suffix(): String {
        return synchronized(formatter) {
            formatter.format(Date())
        }
    }

//    fun getAdLoadInfo(): Map<String, String> {
//        return Shell.cmd("cat ${Constants.AD_LOAD_PATH}").exec()
//            .takeIf { it.isSuccess }
//            ?.out?.map { line ->
//                val (type, network) = line.split("\t")
//                return@map type to network
//            }?.toMap()?.apply {
//                App.addLog(LogItem(LogLevel.NORMAL, "🤪", "获取广告加载信息 ↓↓↓ "))
//                App.addLog(LogItem(LogLevel.NORMAL, "🤪", "$this"))
//                YLLogger.d("🤪adLoadInfo: $this")
//            } ?: emptyMap()
//    }

//    fun setIsPauseGetTask(isPauseGetTask: Boolean) {
//        TaskPollingManager.isPauseGetTask = isPauseGetTask
//    }

//    fun isPauseGetTask(): Boolean {
//        return isPauseGetTask
//    }
}