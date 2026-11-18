package com.chisha.data

import com.chisha.core.PlatformStore
import com.chisha.core.chLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 按菜名取一张真实菜品照片。
 *
 * 背景：内置图包只有 17 个品类，同一类菜共用一张图，容易出现「图不是这道菜」。
 * 这里用公开图片搜索按菜名拿首张合适的照片，命中后缓存到 photos.json，
 * 之后离线也直接复用；拿不到就返回 null，界面回落品类图。
 */
object DishPhoto {
    private const val KEY = "photos.json"
    private const val RETRY_GAP = 30 * 60_000L      // 失败后 30 分钟内不再重试
    private const val MIN_SIDE = 480                 // 太小的图裁到卡片里会糊
    private const val MIN_RATIO = 0.55f              // 宽高比过滤，避免超长条幅
    private const val MAX_RATIO = 1.90f
    private const val REFERER = "https://image.so.com/"

    /** 菜名里常见的做法/配料前缀，搜不到图时先剥掉它 */
    private val MODIFIERS = listOf(
        "青椒", "素炒", "清炒", "小炒", "爆炒", "干煸", "香煎", "凉拌", "蒜蓉", "红油",
        "酱爆", "水煮", "酸汤", "家常", "蒜香", "麻辣", "香辣", "糖醋", "红烧", "清蒸",
        "白灼", "油焖", "豉香", "酱香", "五香", "椒盐", "干锅", "砂锅", "秘制", "原味",
    )

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val cache = mutableMapOf<String, String>()
    private val failedAt = mutableMapOf<String, Long>()
    private var loaded = false

    @Serializable
    private data class Store(
        val photos: Map<String, String> = emptyMap(),
        val failed: Map<String, Long> = emptyMap(),
    )

    /** 360 图片搜索返回的一条结果 */
    @Serializable
    private data class Hit(
        val thumb: String = "",
        val img: String = "",
        val width: Int = 0,
        val height: Int = 0,
        val title: String = "",
        val site: String = "",
    )

    @Serializable
    private data class Resp(val list: List<Hit> = emptyList())

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val raw = PlatformStore.read(KEY) ?: return
            val s = json.decodeFromString<Store>(raw)
            cache.putAll(s.photos)
            failedAt.putAll(s.failed)
        }
    }

    private fun persist() {
        // 只留最近用到的，避免 photos.json 越滚越大（网络菜名会源源不断进来）
        trimCache()
        runCatching {
            PlatformStore.write(
                KEY,
                json.encodeToString(Store(cache.toMap(), failedAt.toMap())),
            )
        }
    }

    private fun trimCache() {
        val cap = 1200
        while (cache.size > cap) {
            val first = cache.keys.firstOrNull() ?: break
            cache.remove(first)
        }
        val now = PlatformStore.nowMillis()
        failedAt.entries.removeAll { now - it.value > RETRY_GAP }
        while (failedAt.size > 400) {
            val first = failedAt.keys.firstOrNull() ?: break
            failedAt.remove(first)
        }
    }

    /** 已经抓到的照片（不发请求，给界面即时用） */
    fun cached(name: String): String? {
        ensureLoaded()
        return cache[name]
    }

    /** 给一道菜补照片，拿到返回 URL，拿不到返回 null */
    suspend fun resolve(name: String): String? = withContext(Dispatchers.IO) {
        val clean = name.trim()
        if (clean.length !in 2..20) return@withContext null
        ensureLoaded()
        cache[clean]?.let { return@withContext it }
        val last = failedAt[clean]
        if (last != null && PlatformStore.nowMillis() - last < RETRY_GAP) return@withContext null

        val url = runCatching { search(clean) }.getOrNull()
            ?: degrade(clean)?.let { alt -> runCatching { search(alt) }.getOrNull() }
        if (url != null) {
            cache[clean] = url
        } else {
            failedAt[clean] = PlatformStore.nowMillis()
        }
        persist()
        url
    }

    /** 「青椒南瓜丝」这类生僻全名搜不到时，剥掉修饰词换成主料再试一次 */
    private fun degrade(name: String): String? {
        for (m in MODIFIERS) {
            if (name.startsWith(m) && name.length - m.length >= 2) return name.removePrefix(m)
        }
        return if (name.length >= 5) name.substring(2) else null
    }

    /** 一次给多道菜补图（并发 3，别把接口打毛） */
    suspend fun resolveAll(names: List<String>): Map<String, String> = coroutineScope {
        val t0 = kotlin.time.TimeSource.Monotonic.markNow()
        val todo = names.map { it.trim() }.distinct().take(12)
        val res = todo.chunked(3).flatMap { chunk ->
            chunk.map { n -> async { n to resolve(n) } }.awaitAll()
        }.mapNotNull { (n, u) -> u?.let { n to it } }.toMap()
        chLog("  取图 ${res.size}/${todo.size} 批=${(todo.size + 2) / 3} 耗时=${t0.elapsedNow().inWholeMilliseconds}ms")
        res
    }

    /** 后台把本地菜库里还没有照片的菜补齐 */
    suspend fun backfill(only: Int = 80) {
        val repo = ChishaRepo.instance
        val missing = repo.dishes
            .asSequence()
            .filter { it.enabled && it.imageUri.isNullOrBlank() }
            .map { it.name }
            .distinct()
            .take(only)
            .toList()
        missing.chunked(3).forEach { chunk ->
            chunk.forEach { name ->
                val url = resolve(name)
                if (url != null) repo.attachPhoto(name, url)
            }
            if (chunk.isNotEmpty()) delay(600)
        }
    }

    // ---------------- 取图 ----------------

    private fun searchUrl(name: String): String {
        val e = NetHttp.enc(name)
        return "https://image.so.com/j?q=$e&src=srp&correct=$e&sn=0&pn=15"
    }

    /** 按菜名搜实拍图：标题里带菜名的优先，其次看尺寸和比例 */
    private fun search(name: String): String? {
        val body = NetHttp.getJson(searchUrl(name), referer = REFERER)
        val hits = json.decodeFromString<Resp>(body).list
        return pick(hits, name)
    }

    /**
     * 只认真实菜谱 / 美食内容站：这些站的配图基本是自家实拍成品图。
     * 插画站、素材站、360 自建图库（AI 合成图多）都排除，宁可回落内置图也不给错图。
     */
    private val TRUSTED = listOf(
        "meishij.net", "xiachufang.com", "douguo.com", "xiangha.com", "meishichina.com",
        "baijiahao.baidu.com", "sohu.com", "163.com", "zhihu.com", "xiaohongshu.com",
        "weibo.com", "douyin.com", "shipu", "chihe.sohu.com",
    )

    private fun trusted(site: String): Boolean = TRUSTED.any { site.contains(it, ignoreCase = true) }

    /** 菜谱站的配图很多是「制作过程」：擀面杖、面团、备料摆拍。
     *  搜「豆浆油条」给张擀面杖出来比没图更糟，这类标题直接排掉。 */
    private val PROCESS_WORDS = listOf(
        "做法", "步骤", "教程", "图解", "食材", "原料", "配料", "备料", "如何", "怎样",
        "和面", "面团", "揉面", "发酵", "面糊", "调料", "厨具", "锅具", "摆盘过程",
    )

    private fun looksLikeProcess(title: String): Boolean =
        PROCESS_WORDS.any { title.contains(it) }

    private fun pick(hits: List<Hit>, name: String): String? {
        val usable = hits.filter { h ->
            val src = h.thumb.ifBlank { h.img }
            src.startsWith("https://") && fits(h.width, h.height) && h.site.isNotBlank() &&
                !looksLikeProcess(h.title)
        }
        val cooking = usable.filter { trusted(it.site) }
        // 成品优先：标题就是菜名 > 标题带菜名 > 分辨率高
        val ranked = cooking.sortedWith(
            compareByDescending<Hit> { it.title.trim() == name }
                .thenByDescending { it.title.contains(name) }
                .thenByDescending { minOf(it.width, it.height) },
        )
        val best = ranked.firstOrNull()
            ?: usable.firstOrNull { it.site != "pic.360.com" && it.title.trim() == name }
        return best?.let { it.thumb.ifBlank { it.img } }
    }

    /** 尺寸、比例都够卡片用；宽高缺失时信任搜索结果排序 */
    private fun fits(w: Int, h: Int): Boolean {
        if (w <= 0 || h <= 0) return true
        if (minOf(w, h) < MIN_SIDE) return false
        val ratio = w.toFloat() / h.toFloat()
        return ratio in MIN_RATIO..MAX_RATIO
    }

    /** 诊断用：把一次搜索的解析结果摊开（只在自检入口里调用） */
    fun debugSearch(name: String): String {
        val body = runCatching { NetHttp.getJson(searchUrl(name), referer = REFERER) }
            .getOrElse { return "HTTP_ERR " + it.message }
        val hits = runCatching { json.decodeFromString<Resp>(body).list }
            .getOrElse { return "PARSE_ERR " + it.message + " body=" + body.take(140) }
        val top = hits.take(6).map { "「${it.title.take(14)}」${it.width}x${it.height}@${it.site}" }
        return "len=${body.length} hits=${hits.size} top=$top picked=" + (pick(hits, name) ?: "null")
    }
}
