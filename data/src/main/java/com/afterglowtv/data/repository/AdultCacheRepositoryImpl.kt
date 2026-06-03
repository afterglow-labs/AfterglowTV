package com.afterglowtv.data.repository

import com.afterglowtv.data.local.dao.AdultCacheDao
import com.afterglowtv.data.local.dao.AdultCacheChannelRefRow
import com.afterglowtv.data.local.entity.AdultCacheCategoryChannelEntity
import com.afterglowtv.data.local.entity.AdultCacheCategoryEntity
import com.afterglowtv.data.local.entity.AdultCacheMetaEntity
import com.afterglowtv.domain.repository.AdultCachedCategory
import com.afterglowtv.domain.repository.AdultCacheRepository
import com.afterglowtv.domain.repository.AdultCacheSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class AdultCacheRepositoryImpl @Inject constructor(
    private val dao: AdultCacheDao
) : AdultCacheRepository {
    override fun observeProviderCache(providerId: Long): Flow<AdultCacheSnapshot?> =
        observeProviderCacheRows(
            providerId = providerId,
            meta = dao.observeMeta(providerId),
            categories = dao.observeCategories(providerId),
            refs = dao.observeChannelRefs(providerId)
        )

    override fun observeProviderCache(
        providerId: Long,
        playlistFingerprint: String
    ): Flow<AdultCacheSnapshot?> =
        observeProviderCacheRows(
            providerId = providerId,
            meta = dao.observeMeta(providerId, playlistFingerprint),
            categories = dao.observeCategories(providerId, playlistFingerprint),
            refs = dao.observeChannelRefs(providerId, playlistFingerprint)
        )

    private fun observeProviderCacheRows(
        providerId: Long,
        meta: Flow<AdultCacheMetaEntity?>,
        categories: Flow<List<AdultCacheCategoryEntity>>,
        refs: Flow<List<AdultCacheChannelRefRow>>
    ): Flow<AdultCacheSnapshot?> =
        combine(meta, categories, refs) { cacheMeta, cacheCategories, cacheRefs ->
            if (cacheMeta == null) {
                null
            } else {
                val refsByCategory = cacheRefs.groupBy { it.categoryKey }
                AdultCacheSnapshot(
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
                            AdultCachedCategory(
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
        categories: List<AdultCachedCategory>
    ) = withContext(Dispatchers.IO) {
        val persistedCategories = categories
            .map { category ->
                category.copy(channelIds = category.channelIds.distinct())
            }
            .filter { category -> category.key.isNotBlank() && category.channelIds.isNotEmpty() }

        dao.replaceProviderCache(
            meta = AdultCacheMetaEntity(
                providerId = providerId,
                playlistFingerprint = playlistFingerprint,
                categorizedChannelCount = categorizedChannelCount.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis()
            ),
            categories = persistedCategories.mapIndexed { index, category ->
                AdultCacheCategoryEntity(
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
                    AdultCacheCategoryChannelEntity(
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
