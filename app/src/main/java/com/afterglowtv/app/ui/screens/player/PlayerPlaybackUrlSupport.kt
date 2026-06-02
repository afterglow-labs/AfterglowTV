package com.afterglowtv.app.ui.screens.player

import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.StreamInfo
import com.afterglowtv.player.PlaybackState

internal fun String?.safeTrimmedOrNull(): String? {
    val value = this ?: return null
    return value.trim().takeIf { it.isNotEmpty() }
}

internal fun resolveTimeshiftStreamInfo(
    streamInfoOverride: StreamInfo?,
    currentResolvedStreamInfo: StreamInfo?,
    currentResolvedPlaybackUrl: String?,
    currentStreamUrl: String?,
    playbackTitle: String,
    currentTitle: String
): StreamInfo? {
    val resolvedTitle = playbackTitle.ifBlank { currentTitle }
    streamInfoOverride?.let { override ->
        return override.copy(title = override.title ?: resolvedTitle)
    }
    currentResolvedStreamInfo?.let { resolved ->
        val resolvedUrl = resolved.url.safeTrimmedOrNull()
        if (resolvedUrl != null) {
            return resolved.copy(
                url = resolvedUrl,
                title = resolved.title ?: resolvedTitle
            )
        }
    }
    val fallbackUrl = currentResolvedPlaybackUrl.safeTrimmedOrNull()
        ?: currentStreamUrl.safeTrimmedOrNull()
        ?: return null
    return StreamInfo(url = fallbackUrl, title = resolvedTitle)
}

internal fun resolvePlaybackIdentityUrl(
    currentResolvedPlaybackUrl: String?,
    currentStreamUrl: String?
): String = currentResolvedPlaybackUrl.safeTrimmedOrNull()
    ?: currentStreamUrl.safeTrimmedOrNull()
    ?: ""

internal fun resolvePlaybackProbeCacheKey(
    currentStreamUrl: String?,
    url: String
): String = currentStreamUrl.safeTrimmedOrNull() ?: url

internal fun matchesActivePlaybackSession(
    requestVersion: Long,
    activeRequestVersion: Long,
    expectedLogicalUrl: String? = null,
    currentResolvedPlaybackUrl: String?,
    currentStreamUrl: String?
): Boolean {
    if (requestVersion != activeRequestVersion) return false
    val expectedUrl = expectedLogicalUrl?.takeIf { it.isNotBlank() } ?: return true
    val activeUrl = resolvePlaybackIdentityUrl(
        currentResolvedPlaybackUrl = currentResolvedPlaybackUrl,
        currentStreamUrl = currentStreamUrl
    )
    return activeUrl.isBlank() || activeUrl == expectedUrl || currentStreamUrl.safeTrimmedOrNull() == expectedUrl
}

internal fun shouldReuseActivePlaybackForRoute(
    hasArchiveRequest: Boolean,
    prepareRequestVersion: Long,
    playbackState: PlaybackState,
    currentContentType: ContentType,
    requestedContentType: String,
    currentContentId: Long,
    requestedContentId: Long,
    currentProviderId: Long,
    requestedProviderId: Long,
    currentStreamUrl: String?,
    currentResolvedPlaybackUrl: String?,
    requestedStreamUrl: String
): Boolean {
    if (hasArchiveRequest || prepareRequestVersion <= 0L) return false
    if (playbackState != PlaybackState.READY && playbackState != PlaybackState.BUFFERING) return false
    val requestedType = runCatching { ContentType.valueOf(requestedContentType) }
        .getOrDefault(ContentType.LIVE)
    if (currentContentType != requestedType) return false
    if (currentContentId != requestedContentId) return false
    if (currentProviderId != requestedProviderId) return false
    val activeUrl = resolvePlaybackIdentityUrl(
        currentResolvedPlaybackUrl = currentResolvedPlaybackUrl,
        currentStreamUrl = currentStreamUrl
    )
    return activeUrl == requestedStreamUrl || currentStreamUrl.safeTrimmedOrNull() == requestedStreamUrl
}
