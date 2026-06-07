import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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

    cloudstream {
        // GITHUB_REPOSITORY is set automatically by the CI workflow.
        // Locally it falls back to the placeholder below — replace USER/REPO.
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/Shishir1035/bdtv-cloudstream-extension")
    }

    android {
        namespace = "recloudstream"

        buildFeatures.buildConfig = true

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35

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
