package com.chisha.data

import kotlinx.serialization.Serializable

/** 四个时段：早餐 / 午餐 / 晚餐 / 夜宵 */
@Serializable
enum class Slot(val label: String, val hint: String) {
    BREAKFAST("早餐", "开启一天"),
    LUNCH("午餐", "补给能量"),
    DINNER("晚餐", "好好吃饭"),
    MIDNIGHT("夜宵", "深夜慰藉");

    val order: Int get() = ordinal
}

/** 菜品来源 */
@Serializable
enum class Origin { SEED, USER, NETWORK }

/** 决定方式 */
enum class PickMode(val label: String) {
    PURE("随机"),
    SMART("推荐")
}

@Serializable
data class Dish(
    val id: String,
    val name: String,
    val slots: Set<Slot> = setOf(Slot.LUNCH, Slot.DINNER),
    val cuisine: String = "家常",
    val tags: List<String> = emptyList(),
    /** 内置图包 key，见 ImageKey */
    val imageKey: String? = null,
    /** 用户自定义图片（本地 Uri 或网络 URL） */
    val imageUri: String? = null,
    /** 价格档位 1-3 */
    val priceLevel: Int = 2,
    /** 辣度 0-3 */
    val spicy: Int = 0,
    /** 手动权重，越大越容易被抽中 */
    val weight: Float = 1f,
    val origin: Origin = Origin.USER,
    val enabled: Boolean = true,
    val note: String = "",
    /** 网络热度 0-100 */
    val heat: Int = 0,
    /** 做法步骤；为空时按菜名查内置菜谱，再退化到品类模板 */
    val steps: List<String> = emptyList(),
    /** 主要食材 */
    val ingredients: List<String> = emptyList(),
) {
    val priceText: String
        get() = when (priceLevel) {
            1 -> "¥"
            2 -> "¥¥"
            else -> "¥¥¥"
        }

    /** 档位的白话说明，光看 ¥¥¥ 猜不出含义，必须配个字 */
    val priceLabel: String
        get() = when (priceLevel) {
            1 -> "平价"
            2 -> "适中"
            else -> "讲究"
        }

    /** 小圆标里只放得下一两个字符，直接给「¥8」这种能看懂的写法 */
    val priceShort: String
        get() = "¥" + PriceTag.money(name, ingredients, priceLevel)

    /** 估算说明，例：自制约 20 元。口径是买菜成本，写明「自制」才不会被当成外卖价 */
    val priceHint: String
        get() = "自制约 " + PriceTag.money(name, ingredients, priceLevel) + " 元"

    /** 详情页展示用的完整菜谱：内置详版 > 自带步骤 > 品类模板 */
    val recipe: Recipe
        get() = RecipeDetail.of(name)
            ?: if (steps.isNotEmpty()) Recipe(ingredients, steps)
            else RecipeBook.rich(name, imageKey)
}

@Serializable
data class MealRecord(
    val dishId: String,
    val dishName: String,
    val slot: Slot,
    val at: Long,
    /** 本地日期 yyyy-MM-dd，便于按天统计 */
    val dateKey: String = "",
    /** 是否被采纳（点了“就它了”），仅“换一个”不算 */
    val accepted: Boolean,
    val cuisine: String = "",
)

@Serializable
data class Prefs(
    val theme: String = "DARK_GOLD",
    val mode: String = "SMART",
    /** 忌口标签，命中即排除 */
    val allergies: List<String> = emptyList(),
    /** 已屏蔽的菜系 */
    val blockedCuisines: List<String> = emptyList(),
    val haptics: Boolean = true,
    val avoidRepeatWindow: Int = 5,
)

object ImageKey {
    const val NOODLE = "cat_noodle"
    const val RICE = "cat_rice"
    const val HOTPOT = "cat_hotpot"
    const val BBQ = "cat_bbq"
    const val SOUP = "cat_soup"
    const val DUMPLING = "cat_dumpling"
    const val PORRIDGE = "cat_porridge"
    const val BAO = "cat_bao"
    const val SALAD = "cat_salad"
    const val BURGER = "cat_burger"
    const val PIZZA = "cat_pizza"
    const val SUSHI = "cat_sushi"
    const val KOREAN = "cat_korean"
    const val SNACK = "cat_snack"
    const val DESSERT = "cat_dessert"
    const val DRINK = "cat_drink"
    const val STIRFRY = "cat_stirfry"

    val all = listOf(
        NOODLE, RICE, HOTPOT, BBQ, SOUP, DUMPLING, PORRIDGE, BAO,
        SALAD, BURGER, PIZZA, SUSHI, KOREAN, SNACK, DESSERT, DRINK, STIRFRY,
    )

    val labels: Map<String, String> = mapOf(
        NOODLE to "面食", RICE to "米饭", HOTPOT to "火锅", BBQ to "烧烤",
        SOUP to "汤羹", DUMPLING to "饺子", PORRIDGE to "粥品", BAO to "包子",
        SALAD to "轻食", BURGER to "汉堡", PIZZA to "披萨", SUSHI to "日料",
        KOREAN to "韩餐", SNACK to "小吃", DESSERT to "甜品", DRINK to "饮品",
        STIRFRY to "炒菜",
    )

    /**
     * 按菜名、菜系、标签自动匹配最贴近的图包类别。
     * 用户自建菜品、网络收录菜品都走这里，保证「每道菜都有图」。
     * 命中顺序即优先级：越具体的类别越靠前。
     */
    fun derive(name: String, cuisine: String = "", tags: List<String> = emptyList()): String {
        val hay = name + " " + cuisine + " " + tags.joinToString(" ")
        fun has(vararg k: String) = k.any { hay.contains(it) }
        return when {
            has("寿司", "刺身", "日料", "生鱼", "三文鱼", "丼") -> SUSHI
            has("披萨", "意式饼") -> PIZZA
            has("汉堡", "三明治", "吐司", "帕尼尼", "贝果") -> BURGER
            has("拌饭", "部队锅", "韩式", "炸鸡", "泡菜", "年糕", "石锅") -> KOREAN
            has("沙拉", "轻食", "酸奶碗", "油醋") -> SALAD
            has("火锅", "麻辣烫", "冒菜", "串串", "涮锅") -> HOTPOT
            has("烧烤", "烤串", "烤鱼", "烤肉", "小龙虾", "铁板", "炸串") -> BBQ
            has("粥", "稀饭") -> PORRIDGE
            has("油条", "煎饼", "灌饼", "烧饼", "肠粉", "小吃", "卤味", "关东煮", "零食") -> SNACK
            has("饺", "馄饨", "锅贴", "烧麦") -> DUMPLING
            has("汤", "羹", "炖", "豆腐脑") -> SOUP
            has("包", "馒头", "花卷") -> BAO
            has("面", "粉", "米线", "拉面", "意面", "凉皮", "河粉") -> NOODLE
            has("饭", "盖浇", "炒饭", "煲仔") -> RICE
            has("甜", "蛋糕", "布丁", "冰淇淋", "糖水") -> DESSERT
            has("奶茶", "咖啡", "牛奶", "豆浆", "果汁", "汽水", "饮料", "茶饮") -> DRINK
            else -> STIRFRY
        }
    }
}
