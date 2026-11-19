package com.chisha.data

/**
 * 只凭一个菜名，把这道菜的其他信息全推出来。
 *
 * 用户添加菜品时只该填菜名 —— 菜系、时段、标签、辣度、价格、配图类别、做法步骤，
 * 全都能从名字里读出来。这里只做纯本地推断（不发请求），图另外走 DishPhoto。
 * 每个字段都是「建议值」：用户在编辑面板里手改了就按他的，没改就用这里的。
 */
object DishAuto {

    data class Draft(
        val slots: Set<Slot>,
        val cuisine: String,
        val tags: List<String>,
        val spicy: Int,
        val priceLevel: Int,
        val imageKey: String,
        val ingredients: List<String>,
        val steps: List<String>,
    )

    /** 早餐向的字眼：命中就至少算早餐 */
    private val BREAKFAST_WORDS = listOf(
        "粥", "豆浆", "油条", "包子", "馒头", "花卷", "煎饼", "灌饼", "烧饼", "蒸饺",
        "肠粉", "面茶", "胡辣汤", "茶叶蛋", "水煮蛋", "荷包蛋", "蒸蛋", "牛奶", "吐司",
        "三明治", "小笼", "生煎", "蛋饼", "豆花", "汤圆", "麦片", "燕麦", "面包",
        "发糕", "锅贴", "馄饨", "饭团", "饭卷", "粥面", "汤面", "挂面",
    )

    /** 夜宵向：烧烤、炸物、大排档那一挂 */
    private val NIGHT_WORDS = listOf(
        "烧烤", "烤串", "串串", "钵钵鸡", "小龙虾", "麻辣烫", "冒菜", "关东煮", "卤味",
        "炸串", "炸鸡", "炒粉", "炒面", "方便面", "啤酒", "夜宵", "大排档", "铁板", "蚵仔煎",
    )

    /** 菜系：按名字里的做法/地名判定，越具体的排前面 */
    private val CUISINES = listOf(
        "川菜" to listOf("回锅", "宫保", "麻婆", "水煮", "辣子", "鱼香", "夫妻肺片", "口水鸡", "蒜泥白肉", "干煸", "麻辣", "毛血旺", "灯影", "棒棒鸡"),
        "湘菜" to listOf("剁椒", "擂", "腊味", "小炒肉", "外婆菜", "口味虾", "湘西"),
        "粤菜" to listOf("白灼", "清蒸", "豉油", "煲仔", "烧鹅", "叉烧", "虾饺", "肠粉", "艇仔", "老火", "砂锅粥", "啫啫"),
        "鲁菜" to listOf("葱烧", "九转", "把子肉", "糖醋鲤鱼", "油焖大虾"),
        "江浙菜" to listOf("狮子头", "蟹粉", "东坡", "西湖", "龙井", "梅干菜", "醉鸡", "糟"),
        "西北菜" to listOf("拉面", "肉夹馍", "臊子", "羊肉泡", "大盘鸡", "馕", "手抓"),
        "东北菜" to listOf("锅包肉", "地三鲜", "小鸡炖蘑菇", "酸菜白肉", "乱炖", "血肠"),
        "云贵菜" to listOf("过桥米线", "酸汤", "折耳根", "汽锅", "丝娃娃"),
        "日式" to listOf("寿司", "刺身", "丼", "味噌", "天妇罗", "亲子", "照烧", "寿喜"),
        "韩式" to listOf("泡菜", "拌饭", "部队锅", "石锅", "炒年糕", "冷面", "参鸡汤"),
        "西式" to listOf("披萨", "意面", "汉堡", "牛排", "沙拉", "三明治", "咖喱", "焗"),
        "东南亚" to listOf("冬阴功", "咖喱蟹", "越南", "泰式", "沙爹", "菠萝饭"),
    )

    /** 明确的重辣写法 */
    private val FIRE_WORDS = listOf("变态辣", "重辣", "魔鬼辣", "超辣", "特辣")

    /** 中等辣度：名字里点到这些就按「辣」算 */
    private val SPICY_WORDS = listOf(
        "辣", "麻", "剁椒", "泡椒", "小米椒", "尖椒", "水煮", "香辣", "麻辣", "椒麻",
        "红油", "藤椒", "胡椒", "咖喱",
        // 菜名里不带「辣」字、但本身就是辣的川湘菜
        "宫保", "鱼香", "干锅", "毛血旺", "口水鸡", "辣子", "回锅", "小炒肉", "香锅", "酸汤鱼",
    )

    /** 这些「椒」是甜的，别当成辣 */
    private val SWEET_PEPPER = listOf("甜椒", "彩椒", "柿子椒", "青椒酿")

    /** 不辣才是特点的写法：这类菜按 0 辣算，只打「清淡」标签 */
    private val CLEAN_WORDS = listOf("白灼", "清蒸", "清淡", "原味", "清汤", "素炒")

    /** 只有「微辣」这种明确写法才记为 1 级 */
    private val MILD_WORDS = listOf("微辣", "小辣", "微微辣")

    /** 素菜词：名字里只有这些、没有肉字，就是素的 */
    private val VEG_WORDS = listOf(
        "土豆", "白菜", "青菜", "娃娃菜", "生菜", "菠菜", "芹菜", "韭菜", "黄瓜", "番茄",
        "西红柿", "茄子", "豆角", "藕", "萝卜", "冬瓜", "南瓜", "丝瓜", "苦瓜", "蘑菇",
        "香菇", "木耳", "豆腐", "豆芽", "花菜", "西兰花", "山药", "芋头", "玉米", "南瓜",
    )

    private const val MEAT_CHARS = "猪牛羊鸡鸭鱼虾蟹鹅肉排骨"

    fun draft(rawName: String): Draft {
        val name = rawName.trim()
        if (name.isBlank()) {
            return Draft(setOf(Slot.LUNCH, Slot.DINNER), "家常", emptyList(), 0, 2, ImageKey.STIRFRY, emptyList(), emptyList())
        }
        val cuisine = cuisineOf(name)
        val slots = slotsOf(name)
        val spicy = spicyOf(name)
        val ingredients = PriceTag.guessIngredients(name)
        val priceLevel = PriceTag.level(name, ingredients)
        // 图类别只看菜名和菜系：把标签也塞进去会让「慢炖」这类词把红烧肉判成汤羹
        val imageKey = ImageKey.derive(name, cuisine, emptyList())
        val steps = RecipeDetail.of(name)?.steps ?: emptyList()
        return Draft(
            slots = slots,
            cuisine = cuisine,
            tags = tagsOf(name, cuisine, spicy),
            spicy = spicy,
            priceLevel = priceLevel,
            imageKey = imageKey,
            ingredients = ingredients,
            steps = steps,
        )
    }

    /** 一句话把推断结果说清楚，给编辑面板顶部的提示用 */
    fun summary(d: Draft): String = buildString {
        append(d.cuisine)
        append(" · ")
        append(d.slots.sortedBy { it.order }.joinToString("/") { it.label })
        append(" · ")
        append(d.priceLevel.let { "¥".repeat(it) })
        append(" · ")
        append(
            when (d.spicy) {
                0 -> "不辣"
                1 -> "微辣"
                2 -> "辣"
                else -> "重辣"
            }
        )
        append(" · ")
        append(ImageKey.labels[d.imageKey] ?: "炒菜")
    }

    private fun slotsOf(name: String): Set<Slot> {
        val bf = BREAKFAST_WORDS.any { name.contains(it) }
        val night = NIGHT_WORDS.any { name.contains(it) }
        return when {
            // 「皮蛋瘦肉粥」既像早餐也像夜宵，给两段
            bf && night -> setOf(Slot.BREAKFAST, Slot.MIDNIGHT)
            bf -> setOf(Slot.BREAKFAST, Slot.LUNCH)
            night -> setOf(Slot.DINNER, Slot.MIDNIGHT)
            else -> setOf(Slot.LUNCH, Slot.DINNER)
        }
    }

    private fun cuisineOf(name: String): String {
        CUISINES.firstOrNull { (_, words) -> words.any { name.contains(it) } }?.let { return it.first }
        // 「粉」「面」这种单字太泛了（粉丝蒸娃娃菜会被判成面食），必须点具体品类
        val noodle = listOf(
            "面条", "拉面", "意面", "炒面", "凉面", "拌面", "挂面", "汤面", "刀削面", "烩面",
            "米线", "米粉", "河粉", "炒粉", "粉丝煲", "凉皮", "馄饨", "面片",
        )
        return when {
            noodle.any { name.contains(it) } -> "面食"
            // 「粉丝」是配菜不是主食，别把蒜蓉粉丝蒸娃娃菜算成面食
            name.contains("面") && !name.contains("粉丝") -> "面食"
            name.contains("粥") -> "粥品"
            name.contains("汤") || name.contains("羹") -> "汤羹"
            name.contains("饼") -> "面点"
            else -> "家常"
        }
    }

    private fun spicyOf(name: String): Int {
        if (SWEET_PEPPER.any { name.contains(it) }) return 0
        if (FIRE_WORDS.any { name.contains(it) }) return 3
        if (SPICY_WORDS.any { name.contains(it) }) return 2
        if (MILD_WORDS.any { name.contains(it) }) return 1
        return 0
    }

    private fun tagsOf(name: String, cuisine: String, spicy: Int): List<String> {
        val tags = mutableListOf<String>()
        if (spicy >= 2) tags += "辣"
        if (CLEAN_WORDS.any { name.contains(it) }) tags += "清淡"
        if (name.contains("汤") || name.contains("羹") || name.contains("粥")) tags += "汤水"
        if (name.contains("饭") || name.contains("面") || name.contains("粉") || name.contains("饼")) tags += "主食"
        if (name.any { it in MEAT_CHARS } && VEG_WORDS.none { name.contains(it) }) tags += "高蛋白"
        if (VEG_WORDS.any { name.contains(it) } && name.none { it in MEAT_CHARS }) tags += "素菜"
        if (name.contains("凉拌") || name.contains("拌") || name.contains("沙拉")) tags += "清爽"
        if (name.contains("炒") || name.contains("爆")) tags += "快手"
        if (name.contains("炖") || name.contains("焖") || name.contains("煲") || name.contains("红烧")) tags += "下饭"
        if (tags.size < 3 && cuisine.isNotBlank()) tags += cuisine
        return tags.distinct().take(3)
    }
}
