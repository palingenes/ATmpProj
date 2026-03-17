package com.cymf.tmp.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object ShellExecutor {
    fun exec(command: String): String = runBlocking {
        withContext(Dispatchers.IO) {
            val process = Runtime.getRuntime().exec(command)
            val output = readStream(process.inputStream)
            val error = readStream(process.errorStream)
            process.waitFor()
            "Output:\n$output\nError:\n$error"
        }
    }

    private fun readStream(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        return sb.toString()
    }
}