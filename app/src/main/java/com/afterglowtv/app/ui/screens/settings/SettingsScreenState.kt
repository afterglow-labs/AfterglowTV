package com.afterglowtv.app.ui.screens.settings

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.afterglowtv.app.MainActivity
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.time.createDateTimeFormat
import com.afterglowtv.domain.model.AppTimeFormat

internal data class SettingsScreenLabels(
    val appLanguageLabel: String,
    val timeFormatLabel: String,
    val preferredAudioLanguageLabel: String,
    val playbackSpeedLabel: String,
    val audioVideoOffsetLabel: String,
    val decoderModeLabel: String,
    val surfaceModeLabel: String,
    val controlsTimeoutLabel: String,
    val liveOverlayTimeoutLabel: String,
    val noticeTimeoutLabel: String,
    val diagnosticsTimeoutLabel: String,
    val subtitleSizeLabel: String,
    val subtitleTextColorLabel: String,
    val subtitleBackgroundLabel: String,
    val wifiQualityLabel: String,
    val ethernetQualityLabel: String,
    val timeshiftDepthLabel: String,
    val defaultStopTimerLabel: String,
    val defaultIdleTimerLabel: String,
    val lastSpeedTestLabel: String,
    val lastSpeedTestSummary: String,
    val speedTestRecommendationLabel: String,
    val protectionSummary: String,
    val guideDefaultCategoryLabel: String,
    val guideNoDataBlockLabel: String
)

@Composable
internal fun rememberSettingsScreenLabels(
    uiState: SettingsUiState,
    context: Context
): SettingsScreenLabels {
    val appLanguageLabel = remember(uiState.appLanguage, context) {
        displayLanguageLabel(uiState.appLanguage, context.getString(R.string.settings_system_default))
    }
    val timeFormatLabel = remember(uiState.appTimeFormat, context) {
        formatAppTimeFormatLabel(uiState.appTimeFormat, context)
    }
    val dateTimeFormat = remember(uiState.appTimeFormat) { uiState.appTimeFormat.createDateTimeFormat() }
    val preferredAudioLanguageLabel = remember(uiState.preferredAudioLanguage, context) {
        displayLanguageLabel(uiState.preferredAudioLanguage, context.getString(R.string.settings_audio_language_auto))
    }
    val playbackSpeedLabel = remember(uiState.playerPlaybackSpeed) {
        formatPlaybackSpeedLabel(uiState.playerPlaybackSpeed)
    }
    val audioVideoOffsetLabel = remember(uiState.playerAudioVideoOffsetMs) {
        formatAudioVideoOffsetLabel(uiState.playerAudioVideoOffsetMs)
    }
    val decoderModeLabel = remember(uiState.playerDecoderMode, context) {
        formatDecoderModeLabel(uiState.playerDecoderMode, context)
    }
    val surfaceModeLabel = remember(uiState.playerSurfaceMode, context) {
        formatSurfaceModeLabel(uiState.playerSurfaceMode, context)
    }
    val controlsTimeoutLabel = remember(uiState.playerControlsTimeoutSeconds, context) {
        formatTimeoutSecondsLabel(uiState.playerControlsTimeoutSeconds, context)
    }
    val liveOverlayTimeoutLabel = remember(uiState.playerLiveOverlayTimeoutSeconds, context) {
        formatTimeoutSecondsLabel(uiState.playerLiveOverlayTimeoutSeconds, context)
    }
    val noticeTimeoutLabel = remember(uiState.playerNoticeTimeoutSeconds, context) {
        formatTimeoutSecondsLabel(uiState.playerNoticeTimeoutSeconds, context)
    }
    val diagnosticsTimeoutLabel = remember(uiState.playerDiagnosticsTimeoutSeconds, context) {
        formatTimeoutSecondsLabel(uiState.playerDiagnosticsTimeoutSeconds, context)
    }
    val subtitleSizeLabel = remember(uiState.subtitleTextScale, context) {
        formatSubtitleSizeLabel(uiState.subtitleTextScale, context)
    }
    val subtitleTextColorLabel = remember(uiState.subtitleTextColor, context) {
        formatSubtitleColorLabel(uiState.subtitleTextColor, subtitleTextColorOptions(context))
    }
    val subtitleBackgroundLabel = remember(uiState.subtitleBackgroundColor, context) {
        formatSubtitleColorLabel(uiState.subtitleBackgroundColor, subtitleBackgroundColorOptions(context))
    }
    val wifiQualityLabel = remember(uiState.wifiMaxVideoHeight, context) {
        formatQualityCapLabel(uiState.wifiMaxVideoHeight, context.getString(R.string.settings_quality_cap_auto))
    }
    val ethernetQualityLabel = remember(uiState.ethernetMaxVideoHeight, context) {
        formatQualityCapLabel(uiState.ethernetMaxVideoHeight, context.getString(R.string.settings_quality_cap_auto))
    }
    val timeshiftDepthLabel = remember(uiState.playerTimeshiftDepthMinutes, context) {
        formatTimeshiftDepthLabel(uiState.playerTimeshiftDepthMinutes, context)
    }
    val defaultStopTimerLabel = remember(uiState.defaultStopPlaybackTimerMinutes, context) {
        formatPlaybackTimerMinutesLabel(uiState.defaultStopPlaybackTimerMinutes, context)
    }
    val defaultIdleTimerLabel = remember(uiState.defaultIdleStandbyTimerMinutes, context) {
        formatPlaybackTimerMinutesLabel(uiState.defaultIdleStandbyTimerMinutes, context)
    }
    val lastSpeedTestLabel = remember(uiState.lastSpeedTest, context) {
        uiState.lastSpeedTest?.let(::formatSpeedTestValueLabel)
            ?: context.getString(R.string.settings_speed_test_not_run)
    }
    val lastSpeedTestSummary = remember(uiState.lastSpeedTest, context, dateTimeFormat) {
        uiState.lastSpeedTest?.let { formatSpeedTestSummary(it, context, dateTimeFormat) }
            ?: context.getString(R.string.settings_speed_test_summary_default)
    }
    val speedTestRecommendationLabel = remember(uiState.lastSpeedTest, context) {
        formatQualityCapLabel(
            uiState.lastSpeedTest?.recommendedMaxVideoHeight,
            context.getString(R.string.settings_quality_cap_auto)
        )
    }
    val protectionSummary = remember(uiState.parentalControlLevel, context) {
        when (uiState.parentalControlLevel) {
            0 -> context.getString(R.string.settings_level_off)
            1 -> context.getString(R.string.settings_level_locked)
            2 -> context.getString(R.string.settings_level_private)
            3 -> context.getString(R.string.settings_level_hidden)
            else -> context.getString(R.string.settings_level_unknown)
        }
    }
    val guideDefaultCategoryLabel = remember(
        uiState.guideDefaultCategoryId,
        uiState.guideDefaultCategoryOptions,
        context
    ) {
        uiState.guideDefaultCategoryOptions
            .firstOrNull { it.id == uiState.guideDefaultCategoryId }
            ?.name
            ?: context.getString(R.string.settings_guide_default_category_fallback)
    }
    val guideNoDataBlockLabel = remember(uiState.guideNoDataBlockMinutes, context) {
        formatGuideNoDataBlockLabel(uiState.guideNoDataBlockMinutes, context)
    }

    return SettingsScreenLabels(
        appLanguageLabel = appLanguageLabel,
        timeFormatLabel = timeFormatLabel,
        preferredAudioLanguageLabel = preferredAudioLanguageLabel,
        playbackSpeedLabel = playbackSpeedLabel,
        audioVideoOffsetLabel = audioVideoOffsetLabel,
        decoderModeLabel = decoderModeLabel,
        surfaceModeLabel = surfaceModeLabel,
        controlsTimeoutLabel = controlsTimeoutLabel,
        liveOverlayTimeoutLabel = liveOverlayTimeoutLabel,
        noticeTimeoutLabel = noticeTimeoutLabel,
        diagnosticsTimeoutLabel = diagnosticsTimeoutLabel,
        subtitleSizeLabel = subtitleSizeLabel,
        subtitleTextColorLabel = subtitleTextColorLabel,
        subtitleBackgroundLabel = subtitleBackgroundLabel,
        wifiQualityLabel = wifiQualityLabel,
        ethernetQualityLabel = ethernetQualityLabel,
        timeshiftDepthLabel = timeshiftDepthLabel,
        defaultStopTimerLabel = defaultStopTimerLabel,
        defaultIdleTimerLabel = defaultIdleTimerLabel,
        lastSpeedTestLabel = lastSpeedTestLabel,
        lastSpeedTestSummary = lastSpeedTestSummary,
        speedTestRecommendationLabel = speedTestRecommendationLabel,
        protectionSummary = protectionSummary,
        guideDefaultCategoryLabel = guideDefaultCategoryLabel,
        guideNoDataBlockLabel = guideNoDataBlockLabel
    )
}

internal fun Context.findMainActivity(): MainActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is MainActivity) return current
        current = current.baseContext
    }
    return null
}

private fun formatAppTimeFormatLabel(
    format: AppTimeFormat,
    context: Context
): String = context.getString(
    when (format) {
        AppTimeFormat.SYSTEM -> R.string.settings_time_format_system
        AppTimeFormat.TWELVE_HOUR -> R.string.settings_time_format_12h
        AppTimeFormat.TWENTY_FOUR_HOUR -> R.string.settings_time_format_24h
    }
)

private fun formatGuideNoDataBlockLabel(minutes: Int, context: Context): String = when (minutes) {
    120 -> context.getString(R.string.settings_guide_no_data_block_2_hours)
    1_440 -> context.getString(R.string.settings_guide_no_data_block_24_hours)
    else -> context.getString(R.string.settings_guide_no_data_block_1_hour)
}

private fun formatTimeshiftDepthLabel(
    depthMinutes: Int,
    context: Context
): String = when (depthMinutes) {
    15 -> context.getString(R.string.settings_live_timeshift_depth_15)
    60 -> context.getString(R.string.settings_live_timeshift_depth_60)
    else -> context.getString(R.string.settings_live_timeshift_depth_30)
}
