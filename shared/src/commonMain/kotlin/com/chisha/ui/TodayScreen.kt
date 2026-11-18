package com.chisha.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.core.PlatformStore
import com.chisha.core.greeting
import com.chisha.core.slotOfHour
import com.chisha.data.ChishaRepo
import com.chisha.data.Dish
import com.chisha.data.Origin
import com.chisha.data.OnlineSource
import com.chisha.data.PickMode
import com.chisha.data.Slot
import com.chisha.data.toDish
import com.chisha.domain.Picker
import com.chisha.ui.components.Chip
import com.chisha.ui.components.DishArt
import com.chisha.ui.components.FoodWheel
import com.chisha.ui.components.ModeSwitch
import com.chisha.ui.components.PillButton
import com.chisha.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun TodayScreen(
    onGoDiscover: () -> Unit = {},
    onGoLibrary: () -> Unit = {},
    onOpenRecipe: (Dish) -> Unit = {},
    initialResult: Dish? = null,
    /** 当前餐段由 App 持有：切换时背景光晕要跟着变，否则点了「夜宵」背景还是晚餐色 */
    slot: Slot,
    onSlotChange: (Slot) -> Unit = {},
) {
    val repo = ChishaRepo.instance
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var smart by remember { mutableStateOf(repo.prefs.mode != "PURE") }
    var wheel by remember { mutableStateOf<List<Dish>>(emptyList()) }
    // 调试时可以直接带着「已抽中的结果」进页面，省得每次都要手动转一下
    var result by remember { mutableStateOf<Dish?>(initialResult) }
    var spinning by remember { mutableStateOf(false) }
    val angle = remember { Animatable(0f) }

    val decided = remember(slot, repo.records.size) { repo.decidedToday(slot) }

    // 随机模式：只在本地菜库里抽（不含从网络收进来的）
    fun localWheel(): List<Dish> {
        val local = repo.dishes.filter { it.origin != Origin.NETWORK }
        var pool = Picker.candidates(local, repo.prefs, slot)
        // 这个餐段一道菜都没有时放宽餐段限制，别给用户一个点不动的空转盘
        if (pool.isEmpty()) pool = Picker.candidates(local, repo.prefs, slot, strictSlot = false)
        val w = Picker.weights(pool, repo.prefs, slot, repo.recentIds(repo.prefs.avoidRepeatWindow), false)
        // 先抽一大把，再按主料去重取 6 道：否则容易出现「红糖羊肉 + 涮羊肉 + 番茄牛腩」整盘牛羊
        return Picker.variety(Picker.sample(pool, w, 18), 6)
    }

    // 只跟餐段和模式走。不跟 dishes.size：收一道菜回首页，轮盘不该整盘换掉（还会白耗池子里的菜）
    LaunchedEffect(slot, smart) {
        spinning = false
        angle.snapTo(0f)
        wheel = if (smart) {
            // 推荐模式：从网络取，和「在线灵感」共用一个池，同一道不会两边都出现
            val nets = OnlineSource.batch(8, slot)
            if (nets.isNotEmpty()) Picker.variety(nets.map { it.toDish(slot) }, 6) else localWheel()
        } else {
            localWheel()
        }
    }

    fun spin(excludeName: String? = null) {
        if (spinning) return
        val current = wheel
        if (current.isEmpty()) return
        spinning = true
        result = null
        val n = current.size
        var idx = Random.nextInt(n)
        // 结果卡上再点一次，有 1/n 的概率又转回同一道，看着像没反应，往后挪一格避开
        if (n > 1 && excludeName != null && current[idx].name == excludeName) idx = (idx + 1) % n
        val sweep = 360f / n
        val targetMod = ((-(idx * sweep + sweep / 2f)) % 360f + 360f) % 360f
        val cur = angle.value
        val norm = ((cur % 360f) + 360f) % 360f
        val delta = ((targetMod - norm) + 360f) % 360f
        scope.launch {
            angle.animateTo(
                targetValue = cur + 360f * 4 + delta,
                animationSpec = tween(2300, easing = CubicBezierEasing(0.15f, 0.9f, 0.1f, 1f)),
            )
            val picked = current[idx]
            result = picked
            spinning = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repo.record(picked, slot, false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    "${greeting(PlatformStore.hourOfDay())} · ${PlatformStore.todayKey()}",
                    color = p.faint,
                    fontSize = 12.sp,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "今天吃点什么",
                    style = MaterialTheme.typography.headlineLarge,
                    color = p.text,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SlotSwitcher(selected = slot, onSelect = { onSlotChange(it); result = null })
        Spacer(Modifier.height(12.dp))

        Crossfade(
            targetState = result,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .aspectRatio(1f),
            animationSpec = tween(380),
        ) { picked ->
            if (picked == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    FoodWheel(
                        items = wheel,
                        rotation = angle.value,
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        dimmed = spinning,
                    )

                    CanvasPointer(Modifier.align(Alignment.TopCenter))

                    // 菜库空 / 全被禁用：转盘点不动，得告诉用户怎么办
                    if (wheel.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("菜库还没有可选的菜", color = p.sub, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "去菜库添加或开启几道",
                                color = p.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onGoLibrary() },
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .testTag("spin")
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(p.accent, p.accent2)))
                                .clickable(enabled = !spinning) { spin() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (spinning) "···" else "转",
                                color = Color(0xFF23170A),
                                fontSize = if (spinning) 22.sp else 26.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            } else {
                ResultHero(
                    dish = picked,
                    onRecipe = { onOpenRecipe(picked) },
                    onAgain = {
                        result = null
                        spin(excludeName = picked.name)
                    },
                    onAccept = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        repo.record(picked, slot, true)
                        // 刚定的这道从转盘里拿掉，否则下次再转会又转到它、又记一次
                        wheel = wheel.filter { it.id != picked.id }
                        result = null
                        if (wheel.size < 3) {
                            scope.launch {
                                wheel = if (smart) {
                                    val nets = OnlineSource.batch(8, slot)
                                    if (nets.isNotEmpty()) Picker.variety(nets.map { it.toDish(slot) }, 6) else localWheel()
                                } else localWheel()
                            }
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (result == null) {
            decided?.let { rec ->
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.card)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(p.accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("今日${slot.label}已定", color = p.faint, fontSize = 11.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        rec.dishName,
                        color = p.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "改主意",
                        color = p.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("revoke")
                            .clickable {
                                // 先撤掉「已定」，再重新转，否则界面上会同时出现已定的 A 和新抽的 B
                                repo.revokeDecided(slot)
                                spin()
                            },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }

        val shown = if (result == null) wheel else wheel.filter { it.id != result?.id }
        if (shown.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (result == null) "今日灵感" else "其他候选",
                    color = p.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (result == null) {
                        ModeSwitch(
                            smart = smart,
                            onToggle = { on ->
                                smart = on
                                repo.updatePrefs { prefs -> prefs.copy(mode = if (on) "SMART" else "PURE") }
                            },
                        )
                    }
                    Text("${shown.size} 道", color = p.faint, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { d ->
                    Column(Modifier.width(104.dp)) {
                        DishArt(
                            d,
                            Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clickable { onOpenRecipe(d) },
                            corner = 16.dp,
                            showName = false,
                            scrim = false,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            d.name,
                            color = p.sub,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(124.dp))
    }
}

@Composable
private fun CanvasPointer(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    androidx.compose.foundation.Canvas(modifier.size(width = 18.dp, height = 26.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2f, h)
            lineTo(0f, 0f)
            lineTo(w, 0f)
            close()
        }
        drawPath(path, color = p.accent)
        drawPath(path, color = Color.Black.copy(alpha = 0.25f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    }
}

/** 抽中结果：满幅大图 + 底部叠加信息与操作 */
@Composable
private fun ResultHero(
    dish: Dish,
    onRecipe: () -> Unit,
    onAgain: () -> Unit,
    onAccept: () -> Unit,
) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(p.cardStrong),
    ) {
        DishArt(dish, Modifier.fillMaxSize(), corner = 28.dp, showName = false)

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dish.name,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                // 菜名右侧留一个食谱入口，点进去看这道菜的做法
                Row(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onRecipe() }
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("食谱", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("  →", color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("换一个", Modifier.weight(1f), primary = false, onClick = onAgain)
                PillButton("就它了", Modifier.weight(1f), primary = true, onClick = onAccept)
            }
        }
    }
}

@Composable
private fun SlotSwitcher(selected: Slot, onSelect: (Slot) -> Unit) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(99.dp))
            .background(p.card)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Slot.entries.forEach { s ->
            val isSel = s == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (isSel) Brush.horizontalGradient(listOf(p.accent, p.accent2)) else Brush.linearGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(s)
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.label,
                    color = if (isSel) Color(0xFF221708) else p.sub,
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
