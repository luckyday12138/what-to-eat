package com.chisha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chisha.core.rememberImagePicker
import com.chisha.data.ChishaRepo
import com.chisha.data.Dish
import com.chisha.data.DishAuto
import com.chisha.data.DishPhoto
import com.chisha.data.ImageKey
import com.chisha.data.Slot
import com.chisha.ui.components.Chip
import com.chisha.ui.components.DishGlyph
import com.chisha.ui.components.PillButton
import com.chisha.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onOpenRecipe: (Dish) -> Unit = {},
    startCreating: Boolean = false,
    startName: String = "",
) {
    val repo = ChishaRepo.instance
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<Slot?>(null) }
    var editing by remember { mutableStateOf<Dish?>(null) }
    var creating by remember { mutableStateOf(startCreating) }
    val scope = rememberCoroutineScope()

    val all = repo.dishes.toList()
    val list = all.filter { d ->
        (filter == null || filter in d.slots) &&
            (query.isBlank() || d.name.contains(query, true) || d.cuisine.contains(query, true) || d.tags.any { it.contains(query, true) })
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(30.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("菜库", style = MaterialTheme.typography.headlineLarge, color = p.text)
                    Text(
                        "${all.size} 道菜 · 已启用 ${all.count { it.enabled }} 道",
                        color = p.faint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(p.accent, p.accent2)))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            creating = true
                        }
                        .size(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color(0xFF231708), fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("搜索菜名 / 菜系 / 标签", color = p.faint, fontSize = 13.sp) },
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("全部", selected = filter == null, onClick = { filter = null })
                Slot.entries.forEach { s ->
                    Chip(s.label, selected = filter == s, onClick = { filter = if (filter == s) null else s })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 22.dp, end = 22.dp, top = 8.dp, bottom = 156.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(list, key = { it.id }) { dish ->
                DishRow(
                    dish,
                    onClick = { onOpenRecipe(dish) },
                    onEdit = { editing = dish },
                    onToggle = { repo.setEnabled(dish, it) },
                )
            }
            if (list.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("没有匹配的菜，换个词试试", color = p.faint)
                    }
                }
            }
        }
    }

    if (creating) {
        DishEditor(
            initial = null,
            initialName = startName,
            onDismiss = { creating = false },
            onSave = { payload ->
                repo.newDish(
                    name = payload.name,
                    slots = payload.slots,
                    cuisine = payload.cuisine,
                    tags = payload.tags,
                    imageKey = payload.imageKey,
                    imageUri = payload.imageUri,
                    priceLevel = payload.priceLevel,
                    spicy = payload.spicy,
                    weight = payload.weight,
                    note = payload.note,
                    ingredients = payload.ingredients,
                    steps = payload.steps,
                )
                creating = false
                // 面板里没等到图就点了保存：这里再补一次，补到就写回菜库
                if (payload.imageUri.isNullOrBlank()) {
                    scope.launch {
                        val url = DishPhoto.cached(payload.name) ?: DishPhoto.resolve(payload.name)
                        if (url != null) repo.attachPhoto(payload.name, url)
                    }
                }
            },
        )
    }

    editing?.let { dish ->
        DishEditor(
            initial = dish,
            onDismiss = { editing = null },
            onSave = { payload ->
                repo.upsert(
                    dish.copy(
                        name = payload.name,
                        slots = payload.slots,
                        cuisine = payload.cuisine,
                        tags = payload.tags,
                        imageKey = payload.imageKey,
                        imageUri = payload.imageUri,
                        priceLevel = payload.priceLevel,
                        spicy = payload.spicy,
                        weight = payload.weight,
                        note = payload.note,
                        ingredients = payload.ingredients,
                        steps = payload.steps,
                    )
                )
                editing = null
            },
            onDelete = {
                repo.delete(dish)
                editing = null
            },
        )
    }
}

@Composable
private fun DishRow(
    dish: Dish,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.card)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DishGlyph(dish, Modifier.size(46.dp), corner = 13.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dish.name, color = p.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (dish.origin == com.chisha.data.Origin.USER) {
                    Text(" 自定义", color = p.accent, fontSize = 10.sp)
                }
                if (dish.origin == com.chisha.data.Origin.NETWORK) {
                    Text(" 网络", color = p.faint, fontSize = 10.sp)
                }
            }
            Text(
                buildString {
                    append(dish.cuisine)
                    append(" · ")
                    append(dish.slots.sortedBy { it.order }.joinToString("/") { it.label })
                    append(" · ${dish.recipe.steps.size} 步做法")
                },
                color = p.faint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(p.accent.copy(alpha = 0.14f))
                .clickable { onEdit() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("编辑", color = p.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = dish.enabled, onCheckedChange = onToggle)
    }
}

private data class DishPayload(
    val name: String,
    val slots: Set<Slot>,
    val cuisine: String,
    val tags: List<String>,
    val imageKey: String?,
    val imageUri: String?,
    val priceLevel: Int,
    val spicy: Int,
    val weight: Float,
    val note: String,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
)

/**
 * 添加 / 编辑面板。
 *
 * 只有一个必填项：菜名。菜系、时段、标签、辣度、价格、配图类别、做法步骤全按菜名推出来，
 * 连实拍图都会在你停下打字后自动去找。每个字段都可以展开手动改，
 * 改过的字段就固定成你填的值，没改的继续跟自动值走。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DishEditor(
    initial: Dish?,
    onDismiss: () -> Unit,
    onSave: (DishPayload) -> Unit,
    onDelete: (() -> Unit)? = null,
    initialName: String = "",
) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.name ?: initialName) }
    // 每敲一个字重算一次，纯本地计算，不涉及网络
    val draft = remember(name) { DishAuto.draft(name) }

    // null 表示「没手动改过」，保存时用 draft 的值
    var slots by remember { mutableStateOf(initial?.slots) }
    var cuisine by remember { mutableStateOf(initial?.cuisine) }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString(" ")) }
    var price by remember { mutableStateOf(initial?.priceLevel) }
    var spicy by remember { mutableStateOf(initial?.spicy) }
    var weight by remember { mutableStateOf(initial?.weight) }
    var imageKey by remember { mutableStateOf(initial?.imageKey) }
    var note by remember { mutableStateOf(initial?.note) }
    var imageUri by remember { mutableStateOf(initial?.imageUri) }

    var expanded by remember { mutableStateOf(initial != null) }
    var error by remember { mutableStateOf<String?>(null) }
    var findingPhoto by remember { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }

    val pickImage = rememberImagePicker { picked ->
        if (picked != null) {
            imageUri = picked
            photoFailed = false
            error = null
        }
    }

    fun findPhoto() {
        val n = name.trim()
        if (n.length < 2 || findingPhoto) return
        findingPhoto = true
        photoFailed = false
        scope.launch {
            val url = DishPhoto.cached(n) ?: runCatching { DishPhoto.resolve(n) }.getOrNull()
            if (url != null) imageUri = url else photoFailed = true
            findingPhoto = false
        }
    }

    // 名字打完停 0.7 秒就去网上找图，预览里马上能看到是这道菜
    LaunchedEffect(name) {
        if (initial != null) return@LaunchedEffect
        val n = name.trim()
        if (n.length < 2 || imageUri != null) return@LaunchedEffect
        delay(700)
        if (name.trim() == n) findPhoto()
    }

    val effSlots = slots ?: draft.slots
    val effCuisine = cuisine ?: draft.cuisine
    val effTags = tagsText?.split(' ', '，', ',')?.filter { it.isNotBlank() }?.distinct()
        ?: draft.tags
    val effPrice = price ?: draft.priceLevel
    val effSpicy = spicy ?: draft.spicy
    val effWeight = weight ?: 1f
    val effKey = imageKey ?: draft.imageKey
    val effNote = note ?: ""

    // 保存时沿用的食材与做法：老菜品自带的不覆盖，新菜用自动推出来的
    val effIngredients = initial?.ingredients?.takeIf { it.isNotEmpty() } ?: draft.ingredients
    val effSteps = initial?.steps?.takeIf { it.isNotEmpty() && name.trim() == initial.name } ?: draft.steps

    val preview = Dish(
        id = "preview",
        name = name.trim().ifBlank { "这道菜" },
        slots = effSlots,
        cuisine = effCuisine,
        tags = effTags,
        imageKey = effKey,
        imageUri = imageUri,
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(p.bgBottom)
                .border(1.dp, p.cardBorder, RoundedCornerShape(26.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                if (initial == null) "添加菜品" else "编辑菜品",
                style = MaterialTheme.typography.headlineMedium,
                color = p.text,
            )
            Text(
                "只要写菜名，其他我来补",
                color = p.faint,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text("菜名") },
                placeholder = { Text("红烧肉、番茄鸡蛋面…") },
            )

            // 图片：自动搜、手动选、清掉
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 外框让「还没配图」的空位也看得见边界，不至于像一块空白
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, p.cardBorder, RoundedCornerShape(14.dp)),
                ) {
                    DishGlyph(preview, Modifier.fillMaxSize(), corner = 14.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            findingPhoto -> "正在按菜名找图…"
                            imageUri != null -> "已配好这道菜的图"
                            photoFailed -> "这个名字没搜到合适的图"
                            name.trim().length < 2 -> "写完菜名自动配图"
                            else -> "等图来，或自己选一张"
                        },
                        color = if (photoFailed) p.faint else p.sub,
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val linkPad = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        TextButton(
                            onClick = { findPhoto() },
                            enabled = name.trim().length >= 2,
                            contentPadding = linkPad,
                        ) {
                            Text("自动找图", color = p.accent, fontSize = 12.sp, maxLines = 1)
                        }
                        TextButton(onClick = { pickImage() }, contentPadding = linkPad) {
                            Text("自己选一张", color = p.accent, fontSize = 12.sp, maxLines = 1)
                        }
                        if (imageUri != null) {
                            TextButton(
                                onClick = { imageUri = null; photoFailed = false },
                                contentPadding = linkPad,
                            ) {
                                Text("清除", color = p.faint, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 自动推断结果一句话说清，默认不用管；名字还没写就先把能补的预告一下
            Spacer(Modifier.height(6.dp))
            if (name.trim().length >= 2) {
                Text(
                    "已按菜名填好：" + DishAuto.summary(draft),
                    color = p.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (draft.ingredients.isNotEmpty()) {
                    Text(
                        "食材：" + draft.ingredients.joinToString("、"),
                        color = p.faint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            } else {
                Text(
                    "菜系 / 时段 / 辣度 / 价格 / 食材 / 配图，我都按菜名自动补",
                    color = p.faint,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { expanded = !expanded }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(
                    if (expanded) "收起调整" else "展开调整（菜系 / 时段 / 辣度 / 价格…）",
                    color = p.sub,
                    fontSize = 12.sp,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = effCuisine,
                    onValueChange = { cuisine = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("菜系 / 分类") },
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = effTags.joinToString(" "),
                    onValueChange = { tagsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("标签（空格分隔）") },
                )

                Spacer(Modifier.height(14.dp))
                Text("适用时段", color = p.sub, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slot.entries.forEach { s ->
                        Chip(
                            s.label,
                            selected = s in effSlots,
                            onClick = {
                                slots = if (s in effSlots) effSlots - s else effSlots + s
                            },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("图片类别（没实拍图时按类别画）", color = p.sub, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ImageKey.labels.forEach { (key, label) ->
                        Chip(
                            label,
                            selected = effKey == key,
                            onClick = { imageKey = if (effKey == key) null else key },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("价格", color = p.sub, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { lv ->
                            Chip("¥".repeat(lv), selected = effPrice == lv, onClick = { price = lv })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("辣度", color = p.sub, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..3).forEach { lv ->
                            Chip(
                                if (lv == 0) "不辣" else "辣".repeat(lv),
                                selected = effSpicy == lv,
                                onClick = { spicy = lv },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("权重", color = p.sub, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1f, 2f, 3f).forEach { w ->
                            Chip("×${w.toInt()}", selected = effWeight == w, onClick = { weight = w })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = effNote,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("备注（可不填）") },
                    maxLines = 3,
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = p.danger, fontSize = 12.sp)
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = p.danger)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("取消", color = p.sub) }
                PillButton(
                    if (initial == null) "加入菜库" else "保存",
                    onClick = {
                        if (name.isBlank()) {
                            error = "先给这道菜起个名字"
                        } else if (effSlots.isEmpty()) {
                            error = "至少选一个时段"
                        } else {
                            onSave(
                                DishPayload(
                                    name = name.trim(),
                                    slots = effSlots,
                                    cuisine = effCuisine.ifBlank { "家常" },
                                    tags = effTags,
                                    imageKey = effKey,
                                    imageUri = imageUri,
                                    priceLevel = effPrice,
                                    spicy = effSpicy,
                                    weight = effWeight,
                                    note = effNote.trim(),
                                    ingredients = effIngredients,
                                    steps = effSteps,
                                )
                            )
                        }
                    },
                )
            }
        }
    }
}

