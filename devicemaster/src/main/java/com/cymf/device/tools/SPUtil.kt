package com.cymf.device.tools

import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.cymf.device.App
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import androidx.core.content.edit

object SPUtil {
    /**
     * 创建 SharedPreferences 对象
     */
    var preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.app)

    /***********************************************************************************************/
//    var HOST by SharedPreferenceDelegates.string("")
//    var PORT by SharedPreferenceDelegates.int(0)
    // 获取值
//    val host = SPUtil.HOST ?: ""
//    val port = SPUtil.PORT ?: 0
    // 设置值
//    SPUtil.HOST = "我是设置的新值"
//    SPUtil.PORT = 8080
    /***********************************************************************************************/

    var TOUCH_LOCATION by SharedPreferenceDelegates.int(0)

    var VPN_TOP_PKG by SharedPreferenceDelegates.string("")   //  前台App包名
    var VPN_PKG_LIST by SharedPreferenceDelegates.array()
}

/**
 * 定义类型 属性委托类
 */
private object SharedPreferenceDelegates {
    /**
     * 定义委托获取和设置对应类型的方法
     * 委托的原理,大家可以看我前面的文章
     */
    fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SPUtil, Int> {

        override fun getValue(thisRef: SPUtil, property: KProperty<*>): Int {
            /**
             * 当获取值的时候,调用此方法
             * key 值是对应变量的昵称
             */
            return SPUtil.preferences.getInt(property.name, defaultValue)
        }

        override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Int) {
            /**
             * 当设置值的时候,调用此方法
             * key 值是对应变量的昵称
             */
            SPUtil.preferences.edit { putInt(property.name, value) }
        }
    }

    fun long(defaultValue: Long = 0L) = object : ReadWriteProperty<SPUtil, Long> {

        override fun getValue(thisRef: SPUtil, property: KProperty<*>): Long {
            return SPUtil.preferences.getLong(property.name, defaultValue)
        }

        override fun setValue(
            thisRef: SPUtil,
            property: KProperty<*>,
            value: Long
        ) {
            SPUtil.preferences.edit().putLong(property.name, value).apply()
        }
    }

    fun boolean(defaultValue: Boolean = false) =
        object : ReadWriteProperty<SPUtil, Boolean> {
            override fun getValue(
                thisRef: SPUtil,
                property: KProperty<*>
            ): Boolean {
                return SPUtil.preferences.getBoolean(property.name, defaultValue)
            }

            override fun setValue(
                thisRef: SPUtil,
                property: KProperty<*>,
                value: Boolean
            ) {
                SPUtil.preferences.edit { putBoolean(property.name, value) }
            }
        }

    fun float(defaultValue: Float = 0.0f) =
        object : ReadWriteProperty<SPUtil, Float> {
            override fun getValue(thisRef: SPUtil, property: KProperty<*>): Float {
                return SPUtil.preferences.getFloat(property.name, defaultValue)
            }

            override fun setValue(
                thisRef: SPUtil,
                property: KProperty<*>,
                value: Float
            ) {
                SPUtil.preferences.edit().putFloat(property.name, value).apply()
            }
        }

    fun string(defaultValue: String) = object : ReadWriteProperty<SPUtil, String> {
        override fun getValue(thisRef: SPUtil, property: KProperty<*>): String {
            return SPUtil.preferences.getString(property.name, defaultValue) ?: ""
        }

        override fun setValue(
            thisRef: SPUtil,
            property: KProperty<*>,
            value: String
        ) {
            SPUtil.preferences.edit().putString(property.name, value).apply()
        }
    }

    fun array() = object : ReadWriteProperty<SPUtil, ArrayList<String>> {
        override fun getValue(thisRef: SPUtil, property: KProperty<*>): ArrayList<String> {
            val tmpArray = ArrayList<String>()
            val preferences = SPUtil.preferences
            val int = preferences.getInt(property.name, 0)
            for (i in int downTo 0) {
                val tmp = preferences.getString("${property.name}_$i", null)
                tmp ?: continue
                tmpArray.add(tmp)
            }
            return tmpArray
        }

        override fun setValue(
            thisRef: SPUtil,
            property: KProperty<*>,
            value: ArrayList<String>
        ) {
            val edit = SPUtil.preferences.edit()
            edit.putInt(property.name, value.size)
            for ((index, v) in value.withIndex()) {
                edit.remove("${property.name}_$index")
                edit.putString("${property.name}_$index", v)
            }
            edit.apply()
        }
    }
}