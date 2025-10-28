package com.cymf.autogame.utils

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException


class GlobalTimer private constructor() : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    private var startTimeMillis: Long = 0
    private var isRunning = false

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> = _currentTime

    @SuppressLint("DefaultLocale")
    fun start() {
        if (isRunning) stop()

        startTimeMillis = System.currentTimeMillis()
        isRunning = true

        launch {
            try {
                while (isRunning) {
                    val totalSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    val timeString = String.format("%02d:%02d", minutes, seconds)

                    _currentTime.value = timeString
                    delay(1000)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                YLLogger.e("Timer error", e)
            }
        }
        _currentTime.value = "00:00"
    }

    fun stop() {
        isRunning = false
    }

    fun reset() {
        stop()
        _currentTime.value = "00:00"
    }

    fun getCurrentTime(): String = _currentTime.value ?: "00:00"

    fun checkTimed(time: Long): Boolean {
        val currentTimeInSeconds = getCurrentTimeInSeconds()
        val lng = time * 60
//        YLLogger.e("currentTimeInSeconds-$currentTimeInSeconds  , lng=$lng")
        return currentTimeInSeconds > lng
    }

    fun getCurrentTimeInSeconds(): Long {
        if (startTimeMillis == 0L) {
            return 0L
        }
        val toSeconds = _currentTime.value?.split(":")?.let { parts ->
            try {
                (parts[0].toInt() * 60) + parts[1].toInt()
            } catch (_: Exception) {
                0
            }
        } ?: 0
        val currentMillis = if (isRunning) System.currentTimeMillis() else {
            startTimeMillis + toSeconds.times(1000)
        }
        return (currentMillis - startTimeMillis) / 1000
    }

    companion object {
        @Volatile
        private var instance: GlobalTimer? = null
        fun getInstance(): GlobalTimer = instance ?: synchronized(this) {
            instance ?: GlobalTimer().also { instance = it }
        }
    }
}