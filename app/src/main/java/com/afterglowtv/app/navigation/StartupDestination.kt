package com.afterglowtv.app.navigation

import androidx.annotation.StringRes
import com.afterglowtv.app.R

enum class StartupDestination(
    val storageValue: String,
    val route: String,
    @StringRes val labelResId: Int,
    val requiresDeveloperMode: Boolean = false
) {
    HOME("home", Routes.HOME, R.string.nav_home),
    LIVE_TV("live_tv", Routes.LIVE_TV, R.string.nav_live_tv),
    IPTV_GUIDE("iptv_guide", Routes.EPG, R.string.nav_iptv_guide),
    VOD_GUIDE("vod_guide", Routes.VOD_GUIDE, R.string.nav_vod_guide, requiresDeveloperMode = true),
    XXX_GUIDE("xxx_guide", Routes.ADULT_GUIDE, R.string.nav_adult_guide, requiresDeveloperMode = true),
    MOVIES("movies", Routes.MOVIES, R.string.nav_movies),
    SERIES("series", Routes.SERIES, R.string.nav_series),
    PERSONAL_GUIDE("personal_guide", Routes.LOCAL_MEDIA, R.string.nav_personal_guide, requiresDeveloperMode = true),
    SETTINGS("settings", Routes.SETTINGS, R.string.nav_settings);

    companion object {
        val default: StartupDestination = HOME

        fun visibleEntries(developerModeEnabled: Boolean): List<StartupDestination> =
            entries.filter { developerModeEnabled || !it.requiresDeveloperMode }

        fun fromStorage(value: String?): StartupDestination =
            entries.firstOrNull { it.storageValue == value || it.route == value } ?: default
    }
}
