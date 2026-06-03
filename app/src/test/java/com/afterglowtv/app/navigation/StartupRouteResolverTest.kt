package com.afterglowtv.app.navigation

import com.afterglowtv.app.store.StorePolicy
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StartupRouteResolverTest {
    @Test
    fun `startup route uses configured destination without provider setup redirect`() {
        assertThat(resolveStartupRoute(StartupDestination.HOME, developerModeEnabled = false)).isEqualTo(Routes.HOME)
        assertThat(resolveStartupRoute(StartupDestination.IPTV_GUIDE, developerModeEnabled = false)).isEqualTo(Routes.EPG)
        assertThat(resolveStartupRoute(StartupDestination.VOD_CONTAINER, developerModeEnabled = false)).isEqualTo(Routes.VOD_CONTAINER)
        assertThat(resolveStartupRoute(StartupDestination.PERSONAL_GUIDE, developerModeEnabled = false)).isEqualTo(Routes.LOCAL_MEDIA)
        assertThat(resolveStartupRoute(StartupDestination.ADULT, developerModeEnabled = true)).isEqualTo(
            if (StorePolicy.current.showAdultSurfaces) Routes.ADULT else Routes.HOME
        )
    }

    @Test
    fun `startup route falls back home for adult surface when developer mode is locked`() {
        assertThat(resolveStartupRoute(StartupDestination.ADULT, developerModeEnabled = false)).isEqualTo(Routes.HOME)
    }

    @Test
    fun `startup destination list hides only adult surface while developer mode is locked`() {
        assertThat(StartupDestination.visibleEntries(developerModeEnabled = false)).doesNotContain(
            StartupDestination.ADULT
        )
        assertThat(StartupDestination.visibleEntries(developerModeEnabled = false)).containsAtLeast(
            StartupDestination.VOD_CONTAINER,
            StartupDestination.PERSONAL_GUIDE
        )
        val developerModeEntries = StartupDestination.visibleEntries(developerModeEnabled = true)
        if (StorePolicy.current.showAdultSurfaces) {
            assertThat(developerModeEntries).contains(StartupDestination.ADULT)
        } else {
            assertThat(developerModeEntries).doesNotContain(StartupDestination.ADULT)
        }
    }

    @Test
    fun `hidden startup destination displays as home while developer mode is locked`() {
        assertThat(
            StartupDestination.visibleOrDefault(
                destination = StartupDestination.ADULT,
                developerModeEnabled = false
            )
        ).isEqualTo(StartupDestination.HOME)
        assertThat(
            StartupDestination.visibleOrDefault(
                destination = StartupDestination.ADULT,
                developerModeEnabled = true
            )
        ).isEqualTo(
            if (StorePolicy.current.showAdultSurfaces) StartupDestination.ADULT else StartupDestination.HOME
        )
    }
}
