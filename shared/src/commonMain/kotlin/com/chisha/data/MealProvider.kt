package com.chisha.data

import com.chisha.core.PlatformStore
import com.chisha.core.chLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** 数据源归属：国内优先，海外只兜底 */
enum class SourceKind(val tag: String) {
    DOMESTIC("国内"),
    OVERSEAS("海外"),
}

/** 一个可插拔的菜谱数据源 */
interface MealProvider {
    val id: String
    val label: String
    val kind: SourceKind
    /** 关键词可以随便传，各源自己决定怎么用；网络或解析失败请抛出异常 */
    suspend fun fetch(keyword: String): List<NetMeal>
}

internal object NetHttp {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .callTimeout(9, TimeUnit.SECONDS)
            .build()
    }

    /** 失败一定要抛出异常，上层靠它判断「源挂了」而不是「这个关键词没菜」 */
    fun getJson(url: String, referer: String? = null): String {
        val b = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        if (referer != null) b.header("Referer", referer)
        val req = b.build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body?.string()?.takeIf { it.isNotBlank() } ?: throw IOException("empty body")
        }
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

internal object NetParse {
    val json = Json { ignoreUnknownKeys = true }

    /** 源站的脏菜名：["减肥菜谱4_木耳豆腐汤"] -> "木耳豆腐汤" */
    fun cleanName(raw: String): String {
        var s = raw.trim()
        s = s.replace(Regex("^[^\\s]{0,10}菜谱\\d{1,3}[_\\-－]"), "")
        // 源站爱把栏目名当地名前缀：「去脂汤水_西芹生菜豆腐鱼尾汤」「健康减重_鳕鱼沙拉」
        s = s.replace(Regex("^[^_\\-－]{1,8}[_\\-－]"), "")
        s = s.replace(Regex("[（(][^）)]{0,10}(岁|食疗|药膳|功效)[）)]"), "")
        s = s.replace(Regex("\\d{1,3}[_\\-－]$"), "")
        return s.trim().ifBlank { raw.trim() }
    }

    /** 源站里混着大量婴幼儿食谱和药膳，直接筛掉 */
    fun usableName(name: String, category: String = ""): Boolean {
        val n = name.trim()
        if (n.length !in 2..14) return false
        val bad = listOf(
            "婴儿", "幼儿", "1－3岁", "1-3岁", "保健", "食疗", "药膳", "孕", "产妇", "术后", "鼻饲",
            // 翻译失败时编出来的占位名，进了池子只会让人觉得菜谱是假的
            "网络菜谱", "今日灵感菜", "风味菜",
        )
        if (bad.any { n.contains(it) || category.contains(it) }) return false
        if (n.contains("·")) return false
        // 半截没翻完的英文（Saudi Arabian、Uruguayan…）
        if (Regex("[A-Za-z]{4,}").containsMatchIn(n)) return false
        // 只有品类没有菜名
        val onlyCat = listOf(
            "配菜", "甜点", "甜品", "前菜", "主菜", "汤", "沙拉", "饮料", "酱料",
            "小吃", "早餐", "午餐", "晚餐", "夜宵", "菜", "主料", "配菜类",
        )
        if (n !in onlyCat) {
            // 「芹菜甜品」「白菜甜品」「生菜甜品」——源站把食材和品类词硬拼出来的假菜名，正常菜不会这么叫
            val pseudo = listOf("甜品", "甜点", "凉菜", "沙拉", "配菜", "小菜", "饮品", "饮料", "汤水")
            if (pseudo.any { n.endsWith(it) && n.length > it.length }) return false
            // 「星星炒茄子」——栏目名/作者名的叠字被带进了菜名（娃娃菜是正经菜，放过）
            if (n.length > 3 && n[0] == n[1] && !n.startsWith("娃娃") && !n.startsWith("椰")) return false
            // 「爆炒蛋液」「面糊」「橙汁」——结尾是原料/半成品或饮品，不是一道菜
            if (n.length > 2 && listOf("液", "糊", "汁", "膏", "末").any { n.endsWith(it) }) return false
            // 直接就是原料名，源站拿来当菜名了
            val rawNames = listOf(
                "蛋液", "面糊", "面团", "肉馅", "面粉", "淀粉", "生粉", "食用油", "清水", "高汤", "糯米粉", "鸡蛋清",
            )
            if (rawNames.any { n.contains(it) }) return false
        }
        return n !in onlyCat &&
            // 「日式」「西班牙风味综合」这种没菜名的形容词组合
            !n.endsWith("式") && !n.endsWith("风味") && !n.contains("综合") &&
            !n.endsWith("风味菜")
    }

    fun splitIngredients(main: String, sauce: String = ""): List<String> {
        val raw = listOf(main, sauce).filter { it.isNotBlank() }.joinToString("；")
        return raw
            .replace(Regex("(主|配|调)料："), "；")
            .replace(Regex("(主|配|调)料"), "")
            .replace('\u3000', ' ')
            .split('；', ';', '，', ',', '。', '\n')
            .map { it.trim().trimEnd('.', '。').replace(Regex("\\s+"), " ") }
            .filter { it.length in 1..30 }
            .distinct()
            .take(16)
    }

    fun splitSteps(raw: String): List<String> {
        val body = stripTail(raw)
        if (body.isBlank()) return emptyList()
        val circles = "①②③④⑤⑥⑦⑧⑨⑩"
        val marked = body.map { c ->
            val idx = circles.indexOf(c)
            if (idx >= 0) "\n${idx + 1}." else c.toString()
        }.joinToString("")

        // 源站写法混杂：1) / 1） / 1、 / 1. 都有，统一成行首的 "1."
        val marked2 = Regex("(^|[^0-9])(\\d{1,2})[)）、]").replace(marked) { m ->
            m.groupValues[1] + "\n" + m.groupValues[2] + "."
        }

        val byNumber = marked2.split(Regex("(?=\\d{1,2}[.、])"))
            .map { it.replace(Regex("^\\d{1,2}[.、]"), "").trim() }
            .filter { it.length > 3 }

        if (byNumber.size >= 2) return byNumber.take(12)

        return body.split('。', '；', '\n')
            .map { it.trim().trimEnd('。') }
            .filter { it.length > 5 }
            .take(12)
    }

    fun stripTail(raw: String): String = raw
        .replace(Regex("制作关键[：:]\\s*(.+?)(。|$)"), "")
        .replace(Regex("关键[：:]\\s*(.+?)(。|$)"), "")
        .replace(Regex("特\\s*点[：:][^。]*。?"), "")
        .trim()

    fun keyNote(raw: String): String {
        Regex("制作关键[：:]\\s*(.+?)(。|$)").find(raw)?.let { return it.groupValues[1].trim().take(80) }
        Regex("关键[：:]\\s*(.+?)(。|$)").find(raw)?.let { return it.groupValues[1].trim().take(80) }
        return ""
    }
}

/** 国内主源：大米菜谱（qqsuu），中文，关键词精确匹配，一次 10 条 */
internal object QqsuuProvider : MealProvider {
    override val id = "qqsuu"
    override val label = "大米菜谱"
    override val kind = SourceKind.DOMESTIC

    override suspend fun fetch(keyword: String): List<NetMeal> = withContext(Dispatchers.IO) {
        // 翻页 + 加大每页条数：同一类菜往后翻几页就是完全不同的一批
        val page = kotlin.random.Random.nextInt(1, 9)
        val url = "https://api.qqsuu.cn/api/dm-caipu?word=${NetHttp.enc(keyword)}&page=$page&num=20"
        val body = NetHttp.getJson(url)
        runCatching {
            val list = NetParse.json.parseToJsonElement(body)
                .jsonObject["data"]?.jsonObject?.get("list")?.jsonArray
                ?: return@runCatching emptyList()

            list.mapNotNull { el ->
                val o = el.jsonObject
                val name = NetParse.cleanName(o["cp_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty())
                val cat = o["type_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (!NetParse.usableName(name, cat)) return@mapNotNull null

                val zuofa = o["zuofa"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val ing = NetParse.splitIngredients(
                    o["yuanliao"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    o["tiaoliao"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                )
                val steps = NetParse.splitSteps(zuofa)
                if (ing.size < 2 || steps.size < 2) return@mapNotNull null

                NetMeal(
                    name = name,
                    category = cat,
                    ingredients = ing,
                    steps = steps,
                    tip = o["tishi"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                        .ifBlank { NetParse.keyNote(zuofa) },
                    feature = o["texing"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
                    source = label,
                )
            }
        }.getOrElse { emptyList() }
    }
}

/** 国内备用：清和 API，宽泛关键词一次能给 20 条，字段最规整 */
internal object QingheProvider : MealProvider {
    override val id = "qinghe"
    override val label = "清和菜谱"
    override val kind = SourceKind.DOMESTIC

    override suspend fun fetch(keyword: String): List<NetMeal> = withContext(Dispatchers.IO) {
        val url = "https://api.qhsou.com/api/cpcx.php?name=${NetHttp.enc(keyword)}"
        val body = NetHttp.getJson(url)
        runCatching {
            val arr = NetParse.json.parseToJsonElement(body).jsonObject["data"]?.jsonArray
                ?: return@runCatching emptyList()

            arr.mapNotNull { el ->
                val o = el.jsonObject
                val name = NetParse.cleanName(o["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty())
                if (!NetParse.usableName(name)) return@mapNotNull null

                val ing = o["ingredients"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf { s -> s.length in 1..30 } }
                    ?.distinct() ?: emptyList()
                val steps = o["steps"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf { s -> s.length > 3 } }
                    ?.distinct() ?: emptyList()
                if (ing.size < 2 || steps.size < 2) return@mapNotNull null

                val tags = o["tags"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim() }
                    ?.filter { it.isNotBlank() } ?: emptyList()
                val minutes = o["time_minutes"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val desc = o["description"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

                NetMeal(
                    name = name,
                    category = tags.firstOrNull() ?: "家常菜",
                    ingredients = ing.take(16),
                    steps = steps.take(12),
                    tip = "",
                    feature = if (minutes.isNotBlank()) "约 $minutes 分钟" else desc,
                    source = label,
                )
            }
        }.getOrElse { emptyList() }
    }
}

/** 海外兜底：TheMealDB，免 key，英文原文 + 中文菜名 */
internal object MealDbProvider : MealProvider {
    override val id = "mealdb"
    override val label = "海外兜底"
    override val kind = SourceKind.OVERSEAS

    override suspend fun fetch(keyword: String): List<NetMeal> = withContext(Dispatchers.IO) {
        // 只有 random.php，一次一道，并发拿 3 道
        val bodies = coroutineScope {
            List(3) { async(Dispatchers.IO) { runCatching { NetHttp.getJson(RANDOM_URL) }.getOrNull() } }.awaitAll()
        }
        bodies.mapNotNull { it?.let(::parseOne) }.distinctBy { it.name }
    }

    private const val RANDOM_URL = "https://www.themealdb.com/api/json/v1/1/random.php"

    private fun parseOne(body: String): NetMeal? = runCatching {
        val o = NetParse.json.parseToJsonElement(body)
            .jsonObject["meals"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: return@runCatching null

        fun s(k: String) = o[k]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

        val rawName = s("strMeal")
        if (rawName.isBlank()) return@runCatching null
        val area = s("strArea")
        val cat = s("strCategory")
        val zhName = MealZh.displayName(rawName, area, cat)
        // 译不出像样中文名的直接丢掉，别让「炒」「配菜」这种占位名进池子
        if (zhName.isBlank() || !NetParse.usableName(zhName, MealZh.category(cat))) return@runCatching null
        val zhArea = MealZh.area(area)
        val zhCat = MealZh.category(cat)

        val ing = (1..20).mapNotNull { i ->
            val n = s("strIngredient$i")
            val m = s("strMeasure$i")
            when {
                n.isBlank() -> null
                m.isBlank() -> n
                else -> "$n $m"
            }
        }.distinct()
        val steps = splitEnglishSteps(s("strInstructions"))
        if (ing.size < 2 || steps.size < 2) return@runCatching null

        NetMeal(
            name = zhName,
            category = listOfNotNull(zhArea.takeIf { it.isNotBlank() }, zhCat.takeIf { it.isNotBlank() })
                .joinToString("·").ifBlank { "海外菜式" },
            ingredients = ing.take(16),
            steps = steps.take(12),
            tip = "",
            feature = if (rawName != zhName) rawName else "",
            source = label,
        )
    }.getOrNull()

    private fun splitEnglishSteps(raw: String): List<String> =
        raw.replace("\r\n", "\n")
            .split('\n')
            .flatMap { it.split(Regex("(?<=[.!?])\\s+")) }
            .map { it.trim().trimEnd('.', '!', '?') }
            .filter { it.length > 12 }
            .take(12)
}

@Serializable
private data class SourceHealth(
    val fails: Int = 0,
    val coolUntil: Long = 0L,
    val okCount: Int = 0,
)

@Serializable
private data class HealthSnapshot(
    val items: Map<String, SourceHealth> = emptyMap(),
    /** 用户手动指定的源 id，空表示自动 */
    val preferred: String = "",
)

/**
 * 数据源调度：国内优先，一台挂了自动切下一台；连续失败进冷却，冷却期内直接跳过，
 * 避免每次都白等一个超时。健康状态落盘，重启后仍然记得谁挂了。
 */
object SourceHub {

    private const val HEALTH_FILE = "sources.json"
    private const val SHORT_COOL = 30_000L          // 请求失败：歇 30 秒再试
    private const val LONG_COOL = 3 * 60_000L       // 连续失败：歇 3 分钟
    private const val EMPTY_COOL = 8_000L           // 通了但这个词没菜：只歇 8 秒，换个词
    private const val LONG_COOL_AFTER = 3

    data class Result(
        val meals: List<NetMeal> = emptyList(),
        val sourceId: String = "",
        val label: String = "",
        val kind: SourceKind? = null,
        val switched: Boolean = false,
        val triedCount: Int = 0,
    ) {
        val ok: Boolean get() = meals.isNotEmpty()
    }

    // 清和的结构化数据最干净、宽泛食材词也能给满，放首选；qqsuu 只认具体词，放第二
    private val providers: List<MealProvider> = listOf(QingheProvider, QqsuuProvider, MealDbProvider)

    private val health = mutableMapOf<String, SourceHealth>()
    private var activeId = ""
    private var loaded = false

    /** 用户手动指定的源，空表示自动首选 */
    private var preferredId = ""

    /** 当前正在用的源 */
    val activeLabel: String
        get() = providers.firstOrNull { it.id == activeId }?.label.orEmpty()

    val preferredLabel: String
        get() = providers.firstOrNull { it.id == preferredId }?.label.orEmpty()

    /** 界面回显用：空字符串表示自动 */
    val preferred: String
        get() = preferredId

    /** 供界面列出可选源：id / 名称 / 归属标签 */
    fun options(): List<Triple<String, String, String>> {
        ensureLoaded()
        return providers.map { Triple(it.id, it.label, it.kind.tag) }
    }

    /** 指定用哪台源；传空字符串恢复自动 */
    fun setPreferred(id: String) {
        ensureLoaded()
        preferredId = if (providers.any { it.id == id }) id else ""
        persist()
    }

    /** 顺序尝试，第一个能出结果的源胜出；手动指定时先试指定的那台 */
    suspend fun fetch(keyword: String): Result = withContext(Dispatchers.IO) {
        ensureLoaded()
        val now = PlatformStore.nowMillis()
        var tried = 0

        val order = if (preferredId.isBlank()) providers
        else providers.filter { it.id == preferredId } + providers.filter { it.id != preferredId }

        for (p in order) {
            val h = health[p.id] ?: SourceHealth()
            if (h.coolUntil > now) continue
            // 海外兜底只在两个国内源都进冷却时才上，否则池子里会灌进一堆「XX风味YY」的假菜名
            if (p.kind == SourceKind.OVERSEAS) {
                val domesticDown = providers.filter { it.kind == SourceKind.DOMESTIC }
                    .all { d -> (health[d.id]?.coolUntil ?: 0L) > now }
                if (!domesticDown) continue
            }

            tried++
            val t0 = kotlin.time.TimeSource.Monotonic.markNow()
            val raw = try {
                p.fetch(keyword)
            } catch (e: Exception) {
                null
            }
            val usable = raw?.filter { it.steps.size >= 2 && it.ingredients.size >= 2 }.orEmpty()
            chLog(
                "  ${p.label} kw=$keyword 拿=${raw?.size ?: 0} 用=${usable.size} " +
                    "耗时=${t0.elapsedNow().inWholeMilliseconds}ms"
            )

            if (usable.isNotEmpty()) {
                health[p.id] = SourceHealth(fails = 0, coolUntil = 0, okCount = h.okCount + 1)
                val switched = if (preferredId.isNotBlank()) {
                    p.id != preferredId
                } else {
                    activeId.isNotEmpty() && activeId != p.id
                }
                activeId = p.id
                persist()
                return@withContext Result(
                    meals = usable.map { it.copy(source = p.label) },
                    sourceId = p.id,
                    label = p.label,
                    kind = p.kind,
                    switched = switched,
                    triedCount = tried,
                )
            }

            // 请求失败算故障；只是这个关键词没菜，就短歇一下换词，别把源打入长冷却
            if (raw == null) {
                val nextFails = h.fails + 1
                val cool = if (nextFails >= LONG_COOL_AFTER) LONG_COOL else SHORT_COOL
                health[p.id] = SourceHealth(fails = nextFails, coolUntil = now + cool, okCount = h.okCount)
            } else {
                // 通了只是这个词没菜：源本身没问题，不设冷却，换个词接着用。
                // 以前这里也进冷却，害得两个国内源动不动「全冷却」，只好拿海外菜顶上
                health[p.id] = SourceHealth(fails = 0, coolUntil = 0, okCount = h.okCount)
            }
            persist()
        }

        Result(triedCount = tried)
    }

    fun providerCount(): Int = providers.size

    /** 当前还有几台源没进冷却 */
    fun availableCount(): Int {
        ensureLoaded()
        val now = PlatformStore.nowMillis()
        return providers.count { (health[it.id]?.coolUntil ?: 0L) <= now }
    }

    /** 给界面用的一句话状态，例如「大米菜谱 在用 · 3 个源都能连」 */
    fun statusLine(): String {
        ensureLoaded()
        val ok = availableCount()
        val total = providers.size
        val okText = if (ok >= total) "$total 个源都能连" else "$ok/$total 个源能连"
        val pre = preferredLabel
        return when {
            pre.isNotBlank() && activeId == preferredId -> "$pre（已选）在用 · $okText"
            pre.isNotBlank() && activeLabel.isNotBlank() -> "已选 $pre 连不上，改用 $activeLabel · $okText"
            activeLabel.isNotBlank() -> "$activeLabel 在用 · $okText"
            else -> okText
        }
    }

    /** 自检用：列出每个源的当前状态 */
    fun debugLines(): List<String> {
        ensureLoaded()
        val now = PlatformStore.nowMillis()
        return providers.map { p ->
            val h = health[p.id] ?: SourceHealth()
            val coolLeft = ((h.coolUntil - now) / 1000).coerceAtLeast(0)
            val state = if (coolLeft > 0) "冷却 ${coolLeft}s" else "可用"
            "${p.id}(${p.label}/${p.kind.tag}) $state fails=${h.fails} ok=${h.okCount}" +
                if (p.id == activeId) " ← 当前" else ""
        }
    }

    /** 自检用：强制让某个源进冷却，验证切换 */
    fun forceCooldown(id: String, seconds: Long = 600) {
        ensureLoaded()
        val h = health[id] ?: SourceHealth()
        health[id] = h.copy(fails = h.fails + LONG_COOL_AFTER, coolUntil = PlatformStore.nowMillis() + seconds * 1000)
    }

    fun resetHealth() {
        ensureLoaded()
        providers.forEach { health[it.id] = SourceHealth() }
        activeId = ""
        preferredId = ""
        persist()
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val text = PlatformStore.read(HEALTH_FILE) ?: return@runCatching
            val snap = NetParse.json.decodeFromString<HealthSnapshot>(text)
            snap.items.forEach { (k, v) -> health[k] = v }
            preferredId = snap.preferred
        }
    }

    private fun persist() {
        runCatching {
            val snap = HealthSnapshot(health.toMap(), preferredId)
            PlatformStore.write(HEALTH_FILE, NetParse.json.encodeToString(snap))
        }
    }
}
