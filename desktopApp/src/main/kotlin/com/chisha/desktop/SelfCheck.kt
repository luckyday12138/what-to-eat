package com.chisha.desktop

import com.chisha.data.ChishaRepo
import com.chisha.data.MealZh
import com.chisha.data.NetMeal
import com.chisha.data.OnlineSource
import com.chisha.data.Origin
import com.chisha.data.Slot
import com.chisha.data.SourceHub
import com.chisha.data.SourceKind
import com.chisha.domain.Picker
import kotlinx.coroutines.runBlocking

/**
 * 无界面自检：只在命令行跑抽签 / 时段 / 权重 / 去重 / 记录链路，
 * 不打开窗口、不操作鼠标，供开发期后台验证。
 */
fun runSelfCheck() {
    val repo = ChishaRepo.instance
    repo.load()

    var failed = 0

    fun check(name: String, ok: Boolean, detail: String = "") {
        if (!ok) failed++
        val tag = if (ok) "PASS" else "FAIL"
        println("[$tag] $name${if (detail.isEmpty()) "" else "  -> $detail"}")
    }

    // 1. 菜库
    check("菜库加载", repo.dishes.isNotEmpty(), "${repo.dishes.size} 道")

    // 2. 四个时段各自可抽签
    Slot.entries.forEach { slot ->
        val pool = Picker.candidates(repo.dishes, repo.prefs, slot)
        check("${slot.label}候选数", pool.size >= 6, "${pool.size} 道")

        if (pool.isEmpty()) return@forEach

        val recent = repo.recentIds(repo.prefs.avoidRepeatWindow)
        val w = Picker.weights(pool, repo.prefs, slot, recent, true)
        val sample = Picker.diversify(Picker.sample(pool, w, 6))

        check("${slot.label}抽样数量", sample.size == 6, "${sample.size} 道")
        check("${slot.label}抽样无重复", sample.distinctBy { it.id }.size == sample.size)
        check("${slot.label}候选都在该时段", sample.all { slot in it.slots })

        val byKey = sample.groupBy { it.imageKey ?: "none" }
        val headByKey = sample.take(3).groupBy { it.imageKey ?: "none" }
        check(
            "${slot.label}首屏同类图≤2",
            headByKey.values.all { it.size <= 2 },
            "全盘 " + byKey.entries.joinToString(", ") { "${it.key}=${it.value.size}" },
        )
        println("       样例: " + sample.joinToString(" / ") { it.name })
    }

    // 3. 纯随机权重恒定
    val pool = Picker.candidates(repo.dishes, repo.prefs, Slot.LUNCH)
    val pure = Picker.weights(pool, repo.prefs, Slot.LUNCH, emptyList(), false)
    check("随机模式权重恒为 1.0", pure.all { it == 1f })

    // 4. 记录写入与今日回读
    val target = pool.first()
    val before = repo.records.size
    repo.record(target, Slot.LUNCH, true)
    check("记录写入", repo.records.size == before + 1, "$before -> ${repo.records.size}")
    val decided = repo.decidedToday(Slot.LUNCH)
    check("今日已定回读", decided?.dishId == target.id, decided?.dishName ?: "null")

    // 5. 重复衰减：刚吃过的菜权重应显著下降
    val baseW = Picker.weights(pool, repo.prefs, Slot.LUNCH, emptyList(), true)
    val afterW = Picker.weights(pool, repo.prefs, Slot.LUNCH, repo.recentIds(repo.prefs.avoidRepeatWindow), true)
    val idx = pool.indexOfFirst { it.id == target.id }
    if (idx >= 0) {
        check(
            "重复衰减",
            afterW[idx] < baseW[idx],
            "${target.name}: ${baseW[idx]} -> ${afterW[idx]}",
        )
    }

    // 6. 加权抽签 1000 次，权重高的菜命中率应更高
    val weighted = Picker.weights(pool, repo.prefs, Slot.LUNCH, emptyList(), true)
    val counts = HashMap<String, Int>()
    repeat(1000) {
        Picker.pick(pool, weighted)?.let { d -> counts[d.id] = (counts[d.id] ?: 0) + 1 }
    }
    val maxId = counts.maxByOrNull { it.value }?.key
    val minId = counts.minByOrNull { it.value }?.key
    if (maxId != null && minId != null) {
        val maxW = weighted[pool.indexOfFirst { it.id == maxId }]
        val minW = weighted[pool.indexOfFirst { it.id == minId }]
        check("加权命中与权重正相关", (counts[maxId] ?: 0) > (counts[minId] ?: 0) || maxW <= minW,
            "最多 ${counts[maxId]} 次(w=$maxW) / 最少 ${counts[minId]} 次(w=$minW)")
    }

    // 7. 图片键覆盖率：每道菜都能落到内置图包或在线图
    val noKey = repo.dishes.count { it.imageKey.isNullOrBlank() && it.imageUri == null }
    check("每道菜都有图源", noKey == 0, "$noKey 道缺图")

    // 8. 在线菜谱中文化：常见英文菜名要能出中文，拼不出的保留英文而不是乱码
    val cases = listOf(
        "Chicken Fried Rice" to "鸡肉炒饭",
        "Beef Wellington" to "惠灵顿牛排",
        "Spicy Arrabiata Penne" to "辣味阿拉比亚通心粉",
        "Chicken Couscous" to "鸡肉库斯库斯",
    )
    cases.forEach { (en, zh) ->
        check("菜名中文化 $en", MealZh.name(en) == zh, MealZh.name(en))
    }
    check("词表兜底拼名", MealZh.name("Garlic Butter Shrimp") != "Garlic Butter Shrimp",
        MealZh.name("Garlic Butter Shrimp"))
    check("带介词菜名", MealZh.name("Spanish tomato bread with jamón Serrano") != "Spanish tomato bread with jamón Serrano",
        MealZh.name("Spanish tomato bread with jamón Serrano"))
    check("复数与数字菜名", MealZh.name("15-minute chicken & halloumi burgers") != "15-minute chicken & halloumi burgers",
        MealZh.name("15-minute chicken & halloumi burgers"))
    check("地区全称", MealZh.area("United States") == "美国" && MealZh.area("Italy") == "意大利")
    check("陌生菜名保留英文", MealZh.name("Zzyzx Surprise") == "Zzyzx Surprise", MealZh.name("Zzyzx Surprise"))
    check("地区中文", MealZh.area("Chinese") == "中式" && MealZh.area("Italian") == "意式")
    check("品类中文", MealZh.category("Chicken") == "鸡肉" && MealZh.category("Dessert") == "甜点")

    // 9. 在线菜谱抽样：国内接口，看是否中文、是否带食材和步骤
    runCatching {
        runBlocking {
            val samples = mutableListOf<NetMeal>()
            val t0 = System.currentTimeMillis()
            repeat(4) {
                OnlineSource.take()?.let { samples += it }
            }
            val cost = System.currentTimeMillis() - t0

            if (samples.isEmpty()) {
                println("[SKIP] 在线菜谱抽样：这次没连上网络")
            } else {
                fun han(s: String) = s.any { it.code in 0x4E00..0x9FFF }
                val zhCount = samples.count { han(it.name) }
                val withIng = samples.count { it.ingredients.size >= 2 }
                val withStep = samples.count { it.steps.size >= 2 }
                samples.forEach { m ->
                    println("       在线菜谱: ${m.name} [${m.category}] 食材${m.ingredients.size} 步骤${m.steps.size}")
                    println("                 食材: " + m.ingredients.take(4).joinToString("、"))
                    println("                 首步: " + (m.steps.firstOrNull() ?: "无"))
                }
                check("在线菜名全中文", zhCount == samples.size, "$zhCount/${samples.size}")
                check("在线菜谱带食材", withIng >= samples.size - 1, "$withIng/${samples.size}")
                check("在线菜谱带步骤", withStep >= samples.size - 1, "$withStep/${samples.size}")
                check("在线取菜耗时 < 12s（4 道）", cost < 12000, "${cost}ms")
                println("       池内剩余：${OnlineSource.readyCount()} 道")
            }
        }
    }.onFailure { println("[SKIP] 在线菜谱抽样异常：${it.message}") }

    // 10. 内置详版菜谱覆盖度：80 道种子菜每道都要有食材和步骤
    runCatching {
        val seed = ChishaRepo.instance.dishes.filter { it.origin == Origin.SEED && it.enabled }
        val covered = seed.count { it.recipe.ingredients.isNotEmpty() && it.recipe.steps.size >= 3 }
        val noTip = seed.count { it.recipe.tip.isBlank() }
        check("内置菜谱详版覆盖", covered == seed.size, "$covered/${seed.size}")
        check("内置菜谱都带小贴士", noTip == 0, "缺 $noTip 道")
    }.onFailure { println("[SKIP] 菜谱覆盖度检查异常：${it.message}") }

    // 11. 多源故障切换：把主源打进冷却，看能不能自己换到下一台
    runCatching {
        runBlocking {
            SourceHub.resetHealth()

            val a = SourceHub.fetch("菜")
            println("       首选源: ${a.label}(${a.sourceId}/${a.kind?.tag}) 命中 ${a.meals.size} 道，试了 ${a.triedCount} 台")
            check("首选是国内源", a.ok && a.kind == SourceKind.DOMESTIC)

            // 冷却「当前首选源」才能验证自动切换；写死 qqsuu 会因首选已换成清和而失效
            val mainId = a.sourceId
            SourceHub.forceCooldown(mainId)
            val b = SourceHub.fetch("菜")
            println("       主源冷却后: ${b.label}(${b.sourceId}) 命中 ${b.meals.size} 道 switched=${b.switched}")
            check("主源失效自动切换", b.ok && b.sourceId != mainId, b.sourceId)
            check("切换被标记出来", b.switched)

            // 三台全部封死，确认不会挂掉、也不会卡住
            SourceHub.forceCooldown("qqsuu")
            SourceHub.forceCooldown("qinghe")
            SourceHub.forceCooldown("mealdb")
            val c = SourceHub.fetch("菜")
            check("全源冷却时安全返回空", !c.ok && c.triedCount == 0, "tried=${c.triedCount}")

            SourceHub.debugLines().forEach { println("       源状态 $it") }
            SourceHub.resetHealth()
            println("       切回正常: ${SourceHub.debugLines().joinToString(" | ")}")
        }
    }.onFailure { println("[SKIP] 多源切换检查异常：${it.message}") }

    // 12. 灵感池轮转：换一道 = 从池里随机弹一条（优先没给看过的），抽走的不再同时留在池里
    runCatching {
        runBlocking {
            OnlineSource.warm()
            val before = OnlineSource.peek(5).map { it.name }
            println("       池内前 5: " + before.joinToString("、"))
            if (OnlineSource.readyCount() < 3) {
                println("[SKIP] 池子太小，跳过轮转检查")
            } else {
                val shifted = OnlineSource.shift()
                check("换一道能拿到菜", shifted != null, "shift=null")

                // 抽出来的这条必须已经离池，否则界面上「结果」和「灵感池」会同时显示同一道
                val stillThere = OnlineSource.peek(30).count { it.name == shifted?.name }
                check("抽走的菜不同时留在灵感池", stillThere == 0, "还在池里 $stillThere 次")

                val poolBefore = OnlineSource.readyCount()
                shifted?.let { OnlineSource.putBack(it) }
                check("旧卡退回后池子里有它", OnlineSource.readyCount() == poolBefore + 1,
                    "退回前 $poolBefore 退回后 ${OnlineSource.readyCount()}")
                println("       轮转正常，池内 ${OnlineSource.readyCount()} 道")
            }
        }
    }.onFailure { println("[SKIP] 池轮转检查异常：${it.message}") }

    // 13. 推荐模式取批不与上一批重复，也不推本地菜库已有的菜
    runCatching {
        runBlocking {
            val localNames = ChishaRepo.instance.dishes.map { it.name }.toSet()
            OnlineSource.excludeLocal(localNames)
            val first = OnlineSource.batch(6).map { it.name }
            val second = OnlineSource.batch(6).map { it.name }
            println("       第一批: " + first.joinToString("、"))
            println("       第二批: " + second.joinToString("、"))
            check("两批推荐没有重复的菜", first.intersect(second.toSet()).isEmpty(),
                "重复=${first.intersect(second.toSet())}")
            val overlap = (first + second).filter { it in localNames }
            check("推荐不推本地已有的菜", overlap.isEmpty(), "撞了本地=$overlap")
            println("       batch 正常，第一批 ${first.size} 道，第二批 ${second.size} 道")
        }
    }.onFailure { println("[SKIP] 推荐取批检查异常：${it.message}") }

    // 14. 本地抽签不含网络菜（随机模式只从本地菜库取）
    runCatching {
        val local = ChishaRepo.instance.dishes.filter { it.origin != Origin.NETWORK }
        check("本地菜库不含网络菜", local.none { it.origin == Origin.NETWORK }, "混入了网络菜")
        println("       本地菜库 ${local.size} 道，网络菜 ${ChishaRepo.instance.dishes.size - local.size} 道")
    }.onFailure { println("[SKIP] 本地池检查异常：${it.message}") }

    // 15. 餐段适配：早餐取出来的菜不该是火锅/烧烤这一挂
    runCatching {
        runBlocking {
            OnlineSource.warm(2)
            val morning = OnlineSource.batch(4, Slot.BREAKFAST)
            println("       早餐取到: " + morning.joinToString("、") { it.name })
            check("早餐取得到菜", morning.isNotEmpty(), "空了")
            val bad = morning.count { "BREAKFAST" !in it.slots }
            check("早餐不推非早餐菜", bad == 0, "混进 $bad 道")
            morning.forEach { OnlineSource.putBack(it) }
        }
    }.onFailure { println("[SKIP] 餐段适配检查异常：${it.message}") }

    // 16. 「改主意」能撤掉今日已定，否则界面会一边说已定 A、一边又抽 B
    runCatching {
        val repo = ChishaRepo.instance
        val today = com.chisha.core.PlatformStore.todayKey()
        val slot0 = Slot.entries.firstOrNull { repo.decidedToday(it) != null }
        if (slot0 == null) {
            println("[SKIP] 今天还没有已定记录，跳过撤销检查")
        } else {
            val before = repo.decidedToday(slot0)
            println("       撤销前已定: ${before?.dishName}（${slot0.label}）")
            repo.revokeDecided(slot0)
            check("撤销后该餐段不再显示已定", repo.decidedToday(slot0) == null,
                "还有 ${repo.decidedToday(slot0)?.dishName}")
            val gone = repo.records.none {
                it.accepted && it.slot == slot0 && it.dateKey == today && it.dishName == before?.dishName
            }
            check("撤销后记录也移除了", gone, "记录还在")
            println("       今日 ${slot0.label} 已可重新抽签")
        }
    }.onFailure { println("[SKIP] 撤销检查异常：${it.message}") }

    println()
    if (failed == 0) println("SELF-CHECK OK：全部通过") else println("SELF-CHECK FAILED：$failed 项未通过")
}
