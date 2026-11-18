package com.chisha.desktop

import com.chisha.data.ChishaRepo
import com.chisha.data.OnlineSource
import kotlinx.coroutines.runBlocking

/**
 * 换菜流自检：模拟连续点「换一道」，看会不会在几道菜里打转。
 * 用法：:desktopApp:run --args="--feedtest 15"
 */
fun runFeedTest(n: Int) {
    val repo = ChishaRepo.instance.also { it.load() }
    OnlineSource.excludeLocal(repo.dishes.map { p -> p.name }.toSet())

    runBlocking {
        OnlineSource.warm()
        val seen = mutableListOf<String>()
        val costs = mutableListOf<Long>()
        repeat(n) { i ->
            val t0 = System.currentTimeMillis()
            val m = OnlineSource.shift()
            val ms = System.currentTimeMillis() - t0
            costs.add(ms)
            val name = m?.name ?: "null"
            seen.add(name)
            println(
                "FEED %2d. %-12s [%s] 池内=%d 耗时=%dms".format(
                    i + 1, name, m?.category ?: "", OnlineSource.readyCount(), ms,
                ),
            )
        }
        val dup = seen.groupingBy { it }.eachCount().filter { it.value > 1 }
        println("FEED distinct=${seen.distinct().size}/$n dup=$dup")
        println(
            "FEED 耗时 平均=%dms 最慢=%dms 超500ms=%d次".format(
                costs.average().toLong(), costs.maxOrNull() ?: 0L, costs.count { it > 500 },
            ),
        )
    }
}
