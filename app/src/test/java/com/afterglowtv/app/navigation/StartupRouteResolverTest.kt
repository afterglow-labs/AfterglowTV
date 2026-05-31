package com.afterglowtv.app.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StartupRouteResolverTest {
    @Test
    fun `startup route uses configured destination without provider setup redirect`() {
        assertThat(resolveStartupRoute(StartupDestination.HOME, developerModeEnabled = false)).isEqualTo(Routes.HOME)
        assertThat(resolveStartupRoute(StartupDestination.IPTV_GUIDE, developerModeEnabled = false)).isEqualTo(Routes.EPG)
        assertThat(resolveStartupRoute(StartupDestination.XXX_GUIDE, developerModeEnabled = true)).isEqualTo(Routes.ADULT_GUIDE)
    }

    @Test
    fun `startup route falls back home for developer destinations when developer mode is locked`() {
        assertThat(resolveStartupRoute(StartupDestination.VOD_GUIDE, developerModeEnabled = false)).isEqualTo(Routes.HOME)
        assertThat(resolveStartupRoute(StartupDestination.XXX_GUIDE, developerModeEnabled = false)).isEqualTo(Routes.HOME)
        assertThat(resolveStartupRoute(StartupDestination.PERSONAL_GUIDE, developerModeEnabled = false)).isEqualTo(Routes.HOME)
    }

    @Test
    fun `vod guide is hidden from startup destinations`() {
        val visibleRoutes = StartupDestination.visibleEntries(developerModeEnabled = true).map { it.route }

        assertThat(visibleRoutes).doesNotContain(Routes.VOD_GUIDE)
        assertThat(visibleRoutes).contains(Routes.MOVIES)
        assertThat(visibleRoutes).contains(Routes.SERIES)
    }

    @Test
    fun `legacy vod guide startup route falls back home`() {
        assertThat(resolveStartupRoute(StartupDestination.VOD_GUIDE, developerModeEnabled = true)).isEqualTo(Routes.HOME)
    }
}
