package com.cymf.keyshot.constant

/**
 * 多语言及同义字 查询
 * 配合 AssistsCore.findByTextMultiLangOpt(MultiLanguageKeywords.val)使用
 */
object MultiLanguageKeywords {
    val INSTALL = listOf("Install", "install", "安装", "安裝")
    val UPDATE = listOf("update", "更新")
    val GOOGLE_ACCOUNT_FAIL = listOf("Authentication is required. You need to sign in to your Google Account.", "需要驗證。請登入你的 Google 帳戶。","需要进行身份验证。您需要登录自己的 Google 账号。")

    val PREVIOUS = listOf("上翻", "上翻", "上一页", "上一頁", "上一頁", "Previous", "Zurück")
    val NEXT = listOf("下翻", "下翻", "下一页", "下一頁", "下一個", "下一个", "Next", "Weiter")
    val HOME_BOOKSTORE = listOf("书城", "書城", "Bookstore", "Buchladen", "书店", "書店")
    val HOME_BOOKSHELF = listOf("书架", "書架", "Bookshelf", "Bücherregal")
    val CATEGORY_KEYWORDS = listOf("分类", "分類", "Category", "Kategorie", "频道", "頻道")
    val GET_START = listOf("Get Start", "Get Started", "开始", "開始", "始めましょう")
    val PRIVACY_POLICY =
        listOf("Privacy Policy", "隱私權政策", "隐私政策", "機密政策", "隱私政策", "隱私政策")
    val PERMISSIONS_DENY = listOf(
        "永久拒絕",
        "拒絕",
        "拒绝",
        "不再提示",
        "永久拒绝",
        "reject",
        "deny",
        "Permanent Rejection",
        "Don't Allow",
        "不允许"
    )


    val LANG_ENGLISH = listOf("English", "英语", "英語", "英文")
    val LANG_CHINESE = listOf("Chinese", "中文简体", "漢語", "汉语")
    val LANG_TW = listOf("繁體", "中文繁體")
    val TXT_SET_DEF = listOf("set as default", "SET AS DEFAULT", "設定為預設值")


    val CONFIRM_BUTTON = listOf("确认", "確認", "Confirm", "Bestätigen")
    val CANCEL_BUTTON = listOf("取消", "取消", "Cancel", "キャンセル", "Abbrechen")
    val CANCEL_BUTTONS = listOf("取消",  "Cancel", "キャンセル", "Abbrechen")
    val SKIP_BUTTON = listOf("Skip", "跳過", "跳过")
    val CONTINUE_BUTTON = listOf("Continue", "繼續", "继续")
    val PRi_BUTTON = listOf("Privacy Policy", "隱私協議", "隐私协议")
    val LATER_BUTTON = listOf(
        "Later",
        "Remind Me Later",
        "Not Now",
        "Skip for Now",
        "Maybe Later",
        "Defer",
        "以後再說",
        "稍後再說"
    )
    val CONTINUE_BUTTON_S =
        listOf("Continue", "繼續", "继续", "接受", "Accept", "Agree", "同意", "I know")
}