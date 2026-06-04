package com.afterglowtv.app.store

import com.afterglowtv.app.BuildConfig
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderSourceSlot

data class HiddenFallbackSourceSpec(
    val assetPath: String,
    val providerFileName: String,
    val providerName: String,
    val sourceSlot: ProviderSourceSlot,
    val m3uVodClassificationEnabled: Boolean
)

data class StorePolicySnapshot(
    val amazonReviewBuild: Boolean,
    val showAdvancedSourceTypes: Boolean,
    val showAdultSurfaces: Boolean,
    val showWelcomeRoute: Boolean,
    val enableHiddenFallbackSource: Boolean,
    val hiddenFallbackSources: List<HiddenFallbackSourceSpec>,
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

    fun effectiveDeveloperModeEnabled(storedDeveloperModeEnabled: Boolean, nowMs: Long): Boolean =
        storedDeveloperModeEnabled || isFeatureReleaseUnlocked(nowMs)

    fun effectiveFor(storedDeveloperModeEnabled: Boolean, nowMs: Long): StorePolicySnapshot =
        if (amazonReviewBuild && effectiveDeveloperModeEnabled(storedDeveloperModeEnabled, nowMs)) {
            copy(
                showAdvancedSourceTypes = true,
                showAdultSurfaces = true,
                guideOnlyReviewSurface = false,
                allowXtreamPlaylistAutoDetection = true,
                enableSideloadUpdates = true,
                enableDvr = true
            )
        } else {
            this
        }

    fun isHiddenFallbackProvider(provider: Provider): Boolean {
        return enableHiddenFallbackSource &&
            hiddenFallbackSources.any { spec ->
                val hiddenPathMarker = "/$HIDDEN_FALLBACK_DIRECTORY/${spec.providerFileName}"
                provider.m3uUrl.isHiddenFallbackUrl(spec.providerFileName, hiddenPathMarker) ||
                    provider.serverUrl.isHiddenFallbackUrl(spec.providerFileName, hiddenPathMarker)
            }
    }

    fun isUserVisibleProvider(provider: Provider): Boolean =
        !isHiddenFallbackProvider(provider)

    fun shouldEnsureHiddenFallback(providers: List<Provider>): Boolean =
        enableHiddenFallbackSource &&
            hiddenFallbackSources.isNotEmpty() &&
            providers.none(::isUserVisibleProvider)

    private fun String.isHiddenFallbackUrl(fileName: String, hiddenPathMarker: String): Boolean =
        contains(hiddenPathMarker) || endsWith("/$fileName") || this == fileName

    companion object {
        const val HIDDEN_FALLBACK_DIRECTORY = "hidden_fallback"
        const val DIRECT_PREVIEW_UNLOCK_EPOCH_MS = 1_782_864_000_000L
        const val DIRECT_PREVIEW_FREE_UNTIL_EPOCH_MS = 1_790_812_800_000L

        val standard = StorePolicySnapshot(
            amazonReviewBuild = false,
            showAdvancedSourceTypes = true,
            showAdultSurfaces = true,
            showWelcomeRoute = true,
            enableHiddenFallbackSource = false,
            hiddenFallbackSources = emptyList(),
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
            enableHiddenFallbackSource = true,
            hiddenFallbackSources = listOf(
                HiddenFallbackSourceSpec(
                    assetPath = "amazon_fallback/playlist_usa.m3u8",
                    providerFileName = "afterglow_amazon_live.m3u8",
                    providerName = "AfterglowTV",
                    sourceSlot = ProviderSourceSlot.LIVE,
                    m3uVodClassificationEnabled = false
                )
            ),
            allowXtreamPlaylistAutoDetection = false,
            enableSideloadUpdates = false,
            enableDvr = false,
            allowDvrDeveloperUnlock = true,
            guideOnlyReviewSurface = true
        )

        val direct = amazon.copy(
            dateUnlocksHiddenFeatures = true,
            featureReleaseUnlockEpochMs = DIRECT_PREVIEW_UNLOCK_EPOCH_MS,
            premiumPreviewFreeUntilEpochMs = DIRECT_PREVIEW_FREE_UNTIL_EPOCH_MS
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
                enableHiddenFallbackSource = BuildConfig.ENABLE_HIDDEN_FALLBACK_SOURCE,
                hiddenFallbackSources = parseHiddenFallbackSourceSpecs(BuildConfig.HIDDEN_FALLBACK_SOURCE_SPECS),
                allowXtreamPlaylistAutoDetection = BuildConfig.ALLOW_XTREAM_PLAYLIST_AUTO_DETECTION,
                enableSideloadUpdates = BuildConfig.ENABLE_SIDELOAD_UPDATES,
                enableDvr = BuildConfig.ENABLE_DVR,
                allowDvrDeveloperUnlock = BuildConfig.ALLOW_DVR_DEVELOPER_UNLOCK,
                guideOnlyReviewSurface = BuildConfig.AMAZON_REVIEW_BUILD,
                dateUnlocksHiddenFeatures = BuildConfig.DATE_UNLOCKS_HIDDEN_FEATURES,
                featureReleaseUnlockEpochMs = BuildConfig.FEATURE_RELEASE_UNLOCK_EPOCH_MS,
                premiumPreviewFreeUntilEpochMs = BuildConfig.PREMIUM_PREVIEW_FREE_UNTIL_EPOCH_MS
            )

        private fun parseHiddenFallbackSourceSpecs(rawSpecs: String): List<HiddenFallbackSourceSpec> =
            rawSpecs.split('|')
                .mapNotNull { rawSpec ->
                    val parts = rawSpec.split("::")
                    if (parts.size != 5) return@mapNotNull null
                    val slot = runCatching { ProviderSourceSlot.valueOf(parts[3]) }.getOrNull()
                        ?: return@mapNotNull null
                    HiddenFallbackSourceSpec(
                        assetPath = parts[0].trim(),
                        providerFileName = parts[1].trim(),
                        providerName = parts[2].trim().ifBlank { "AfterglowTV" },
                        sourceSlot = slot,
                        m3uVodClassificationEnabled = parts[4].trim().toBoolean()
                    ).takeIf { it.assetPath.isNotBlank() && it.providerFileName.isNotBlank() }
                }
    }
}

object StorePolicy {
    val current: StorePolicySnapshot
        get() = StorePolicySnapshot.current

    val rawCurrent: StorePolicySnapshot
        get() = StorePolicySnapshot.rawCurrent

    fun currentTimeMillis(): Long = System.currentTimeMillis()

    fun currentFor(storedDeveloperModeEnabled: Boolean, nowMs: Long = currentTimeMillis()): StorePolicySnapshot =
        rawCurrent.effectiveFor(storedDeveloperModeEnabled, nowMs)

    fun effectiveDeveloperModeEnabled(storedDeveloperModeEnabled: Boolean, nowMs: Long = currentTimeMillis()): Boolean =
        rawCurrent.effectiveDeveloperModeEnabled(storedDeveloperModeEnabled, nowMs)
}
