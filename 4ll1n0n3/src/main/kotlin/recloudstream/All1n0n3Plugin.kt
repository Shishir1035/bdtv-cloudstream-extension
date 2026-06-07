package recloudstream

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class All1n0n3Plugin : BasePlugin() {
    override fun load() {
        registerMainAPI(All1n0n3Provider())
    }
}
