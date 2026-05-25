package com.afterglowtv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.afterglowtv.data.local.entity.AdultGuideCacheCategoryChannelEntity
import com.afterglowtv.data.local.entity.AdultGuideCacheCategoryEntity
import com.afterglowtv.data.local.entity.AdultGuideCacheMetaEntity
import kotlinx.coroutines.flow.Flow

data class AdultGuideCacheChannelRefRow(
    val categoryKey: String,
    val channelId: Long,
    val position: Int
)

@Dao
abstract class AdultGuideCacheDao {
    @Query(
        """
        SELECT *
        FROM adult_guide_cache_meta
        WHERE provider_id = :providerId
          AND playlist_fingerprint = :playlistFingerprint
        LIMIT 1
        """
    )
    abstract fun observeMeta(providerId: Long, playlistFingerprint: String): Flow<AdultGuideCacheMetaEntity?>

    @Query(
        """
        SELECT *
        FROM adult_guide_cache_categories
        WHERE provider_id = :providerId
          AND playlist_fingerprint = :playlistFingerprint
        ORDER BY position ASC, title ASC
        """
    )
    abstract fun observeCategories(
        providerId: Long,
        playlistFingerprint: String
    ): Flow<List<AdultGuideCacheCategoryEntity>>

    @Query(
        """
        SELECT category_key AS categoryKey, channel_id AS channelId, position
        FROM adult_guide_cache_category_channels
        WHERE provider_id = :providerId
          AND playlist_fingerprint = :playlistFingerprint
        ORDER BY category_key ASC, position ASC
        """
    )
    abstract fun observeChannelRefs(
        providerId: Long,
        playlistFingerprint: String
    ): Flow<List<AdultGuideCacheChannelRefRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMeta(meta: AdultGuideCacheMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCategories(categories: List<AdultGuideCacheCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertChannelRefs(refs: List<AdultGuideCacheCategoryChannelEntity>)

    @Query("DELETE FROM adult_guide_cache_meta WHERE provider_id = :providerId")
    protected abstract suspend fun deleteMeta(providerId: Long)

    @Query("DELETE FROM adult_guide_cache_categories WHERE provider_id = :providerId")
    protected abstract suspend fun deleteCategories(providerId: Long)

    @Query("DELETE FROM adult_guide_cache_category_channels WHERE provider_id = :providerId")
    protected abstract suspend fun deleteChannelRefs(providerId: Long)

    @Transaction
    open suspend fun replaceProviderCache(
        meta: AdultGuideCacheMetaEntity,
        categories: List<AdultGuideCacheCategoryEntity>,
        channelRefs: List<AdultGuideCacheCategoryChannelEntity>
    ) {
        clearProviderCache(meta.providerId)
        insertMeta(meta)
        if (categories.isNotEmpty()) {
            insertCategories(categories)
        }
        if (channelRefs.isNotEmpty()) {
            insertChannelRefs(channelRefs)
        }
    }

    @Transaction
    open suspend fun clearProviderCache(providerId: Long) {
        deleteChannelRefs(providerId)
        deleteCategories(providerId)
        deleteMeta(providerId)
    }
}
