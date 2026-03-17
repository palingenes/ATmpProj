package com.cymf.humanbehaviorcollector

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

object GestureRecordManager {

    private val moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    private val listType = Types.newParameterizedType(List::class.java, GestureRecord::class.java)
    private val jsonAdapter: JsonAdapter<List<GestureRecord>> = moshi.adapter(listType)

    /**
     * 读取指定文件中的手势记录
     */
    private suspend fun readRecords(context: Context, filename: String): List<GestureRecord> {
        return withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                try {
                    val json = file.readText(Charset.defaultCharset())
                    jsonAdapter.fromJson(json) ?: emptyList()
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    /**
     * 向指定文件追加一条手势记录
     */
    suspend fun appendRecord(context: Context, filename: String, record: GestureRecord) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            val existingList = readRecords(context, filename).toMutableList()
            existingList.add(record)

            // 将更新后的列表转换为 JSON 并写入文件
            val newJson = jsonAdapter.toJson(existingList)
            try {
                file.writeText(newJson, Charset.defaultCharset())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


@JsonClass(generateAdapter = true)
data class GestureRecord(
    val viewRect: String,
    val downTime: Long,
    val touch: String,
    val action: String
)