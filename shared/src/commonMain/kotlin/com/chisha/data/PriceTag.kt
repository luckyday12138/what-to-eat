package com.chisha.data

/**
 * 菜品价格：按「自己买菜做这道菜」的食材成本算，单价全部取市场平均采购价。
 *
 * 口径说明（避免和外卖价混淆）：
 * - 单价是菜市场均价（元/斤），不是餐厅售价，所以整体偏低是正常的；
 * - 含 4 元固定摊销（油盐酱醋、燃气水电、洗切损耗）；
 * - 调料不单独计价（已在摊销里），只算主料和用量；
 * - 每档的金额会被夹在该档区间内，避免「¥¥¥ 的菜写着 12 元」这种自相矛盾。
 *
 * 分档阈值：≤12 元 ¥ / 12–28 元 ¥¥ / >28 元 ¥¥¥。
 */
object PriceTag {

    /** 油盐酱醋 + 燃气水电 + 损耗摊销 */
    private const val SEASONING = 4.0

    /** 元/斤（500g）市场平均价。比餐厅价低、比批发价高，对着菜市场走一圈的量级 */
    private val UNIT: Map<String, Double> = mapOf(
        // 蔬菜
        "白菜" to 2.5, "青菜" to 3.0, "小白菜" to 3.0, "娃娃菜" to 4.0, "生菜" to 4.0,
        "菠菜" to 4.5, "油菜" to 3.5, "芹菜" to 4.0, "韭菜" to 4.5, "蒜苗" to 6.0,
        "香菜" to 8.0, "土豆" to 2.5, "红薯" to 3.0, "芋头" to 5.0, "山药" to 7.0,
        "莲藕" to 7.0, "萝卜" to 2.5, "胡萝卜" to 3.0, "冬瓜" to 2.5, "南瓜" to 3.5,
        "丝瓜" to 4.0, "苦瓜" to 5.0, "黄瓜" to 3.5, "番茄" to 4.0, "西红柿" to 4.0,
        "茄子" to 4.0, "青椒" to 4.0, "彩椒" to 7.0, "尖椒" to 4.0, "洋葱" to 3.0,
        "包菜" to 2.5, "卷心菜" to 2.5, "花菜" to 5.0, "西兰花" to 6.0, "豆芽" to 2.5,
        "豆角" to 7.0, "四季豆" to 7.0, "豇豆" to 7.0, "荷兰豆" to 9.0, "豌豆" to 7.0,
        "玉米" to 3.5, "毛豆" to 7.0, "冬笋" to 10.0, "竹笋" to 9.0, "芦笋" to 12.0,
        "茭白" to 7.0, "莴笋" to 4.0, "芥菜" to 4.0, "空心菜" to 3.5, "青蒜" to 5.0,
        // 菌菇藻类
        "香菇" to 8.0, "平菇" to 6.0, "口蘑" to 10.0, "金针菇" to 5.0, "杏鲍菇" to 7.0,
        "木耳" to 25.0, "银耳" to 22.0, "海带" to 6.0, "紫菜" to 18.0, "裙带菜" to 10.0,
        // 禽畜
        "五花肉" to 15.0, "瘦肉" to 16.0, "里脊" to 18.0, "猪肉" to 15.0, "肉末" to 16.0,
        "肉片" to 16.0, "肉丝" to 16.0, "肉块" to 15.0, "肉馅" to 16.0,
        "排骨" to 22.0, "猪蹄" to 20.0, "猪骨" to 12.0, "猪肚" to 28.0, "猪肝" to 12.0,
        "腰花" to 20.0, "猪腰" to 20.0, "腊肉" to 35.0, "香肠" to 30.0, "培根" to 28.0,
        // 牛羊肉的实际市场价远高于猪鸡，之前按 34~36 算出来的 300g 只有二十出头
        "牛腩" to 38.0, "牛肉" to 42.0, "牛里脊" to 48.0, "牛腱" to 40.0, "羊腩" to 40.0,
        "牛排" to 52.0, "牛柳" to 50.0, "牛肋条" to 45.0, "牛仔骨" to 35.0, "鸡排" to 12.0, "猪排" to 20.0,
        "牛肉片" to 42.0, "牛肉末" to 40.0, "肥肠" to 22.0, "大肠" to 22.0, "肉串" to 20.0,
        "咸鱼" to 25.0, "牛骨" to 12.0, "糯米" to 3.5, "糯米粉" to 4.0, "小米" to 4.0,
        "低筋面粉" to 3.0, "高筋面粉" to 3.5, "松子" to 60.0, "冬瓜糖" to 12.0,
        "甜玉米粒" to 4.0, "青豆" to 8.0, "胡萝卜丁" to 3.0, "茶树菇" to 18.0,
        "苋菜" to 4.0, "剁椒" to 8.0, "腐乳" to 8.0,
        "羊肉" to 42.0, "羊腿" to 40.0, "羊排" to 46.0, "鸡腿" to 10.0, "鸡胸" to 10.0,
        "鸡肉" to 12.0, "整鸡" to 12.0, "鸡翅" to 15.0, "鸡爪" to 12.0, "鸭" to 15.0,
        "鸭腿" to 16.0, "鸭胸" to 16.0, "鸭翅" to 16.0,
        // 水产
        "草鱼" to 9.0, "鲫鱼" to 11.0, "鲤鱼" to 9.0, "鲈鱼" to 20.0, "带鱼" to 16.0,
        "黄鱼" to 25.0, "鲳鱼" to 25.0, "鳕鱼" to 45.0, "鳜鱼" to 38.0, "鲶鱼" to 12.0,
        "黑鱼" to 18.0, "龙利鱼" to 28.0, "三文鱼" to 50.0, "偏口鱼" to 18.0,
        "虾" to 32.0, "虾仁" to 38.0, "基围虾" to 35.0, "河虾" to 28.0,
        "龙虾" to 100.0, "小龙虾" to 22.0, "蟹" to 50.0, "螃蟹" to 50.0,
        "蛤蜊" to 8.0, "花蛤" to 8.0, "扇贝" to 15.0, "生蚝" to 12.0, "鱿鱼" to 20.0,
        "墨鱼" to 24.0, "牛蛙" to 20.0, "田螺" to 8.0, "鱼丸" to 15.0,
        // 蛋豆制品
        "鸡蛋" to 5.5, "鸭蛋" to 7.0, "皮蛋" to 10.0, "咸蛋" to 10.0, "蛋清" to 6.0,
        "豆腐" to 2.5, "嫩豆腐" to 2.5, "老豆腐" to 3.0, "豆干" to 6.0, "香干" to 6.0,
        "腐竹" to 12.0, "豆皮" to 6.0, "千张" to 8.0, "油豆腐" to 6.0, "豆泡" to 6.0,
        "面筋" to 6.0, "素鸡" to 6.0, "豆浆" to 2.0,
        // 主食
        "米" to 2.5, "米饭" to 2.5, "面" to 2.5, "面粉" to 2.5, "面条" to 3.5,
        "米线" to 4.0, "米粉" to 4.0, "河粉" to 4.0, "年糕" to 6.0, "粉丝" to 8.0,
        "粉条" to 8.0, "饺子皮" to 4.0, "馄饨皮" to 4.0, "面包" to 7.0,
        // 配杂
        "花生米" to 10.0, "花生碎" to 10.0, "黄豆" to 5.0, "绿豆面" to 4.0,
        "碱水面" to 3.5, "粘米粉" to 4.0, "薄脆" to 6.0, "咖啡粉" to 55.0,
        "酸奶" to 10.0, "燕麦片" to 8.0, "意面" to 7.0, "淡奶油" to 25.0, "芝士" to 35.0,
        "腊肠" to 30.0, "梅花肉" to 20.0, "叉烧" to 30.0, "午餐肉" to 18.0,
        "火腿" to 22.0, "火腿骨" to 8.0, "方便面" to 5.0, "泡菜" to 8.0, "酸笋" to 6.0,
        "丸子" to 16.0, "冷面" to 5.0, "啤酒" to 5.0, "海苔" to 25.0, "生鱼片" to 50.0,
        "牛油" to 25.0, "毛肚" to 28.0, "肥牛" to 38.0, "蔬菜" to 4.0, "时蔬" to 4.0,
        "红豆" to 7.0, "莲子" to 18.0, "蓝莓" to 25.0, "牛骨" to 12.0, "榨菜" to 6.0,
        "咸菜" to 5.0, "酸菜" to 5.0, "雪菜" to 5.0, "荸荠" to 6.0, "蒜薹" to 6.0,
        "面包粉" to 5.0, "吐司" to 8.0, "猪肥膘" to 10.0, "猪梅花肉" to 20.0,
    )

    /** 按「个」计价的，元/个（不与斤混用，避免鲍鱼被算成 3 块钱） */
    private val PER_PIECE: Map<String, Double> = mapOf(
        "鲍鱼" to 8.0, "海参" to 20.0, "燕窝" to 50.0, "鹅肝" to 35.0,
        "馒头" to 1.2, "包子" to 2.0, "烧麦" to 2.5, "饺子" to 0.8, "馄饨" to 0.6,
        "油条" to 1.5, "茶叶蛋" to 1.5, "粽子" to 4.0, "月饼" to 5.0,
    )

    /** 出现即至少 ¥¥¥ 的硬货 */
    private val PREMIUM = listOf(
        "龙虾", "鲍鱼", "海参", "鱼翅", "燕窝", "帝王蟹", "大闸蟹", "螃蟹",
        "和牛", "牛排", "羊排", "鳜鱼", "三文鱼", "金枪鱼", "鹅肝", "松露", "鱼子酱",
    )

    /** 调料与辅料：钱已算进 SEASONING，不重复计价 */
    private val SEASONINGS = listOf(
        "盐", "精盐", "味精", "鸡精", "糖", "白糖", "冰糖", "蜂蜜", "生抽", "老抽",
        "酱油", "醋", "香醋", "陈醋", "料酒", "黄酒", "淀粉", "生粉",
        "花椒", "花椒油", "八角", "大料", "桂皮", "香叶", "干辣椒", "辣椒粉", "辣椒油",
        "香油", "芝麻油", "蚝油", "豆瓣酱", "黄豆酱", "甜面酱", "芝麻酱", "花生酱",
        "番茄酱", "沙茶酱", "叉烧酱", "豆豉", "咖喱", "色拉油", "食用油", "植物油",
        "花生油", "橄榄油", "菜籽油", "猪油", "葱", "姜", "蒜", "香菜末", "葱花",
        "姜片", "蒜末", "蒜蓉", "蒜片", "蒜瓣", "枸杞", "芝麻", "白胡椒", "黑胡椒",
        "胡椒", "五香粉", "孜然", "泡打粉", "酵母", "小苏打", "干葱", "清水", "凉水",
        "清汤", "高汤", "鲜汤", "蒸鱼豉油", "花椒盐", "椒盐", "冬菇仔",
    )

    /** 中文数量词：老菜谱爱写「半只」「六两」「一汤匙」 */
    private val CN_NUM = mapOf(
        "半" to 0.5, "一" to 1.0, "二" to 2.0, "两" to 2.0, "三" to 3.0,
        "四" to 4.0, "五" to 5.0, "六" to 6.0, "七" to 7.0, "八" to 8.0, "九" to 9.0,
    )

    /** 只是做法名的字，判价前先剔掉：「鱼香肉丝」不是鱼 */
    private val FAKE_WORDS = listOf("鱼香", "虎皮", "蚂蚁上树", "夫妻肺片", "老婆饼")

    /** 单个按只/条卖的克重，别把一只扇贝当成 450 克 */
    private val PIECE_WEIGHT: Map<String, Double> = mapOf(
        "扇贝" to 60.0, "生蚝" to 80.0, "蛤蜊" to 12.0, "花蛤" to 12.0, "田螺" to 12.0,
        "基围虾" to 15.0, "河虾" to 10.0, "虾" to 15.0, "小龙虾" to 25.0, "龙虾" to 500.0,
        "蟹" to 250.0, "螃蟹" to 250.0, "鲍鱼" to 60.0, "海参" to 80.0,
        "鱿鱼" to 250.0, "牛蛙" to 150.0, "玉米" to 250.0, "红薯" to 200.0,
    )

    /** 家常小炒的兜底金额：没有食材清单时按菜名给量级 */
    private val CHEAP_COST: Map<String, Double> = mapOf(
        "馒头" to 2.0, "包子" to 3.0, "米饭" to 3.0, "粥" to 4.0, "面" to 5.0, "饼" to 5.0,
        "凉皮" to 6.0, "蛋" to 6.0, "豆腐" to 6.0, "粉丝" to 7.0, "粉条" to 7.0,
        "土豆" to 5.0, "白菜" to 4.0, "青菜" to 4.0, "豆芽" to 4.0, "黄瓜" to 4.0,
        "茄子" to 5.0, "番茄" to 5.0, "西红柿" to 5.0, "萝卜" to 4.0, "冬瓜" to 4.0,
        "南瓜" to 4.0, "海带" to 5.0, "木耳" to 6.0, "菇" to 7.0, "菠菜" to 5.0,
        "韭菜" to 5.0, "藕" to 6.0, "奶" to 4.0, "麦片" to 5.0, "时蔬" to 5.0,
    )

    /** 明显上档的主料词（写具体词，别写「牛」「鸡」「肉」这种半个字） */
    private val PRICEY_HINT = listOf(
        "牛腩", "牛肉", "牛排", "牛柳", "牛腱", "羊肉", "羊排", "猪肉", "五花",
        "排骨", "猪蹄", "肘子", "猪肚", "大肠", "虾", "蟹", "鱼", "腊肉", "香肠",
        "培根", "猪肝", "腰花",
        "鸡腿", "鸡翅", "鸡肉", "鸡丁", "鸡块", "烤鸡", "烧鸡", "扒鸡", "黄焖鸡", "大盘鸡",
        "鸭肉", "烤鸭", "酱鸭", "老鸭", "鸭腿", "鸭掌",
    )

    /** 泛字兜底：「炒肉」「炖鱼」这种没写具体主料的，按最常见的那一个算 */
    private val GENERIC = listOf(
        "羊" to "羊肉", "牛" to "牛肉", "猪" to "猪肉", "鸡" to "鸡肉",
        "鸭" to "鸭肉", "鱼" to "草鱼", "虾" to "虾", "蟹" to "蟹",
        "蛋" to "鸡蛋", "豆腐" to "豆腐", "菜" to "青菜", "面" to "面条",
        "米" to "米饭", "粥" to "米饭",
        // 「红烧肉」「回锅肉」只认出一个肉字，按猪肉算总比清单空着强
        "肉" to "猪肉",
    )

    /** 只当调料用的词：出现在菜名里不代表要买它，别算进食材清单 */
    private val CONDIMENT_NAMES = listOf(
        "剁椒", "泡椒", "豆瓣", "腐乳", "榨菜", "咸菜", "酸菜", "雪菜", "泡菜", "酸笋",
        "花椒", "干辣椒", "辣椒粉", "辣椒油", "豆豉", "咖喱", "沙茶", "芝麻酱", "花生酱",
    )

    /** 「肉串」「肉片」这类泛称：已经认出具体肉类时就不再重复算一笔 */
    private val GENERIC_MEAT = listOf("肉串", "肉片", "肉丝", "肉末", "肉块", "肉馅", "丸子")

    /**
     * 从菜名里认出主料，拼成食材清单（「猪肉 200克」这种写法，和源站数据同一种格式）。
     * 用户只填菜名时，价格与菜谱都靠它打底；实在认不出来就返回空，让价格退回按菜名估。
     */
    fun guessIngredients(name: String): List<String> {
        var cleaned = name
        for (w in FAKE_WORDS) cleaned = cleaned.replace(w, "")
        if (cleaned.isBlank()) cleaned = name
        // 长词优先：命中「牛肉片」之后就不再算「牛肉」「肉片」
        val hits = UNIT.keys.asSequence()
            .filter { cleaned.contains(it) }
            // 注意方向：是「这个词本身属于调料」，不是「调料表里有词包含它」——
            // 反着写会把「番茄」被「番茄酱」连坐掉，番茄鸡蛋面就只剩鸡蛋了
            .filterNot { k -> k in CONDIMENT_NAMES || SEASONINGS.any { it == k || k.contains(it) } }
            .sortedWith(compareByDescending<String> { it.length }.thenByDescending { score(it) })
            .toList()
        val picked = mutableListOf<String>()
        for (k in hits) {
            if (picked.any { it.contains(k) }) continue
            // 「羊肉串」已经认出羊肉，别再补一条「肉串」
            if (k in GENERIC_MEAT && picked.any { p -> p.isNotEmpty() && p.first() in MEAT_HEAD }) continue
            picked += k
            if (picked.size >= 3) break
        }
        if (picked.isEmpty()) {
            // 泛称一个都认不出来时，按名字里最宽的字眼给一样主料，别让清单空着
            val generic = GENERIC.firstOrNull { cleaned.contains(it.first) }
            if (generic != null) picked += generic.second
            else if (cleaned.contains("肉")) picked += "猪肉"
        }
        // 「肉丝」「肉片」按猪肉算：真正具体的肉（羊肉、鸡肉…）早就被认出来了，剩下的泛称只能当猪肉
        return picked.map { k ->
            val ing = if (k in GENERIC_MEAT) "猪肉" else k
            "$ing ${gramOf(ing)}克"
        }.distinct()
    }

    private fun gramOf(ing: String): Int = when {
        ing in PER_PIECE -> pieceGram(ing).toInt()
        ing in listOf("米", "米饭", "面", "面条", "面粉", "米线", "米粉", "河粉", "粉丝") -> 150
        else -> 200
    }

    /** 1/2/3 档。菜名给一个基准，食材金额偏离两档以上时往菜名靠一档，挡住源站串位的模板食材 */
    fun level(name: String, ingredients: List<String> = emptyList()): Int {
        // 只要能解析出食材清单，价格就完全由采购成本决定，菜名不再参与任何校正
        val lv = if (hasIngredient(name, ingredients)) {
            bucketOf(amount(name, ingredients))
        } else {
            bucketOf(amount(name, emptyList(), byNameOnly = true))
        }
        val premium = if (hasIngredient(name, ingredients)) {
            ingredients.any { ing -> PREMIUM.any { ing.contains(it) } }
        } else {
            PREMIUM.any { name.contains(it) }
        }
        return if (premium) maxOf(lv, 3) else lv
    }

    private fun bucketOf(yuan: Int): Int = when {
        yuan <= 12 -> 1
        yuan <= 28 -> 2
        else -> 3
    }

    private fun hasIngredient(name: String, ingredients: List<String>): Boolean =
        trustworthy(name, ingredients) && ingredients.any { parse(it) != null }

    /** 原始估算金额（未夹档位区间） */
    fun amount(
        name: String,
        ingredients: List<String> = emptyList(),
        byNameOnly: Boolean = false,
    ): Int {
        var total = SEASONING
        var parsed = 0
        // 源站偶尔把食材串到别的菜（「胡椒猪肚汤」配五花肉+平菇），这种清单不能拿来算钱
        if (!byNameOnly && trustworthy(name, ingredients)) {
            for (raw in ingredients) {
                val cell = parse(raw) ?: continue
                parsed++
                total += cost(cell.first, cell.second)
            }
        }
        if (parsed == 0) total += guessByName(name)
        return total.toInt().coerceAtLeast(3)
    }

    /** 菜名里点了主料字（猪/牛/鱼/蛋…）却一条清单都不含它，多半是源站串位了 */
    private fun trustworthy(name: String, ingredients: List<String>): Boolean {
        if (ingredients.isEmpty()) return false
        // 「鱼香肉丝」的鱼是味型不是食材，这类假字先剔掉再比
        var cleaned = name
        for (w in FAKE_WORDS) cleaned = cleaned.replace(w, "")
        val marks = cleaned.filter { it in "猪牛羊鸡鸭鱼虾蟹蛋豆腐菜瓜茄椒菇笋藕卜" }
        if (marks.isEmpty()) return true          // 菜名本来就没点主料，无从校验
        val joined = ingredients.joinToString()
        return marks.any { joined.contains(it) }
    }

    /** 最终金额：有食材就是实算的采购成本，不再夹进档位区间 */
    fun money(name: String, ingredients: List<String> = emptyList(), level: Int = 0): Int =
        amount(name, ingredients).coerceIn(3, 199)

    /** 展示用一句话，例：¥¥ 自制约 20 元 */
    fun hint(name: String, ingredients: List<String> = emptyList(), level: Int = 0): String {
        val lv = if (level in 1..3) level else level(name, ingredients)
        return "¥".repeat(lv) + " 自制约 " + money(name, ingredients, lv) + " 元"
    }

    /** 各档金额区间 */
    private fun band(lv: Int): IntRange = when (lv) {
        1 -> 3..12
        2 -> 12..28
        else -> 28..99
    }

    /** 解析「瘦肉 100克」「鲈鱼 1条」「老鸭 半只」 */
    private fun parse(raw: String): Pair<String, Double>? {
        val text = raw.replace("：", " ").replace(":", " ").trim()
        if (text.isBlank()) return null
        val name = text.takeWhile { !it.isDigit() && it !in "0123456789" }
            .trim().trim('、', '，', ',', '。', '·', '-', '（', '(')
        if (name.isBlank()) return null
        if (SEASONINGS.any { name.contains(it) }) return null   // 调料不算钱

        // 「植物油 2公斤（实耗150克）」按实耗算
        val actual = Regex("(?:实耗|约耗|耗)约?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(克|g)")
            .find(text)
        if (actual != null) {
            val grams = actual.groupValues[1].toDoubleOrNull() ?: 0.0
            return name to grams
        }

        val qty = Regex(
            "([0-9]+(?:\\.[0-9]+)?)\\s*(克|g|G|千克|公斤|斤|两|个|只|条|块|根|片|把|碗|勺|颗|枚|杯|瓣|段|张|毫升|ml)"
        ).find(text)
        val cn = Regex("(半|一|二|两|三|四|五|六|七|八|九)\\s*(斤|两|个|只|条|块|根|片|把|碗|勺|颗|枚)")
            .find(text)

        val n: Double
        val unit: String
        when {
            qty != null -> {
                n = qty.groupValues[1].toDoubleOrNull() ?: return name to defaultGram(name)
                unit = qty.groupValues[2]
            }
            cn != null -> {
                n = CN_NUM[cn.groupValues[1]] ?: return name to defaultGram(name)
                unit = cn.groupValues[2]
            }
            else -> return name to defaultGram(name)
        }

        val grams = when (unit) {
            "克", "g", "G", "毫升", "ml" -> n
            "千克", "公斤" -> n * 1000
            "斤" -> n * 500
            "两" -> n * 50
            "个", "颗", "枚" -> n * pieceGram(name)
            "只", "条" -> n * pieceWeight(name)
            "块", "段", "张", "片", "瓣" -> n * 120
            "根", "把" -> n * 100
            "碗", "杯" -> n * 200
            "勺" -> n * 15
            else -> n
        }
        return name to grams
    }

    /** 不带量词时的常规单份用量 */
    private fun defaultGram(name: String): Double = when {
        name in PER_PIECE -> pieceGram(name)
        name.any { it in "鱼肉虾蟹鸡鸭牛羊猪" } -> 200.0
        else -> 150.0
    }

    private fun pieceGram(name: String): Double = when (name) {
        "鸡蛋", "鸭蛋", "皮蛋", "咸蛋", "茶叶蛋" -> 55.0
        "鲍鱼" -> 60.0
        "海参" -> 80.0
        "馒头" -> 100.0
        "包子" -> 120.0
        "饺子", "馄饨", "烧麦" -> 25.0
        "油条" -> 80.0
        "粽子" -> 150.0
        else -> 60.0
    }

    private fun pieceWeight(name: String): Double {
        PIECE_WEIGHT.keys.firstOrNull { name.contains(it) }?.let { return PIECE_WEIGHT.getValue(it) }
        return when {
            name.contains("鸡") -> 1200.0
            name.contains("鸭") -> 1500.0
            name.contains("鱼") -> 450.0
            else -> 450.0
        }
    }

    /** 具体肉类的首字集合。注意不能把「肉」放进来，否则「肉片」「肉串」也会被当成具体词 */
    private const val MEAT_HEAD = "牛羊猪鸡鸭鱼虾蟹鹅"

    /** 调试用：把一条食材怎么被解析、按哪个单价算钱全打出来 */
    fun debug(raw: String): String {
        val pr = parse(raw) ?: return "$raw -> 解析失败(空或调料)"
        val keys = UNIT.keys.filter { pr.first.contains(it) }
        val picked = keys.maxByOrNull { score(it) }
        return "$raw -> name=${pr.first} grams=${pr.second} 候选=$keys 选中=$picked 单价=${picked?.let { UNIT[it] }} 成本=${cost(pr.first, pr.second)}"
    }

    /** 词条优先级：长度优先，同长时具体肉类（羊/牛/虾…）压过泛词（肉片/肉串/肉末） */
    private fun score(key: String): Int {
        val specific = if (key.first() in MEAT_HEAD) 1 else 0
        return key.length * 10 + specific
    }

    /** 单样食材成本。长词优先，同长时具体肉类优先 —— 否则「羊肉串」会被泛词「肉串」按 20 元/斤算掉 */
    private fun cost(name: String, grams: Double): Double {
        PER_PIECE.entries.filter { name.contains(it.key) }.maxByOrNull { it.key.length }?.let { (k, p) ->
            return p * (grams / pieceGram(k)).coerceAtLeast(0.5)
        }
        val keys = UNIT.keys.filter { name.contains(it) }
        val key = keys.maxByOrNull { score(it) }
        val perJin = key?.let { UNIT[it] } ?: 8.0
        return perJin * (grams / 500.0)
    }

    /** 没有食材数据时按菜名猜 */
    private fun guessByName(name: String): Double {
        var cleaned = name
        for (w in FAKE_WORDS) cleaned = cleaned.replace(w, "")
        if (cleaned.isBlank()) cleaned = name
        if (PREMIUM.any { cleaned.contains(it) }) return 30.0
        if (PRICEY_HINT.any { cleaned.contains(it) }) return 16.0
        CHEAP_COST.keys.filter { cleaned.contains(it) }.maxByOrNull { it.length }
            ?.let { return CHEAP_COST.getValue(it) }
        return 10.0
    }
}
