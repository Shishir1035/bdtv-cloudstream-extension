package recloudstream

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
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.newExtractorLink

/**
 * Sportzfy live-sports provider (sportzfylive.com).
 *
 * Site is event-driven (Next.js), not a static playlist — hence a dedicated scraper
 * rather than the generic config-key M3U reader. Flow:
 *   home `/`                      -> live events as `/matches/<slug>`
 *   `/matches/<slug>`             -> server list as `/stream/<server>-<slug>`
 *   `/stream/<server>-<slug>/manifest.m3u8` -> plain HLS media playlist (no auth/token);
 *                                    segments are proxied back through sportzfylive.com.
 */
class SportzfyProvider : MainAPI() {
    override var mainUrl = "https://sportzfylive.com"
    override var name = "Sportzfy"
    override var lang = "en"
    override val hasMainPage = true
    override val hasDownloadSupport = false
    override val supportedTypes = setOf(TvType.Live)

    // slug -> human title. Title derived from the slug (the site renders titles client-side).
    private suspend fun matches(): List<Pair<String, String>> {
        val html = app.get("$mainUrl/", timeout = 30).text
        return MATCH_RE.findAll(html).map { it.groupValues[1] }.distinct()
            .map { it to prettify(it) }.toList()
    }

    private fun matchUrl(slug: String) = "$mainUrl/matches/$slug"

    private fun prettify(slug: String): String =
        slug.replace(DATE_RE, "").split("-")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .replace(" X ", " x ")

    private fun Pair<String, String>.toSearch(): SearchResponse =
        newLiveSearchResponse(second, matchUrl(first), TvType.Live)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse =
        newHomePageResponse(
            listOf(HomePageList("Live Events", matches().map { it.toSearch() })),
            hasNext = false
        )

    override suspend fun search(query: String): List<SearchResponse> {
        val q = query.trim().lowercase()
        return matches().filter { it.second.lowercase().contains(q) }.map { it.toSearch() }
    }

    override suspend fun load(url: String): LoadResponse {
        val slug = url.substringAfterLast("/matches/")
        val title = prettify(slug)
        return newLiveStreamLoadResponse(title, url, url) {
            this.plot = "Live • Sportzfy"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val slug = data.substringAfterLast("/matches/")
        val html = app.get(data, timeout = 30).text
        val servers = streamRe(slug).findAll(html).map { it.groupValues[1] }.distinct().toList()
        if (servers.isEmpty()) return false
        servers.forEach { server ->
            val pageUrl = "$mainUrl/stream/$server-$slug"
            val manifest = "$pageUrl/manifest.m3u8"
            val label = "$name ${server.uppercase()}"
            try {
                // referer = the player page; segments are same-origin proxies on sportzfylive.com.
                M3u8Helper.generateM3u8(label, manifest, pageUrl).forEach(callback)
            } catch (_: Exception) {
                callback(newExtractorLink(label, label, manifest, ExtractorLinkType.M3U8) {
                    this.referer = pageUrl
                })
            }
        }
        return true
    }

    companion object {
        private val MATCH_RE = Regex("""href="/matches/([a-z0-9-]+)"""")
        private val DATE_RE = Regex("""-\d{4}-\d{2}-\d{2}$""")
        // `<server>` is the prefix before this match's slug in `/stream/<server>-<slug>`.
        private fun streamRe(slug: String) = Regex("""/stream/([a-z0-9]+)-${Regex.escape(slug)}\b""")
    }
}
