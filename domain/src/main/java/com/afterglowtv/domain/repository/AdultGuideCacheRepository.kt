package com.afterglowtv.domain.repository

import kotlinx.coroutines.flow.Flow

data class AdultGuideCachedCategory(
    val key: String,
    val title: String,
    val channelIds: List<Long>
)

data class AdultGuideCacheSnapshot(
    val providerId: Long,
    val playlistFingerprint: String,
    val categorizedChannelCount: Int,
    val categories: List<AdultGuideCachedCategory>
)

interface AdultGuideCacheRepository {
    fun observeProviderCache(providerId: Long, playlistFingerprint: String): Flow<AdultGuideCacheSnapshot?>
    suspend fun replaceProviderCache(
        providerId: Long,
        playlistFingerprint: String,
        categorizedChannelCount: Int,
        categories: List<AdultGuideCachedCategory>
    )
    suspend fun clearProviderCache(providerId: Long)
}
