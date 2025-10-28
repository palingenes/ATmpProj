package com.cymf.keyshot.constant

/**
 * 常量
 *
 * @author wzy
 * @date on 2025-05-01 11:20
 */
object Constants {

    const val TAG_RESET_TOUCH_BAR = "TAG_RESET_TOUCH_BAR"
    const val TAG_CRASH = "TAG_CRASH"

    const  val SEARCH_LM =  "com.retry.lemo.ui.activity.SeachActvityn"  //  LM 的书籍搜索页
    const  val MAIN_LM =  "com.retry.lemo.ui.MainActivity"  //  LM 首页
    const  val READER_LM =  "com.retry.lemo.reader.ReaderActivity"  //  LM 的书籍阅读页
    const  val BOOK_DETAIL_LM =  "com.retry.lemo.ui.activity.BkdetActvityn"  //  LM 的书籍详情页

    const val PLAY_PACKAGE = "com.android.vending"
    const val PKG_VENDING = "com.android.vending/com.google.android.finsky.activities.MainActivity"
    const val PKG_VENDING_MOCK = "com.android.vending_lib/.EntranceActivity"
    const val GG_SIGN_ACTIVITY = "com.google.android.play.games/com.google.android.gms.games.ui.signin.SignInActivity"
    const val GG_ACCOUNT_ACTIVITY = "com.google.android.gms/.common.account.AccountPickerActivity"

    const val AD_BROWSER = "com.opera.browser/com.opera.android.BrowserActivity"
    const val AD_BROWSER_1 = "com.android.vending_lib/.EntranceActivity"
    const val AD_FULL_NAME_MAX = "com.applovin.adview.AppLovinFullscreenActivity"
    const val AD_FULL_NAME_U3D = "com.unity3d.services.ads.adunit.AdUnitActivity"
    const val AD_FULL_NAME_INMOBI = "com.inmobi.ads.rendering.InMobiAdActivity"
    const val AD_FULL_NAME_PANGLE = "com.bytedance.sdk.openadsdk.activity.TTFullScreenExpressVideoActivity"
    const val AD_FULL_NAME_PANGLE2 = "com.bytedance.sdk.openadsdk.activity.base.TTRewardExpressVideoActivity"
    const val AD_FULL_NAME_PANGLE3 = "com.bytedance.sdk.openadsdk.activity.TTFullScreenVideoActivity"
    const val AD_FULL_NAME_PANGLE4 = "com.bytedance.sdk.openadsdk.activity.TTAdActivity"
    const val AD_FULL_NAME_PANGLE5 = "com.bytedance.sdk.openadsdk.activity.TTAppOpenAdActivity"
    const val AD_FULL_NAME_PANGLE6 = "com.bytedance.sdk.openadsdk.activity.TTLandingPageActivity"
    const val AD_FULL_NAME_PANGLE7 = "com.bytedance.sdk.openadsdk.activity.TTWebsiteActivity"



    //----------------------------------------------------------------
    const val AD_FULL_NAME_BIGO = "sg.bigo.ads.api.CompanionAdActivity" //  和正常逻辑一样
    const val AD_FULL_NAME_MB = "com.mbridge.msdk.reward.player.MBRewardVideoActivity"
    const val AD_FULL_NAME_GG = "com.google.android.gms.ads.AdActivity" //  google ads 的关闭按钮在底部！！！


    const val AD_TYPE_OPEN = "OpenAd"
    const val AD_TYPE_INTERSTITIAL = "Interstitial"
    const val AD_TYPE_BANNER = "Banner"
//    const val AD_TYPE_REWARDAD = "RewardAd"
//    const val AD_TYPE_NATIVE = "Native"
//    const val AD_TYPE_OPENAD = "OpenAd"

    const val AD_NET_APPLOVIN = "Applovin"
    const val AD_NET_INMOBI = "Inmobi"
    const val AD_NET_PANGLE = "Pangle"

    val BROWSER_LIST = arrayOf(
        "com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx", "com.opera.browser"//, "com.android.vending_lib"
    )

    const val TXT_GUIDE = "Guide"
    const val TXT_LANGUAGE_SELECT = "LanguageSelect"

    const val UNKNOWN: String = "\$unknown"

    const val AD_LOAD_PATH = "/data/local/files/gms/bin/adload"

    /**
     * opera 浏览器弹窗的取消按钮
     */
    const val BROWSER_DIALOG_NEGATIVE = "com.opera.browser:id/negative_button"
}