package com.afterglowtv.domain.model

object BuiltInPlaylists {
    const val FREE_BROADCASTS_NAME = "Free Broadcasts"
    const val FREE_BROADCASTS_URL = "https://iptv-org.github.io/iptv/languages/eng.m3u"

    fun isBuiltInProvider(provider: Provider): Boolean =
        provider.type == ProviderType.M3U &&
            provider.m3uUrl.equals(FREE_BROADCASTS_URL, ignoreCase = true)
}
