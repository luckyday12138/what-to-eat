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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.chisha.data.ChishaRepo
import com.chisha.data.Dish
import com.chisha.data.ImageKey
import com.chisha.data.Slot
import com.chisha.ui.components.Chip
import com.chisha.ui.components.DishGlyph
import com.chisha.ui.components.PillButton
import com.chisha.ui.theme.LocalPalette

@Composable
fun LibraryScreen(onOpenRecipe: (Dish) -> Unit = {}) {
    val repo = ChishaRepo.instance
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<Slot?>(null) }
    var editing by remember { mutableStateOf<Dish?>(null) }
    var creating by remember { mutableStateOf(false) }

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
            onDismiss = { creating = false },
            onSave = { payload ->
                repo.newDish(
                    name = payload.name,
                    slots = payload.slots,
                    cuisine = payload.cuisine,
                    tags = payload.tags,
                    imageKey = payload.imageKey,
                    imageUri = null,
                    priceLevel = payload.priceLevel,
                    spicy = payload.spicy,
                    weight = payload.weight,
                    note = payload.note,
                )
                creating = false
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
                        priceLevel = payload.priceLevel,
                        spicy = payload.spicy,
                        weight = payload.weight,
                        note = payload.note,
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
    val priceLevel: Int,
    val spicy: Int,
    val weight: Float,
    val note: String,
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DishEditor(
    initial: Dish?,
    onDismiss: () -> Unit,
    onSave: (DishPayload) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var slots by remember { mutableStateOf(initial?.slots ?: setOf(Slot.LUNCH, Slot.DINNER)) }
    var cuisine by remember { mutableStateOf(initial?.cuisine ?: "") }
    var tagsText by remember { mutableStateOf(initial?.tags?.joinToString(" ") ?: "") }
    var imageKey by remember { mutableStateOf(initial?.imageKey) }
    var price by remember { mutableStateOf(initial?.priceLevel ?: 2) }
    var spicy by remember { mutableStateOf(initial?.spicy ?: 0) }
    var weight by remember { mutableStateOf(initial?.weight ?: 1f) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

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
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text("菜名") },
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = cuisine,
                onValueChange = { cuisine = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text("菜系 / 分类") },
                placeholder = { Text("家常、川菜、日式…") },
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text("标签（空格分隔）") },
                placeholder = { Text("辣 下饭 牛肉") },
            )

            Spacer(Modifier.height(16.dp))
            Text("适用时段", color = p.sub, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Slot.entries.forEach { s ->
                    Chip(
                        s.label,
                        selected = s in slots,
                        onClick = {
                            slots = if (s in slots) slots - s else slots + s
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("图片类别（无实拍图时按类别生成）", color = p.sub, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ImageKey.labels.forEach { (key, label) ->
                    Chip(label, selected = imageKey == key, onClick = { imageKey = if (imageKey == key) null else key })
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("价格", color = p.sub, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { lv ->
                        Chip("¥".repeat(lv), selected = price == lv, onClick = { price = lv })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("辣度", color = p.sub, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..3).forEach { lv ->
                        Chip(if (lv == 0) "不辣" else "辣".repeat(lv), selected = spicy == lv, onClick = { spicy = lv })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("权重", color = p.sub, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1f, 2f, 3f).forEach { w ->
                        Chip("×${w.toInt()}", selected = weight == w, onClick = { weight = w })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                label = { Text("备注（可不填）") },
                maxLines = 3,
            )

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
                    "保存",
                    onClick = {
                        if (name.isBlank()) {
                            error = "先给这道菜起个名字"
                        } else if (slots.isEmpty()) {
                            error = "至少选一个时段"
                        } else {
                            onSave(
                                DishPayload(
                                    name = name.trim(),
                                    slots = slots,
                                    cuisine = cuisine.trim().ifBlank { "自定义" },
                                    tags = tagsText.split(' ', '，', ',').filter { it.isNotBlank() }.distinct(),
                                    imageKey = imageKey,
                                    priceLevel = price,
                                    spicy = spicy,
                                    weight = weight,
                                    note = note.trim(),
                                )
                            )
                        }
                    },
                )
            }
        }
    }
}
