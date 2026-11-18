package com.chisha.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.chisha.App
import com.chisha.resources.Res
import com.chisha.resources.ic_launcher
import org.jetbrains.compose.resources.painterResource

fun main(args: Array<String>) {
    // 后台逻辑自检：--selfcheck 只跑抽签/时段/权重断言，不弹窗口
    if (args.contains("--selfcheck")) {
        runSelfCheck()
        return
    }

    // 取图自检：--phototest 菜名，打印「按菜名搜图」的真实结果（不弹窗口）
    argValue(args, "--phototest")?.let { name ->
        runPhotoTest(name)
        return
    }

    // 换菜流自检：--feedtest N，连续取 N 道，看会不会打转（不弹窗口）
    argValue(args, "--feedtest")?.let { n ->
        runFeedTest(n.toIntOrNull() ?: 15)
        return
    }

    // 价格估算自检：--pricetest，打印档位分布与典型菜金额（不弹窗口）
    if (args.contains("--pricetest")) {
        runPriceTest()
        return
    }

    // 价格全量自查：--priceaudit，把本地库和在线灵感所有菜的估算连食材一起打出来
    if (args.contains("--priceaudit")) {
        runPriceAudit()
        return
    }

    // 首屏耗时自查：--batchtest，连取两次「首页推荐」批次，看第一次要等多久
    if (args.contains("--batchtest")) {
        runBatchTest()
        return
    }

    // 直开指定页：--tab 1 进发现页，--section 1 直接停在「在线灵感」
    val tab = argValue(args, "--tab")?.toIntOrNull() ?: 0
    val section = argValue(args, "--section")?.toIntOrNull() ?: 0
    val demoRecipe = args.contains("--demo")
    val demoResult = args.contains("--result")

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "今天吃什么",
            icon = painterResource(Res.drawable.ic_launcher),
            state = rememberWindowState(width = 400.dp, height = 840.dp),
        ) {
            App(initialTab = tab, initialSection = section, demoRecipe = demoRecipe, demoResult = demoResult)
        }
    }
}

/**
 * 取参数值。gradle 有时把 `--tab 1` 拆成两项，有时原样塞成一项，两种都认；
 * `--tab=1` 这种写法也认。
 */
private fun argValue(args: Array<String>, name: String): String? {
    args.joinToString(" ").let { flat ->
        Regex(Regex.escape(name) + "\\s*=\\s*(\\S+)").find(flat)?.let { return it.groupValues[1] }
        Regex(Regex.escape(name) + "\\s+(\\S+)").find(flat)?.let { return it.groupValues[1] }
    }
    return null
}

/** 首页「网络推荐」首屏到底等多久：第一次带补货+取图，第二次池子里已有货 */
private fun runBatchTest() {
    val slot = com.chisha.core.slotOfHour(com.chisha.core.PlatformStore.hourOfDay())
    kotlinx.coroutines.runBlocking {
        repeat(3) { round ->
            val t0 = System.currentTimeMillis()
            val list = com.chisha.data.OnlineSource.batch(8, slot)
            val cost = System.currentTimeMillis() - t0
            println(
                "BATCH #" + (round + 1) + " 取到 " + list.size + " 道 耗时=" + cost + "ms | " +
                    list.joinToString("、") { it.name }
            )
        }
    }
}
