package com.cymf.autogame.utils

import com.cymf.autogame.bean.AdClicks
import com.cymf.autogame.bean.FatalBody
import com.cymf.autogame.bean.Task
import com.cymf.autogame.bean.TaskReqBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.forEach
import kotlin.jvm.java

object JsonConvert {

    // 全局 Moshi 实例
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // data class -> JSONObject
    fun <T : Any> T.toJsonObject(): JSONObject? {
        val adapter = moshi.adapter(this::class.java as Class<T>)
        val json = adapter.toJson(this)
        return try {
            JSONObject(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // List<data class> -> JSONArray
    fun <T : Any> List<T>.toJsonArray(): JSONArray? {
        val jsonArray = JSONArray()
        forEach { item ->
            val adapter = moshi.adapter(item::class.java as Class<T>)
            val json = adapter.toJson(item)
            jsonArray.put(JSONObject(json))
        }
        return jsonArray
    }

    fun buildTaskBody(
        isSuccess: Boolean,
        task: Task,
        adClickList: List<AdClicks>?
    ): String {
        val response = TaskReqBody(
            isSuccess = isSuccess,
            task = task,
            adclicks = adClickList
        )
        val jsonAdapter = moshi.adapter(TaskReqBody::class.java)
        return jsonAdapter.toJson(response)
    }

    fun buildFatalBody(
        title: String,
        detail: String,
        task: Task
    ): String {
        val response = FatalBody(
            title = title,
            detail = detail,
            task = task
        )
        val jsonAdapter = moshi.adapter(FatalBody::class.java)
        return jsonAdapter.toJson(response)
    }

    // data class -> JSON String（用于日志打印）
    fun <T : Any> toJsonString(obj: T): String? {
        val adapter = moshi.adapter(obj::class.java as Class<T>)
        return adapter.toJson(obj)
    }

    // JSON String -> data class
    inline fun <reified T> fromJsonString(json: String): T? {
        val adapter = moshi.adapter(T::class.java)
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}