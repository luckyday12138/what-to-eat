package com.chisha.desktop

import com.chisha.data.DishPhoto
import kotlinx.coroutines.runBlocking

/**
 * 取图自检：不发 UI，按菜名批量跑「搜图 -> 选图」，把结果打出来。
 * 用法：:desktopApp:run --args="--phototest 红烧肉,水煮鱼,麻婆豆腐"
 */
fun runPhotoTest(names: String) {
    val list = names.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }
    var ok = 0
    for (n in list) {
        val t0 = System.currentTimeMillis()
        val url = runBlocking { DishPhoto.resolve(n) }
        val ms = System.currentTimeMillis() - t0
        if (url != null) ok++ else println("PROBE miss  $n ($ms ms)")
        if (url == null) println("PROBE dbg   " + DishPhoto.debugSearch(n))
        if (url != null) println("PROBE hit   $n -> $url ($ms ms)")
    }
    println("PROBE done  $ok/${list.size}")
}
