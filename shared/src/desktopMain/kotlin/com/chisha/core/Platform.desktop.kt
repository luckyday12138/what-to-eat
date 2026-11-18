package com.chisha.core

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dir: File by lazy {
    File(System.getProperty("user.home"), ".chisha").apply { mkdirs() }
}

actual object PlatformStore {
    actual fun read(name: String): String? =
        runCatching { File(dir, name).takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull()

    actual fun write(name: String, text: String) {
        runCatching { File(dir, name).writeText(text, Charsets.UTF_8) }
    }

    actual fun nowMillis(): Long = System.currentTimeMillis()

    actual fun hourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    actual fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
}
