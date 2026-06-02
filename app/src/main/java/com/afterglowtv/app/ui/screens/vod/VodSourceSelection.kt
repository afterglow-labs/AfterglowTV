package com.afterglowtv.app.ui.screens.vod

import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Provider

internal fun selectVodProvider(
    activeVodSource: ActiveLiveSource?,
    activeProvider: Provider?,
    providers: List<Provider>
): Provider? {
    val activeVodProviderId = (activeVodSource as? ActiveLiveSource.ProviderSource)?.providerId
    return activeVodProviderId?.let { providerId ->
        providers.firstOrNull { provider -> provider.id == providerId }
    } ?: activeProvider
}
