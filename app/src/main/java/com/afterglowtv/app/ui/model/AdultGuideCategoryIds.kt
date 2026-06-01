package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.AdultGuideCacheSnapshot
import com.afterglowtv.domain.repository.AdultGuideCachedCategory

private const val ADULT_GUIDE_CATEGORY_ID_BASE = -10_000_000L
private const val ADULT_GUIDE_SORT_VERSION = 2

internal fun adultGuideCategoryId(key: String): Long =
    if (key == AdultGuideCategoryBuilder.ALL_CATEGORY_KEY) {
        VirtualCategoryIds.ADULT_GUIDE
    } else {
        ADULT_GUIDE_CATEGORY_ID_BASE - (key.hashCode().toLong() and 0x7fffffffL)
    }

internal fun isAdultGuideGeneratedCategoryId(categoryId: Long): Boolean =
    categoryId <= ADULT_GUIDE_CATEGORY_ID_BASE

internal fun adultGuideCachedChannelIdsForCategory(
    cache: AdultGuideCacheSnapshot?,
    categoryId: Long
): List<Long>? {
    val categories = cache?.categories ?: return emptyList()
    return when {
        categoryId == VirtualCategoryIds.ADULT_GUIDE ->
            categories.flatMap(AdultGuideCachedCategory::channelIds).distinct()

        isAdultGuideGeneratedCategoryId(categoryId) ->
            categories
                .firstOrNull { adultGuideCategoryId(it.key) == categoryId }
                ?.channelIds
                ?.distinct()
                .orEmpty()

        else -> null
    }
}

internal data class AdultGuideVisibleCategory(
    val key: String,
    val title: String,
    val channelIds: List<Long>
)

internal data class AdultGuideVisibleCategorySet(
    val categories: List<AdultGuideVisibleCategory>,
    val allChannelIds: List<Long>
)

internal fun filterAdultGuideCategoriesForHiddenIds(
    generatedCategories: List<AdultGuideCategory>,
    adultChannelIds: List<Long>,
    hiddenCategoryIds: Set<Long>
): AdultGuideVisibleCategorySet {
    val hiddenAdultChannelIds = generatedCategories
        .filter { category -> adultGuideCategoryId(category.key) in hiddenCategoryIds }
        .flatMap { category -> category.channels.map { it.id } }
        .toSet()
    val visibleCategories = generatedCategories
        .filterNot { category -> adultGuideCategoryId(category.key) in hiddenCategoryIds }
        .mapNotNull { category ->
            val visibleChannelIds = category.channels
                .map { it.id }
                .distinct()
                .filterNot(hiddenAdultChannelIds::contains)
            if (visibleChannelIds.isEmpty()) {
                null
            } else {
                AdultGuideVisibleCategory(
                    key = category.key,
                    title = category.title,
                    channelIds = visibleChannelIds
                )
            }
        }
    val visibleAllChannelIds = adultChannelIds
        .distinct()
        .filterNot(hiddenAdultChannelIds::contains)

    return AdultGuideVisibleCategorySet(
        categories = visibleCategories,
        allChannelIds = visibleAllChannelIds
    )
}

internal fun adultGuidePlaylistFingerprint(providerId: Long, lastSyncedAt: Long, channelCount: Int): String =
    "provider:$providerId:live:v$ADULT_GUIDE_SORT_VERSION:${lastSyncedAt.coerceAtLeast(0L)}:${channelCount.coerceAtLeast(0)}"
