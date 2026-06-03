package com.afterglowtv.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "adult_guide_cache_meta",
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["provider_id"], unique = true)]
)
data class AdultGuideCacheMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "provider_id")
    val providerId: Long,
    @ColumnInfo(name = "playlist_fingerprint")
    val playlistFingerprint: String,
    @ColumnInfo(name = "categorized_channel_count")
    val categorizedChannelCount: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "adult_guide_cache_categories",
    primaryKeys = ["provider_id", "playlist_fingerprint", "category_key"],
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["provider_id", "playlist_fingerprint", "position"]),
        Index(value = ["provider_id"])
    ]
)
data class AdultGuideCacheCategoryEntity(
    @ColumnInfo(name = "provider_id")
    val providerId: Long,
    @ColumnInfo(name = "playlist_fingerprint")
    val playlistFingerprint: String,
    @ColumnInfo(name = "category_key")
    val categoryKey: String,
    val title: String,
    val position: Int,
    @ColumnInfo(name = "channel_count")
    val channelCount: Int
)

@Entity(
    tableName = "adult_guide_cache_category_channels",
    primaryKeys = ["provider_id", "playlist_fingerprint", "category_key", "channel_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["provider_id", "playlist_fingerprint", "category_key", "position"]),
        Index(value = ["provider_id"]),
        Index(value = ["channel_id"])
    ]
)
data class AdultGuideCacheCategoryChannelEntity(
    @ColumnInfo(name = "provider_id")
    val providerId: Long,
    @ColumnInfo(name = "playlist_fingerprint")
    val playlistFingerprint: String,
    @ColumnInfo(name = "category_key")
    val categoryKey: String,
    @ColumnInfo(name = "channel_id")
    val channelId: Long,
    val position: Int
)
