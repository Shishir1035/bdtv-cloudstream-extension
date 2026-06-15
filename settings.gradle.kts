import groovy.json.JsonSlurper

rootProject.name = "CloudstreamPlugins"

// Single source of truth: the config manifest, shape { "<key>": { link, desc, logo }, ... }.
// CI passes it as the CONFIG_JSON secret (env); locally it's the gitignored config.json file.
// Every key becomes a buildable CloudStream extension — sources come from shared/, identity
// (BuildConfig.MODULE_KEY) is the key. Module dirs are created on demand (build output only).
val configRaw = System.getenv("CONFIG_JSON")?.takeIf { it.isNotBlank() }
    ?: File(rootDir, "config.json").takeIf { it.exists() }?.readText()
    ?: "{}"

@Suppress("UNCHECKED_CAST")
val manifest = JsonSlurper().parseText(configRaw) as Map<String, Any>

manifest.keys.forEach { key ->
    File(rootDir, key).mkdirs()
    include(key)
}
