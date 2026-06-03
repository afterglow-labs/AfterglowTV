package com.afterglowtv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.afterglowtv.data.local.entity.AdultCacheCategoryChannelEntity
import com.afterglowtv.data.local.entity.AdultCacheCategoryEntity
import com.afterglowtv.data.local.entity.AdultCacheMetaEntity
import kotlinx.coroutines.flow.Flow

data class AdultCacheChannelRefRow(
    val categoryKey: String,
    val channelId: Long,
    val position: Int
)

@Dao
abstract class AdultCacheDao {
    @Query(
        """
        SELECT *
        FROM adult_cache_meta
        WHERE provider_id = :providerId
          AND playlist_fingerprint = :playlistFingerprint
        LIMIT 1
        """
    )
    abstract fun observeMeta(providerId: Long, playlistFingerprint: String): Flow<AdultCacheMetaEntity?>

    @Query(
        """
        SELECT *
        FROM adult_cache_meta
        WHERE provider_id = :providerId
        LIMIT 1
        """
    )
    abstract fun observeMeta(providerId: Long): Flow<AdultCacheMetaEntity?>

    @Query(
        """
        SELECT *
        FROM adult_cache_categories
        WHERE provider_id = :providerId
          AND playlist_fingerprint = :playlistFingerprint
        ORDER BY position ASC, title ASC
        """
    )
    abstract fun observeCategories(
        providerId: Long,
        playlistFingerprint: String
    ): Flow<List<AdultCacheCategoryEntity>>

    @Query(
        """
        SELECT *
        FROM adult_cache_categories
        WHERE provider_id = :providerId
        ORDER BY position ASC, title ASC
        """
    )
    abstract fun observeCategories(providerId: Long): Flow<List<AdultCacheCategoryEntity>>

    @Query(
        """
        SELECT category_key AS categoryKey, channel_id AS channelId, position
        FROM adult_cache_category_channels
        WHERE provider_id = :providerId
          AND playlist_fingerprint = :playlistFingerprint
        ORDER BY category_key ASC, position ASC
        """
    )
    abstract fun observeChannelRefs(
        providerId: Long,
        playlistFingerprint: String
    ): Flow<List<AdultCacheChannelRefRow>>

    @Query(
        """
        SELECT category_key AS categoryKey, channel_id AS channelId, position
        FROM adult_cache_category_channels
        WHERE provider_id = :providerId
        ORDER BY category_key ASC, position ASC
        """
    )
    abstract fun observeChannelRefs(providerId: Long): Flow<List<AdultCacheChannelRefRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMeta(meta: AdultCacheMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCategories(categories: List<AdultCacheCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertChannelRefs(refs: List<AdultCacheCategoryChannelEntity>)

    @Query("DELETE FROM adult_cache_meta WHERE provider_id = :providerId")
    protected abstract suspend fun deleteMeta(providerId: Long)

    @Query("DELETE FROM adult_cache_categories WHERE provider_id = :providerId")
    protected abstract suspend fun deleteCategories(providerId: Long)

    @Query("DELETE FROM adult_cache_category_channels WHERE provider_id = :providerId")
    protected abstract suspend fun deleteChannelRefs(providerId: Long)

    @Transaction
    open suspend fun replaceProviderCache(
        meta: AdultCacheMetaEntity,
        categories: List<AdultCacheCategoryEntity>,
        channelRefs: List<AdultCacheCategoryChannelEntity>
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
