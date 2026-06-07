package recloudstream

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class BdD3vPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(BdD3vProvider())
    }
}
