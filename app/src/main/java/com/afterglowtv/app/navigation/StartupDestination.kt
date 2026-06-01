package com.afterglowtv.app.navigation

import androidx.annotation.StringRes
import com.afterglowtv.app.R

enum class StartupDestination(
    val storageValue: String,
    val route: String,
    @StringRes val labelResId: Int,
    val requiresDeveloperMode: Boolean = false,
    val visibleInSettings: Boolean = true
) {
    HOME("home", Routes.HOME, R.string.nav_home),
    LIVE_TV("live_tv", Routes.LIVE_TV, R.string.nav_live_tv),
    IPTV_GUIDE("iptv_guide", Routes.EPG, R.string.nav_iptv_guide),
    VOD_CONTAINER("vod_container", Routes.VOD_CONTAINER, R.string.nav_vod),
    XXX_GUIDE("xxx_guide", Routes.ADULT_GUIDE, R.string.nav_adult_guide, requiresDeveloperMode = true),
    PERSONAL_GUIDE("personal_guide", Routes.LOCAL_MEDIA, R.string.nav_personal_guide, requiresDeveloperMode = true),
    SETTINGS("settings", Routes.SETTINGS, R.string.nav_settings);

    companion object {
        val default: StartupDestination = HOME

        fun visibleEntries(developerModeEnabled: Boolean): List<StartupDestination> =
            entries.filter { it.visibleInSettings && (developerModeEnabled || !it.requiresDeveloperMode) }

        fun fromStorage(value: String?): StartupDestination =
            entries.firstOrNull { it.storageValue == value || it.route == value } ?: default
    }
}
