package com.afterglowtv.app.store

import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.app.navigation.resolveStartupRoute
import com.afterglowtv.app.ui.screens.provider.ProviderSetupSourceType
import com.afterglowtv.app.ui.screens.provider.visibleProviderSetupSourceTypes
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderSourceSlot
import com.afterglowtv.domain.model.ProviderType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StorePolicyTest {
    @Test
    fun `amazon source setup exposes only playlist choices`() {
        val sourceTypes = visibleProviderSetupSourceTypes(StorePolicySnapshot.amazon)

        assertThat(sourceTypes).containsExactly(
            ProviderSetupSourceType.PLAYLIST_URL,
            ProviderSetupSourceType.PLAYLIST_FILE
        ).inOrder()
    }

    @Test
    fun `standard source setup exposes all source choices`() {
        val sourceTypes = visibleProviderSetupSourceTypes(StorePolicySnapshot.standard)

        assertThat(sourceTypes).containsExactly(
            ProviderSetupSourceType.SERVER_LOGIN,
            ProviderSetupSourceType.PORTAL_LOGIN,
            ProviderSetupSourceType.PLAYLIST_URL,
            ProviderSetupSourceType.PLAYLIST_FILE
        ).inOrder()
    }

    @Test
    fun `amazon startup blocks adult route even when developer mode is enabled`() {
        val route = resolveStartupRoute(
            destination = StartupDestination.XXX_GUIDE,
            developerModeEnabled = true,
            policy = StorePolicySnapshot.amazon
        )

        assertThat(route).isEqualTo(Routes.HOME)
    }

    @Test
    fun `amazon hides fallback source from user source lists`() {
        val fallback = provider(
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/hidden_fallback/afterglow_amazon_live.m3u8"
        )
        val userProvider = provider(id = 2L, m3uUrl = "https://example.test/user.m3u8")

        val visibleProviders = listOf(fallback, userProvider)
            .filter { StorePolicySnapshot.amazon.isUserVisibleProvider(it) }

        assertThat(visibleProviders).containsExactly(userProvider)
    }

    @Test
    fun `amazon should seed fallback only when no user source exists`() {
        val fallback = provider(
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/hidden_fallback/afterglow_amazon_vod.m3u8"
        )
        val userProvider = provider(id = 2L, m3uUrl = "https://example.test/user.m3u8")

        assertThat(StorePolicySnapshot.amazon.shouldEnsureHiddenFallback(emptyList())).isTrue()
        assertThat(StorePolicySnapshot.amazon.shouldEnsureHiddenFallback(listOf(fallback))).isTrue()
        assertThat(StorePolicySnapshot.amazon.shouldEnsureHiddenFallback(listOf(fallback, userProvider))).isFalse()
    }

    @Test
    fun `amazon fallback source uses bundled playlists instead of remote url`() {
        val policy = StorePolicySnapshot.amazon

        assertThat(policy.hiddenFallbackSources.map { it.assetPath }).containsExactly(
            "amazon_fallback/playlist_usa.m3u8",
            "amazon_fallback/playlist_usa_vod.m3u8"
        ).inOrder()
        assertThat(policy.hiddenFallbackSources.map { it.providerFileName }).containsExactly(
            "afterglow_amazon_live.m3u8",
            "afterglow_amazon_vod.m3u8"
        ).inOrder()
        assertThat(policy.hiddenFallbackSources.map { it.sourceSlot }).containsExactly(
            ProviderSourceSlot.LIVE,
            ProviderSourceSlot.VOD
        ).inOrder()
        assertThat(policy.hiddenFallbackSources.map { it.m3uVodClassificationEnabled }).containsExactly(
            false,
            true
        ).inOrder()
    }

    private fun provider(id: Long = 1L, m3uUrl: String): Provider =
        Provider(
            id = id,
            name = "Source $id",
            type = ProviderType.M3U,
            serverUrl = m3uUrl,
            m3uUrl = m3uUrl
        )
}
