package recloudstream

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newLiveSearchResponse
import com.lagradost.cloudstream3.newLiveStreamLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder

class All1n0n3Provider : MainAPI() {
    override var name = "4ll1n0n3"
    override var mainUrl = "config://4ll1n0n3"
    override var lang = "bn"
    override val hasMainPage = true
    override val hasDownloadSupport = false
    override val supportedTypes = setOf(TvType.Live)

    // This module's key inside the remote config JSON ({ "<key>": "<playlist-url>", ... }).
    private val key = "4ll1n0n3"

    private data class Channel(
        @JsonProperty("name") val name: String,
        @JsonProperty("url") val url: String,
        @JsonProperty("logo") val logo: String? = null,
        @JsonProperty("group") val group: String = "Other",
    )

    private var cache: List<Channel>? = null

    // Resolve this module's playlist URL from the baked config JSON (CONFIG_JSON secret).
    private fun playlistUrl(): String? = try {
        parseJson<Map<String, String>>(BuildConfig.CONFIG_JSON)[key]?.takeIf { it.startsWith("http") }
    } catch (_: Exception) {
        null
    }

    private suspend fun getChannels(): List<Channel> {
        cache?.let { return it }
        val url = playlistUrl() ?: return emptyList()
        val all = try {
            parseM3u(app.get(url, timeout = 30).text)
        } catch (_: Exception) {
            emptyList()
        }
        val seen = HashSet<String>()
        val cleaned = all
            .filter { it.url.startsWith("http") }
            .filter { seen.add(normalizeName(it.name)) }
        cache = cleaned
        return cleaned
    }

    private fun normalizeName(name: String): String =
        name.lowercase().replace(QUALITY_RE, "").replace(Regex("\\s+"), " ").trim()

    private fun parseM3u(content: String): List<Channel> {
        val out = ArrayList<Channel>()
        var pending: Triple<String, String?, String>? = null
        for (raw in content.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF")) {
                val logo = LOGO_RE.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                val group = GROUP_RE.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: "Other"
                val name = line.substringAfterLast(",").trim().ifBlank { "Unknown" }
                pending = Triple(name, logo, group)
            } else if (!line.startsWith("#")) {
                val p = pending ?: continue
                out.add(Channel(name = p.first, url = line, logo = p.second, group = p.third))
                pending = null
            }
        }
        return out
    }

    private fun paddedLogo(logo: String?): String? {
        val l = logo?.takeIf { it.startsWith("http") } ?: return logo
        val enc = URLEncoder.encode(l, "UTF-8")
        return "https://images.weserv.nl/?url=$enc&w=342&h=513&fit=contain&cbg=0d0d0d&output=webp"
    }

    private fun Channel.toSearch(): SearchResponse =
        newLiveSearchResponse(name, this.toJson(), TvType.Live, fix = false) {
            this.posterUrl = paddedLogo(logo)
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val rows = getChannels()
            .groupBy { it.group }
            .toList()
            .sortedByDescending { it.second.size }
            .map { (group, list) -> HomePageList(group, list.map { it.toSearch() }) }
        return newHomePageResponse(rows, hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val q = query.trim().lowercase()
        return getChannels().filter { it.name.lowercase().contains(q) }.map { it.toSearch() }
    }

    override suspend fun load(url: String): LoadResponse {
        val ch = parseJson<Channel>(url)
        return newLiveStreamLoadResponse(ch.name, url, ch.url) {
            this.posterUrl = paddedLogo(ch.logo)
            this.plot = "Live • ${ch.group}"
            this.tags = listOf(ch.group)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val streamUrl = if (data.startsWith("{")) parseJson<Channel>(data).url else data
        try {
            M3u8Helper.generateM3u8(source = name, streamUrl = streamUrl, referer = "").forEach(callback)
        } catch (e: Exception) {
            callback(newExtractorLink(name, name, streamUrl, ExtractorLinkType.M3U8))
        }
        return true
    }

    companion object {
        private val LOGO_RE = Regex("""tvg-logo="([^"]*)"""")
        private val GROUP_RE = Regex("""group-title="([^"]*)"""")
        private val QUALITY_RE = Regex("""\(?\b\d{3,4}p\b\)?|\bhd\b""")
    }
}
