package com.chisha.core

/** 平台能力（Android / Desktop 各自实现，后续接 iOS / Web 只需新增 actual） */
expect object PlatformStore {
    fun read(name: String): String?
    fun write(name: String, text: String)
    fun nowMillis(): Long
    /** 本地时区小时 0-23 */
    fun hourOfDay(): Int
    /** 本地日期 yyyy-MM-dd */
    fun todayKey(): String
}

/** 时段问候 */
fun greeting(hour: Int): String = when (hour) {
    in 5..9 -> "早上好"
    in 10..13 -> "中午好"
    in 14..17 -> "下午好"
    in 18..22 -> "晚上好"
    else -> "夜深了"
}

fun slotOfHour(hour: Int): com.chisha.data.Slot = when (hour) {
    in 4..9 -> com.chisha.data.Slot.BREAKFAST
    in 10..14 -> com.chisha.data.Slot.LUNCH
    in 15..20 -> com.chisha.data.Slot.DINNER
    else -> com.chisha.data.Slot.MIDNIGHT
}
