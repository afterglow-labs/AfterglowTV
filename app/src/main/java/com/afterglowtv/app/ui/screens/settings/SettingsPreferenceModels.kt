package com.afterglowtv.app.ui.screens.settings

import android.app.Application
import com.afterglowtv.app.R
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.app.ui.model.LiveTvChannelMode
import com.afterglowtv.app.ui.model.LiveTvQuickFilterVisibilityMode
import com.afterglowtv.app.ui.model.RemoteChannelButtonAction
import com.afterglowtv.app.ui.model.VodViewMode
import com.afterglowtv.domain.model.AppTimeFormat
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.ChannelNumberingMode
import com.afterglowtv.domain.model.DecoderMode
import com.afterglowtv.domain.model.GroupedChannelLabelMode
import com.afterglowtv.domain.model.LiveChannelGroupingMode
import com.afterglowtv.domain.model.LiveVariantPreferenceMode
import com.afterglowtv.domain.model.PlayerSurfaceMode
import com.afterglowtv.domain.model.Provider

enum class ProviderWarningAction {
    EPG,
    MOVIES,
    SERIES
}

enum class ProviderSyncSelection {
    SYNC_NOW,
    REBUILD_INDEX,
    TV,
    MOVIES,
    SERIES,
    EPG
}

internal data class SettingsPreferenceSnapshot(
    val providers: List<Provider>,
    val activeProviderId: Long?,
    val parentalControlLevel: Int,
    val hasParentalPin: Boolean,
    val appLanguage: String,
    val appTimeFormat: AppTimeFormat,
    val preferredAudioLanguage: String,
    val playerMediaSessionEnabled: Boolean,
    val playerDecoderMode: DecoderMode,
    val playerSurfaceMode: PlayerSurfaceMode,
    val playerPlaybackSpeed: Float,
    val playerAudioVideoSyncEnabled: Boolean,
    val playerAudioVideoOffsetMs: Int,
    val centerTwoSlotMultiviewLayout: Boolean,
    val playerControlsTimeoutSeconds: Int,
    val playerLiveOverlayTimeoutSeconds: Int,
    val playerNoticeTimeoutSeconds: Int,
    val playerDiagnosticsTimeoutSeconds: Int,
    val subtitleTextScale: Float,
    val subtitleTextColor: Int,
    val subtitleBackgroundColor: Int,
    val wifiMaxVideoHeight: Int?,
    val ethernetMaxVideoHeight: Int?,
    val playerTimeshiftEnabled: Boolean,
    val playerTimeshiftDepthMinutes: Int,
    val defaultStopPlaybackTimerMinutes: Int,
    val defaultIdleStandbyTimerMinutes: Int,
    val lastSpeedTestMegabits: Double?,
    val lastSpeedTestTimestamp: Long?,
    val lastSpeedTestTransport: String?,
    val lastSpeedTestRecommendedHeight: Int?,
    val lastSpeedTestEstimated: Boolean,
    val isIncognitoMode: Boolean,
    val useXtreamTextClassification: Boolean,
    val xtreamBase64TextCompatibility: Boolean,
    val liveTvChannelMode: LiveTvChannelMode,
    val startupDestination: StartupDestination,
    val showLiveSourceSwitcher: Boolean,
    val showAllChannelsCategory: Boolean,
    val showRecentChannelsCategory: Boolean,
    val liveTvCategoryFilters: List<String>,
    val liveTvQuickFilterVisibilityMode: LiveTvQuickFilterVisibilityMode,
    val showAdultGuideTab: Boolean,
    val liveChannelNumberingMode: ChannelNumberingMode,
    val liveChannelGroupingMode: LiveChannelGroupingMode,
    val groupedChannelLabelMode: GroupedChannelLabelMode,
    val liveVariantPreferenceMode: LiveVariantPreferenceMode,
    val vodViewMode: VodViewMode,
    val vodInfiniteScroll: Boolean,
    val guideDefaultCategoryId: Long,
    val guideDefaultCategoryOptions: List<Category>,
    val guideNoDataBlockMinutes: Int,
    val guideNoDataShowChannelText: Boolean,
    val preventStandbyDuringPlayback: Boolean,
    val zapAutoRevert: Boolean,
    val remoteDpadChannelZapping: Boolean,
    val remoteDpadInvertChannelZapping: Boolean,
    val remoteShowInfoOnZap: Boolean,
    val remoteChannelUpButtonAction: RemoteChannelButtonAction,
    val remoteChannelDownButtonAction: RemoteChannelButtonAction,
    val autoPlayNextEpisode: Boolean,
    val autoCheckAppUpdates: Boolean,
    val autoDownloadAppUpdates: Boolean,
    val lastAppUpdateCheckAt: Long?,
    val cachedAppUpdateVersionName: String?,
    val cachedAppUpdateVersionCode: Int?,
    val cachedAppUpdateReleaseUrl: String?,
    val cachedAppUpdateDownloadUrl: String?,
    val cachedAppUpdateReleaseNotes: String,
    val cachedAppUpdatePublishedAt: String?
)

internal fun ProviderSyncSelection.label(application: Application): String = when (this) {
    ProviderSyncSelection.SYNC_NOW -> application.getString(R.string.settings_sync_option_sync_now)
    ProviderSyncSelection.REBUILD_INDEX -> application.getString(R.string.settings_sync_option_rebuild_index)
    ProviderSyncSelection.TV -> application.getString(R.string.settings_sync_option_tv)
    ProviderSyncSelection.MOVIES -> application.getString(R.string.settings_sync_option_movies)
    ProviderSyncSelection.SERIES -> application.getString(R.string.settings_sync_option_series)
    ProviderSyncSelection.EPG -> application.getString(R.string.settings_sync_option_epg)
}
