package com.cymf.keyshot.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.cymf.keyshot.App
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object SPUtil {
    /**
     * 创建 SharedPreferences 对象
     */
    val preferences: SharedPreferences by lazy {
        App.app.getSharedPreferences(
            "${App.app.packageName}_preferences",
            Context.MODE_PRIVATE
        )
    }

    var TOUCH_LOCATION by SharedPreferenceDelegates.int(2)

    var PHONE_ID by SharedPreferenceDelegates.string("")

    var VPN_TOP_PKG by SharedPreferenceDelegates.string("")   // 前台App包名
    var VPN_PKG_LIST by SharedPreferenceDelegates.array()
}

/**
 * 定义类型 属性委托类
 */
private object SharedPreferenceDelegates {

    private val lock = Any()

    fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SPUtil, Int> {
        override fun getValue(thisRef: SPUtil, property: KProperty<*>): Int {
            synchronized(lock) {
                return thisRef.preferences.getInt(property.name, defaultValue)
            }
        }

        override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Int) {
            synchronized(lock) {
                thisRef.preferences.edit { putInt(property.name, value) }
            }
        }
    }

    fun long(defaultValue: Long = 0L) = object : ReadWriteProperty<SPUtil, Long> {
        override fun getValue(thisRef: SPUtil, property: KProperty<*>): Long {
            synchronized(lock) {
                return thisRef.preferences.getLong(property.name, defaultValue)
            }
        }

        override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Long) {
            synchronized(lock) {
                thisRef.preferences.edit { putLong(property.name, value) }
            }
        }
    }

    fun boolean(defaultValue: Boolean = false) =
        object : ReadWriteProperty<SPUtil, Boolean> {
            override fun getValue(thisRef: SPUtil, property: KProperty<*>): Boolean {
                synchronized(lock) {
                    return thisRef.preferences.getBoolean(property.name, defaultValue)
                }
            }

            override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Boolean) {
                synchronized(lock) {
                    thisRef.preferences.edit { putBoolean(property.name, value) }
                }
            }
        }

    fun float(defaultValue: Float = 0.0f) =
        object : ReadWriteProperty<SPUtil, Float> {
            override fun getValue(thisRef: SPUtil, property: KProperty<*>): Float {
                synchronized(lock) {
                    return thisRef.preferences.getFloat(property.name, defaultValue)
                }
            }

            override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Float) {
                synchronized(lock) {
                    thisRef.preferences.edit { putFloat(property.name, value) }
                }
            }
        }

    fun string(defaultValue: String) = object : ReadWriteProperty<SPUtil, String> {
        override fun getValue(thisRef: SPUtil, property: KProperty<*>): String {
            synchronized(lock) {
                return thisRef.preferences.getString(property.name, defaultValue) ?: ""
            }
        }

        override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: String) {
            synchronized(lock) {
                thisRef.preferences.edit { putString(property.name, value) }
            }
        }
    }

    fun array() = object : ReadWriteProperty<SPUtil, ArrayList<String>> {
        override fun getValue(thisRef: SPUtil, property: KProperty<*>): ArrayList<String> {
            synchronized(lock) {
                val tmpArray = ArrayList<String>()
                val preferences = thisRef.preferences
                val count = preferences.getInt(property.name, 0)
                for (i in 0 until count) {
                    val tmp = preferences.getString("${property.name}_$i", null)
                    tmp?.let { tmpArray.add(it) }
                }
                return tmpArray
            }
        }

        override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: ArrayList<String>) {
            synchronized(lock) {
                val edit = thisRef.preferences.edit()
                edit.putInt(property.name, value.size)
                for ((index, v) in value.withIndex()) {
                    edit.putString("${property.name}_$index", v)
                }
                edit.apply()
            }
        }
    }
}