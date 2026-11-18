package com.chisha.data

import com.chisha.core.PlatformStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** 一条网络菜谱 */
@Serializable
data class NetMeal(
    val name: String,
    val category: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val tip: String = "",
    val feature: String = "",
    /** 这条来自哪个源（离线看缓存时也能显示） */
    val source: String = "",
    /** 按菜名抓到的实拍图，空表示没抓到、界面回落品类图 */
    val photo: String = "",
    /** 适配的餐段（BREAKFAST/LUNCH/DINNER/MIDNIGHT）。接口不给餐段，按菜名推出来 */
    val slots: List<String> = emptyList(),
)

/** 菜品关键词 → 餐段。网络接口一律不返回餐段，只能按菜名和食材推；
 *  推不准时宁可宽一点（午晚都算），别让早餐只能吃粥。 */
internal object SlotGuess {
    private val BREAKFAST = listOf(
        "粥", "馒头", "包子", "小笼", "烧麦", "油条", "豆浆", "煎饼", "肠粉", "吐司",
        "三明治", "燕麦", "水饺", "馄饨", "面条", "拌面", "米线", "米粉", "汤圆", "花卷",
    )
    private val MIDNIGHT = listOf(
        "烧烤", "烤串", "肉串", "小龙虾", "麻辣烫", "火锅", "炸鸡", "卤味", "毛豆",
        "花生", "烤鱼", "串",
    )
    private val HEAVY = listOf(
        "火锅", "烤鱼", "牛排", "肘子", "扣肉", "红烧", "焖", "炖", "水煮", "干锅",
    )

    fun of(name: String, category: String = "", ingredients: List<String> = emptyList()): List<String> {
        val text = name + category + ingredients.take(3).joinToString("")
        val breakfast = BREAKFAST.any { text.contains(it) }
        val midnight = MIDNIGHT.any { text.contains(it) }
        val heavy = HEAVY.any { text.contains(it) }
        return when {
            // 烧烤串类只当夜宵（早餐不会吃烤串，午餐也少见）
            midnight -> listOf("MIDNIGHT", "DINNER")
            breakfast -> if (heavy) listOf("LUNCH", "DINNER") else listOf("BREAKFAST", "LUNCH")
            heavy -> listOf("LUNCH", "DINNER")
            // 剩下的大多数是家常炒菜、汤：午晚都合适
            else -> listOf("LUNCH", "DINNER")
        }
    }
}

@Serializable
private data class NetCache(
    val items: List<NetMeal> = emptyList(),
    val at: Long = 0L,
    /** 已经推过的菜名。只增不减，靠它保证「换一道」永远是没上过的新菜 */
    val seen: List<String> = emptyList(),
)

/** 网络菜转成转盘能用的 Dish：不落库，只给首页「推荐」模式用 */
fun NetMeal.toDish(slot: Slot): Dish = Dish(
    id = "net:$name",
    name = name,
    slots = slots.mapNotNull { runCatching { Slot.valueOf(it) }.getOrNull() }.toSet()
        .ifEmpty { setOf(slot) },
    cuisine = category.ifBlank { "网络菜谱" },
    tags = listOfNotNull(category.takeIf { it.isNotBlank() }),
    imageKey = ImageKey.derive(name, category, emptyList()),
    imageUri = photo.ifBlank { DishPhoto.cached(name) },
    // 价格按食材清单和用量估，接口本身不返回价格
    priceLevel = PriceTag.level(name, ingredients),
    origin = Origin.NETWORK,
    heat = 30,
    steps = steps,
    ingredients = ingredients,
    note = feature,
)

/**
 * 在线灵感：多源调度（见 [SourceHub]）+ 内存池 + 磁盘缓存。
 *
 * 提速靠三件事：
 * 1. 一次请求拿一批进池子，「换一道」从池里取，零等待；
 * 2. 池子落盘，冷启动和断网都有内容；
 * 3. 进页面预热，点击时不用等网络。
 */
object OnlineSource {

    private const val CACHE_FILE = "netcache.json"
    /** 池子只是「预取缓冲」，不设推菜上限；见池到这份上就丢最老的 */
    private const val MAX_POOL = 600
    private const val POOL_TARGET = 30
    private const val PER_CATEGORY = 40

    /** 一次关键词结果最多收几条；接口一页给 20 条，多收几条池子才长得快 */
    private const val PER_BATCH = 12

    /** 最近出过多少道参与「别连着上同一主料」的压制（不影响去重，去重是永久的） */
    private const val RECENT_KEEP = 40

    /** 点一次换菜，最多为找图等这么久；超时先出菜，图后台继续补 */
    private const val PHOTO_BUDGET = 900L

    /** batch() 首屏最多为「补货」等这么久，到点就先用池里现有的 */
    private const val WARM_BUDGET = 1_600L

    /** batch() 首屏最多为「取图」等这么久，剩下的图后台补 */
    private const val PHOTO_WAIT = 700L

    /** 池子空时「换一道」最多等网络这么久，超时就用本地/缓存兜底 */
    private const val TAKE_BUDGET = 4_000L

    /** 具体菜名 / 食材：主源 qqsuu 对这类词更准 */
    private val specific = listOf(
        "红烧肉", "排骨", "鸡肉", "鲈鱼", "豆腐", "土豆", "茄子", "白菜", "面条", "米饭",
        "粥", "鸡蛋", "虾", "牛肉", "羊肉", "鸭肉", "蘑菇", "番茄", "冬瓜", "莲藕",
        "豆角", "花菜", "南瓜", "山药", "米粉", "饺子", "包子", "甜品", "海带", "辣椒",
        "黄瓜", "玉米", "菠菜", "苦瓜", "芋头", "木耳", "牛肉面", "鸡蛋羹", "酸菜鱼",
        "回锅肉", "糖醋里脊", "可乐鸡翅", "番茄鸡蛋", "蒜苗", "豆芽", "丝瓜", "口蘑",
        "芹菜", "韭菜", "洋葱", "平菇", "金针菇", "粉丝", "年糕", "米线", "炒饭",
        "拌面", "馄饨", "烧麦", "油条", "豆浆", "凉皮", "卤肉", "腊肉", "香肠",
        "鱿鱼", "带鱼", "黄鱼", "蛤蜊", "扇贝", "鸡肉卷", "猪蹄", "排骨汤",
    )

    /** 宽泛词：大食材 / 品类词。清和、大米两个国内源对这类词一次能给 15~20 条，
     *  菜系词（川菜/家常菜）和餐段词（早餐）在接口上是空的，别浪费请求 */
    private val broad = listOf(
        "肉", "鱼", "虾", "鸡", "鸭", "牛", "羊", "蛋", "豆腐", "面", "饭", "汤",
        "排骨", "火锅", "饺子", "包子", "粥", "饼", "土豆", "茄子", "豆角", "白菜",
        "蘑菇", "木耳", "海带", "番茄", "冬瓜", "莲藕", "山药", "南瓜", "黄瓜",
        "玉米", "菠菜", "苦瓜", "芋头", "韭菜", "芹菜", "洋葱", "豆芽", "粉丝",
        "年糕", "米线", "馄饨", "扇贝", "鱿鱼", "带鱼", "花菜", "丝瓜", "豆皮",
    )

    /** 做法 × 食材现场拼关键词：单靠固定词池，转几轮就绕回同一批菜了 */
    private val methods = listOf(
        "红烧", "清蒸", "爆炒", "干锅", "凉拌", "酸辣", "香辣", "椒盐", "蒜蓉",
        "糖醋", "砂锅", "白灼", "小炒", "炖", "焖", "煎", "烤", "卤", "炝炒", "姜葱",
    )

    private val comboTargets = listOf(
        "鱼", "虾", "鸡", "鸭", "牛肉", "羊肉", "猪肉", "排骨", "豆腐", "茄子",
        "土豆", "豆角", "白菜", "蛋", "蘑菇", "木耳", "海带", "冬瓜", "莲藕",
        "山药", "南瓜", "黄瓜", "玉米", "菠菜", "苦瓜", "芋头", "韭菜", "芹菜",
        "洋葱", "豆芽", "粉丝", "年糕", "米线", "花甲", "鱿鱼", "带鱼", "花菜",
        "丝瓜", "豆皮", "鸡翅", "猪蹄", "牛腩", "肥肠", "腰花", "面", "饭",
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** 后台干活用的作用域：补货、存盘、备图都丢这里，别让点击等它们 */
    private val bg = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var lastPersistAt = 0L

    private val pool: MutableList<NetMeal> = mutableListOf()
    /** 补货单飞标志：同一时刻只允许一路在拉网络 */
    private var refilling = false
    /** 抓图单飞标志 */
    private var prefetching = false

    /** 最近展示过的菜名，只用来压「别连着上同一主料」的频率 */
    private val recent: MutableList<String> = mutableListOf()

    /** 永久去重表：推过的菜都记在这，永不清空。靠它做到「没有上限、永不重样」 */
    private val seen: MutableSet<String> = mutableSetOf()

    /** 本地菜库已有的菜名，网络池里同名的不再推，两边才不重复 */
    private var localNames: Set<String> = emptySet()

    private var loadedCache = false
    private var lastServed = ""

    /** 最近一次取菜用的源，界面据此提示「已自动切换」 */
    var lastSourceLabel: String = ""
        private set
    var lastSwitched: Boolean = false
        private set

    /** 告诉网络池「这些本地已经有了」，同名的网络菜不再出现在在线灵感里 */
    fun excludeLocal(names: Set<String>) {
        ensureCacheLoaded()
        synchronized(pool) {
            localNames = names
            pool.removeAll { it.name in names }
            persist()
        }
    }

    /** 换了数据源，旧池子作废，别拿着上一台的菜充数 */
    fun clearPool() {
        synchronized(pool) {
            pool.clear()
            recent.clear()
            lastServed = ""
            persist()
        }
    }

    /** 取一道：先池子，池子空就网取（多源自动切换），全挂了才用磁盘缓存 */
    suspend fun take(preferKeyword: String? = null): NetMeal? = withContext(Dispatchers.IO) {
        ensureCacheLoaded()

        val fromPool = popFromPool()
        if (fromPool != null) {
            refillInBackground()
            return@withContext fromPool.withPhoto(PHOTO_BUDGET)
        }

        // 池子空了才走这条路：给网络一个上限，别让人对着转圈等 9 秒
        val result = withTimeoutOrNull(TAKE_BUDGET) {
            runCatching { SourceHub.fetch(preferKeyword ?: nextKeyword()) }.getOrNull()
        }
        if (result != null) noteSource(result)
        if (result != null && result.ok) {
            val pick = result.meals.firstOrNull { isNew(it.name) } ?: result.meals.first()
            synchronized(pool) {
                addFiltered(result.meals.filter { it.name != pick.name })
                markServed(pick.name)
                schedulePersist()
            }
            return@withContext pick.withPhoto(PHOTO_BUDGET)
        }

        // 所有源都不可用：拿缓存顶一下，别让页面空着
        synchronized(pool) {
            if (pool.isEmpty()) pool.addAll(readCache().items.take(MAX_POOL))
            if (pool.isNotEmpty()) pool.removeAt(freshIndex()).also { markServed(it.name) } else null
        }
    }

    /** 进页面先预热，让「换一道」不用等。times 控制最多拉几个关键词（每个词一页 20 道） */
    suspend fun warm(times: Int = 8) = withContext(Dispatchers.IO) {
        ensureCacheLoaded()
        // 看还能推出多少道，而不是池子总条数（池里可能全是刚推过的）
        val usable = { synchronized(pool) { pool.count { isNew(it.name) } } }
        if (usable() >= POOL_TARGET) return@withContext

        val used = HashSet<String>()
        repeat(times) {
            if (usable() >= POOL_TARGET) return@withContext
            var kw = nextKeyword()
            var guard = 0
            while (kw in used && guard++ < 8) kw = nextKeyword()
            used.add(kw)
            val result = SourceHub.fetch(kw)
            noteSource(result)
            // 这个词没货就换下一个词，别整批放弃（菜系词在这几个源上都是空的）
            if (!result.ok) return@repeat
            synchronized(pool) {
                addFiltered(result.meals)
                schedulePersist()
            }
            prefetchPhotos()
        }
    }

    /** 给首页「推荐」模式取一批：一次拿走 n 道并记为已展示，和在线灵感不撞菜。
     *  slot 不为空时优先取适配这个餐段的菜——不然早上会给你推火锅。 */
    suspend fun batch(n: Int, slot: Slot? = null): List<NetMeal> = withContext(Dispatchers.IO) {
        ensureCacheLoaded()
        val total = synchronized(pool) { pool.size }
        val fresh = synchronized(pool) { pool.count { isNew(it.name) } }
        // 只有池子真的没货时才值得让首屏等一等（限时 2.2s，源被限流时一次 fetch 要 4~9 秒）
        if (total < n) {
            withTimeoutOrNull(WARM_BUDGET) { runCatching { warmOnce(6) } }
        } else if (fresh < PER_BATCH) {
            // 池里有菜但大多是推过的：先给用户用，新菜后台静默补进来
            bg.launch { runCatching { warmOnce(6) } }
        }

        val taken = synchronized(pool) {
            val out = ArrayList<NetMeal>(n)
            if (slot != null) {
                while (out.size < n) {
                    // 先挑主料跟已取的不重样的，池里就剩同类了再放宽——
                    // 否则池里一堆豆皮，转盘上能连着出来七八道豆皮
                    val picked = out.flatMap { familiesOf(it.name) }.toSet()
                    val fresh = pool.indexOfFirst { isNew(it.name) && slot.name in it.slots }
                    val diverse = if (picked.isEmpty()) -1 else pool.indexOfFirst {
                        isNew(it.name) && slot.name in it.slots && familiesOf(it.name).none { f -> f in picked }
                    }
                    val any = when {
                        diverse >= 0 -> diverse
                        fresh >= 0 -> fresh
                        else -> pool.indexOfFirst { slot.name in it.slots }
                    }
                    if (any < 0) break
                    val m = pool.removeAt(any)
                    markServed(m.name)
                    out.add(m)
                }
            }
            // 池子可能被前面的取菜掏空，取不满就拿现有的，别让 removeAt 越界崩掉。
            // 指定了餐段时不做这种兜底：宁可返回空、让首页回落到本地菜，
            // 也不能早上给人端出一锅火锅。
            if (slot == null) {
                while (out.size < n && pool.isNotEmpty()) {
                    val idx = freshIndex()
                    if (idx !in pool.indices) break
                    val m = pool.removeAt(idx)
                    markServed(m.name)
                    out.add(m)
                }
            }
            if (out.isNotEmpty()) schedulePersist()
            out
        }
        if (taken.isEmpty()) return@withContext emptyList()
        val names = taken.map { it.name }
        // 取图绝不能拖住首屏：12 道菜搜图分 4 批串行，慢的时候能吃掉七八秒。
        // 等一小会儿，拿到的先用，没拿到的后台继续抓（抓到会落缓存，下次遇到就有图）。
        val photos = withTimeoutOrNull(PHOTO_WAIT) {
            runCatching { DishPhoto.resolveAll(names) }.getOrNull()
        }.orEmpty()
        if (photos.size < names.size) {
            bg.launch { runCatching { DishPhoto.resolveAll(names) } }
        }
        taken.map { m -> photos[m.name]?.let { m.copy(photo = it) } ?: m }
    }

    /** 只看不取：给界面展示「已备好的其他灵感」。
     *  exclude 用来挡住当前正展示的那道 —— 否则主卡和小卡会出现同一道菜 */
    fun peek(n: Int = 8, exclude: Set<String> = emptySet()): List<NetMeal> = synchronized(pool) {
        // 只给没推过的菜。以前新菜不足 8 条时会拿老菜凑数，
        // 结果「换一道」刚换掉的菜又出现在下面的小卡里，看着像没换。
        pool.filter { isNew(it.name) && it.name !in exclude }.take(n)
    }

    /** 从池头取一条 —— 和「灵感池」第一条是同一道，界面两边才同步 */
    suspend fun shift(): NetMeal? {
        ensureCacheLoaded()
        val meal = popFromPool()
        if (meal != null) {
            refillInBackground()
            return meal.withPhoto(PHOTO_BUDGET)
        }
        // 池子真见底了才同步等一次网络，其余情况一律点完就出
        warm(2)
        val again = popFromPool() ?: return null
        refillInBackground()
        return again.withPhoto(PHOTO_BUDGET)
    }

    /** 从池里取一条并记账：同步、极快，不碰网络也不写盘 */
    private fun popFromPool(): NetMeal? = synchronized(pool) {
        if (pool.isEmpty()) return@synchronized null
        pool.removeAt(freshIndex()).also {
            markServed(it.name)
            schedulePersist()
        }
    }

    /** 池里新菜不多了就在后台补货，不阻塞这次点击。
     *  单飞：连点「换一道」十几次只会跑一次补货，否则每次点击都起一批网络请求，
     *  几路并发把源打限流，池子反而补不上（表现为越点越空、越点越卡）。*/
    private fun refillInBackground() {
        synchronized(pool) {
            if (refilling) return
            if (pool.count { isNew(it.name) } >= 20) return
            refilling = true
        }
        bg.launch {
            try {
                warm(8)
            } catch (_: Throwable) {
                // 补货失败不影响已经展示的那道
            } finally {
                synchronized(pool) { refilling = false }
            }
        }
    }

    /** 抢到补货权才真去拉网络，抢不到说明已经有一路在拉了，直接返回现有池 */
    private suspend fun warmOnce(times: Int) {
        val mine = synchronized(pool) {
            if (refilling) false else { refilling = true; true }
        }
        if (!mine) return
        try {
            warm(times)
        } finally {
            synchronized(pool) { refilling = false }
        }
    }

    /** 取图最多等这么久；超时先用品类图顶上，图继续在后台抓，下次遇到就有了 */
    private suspend fun NetMeal.withPhoto(budgetMs: Long): NetMeal {
        DishPhoto.cached(name)?.let { return copy(photo = it) }
        if (photo.isNotBlank()) return this
        val url = withTimeoutOrNull(budgetMs) { runCatching { DishPhoto.resolve(name) }.getOrNull() }
        return url?.let { copy(photo = it) } ?: this
    }

    /** 池里还没备好图的菜，后台悄悄抓一批，之后切过去就是秒出图 */
    private fun prefetchPhotos() {
        // 一次补货循环会调好几次这个函数，单飞免得同时开五六路抓图把图源打限流
        if (prefetching) return
        val targets = synchronized(pool) { pool.filter { isNew(it.name) && it.photo.isBlank() }.take(6) }
        if (targets.isEmpty()) return
        prefetching = true
        bg.launch {
            runCatching {
                val photos = DishPhoto.resolveAll(targets.map { it.name })
                if (photos.isEmpty()) return@runCatching
                synchronized(pool) {
                    for (i in pool.indices) {
                        photos[pool[i].name]?.let { url ->
                            if (pool[i].photo.isBlank()) pool[i] = pool[i].copy(photo = url)
                        }
                    }
                }
            }
            prefetching = false
        }
    }

    /** 写盘太频繁会卡：合并成最多两秒一次，而且丢后台 */
    private fun schedulePersist() {
        val now = PlatformStore.nowMillis()
        if (now - lastPersistAt < 2_000L) return
        lastPersistAt = now
        bg.launch { runCatching { persist() } }
    }

    /** 把刚看过的菜退回池尾：池子不会越换越少，而且能在灵感池里看到它 */
    fun putBack(meal: NetMeal) {
        synchronized(pool) {
            if (pool.none { it.name == meal.name }) {
                pool.add(meal)
                while (pool.size > MAX_POOL) pool.removeAt(0)
                schedulePersist()
            }
        }
    }

    /** 点选某一道时把它从池子里取出来 */
    suspend fun takeSpecific(name: String): NetMeal? = withContext(Dispatchers.IO) {
        ensureCacheLoaded()
        val meal = synchronized(pool) {
            val idx = pool.indexOfFirst { it.name == name }
            if (idx >= 0) {
                pool.removeAt(idx).also {
                    markServed(it.name)
                    schedulePersist()
                }
            } else null
        }
        meal?.withPhoto(PHOTO_BUDGET)
    }

    fun readyCount(): Int = synchronized(pool) { pool.size }

    /** 源状态一句话，用于界面小字提示 */
    fun sourceStatus(): String = SourceHub.statusLine()

    fun availableCount(): Int = SourceHub.availableCount()

    fun providerCount(): Int = SourceHub.providerCount()

    // ---------------- 内部实现 ----------------

    private fun noteSource(result: SourceHub.Result) {
        if (result.ok) {
            lastSourceLabel = result.label
            lastSwitched = result.switched
        }
    }

    /** 入池时按品类和主料同时限量，别让一个关键词灌进来 20 道土豆菜 */
    private fun addFiltered(meals: List<NetMeal>) {
        val counts = pool.groupingBy { it.category }.eachCount().toMutableMap()
        val mains = pool.flatMap { familiesOf(it.name) }.groupingBy { it }.eachCount().toMutableMap()
        // 池子越大，单个主料的额度越大；池子小时收紧，免得一眼看去全是土豆
        val mainCap = (pool.size / 12).coerceIn(3, 8)
        val batchMain = mutableMapOf<String, Int>()
        var taken = 0
        for (m in meals) {
            if (pool.size >= MAX_POOL) break
            if (taken >= PER_BATCH) break
            if (m.name in localNames) continue          // 本地菜库已有，别重复推
            if (pool.any { it.name == m.name }) continue
            val c = counts[m.category] ?: 0
            if (c >= PER_CATEGORY) continue
            // 一道菜带几个主料就算几个，任一主料超过额度就跳过
            val fams = familiesOf(m.name)
            if (fams.any { (mains[it] ?: 0) >= mainCap }) continue
            // 同一批里同一个主料最多 2 条：这批剩下的靠换关键词去拿，池子才分散
            if (fams.any { (batchMain[it] ?: 0) >= 2 }) continue
            pool.add(if (m.slots.isEmpty()) m.copy(slots = SlotGuess.of(m.name, m.category, m.ingredients)) else m)
            taken++
            counts[m.category] = c + 1
            for (f in fams) {
                mains[f] = (mains[f] ?: 0) + 1
                batchMain[f] = (batchMain[f] ?: 0) + 1
            }
        }
    }

    /** 池内挑一条：优先没推过的，其次压住主料频率，实在没新菜才让老菜隔很久再来一次 */
    private fun freshIndex(): Int {
        if (pool.size <= 1) return 0

        // 第一优先：永久没推过的菜，同时避开最近几道用过的主料
        // 窗口要看够长（16 道）：只看 8 道的话，隔一道来个丝瓜照样觉得「怎么老是丝瓜」
        val near = recent.takeLast(16).flatMap { familiesOf(it) }.groupingBy { it }.eachCount()
        for (limit in 2..4) {
            val pick = pool.indices.filter { i ->
                val n = pool[i].name
                isNew(n) && familiesOf(n).none { (near[it] ?: 0) >= limit }
            }
            if (pick.isNotEmpty()) return pick.random()
        }
        val step2 = pool.indices.filter { isNew(pool[it].name) && !sameFamily(pool[it].name, lastServed) }
        if (step2.isNotEmpty()) return step2.random()
        val step3 = pool.indices.filter { isNew(pool[it].name) }
        if (step3.isNotEmpty()) return step3.random()

        // 池里确实没有新菜了：退一步用「很久没上过」的老菜（偶尔重复，但不会一直重复）
        val stale = pool.indices.filter { pool[it].name !in recent && pool[it].name !in localNames }
        if (stale.isNotEmpty()) return stale.random()

        return pool.indices.random()
    }

    /** 这条是不是还没推过的新菜 */
    private fun isNew(name: String): Boolean = name !in seen && name !in localNames

    /** 兼容老调用点：以前叫 fresh，语义已升级为「永久没推过」 */
    private fun fresh(name: String): Boolean = isNew(name)

    private fun markServed(name: String) {
        if (name.isBlank()) return
        seen.add(name)
        recent.add(name)
        while (recent.size > RECENT_KEEP) recent.removeAt(0)
        lastServed = name
    }

    /** 主料表：出现哪个就算哪一族。具体词必须排在泛词前面（扇贝先于虾、牛肉先于肉） */
    private val MAINS = listOf(
        "扇贝", "蛤蜊", "虾", "蟹", "鱿鱼", "带鱼", "鱼", "鸡", "鸭", "鹅", "牛", "羊",
        "猪", "排骨", "肉", "蛋", "豆腐", "口蘑", "香菇", "蘑菇", "木耳", "海带",
        // 金针菇这类配菜在源站会被当成模板批量出「XX炖金针菇」，不记进主料就压不住
        "金针菇", "杏鲍菇", "平菇", "茶树菇", "鸡腿菇", "西兰花", "菜花", "莴笋",
        "青椒", "辣椒", "胡萝卜", "萝卜", "蒜", "姜", "葱", "蒜苔", "蒜薹", "空心菜",
        "土豆", "茄子", "豆角", "白菜", "番茄", "冬瓜", "莲藕", "山药", "南瓜",
        "黄瓜", "玉米", "菠菜", "苦瓜", "芋头", "韭菜", "芹菜", "洋葱", "豆芽",
        // 主食/点心形态，不写进来「馄饨/抄手/云吞」会被当成三种不同的菜
        "米线", "米粉", "河粉", "年糕", "粉丝", "馄饨", "抄手", "云吞", "烧麦",
        "饺子", "包子", "馒头", "花卷", "火锅", "凉皮", "粥", "面", "饭", "饼",
        // 豆制品 / 干货：不列出来，「豆皮炒饭」「凉拌豆皮」会被当成人畜无害的「饭」，
        // 一个关键词就能灌进来七八道豆皮菜（实测过）
        "豆皮", "腐竹", "豆干", "香干", "千张", "豆泡", "面筋", "粉条", "紫菜",
        "裙带菜", "海苔", "银耳", "红枣", "花生",
        // 肉的分部位：源站爱按部位批量出「XX炒鸡胸」「XX鸡翅」
        "鸡翅", "鸡腿", "鸡胸", "鸡爪", "猪蹄", "猪肚", "肥肠", "猪肝", "腰花",
        "鸭血", "牛腩", "羊肉", "腊肉", "火腿", "香肠", "培根", "皮蛋", "咸蛋",
        // 常被批量模板化的蔬菜
        "丝瓜", "西葫芦", "秋葵", "芦笋", "茼蒿", "生菜", "娃娃菜", "包菜",
        "卷心菜", "豌豆", "四季豆", "扁豆", "蚕豆", "荷兰豆", "红薯", "紫薯",
    )

    /** 一道菜可能同时带几个主料（「鳕鱼炖茄子」= 鱼 + 茄子），全都要记下来 */
    private fun familiesOf(name: String): List<String> = MAINS.filter { name.contains(it) }

    /** 同族判定：先认主料（「蒜蓉蒸扇贝」「扇贝炒饭」「炸扇贝」都算扇贝族），认不出再看尾部 */
    private fun familyOf(name: String): String =
        MAINS.firstOrNull { name.contains(it) } ?: name.takeLast(2)

    private fun sameFamily(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        return familyOf(a) == familyOf(b)
    }

    /** 具体词 / 宽泛词 / 现场拼的做法+食材，三路混着喂，才不会一直在同一批菜里打转 */
    private fun nextKeyword(): String {
        return when (Random.nextInt(100)) {
            in 0..34 -> specific[Random.nextInt(specific.size)]
            in 35..64 -> broad[Random.nextInt(broad.size)]
            // 做法 + 食材组合：一次两万多种组合，永远有没搜过的
            else -> methods[Random.nextInt(methods.size)] + comboTargets[Random.nextInt(comboTargets.size)]
        }
    }

    private fun ensureCacheLoaded() {
        if (loadedCache) return
        loadedCache = true
        synchronized(pool) {
            val cache = readCache()
            if (pool.isEmpty()) {
                pool.addAll(cache.items.take(MAX_POOL).map {
                    if (it.slots.isEmpty()) it.copy(slots = SlotGuess.of(it.name, it.category, it.ingredients)) else it
                })
            }
            if (seen.isEmpty()) {
                seen.addAll(cache.seen)
                recent.addAll(cache.seen.takeLast(RECENT_KEEP))
            }
        }
    }

    private fun persist() {
        runCatching {
            val snapshot = NetCache(
                items = synchronized(pool) { pool.take(MAX_POOL).toList() },
                at = PlatformStore.nowMillis(),
                // 去重表只留最近 2 万条，够用几万次点击，也不至于把文件撑爆
                seen = synchronized(seen) { seen.toList().takeLast(20_000) },
            )
            PlatformStore.write(CACHE_FILE, json.encodeToString(snapshot))
        }
    }

    private fun readCache(): NetCache = runCatching {
        val text = PlatformStore.read(CACHE_FILE) ?: return NetCache()
        json.decodeFromString<NetCache>(text)
    }.getOrElse { NetCache() }
}
