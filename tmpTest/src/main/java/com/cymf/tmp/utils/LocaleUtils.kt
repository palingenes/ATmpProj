package com.cymf.tmp.utils

import java.util.*

object LocaleUtils {

    /**
     * 获取所有支持的 ISO 语言代码（如 en, zh, ja）
     * ISO 639-1语言代码
     */
    fun getISOLanguages(): List<String> {
        return Locale.getISOLanguages().toList()
    }

    /**
     * 获取所有支持的 Locale 对象（基于可用 locales）
     */
    fun getAvailableLocales(): List<Locale> {
        return Locale.getAvailableLocales().toList()
    }

    /**
     * 获取本地化语言名称（显示给用户看）
     *
     * @param languageCode 如 "zh", "en"
     * @param contextLocale 当前系统或应用使用的 locale，用于显示对应语言的名称
     * @return 本地化的语言名称（如 中文、English）
     */
    fun getLocalizedLanguageName(languageCode: String, contextLocale: Locale = Locale.getDefault()): String {
        val locale = Locale(languageCode)
        return locale.getDisplayLanguage(contextLocale)
    }

    /**
     * 获取语言代码 -> 本地化名称 的映射表
     */
    fun getLanguageCodeToNameMap(contextLocale: Locale = Locale.getDefault()): Map<String, String> {
        return getISOLanguages().associateWith { code ->
            getLocalizedLanguageName(code, contextLocale)
        }
    }

    /**
     * 按语言名称排序后返回 Map
     */
    fun getSortedLanguageCodeToNameMap(contextLocale: Locale = Locale.getDefault()): Map<String, String> {
        return getLanguageCodeToNameMap(contextLocale)
            .toSortedMap(compareBy { getLocalizedLanguageName(it, contextLocale) })
    }
}