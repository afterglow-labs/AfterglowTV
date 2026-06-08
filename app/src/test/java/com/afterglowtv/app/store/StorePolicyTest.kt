package com.afterglowtv.app.store

import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.app.navigation.resolveStartupRoute
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.store.amazon.AfterglowIapConfig
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
    fun `locked amazon source dialog exposes supported source choices`() {
        val sourceTypes = visibleSourceDialogChoices(StorePolicySnapshot.amazon)

        assertThat(sourceTypes).containsExactly(
            SourceDialogChoice.PLAYLIST,
            SourceDialogChoice.XTREAM,
            SourceDialogChoice.PORTAL
        ).inOrder()
    }

    @Test
    fun `amazon build uses AfterglowTV package identity`() {
        assertThat(BuildConfig.APPLICATION_ID).isEqualTo("com.afterglowtv.app")
        assertThat(BuildConfig.OFFICIAL_APPLICATION_ID).isEqualTo("com.afterglowtv.app")
    }

    @Test
    fun `amazon appstore sdk is enabled only for fire builds`() {
        if (BuildConfig.AMAZON_REVIEW_BUILD) {
            assertThat(BuildConfig.ENABLE_AMAZON_APPSTORE_SDK).isTrue()
            assertThat(BuildConfig.ENABLE_AMAZON_DRM_LICENSING).isTrue()
            assertThat(BuildConfig.AMAZON_PREMIUM_MONTHLY_SKU).isEqualTo("com.afterglowtv.app.premium.monthly.v1")
            assertThat(BuildConfig.AMAZON_PREMIUM_QUARTERLY_SKU).isEqualTo("com.afterglowtv.app.premium.quarterly.v1")
            assertThat(BuildConfig.AMAZON_PREMIUM_ANNUALLY_SKU).isEqualTo("com.afterglowtv.app.premium.annually.v1")
            assertThat(BuildConfig.AMAZON_PREMIUM_LIFETIME_SKU).isEqualTo("com.afterglowtv.app.premium.lifetime.v1")
            assertThat(AfterglowIapConfig.productionSkus).containsExactly(
                "com.afterglowtv.app.premium.lifetime.v1",
                "com.afterglowtv.app.premium.monthly.v1",
                "com.afterglowtv.app.premium.quarterly.v1",
                "com.afterglowtv.app.premium.annually.v1"
            )
            assertThat(AfterglowIapConfig.testSkus).containsExactly(
                "com.afterglowtv.app.premium.monthly",
                "com.afterglowtv.app.premium.yearly"
            )
        } else {
            assertThat(BuildConfig.ENABLE_AMAZON_APPSTORE_SDK).isFalse()
            assertThat(BuildConfig.ENABLE_AMAZON_DRM_LICENSING).isFalse()
            assertThat(BuildConfig.AMAZON_PREMIUM_MONTHLY_SKU).isEmpty()
            assertThat(BuildConfig.AMAZON_PREMIUM_QUARTERLY_SKU).isEmpty()
            assertThat(BuildConfig.AMAZON_PREMIUM_ANNUALLY_SKU).isEmpty()
            assertThat(BuildConfig.AMAZON_PREMIUM_LIFETIME_SKU).isEmpty()
        }
    }

    @Test
    fun `full feature source dialog exposes all source choices`() {
        val sourceTypes = visibleSourceDialogChoices(StorePolicySnapshot.fullFeature)

        assertThat(sourceTypes).containsExactly(
            SourceDialogChoice.PLAYLIST,
            SourceDialogChoice.XTREAM,
            SourceDialogChoice.PORTAL
        ).inOrder()
    }

    @Test
    fun `amazon review build matches hidden defaults`() {
        val now = utcMs("2026-07-14T23:59:59Z")
        val locked = StorePolicySnapshot.amazon.effectiveFor(
            storedDeveloperModeEnabled = false,
            nowMs = now
        )

        assertThat(locked.showAdvancedSourceTypes).isFalse()
        assertThat(locked.guideOnlyReviewSurface).isTrue()
        assertThat(locked.canUseDvr(developerModeEnabled = false)).isFalse()
        assertThat(StorePolicySnapshot.amazon.effectiveDeveloperModeEnabled(false, now)).isFalse()
        assertThat(visibleSourceDialogChoices(locked)).containsExactly(
            SourceDialogChoice.PLAYLIST,
            SourceDialogChoice.XTREAM,
            SourceDialogChoice.PORTAL
        ).inOrder()
    }

    @Test
    fun `amazon developer mode unlocks full feature set`() {
        val now = utcMs("2026-07-14T23:59:59Z")
        val unlocked = StorePolicySnapshot.amazon.effectiveFor(
            storedDeveloperModeEnabled = true,
            nowMs = now
        )

        assertThat(unlocked.showAdvancedSourceTypes).isTrue()
        assertThat(unlocked.guideOnlyReviewSurface).isFalse()
        assertThat(unlocked.canUseDvr(developerModeEnabled = false)).isTrue()
        assertThat(StorePolicySnapshot.amazon.effectiveDeveloperModeEnabled(true, now)).isTrue()
        assertThat(visibleSourceDialogChoices(unlocked)).containsExactly(
            SourceDialogChoice.PLAYLIST,
            SourceDialogChoice.XTREAM,
            SourceDialogChoice.PORTAL
        ).inOrder()
    }

    @Test
    fun `amazon does not unlock full feature set by date`() {
        val releaseDate = utcMs("2026-07-15T00:00:00Z")
        val amazon = StorePolicySnapshot.amazon.effectiveFor(
            storedDeveloperModeEnabled = false,
            nowMs = releaseDate
        )

        assertThat(amazon.showAdvancedSourceTypes).isFalse()
        assertThat(amazon.guideOnlyReviewSurface).isTrue()
        assertThat(amazon.canUseDvr(developerModeEnabled = false)).isFalse()
        assertThat(StorePolicySnapshot.amazon.effectiveDeveloperModeEnabled(false, releaseDate)).isFalse()
    }

    @Test
    fun `amazon review build hides dvr unless developer mode unlocks it`() {
        assertThat(StorePolicySnapshot.amazon.canUseDvr(developerModeEnabled = false)).isFalse()
        assertThat(StorePolicySnapshot.amazon.canUseDvr(developerModeEnabled = true)).isTrue()
        assertThat(StorePolicySnapshot.fullFeature.canUseDvr(developerModeEnabled = false)).isTrue()
    }

    @Test
    fun `amazon startup allows live tv guide and settings until developer mode unlock`() {
        assertThat(
            resolveStartupRoute(
                destination = StartupDestination.HOME,
                developerModeEnabled = false,
                policy = StorePolicySnapshot.amazon
            )
        ).isEqualTo(Routes.HOME)
        assertThat(
            resolveStartupRoute(
                destination = StartupDestination.LIVE_TV,
                developerModeEnabled = false,
                policy = StorePolicySnapshot.amazon
            )
        ).isEqualTo(Routes.LIVE_TV)
        assertThat(
            resolveStartupRoute(
                destination = StartupDestination.SETTINGS,
                developerModeEnabled = false,
                policy = StorePolicySnapshot.amazon
            )
        ).isEqualTo(Routes.SETTINGS)
        assertThat(
            resolveStartupRoute(
                destination = StartupDestination.VOD_CONTAINER,
                developerModeEnabled = false,
                policy = StorePolicySnapshot.amazon
            )
        ).isEqualTo(Routes.HOME)
        val lockedRoute = resolveStartupRoute(
            destination = StartupDestination.ADULT,
            developerModeEnabled = false,
            policy = StorePolicySnapshot.amazon
        )
        val route = resolveStartupRoute(
            destination = StartupDestination.ADULT,
            developerModeEnabled = true,
            policy = StorePolicySnapshot.amazon.effectiveFor(
                storedDeveloperModeEnabled = true,
                nowMs = utcMs("2026-06-01T00:00:00Z")
            )
        )

        assertThat(lockedRoute).isEqualTo(Routes.HOME)
        assertThat(route).isEqualTo(Routes.ADULT)
    }

    @Test
    fun `amazon bundled public source is visible in user source lists`() {
        val bundled = provider(
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/bundled_public_sources/afterglow_public_live.m3u8"
        )
        val userProvider = provider(id = 2L, m3uUrl = "https://example.test/user.m3u8")

        val visibleProviders = listOf(bundled, userProvider)
            .filter { StorePolicySnapshot.amazon.isUserVisibleProvider(it) }

        assertThat(visibleProviders).containsExactly(bundled, userProvider).inOrder()
    }

    @Test
    fun `amazon should seed bundled public source only before first seed and when no source exists`() {
        val bundled = provider(
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/bundled_public_sources/afterglow_public_live.m3u8"
        )
        val userProvider = provider(id = 2L, m3uUrl = "https://example.test/user.m3u8")

        assertThat(StorePolicySnapshot.amazon.shouldSeedBundledPublicSource(emptyList(), seededOnce = false)).isTrue()
        assertThat(StorePolicySnapshot.amazon.shouldSeedBundledPublicSource(emptyList(), seededOnce = true)).isFalse()
        assertThat(StorePolicySnapshot.amazon.shouldSeedBundledPublicSource(listOf(bundled), seededOnce = false)).isFalse()
        assertThat(StorePolicySnapshot.amazon.shouldSeedBundledPublicSource(listOf(bundled, userProvider), seededOnce = false)).isFalse()
    }

    @Test
    fun `amazon public source uses hosted playlist and guide with included fallbacks`() {
        val policy = StorePolicySnapshot.amazon

        assertThat(policy.bundledPublicSources.map { it.playlistAssetPath }).containsExactly(
            "public_sources/playlist_usa.m3u8"
        )
        assertThat(policy.bundledPublicSources.map { it.playlistFileName }).containsExactly(
            "afterglow_public_live.m3u8"
        )
        assertThat(policy.bundledPublicSources.map { it.providerName }).containsExactly(
            "Free, Authorized Public M3U Playlist"
        )
        assertThat(policy.bundledPublicSources.map { it.sourceSlot }).containsExactly(
            ProviderSourceSlot.LIVE
        )
        assertThat(policy.bundledPublicSources.map { it.m3uVodClassificationEnabled }).containsExactly(
            false
        )
        assertThat(policy.bundledPublicSources.map { it.guideFileName }).containsExactly(
            "afterglow_public_live.xml"
        )
        assertThat(policy.bundledPublicSources.map { it.playlistUrl }).containsExactly(
            "https://afterglow-labs.com/tv/afterglow_public_live.m3u8"
        )
        assertThat(policy.bundledPublicSources.map { it.guideUrl }).containsExactly(
            "https://afterglow-labs.com/tv/afterglow_public_live.xml"
        )
    }

    @Test
    fun `amazon bundled source repairs stale active source when no user source exists`() {
        val bundled = provider(
            id = 2L,
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/bundled_public_sources/afterglow_public_live.m3u8"
        )

        assertThat(
            shouldUseBundledPublicSourceForSlot(
                providers = listOf(bundled),
                currentSource = ActiveLiveSource.ProviderSource(1L),
                bundledProviderId = bundled.id
            )
        ).isTrue()
    }

    @Test
    fun `amazon bundled source keeps matching active source when no user source exists`() {
        val bundled = provider(
            id = 2L,
            m3uUrl = "file:///data/user/0/com.afterglowtv.app/files/bundled_public_sources/afterglow_public_live.m3u8"
        )

        assertThat(
            shouldUseBundledPublicSourceForSlot(
                providers = listOf(bundled),
                currentSource = ActiveLiveSource.ProviderSource(bundled.id),
                bundledProviderId = bundled.id
            )
        ).isFalse()
    }

    @Test
    fun `amazon bundled source keeps user source active`() {
        val userProvider = provider(id = 7L, m3uUrl = "https://example.test/user.m3u8")

        assertThat(
            shouldUseBundledPublicSourceForSlot(
                providers = listOf(userProvider),
                currentSource = ActiveLiveSource.ProviderSource(userProvider.id),
                bundledProviderId = 2L
            )
        ).isFalse()
    }

    @Test
    fun `amazon bundled public playlist avoids automatic epg discovery`() {
        assertThat(publicSourceAsset("playlist_usa.m3u8").readText()).doesNotContain("x-tvg-url")
    }

    @Test
    fun `amazon bundled public playlist contains known public channels`() {
        val livePlaylist = publicSourceAsset("playlist_usa.m3u8").readText()

        assertThat(livePlaylist).contains("30A TV Classic Movies")
        assertThat(livePlaylist).contains("30a-tv.com/feeds/pzaz/30atvmovies.m3u8")
        assertThat(livePlaylist).contains("Classic Arts Showcase")
        assertThat(livePlaylist).contains("classicarts.akamaized.net/hls/live/1024257/CAS/master.m3u8")
        assertThat(livePlaylist).contains("Boni Records")
        assertThat(livePlaylist).contains("Access Media Productions Channel")
        assertThat(livePlaylist).contains("Access Nashua")
        assertThat(livePlaylist).contains("AccuWeather Now")
        assertThat(livePlaylist).contains("AFTV")
    }

    @Test
    fun `amazon bundled public playlist excludes unsupported or review risk sources`() {
        val livePlaylist = publicSourceAsset("playlist_usa.m3u8").readText()

        assertThat(livePlaylist).doesNotContain("NASA TV")
        assertThat(livePlaylist).doesNotContain("ntv1.akamaized.net")
        assertThat(livePlaylist).doesNotContain("Afterglow Demo")
        assertThat(livePlaylist).doesNotContain("3catdirectes.cat")
        assertThat(livePlaylist).doesNotContain("ztnr.rtve.es")
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
        assertThat(livePlaylist).doesNotContain("ntv2.akamaized.net")
        assertThat(livePlaylist).doesNotContain("service-stitcher.clusters.pluto.tv")
    }

    private fun provider(id: Long = 1L, m3uUrl: String): Provider =
        Provider(
            id = id,
            name = "Source $id",
            type = ProviderType.M3U,
            serverUrl = m3uUrl,
            m3uUrl = m3uUrl
        )

    private enum class SourceDialogChoice {
        PLAYLIST,
        XTREAM,
        PORTAL
    }

    private fun visibleSourceDialogChoices(policy: StorePolicySnapshot): List<SourceDialogChoice> =
        listOf(
            SourceDialogChoice.PLAYLIST,
            SourceDialogChoice.XTREAM,
            SourceDialogChoice.PORTAL
        )

    private fun publicSourceAsset(fileName: String): File =
        File("src/amazon/assets/public_sources/$fileName")

    private fun utcMs(value: String): Long = Instant.parse(value).toEpochMilli()
}
