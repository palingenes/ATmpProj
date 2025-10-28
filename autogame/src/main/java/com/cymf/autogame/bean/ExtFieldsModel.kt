package com.cymf.autogame.bean

/**
 * 供任务使用的
 */
data class ExtFieldsModel(
    val pkgName: String,    //  目标包名
    val isGame: Boolean,    //  区分是应用还是游戏
    val maxTaskTime: Long,   //  最大任务时长（可以有部分偏差） 单位 s
    val isUpdate: Boolean = false,    //  区分是否是更新任务
)