package com.afterglowtv.domain.repository

import kotlinx.coroutines.flow.Flow

data class AdultCachedCategory(
    val key: String,
    val title: String,
    val channelIds: List<Long>
)

data class AdultCacheSnapshot(
    val providerId: Long,
    val playlistFingerprint: String,
    val categorizedChannelCount: Int,
    val categories: List<AdultCachedCategory>
)

interface AdultCacheRepository {
    fun observeProviderCache(providerId: Long): Flow<AdultCacheSnapshot?>
    fun observeProviderCache(providerId: Long, playlistFingerprint: String): Flow<AdultCacheSnapshot?>
    suspend fun replaceProviderCache(
        providerId: Long,
        playlistFingerprint: String,
        categorizedChannelCount: Int,
        categories: List<AdultCachedCategory>
    )
    suspend fun clearProviderCache(providerId: Long)
}
