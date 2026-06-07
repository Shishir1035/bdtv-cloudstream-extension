package recloudstream

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class F1f4Plugin : BasePlugin() {
    override fun load() {
        registerMainAPI(F1f4Provider())
    }
}
