package com.afterglowtv.data.repository

import com.afterglowtv.data.local.dao.AdultGuideCacheDao
import com.afterglowtv.data.local.dao.AdultGuideCacheChannelRefRow
import com.afterglowtv.data.local.entity.AdultGuideCacheCategoryChannelEntity
import com.afterglowtv.data.local.entity.AdultGuideCacheCategoryEntity
import com.afterglowtv.data.local.entity.AdultGuideCacheMetaEntity
import com.afterglowtv.domain.repository.AdultGuideCachedCategory
import com.afterglowtv.domain.repository.AdultGuideCacheRepository
import com.afterglowtv.domain.repository.AdultGuideCacheSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class AdultGuideCacheRepositoryImpl @Inject constructor(
    private val dao: AdultGuideCacheDao
) : AdultGuideCacheRepository {
    override fun observeProviderCache(providerId: Long): Flow<AdultGuideCacheSnapshot?> =
        observeProviderCacheRows(
            providerId = providerId,
            meta = dao.observeMeta(providerId),
            categories = dao.observeCategories(providerId),
            refs = dao.observeChannelRefs(providerId)
        )

    override fun observeProviderCache(
        providerId: Long,
        playlistFingerprint: String
    ): Flow<AdultGuideCacheSnapshot?> =
        observeProviderCacheRows(
            providerId = providerId,
            meta = dao.observeMeta(providerId, playlistFingerprint),
            categories = dao.observeCategories(providerId, playlistFingerprint),
            refs = dao.observeChannelRefs(providerId, playlistFingerprint)
        )

    private fun observeProviderCacheRows(
        providerId: Long,
        meta: Flow<AdultGuideCacheMetaEntity?>,
        categories: Flow<List<AdultGuideCacheCategoryEntity>>,
        refs: Flow<List<AdultGuideCacheChannelRefRow>>
    ): Flow<AdultGuideCacheSnapshot?> =
        combine(meta, categories, refs) { cacheMeta, cacheCategories, cacheRefs ->
            if (cacheMeta == null) {
                null
            } else {
                val refsByCategory = cacheRefs.groupBy { it.categoryKey }
                AdultGuideCacheSnapshot(
                    providerId = providerId,
                    playlistFingerprint = cacheMeta.playlistFingerprint,
                    categorizedChannelCount = cacheMeta.categorizedChannelCount,
                    categories = cacheCategories.mapNotNull { category ->
                        val channelIds = refsByCategory[category.categoryKey]
                            .orEmpty()
                            .sortedBy { it.position }
                            .map { it.channelId }
                            .distinct()
                        if (channelIds.isEmpty()) {
                            null
                        } else {
                            AdultGuideCachedCategory(
                                key = category.categoryKey,
                                title = category.title,
                                channelIds = channelIds
                            )
                        }
                    }
                )
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun replaceProviderCache(
        providerId: Long,
        playlistFingerprint: String,
        categorizedChannelCount: Int,
        categories: List<AdultGuideCachedCategory>
    ) = withContext(Dispatchers.IO) {
        val persistedCategories = categories
            .map { category ->
                category.copy(channelIds = category.channelIds.distinct())
            }
            .filter { category -> category.key.isNotBlank() && category.channelIds.isNotEmpty() }

        dao.replaceProviderCache(
            meta = AdultGuideCacheMetaEntity(
                providerId = providerId,
                playlistFingerprint = playlistFingerprint,
                categorizedChannelCount = categorizedChannelCount.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis()
            ),
            categories = persistedCategories.mapIndexed { index, category ->
                AdultGuideCacheCategoryEntity(
                    providerId = providerId,
                    playlistFingerprint = playlistFingerprint,
                    categoryKey = category.key,
                    title = category.title,
                    position = index,
                    channelCount = category.channelIds.size
                )
            },
            channelRefs = persistedCategories.flatMap { category ->
                category.channelIds.mapIndexed { index, channelId ->
                    AdultGuideCacheCategoryChannelEntity(
                        providerId = providerId,
                        playlistFingerprint = playlistFingerprint,
                        categoryKey = category.key,
                        channelId = channelId,
                        position = index
                    )
                }
            }
        )
    }

    override suspend fun clearProviderCache(providerId: Long) = withContext(Dispatchers.IO) {
        dao.clearProviderCache(providerId)
    }
}
