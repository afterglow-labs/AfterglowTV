package com.afterglowtv.app.store

import com.afterglowtv.app.BuildConfig
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderSourceSlot

data class BundledPublicSourceSpec(
    val playlistAssetPath: String,
    val playlistFileName: String,
    val providerName: String,
    val sourceSlot: ProviderSourceSlot,
    val m3uVodClassificationEnabled: Boolean,
    val guideFileName: String,
    val playlistUrl: String = "",
    val guideUrl: String = ""
)

data class StorePolicySnapshot(
    val amazonReviewBuild: Boolean,
    val showAdvancedSourceTypes: Boolean,
    val showAdultSurfaces: Boolean,
    val showWelcomeRoute: Boolean,
    val enableBundledPublicSource: Boolean,
    val bundledPublicSources: List<BundledPublicSourceSpec>,
    val allowXtreamPlaylistAutoDetection: Boolean,
    val enableSideloadUpdates: Boolean,
    val enableDvr: Boolean,
    val allowDvrDeveloperUnlock: Boolean,
    val guideOnlyReviewSurface: Boolean = false,
    val dateUnlocksHiddenFeatures: Boolean = false,
    val featureReleaseUnlockEpochMs: Long = 0L,
    val premiumPreviewFreeUntilEpochMs: Long = 0L
) {
    fun canUseDvr(developerModeEnabled: Boolean): Boolean =
        enableDvr || (allowDvrDeveloperUnlock && developerModeEnabled)

    fun isFeatureReleaseUnlocked(nowMs: Long): Boolean =
        dateUnlocksHiddenFeatures &&
            featureReleaseUnlockEpochMs > 0L &&
            nowMs >= featureReleaseUnlockEpochMs

    fun isPremiumPreviewFree(nowMs: Long): Boolean =
        premiumPreviewFreeUntilEpochMs <= 0L || nowMs < premiumPreviewFreeUntilEpochMs

    fun isPremiumPreviewActive(nowMs: Long): Boolean =
        isFeatureReleaseUnlocked(nowMs) && isPremiumPreviewFree(nowMs)

    fun isPremiumPaymentRequired(nowMs: Long): Boolean =
        dateUnlocksHiddenFeatures &&
            featureReleaseUnlockEpochMs > 0L &&
            premiumPreviewFreeUntilEpochMs > 0L &&
            nowMs >= premiumPreviewFreeUntilEpochMs

    fun effectiveDeveloperModeEnabled(storedDeveloperModeEnabled: Boolean, nowMs: Long): Boolean =
        effectiveDeveloperModeEnabled(
            storedDeveloperModeEnabled = storedDeveloperModeEnabled,
            amazonPremiumEntitled = false,
            nowMs = nowMs
        )

    fun effectiveDeveloperModeEnabled(
        storedDeveloperModeEnabled: Boolean,
        amazonPremiumEntitled: Boolean,
        nowMs: Long
    ): Boolean =
        storedDeveloperModeEnabled ||
            (amazonReviewBuild && (amazonPremiumEntitled || isPremiumPreviewActive(nowMs)))

    fun shouldShowPremiumPurchaseOptions(
        storedDeveloperModeEnabled: Boolean,
        amazonPremiumEntitled: Boolean,
        nowMs: Long
    ): Boolean =
        amazonReviewBuild &&
            isPremiumPaymentRequired(nowMs) &&
            !storedDeveloperModeEnabled &&
            !amazonPremiumEntitled

    fun effectiveFor(storedDeveloperModeEnabled: Boolean, nowMs: Long): StorePolicySnapshot =
        effectiveFor(
            storedDeveloperModeEnabled = storedDeveloperModeEnabled,
            amazonPremiumEntitled = false,
            nowMs = nowMs
        )

    fun effectiveFor(
        storedDeveloperModeEnabled: Boolean,
        amazonPremiumEntitled: Boolean,
        nowMs: Long
    ): StorePolicySnapshot =
        if (amazonReviewBuild && effectiveDeveloperModeEnabled(storedDeveloperModeEnabled, amazonPremiumEntitled, nowMs)) {
            copy(
                showAdvancedSourceTypes = true,
                showAdultSurfaces = storedDeveloperModeEnabled,
                guideOnlyReviewSurface = false,
                allowXtreamPlaylistAutoDetection = true,
                enableSideloadUpdates = true,
                enableDvr = true
            )
        } else {
            this
        }

    fun isBundledPublicSourceProvider(provider: Provider): Boolean {
        return enableBundledPublicSource &&
            bundledPublicSources.any { spec ->
                val bundledPathMarker = "/$BUNDLED_PUBLIC_SOURCE_DIRECTORY/${spec.playlistFileName}"
                provider.m3uUrl.isBundledPublicSourceUrl(spec.playlistFileName, bundledPathMarker) ||
                    provider.serverUrl.isBundledPublicSourceUrl(spec.playlistFileName, bundledPathMarker) ||
                    LEGACY_BUNDLED_PUBLIC_PLAYLIST_FILE_NAMES.any { legacyName ->
                        provider.m3uUrl.endsWith("/$legacyName") ||
                            provider.serverUrl.endsWith("/$legacyName") ||
                            provider.m3uUrl == legacyName ||
                            provider.serverUrl == legacyName
                    }
            }
    }

    fun isUserVisibleProvider(provider: Provider): Boolean =
        true

    fun shouldSeedBundledPublicSource(providers: List<Provider>, seededOnce: Boolean): Boolean =
        enableBundledPublicSource &&
            bundledPublicSources.isNotEmpty() &&
            !seededOnce &&
            providers.isEmpty()

    private fun String.isBundledPublicSourceUrl(fileName: String, bundledPathMarker: String): Boolean =
        contains(bundledPathMarker) || endsWith("/$fileName") || this == fileName

    companion object {
        const val BUNDLED_PUBLIC_SOURCE_DIRECTORY = "bundled_public_sources"
        val LEGACY_BUNDLED_PUBLIC_PLAYLIST_FILE_NAMES = setOf("afterglow_amazon_live.m3u8")

        val fullFeature = StorePolicySnapshot(
            amazonReviewBuild = false,
            showAdvancedSourceTypes = true,
            showAdultSurfaces = true,
            showWelcomeRoute = true,
            enableBundledPublicSource = false,
            bundledPublicSources = emptyList(),
            allowXtreamPlaylistAutoDetection = true,
            enableSideloadUpdates = true,
            enableDvr = true,
            allowDvrDeveloperUnlock = false
        )

        val amazon = StorePolicySnapshot(
            amazonReviewBuild = true,
            showAdvancedSourceTypes = false,
            showAdultSurfaces = true,
            showWelcomeRoute = false,
            enableBundledPublicSource = true,
            bundledPublicSources = listOf(
                BundledPublicSourceSpec(
                    playlistAssetPath = "public_sources/playlist_usa.m3u8",
                    playlistFileName = "afterglow_public_live.m3u8",
                    providerName = "Demo Playlist",
                    sourceSlot = ProviderSourceSlot.LIVE,
                    m3uVodClassificationEnabled = false,
                    guideFileName = "afterglow_public_live.xml",
                    playlistUrl = "https://afterglow-labs.com/tv/afterglow_public_live.m3u8",
                    guideUrl = "https://afterglow-labs.com/tv/afterglow_public_live.xml"
                )
            ),
            allowXtreamPlaylistAutoDetection = false,
            enableSideloadUpdates = false,
            enableDvr = false,
            allowDvrDeveloperUnlock = true,
            guideOnlyReviewSurface = true
        )

        val current: StorePolicySnapshot
            get() = fromBuildConfig().effectiveFor(
                storedDeveloperModeEnabled = false,
                nowMs = System.currentTimeMillis()
            )

        val rawCurrent: StorePolicySnapshot
            get() = fromBuildConfig()

        private fun fromBuildConfig(): StorePolicySnapshot =
            StorePolicySnapshot(
                amazonReviewBuild = BuildConfig.AMAZON_REVIEW_BUILD,
                showAdvancedSourceTypes = BuildConfig.SHOW_ADVANCED_SOURCE_TYPES,
                showAdultSurfaces = BuildConfig.SHOW_ADULT_SURFACES,
                showWelcomeRoute = BuildConfig.SHOW_WELCOME_ROUTE,
                enableBundledPublicSource = BuildConfig.ENABLE_BUNDLED_PUBLIC_SOURCE,
                bundledPublicSources = parseBundledPublicSourceSpecs(BuildConfig.BUNDLED_PUBLIC_SOURCE_SPECS),
                allowXtreamPlaylistAutoDetection = BuildConfig.ALLOW_XTREAM_PLAYLIST_AUTO_DETECTION,
                enableSideloadUpdates = BuildConfig.ENABLE_SIDELOAD_UPDATES,
                enableDvr = BuildConfig.ENABLE_DVR,
                allowDvrDeveloperUnlock = BuildConfig.ALLOW_DVR_DEVELOPER_UNLOCK,
                guideOnlyReviewSurface = BuildConfig.AMAZON_REVIEW_BUILD,
                dateUnlocksHiddenFeatures = BuildConfig.DATE_UNLOCKS_HIDDEN_FEATURES,
                featureReleaseUnlockEpochMs = BuildConfig.FEATURE_RELEASE_UNLOCK_EPOCH_MS,
                premiumPreviewFreeUntilEpochMs = BuildConfig.PREMIUM_PREVIEW_FREE_UNTIL_EPOCH_MS
            )

        private fun parseBundledPublicSourceSpecs(rawSpecs: String): List<BundledPublicSourceSpec> =
            rawSpecs.split('|')
                .mapNotNull { rawSpec ->
                    val parts = rawSpec.split("::")
                    if (parts.size !in 5..8) return@mapNotNull null
                    val slot = runCatching { ProviderSourceSlot.valueOf(parts[3]) }.getOrNull()
                        ?: return@mapNotNull null
                    BundledPublicSourceSpec(
                        playlistAssetPath = parts[0].trim(),
                        playlistFileName = parts[1].trim(),
                        providerName = parts[2].trim().ifBlank { "AfterglowTV" },
                        sourceSlot = slot,
                        m3uVodClassificationEnabled = parts[4].trim().toBoolean(),
                        guideFileName = parts.getOrNull(5)?.trim().orEmpty()
                            .ifBlank { parts[1].trim().substringBeforeLast('.') + ".xml" },
                        playlistUrl = parts.getOrNull(6)?.trim().orEmpty(),
                        guideUrl = parts.getOrNull(7)?.trim().orEmpty()
                    ).takeIf { it.playlistAssetPath.isNotBlank() && it.playlistFileName.isNotBlank() }
                }
    }
}

object StorePolicy {
    @Volatile
    private var amazonPremiumEntitledForProcess = false

    val current: StorePolicySnapshot
        get() = currentFor(storedDeveloperModeEnabled = false)

    val rawCurrent: StorePolicySnapshot
        get() = StorePolicySnapshot.rawCurrent

    fun currentTimeMillis(): Long = System.currentTimeMillis()

    fun setAmazonPremiumEntitledForProcess(entitled: Boolean) {
        amazonPremiumEntitledForProcess = entitled
    }

    fun isAmazonPremiumEntitledForProcess(): Boolean = amazonPremiumEntitledForProcess

    fun currentFor(storedDeveloperModeEnabled: Boolean, nowMs: Long = currentTimeMillis()): StorePolicySnapshot =
        currentFor(
            storedDeveloperModeEnabled = storedDeveloperModeEnabled,
            amazonPremiumEntitled = amazonPremiumEntitledForProcess,
            nowMs = nowMs
        )

    fun currentFor(
        storedDeveloperModeEnabled: Boolean,
        amazonPremiumEntitled: Boolean,
        nowMs: Long = currentTimeMillis()
    ): StorePolicySnapshot =
        rawCurrent.effectiveFor(
            storedDeveloperModeEnabled = storedDeveloperModeEnabled,
            amazonPremiumEntitled = amazonPremiumEntitled,
            nowMs = nowMs
        )

    fun effectiveDeveloperModeEnabled(storedDeveloperModeEnabled: Boolean, nowMs: Long = currentTimeMillis()): Boolean =
        effectiveDeveloperModeEnabled(
            storedDeveloperModeEnabled = storedDeveloperModeEnabled,
            amazonPremiumEntitled = amazonPremiumEntitledForProcess,
            nowMs = nowMs
        )

    fun effectiveDeveloperModeEnabled(
        storedDeveloperModeEnabled: Boolean,
        amazonPremiumEntitled: Boolean,
        nowMs: Long = currentTimeMillis()
    ): Boolean =
        rawCurrent.effectiveDeveloperModeEnabled(
            storedDeveloperModeEnabled = storedDeveloperModeEnabled,
            amazonPremiumEntitled = amazonPremiumEntitled,
            nowMs = nowMs
        )
}
