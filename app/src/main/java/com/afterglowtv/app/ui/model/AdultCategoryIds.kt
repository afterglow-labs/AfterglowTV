package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.AdultGuideCacheSnapshot
import com.afterglowtv.domain.repository.AdultGuideCachedCategory

private const val ADULT_GUIDE_CATEGORY_ID_BASE = -10_000_000L

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

internal fun adultGuidePlaylistFingerprint(providerId: Long, lastSyncedAt: Long, channelCount: Int): String =
    "provider:$providerId:live:${lastSyncedAt.coerceAtLeast(0L)}:${channelCount.coerceAtLeast(0)}"
