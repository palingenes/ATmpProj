package com.cymf.autogame.utils

import androidx.core.content.FileProvider

class AssistsFileProvider : FileProvider() {
    override fun onCreate(): Boolean {
        val applicationContext = context?.applicationContext
//        if (applicationContext is Application) {
//            LogUtils.getConfig().globalTag = "AutoMatic"
//        }
        return super.onCreate()
    }
}