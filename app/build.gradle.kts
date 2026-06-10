import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use(keystoreProperties::load)
}

val devPlaylistsPropertiesFile = rootProject.file("dev-playlists.properties")
val devPlaylistsProperties = Properties()
if (devPlaylistsPropertiesFile.exists()) {
    FileInputStream(devPlaylistsPropertiesFile).use(devPlaylistsProperties::load)
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val devModePlaylistPresetSpecs: String =
    devPlaylistsProperties.getProperty("devModePlaylistPresetSpecs", "").orEmpty()

val bundledPublicSourceSpec =
    "public_sources/playlist_usa.m3u8" +
        "::afterglow_public_live.m3u8" +
        "::Demo Playlist" +
        "::LIVE" +
        "::false" +
        "::afterglow_public_live.xml" +
        "::https://afterglow-labs.com/tv/afterglow_public_live.m3u8" +
        "::https://afterglow-labs.com/tv/afterglow_public_live.xml"

android {
    namespace = "com.afterglowtv.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.afterglowtv.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 32
        versionName = "0.1.31"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"com.afterglowtv.app\"")
        buildConfigField("boolean", "DATE_UNLOCKS_HIDDEN_FEATURES", "false")
        buildConfigField("long", "FEATURE_RELEASE_UNLOCK_EPOCH_MS", "0L")
        buildConfigField("long", "PREMIUM_PREVIEW_FREE_UNTIL_EPOCH_MS", "0L")
        buildConfigField("boolean", "ENABLE_AMAZON_APPSTORE_SDK", "false")
        buildConfigField("boolean", "ENABLE_AMAZON_DRM_LICENSING", "false")
        buildConfigField("boolean", "ENABLE_AMAZON_DEVICE_MESSAGING", "false")
        buildConfigField("String", "AMAZON_PREMIUM_MONTHLY_SKU", "\"\"")
        buildConfigField("String", "AMAZON_PREMIUM_QUARTERLY_SKU", "\"\"")
        buildConfigField("String", "AMAZON_PREMIUM_ANNUALLY_SKU", "\"\"")
        buildConfigField("String", "AMAZON_PREMIUM_LIFETIME_SKU", "\"\"")
        buildConfigField("String", "DEV_MODE_PLAYLIST_PRESET_SPECS", devModePlaylistPresetSpecs.asBuildConfigString())
    }

    flavorDimensions += "store"

    productFlavors {
        create("amazon") {
            dimension = "store"
            applicationId = "com.afterglowtv.app"
            versionNameSuffix = "-amazon"
            buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"com.afterglowtv.app\"")
            buildConfigField("boolean", "AMAZON_REVIEW_BUILD", "true")
            buildConfigField("String", "DEFAULT_NETWORK_SHARE_NAME", "\"\"")
            buildConfigField("String", "DEFAULT_NETWORK_SHARE_PATH", "\"\"")
            buildConfigField("boolean", "SHOW_ADVANCED_SOURCE_TYPES", "false")
            buildConfigField("boolean", "SHOW_ADULT_SURFACES", "true")
            buildConfigField("boolean", "SHOW_WELCOME_ROUTE", "false")
            buildConfigField("boolean", "ENABLE_BUNDLED_PUBLIC_SOURCE", "true")
            buildConfigField(
                "String",
                "BUNDLED_PUBLIC_SOURCE_SPECS",
                "\"$bundledPublicSourceSpec\""
            )
            buildConfigField("boolean", "ALLOW_XTREAM_PLAYLIST_AUTO_DETECTION", "false")
            buildConfigField("boolean", "ENABLE_SIDELOAD_UPDATES", "false")
            buildConfigField("boolean", "ENABLE_DVR", "false")
            buildConfigField("boolean", "ALLOW_DVR_DEVELOPER_UNLOCK", "true")
            buildConfigField("boolean", "ENABLE_AMAZON_APPSTORE_SDK", "true")
            buildConfigField("boolean", "ENABLE_AMAZON_DRM_LICENSING", "true")
            buildConfigField("boolean", "ENABLE_AMAZON_DEVICE_MESSAGING", "true")
            buildConfigField("String", "AMAZON_PREMIUM_MONTHLY_SKU", "\"com.afterglowtv.app.premium.monthly.v1\"")
            buildConfigField("String", "AMAZON_PREMIUM_QUARTERLY_SKU", "\"com.afterglowtv.app.premium.quarterly.v1\"")
            buildConfigField("String", "AMAZON_PREMIUM_ANNUALLY_SKU", "\"com.afterglowtv.app.premium.annually.v1\"")
            buildConfigField("String", "AMAZON_PREMIUM_LIFETIME_SKU", "\"com.afterglowtv.app.premium.lifetime.v1\"")
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_AMAZON_DRM_LICENSING", "false")
        }

        release {
            // Amazon Appstore SDK 3.0.9 ships legacy bytecode that R8 reports as
            // malformed during release minification. Do not run R8 over the
            // submission APK; Amazon DRM/IAP stability matters more than APK
            // size for this Fire TV build.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        jniLibs {
            keepDebugSymbols += setOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so"
            )
        }
    }

testOptions {
        animationsDisabled = true
    }

}

androidComponents {
    onVariants(selector().withFlavor("store", "amazon")) { variant ->
        variant.androidResources.localeFilters.addAll(
            listOf(
                "en",
                "ar",
                "cs",
                "da",
                "de",
                "el",
                "es",
                "fi",
                "fr",
                "hu",
                "in",
                "it",
                "iw",
                "ja",
                "ko",
                "nb",
                "nl",
                "pl",
                "pt",
                "ro",
                "ru",
                "sv",
                "tr",
                "uk",
                "vi",
                "zh"
            )
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

kover {
    currentProject {
        createVariant("ci") {
            add("amazonDebug")
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":player"))

    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Compose TV
    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.ui)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Activity & Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.documentfile)
    implementation(libs.coroutines.android)
    implementation(libs.appcompat)
    implementation(libs.mediarouter)
    implementation(libs.play.services.cast.framework)
    implementation(libs.amazon.appstore.sdk)
    compileOnly(files("libs/amazon-device-messaging-1.2.0.jar"))

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.configureEach {
    if (name.startsWith("hiltJavaCompile") && name.endsWith("UnitTest")) {
        enabled = false
    }
}
