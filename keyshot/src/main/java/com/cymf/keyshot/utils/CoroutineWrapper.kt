package com.cymf.keyshot.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CoroutineWrapper {
    private var job = Job()
    private var coroutine: CoroutineScope = CoroutineScope(job + Dispatchers.IO)

    fun launch(isMain: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job {
        return coroutine.launch(context = if (isMain) Dispatchers.Main else Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
            } catch (e: Throwable) {
                e.printStackTrace()
            } catch (e: Error) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 启动一个无限循环任务，直到 [onEachIteration] 返回 false
     *
     * @param intervalMillis 间隔时间（毫秒）
     * @param isMain 是否运行在主线程
     * @param onEachIteration 每次循环执行的内容，返回值表示是否继续循环（true = 继续，false = 停止）
     * @param onFinish 循环结束后的回调
     */
    fun repeatUntil(
        intervalMillis: Long = 2000,
        isMain: Boolean = false,
        onEachIteration: suspend () -> Int,
        onFinish: ((Int) -> Unit)? = null
    ): Job {
        return launch(isMain) {
            try {
                while (true) {
                    val shouldContinue = onEachIteration()
                    if (shouldContinue != 0) {
                        onFinish?.invoke(shouldContinue)
                        break
                    }
                    delay(intervalMillis)
                }
            } catch (_: Exception) {
                // 可记录日志或处理异常
            }
        }
    }
}

suspend fun <T> runMain(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Main, block = block)
}

suspend fun <T> runIO(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO, block = block)
}