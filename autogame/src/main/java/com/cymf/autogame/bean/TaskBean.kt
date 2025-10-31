package com.cymf.autogame.bean

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class TaskBean(
    @Json(name = "code")
    val code: Int,
    @Json(name = "msg")
    val msg: String?,
    @Json(name = "task")
    val task: Task?,
    @Json(name = "ad_config")
    val adConfig: List<AdConfig>?
)


@JsonClass(generateAdapter = true)
data class TaskReqBody(
    @Json(name = "is_success")
    val isSuccess: Boolean,
    @Json(name = "task")
    val task: Task,
    @Json(name = "adclicks")
    val adclicks: List<AdClicks>? = null
)

@JsonClass(generateAdapter = true)
data class FatalBody(
    @Json(name = "title")
    val title: String,
    @Json(name = "detail")
    val detail: String,
    @Json(name = "task")
    val task: Task,
)

data class AdConfig(
    @Json(name = "ad_type")
    val adType: String, //  广告类型
    @Json(name = "ad_network")
    val adNetwork: String,  //  广告联盟

    @Json(name = "max_click_times")
    val maxClickTimes: Int, //  同一次广告展示的最大点击次数

    @Json(name = "real_click_rate")
    val realClickRate: Float,   //  真实意愿点击率
    @Json(name = "real_next_click_rate")
    val realNextClickRate: Float,   //  点击后进行下次点击的概率。如第1次点击后0.2概率再点一次(点击位置要有差别)，第2次点完仍然后0.2概率再点一次，直到达到点击最大次数
    @Json(name = "real_web_session_time")
    val realWebSessionTime: Long,   //  真实点击网页广告的停留时长基数，每次点击需要加-50%至+50%的随机变化

    @Json(name = "acci_click_rate")
    val acciClickRate: Float,   //  误点击概率
    @Json(name = "acci_next_click_rate")
    val acciNextClickRate: Float,   //  误点击的点击后进行下次点击的概率
    @Json(name = "acci_web_session_time")
    val acciWebSessionTime: Long,   //  误点击网页广告的停留时长基数，每次点击需要加-50%至+50%的随机变化
)

@JsonClass(generateAdapter = true)
data class Task(
    @Json(name = "user_id")
    val userID: Int?,   //  用户ID
    @Json(name = "mode_id")
    val modeID: Int?,   //  // 任务类型ID
    @Json(name = "mode_name")
    val modeName: String?,  // 任务类型英文
    @Json(name = "package_name")
    val packageName: String?,// 包名
    @Json(name = "task_id")
    val taskID: Int?,   //  任务ID
    @Json(name = "is_game_app")
    val isGameApp: Boolean?,    //  是否是游戏APP
    @Json(name = "sapp_id")
    val sappID: Int?,   //  SAPP应用ID
    @Json(name = "aapp_id")
    val aappID: Int?,   //  AAPP应用ID
    @Json(name = "tap_speed_inc_rate")
    val tapSpeedIncRate: Double?,   //  点击间隔时间增幅（如-0.2，就是0.8倍点击间隔）
    @Json(name = "max_run_time")
    val maxRunTime: Long?,  //  最大运行时间（毫秒）
    @Json(name = "phone_id")
    val phoneID: Int?,   //  云手机ID
    @Json(name = "wait_time")
    val waitTime: Long?,   //  等待时间（毫秒）  modeID=11 modeName=MODE_WAIT时才有
    @Json(name = "activity_down_up_diff_rate")
    val activityDownUpDiffRate: Double?,   //  Activity 的点击，按下抬起坐标不同的概率
    @Json(name = "webview_down_up_diff_rate")
    val webviewDownUpDiffRate: Double?   //  WebView 的点击，按下抬起坐标不同的概率
)

@JsonClass(generateAdapter = true)
data class AdClicks(
    @Json(name = "time")
    val time: Long = 0,    // 点击时间戳（毫秒）
    @Json(name = "ad_type")
    val adType: String? = null,    // 点击的广告形式，仅限： Interstitial、RewardAd、Native、Banner、OpenAd
    @Json(name = "ad_network")
    val adNetwork: String? = null,    // 广告点击的广告联盟code，如：Applovin、Pangle、Inmobi、Vungle
    @Json(name = "acci")
    var acci: Boolean = false,   // 是否是误点击
)