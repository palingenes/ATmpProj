package com.cymf.autogame.utils.net

import com.cymf.autogame.bean.TaskBean
import com.cymf.autogame.utils.SPUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// 回调定义
typealias SimpleCallback<T> = (data: T?, error: Exception?) -> Unit

class ApiException(val code: Int, override val message: String) : Exception(message)

object NetworkClient {

    private const val BASE_URL = "https://api.vantagereports.com"
//    private const val BASE_URL = "http://92.38.135.75:3888"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(RetryInterceptor(maxRetries = 3)) // 添加重试拦截器
        .build()


    private val retrofit by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    interface ApiService {
        @GET
        fun get(@Url url: String): Call<TaskBean>

        @POST("{url}") // 使用路径变量
        fun post(
            @Path("url") url: String,
            @Query("code") id: String,
            @Query("time") timestamp: Long,
            @Body body: RequestBody
        ): Call<TaskBean>

        @POST("/server/v1/complete")
        fun complete(
            @Query("code") id: String,
            @Query("time") timestamp: Long,
            @Body body: RequestBody
        ): Call<TaskBean>

        @POST("/server/v1/fatal")
        fun fatal(
            @Query("code") id: String,
            @Query("time") timestamp: Long,
            @Body body: RequestBody
        ): Call<TaskBean>
    }

    private val apiService = retrofit.create(ApiService::class.java)


    suspend fun get(phoneCode: String): Pair<TaskBean?, Exception?> =
        suspendCoroutine { continuation ->
            val call =
                apiService.get("/server/v1/task?code=${phoneCode}&time=${System.currentTimeMillis()}&m=1") as? Call<TaskBean>
                    ?: run {
                        continuation.resume(Pair(null, Exception("Failed to create call for URL")))
                        return@suspendCoroutine
                    }

            call.enqueue(object : Callback<TaskBean> {
                override fun onResponse(call: Call<TaskBean>, response: Response<TaskBean>) {
                    if (response.isSuccessful) {
                        continuation.resume(Pair(response.body(), null))
                    } else {
                        continuation.resume(
                            Pair(
                                null,
                                Exception("Request failed with code: ${response.code()}")
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<TaskBean>, t: Throwable) {
                    continuation.resume(Pair(null, Exception("Network error: ${t.message}", t)))
                }
            })
        }

    // POST 请求封装（返回 ApiResponse<T>）
    fun <T> post(url: String, jsonObject: String, callback: SimpleCallback<T>) {
        val contentType = "application/json".toMediaType()
        val body = jsonObject.toRequestBody(contentType)

        val call = apiService.post(
            url,
            SPUtil.PHONE_ID,
            System.currentTimeMillis(),
            body
        ) as? Call<TaskBean> ?: run {
            callback(null, Exception("Failed to create call for URL: $url"))
            return
        }

        call.enqueue(object : Callback<TaskBean> {
            override fun onResponse(call: Call<TaskBean>, response: Response<TaskBean>) {
                handleResponse(response, callback)
            }

            override fun onFailure(call: Call<TaskBean>, t: Throwable) {
                callback(null, Exception("Network error: ${t.message}", t))
            }
        })
    }

    // POST 请求封装（返回 ApiResponse<T>）
    fun <T> complete(jsonObject: String, callback: SimpleCallback<T>) {
        val contentType = "application/json".toMediaType()
        val body = jsonObject.toRequestBody(contentType)

        val call = apiService.complete(
            SPUtil.PHONE_ID,
            System.currentTimeMillis(),
            body
        ) as? Call<TaskBean> ?: run {
            callback(null, Exception("Failed to create call"))
            return
        }

        call.enqueue(object : Callback<TaskBean> {
            override fun onResponse(call: Call<TaskBean>, response: Response<TaskBean>) {
                handleResponse(response, callback)
            }

            override fun onFailure(call: Call<TaskBean>, t: Throwable) {
                callback(null, Exception("Network error: ${t.message}", t))
            }
        })
    }

    // POST 请求封装（返回 ApiResponse<T>）
    fun <T> fatal(jsonObject: String, callback: SimpleCallback<T>) {
        val contentType = "application/json".toMediaType()
        val body = jsonObject.toRequestBody(contentType)

        val call =
            apiService.fatal(SPUtil.PHONE_ID, System.currentTimeMillis(), body) as? Call<TaskBean>
                ?: run {
                    callback(null, Exception("Failed to create call"))
                    return
                }

        call.enqueue(object : Callback<TaskBean> {
            override fun onResponse(call: Call<TaskBean>, response: Response<TaskBean>) {
                handleResponse(response, callback)
            }

            override fun onFailure(call: Call<TaskBean>, t: Throwable) {
                callback(null, Exception("Network error: ${t.message}", t))
            }
        })
    }

    // 处理响应
    private fun <T> handleResponse(response: Response<TaskBean>, callback: SimpleCallback<T>) {
        if (response.isSuccessful && response.body() != null) {
            val apiResponse = response.body()
            if (apiResponse != null) {
                if (apiResponse.code == 200) {
                    callback(apiResponse as T?, null)
                } else {
                    callback(null, ApiException(apiResponse.code, apiResponse.msg ?: ""))
                }
            } else {
                callback(null, Exception("Server error: ${null} (Code: ${null})"))
            }
        } else {
            callback(null, Exception("Request failed: ${response.message()}"))
        }
    }
}