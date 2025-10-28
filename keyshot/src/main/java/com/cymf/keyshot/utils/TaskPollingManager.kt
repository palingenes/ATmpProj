package com.cymf.keyshot.utils

import kotlin.random.Random

object TaskPollingManager {

    fun getCurrTaskSpeed(duration: Long = -1): Long {
        val baseDuration = if (duration == -1L) {
            Random.nextInt(50, 200).toLong()
        } else duration

        return (baseDuration * Random.nextDouble(0.5, 1.8)).toLong()
    }

}