// 构建说明：Android 应用模块构建脚本，负责声明 SDK、依赖与调试地址注入。

import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

fun escapeForBuildConfig(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

fun escapeForJavaScript(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

// 这组参数由命令行 `-P` 注入，目的是让同一个 APK 同时兼容热点、USB 反向代理和模拟器三种调试场景。
val hybridApiBaseUrlProvider = providers.gradleProperty("TPS_WEB_API_BASE_URL")
    .orElse("http://10.0.2.2:3000")
val apiBaseUrlProvider = providers.gradleProperty("TPS_API_BASE_URL")
    .orElse("http://127.0.0.1:8080/")
val apiFallbackBaseUrlsProvider = providers.gradleProperty("TPS_API_FALLBACK_BASE_URLS")
    .orElse("http://10.0.2.2:8080/,http://127.0.0.1:8080/")
val websocketUrlProvider = providers.gradleProperty("TPS_WS_URL")
    .orElse(apiBaseUrlProvider.map { apiBaseUrl ->
        apiBaseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
            .trimEnd('/') + "/ws/websocket"
    })
val websocketFallbackUrlsProvider = providers.gradleProperty("TPS_WS_FALLBACK_URLS")
    .orElse(apiFallbackBaseUrlsProvider.map { fallbackBaseUrls ->
        fallbackBaseUrls
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(",") { apiBaseUrl ->
                apiBaseUrl
                    .replaceFirst("https://", "wss://")
                    .replaceFirst("http://", "ws://")
                    .trimEnd('/') + "/ws/websocket"
            }
    })
val hybridAssetsOutputDir = layout.buildDirectory.dir("generated/hybridWebAssets/main")
val hybridSourceDir = layout.projectDirectory.dir("src/main/assets/www-source")
val hybridStaticFiles = listOf(
    "index.html",
    "styles.css",
    "app.js",
    "app-config.js",
    "manifest.webmanifest",
    "sw.js",
    "icon.svg",
)

val syncHybridWebAssets by tasks.registering {
    // 把内嵌 Web 静态资源同步进 APK assets，避免 Android 构建依赖根目录下的历史演示工程。
    inputs.files(hybridStaticFiles.map { hybridSourceDir.file(it) })
    inputs.property("webApiBaseUrl", hybridApiBaseUrlProvider)
    outputs.dir(hybridAssetsOutputDir)

    doLast {
        val targetDir = hybridAssetsOutputDir.get().file("www").asFile
        delete(targetDir)
        targetDir.mkdirs()

        hybridStaticFiles.forEach { fileName ->
            copy {
                from(hybridSourceDir.file(fileName))
                into(targetDir)
            }
        }

        File(targetDir, "app-config.js").writeText(
            """
            window.__APP_CONFIG__ = Object.assign(
              {
                apiBaseUrl: "${escapeForJavaScript(hybridApiBaseUrlProvider.get())}",
              },
              window.__APP_CONFIG__ || {},
            );
            """.trimIndent() + "\n",
        )
    }
}

android {
    namespace = "com.tps"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.tps"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "BASE_URL", "\"${escapeForBuildConfig(apiBaseUrlProvider.get())}\"")
            buildConfigField("String", "FALLBACK_BASE_URLS", "\"${escapeForBuildConfig(apiFallbackBaseUrlsProvider.get())}\"")
            buildConfigField("String", "WS_URL", "\"${escapeForBuildConfig(websocketUrlProvider.get())}\"")
            buildConfigField("String", "FALLBACK_WS_URLS", "\"${escapeForBuildConfig(websocketFallbackUrlsProvider.get())}\"")
            buildConfigField(
                "String",
                "WEB_API_BASE_URL",
                "\"${escapeForBuildConfig(hybridApiBaseUrlProvider.get())}\"",
            )
        }
        debug {
            // Debug 版同样写入地址常量，便于命令行构建出的 APK 直接给真机安装测试。
            buildConfigField("String", "BASE_URL", "\"${escapeForBuildConfig(apiBaseUrlProvider.get())}\"")
            buildConfigField("String", "FALLBACK_BASE_URLS", "\"${escapeForBuildConfig(apiFallbackBaseUrlsProvider.get())}\"")
            buildConfigField("String", "WS_URL", "\"${escapeForBuildConfig(websocketUrlProvider.get())}\"")
            buildConfigField("String", "FALLBACK_WS_URLS", "\"${escapeForBuildConfig(websocketFallbackUrlsProvider.get())}\"")
            buildConfigField(
                "String",
                "WEB_API_BASE_URL",
                "\"${escapeForBuildConfig(hybridApiBaseUrlProvider.get())}\"",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(hybridAssetsOutputDir)
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.webkit)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // WebSocket (OkHttp)
    implementation(libs.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.named("preBuild") {
    dependsOn(syncHybridWebAssets)
}
