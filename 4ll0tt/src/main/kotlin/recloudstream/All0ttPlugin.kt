package recloudstream

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class All0ttPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(All0ttProvider())
    }
}
