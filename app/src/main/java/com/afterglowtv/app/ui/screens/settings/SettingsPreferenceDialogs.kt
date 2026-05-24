package com.afterglowtv.app.ui.screens.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.afterglowtv.app.R
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.domain.model.AppTimeFormat
import com.afterglowtv.domain.model.CategorySortMode
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.DecoderMode
import com.afterglowtv.domain.model.PlayerSurfaceMode

@Composable
internal fun SettingsPreferenceDialogs(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    context: Context,
    showGuideDefaultCategoryDialog: Boolean,
    onShowGuideDefaultCategoryDialogChange: (Boolean) -> Unit,
    showGuideNoDataBlockDialog: Boolean,
    onShowGuideNoDataBlockDialogChange: (Boolean) -> Unit,
    showStartupDestinationDialog: Boolean,
    onShowStartupDestinationDialogChange: (Boolean) -> Unit,
    showPlaybackSpeedDialog: Boolean,
    onShowPlaybackSpeedDialogChange: (Boolean) -> Unit,
    showTimeFormatDialog: Boolean,
    onShowTimeFormatDialogChange: (Boolean) -> Unit,
    showAudioVideoOffsetDialog: Boolean,
    onShowAudioVideoOffsetDialogChange: (Boolean) -> Unit,
    showDecoderModeDialog: Boolean,
    onShowDecoderModeDialogChange: (Boolean) -> Unit,
    showSurfaceModeDialog: Boolean,
    onShowSurfaceModeDialogChange: (Boolean) -> Unit,
    showTimeshiftDepthDialog: Boolean,
    onShowTimeshiftDepthDialogChange: (Boolean) -> Unit,
    showDefaultStopTimerDialog: Boolean,
    onShowDefaultStopTimerDialogChange: (Boolean) -> Unit,
    showDefaultIdleTimerDialog: Boolean,
    onShowDefaultIdleTimerDialogChange: (Boolean) -> Unit,
    showControlsTimeoutDialog: Boolean,
    onShowControlsTimeoutDialogChange: (Boolean) -> Unit,
    showLiveOverlayTimeoutDialog: Boolean,
    onShowLiveOverlayTimeoutDialogChange: (Boolean) -> Unit,
    showNoticeTimeoutDialog: Boolean,
    onShowNoticeTimeoutDialogChange: (Boolean) -> Unit,
    showDiagnosticsTimeoutDialog: Boolean,
    onShowDiagnosticsTimeoutDialogChange: (Boolean) -> Unit,
    showLiveTvFiltersDialog: Boolean,
    onShowLiveTvFiltersDialogChange: (Boolean) -> Unit,
    showAudioLanguageDialog: Boolean,
    onShowAudioLanguageDialogChange: (Boolean) -> Unit,
    showSubtitleSizeDialog: Boolean,
    onShowSubtitleSizeDialogChange: (Boolean) -> Unit,
    showSubtitleTextColorDialog: Boolean,
    onShowSubtitleTextColorDialogChange: (Boolean) -> Unit,
    showSubtitleBackgroundDialog: Boolean,
    onShowSubtitleBackgroundDialogChange: (Boolean) -> Unit,
    showWifiQualityDialog: Boolean,
    onShowWifiQualityDialogChange: (Boolean) -> Unit,
    showEthernetQualityDialog: Boolean,
    onShowEthernetQualityDialogChange: (Boolean) -> Unit,
    showLanguageDialog: Boolean,
    onShowLanguageDialogChange: (Boolean) -> Unit,
    categorySortDialogType: String?,
    onCategorySortDialogTypeChange: (String?) -> Unit
) {
    SettingsPlayerPreferenceDialogs(
        uiState = uiState,
        viewModel = viewModel,
        context = context,
        showPlaybackSpeedDialog = showPlaybackSpeedDialog,
        onShowPlaybackSpeedDialogChange = onShowPlaybackSpeedDialogChange,
        showTimeFormatDialog = showTimeFormatDialog,
        onShowTimeFormatDialogChange = onShowTimeFormatDialogChange,
        showAudioVideoOffsetDialog = showAudioVideoOffsetDialog,
        onShowAudioVideoOffsetDialogChange = onShowAudioVideoOffsetDialogChange,
        showDecoderModeDialog = showDecoderModeDialog,
        onShowDecoderModeDialogChange = onShowDecoderModeDialogChange,
        showSurfaceModeDialog = showSurfaceModeDialog,
        onShowSurfaceModeDialogChange = onShowSurfaceModeDialogChange,
        showTimeshiftDepthDialog = showTimeshiftDepthDialog,
        onShowTimeshiftDepthDialogChange = onShowTimeshiftDepthDialogChange,
        showDefaultStopTimerDialog = showDefaultStopTimerDialog,
        onShowDefaultStopTimerDialogChange = onShowDefaultStopTimerDialogChange,
        showDefaultIdleTimerDialog = showDefaultIdleTimerDialog,
        onShowDefaultIdleTimerDialogChange = onShowDefaultIdleTimerDialogChange,
        showControlsTimeoutDialog = showControlsTimeoutDialog,
        onShowControlsTimeoutDialogChange = onShowControlsTimeoutDialogChange,
        showLiveOverlayTimeoutDialog = showLiveOverlayTimeoutDialog,
        onShowLiveOverlayTimeoutDialogChange = onShowLiveOverlayTimeoutDialogChange,
        showNoticeTimeoutDialog = showNoticeTimeoutDialog,
        onShowNoticeTimeoutDialogChange = onShowNoticeTimeoutDialogChange,
        showDiagnosticsTimeoutDialog = showDiagnosticsTimeoutDialog,
        onShowDiagnosticsTimeoutDialogChange = onShowDiagnosticsTimeoutDialogChange
    )

    if (showGuideDefaultCategoryDialog) {
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_guide_default_category),
            onDismiss = { onShowGuideDefaultCategoryDialogChange(false) }
        ) {
            uiState.guideDefaultCategoryOptions.forEachIndexed { index, category ->
                LevelOption(
                    level = index,
                    text = category.name,
                    currentLevel = if (uiState.guideDefaultCategoryId == category.id) index else -1,
                    onSelect = {
                        viewModel.setGuideDefaultCategory(category.id)
                        onShowGuideDefaultCategoryDialogChange(false)
                    }
                )
            }
        }
    }

    if (showGuideNoDataBlockDialog) {
        val options = remember {
            listOf(60, 120, 1_440)
        }
        val selectedIndex = options.indexOf(uiState.guideNoDataBlockMinutes).takeIf { it >= 0 } ?: 0
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_guide_no_data_block_size),
            onDismiss = { onShowGuideNoDataBlockDialogChange(false) }
        ) {
            options.forEachIndexed { index, minutes ->
                LevelOption(
                    level = index,
                    text = stringResource(
                        when (minutes) {
                            120 -> R.string.settings_guide_no_data_block_2_hours
                            1_440 -> R.string.settings_guide_no_data_block_24_hours
                            else -> R.string.settings_guide_no_data_block_1_hour
                        }
                    ),
                    currentLevel = selectedIndex,
                    onSelect = {
                        viewModel.setGuideNoDataBlockMinutes(minutes)
                        onShowGuideNoDataBlockDialogChange(false)
                    }
                )
            }
        }
    }

    if (showStartupDestinationDialog) {
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_startup_destination),
            onDismiss = { onShowStartupDestinationDialogChange(false) }
        ) {
            StartupDestination.entries.forEachIndexed { index, destination ->
                LevelOption(
                    level = index,
                    text = stringResource(destination.labelResId),
                    currentLevel = if (uiState.startupDestination == destination) index else -1,
                    onSelect = {
                        viewModel.setStartupDestination(destination)
                        onShowStartupDestinationDialogChange(false)
                    }
                )
            }
        }
    }

    if (showLiveTvFiltersDialog) {
        LiveTvQuickFiltersDialog(
            filters = uiState.liveTvCategoryFilters,
            onDismiss = { onShowLiveTvFiltersDialogChange(false) },
            onAddFilter = viewModel::addLiveTvCategoryFilter,
            onRemoveFilter = viewModel::removeLiveTvCategoryFilter
        )
    }

    if (showAudioLanguageDialog) {
        val autoLabel = stringResource(R.string.settings_audio_language_auto)
        val audioLanguageOptions = remember(autoLabel) { supportedAudioLanguages(autoLabel) }
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_audio_language),
            onDismiss = { onShowAudioLanguageDialogChange(false) }
        ) {
            audioLanguageOptions.forEachIndexed { index, option ->
                LevelOption(
                    level = index,
                    text = option.label,
                    currentLevel = if (uiState.preferredAudioLanguage == option.tag) index else -1,
                    onSelect = {
                        viewModel.setPreferredAudioLanguage(option.tag)
                        onShowAudioLanguageDialogChange(false)
                    }
                )
            }
        }
    }

    if (showSubtitleSizeDialog) {
        val subtitleSizeOptions = remember { subtitleSizeOptions() }
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_subtitle_size),
            onDismiss = { onShowSubtitleSizeDialogChange(false) }
        ) {
            subtitleSizeOptions.forEachIndexed { index, option ->
                LevelOption(
                    level = index,
                    text = option.label(context),
                    currentLevel = if (uiState.subtitleTextScale == option.scale) index else -1,
                    onSelect = {
                        viewModel.setSubtitleTextScale(option.scale)
                        onShowSubtitleSizeDialogChange(false)
                    }
                )
            }
        }
    }

    if (showSubtitleTextColorDialog) {
        val options = remember(context) { subtitleTextColorOptions(context) }
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_subtitle_text_color),
            onDismiss = { onShowSubtitleTextColorDialogChange(false) }
        ) {
            options.forEachIndexed { index, option ->
                LevelOption(
                    level = index,
                    text = option.label,
                    currentLevel = if (uiState.subtitleTextColor == option.colorArgb) index else -1,
                    onSelect = {
                        viewModel.setSubtitleTextColor(option.colorArgb)
                        onShowSubtitleTextColorDialogChange(false)
                    }
                )
            }
        }
    }

    if (showSubtitleBackgroundDialog) {
        val options = remember(context) { subtitleBackgroundColorOptions(context) }
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_subtitle_background),
            onDismiss = { onShowSubtitleBackgroundDialogChange(false) }
        ) {
            options.forEachIndexed { index, option ->
                LevelOption(
                    level = index,
                    text = option.label,
                    currentLevel = if (uiState.subtitleBackgroundColor == option.colorArgb) index else -1,
                    onSelect = {
                        viewModel.setSubtitleBackgroundColor(option.colorArgb)
                        onShowSubtitleBackgroundDialogChange(false)
                    }
                )
            }
        }
    }

    if (showWifiQualityDialog) {
        QualityCapSelectionDialog(
            title = stringResource(R.string.settings_select_wifi_quality_cap),
            currentValue = uiState.wifiMaxVideoHeight,
            onDismiss = { onShowWifiQualityDialogChange(false) },
            onSelect = {
                viewModel.setWifiQualityCap(it)
                onShowWifiQualityDialogChange(false)
            }
        )
    }

    if (showEthernetQualityDialog) {
        QualityCapSelectionDialog(
            title = stringResource(R.string.settings_select_ethernet_quality_cap),
            currentValue = uiState.ethernetMaxVideoHeight,
            onDismiss = { onShowEthernetQualityDialogChange(false) },
            onSelect = {
                viewModel.setEthernetQualityCap(it)
                onShowEthernetQualityDialogChange(false)
            }
        )
    }

    if (showLanguageDialog) {
        val systemDefaultLabel = stringResource(R.string.settings_system_default)
        val languageOptions = remember(systemDefaultLabel) { supportedAppLanguages(systemDefaultLabel) }
        PremiumSelectionDialog(
            title = stringResource(R.string.settings_select_language),
            onDismiss = { onShowLanguageDialogChange(false) }
        ) {
            languageOptions.forEachIndexed { index, option ->
                LevelOption(
                    level = index,
                    text = option.label,
                    currentLevel = if (uiState.appLanguage == option.tag) index else -1,
                    onSelect = {
                        viewModel.setAppLanguage(option.tag)
                        onShowLanguageDialogChange(false)
                    }
                )
            }
        }
    }

    categorySortDialogType?.let { typeName ->
        val type = ContentType.entries.firstOrNull { it.name == typeName }
        if (type != null) {
            CategorySortModeDialog(
                type = type,
                currentMode = uiState.categorySortModes[type] ?: CategorySortMode.DEFAULT,
                onDismiss = { onCategorySortDialogTypeChange(null) },
                onModeSelected = { mode ->
                    viewModel.setCategorySortMode(type, mode)
                    onCategorySortDialogTypeChange(null)
                }
            )
        }
    }
}
