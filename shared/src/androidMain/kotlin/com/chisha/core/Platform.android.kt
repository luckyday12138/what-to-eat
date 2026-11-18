package com.chisha.core

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AndroidPlatform {
    lateinit var context: Context
        private set

    val isReady: Boolean get() = this::context.isInitialized

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }
}

actual object PlatformStore {
    private fun file(name: String): File? {
        if (!AndroidPlatform.isReady) return null
        return File(AndroidPlatform.context.filesDir, name)
    }

    actual fun read(name: String): String? = runCatching {
        file(name)?.takeIf { it.exists() }?.readText(Charsets.UTF_8)
    }.getOrNull()

    actual fun write(name: String, text: String) {
        runCatching { file(name)?.writeText(text, Charsets.UTF_8) }
    }

    actual fun nowMillis(): Long = System.currentTimeMillis()

    actual fun hourOfDay(): Int =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    actual fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
}
