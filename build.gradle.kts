import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// The same config manifest settings.gradle.kts uses to define the modules:
// { "<key>": { link, desc, logo }, ... }. CONFIG_JSON secret (CI) or config.json (local).
// `link` is read at runtime by the provider; `desc`/`logo` are build-time extension metadata.
val configRaw: String = System.getenv("CONFIG_JSON")?.takeIf { it.isNotBlank() }
    ?: rootProject.file("config.json").takeIf { it.exists() }?.readText()
    ?: "{}"
@Suppress("UNCHECKED_CAST")
val extensionMeta: Map<String, Map<String, Any?>> =
    JsonSlurper().parseText(configRaw) as Map<String, Map<String, Any?>>

buildscript {
    repositories {
        google()
        mavenCentral()
        // Shitpack repo which contains our tools and dependencies
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        // Cloudstream gradle plugin which makes everything work and builds plugins.
        // Pinned to a jitpack commit version: the bare -SNAPSHOT pom 404s due to a
        // jitpack filename-mangling bug, but a commit coordinate resolves cleanly
        // (pom self-version matches the request). Bump to a newer commit when needed.
        classpath("com.github.recloudstream:gradle:81b1d424d2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
    extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) =
    extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    // cloudstream:library is a -SNAPSHOT (changing module); by default Gradle re-checks its
    // jitpack metadata every build, which fails whenever jitpack is slow/down. Trust the
    // cached copy for 30 days so a once-resolved artifact is reused without hitting jitpack.
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(30, "days")
    }

    // Common extension metadata. Every module is the same generic M3U reader; only its
    // key (= project name), description and icon differ — all sourced from the config manifest.
    version = 1
    val meta = extensionMeta[project.name]
    val metaDesc = (meta?.get("desc") as? String)?.takeIf { it.isNotBlank() }
    val metaLogo = (meta?.get("logo") as? String)?.takeIf { it.isNotBlank() }
    cloudstream {
        // GITHUB_REPOSITORY is set automatically by the CI workflow.
        // Locally it falls back to the placeholder below — replace USER/REPO.
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/Shishir1035/bdtv-cloudstream-extension")
        description = metaDesc ?: "Bangla live TV (M3U)."
        authors = listOf("Shishir1035")
        // Status: 0=Down, 1=Ok, 2=Slow, 3=Beta-only
        status = 1
        tvTypes = listOf("Live")
        iconUrl = metaLogo ?: "https://www.google.com/s2/favicons?domain=toffeelive.com&sz=%size%"
        isCrossPlatform = true
    }

    android {
        namespace = "recloudstream"

        buildFeatures.buildConfig = true

        // All modules compile from one shared source tree; only BuildConfig.MODULE_KEY differs.
        sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src/main/kotlin"))

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35

            // This module's key inside the config JSON = its gradle project (dir) name.
            buildConfigField("String", "MODULE_KEY", "\"${project.name}\"")

            // Config JSON ({ "<key>": "<playlist-url>", ... }) injected from the CONFIG_JSON
            // env var (a GitHub secret in CI). Escaped into a Java string literal so the URLs
            // live in the secret, never in source. Empty object fallback for local builds.
            val configJson = System.getenv("CONFIG_JSON") ?: "{}"
            val escaped = configJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n")
            buildConfigField("String", "CONFIG_JSON", "\"$escaped\"")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8) // Required
                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions"
                )
            }
        }
    }

    dependencies {
        val implementation by configurations

        implementation("com.github.recloudstream.cloudstream:library:-SNAPSHOT")
        implementation(kotlin("stdlib")) // Adds Standard Kotlin Features
        implementation("com.github.Blatzar:NiceHttp:0.4.11") // HTTP Lib
        implementation("org.jsoup:jsoup:1.18.3") // HTML Parser
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1") // JSON Parser
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
