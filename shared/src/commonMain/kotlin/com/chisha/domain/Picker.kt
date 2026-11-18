package com.chisha.domain

import com.chisha.data.Dish
import com.chisha.data.Prefs
import com.chisha.data.Slot
import kotlin.random.Random

/** 抽签引擎：纯随机 / 智能推荐（加权 + 时段亲和 + 重复衰减） */
object Picker {

    fun candidates(
        dishes: List<Dish>,
        prefs: Prefs,
        slot: Slot,
        strictSlot: Boolean = true,
    ): List<Dish> = dishes.filter { d ->
        d.enabled &&
            (!strictSlot || slot in d.slots) &&
            d.cuisine !in prefs.blockedCuisines &&
            d.tags.none { it in prefs.allergies }
    }

    /** 计算权重：纯随机恒定 1.0；智能模式叠加热度、时段亲和、最近吃过衰减 */
    fun weights(
        list: List<Dish>,
        prefs: Prefs,
        slot: Slot,
        recentIds: List<String>,
        smart: Boolean,
    ): List<Float> {
        if (!smart) return List(list.size) { 1f }
        return list.map { d ->
            var w = d.weight.coerceIn(0.05f, 10f)
            w *= 1f + d.heat.coerceIn(0, 100) / 130f

            val idx = recentIds.indexOf(d.id)
            if (idx >= 0) {
                w *= if (idx < prefs.avoidRepeatWindow) 0.22f else 0.55f
            }

            w *= slotAffinity(d, slot)
            w
        }
    }

    /** 时段亲和系数：让早餐更偏清淡面点、夜宵更偏重口小吃 */
    private fun slotAffinity(d: Dish, slot: Slot): Float = when (slot) {
        Slot.BREAKFAST -> {
            var f = 1f
            if (d.tags.any { it in setOf("粥", "清淡", "蒸", "面", "鸡蛋") }) f += 0.35f
            if (d.tags.any { it in setOf("辣", "重口", "酒", "聚餐") }) f -= 0.4f
            f.coerceAtLeast(0.15f)
        }
        Slot.LUNCH -> {
            var f = 1f
            if (d.tags.any { it in setOf("米饭", "面", "下饭", "快手") }) f += 0.25f
            f
        }
        Slot.DINNER -> {
            var f = 1f
            if (d.tags.any { it in setOf("聚餐", "汤", "炒", "下饭") }) f += 0.2f
            f
        }
        Slot.MIDNIGHT -> {
            var f = 1f
            if (d.tags.any { it in setOf("油炸", "辣", "烧烤", "小吃", "快手") }) f += 0.3f
            if (d.tags.any { it in setOf("清淡", "粥") }) f -= 0.2f
            f.coerceAtLeast(0.2f)
        }
    }

    fun pick(list: List<Dish>, weights: List<Float>, random: Random = Random): Dish? {
        if (list.isEmpty()) return null
        val total = weights.sum().coerceAtLeast(0.0001f)
        var r = random.nextFloat() * total
        for (i in list.indices) {
            r -= weights[i]
            if (r <= 0f) return list[i]
        }
        return list.last()
    }

    /** 同类图片最多保留 maxPerKey 个：避免整盘都是同一种菜相 */
    fun diversify(list: List<Dish>, maxPerKey: Int = 2): List<Dish> {
        val count = HashMap<String, Int>()
        val head = ArrayList<Dish>(list.size)
        val tail = ArrayList<Dish>(list.size)
        list.forEach { d ->
            val k = d.imageKey ?: "none"
            val c = count[k] ?: 0
            if (c < maxPerKey) {
                count[k] = c + 1
                head.add(d)
            } else {
                tail.add(d)
            }
        }
        return head + tail
    }

    private val MEAT = "猪牛羊鸡鸭鱼虾蟹鹅"
    private val SIDE = listOf("山药", "土豆", "豆腐", "茄子", "排骨", "鸡翅", "藕", "冬瓜", "白菜")

    /** 主料归类：红烧羊肉 / 涮羊肉 / 羊肉汤 都算「羊」，避免一整盘全是羊 */
    private fun mainMark(name: String): String {
        MEAT.firstOrNull { name.contains(it) }?.let { return it.toString() }
        SIDE.firstOrNull { name.contains(it) }?.let { return it }
        return ""
    }

    /**
     * 转盘要看得出的差别：同一主料（含同类菜相）最多 maxSame 个，
     * 剩下的菜宁可不用，也不要给一盘「羊肉×3 + 山药×2」。
     * 菜库本身不够时再用其余菜补满，别留空转盘。
     */
    fun variety(list: List<Dish>, n: Int, maxSame: Int = 2): List<Dish> {
        val out = ArrayList<Dish>(n)
        val mark = HashMap<String, Int>()
        val art = HashMap<String, Int>()
        for (d in list) {
            if (out.size >= n) break
            val k = mainMark(d.name)
            val a = d.imageKey ?: "none"
            if (k.isNotEmpty() && (mark[k] ?: 0) >= maxSame) continue
            if ((art[a] ?: 0) >= maxSame) continue
            if (k.isNotEmpty()) mark[k] = (mark[k] ?: 0) + 1
            art[a] = (art[a] ?: 0) + 1
            out += d
        }
        if (out.size < n) {
            for (d in list) {
                if (d in out) continue
                out += d
                if (out.size >= n) break
            }
        }
        return out
    }

    /** 转盘扇区：按权重做无放回抽样，最多 n 个，去重 */    fun sample(list: List<Dish>, weights: List<Float>, n: Int, random: Random = Random): List<Dish> {
        if (list.isEmpty()) return emptyList()
        val pool = list.indices.toMutableList()
        val w = weights.toMutableList()
        val out = ArrayList<Dish>(n)
        repeat(n.coerceAtMost(list.size)) {
            val total = w.sum().coerceAtLeast(0.0001f)
            var r = random.nextFloat() * total
            var hit = pool.lastIndex
            for (i in pool.indices) {
                r -= w[i]
                if (r <= 0f) { hit = i; break }
            }
            out += list[pool[hit]]
            pool.removeAt(hit)
            w.removeAt(hit)
        }
        return out
    }
}
