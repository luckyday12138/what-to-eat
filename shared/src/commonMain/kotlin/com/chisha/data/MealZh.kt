package com.chisha.data

/**
 * 在线菜谱的中文本地化：
 * TheMealDB 这类公开库只给英文，这里用菜名/地区/品类/食材词表做本地映射，
 * 常见组合能拼出中文菜名；拼不出来时保留英文原文，不硬造。
 */
object MealZh {

    private val dishDict = mapOf(
        "Chicken Fried Rice" to "鸡肉炒饭",
        "Chicken Couscous" to "鸡肉库斯库斯",
        "Beef Wellington" to "惠灵顿牛排",
        "Teriyaki Chicken Casserole" to "照烧鸡肉焗饭",
        "Spicy Arrabiata Penne" to "辣味阿拉比亚通心粉",
        "Big Mac" to "巨无霸汉堡",
        "Lasagne" to "意式千层面",
        "Risotto" to "意式烩饭",
        "Paella" to "西班牙海鲜饭",
        "Beef Stroganoff" to "俄式奶油牛肉",
        "Chicken Alfredo" to "奶油鸡肉意面",
        "Pad Thai" to "泰式炒河粉",
        "Fish and Chips" to "炸鱼薯条",
        "Shepherd's Pie" to "牧羊人派",
        "Cottage Pie" to "农舍派",
        "Shepherds Pie" to "牧羊人派",
        "Beef Dumpling Stew" to "牛肉饺子炖汤",
        "Chicken Karaage" to "日式炸鸡块",
        "Tonkatsu" to "炸猪排",
        "Miso Soup" to "味噌汤",
        "Bibimbap" to "石锅拌饭",
        "Kimchi" to "韩式泡菜",
        "Sushi" to "寿司",
        "Tempura" to "天妇罗",
        "Mapo Tofu" to "麻婆豆腐",
        "Kung Pao Chicken" to "宫保鸡丁",
        "Sweet and Sour Pork" to "糖醋里脊",
        "Sweet and Sour Chicken" to "糖醋鸡肉",
        "Sweet and Sour Fish" to "糖醋鱼",
        "Peking Duck" to "北京烤鸭",
        "Dim Sum" to "点心",
        "Wonton" to "云吞",
        "Hot and Sour Soup" to "酸辣汤",
        "Egg Drop Soup" to "蛋花汤",
        "Fried Rice" to "炒饭",
        "Spring Rolls" to "春卷",
        "Chicken Noodle Soup" to "鸡肉面汤",
        "Tomato Soup" to "番茄汤",
        "Pumpkin Soup" to "南瓜汤",
        "Mushroom Soup" to "蘑菇汤",
        "Caesar Salad" to "凯撒沙拉",
        "Greek Salad" to "希腊沙拉",
        "Chicken Salad" to "鸡肉沙拉",
        "Chocolate Cake" to "巧克力蛋糕",
        "Carrot Cake" to "胡萝卜蛋糕",
        "Cheesecake" to "芝士蛋糕",
        "Apple Pie" to "苹果派",
        "Lemon Tart" to "柠檬挞",
        "Banana Pancakes" to "香蕉松饼",
        "Pancakes" to "松饼",
        "Brownies" to "布朗尼",
        "Ice Cream" to "冰淇淋",
        "Rice Pudding" to "米布丁",
        "Bread Pudding" to "面包布丁",
        "French Toast" to "法式吐司",
        "Scrambled Eggs" to "炒蛋",
        "Omlette" to "煎蛋卷",
        "Omelette" to "煎蛋卷",
        "Spaghetti Bolognese" to "番茄肉酱意面",
        "Mac and Cheese" to "芝士通心粉",
        "Meatloaf" to "肉卷",
        "Chicken Wings" to "烤鸡翅",
        "Roast Chicken" to "烤鸡",
        "Roast Beef" to "烤牛肉",
        "Beef Brisket" to "牛腩",
        "Beef Stew" to "炖牛肉",
        "Lamb Chops" to "羊排",
        "Pork Belly" to "五花肉",
        "Rabbit Stew" to "炖兔肉",
        "Grilled Salmon" to "烤三文鱼",
        "Salmon En Croute" to "酥皮三文鱼",
        "Prawn Curry" to "咖喱虾",
        "Fish Pie" to "鱼肉派",
        "Tuna Salad" to "金枪鱼沙拉",
        "Vegetable Soup" to "蔬菜汤",
        "Vegetarian Chili" to "素食辣豆汤",
        "Nachos" to "墨西哥玉米片",
        "Tacos" to "墨西哥卷饼",
        "Burrito" to "墨西哥卷",
        "Guacamole" to "牛油果酱",
        "Chicken Enchilada" to "墨西哥鸡肉卷",
        "Hummus" to "鹰嘴豆泥",
        "Falafel" to "炸鹰嘴豆丸",
        "Tagine" to "摩洛哥塔吉锅",
        "Kebab" to "烤肉串",
        "Naan Bread" to "印度馕",
        "Butter Chicken" to "黄油鸡",
        "Chicken Tikka Masala" to "香料鸡肉咖喱",
        "Biryani" to "印度香料饭",
        "Dal" to "印度豆汤",
        "Samosa" to "印度咖喱角",
        "Pho" to "越南河粉",
        "Banh Mi" to "越南法棍三明治",
        "Curry" to "咖喱",
        "Porridge" to "粥",
        "Congee" to "白粥",
        "Dumplings" to "饺子",
        "Baozi" to "包子",
        "Char Siu" to "叉烧",
        "Claypot Rice" to "煲仔饭",
        "Hainanese Chicken Rice" to "海南鸡饭",
        "Laksa" to "叻沙",
        "Nasi Goreng" to "印尼炒饭",
        "Satay" to "沙爹串",
        "Tom Yum" to "冬阴功汤",
        "Green Curry" to "泰式青咖喱",
        "Massaman Curry" to "玛莎曼咖喱",
        "Pastel de Choclo" to "智利玉米派",
        "Lancashire hotpot" to "兰开夏砂锅炖",
        "Toad in the Hole" to "面糊烤香肠",
        "Bubble and Squeak" to "煎土豆卷心菜",
        "Cornish pasty" to "康沃尔肉馅饼",
        "Eton Mess" to "奶油蛋白甜点",
        "Sticky Toffee Pudding" to "太妃糖布丁",
        "Spotted Dick" to "葡萄干布丁",
        "Steak and Kidney Pie" to "牛排腰子派",
        "Jamon & wild garlic croquetas" to "火腿野蒜炸丸子",
    )

    private val wordMap = mapOf(
        // 主料与肉食
        "chicken" to "鸡肉", "beef" to "牛肉", "pork" to "猪肉", "lamb" to "羊肉",
        "mutton" to "羊肉", "duck" to "鸭肉", "turkey" to "火鸡", "rabbit" to "兔肉",
        "goat" to "山羊肉", "venison" to "鹿肉", "bacon" to "培根", "ham" to "火腿",
        "sausage" to "香肠", "meat" to "肉", "meatloaf" to "肉卷", "ribs" to "排骨",
        "steak" to "牛排", "brisket" to "牛腩", "chorizo" to "辣香肠",
        // 水产
        "fish" to "鱼", "salmon" to "三文鱼", "tuna" to "金枪鱼", "cod" to "鳕鱼",
        "prawn" to "虾", "prawns" to "虾", "shrimp" to "虾", "crab" to "蟹",
        "lobster" to "龙虾", "squid" to "鱿鱼", "octopus" to "章鱼", "clam" to "蛤蜊",
        "mussels" to "贻贝", "oyster" to "牡蛎", "sardines" to "沙丁鱼", "anchovy" to "鳀鱼",
        // 主食与谷物
        "rice" to "饭", "noodle" to "面", "noodles" to "面", "pasta" to "意面",
        "spaghetti" to "意面", "penne" to "通心粉", "macaroni" to "通心粉", "lasagne" to "千层面",
        "bread" to "面包", "toast" to "吐司", "bun" to "包", "flour" to "面粉",
        "oats" to "燕麦", "quinoa" to "藜麦", "couscous" to "库斯库斯", "barley" to "大麦",
        "polenta" to "玉米糊", "tortilla" to "薄饼", "pita" to "薄饼",
        // 蔬菜与配料
        "potato" to "土豆", "potatoes" to "土豆", "tomato" to "番茄", "tomatoes" to "番茄",
        "onion" to "洋葱", "garlic" to "大蒜", "ginger" to "姜", "carrot" to "胡萝卜",
        "cabbage" to "卷心菜", "spinach" to "菠菜", "broccoli" to "西兰花", "cauliflower" to "花椰菜",
        "mushroom" to "蘑菇", "mushrooms" to "蘑菇", "pepper" to "椒", "peppers" to "椒",
        "chilli" to "辣椒", "chili" to "辣椒", "chillies" to "辣椒", "egg" to "蛋",
        "eggs" to "蛋", "tofu" to "豆腐", "bean" to "豆", "beans" to "豆", "lentil" to "扁豆",
        "peas" to "豌豆", "corn" to "玉米", "pumpkin" to "南瓜", "squash" to "南瓜",
        "aubergine" to "茄子", "eggplant" to "茄子", "cucumber" to "黄瓜", "lettuce" to "生菜",
        "avocado" to "牛油果", "olive" to "橄榄", "olives" to "橄榄", "chickpea" to "鹰嘴豆",
        "celery" to "芹菜", "leek" to "韭葱", "radish" to "萝卜", "beetroot" to "甜菜根",
        // 调味与乳制品
        "cheese" to "芝士", "cream" to "奶油", "butter" to "黄油", "milk" to "牛奶",
        "yogurt" to "酸奶", "yoghurt" to "酸奶", "honey" to "蜂蜜", "sugar" to "糖",
        "salt" to "盐", "sauce" to "酱", "soy" to "酱油", "vinegar" to "醋",
        "oil" to "油", "lemon" to "柠檬", "lime" to "青柠", "orange" to "橙",
        "apple" to "苹果", "banana" to "香蕉", "coconut" to "椰子", "chocolate" to "巧克力",
        "vanilla" to "香草", "almond" to "杏仁", "walnuts" to "核桃", "peanut" to "花生",
        "sesame" to "芝麻", "cinnamon" to "肉桂", "curry" to "咖喱", "stock" to "高汤",
        // 做法动词
        "fried" to "炒", "grilled" to "烤", "roast" to "烤", "roasted" to "烤",
        "baked" to "烤", "steamed" to "蒸", "boiled" to "煮", "braised" to "红烧",
        "stuffed" to "酿", "smoked" to "烟熏", "spicy" to "辣味", "sweet" to "甜",
        "sour" to "酸", "crispy" to "脆皮", "creamy" to "奶油", "mashed" to "泥",
        "pie" to "派", "soup" to "汤", "salad" to "沙拉", "cake" to "蛋糕",
        "pudding" to "布丁", "stew" to "炖菜", "roll" to "卷", "rolls" to "卷",
        "ball" to "丸", "balls" to "丸", "wrap" to "卷饼", "sandwich" to "三明治",
        "burger" to "汉堡", "pizza" to "披萨", "sushi" to "寿司", "tempura" to "天妇罗",
        "ramen" to "拉面", "udon" to "乌冬", "soba" to "荞麦面", "casserole" to "焗饭",
        "risotto" to "烩饭", "paella" to "海鲜饭", "tart" to "挞", "muffin" to "松饼",
        "cookie" to "曲奇", "cookies" to "曲奇", "biscuit" to "饼干", "jam" to "果酱",
        "tea" to "茶", "coffee" to "咖啡", "juice" to "果汁", "milkshake" to "奶昔",
        "smoothie" to "冰沙", "strudel" to "果卷", "kebab" to "烤肉串", "tagine" to "塔吉锅",
        "satay" to "沙爹", "bibimbap" to "石锅拌饭", "kimchi" to "泡菜", "wonton" to "云吞",
        "dumpling" to "饺子", "dumplings" to "饺子", "loaf" to "条", "balls" to "丸子",
    )

    private val areaMap = mapOf(
        "Chinese" to "中式", "Italian" to "意式", "Japanese" to "日式", "Thai" to "泰式",
        "Indian" to "印度", "Mexican" to "墨西哥", "French" to "法式", "American" to "美式",
        "British" to "英式", "Greek" to "希腊", "Spanish" to "西班牙", "Turkish" to "土耳其",
        "Moroccan" to "摩洛哥", "Egyptian" to "埃及", "Vietnamese" to "越南", "Filipino" to "菲律宾",
        "Ukrainian" to "乌克兰", "Polish" to "波兰", "Dutch" to "荷兰", "Portuguese" to "葡萄牙",
        "Russian" to "俄式", "Irish" to "爱尔兰", "Jamaican" to "牙买加", "Kenyan" to "肯尼亚",
        "Malaysian" to "马来", "Croatian" to "克罗地亚", "Canadian" to "加拿大", "Tunisian" to "突尼斯",
        "Uruguayan" to "乌拉圭", "Australian" to "澳洲", "Norwegian" to "挪威", "Swedish" to "瑞典",
        "Danish" to "丹麦", "German" to "德式", "Korean" to "韩式", "Singaporean" to "新加坡",
        "Indonesian" to "印尼",
        "United States" to "美国", "USA" to "美国", "United Kingdom" to "英国",
        "China" to "中国", "Ireland" to "爱尔兰", "France" to "法国", "Italy" to "意大利",
        "Japan" to "日本", "Thailand" to "泰国", "India" to "印度", "Greece" to "希腊",
        "Turkey" to "土耳其", "Egypt" to "埃及", "Morocco" to "摩洛哥", "Vietnam" to "越南",
        "Malaysia" to "马来", "Philippines" to "菲律宾", "Croatia" to "克罗地亚",
        "Poland" to "波兰", "Portugal" to "葡萄牙", "Russia" to "俄式",
        "Netherlands" to "荷兰", "Jamaica" to "牙买加", "Kenya" to "肯尼亚",
        "Tunisia" to "突尼斯", "Uruguay" to "乌拉圭", "Norway" to "挪威",
        "Ukraine" to "乌克兰", "Spain" to "西班牙", "Germany" to "德国",
        "Argentina" to "阿根廷", "Brazil" to "巴西", "Mexico" to "墨西哥",
        "Canada" to "加拿大", "Australia" to "澳洲", "Sweden" to "瑞典",
        "Denmark" to "丹麦", "Ireland" to "爱尔兰", "Scotland" to "苏格兰",
        "Wales" to "威尔士",
    )

    private val categoryMap = mapOf(
        "Chicken" to "鸡肉", "Beef" to "牛肉", "Pork" to "猪肉", "Seafood" to "海鲜",
        "Vegetarian" to "素食", "Dessert" to "甜点", "Side" to "配菜", "Starter" to "前菜",
        "Breakfast" to "早餐", "Lamb" to "羊肉", "Pasta" to "面食", "Miscellaneous" to "综合",
        "Vegan" to "纯素",
    )

    private val stopWords = setOf(
        "with", "and", "of", "in", "on", "for", "to", "the", "a", "an",
        "de", "la", "le", "au", "style", "homemade", "classic", "easy",
        "minute", "minutes", "quick", "simple", "best", "perfect", "my",
        "our", "ultimate", "healthy", "favourite", "favorite",
    )

    /** 词表查不到时按单复数互查一次，英文菜名复数很常见 */
    private fun lookup(token: String): String? {
        val lower = token.lowercase()
        wordMap[lower]?.let { return it }
        adjMap[lower]?.let { return it }
        extraMap[lower]?.let { return it }

        val singular = when {
            lower.endsWith("ies") && lower.length > 4 -> lower.dropLast(3) + "y"
            lower.endsWith("es") && lower.length > 3 -> lower.dropLast(2)
            lower.endsWith("s") && lower.length > 3 -> lower.dropLast(1)
            else -> null
        } ?: return null

        return wordMap[singular] ?: extraMap[singular] ?: adjMap[singular]
    }

    private val adjMap = mapOf(
        "spanish" to "西班牙", "french" to "法式", "italian" to "意式",
        "japanese" to "日式", "thai" to "泰式", "chinese" to "中式",
        "korean" to "韩式", "mexican" to "墨西哥", "indian" to "印度",
        "greek" to "希腊", "turkish" to "土耳其", "moroccan" to "摩洛哥",
        "vietnamese" to "越南", "american" to "美式", "british" to "英式",
        "german" to "德式", "russian" to "俄式", "polish" to "波兰",
        "portuguese" to "葡萄牙", "lebanese" to "黎巴嫩", "persian" to "波斯",
        "caribbean" to "加勒比", "cajun" to "卡真", "creole" to "克里奥尔",
        "hawaiian" to "夏威夷", "singapore" to "新加坡", "singaporean" to "新加坡",
        "indonesian" to "印尼", "malaysian" to "马来", "filipino" to "菲律宾",
        "turkish" to "土耳其", "egyptian" to "埃及", "kenyan" to "肯尼亚",
        "nigerian" to "尼日利亚", "brazilian" to "巴西", "peruvian" to "秘鲁",
    )

    private val extraMap = mapOf(
        "puff" to "酥皮", "pastry" to "酥皮点心", "shortbread" to "酥饼",
        "crumble" to "酥粒甜点", "scone" to "司康", "scones" to "司康",
        "trifle" to "奶油果冻杯", "brownie" to "布朗尼", "brownies" to "布朗尼",
        "flapjack" to "燕麦饼", "moussaka" to "茄子肉酱千层", "souvlaki" to "希腊烤肉串",
        "katsu" to "日式炸排", "gyoza" to "煎饺", "okonomiyaki" to "大阪烧",
        "takoyaki" to "章鱼小丸子", "chow" to "炒", "mein" to "面",
        "lomo" to "里脊", "saltimbocca" to "盐煎小牛肉", "carpaccio" to "生薄片",
        "tiramisu" to "提拉米苏", "pavlova" to "帕夫洛娃", "schnitzel" to "炸肉排",
        "gnocchi" to "意式土豆丸", "tamale" to "玉米粽", "empanada" to "肉馅饼",
        "ceviche" to "柠檬腌鱼", "gazpacho" to "西班牙冷汤", "fabada" to "西班牙豆炖肉",
        "jambalaya" to "什锦饭", "gumbo" to "秋葵浓汤", "poutine" to "肉酱芝士薯条",
        "borscht" to "罗宋汤", "stroganoff" to "奶油蘑菇牛肉", "shawarma" to "烤肉卷",
        "tabbouleh" to "香草沙拉", "baklava" to "果仁蜜饼", "nigiri" to "握寿司",
        "donburi" to "盖饭", "jamon" to "火腿", "chorizo" to "辣香肠",
        "nutella" to "巧克力榛子酱", "pesto" to "青酱", "salsa" to "辣酱",
        "mayonnaise" to "蛋黄酱", "mustard" to "芥末", "ketchup" to "番茄酱",
        "bin" to "", "jar" to "", "packet" to "",
        "halloumi" to "哈罗米芝士", "feta" to "菲达", "mozzarella" to "马苏里拉",
        "cheddar" to "切达芝士", "parmesan" to "帕玛森", "brie" to "布里芝士",
        "ricotta" to "里科塔", "hummus" to "鹰嘴豆泥", "guacamole" to "牛油果酱",
        "tofu" to "豆腐", "seitan" to "面筋", "tempeh" to "天贝",
        "sriracha" to "是拉差辣酱", "tabasco" to "辣酱", "worcestershire" to "伍斯特酱",
        "balsamic" to "黑醋", "saffron" to "藏红花", "paprika" to "红椒粉",
        "turmeric" to "姜黄", "cumin" to "孜然", "coriander" to "香菜",
        "basil" to "罗勒", "parsley" to "欧芹", "thyme" to "百里香",
        "rosemary" to "迷迭香", "oregano" to "牛至", "mint" to "薄荷",
        "cilantro" to "香菜", "chives" to "细香葱", "shallot" to "红葱头",
        "scallion" to "小葱", "spring" to "春", "onions" to "洋葱",
        "hotpot" to "砂锅炖", "latke" to "薯饼", "latkes" to "薯饼",
        "croqueta" to "炸丸子", "croquetas" to "炸丸子", "kidney" to "腰子",
        "wild" to "野", "choclo" to "玉米", "cocoy" to "香蕉",
        "sticky" to "黏甜", "toffee" to "太妃糖", "pasty" to "肉馅饼",
        "squeak" to "卷心菜", "mess" to "甜点杯", "spotted" to "葡萄干",
        "shakshuka" to "北非蛋", "fajita" to "墨西哥烤肉", "mac" to "通心粉",
        "strawberry" to "草莓", "rhubarb" to "大黄", "mandi" to "焖饭",
        "pepitoria" to "酱汁", "pollo" to "鸡肉", "corba" to "扁豆汤",
        "frangipan" to "杏仁奶油", "frangipane" to "杏仁奶油",
        "burrata" to "布拉塔芝士", "gorgonzola" to "蓝纹芝士",
        "pancetta" to "意式培根", "prosciutto" to "生火腿", "salami" to "萨拉米",
        "avocado" to "牛油果", "pomegranate" to "石榴", "fig" to "无花果",
        "date" to "椰枣", "raisin" to "葡萄干", "raisins" to "葡萄干",
        "peach" to "桃子", "pear" to "梨", "plum" to "李子", "cherry" to "樱桃",
        "blueberry" to "蓝莓", "raspberry" to "覆盆子", "mango" to "芒果",
        "pineapple" to "菠萝", "papaya" to "木瓜", "grape" to "葡萄",
    )

    /** 菜名：先查完整词典，再按词拼中文；拼不出就保留英文，不硬造 */
    fun name(en: String): String {
        val key = en.trim()
        if (key.isBlank()) return key
        dishDict[key]?.let { return it }

        val tokens = key.split(Regex("[^A-Za-z']+"))
            .filter { it.isNotBlank() && it.lowercase() !in stopWords }
        if (tokens.isEmpty()) return key

        val mapped = tokens.map { lookup(it) }
        val hit = mapped.count { it != null }
        if (hit == 0) return key
        // 命中不到一半时，拼出来会残缺，宁可保留英文原文
        if (hit * 2 < tokens.size) return key

        val zhParts = mapped.filterNotNull()
        // 相邻重复词合并，避免「菲达芝士芝士」这种堆叠
        val dedup = zhParts.filterIndexed { i, s -> i == 0 || s != zhParts[i - 1] }
        val zh = dedup.joinToString("")
        return zh.ifBlank { key }
    }

    fun area(en: String): String = areaMap[en.trim()] ?: en.trim()

    fun category(en: String): String = categoryMap[en.trim()] ?: en.trim()

    private fun hasHan(text: String): Boolean =
        text.any { ch -> ch.code in 0x4E00..0x9FFF }

    /**
     * 展示名：世界菜谱名不可能全被词表覆盖，翻不出来时不再硬拼，
     * 改用「地区风味 + 品类」生成中文名，保证界面永远是中文；
     * 英文原名留在副标题里做对照。
     */
    fun displayName(rawName: String, rawArea: String, rawCategory: String): String {
        val zh = name(rawName)
        if (hasHan(zh)) return zh

        val a = area(rawArea).takeIf { hasHan(it) }
        val c = category(rawCategory).takeIf { hasHan(it) }

        return when {
            a != null && c != null -> "${a}风味$c"
            a != null -> "${a}风味菜"
            // 连产地都拿不到就编不出像样的名字了，宁可丢掉，也别推「配菜 · 网络菜谱」这种假名
            else -> ""
        }
    }

    /** 给在线菜谱配一道中文配菜时段：西式偏午晚，甜点/饮品偏夜宵 */
    fun suggestSlots(category: String): Set<Slot> = when (category.lowercase()) {
        "breakfast" -> setOf(Slot.BREAKFAST)
        "dessert", "side" -> setOf(Slot.MIDNIGHT, Slot.DINNER)
        else -> setOf(Slot.LUNCH, Slot.DINNER)
    }
}
