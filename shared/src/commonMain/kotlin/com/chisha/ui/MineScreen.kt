package com.chisha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chisha.data.ChishaRepo
import com.chisha.data.Slot
import com.chisha.ui.components.Chip
import com.chisha.ui.components.PillButton
import com.chisha.ui.theme.LocalPalette
import com.chisha.ui.theme.ThemeStyle
import com.chisha.ui.theme.paletteOf
import com.chisha.core.PlatformStore
import com.chisha.core.slotOfHour
import com.chisha.data.OnlineSource
import com.chisha.data.SourceHub

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MineScreen() {
    val repo = ChishaRepo.instance
    val p = LocalPalette.current
    val slot = slotOfHour(PlatformStore.hourOfDay())

    var showExport by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var srcPref by remember { mutableStateOf(SourceHub.preferred) }

    val accepted = repo.totalAccepted()
    val days = repo.activeDays()
    val cuisines = repo.topCuisines(3)
    val last7 = repo.last7Days()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(30.dp))
        Text("我的", style = MaterialTheme.typography.headlineLarge, color = p.text)
        Spacer(Modifier.height(16.dp))

        // 数据概要
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(p.card)
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatBlock("$accepted", "次决定")
            StatBlock("$days", "天打卡")
            StatBlock("${repo.dishes.size}", "道菜")
        }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最近 7 天", color = p.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("共 ${last7.sumOf { it.second }} 顿", color = p.faint, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(74.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val max = (last7.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
            if (last7.sumOf { it.second } == 0) {
                Text("还没有记录，去转一次吧", color = p.faint, fontSize = 12.sp)
            } else {
                last7.forEach { (date, count) ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((4 + 52 * (count.toFloat() / max)).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.verticalGradient(listOf(p.accent, p.accent2.copy(alpha = 0.6f))))
                        )
                        Text(
                            date.takeLast(2),
                            color = p.faint,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        // 最近吃过哪些：只列点过「就它了」的，按时间倒序
        val recentMeals = remember(repo.records.size) {
            repo.records.filter { it.accepted }.sortedByDescending { it.at }.take(8)
        }
        if (recentMeals.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("最近吃过", color = p.sub, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            recentMeals.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        r.dishName,
                        color = p.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(r.slot.label, color = p.faint, fontSize = 10.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(r.dateKey.takeLast(5).replace("-", "/"), color = p.faint, fontSize = 10.sp)
                }
            }
        }

        if (cuisines.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("最常吃", color = p.sub, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cuisines.forEach { (name, count) ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(p.card)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(name, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("  $count", color = p.accent, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("在线源（首页推荐 / 在线灵感用哪个）", color = p.sub, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(
                "自动",
                selected = srcPref.isEmpty(),
                onClick = {
                    SourceHub.setPreferred("")
                    OnlineSource.clearPool()
                    srcPref = ""
                },
            )
            SourceHub.options().forEach { (id, label, tag) ->
                Chip(
                    "$label · $tag",
                    selected = srcPref == id,
                    onClick = {
                        SourceHub.setPreferred(id)
                        OnlineSource.clearPool()
                        srcPref = id
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("选中的源连不上时会自动换下一个，恢复后再回来", color = p.faint, fontSize = 11.sp)

        Spacer(Modifier.height(26.dp))
        Text("主题", color = p.sub, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeStyle.entries.forEach { style ->
                val pal = paletteOf(style, slot)
                val sel = repo.prefs.theme == style.key
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(pal.bgTop, pal.bgBottom)))
                            .then(if (sel) Modifier.border(2.dp, pal.accent, RoundedCornerShape(18.dp)) else Modifier)
                            .clickable { repo.updatePrefs { it.copy(theme = style.key) } },
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .size(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Brush.horizontalGradient(listOf(pal.accent, pal.accent2))),
                        )
                        if (sel) {
                            Text(
                                "当前",
                                color = pal.accent,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            )
                        }
                    }
                    Text(
                        style.label,
                        color = if (sel) p.text else p.faint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("偏好", color = p.sub, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.card).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("避免重复", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("最近吃过的菜会被降低命中率", color = p.faint, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(3, 5, 8).forEach { w ->
                    Chip(
                        "$w",
                        selected = repo.prefs.avoidRepeatWindow == w,
                        onClick = { repo.updatePrefs { it.copy(avoidRepeatWindow = w) } },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.card).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("震动反馈", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("转盘落定、按钮点击时轻震", color = p.faint, fontSize = 11.sp)
            }
            Switch(
                checked = repo.prefs.haptics,
                onCheckedChange = { enabled -> repo.updatePrefs { prefs -> prefs.copy(haptics = enabled) } },
            )
        }

        Spacer(Modifier.height(10.dp))
        Text("忌口（命中即排除，不参与抽签）", color = p.faint, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("辣", "海鲜", "牛肉", "猪肉", "油炸", "奶", "含麸质", "香菜", "内脏").forEach { tag ->
                Chip(
                    tag,
                    selected = tag in repo.prefs.allergies,
                    onClick = {
                        repo.updatePrefs { prefs ->
                            prefs.copy(
                                allergies = if (tag in prefs.allergies) prefs.allergies - tag else prefs.allergies + tag
                            )
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("数据", color = p.sub, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("导出备份", Modifier.weight(1f), primary = false) { showExport = true }
            PillButton("导入恢复", Modifier.weight(1f), primary = false) { showImport = true }
        }

        Spacer(Modifier.height(26.dp))
        Text(
            "今天吃什么 · 1.2  ·  Kotlin Multiplatform\nAndroid / iOS / 桌面 / Web 同一套代码",
            color = p.faint,
            fontSize = 11.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(180.dp))
    }

    if (showExport) {
        Dialog(onDismissRequest = { showExport = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(p.bgBottom)
                    .border(1.dp, p.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Text("备份数据", style = MaterialTheme.typography.titleLarge, color = p.text)
                Spacer(Modifier.height(6.dp))
                Text("复制下面全部内容留存，可随时导入恢复", color = p.faint, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .height(220.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.bgTop)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                ) {
                    Text(repo.exportJson(), color = p.sub, fontSize = 10.sp, lineHeight = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showExport = false }) { Text("关闭", color = p.sub) }
                }
            }
        }
    }

    if (showImport) {
        var text by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf<String?>(null) }
        Dialog(onDismissRequest = { showImport = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(p.bgBottom)
                    .border(1.dp, p.cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Text("导入数据", style = MaterialTheme.typography.titleLarge, color = p.text)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; msg = null },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("粘贴备份 JSON", color = p.faint, fontSize = 12.sp) },
                )
                msg?.let {
                    Text(it, color = if (it.startsWith("成功")) p.accent else p.danger, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showImport = false }) { Text("取消", color = p.sub) }
                    PillButton("导入") {
                        msg = if (repo.importJson(text)) "成功导入" else "格式不对，检查一下"
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = p.text)
        Text(label, color = p.faint, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
