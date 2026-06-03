package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.AdultCacheSnapshot
import com.afterglowtv.domain.repository.AdultCachedCategory

private const val ADULT_CATEGORY_ID_BASE = -10_000_000L

internal fun adultCategoryId(key: String): Long =
    if (key == AdultCategoryBuilder.ALL_CATEGORY_KEY) {
        VirtualCategoryIds.ADULT
    } else {
        ADULT_CATEGORY_ID_BASE - (key.hashCode().toLong() and 0x7fffffffL)
    }

internal fun isAdultGeneratedCategoryId(categoryId: Long): Boolean =
    categoryId <= ADULT_CATEGORY_ID_BASE

internal fun adultCachedChannelIdsForCategory(
    cache: AdultCacheSnapshot?,
    categoryId: Long
): List<Long>? {
    val categories = cache?.categories ?: return emptyList()
    return when {
        categoryId == VirtualCategoryIds.ADULT ->
            categories.flatMap(AdultCachedCategory::channelIds).distinct()

        isAdultGeneratedCategoryId(categoryId) ->
            categories
                .firstOrNull { adultCategoryId(it.key) == categoryId }
                ?.channelIds
                ?.distinct()
                .orEmpty()

        else -> null
    }
}

internal fun adultPlaylistFingerprint(providerId: Long, lastSyncedAt: Long, channelCount: Int): String =
    "provider:$providerId:live:${lastSyncedAt.coerceAtLeast(0L)}:${channelCount.coerceAtLeast(0)}"
