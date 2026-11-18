package com.chisha.desktop

import com.chisha.data.ChishaRepo
import com.chisha.data.OnlineSource
import com.chisha.data.PriceTag
import kotlinx.coroutines.runBlocking

/**
 * 价格自查：把本地库全量和在线灵感拿到的菜都连估算一起打出来，
 * 用来看解析有没有失手、金额合不合常理。
 * 用法：:desktopApp:run --args="--priceaudit"
 */
fun runPriceAudit() {
    val repo = ChishaRepo.instance.also { it.load() }
    println("AUDIT ==== 本地库 ${repo.dishes.size} 道 ====")
    for (d in repo.dishes) {
        println(
            "AUDIT L %-12s %-8s ing=[%s]".format(
                d.name,
                d.priceText + " " + d.priceHint,
                d.ingredients.joinToString("/"),
            ),
        )
    }

    println("AUDIT ==== 在线灵感（真实接口返回，含解析情况）====")
    runBlocking {
        OnlineSource.excludeLocal(repo.dishes.map { it.name }.toSet())
        OnlineSource.warm()
        repeat(14) {
            val m = OnlineSource.shift() ?: return@repeat
            val lv = PriceTag.level(m.name, m.ingredients)
            println(
                "AUDIT N %-12s %-5s %-8s ing=[%s]".format(
                    m.name, "¥".repeat(lv),
                    "自制约 " + PriceTag.money(m.name, m.ingredients, lv) + " 元",
                    m.ingredients.joinToString("/"),
                ),
            )
        }
    }
}
