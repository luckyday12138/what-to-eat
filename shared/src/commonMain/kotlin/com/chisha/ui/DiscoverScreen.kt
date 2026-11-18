package com.chisha.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.data.ChishaRepo
import com.chisha.data.Dish
import com.chisha.data.DishPhoto
import com.chisha.data.ImageKey
import com.chisha.data.MealZh
import com.chisha.data.NetMeal
import com.chisha.data.OnlineSource
import com.chisha.data.Origin
import com.chisha.data.PriceTag
import com.chisha.data.Slot
import com.chisha.ui.components.Chip
import com.chisha.ui.components.DishArt
import com.chisha.ui.components.DishGlyph
import com.chisha.ui.components.PillButton
import com.chisha.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DiscoverScreen(initialSection: Int = 0, onOpenRecipe: (Dish) -> Unit = {}) {
    val repo = ChishaRepo.instance
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(initialSection) }
    var meal by remember { mutableStateOf<NetMeal?>(null) }
    var loading by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var fallback by remember { mutableStateOf<Dish?>(null) }
    var quick by remember { mutableStateOf<List<NetMeal>>(emptyList()) }
    var srcLine by remember { mutableStateOf("") }
    // 灵感池小卡的实拍图：小卡只读缓存不会主动抓图，这里补抓一次再回填
    var quickPhotos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // 网络源全挂时，灵感池用本地热门菜顶上，界面不塌成一片空白
    var localFill by remember { mutableStateOf(false) }
    val localTag = "本地菜库"

    // 灵感池小卡里还没图的菜，后台补抓一次真图（并发 3，通常 1~2 秒）
    LaunchedEffect(quick.map { it.name }) {
        val need = quick.filter { it.photo.isBlank() }
            .map { it.name }
            .filter { quickPhotos[it] == null }
        if (need.isEmpty()) return@LaunchedEffect
        val got = DishPhoto.resolveAll(need)
        if (got.isNotEmpty()) quickPhotos = quickPhotos + got
    }

    fun fillLocalQuick() {
        localFill = true
        quick = repo.dishes.filter { it.enabled }
            .sortedByDescending { it.heat }
            .take(4)
            .map {
                NetMeal(
                    name = it.name,
                    category = it.cuisine,
                    steps = it.steps,
                    ingredients = it.ingredients,
                    feature = "本地热门",
                    source = localTag,
                )
            }
    }

    val hot = repo.dishes.sortedByDescending { it.heat }.take(30)

    fun refreshSrcLine() {
        // 从池里取菜时没有实时请求，用这道菜自己记的来源兜底
        val label = OnlineSource.lastSourceLabel.ifBlank { meal?.source.orEmpty() }
        val ok = OnlineSource.availableCount()
        val total = OnlineSource.providerCount()
        srcLine = if (label.isBlank()) {
            OnlineSource.sourceStatus()
        } else {
            "$label 在用 · " + if (ok >= total) "$total 个源都能连" else "$ok/$total 个源能连"
        }
    }

    fun loadOnline() {
        if (loading) return
        loading = true
        note = null
        scope.launch {
            val result = OnlineSource.take()
            loading = false
            // 排除刚拿到的这道，主卡和小卡不该出现同一道菜
            quick = OnlineSource.peek(8, setOfNotNull(result?.name ?: meal?.name))
            refreshSrcLine()
            if (result != null) {
                meal = result
                fallback = null
                localFill = false
                if (OnlineSource.lastSwitched) {
                    note = "主源不稳，已自动切到「${OnlineSource.lastSourceLabel}」"
                }
            } else {
                meal = null
                fallback = repo.dishes.filter { it.enabled }.randomOrNullCompat()
                fillLocalQuick()
                note = "几个数据源都连不上，先给你端一份本地热门灵感"
            }
        }
    }

    // 换一道：当前这道退回池尾，池头那道升为主卡，灵感池跟着往前挪一位
    fun nextOne() {
        if (loading) return
        scope.launch {
            val cur = meal
            val next = OnlineSource.shift()
            if (next != null) {
                cur?.let { OnlineSource.putBack(it) }
                meal = next
                fallback = null
                note = null
                quick = OnlineSource.peek(8, setOfNotNull(meal?.name))
                refreshSrcLine()
            } else {
                loadOnline()
            }
        }
    }

    // 进页面先预热池子，然后再拿一道；换一道时就不用等
    LaunchedEffect(tab) {
        if (tab == 1) {
            // 本地菜库已有的菜，网络池不再推，两边才不重复
            OnlineSource.excludeLocal(repo.dishes.map { it.name }.toSet())
            OnlineSource.warm()
            val pool = OnlineSource.peek(8, setOfNotNull(meal?.name))
            if (pool.isNotEmpty()) {
                localFill = false
                quick = pool
            } else if (quick.isEmpty()) {
                fillLocalQuick()
            }
            refreshSrcLine()
            if (meal == null && fallback == null) loadOnline()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(30.dp))
            Text("发现", style = MaterialTheme.typography.headlineLarge, color = p.text)
            Text(
                "热门榜单 + 在线灵感，看中就收进菜库",
                color = p.faint,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(99.dp))
                    .background(p.card)
                    .padding(5.dp),
            ) {
                listOf("热门榜", "在线灵感").forEachIndexed { i, label ->
                    val sel = tab == i
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(99.dp))
                            .then(
                                if (sel) Modifier.background(Brush.horizontalGradient(listOf(p.accent, p.accent2)))
                                else Modifier
                            )
                            .clickable { tab = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (sel) Color(0xFF221708) else p.sub,
                            fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        if (tab == 0) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 22.dp, end = 22.dp, top = 14.dp, bottom = 156.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item {
                    // 这个榜单是本地的（按收进菜库的菜热度排），不写成「全网」免得误导
                    Text("菜库热度 Top ${hot.size} · 收录后自动进榜", color = p.faint, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                items(hot, key = { it.id }) { dish ->
                    HotRow(
                        dish = dish,
                        collected = repo.dishes.any { it.name == dish.name && it.origin != Origin.SEED },
                        onOpen = { onOpenRecipe(dish) },
                        onAdd = {
                            repo.addNetworkDish(
                                name = dish.name,
                                cuisine = dish.cuisine,
                                tags = dish.tags,
                                imageUri = dish.imageUri,
                                heat = dish.heat,
                                slots = dish.slots,
                            )
                        },
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                Spacer(Modifier.height(14.dp))

                if (quick.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (localFill) "本地热门 · ${quick.size} 道，点开看做法"
                            else "灵感池 · ${quick.size} 道，点一下直接换",
                            color = p.faint,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        if (srcLine.isNotBlank()) {
                            Text(
                                srcLine,
                                color = p.text.copy(alpha = 0.72f),
                                fontSize = 10.sp,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(quick, key = { it.name }) { q ->
                            val qd = Dish(
                                id = "q_" + q.name.hashCode().toString(),
                                name = q.name,
                                cuisine = q.category.ifBlank { "家常" },
                                imageKey = ImageKey.derive(q.name, q.category, emptyList()),
                                imageUri = q.photo.ifBlank {
                                    quickPhotos[q.name] ?: DishPhoto.cached(q.name)
                                },
                                origin = Origin.NETWORK,
                                steps = q.steps,
                                ingredients = q.ingredients,
                            )
                            Column(
                                Modifier
                                    .width(104.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(p.card)
                                    .clickable {
                                        // 兜底的本地菜：直接打开它的菜谱，不当网络菜处理
                                        if (q.source == localTag) {
                                            repo.dishes.firstOrNull { it.name == q.name }?.let { onOpenRecipe(it) }
                                        } else {
                                            scope.launch {
                                                val picked = OnlineSource.takeSpecific(q.name)
                                                if (picked != null) {
                                                    meal?.let { OnlineSource.putBack(it) }
                                                    meal = picked
                                                    fallback = null
                                                    note = null
                                                    quick = OnlineSource.peek(8, setOfNotNull(meal?.name))
                                                }
                                            }
                                        }
                                    },
                            ) {
                                DishArt(
                                    qd,
                                    Modifier.fillMaxWidth().height(56.dp),
                                    corner = 16.dp,
                                    showName = false,
                                    scrim = false,
                                )
                                Text(
                                    q.name,
                                    color = p.text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (meal == null && fallback == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(p.card)
                            .padding(20.dp),
                    ) {
                        Column {
                            Text("在线灵感", color = p.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "从公开菜谱库抓一条随机菜谱，喂给菜库再加一点新鲜感。",
                                color = p.faint,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                            Spacer(Modifier.height(14.dp))
                            PillButton(if (loading) "抓取中…" else "抓一道菜") { loadOnline() }
                        }
                    }
                } else {
                    note?.let {
                        Text(it, color = p.accent, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    val online = meal
                    if (online != null) {
                        val cuisine = online.category.ifBlank { "家常" }
                        val previewDish = Dish(
                            id = "net_" + online.name.hashCode().toString(),
                            name = online.name,
                            cuisine = cuisine,
                            tags = listOfNotNull(online.feature.takeIf { it.isNotBlank() }),
                            imageKey = ImageKey.derive(online.name, cuisine, emptyList()),
                            imageUri = online.photo.ifBlank { DishPhoto.cached(online.name) },
                            priceLevel = PriceTag.level(online.name, online.ingredients),
                            origin = Origin.NETWORK,
                            heat = 70,
                            steps = online.steps,
                            ingredients = online.ingredients,
                        )
                        OnlineCard(
                            dish = previewDish,
                            title = online.name,
                            subtitle = listOfNotNull(
                                cuisine.takeIf { it.isNotBlank() },
                                "网络菜谱",
                                online.source.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                            ingredients = online.ingredients,
                            steps = online.steps,
                            tip = online.tip,
                            onRecipe = { onOpenRecipe(previewDish) },
                            onAdd = {
                                repo.addNetworkDish(
                                    name = online.name,
                                    cuisine = cuisine,
                                    tags = listOfNotNull(online.feature.takeIf { it.isNotBlank() }),
                                    imageUri = previewDish.imageUri,
                                    heat = 70,
                                    slots = setOf(Slot.LUNCH, Slot.DINNER),
                                )
                                note = "已收进菜库"
                            },
                            onRefresh = { nextOne() },
                        )
                    }
                    fallback?.let { d ->
                        // 本地兜底的菜本来就在库里，不显示「收进菜库」
                        OnlineCard(
                            dish = d,
                            title = d.name,
                            subtitle = "${d.cuisine} · 本地热门",
                            ingredients = emptyList(),
                            steps = d.recipe.steps,
                            tip = "网络暂时不通，这道是你菜库里的高热度选择。",
                            onRecipe = { onOpenRecipe(d) },
                            onAdd = {},
                            onRefresh = { nextOne() },
                            showAdd = false,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Spacer(Modifier.height(150.dp))
            }
        }
    }
}

private fun <T> List<T>.randomOrNullCompat(): T? =
    if (isEmpty()) null else this[Random.nextInt(size)]

@Composable
private fun HotRow(dish: Dish, collected: Boolean, onOpen: () -> Unit, onAdd: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.card)
            .clickable { onOpen() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DishGlyph(dish, Modifier.size(48.dp), corner = 13.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(dish.name, color = p.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dish.cuisine, color = p.faint, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(56.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(p.cardBorder)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(dish.heat / 100f)
                            .height(4.dp)
                            .background(Brush.horizontalGradient(listOf(p.accent, p.accent2)))
                    )
                }
                Text(" ${dish.heat}", color = p.faint, fontSize = 10.sp)
            }
        }
        if (collected) {
            Chip("已收录")
        } else {
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(p.accent.copy(alpha = 0.18f))
                    .clickable { onAdd() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text("收录", color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OnlineCard(
    dish: Dish,
    title: String,
    subtitle: String,
    ingredients: List<String>,
    steps: List<String>,
    tip: String,
    onRecipe: () -> Unit = {},
    onAdd: () -> Unit,
    onRefresh: () -> Unit,
    showAdd: Boolean = true,
) {
    val p = LocalPalette.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(p.cardStrong)
            .padding(12.dp),
    ) {
        DishArt(
            dish,
            Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clickable { onRecipe() },
            corner = 20.dp,
            showName = false,
            scrim = false,
        )
        Column(Modifier.padding(horizontal = 8.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = p.text,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(p.accent.copy(alpha = 0.14f))
                        .clickable { onRecipe() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("食谱", color = p.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dish.priceLabel, color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(5.dp))
                Text("·", color = p.faint, fontSize = 11.sp)
                Spacer(Modifier.width(5.dp))
                Text(dish.priceHint, color = p.faint, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    subtitle,
                    color = p.faint,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }

            // 食材速览：一眼能判断想不想吃
            if (ingredients.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "食材 · " + ingredients.take(6).joinToString("、") +
                        if (ingredients.size > 6) " 等 ${ingredients.size} 样" else "",
                    color = p.sub,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }

            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(p.card)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (expanded) "收起做法" else "看做法（${steps.size} 步）",
                        color = p.accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(if (expanded) "  ︿" else "  ﹀", color = p.accent, fontSize = 11.sp)
                }

                AnimatedVisibility(visible = expanded) {
                    Column(Modifier.padding(top = 6.dp)) {
                        steps.forEachIndexed { i, step ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    Modifier
                                        .size(19.dp)
                                        .clip(CircleShape)
                                        .background(p.accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${i + 1}",
                                        color = p.accent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    step,
                                    color = p.sub,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        if (tip.isNotBlank()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(p.accent.copy(alpha = 0.10f))
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text("小贴士", color = p.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(tip, color = p.sub, fontSize = 12.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (showAdd) {
                    PillButton("收进菜库", Modifier.weight(1f)) { onAdd() }
                    PillButton("换一道", Modifier.weight(1f), primary = false) { onRefresh() }
                } else {
                    PillButton("看做法", Modifier.weight(1f)) { onRecipe() }
                    PillButton("换一道", Modifier.weight(1f), primary = false) { onRefresh() }
                }
            }
        }
    }
}

