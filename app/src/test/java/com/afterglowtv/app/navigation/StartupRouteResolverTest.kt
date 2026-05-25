package com.afterglowtv.app.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StartupRouteResolverTest {
    @Test
    fun `startup route uses configured destination without provider setup redirect`() {
        assertThat(resolveStartupRoute(StartupDestination.HOME)).isEqualTo(Routes.HOME)
        assertThat(resolveStartupRoute(StartupDestination.IPTV_GUIDE)).isEqualTo(Routes.EPG)
        assertThat(resolveStartupRoute(StartupDestination.XXX_GUIDE)).isEqualTo(Routes.ADULT_GUIDE)
    }
}
