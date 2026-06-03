package com.afterglowtv.app.ui.screens.settings

import com.afterglowtv.app.R
import com.afterglowtv.app.localization.localeForLanguageTag
import com.afterglowtv.app.localization.supportedAppLanguageTags
import com.afterglowtv.app.ui.model.LiveTvChannelMode
import com.afterglowtv.app.ui.model.LiveTvQuickFilterVisibilityMode
import com.afterglowtv.app.ui.model.VodViewMode
import com.afterglowtv.domain.model.CategorySortMode
import com.afterglowtv.domain.model.ChannelNumberingMode
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.DecoderMode
import com.afterglowtv.domain.model.GroupedChannelLabelMode
import com.afterglowtv.domain.model.LiveChannelGroupingMode
import com.afterglowtv.domain.model.LiveVariantPreferenceMode
import com.afterglowtv.domain.model.PlayerSurfaceMode
import java.text.DateFormat
import java.util.Locale

internal fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

internal fun formatTimestamp(
    timestampMs: Long,
    dateTimeFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
): String {
    if (timestampMs <= 0L) return "--:--"
    return dateTimeFormat.format(java.util.Date(timestampMs))
}

internal data class AppLanguageOption(
    val tag: String,
    val label: String
)

internal fun supportedAppLanguages(systemDefaultLabel: String): List<AppLanguageOption> {
    val localeTags = listOf("system") + supportedAppLanguageTags()

    return localeTags.map { tag ->
        AppLanguageOption(
            tag = tag,
            label = if (tag == "system") {
                systemDefaultLabel
            } else {
                val locale = localeForLanguageTag(tag)
                locale.getDisplayLanguage(locale)
                    .replaceFirstChar { character ->
                        if (character.isLowerCase()) {
                            character.titlecase(locale)
                        } else {
                            character.toString()
                        }
                    }
            }
        )
    }
}

internal fun supportedAudioLanguages(autoLabel: String): List<AppLanguageOption> {
    return buildList {
        add(AppLanguageOption(tag = "auto", label = autoLabel))
        addAll(supportedAppLanguages(autoLabel).filterNot { it.tag == "system" })
    }
}

internal data class SubtitleScaleOption(
    val scale: Float,
    val label: (android.content.Context) -> String
)

internal data class SubtitleColorOption(
    val colorArgb: Int,
    val label: String
)

internal fun subtitleSizeOptions(): List<SubtitleScaleOption> {
    return listOf(
        SubtitleScaleOption(0.85f) { it.getString(R.string.settings_subtitle_size_small) },
        SubtitleScaleOption(1f) { it.getString(R.string.settings_subtitle_size_default) },
        SubtitleScaleOption(1.15f) { it.getString(R.string.settings_subtitle_size_large) },
        SubtitleScaleOption(1.3f) { it.getString(R.string.settings_subtitle_size_extra_large) }
    )
}

internal fun subtitleTextColorOptions(context: android.content.Context): List<SubtitleColorOption> {
    return listOf(
        SubtitleColorOption(0xFFFFFFFF.toInt(), context.getString(R.string.settings_subtitle_color_white)),
        SubtitleColorOption(0xFFFFEB3B.toInt(), context.getString(R.string.settings_subtitle_color_yellow)),
        SubtitleColorOption(0xFF80DEEA.toInt(), context.getString(R.string.settings_subtitle_color_cyan)),
        SubtitleColorOption(0xFFA5D6A7.toInt(), context.getString(R.string.settings_subtitle_color_green))
    )
}

internal fun subtitleBackgroundColorOptions(context: android.content.Context): List<SubtitleColorOption> {
    return listOf(
        SubtitleColorOption(0x00000000, context.getString(R.string.settings_subtitle_background_transparent)),
        SubtitleColorOption(0x80000000.toInt(), context.getString(R.string.settings_subtitle_background_dim)),
        SubtitleColorOption(0xCC000000.toInt(), context.getString(R.string.settings_subtitle_background_black)),
        SubtitleColorOption(0xCC102A43.toInt(), context.getString(R.string.settings_subtitle_background_blue))
    )
}

internal fun displayLanguageLabel(languageTag: String, defaultLabel: String): String {
    if (languageTag.isBlank() || languageTag == "system" || languageTag == "auto") return defaultLabel
    val locale = localeForLanguageTag(languageTag)
    if (locale.language.isBlank()) return defaultLabel
    return locale.getDisplayLanguage(Locale.getDefault())
        .replaceFirstChar { character ->
            if (character.isLowerCase()) {
                character.titlecase(Locale.getDefault())
            } else {
                character.toString()
            }
        }
}

internal fun formatPlaybackSpeedLabel(speed: Float): String {
    return if (speed % 1f == 0f) {
        "${speed.toInt()}x"
    } else {
        "${("%.2f".format(Locale.US, speed)).trimEnd('0').trimEnd('.')}x"
    }
}

internal fun playerTimeoutOptions(): List<Int> = listOf(2, 3, 4, 5, 6, 8, 10, 15, 20, 30)

internal fun formatDecoderModeLabel(mode: DecoderMode, context: android.content.Context): String {
    return when (mode) {
        DecoderMode.AUTO -> context.getString(R.string.settings_decoder_auto)
        DecoderMode.HARDWARE -> context.getString(R.string.settings_decoder_hardware)
        DecoderMode.SOFTWARE -> context.getString(R.string.settings_decoder_software)
        DecoderMode.COMPATIBILITY -> context.getString(R.string.settings_decoder_compatibility)
    }
}

internal fun formatSurfaceModeLabel(
    mode: PlayerSurfaceMode,
    context: android.content.Context
): String = when (mode) {
    PlayerSurfaceMode.AUTO -> context.getString(R.string.settings_surface_auto)
    PlayerSurfaceMode.SURFACE_VIEW -> context.getString(R.string.settings_surface_surface_view)
    PlayerSurfaceMode.TEXTURE_VIEW -> context.getString(R.string.settings_surface_texture_view)
}

internal fun formatTimeoutSecondsLabel(seconds: Int, context: android.content.Context): String {
    return context.resources.getQuantityString(
        R.plurals.settings_timeout_seconds,
        seconds,
        seconds
    )
}

internal fun formatSubtitleSizeLabel(scale: Float, context: android.content.Context): String {
    return subtitleSizeOptions().firstOrNull { it.scale == scale }?.label?.invoke(context)
        ?: context.getString(R.string.settings_subtitle_size_default)
}

internal fun formatSubtitleColorLabel(colorArgb: Int, options: List<SubtitleColorOption>): String {
    return options.firstOrNull { it.colorArgb == colorArgb }?.label ?: options.first().label
}

internal fun formatQualityCapLabel(maxHeight: Int?, autoLabel: String): String {
    return maxHeight?.let { "${it}p" } ?: autoLabel
}

internal fun formatSpeedTestValueLabel(speedTest: InternetSpeedTestUiModel): String {
    return String.format(Locale.getDefault(), "%.1f Mbps", speedTest.megabitsPerSecond)
}

internal fun formatSpeedTestSummary(
    speedTest: InternetSpeedTestUiModel,
    context: android.content.Context,
    dateTimeFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
): String {
    val transportLabel = when (speedTest.transportLabel) {
        InternetSpeedTestTransport.WIFI.name -> context.getString(R.string.settings_speed_test_transport_wifi)
        InternetSpeedTestTransport.ETHERNET.name -> context.getString(R.string.settings_speed_test_transport_ethernet)
        InternetSpeedTestTransport.CELLULAR.name -> context.getString(R.string.settings_speed_test_transport_cellular)
        InternetSpeedTestTransport.OTHER.name -> context.getString(R.string.settings_speed_test_transport_other)
        else -> context.getString(R.string.settings_speed_test_transport_unknown)
    }
    val measuredAtLabel = formatTimestamp(speedTest.measuredAtMs, dateTimeFormat)
    return if (speedTest.isEstimated) {
        context.getString(R.string.settings_speed_test_summary_estimated, transportLabel, measuredAtLabel)
    } else {
        context.getString(R.string.settings_speed_test_summary_measured, transportLabel, measuredAtLabel)
    }
}

internal fun sortModeLabel(mode: CategorySortMode, context: android.content.Context): String {
    return when (mode) {
        CategorySortMode.DEFAULT -> context.getString(R.string.settings_category_sort_default)
        CategorySortMode.TITLE_ASC -> context.getString(R.string.settings_category_sort_az)
        CategorySortMode.TITLE_DESC -> context.getString(R.string.settings_category_sort_za)
        CategorySortMode.COUNT_DESC -> context.getString(R.string.settings_category_sort_most_items)
        CategorySortMode.COUNT_ASC -> context.getString(R.string.settings_category_sort_least_items)
    }
}

internal fun formatCategorySortModeLabel(mode: CategorySortMode, context: android.content.Context): String {
    return sortModeLabel(mode, context)
}

internal fun categoryTypeLabel(type: ContentType, context: android.content.Context): String {
    return when (type) {
        ContentType.LIVE -> context.getString(R.string.settings_category_sort_live)
        ContentType.MOVIE -> context.getString(R.string.settings_category_sort_vod_movies)
        ContentType.SERIES -> context.getString(R.string.settings_category_sort_vod_tv)
        ContentType.SERIES_EPISODE -> context.getString(R.string.settings_category_sort_vod_tv)
    }
}

internal fun categoryTypeDescription(type: ContentType, context: android.content.Context): String {
    return when (type) {
        ContentType.LIVE -> context.getString(R.string.settings_category_type_live_description)
        ContentType.MOVIE -> context.getString(R.string.settings_category_type_vod_movies_description)
        ContentType.SERIES -> context.getString(R.string.settings_category_type_vod_tv_description)
        ContentType.SERIES_EPISODE -> context.getString(R.string.settings_category_type_vod_tv_description)
    }
}

internal fun LiveTvChannelMode.labelResId(): Int = when (this) {
    LiveTvChannelMode.COMFORTABLE -> R.string.settings_live_tv_mode_comfortable
    LiveTvChannelMode.COMPACT -> R.string.settings_live_tv_mode_compact
    LiveTvChannelMode.PRO -> R.string.settings_live_tv_mode_pro
}

internal fun LiveTvChannelMode.descriptionResId(): Int = when (this) {
    LiveTvChannelMode.COMFORTABLE -> R.string.settings_live_tv_mode_comfortable_desc
    LiveTvChannelMode.COMPACT -> R.string.settings_live_tv_mode_compact_desc
    LiveTvChannelMode.PRO -> R.string.settings_live_tv_mode_pro_desc
}

internal fun LiveTvQuickFilterVisibilityMode.labelResId(): Int = when (this) {
    LiveTvQuickFilterVisibilityMode.HIDE -> R.string.settings_live_tv_quick_filter_visibility_hide
    LiveTvQuickFilterVisibilityMode.SHOW_WHEN_FILTERS_AVAILABLE -> R.string.settings_live_tv_quick_filter_visibility_available
    LiveTvQuickFilterVisibilityMode.ALWAYS_VISIBLE -> R.string.settings_live_tv_quick_filter_visibility_always
}

internal fun LiveTvQuickFilterVisibilityMode.descriptionResId(): Int = when (this) {
    LiveTvQuickFilterVisibilityMode.HIDE -> R.string.settings_live_tv_quick_filter_visibility_hide_desc
    LiveTvQuickFilterVisibilityMode.SHOW_WHEN_FILTERS_AVAILABLE -> R.string.settings_live_tv_quick_filter_visibility_available_desc
    LiveTvQuickFilterVisibilityMode.ALWAYS_VISIBLE -> R.string.settings_live_tv_quick_filter_visibility_always_desc
}

internal fun VodViewMode.labelResId(): Int = when (this) {
    VodViewMode.SHELVES -> R.string.settings_vod_view_mode_shelves
    VodViewMode.GRID -> R.string.settings_vod_view_mode_grid
    VodViewMode.CONTAINER -> R.string.settings_vod_view_mode_container
}

internal fun VodViewMode.descriptionResId(): Int = when (this) {
    VodViewMode.SHELVES -> R.string.settings_vod_view_mode_shelves_desc
    VodViewMode.GRID -> R.string.settings_vod_view_mode_grid_desc
    VodViewMode.CONTAINER -> R.string.settings_vod_view_mode_container_desc
}

internal fun ChannelNumberingMode.labelResId(): Int = when (this) {
    ChannelNumberingMode.GROUP -> R.string.settings_live_channel_numbering_group
    ChannelNumberingMode.PROVIDER -> R.string.settings_live_channel_numbering_provider
    ChannelNumberingMode.HIDDEN -> R.string.settings_live_channel_numbering_hidden
}

internal fun ChannelNumberingMode.descriptionResId(): Int = when (this) {
    ChannelNumberingMode.GROUP -> R.string.settings_live_channel_numbering_group_desc
    ChannelNumberingMode.PROVIDER -> R.string.settings_live_channel_numbering_provider_desc
    ChannelNumberingMode.HIDDEN -> R.string.settings_live_channel_numbering_hidden_desc
}

internal fun LiveChannelGroupingMode.labelResId(): Int = when (this) {
    LiveChannelGroupingMode.GROUPED -> R.string.settings_live_channel_grouping_grouped
    LiveChannelGroupingMode.RAW_VARIANTS -> R.string.settings_live_channel_grouping_raw_variants
}

internal fun LiveChannelGroupingMode.descriptionResId(): Int = when (this) {
    LiveChannelGroupingMode.GROUPED -> R.string.settings_live_channel_grouping_grouped_desc
    LiveChannelGroupingMode.RAW_VARIANTS -> R.string.settings_live_channel_grouping_raw_variants_desc
}

internal fun GroupedChannelLabelMode.labelResId(): Int = when (this) {
    GroupedChannelLabelMode.CANONICAL -> R.string.settings_grouped_channel_label_canonical
    GroupedChannelLabelMode.ORIGINAL_PROVIDER_LABEL -> R.string.settings_grouped_channel_label_original
    GroupedChannelLabelMode.HYBRID -> R.string.settings_grouped_channel_label_hybrid
}

internal fun GroupedChannelLabelMode.descriptionResId(): Int = when (this) {
    GroupedChannelLabelMode.CANONICAL -> R.string.settings_grouped_channel_label_canonical_desc
    GroupedChannelLabelMode.ORIGINAL_PROVIDER_LABEL -> R.string.settings_grouped_channel_label_original_desc
    GroupedChannelLabelMode.HYBRID -> R.string.settings_grouped_channel_label_hybrid_desc
}

internal fun LiveVariantPreferenceMode.labelResId(): Int = when (this) {
    LiveVariantPreferenceMode.BEST_QUALITY -> R.string.settings_live_variant_preference_best_quality
    LiveVariantPreferenceMode.OBSERVED_ONLY -> R.string.settings_live_variant_preference_observed_only
    LiveVariantPreferenceMode.BALANCED -> R.string.settings_live_variant_preference_balanced
    LiveVariantPreferenceMode.STABILITY_FIRST -> R.string.settings_live_variant_preference_stability_first
}

internal fun LiveVariantPreferenceMode.descriptionResId(): Int = when (this) {
    LiveVariantPreferenceMode.BEST_QUALITY -> R.string.settings_live_variant_preference_best_quality_desc
    LiveVariantPreferenceMode.OBSERVED_ONLY -> R.string.settings_live_variant_preference_observed_only_desc
    LiveVariantPreferenceMode.BALANCED -> R.string.settings_live_variant_preference_balanced_desc
    LiveVariantPreferenceMode.STABILITY_FIRST -> R.string.settings_live_variant_preference_stability_first_desc
}
