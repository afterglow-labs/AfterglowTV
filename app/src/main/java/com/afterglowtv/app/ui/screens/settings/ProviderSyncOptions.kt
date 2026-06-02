package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderType

internal fun Provider.availableSyncSelections(): List<ProviderSyncSelection> = when {
    isVodOnlyM3uProvider() -> listOf(
        ProviderSyncSelection.VOD,
        ProviderSyncSelection.EPG
    )
    type == ProviderType.XTREAM_CODES -> listOf(
        ProviderSyncSelection.TV,
        ProviderSyncSelection.VOD,
        ProviderSyncSelection.SERIES,
        ProviderSyncSelection.EPG
    )
    else -> listOf(
        ProviderSyncSelection.TV,
        ProviderSyncSelection.EPG
    )
}

internal fun Provider.defaultCustomSyncSelections(): Set<ProviderSyncSelection> =
    availableSyncSelections().toSet()

internal fun orderedProviderSyncSelections(selections: Set<ProviderSyncSelection>): List<ProviderSyncSelection> =
    listOf(
        ProviderSyncSelection.TV,
        ProviderSyncSelection.VOD,
        ProviderSyncSelection.MOVIES,
        ProviderSyncSelection.SERIES,
        ProviderSyncSelection.EPG
    ).filter { it in selections }

internal fun Provider?.syncNowSelections(): List<ProviderSyncSelection> =
    this?.availableSyncSelections() ?: listOf(
        ProviderSyncSelection.TV,
        ProviderSyncSelection.VOD,
        ProviderSyncSelection.EPG
    )

internal fun Provider.isVodOnlyM3uProvider(): Boolean =
    type == ProviderType.M3U && m3uPlaylistKind == ProviderM3uPlaylistKind.VOD
