package com.chisha.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chisha.core.PlatformStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** 全应用唯一数据仓库：菜库 + 历史 + 偏好，全部落盘 */
class ChishaRepo private constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    val dishes = mutableStateListOf<Dish>()
    val records = mutableStateListOf<MealRecord>()

    var prefs by mutableStateOf(Prefs())
        private set

    var loaded by mutableStateOf(false)
        private set

    fun load() {
        if (loaded) return
        loaded = true

        val dishJson = PlatformStore.read(KEY_DISHES)
        if (dishJson.isNullOrBlank()) {
            dishes.addAll(Seed.initial())
            persistDishes()
        } else {
            runCatching { json.decodeFromString<List<Dish>>(dishJson) }
                .getOrNull()
                ?.let { saved ->
                    val seeds = Seed.initial()
                    // 老版本缺的种子菜自动补齐，不覆盖用户数据
                    val existing = saved.map { it.id }.toSet()
                    dishes.addAll(saved)
                    dishes.addAll(seeds.filter { it.id !in existing })
                }
        }

        PlatformStore.read(KEY_RECORDS)?.let { raw ->
            runCatching { json.decodeFromString<List<MealRecord>>(raw) }
                .getOrNull()?.let { records.addAll(it.takeLast(MAX_RECORDS)) }
        }

        PlatformStore.read(KEY_PREFS)?.let { raw ->
            runCatching { json.decodeFromString<Prefs>(raw) }
                .getOrNull()?.let { prefs = it }
        }

        // 老版本从网络收进来的原料名（「爆炒蛋液」）就地清掉；只动网络来源，用户自己加的菜不碰
        val junk = dishes.filter { it.origin == Origin.NETWORK && !NetParse.usableName(it.name, it.cuisine) }
        if (junk.isNotEmpty()) {
            dishes.removeAll(junk)
            persistDishes()
        }
    }

    // ---------- 菜库 ----------
    fun upsert(dish: Dish) {
        val idx = dishes.indexOfFirst { it.id == dish.id }
        if (idx >= 0) dishes[idx] = dish else dishes.add(dish)
        persistDishes()
    }

    fun newDish(
        name: String,
        slots: Set<Slot>,
        cuisine: String,
        tags: List<String>,
        imageKey: String?,
        imageUri: String?,
        priceLevel: Int,
        spicy: Int,
        weight: Float,
        note: String,
        ingredients: List<String> = emptyList(),
        steps: List<String> = emptyList(),
    ): Dish {
        val id = "u_" + PlatformStore.nowMillis().toString(36) + Random.nextInt(1000, 9999)
        val dish = Dish(
            id = id,
            name = name.trim(),
            slots = slots,
            cuisine = cuisine.ifBlank { "自定义" },
            tags = tags,
            imageKey = imageKey ?: ImageKey.derive(name, cuisine, tags),
            imageUri = imageUri,
            priceLevel = priceLevel,
            spicy = spicy,
            weight = weight,
            origin = Origin.USER,
            note = note,
            ingredients = ingredients,
            steps = steps,
        )
        upsert(dish)
        return dish
    }

    fun addNetworkDish(
        name: String,
        cuisine: String,
        tags: List<String>,
        imageUri: String?,
        heat: Int,
        slots: Set<Slot>,
        imageKey: String? = null,
    ) {
        val exist = dishes.firstOrNull { it.name == name }
        if (exist != null) {
            upsert(exist.copy(enabled = true, origin = if (exist.origin == Origin.SEED) Origin.SEED else Origin.NETWORK))
            return
        }
        val id = "n_" + PlatformStore.nowMillis().toString(36) + Random.nextInt(1000, 9999)
        upsert(
            Dish(
                id = id,
                name = name,
                slots = slots,
                cuisine = cuisine,
                tags = tags,
                imageKey = imageKey ?: ImageKey.derive(name, cuisine, tags),
                imageUri = imageUri,
                origin = Origin.NETWORK,
                heat = heat,
            )
        )
    }

    fun delete(dish: Dish) {
        dishes.removeAll { it.id == dish.id }
        persistDishes()
    }

    /** 给同名菜补上实拍图（只补空白，不覆盖用户自己设的图） */
    fun attachPhoto(name: String, url: String) {
        var changed = false
        for (i in dishes.indices) {
            val d = dishes[i]
            if (d.name == name && d.imageUri.isNullOrBlank()) {
                dishes[i] = d.copy(imageUri = url)
                changed = true
            }
        }
        if (changed) persistDishes()
    }

    fun setEnabled(dish: Dish, enabled: Boolean) {
        upsert(dish.copy(enabled = enabled))
    }

    // ---------- 偏好 ----------
    fun updatePrefs(block: (Prefs) -> Prefs) {
        prefs = block(prefs)
        PlatformStore.write(KEY_PREFS, json.encodeToString(prefs))
    }

    // ---------- 历史 ----------
    fun record(dish: Dish, slot: Slot, accepted: Boolean) {
        records.add(
            MealRecord(
                dishId = dish.id,
                dishName = dish.name,
                slot = slot,
                at = PlatformStore.nowMillis(),
                dateKey = PlatformStore.todayKey(),
                accepted = accepted,
                cuisine = dish.cuisine,
            )
        )
        while (records.size > MAX_RECORDS) records.removeAt(0)
        PlatformStore.write(KEY_RECORDS, json.encodeToString(records.toList()))
    }

    fun recentIds(limit: Int = 12): List<String> =
        records.asReversed().take(limit).map { it.dishId }

    fun decidedToday(slot: Slot): MealRecord? =
        records.asReversed().firstOrNull {
            it.slot == slot && it.accepted && it.dateKey == PlatformStore.todayKey()
        }

    /** 改主意：把今天这个餐段"已定"的记录撤掉，界面才不会一边说已定 A、一边又抽 B */
    fun revokeDecided(slot: Slot) {
        val removed = records.count {
            it.slot == slot && it.accepted && it.dateKey == PlatformStore.todayKey()
        }
        if (removed == 0) return
        records.removeAll {
            it.slot == slot && it.accepted && it.dateKey == PlatformStore.todayKey()
        }
        PlatformStore.write(KEY_RECORDS, json.encodeToString(records.toList()))
    }

    fun swapDishFor(record: MealRecord): Dish? =
        dishes.firstOrNull { it.id == record.dishId }

    fun totalAccepted(): Int = records.count { it.accepted }

    fun activeDays(): Int = records.filter { it.accepted }.map { it.dateKey }.distinct().size

    fun topDishes(limit: Int = 5): List<Pair<String, Int>> =
        records.filter { it.accepted }
            .groupBy { it.dishName }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(limit)

    fun topCuisines(limit: Int = 5): List<Pair<String, Int>> =
        records.filter { it.accepted }.filter { it.cuisine.isNotBlank() }
            .groupBy { it.cuisine }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(limit)

    /** 最近 7 天固定补满（没记录的那天记 0），柱状图才不会只剩一根 */
    fun last7Days(): List<Pair<String, Int>> {
        val counts = records.filter { it.accepted }
            .groupBy { it.dateKey }
            .mapValues { it.value.size }
        val today = PlatformStore.todayKey()
        return (6 downTo 0).map { back ->
            val key = shiftDate(today, back)
            key to (counts[key] ?: 0)
        }
    }

    /** dateKey 往前推 n 天，手工处理跨月跨年（commonMain 里没有 java.time 可用） */
    private fun shiftDate(key: String, minus: Int): String {
        val parts = key.split("-")
        if (parts.size != 3) return key
        var y = parts[0].toIntOrNull() ?: return key
        var m = parts[1].toIntOrNull() ?: return key
        var d = parts[2].toIntOrNull() ?: return key
        repeat(minus) {
            d -= 1
            if (d < 1) {
                m -= 1
                if (m < 1) {
                    m = 12
                    y -= 1
                }
                d = daysInMonth(y, m)
            }
        }
        return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    private fun daysInMonth(y: Int, m: Int): Int = when (m) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 29 else 28
    }

    private fun persistDishes() {
        PlatformStore.write(KEY_DISHES, json.encodeToString(dishes.toList()))
    }

    fun exportJson(): String = buildString {
        append("{\"dishes\":")
        append(json.encodeToString(dishes.toList()))
        append(",\"records\":")
        append(json.encodeToString(records.toList()))
        append(",\"prefs\":")
        append(json.encodeToString(prefs))
        append("}")
    }

    fun importJson(raw: String): Boolean = runCatching {
        val root = json.parseToJsonElement(raw)
        val obj = root as kotlinx.serialization.json.JsonObject
        obj["dishes"]?.let {
            val list = json.decodeFromString<List<Dish>>(it.toString())
            dishes.clear()
            dishes.addAll(list)
        }
        obj["records"]?.let {
            val list = json.decodeFromString<List<MealRecord>>(it.toString())
            records.clear()
            records.addAll(list.takeLast(MAX_RECORDS))
        }
        obj["prefs"]?.let { prefs = json.decodeFromString<Prefs>(it.toString()) }
        persistDishes()
        PlatformStore.write(KEY_RECORDS, json.encodeToString(records.toList()))
        PlatformStore.write(KEY_PREFS, json.encodeToString(prefs))
        true
    }.getOrDefault(false)

    companion object {
        private const val KEY_DISHES = "dishes.json"
        private const val KEY_RECORDS = "records.json"
        private const val KEY_PREFS = "prefs.json"
        private const val MAX_RECORDS = 800

        val instance: ChishaRepo by lazy { ChishaRepo() }
    }
}
