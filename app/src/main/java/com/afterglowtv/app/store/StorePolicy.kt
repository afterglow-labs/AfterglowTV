package com.afterglowtv.app.store

import com.afterglowtv.app.BuildConfig
import com.afterglowtv.domain.model.Provider

private const val AMAZON_FALLBACK_PLAYLIST_URL =
    "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"

data class StorePolicySnapshot(
    val amazonReviewBuild: Boolean,
    val showAdvancedSourceTypes: Boolean,
    val showAdultSurfaces: Boolean,
    val showWelcomeRoute: Boolean,
    val enableHiddenFallbackSource: Boolean,
    val hiddenFallbackPlaylistUrl: String?,
    val allowXtreamPlaylistAutoDetection: Boolean,
    val enableSideloadUpdates: Boolean
) {
    fun isHiddenFallbackProvider(provider: Provider): Boolean {
        val fallbackUrl = hiddenFallbackPlaylistUrl?.takeIf { it.isNotBlank() } ?: return false
        return enableHiddenFallbackSource &&
            (provider.m3uUrl == fallbackUrl || provider.serverUrl == fallbackUrl)
    }

    fun isUserVisibleProvider(provider: Provider): Boolean =
        !isHiddenFallbackProvider(provider)

    fun shouldEnsureHiddenFallback(providers: List<Provider>): Boolean =
        enableHiddenFallbackSource &&
            !hiddenFallbackPlaylistUrl.isNullOrBlank() &&
            providers.none(::isUserVisibleProvider)

    companion object {
        val standard = StorePolicySnapshot(
            amazonReviewBuild = false,
            showAdvancedSourceTypes = true,
            showAdultSurfaces = true,
            showWelcomeRoute = true,
            enableHiddenFallbackSource = false,
            hiddenFallbackPlaylistUrl = null,
            allowXtreamPlaylistAutoDetection = true,
            enableSideloadUpdates = true
        )

        val amazon = StorePolicySnapshot(
            amazonReviewBuild = true,
            showAdvancedSourceTypes = false,
            showAdultSurfaces = false,
            showWelcomeRoute = false,
            enableHiddenFallbackSource = true,
            hiddenFallbackPlaylistUrl = AMAZON_FALLBACK_PLAYLIST_URL,
            allowXtreamPlaylistAutoDetection = false,
            enableSideloadUpdates = false
        )

        val current: StorePolicySnapshot
            get() = StorePolicySnapshot(
                amazonReviewBuild = BuildConfig.AMAZON_REVIEW_BUILD,
                showAdvancedSourceTypes = BuildConfig.SHOW_ADVANCED_SOURCE_TYPES,
                showAdultSurfaces = BuildConfig.SHOW_ADULT_SURFACES,
                showWelcomeRoute = BuildConfig.SHOW_WELCOME_ROUTE,
                enableHiddenFallbackSource = BuildConfig.ENABLE_HIDDEN_FALLBACK_SOURCE,
                hiddenFallbackPlaylistUrl = BuildConfig.HIDDEN_FALLBACK_PLAYLIST_URL.takeIf { it.isNotBlank() },
                allowXtreamPlaylistAutoDetection = BuildConfig.ALLOW_XTREAM_PLAYLIST_AUTO_DETECTION,
                enableSideloadUpdates = BuildConfig.ENABLE_SIDELOAD_UPDATES
            )
    }
}

object StorePolicy {
    val current: StorePolicySnapshot
        get() = StorePolicySnapshot.current
}
