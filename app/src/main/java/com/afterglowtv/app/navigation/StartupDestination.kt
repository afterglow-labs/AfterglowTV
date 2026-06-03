package com.afterglowtv.app.navigation

import androidx.annotation.StringRes
import com.afterglowtv.app.R
import com.afterglowtv.app.store.StorePolicy

enum class StartupDestination(
    val storageValue: String,
    val route: String,
    @StringRes val labelResId: Int,
    val requiresDeveloperMode: Boolean = false
) {
    HOME("home", Routes.HOME, R.string.nav_home),
    LIVE_TV("live_tv", Routes.LIVE_TV, R.string.nav_live_tv),
    IPTV_GUIDE("iptv_guide", Routes.EPG, R.string.nav_iptv_guide),
    VOD_CONTAINER("vod_container", Routes.VOD_CONTAINER, R.string.nav_vod_container),
    ADULT("adult", Routes.ADULT, R.string.nav_adult, requiresDeveloperMode = true),
    PERSONAL_GUIDE("personal_guide", Routes.LOCAL_MEDIA, R.string.nav_personal_guide),
    SETTINGS("settings", Routes.SETTINGS, R.string.nav_settings);

    companion object {
        val default: StartupDestination = HOME

        fun visibleEntries(developerModeEnabled: Boolean): List<StartupDestination> =
            entries.filter {
                (developerModeEnabled || !it.requiresDeveloperMode) &&
                    (StorePolicy.current.showAdultSurfaces || !it.requiresDeveloperMode)
            }

        fun visibleOrDefault(
            destination: StartupDestination,
            developerModeEnabled: Boolean
        ): StartupDestination =
            if (
                (developerModeEnabled || !destination.requiresDeveloperMode) &&
                (StorePolicy.current.showAdultSurfaces || !destination.requiresDeveloperMode)
            ) {
                destination
            } else {
                default
            }

        fun fromStorage(value: String?): StartupDestination =
            entries.firstOrNull { it.storageValue == value || it.route == value } ?: default
    }
}
