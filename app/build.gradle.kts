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

val bundledPublicSourceSpec =
    "public_sources/playlist_usa.m3u8" +
        "::afterglow_public_live.m3u8" +
        "::Free, Authorized Public M3U Playlist" +
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
        minSdk = 28
        targetSdk = 36
        versionCode = 29
        versionName = "0.1.28"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"com.afterglowtv.app\"")
        buildConfigField("boolean", "DATE_UNLOCKS_HIDDEN_FEATURES", "false")
        buildConfigField("long", "FEATURE_RELEASE_UNLOCK_EPOCH_MS", "0L")
        buildConfigField("long", "PREMIUM_PREVIEW_FREE_UNTIL_EPOCH_MS", "0L")
        buildConfigField("boolean", "ENABLE_AMAZON_APPSTORE_SDK", "false")
        buildConfigField("String", "AMAZON_PREMIUM_MONTHLY_SKU", "\"\"")
        buildConfigField("String", "AMAZON_PREMIUM_LIFETIME_SKU", "\"\"")
    }

    flavorDimensions += "store"

    productFlavors {
        create("standard") {
            dimension = "store"
            buildConfigField("boolean", "AMAZON_REVIEW_BUILD", "false")
            buildConfigField("String", "DEFAULT_NETWORK_SHARE_NAME", "\"\"")
            buildConfigField("String", "DEFAULT_NETWORK_SHARE_PATH", "\"\"")
            buildConfigField("boolean", "SHOW_ADVANCED_SOURCE_TYPES", "true")
            buildConfigField("boolean", "SHOW_ADULT_SURFACES", "true")
            buildConfigField("boolean", "SHOW_WELCOME_ROUTE", "true")
            buildConfigField("boolean", "ENABLE_BUNDLED_PUBLIC_SOURCE", "false")
            buildConfigField("String", "BUNDLED_PUBLIC_SOURCE_SPECS", "\"\"")
            buildConfigField("boolean", "ALLOW_XTREAM_PLAYLIST_AUTO_DETECTION", "true")
            buildConfigField("boolean", "ENABLE_SIDELOAD_UPDATES", "true")
            buildConfigField("boolean", "ENABLE_DVR", "true")
            buildConfigField("boolean", "ALLOW_DVR_DEVELOPER_UNLOCK", "false")
        }

        create("amazon") {
            dimension = "store"
            applicationId = "com.afterglow.tv.fire"
            versionNameSuffix = "-amazon"
            buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"com.afterglow.tv.fire\"")
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
            buildConfigField("String", "AMAZON_PREMIUM_MONTHLY_SKU", "\"afterglow_premium_monthly\"")
            buildConfigField("String", "AMAZON_PREMIUM_LIFETIME_SKU", "\"afterglow_premium_lifetime\"")
        }

        create("direct") {
            dimension = "store"
            applicationId = "com.afterglow.tv.fire"
            versionNameSuffix = "-direct"
            buildConfigField("String", "OFFICIAL_APPLICATION_ID", "\"com.afterglow.tv.fire\"")
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
            buildConfigField("boolean", "DATE_UNLOCKS_HIDDEN_FEATURES", "true")
            buildConfigField("long", "FEATURE_RELEASE_UNLOCK_EPOCH_MS", "1784073600000L")
            buildConfigField("long", "PREMIUM_PREVIEW_FREE_UNTIL_EPOCH_MS", "1790812800000L")
            buildConfigField("boolean", "ENABLE_AMAZON_APPSTORE_SDK", "true")
            buildConfigField("String", "AMAZON_PREMIUM_MONTHLY_SKU", "\"afterglow_premium_monthly\"")
            buildConfigField("String", "AMAZON_PREMIUM_LIFETIME_SKU", "\"afterglow_premium_lifetime\"")
        }

        create("corey") {
            dimension = "store"
            applicationIdSuffix = ".corey"
            versionNameSuffix = "-corey"
            buildConfigField("boolean", "AMAZON_REVIEW_BUILD", "false")
            buildConfigField("String", "DEFAULT_NETWORK_SHARE_NAME", "\"Plex\"")
            buildConfigField("String", "DEFAULT_NETWORK_SHARE_PATH", "\"\\\\\\\\192.168.1.8\\\\Plex\"")
            buildConfigField("boolean", "SHOW_ADVANCED_SOURCE_TYPES", "true")
            buildConfigField("boolean", "SHOW_ADULT_SURFACES", "true")
            buildConfigField("boolean", "SHOW_WELCOME_ROUTE", "true")
            buildConfigField("boolean", "ENABLE_BUNDLED_PUBLIC_SOURCE", "false")
            buildConfigField("String", "BUNDLED_PUBLIC_SOURCE_SPECS", "\"\"")
            buildConfigField("boolean", "ALLOW_XTREAM_PLAYLIST_AUTO_DETECTION", "true")
            buildConfigField("boolean", "ENABLE_SIDELOAD_UPDATES", "false")
            buildConfigField("boolean", "ENABLE_DVR", "true")
            buildConfigField("boolean", "ALLOW_DVR_DEVELOPER_UNLOCK", "false")
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
        release {
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

    sourceSets {
        getByName("direct") {
            res.directories.add("src/amazon/res")
            assets.directories.add("src/amazon/assets")
        }
    }
}

androidComponents {
    onVariants(selector().withFlavor("store", "amazon")) { variant ->
        variant.androidResources.localeFilters.add("en")
    }
    onVariants(selector().withFlavor("store", "direct")) { variant ->
        variant.androidResources.localeFilters.add("en")
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
            add("standardDebug")
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
