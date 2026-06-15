package recloudstream

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class M3uLivePlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(M3uLiveProvider())
    }
}
