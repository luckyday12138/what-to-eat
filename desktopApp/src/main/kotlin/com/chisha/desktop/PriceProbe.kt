package com.chisha.desktop

import com.chisha.data.ChishaRepo
import com.chisha.data.PriceTag

/**
 * 价格估算自检：打印本地菜库的档位分布 + 几道典型菜，确认估出来的钱合不合常理。
 * 用法：:desktopApp:run --args="--pricetest"
 */
fun runPriceTest() {
    val repo = ChishaRepo.instance.also { it.load() }
    val byLevel = repo.dishes.groupingBy { it.priceLevel }.eachCount()
    println("PRICE 本地菜库 ${repo.dishes.size} 道，档位分布: " + byLevel.entries
        .sortedBy { it.key }
        .joinToString("  ") { "${"¥".repeat(it.key)}=${it.value}" })

    val samples = listOf(
        "炒青菜", "番茄炒蛋", "麻婆豆腐", "酸辣土豆丝", "凉拌黄瓜",
        "红烧肉", "宫保鸡丁", "鱼香肉丝", "水煮肉片", "糖醋排骨",
        "清蒸鲈鱼", "红烧带鱼", "葱烧海参", "白灼虾", "蒜蓉蒸扇贝",
        "葱烧海参", "清蒸大闸蟹", "蒜香龙虾",
    )
    for (n in samples) {
        println("PRICE %-8s %-6s %s".format(n, "¥".repeat(PriceTag.level(n)), PriceTag.hint(n)))
    }

    println("PRICE 带食材用量的估算（模拟接口返回）：")
    val withIng = listOf(
        "青椒肉丝" to listOf("猪肉 150克", "青椒 100克", "食用油 15克", "盐 3克"),
        "番茄牛腩" to listOf("牛腩 500克", "番茄 200克", "洋葱 100克"),
        "蒜蓉粉丝蒸扇贝" to listOf("扇贝 8只", "粉丝 50克", "大蒜 30克"),
        "清炒时蔬" to listOf("青菜 300克", "蒜 10克"),
    )
    for ((n, ing) in withIng) {
        println("PRICE %-10s %-6s %s   <- %s".format(n, "¥".repeat(PriceTag.level(n, ing)), PriceTag.hint(n, ing), ing.joinToString("/")))
    }

    // 逐项拆：确认「羊肉串」这种带泛词的写法有没有把羊价吃掉
    println("PRICE 单项拆解（300 克）:")
    listOf("羊肉 300克", "羊肉串 300克", "羊肉片 300克", "羊腿 300克", "牛腩 300克", "牛肉 300克", "牛肉末 300克")
        .forEach { raw ->
            val n = raw.takeWhile { !it.isDigit() }.trim()
            println("PRICE   %-10s -> %s".format(raw, PriceTag.debug(raw)))
        }

    // 本地库里最贵和最便宜的各看几条，方便判断有没有明显跑偏
    println("PRICE 库里最贵 5 道:")
    repo.dishes.sortedByDescending { PriceTag.amount(it.name, it.ingredients) }.take(5).forEach {
        println("PRICE   %-10s %s".format(it.name, PriceTag.hint(it.name, it.ingredients)))
    }
    println("PRICE 库里最便宜 5 道:")
    repo.dishes.sortedBy { PriceTag.amount(it.name, it.ingredients) }.take(5).forEach {
        println("PRICE   %-10s %s".format(it.name, PriceTag.hint(it.name, it.ingredients)))
    }
}
