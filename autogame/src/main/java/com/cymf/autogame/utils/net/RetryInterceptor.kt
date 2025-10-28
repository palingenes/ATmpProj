package com.cymf.autogame.utils.net

import com.cymf.autogame.utils.YLLogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.min

class RetryInterceptor(
    private val maxRetries: Int = 5,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 10000,
    private val factor: Double = 2.0
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var currentDelay = initialDelayMs
        var tryCount = 0
        val request = chain.request()
        if (request.method != "POST") {
            // 不是POST请求，直接返回
            return chain.proceed(request)
        }

        var lastException: IOException? = null

        while (tryCount <= maxRetries) {
            try {
                return chain.proceed(request)
            } catch (e: IOException) {
                lastException = e
                if (!isNetworkError(e)) {
                    throw e // 非网络异常，不重试
                }
                YLLogger.i("Network error: ${e.javaClass.simpleName}, retrying... (attempt $tryCount)")
            }

            // 已达最大重试次数
            if (tryCount == maxRetries) {
                break
            }

            // 指数退避 + 延迟
            Thread.sleep(currentDelay)
            currentDelay = min((currentDelay * factor).toLong(), maxDelayMs)
            tryCount++
        }

        // 所有重试失败，抛出最后一次异常
        throw lastException ?: IOException("Unknown error after $maxRetries retries")
    }

    // 判断是否为可重试的网络异常
    private fun isNetworkError(e: Exception): Boolean {
        return e is SocketTimeoutException ||
                e is ConnectException ||
                e is UnknownHostException ||
                e is SocketException
    }
}