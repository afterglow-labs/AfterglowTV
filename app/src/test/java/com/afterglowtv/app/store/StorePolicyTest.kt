package com.afterglowtv.app.store

import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.app.navigation.resolveStartupRoute
import com.afterglowtv.app.ui.screens.provider.ProviderSetupSourceType
import com.afterglowtv.app.ui.screens.provider.visibleProviderSetupSourceTypes
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderSourceSlot
import com.afterglowtv.domain.model.ProviderType
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
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
    fun `amazon build uses AfterglowTV package identity`() {
        if (BuildConfig.AMAZON_REVIEW_BUILD) {
            assertThat(BuildConfig.APPLICATION_ID).isAnyOf(
                "com.afterglowtv.app.amazon",
                "com.afterglowtv.app.direct"
            )
            assertThat(BuildConfig.OFFICIAL_APPLICATION_ID).isEqualTo("com.afterglowtv.app")
        } else {
            assertThat(BuildConfig.APPLICATION_ID).startsWith("com.afterglowtv.app")
            assertThat(BuildConfig.OFFICIAL_APPLICATION_ID).isEqualTo("com.afterglowtv.app")
        }
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
    fun `direct preview matches amazon hidden defaults before release date`() {
        val beforeRelease = utcMs("2026-06-30T23:59:59Z")
        val locked = StorePolicySnapshot.direct.effectiveFor(
            storedDeveloperModeEnabled = false,
            nowMs = beforeRelease
        )

        assertThat(locked.showAdvancedSourceTypes).isFalse()
        assertThat(locked.canUseDvr(developerModeEnabled = false)).isFalse()
        assertThat(StorePolicySnapshot.direct.effectiveDeveloperModeEnabled(false, beforeRelease)).isFalse()
        assertThat(visibleProviderSetupSourceTypes(locked)).containsExactly(
            ProviderSetupSourceType.PLAYLIST_URL,
            ProviderSetupSourceType.PLAYLIST_FILE
        ).inOrder()
    }

    @Test
    fun `direct preview developer mode unlocks full feature set before release date`() {
        val beforeRelease = utcMs("2026-06-30T23:59:59Z")
        val unlocked = StorePolicySnapshot.direct.effectiveFor(
            storedDeveloperModeEnabled = true,
            nowMs = beforeRelease
        )

        assertThat(unlocked.showAdvancedSourceTypes).isTrue()
        assertThat(unlocked.canUseDvr(developerModeEnabled = false)).isTrue()
        assertThat(StorePolicySnapshot.direct.effectiveDeveloperModeEnabled(true, beforeRelease)).isTrue()
        assertThat(visibleProviderSetupSourceTypes(unlocked)).containsExactly(
            ProviderSetupSourceType.SERVER_LOGIN,
            ProviderSetupSourceType.PORTAL_LOGIN,
            ProviderSetupSourceType.PLAYLIST_URL,
            ProviderSetupSourceType.PLAYLIST_FILE
        ).inOrder()
    }

    @Test
    fun `direct preview unlocks full feature set on release date`() {
        val releaseDate = utcMs("2026-07-01T00:00:00Z")
        val unlocked = StorePolicySnapshot.direct.effectiveFor(
            storedDeveloperModeEnabled = false,
            nowMs = releaseDate
        )

        assertThat(unlocked.showAdvancedSourceTypes).isTrue()
        assertThat(unlocked.allowXtreamPlaylistAutoDetection).isTrue()
        assertThat(unlocked.canUseDvr(developerModeEnabled = false)).isTrue()
        assertThat(StorePolicySnapshot.direct.effectiveDeveloperModeEnabled(false, releaseDate)).isTrue()
        assertThat(visibleProviderSetupSourceTypes(unlocked)).containsExactly(
            ProviderSetupSourceType.SERVER_LOGIN,
            ProviderSetupSourceType.PORTAL_LOGIN,
            ProviderSetupSourceType.PLAYLIST_URL,
            ProviderSetupSourceType.PLAYLIST_FILE
        ).inOrder()
    }

    @Test
    fun `amazon does not unlock full feature set by date`() {
        val releaseDate = utcMs("2026-07-01T00:00:00Z")
        val amazon = StorePolicySnapshot.amazon.effectiveFor(
            storedDeveloperModeEnabled = false,
            nowMs = releaseDate
        )

        assertThat(amazon.showAdvancedSourceTypes).isFalse()
        assertThat(amazon.canUseDvr(developerModeEnabled = false)).isFalse()
        assertThat(StorePolicySnapshot.amazon.effectiveDeveloperModeEnabled(false, releaseDate)).isFalse()
    }

    @Test
    fun `direct premium preview free window ends on october first`() {
        assertThat(StorePolicySnapshot.direct.isPremiumPreviewFree(utcMs("2026-09-30T23:59:59Z"))).isTrue()
        assertThat(StorePolicySnapshot.direct.isPremiumPreviewFree(utcMs("2026-10-01T00:00:00Z"))).isFalse()
    }

    @Test
    fun `amazon review build hides dvr unless developer mode unlocks it`() {
        assertThat(StorePolicySnapshot.amazon.canUseDvr(developerModeEnabled = false)).isFalse()
        assertThat(StorePolicySnapshot.amazon.canUseDvr(developerModeEnabled = true)).isTrue()
        assertThat(StorePolicySnapshot.standard.canUseDvr(developerModeEnabled = false)).isTrue()
    }

    @Test
    fun `amazon startup allows adult route only after developer mode unlock`() {
        val lockedRoute = resolveStartupRoute(
            destination = StartupDestination.ADULT,
            developerModeEnabled = false,
            policy = StorePolicySnapshot.amazon
        )
        val route = resolveStartupRoute(
            destination = StartupDestination.ADULT,
            developerModeEnabled = true,
            policy = StorePolicySnapshot.amazon
        )

        assertThat(lockedRoute).isEqualTo(Routes.HOME)
        assertThat(route).isEqualTo(Routes.ADULT)
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

    @Test
    fun `amazon fallback repairs stale active source when no user source exists`() {
        val fallback = provider(
            id = 2L,
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/hidden_fallback/afterglow_amazon_live.m3u8"
        )

        assertThat(
            shouldUseHiddenFallbackSourceForSlot(
                policy = StorePolicySnapshot.amazon,
                providers = listOf(fallback),
                currentSource = ActiveLiveSource.ProviderSource(1L),
                fallbackProviderId = fallback.id
            )
        ).isTrue()
    }

    @Test
    fun `amazon fallback keeps matching active source when no user source exists`() {
        val fallback = provider(
            id = 2L,
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/hidden_fallback/afterglow_amazon_live.m3u8"
        )

        assertThat(
            shouldUseHiddenFallbackSourceForSlot(
                policy = StorePolicySnapshot.amazon,
                providers = listOf(fallback),
                currentSource = ActiveLiveSource.ProviderSource(fallback.id),
                fallbackProviderId = fallback.id
            )
        ).isFalse()
    }

    @Test
    fun `amazon fallback keeps user source active`() {
        val userProvider = provider(id = 7L, m3uUrl = "https://example.test/user.m3u8")

        assertThat(
            shouldUseHiddenFallbackSourceForSlot(
                policy = StorePolicySnapshot.amazon,
                providers = listOf(userProvider),
                currentSource = ActiveLiveSource.ProviderSource(userProvider.id),
                fallbackProviderId = 2L
            )
        ).isFalse()
    }

    @Test
    fun `amazon bundled fallback playlists avoid automatic epg discovery`() {
        assertThat(amazonAsset("playlist_usa.m3u8").readText()).doesNotContain("x-tvg-url")
        assertThat(amazonAsset("playlist_usa_vod.m3u8").readText()).doesNotContain("x-tvg-url")
    }

    @Test
    fun `amazon bundled live fallback contains controlled demo and public channels`() {
        val livePlaylist = amazonAsset("playlist_usa.m3u8").readText()

        assertThat(livePlaylist).contains("Afterglow Music Demo")
        assertThat(livePlaylist).contains("media.boni-records.com/index.m3u8")
        assertThat(livePlaylist).contains("Access Media Productions Channel")
        assertThat(livePlaylist).contains("Access Nashua")
        assertThat(livePlaylist).contains("AccuWeather Now")
        assertThat(livePlaylist).contains("AFTV")
    }

    @Test
    fun `amazon bundled live fallback excludes unsupported or review risk sources`() {
        val livePlaylist = amazonAsset("playlist_usa.m3u8").readText()

        assertThat(livePlaylist).doesNotContain("3catdirectes.cat")
        assertThat(livePlaylist).doesNotContain("ztnr.rtve.es")
        assertThat(livePlaylist).doesNotContain("30a-tv.com")
        assertThat(livePlaylist).doesNotContain("ADN TV+")
        assertThat(livePlaylist).doesNotContain("Anime Vision")
        assertThat(livePlaylist).doesNotContain("Canal 24 Horas")
        assertThat(livePlaylist).doesNotContain("Afghan")
        assertThat(livePlaylist).doesNotContain("Afghanistan")
        assertThat(livePlaylist).doesNotContain("Africa")
        assertThat(livePlaylist).doesNotContain("Afrique")
        assertThat(livePlaylist).doesNotContain("AfroLand")
        assertThat(livePlaylist).doesNotContain("1KZN")
        assertThat(livePlaylist).doesNotContain(".za@")
        assertThat(livePlaylist).doesNotContain(".cm@")
        assertThat(livePlaylist).doesNotContain("Abante")
        assertThat(livePlaylist).doesNotContain("ABP News")
        assertThat(livePlaylist).doesNotContain("10 Bold")
        assertThat(livePlaylist).doesNotContain("Acheloos")
        assertThat(livePlaylist).doesNotContain("A2i TV")
        assertThat(livePlaylist).doesNotContain("[Geo-blocked]")
        assertThat(livePlaylist).doesNotContain("youtube.com")
        assertThat(livePlaylist).doesNotContain("Disney")
        assertThat(livePlaylist).doesNotContain("Naruto")
        assertThat(livePlaylist).doesNotContain("XXX")
        assertThat(livePlaylist).doesNotContain("xxx.m3u")
        assertThat(livePlaylist).doesNotContain("Undefined")
        assertThat(livePlaylist).doesNotContain("buzzrota-ono.amagi.tv")
        assertThat(livePlaylist).doesNotContain("bcovlive-a.akamaihd.net")
        assertThat(livePlaylist).doesNotContain("tve-live-lln.warnermediacdn.com")
        assertThat(livePlaylist).doesNotContain("ntv1.akamaized.net")
        assertThat(livePlaylist).doesNotContain("ntv2.akamaized.net")
        assertThat(livePlaylist).doesNotContain("service-stitcher.clusters.pluto.tv")
    }

    @Test
    fun `amazon bundled vod fallback contains direct video files`() {
        val vodPlaylist = amazonAsset("playlist_usa_vod.m3u8").readText()

        assertThat(vodPlaylist).contains("group-title=\"Demo Videos\"")
        assertThat(vodPlaylist).containsMatch("""https://[^\n]+\.(m4v|mp4|mov)""")
    }

    private fun provider(id: Long = 1L, m3uUrl: String): Provider =
        Provider(
            id = id,
            name = "Source $id",
            type = ProviderType.M3U,
            serverUrl = m3uUrl,
            m3uUrl = m3uUrl
        )

    private fun amazonAsset(fileName: String): File =
        File("src/amazon/assets/amazon_fallback/$fileName")

    private fun utcMs(value: String): Long = Instant.parse(value).toEpochMilli()
}
