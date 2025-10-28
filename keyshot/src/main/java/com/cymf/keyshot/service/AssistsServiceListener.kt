package com.cymf.keyshot.service

import android.view.accessibility.AccessibilityEvent

interface AssistsServiceListener {

    /**
     * 通过监听[AssistsService.onAccessibilityEvent] 回调来判断是否“卡”在某个页面无法操作
     * @param currPkgClazzName com.xxx.xxx/.xxxActivity
     */
    fun onStuckPage(currPkgClazzName: String?) = Unit

    /**
     * 任务已经执行了多长时间 (s)
     */
    fun onTaskTime(time: Int)= Unit

    /**
     * 当界面发生事件时回调，即 [AssistsService.onAccessibilityEvent] 回调
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {}

    /**
     * 服务启用后的回调，即[AssistsService.onServiceConnected]回调
     */
    fun onServiceConnected(service: AssistsService) {}

    fun onInterrupt() {}

    /**
     * 服务关闭后的回调，即[AssistsService.onUnbind]回调
     */
    fun onUnbind() {}
    fun onServiceDestroy(service: AssistsService) {}
}