package com.afterglowtv.app.navigation

import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExternalDestinationTest {

    @Test
    fun fromLegacyRoute_parsesSupportedRoutes() {
        assertThat(ExternalDestination.fromLegacyRoute("home"))
            .isEqualTo(ExternalDestination.Home)
        assertThat(ExternalDestination.fromLegacyRoute("provider_setup?providerId=-1&importUri="))
            .isEqualTo(ExternalDestination.ProviderSetup())
        assertThat(ExternalDestination.fromLegacyRoute("provider_setup?providerId=-1&importUri=&m3uKind=VOD"))
            .isEqualTo(ExternalDestination.ProviderSetup(m3uKind = ProviderM3uPlaylistKind.VOD))
    }

    @Test
    fun fromLegacyRoute_rejectsUnsupportedRoutes() {
        assertThat(ExternalDestination.fromLegacyRoute("settings"))
            .isNull()
        assertThat(ExternalDestination.fromLegacyRoute("movie_detail/42?returnRoute=home"))
            .isNull()
        assertThat(ExternalDestination.fromLegacyRoute("series_detail/not-a-number"))
            .isNull()
        assertThat(ExternalDestination.fromLegacyRoute("series_detail/42?returnRoute=home"))
            .isNull()
    }
}
