package com.afterglowtv.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.afterglowtv.domain.model.LocalMediaKind

@Entity(
    tableName = "local_media_libraries",
    indices = [
        Index(value = ["root_uri"], unique = true),
        Index(value = ["enabled"])
    ]
)
data class LocalMediaLibraryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "root_uri") val rootUri: String,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    val enabled: Boolean = true,
    @ColumnInfo(name = "item_count") val itemCount: Int = 0,
    @ColumnInfo(name = "added_at_ms") val addedAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_scanned_at_ms") val lastScannedAtMs: Long? = null
)

@Entity(
    tableName = "local_media_items",
    foreignKeys = [
        ForeignKey(
            entity = LocalMediaLibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["library_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["library_id"]),
        Index(value = ["uri"], unique = true),
        Index(value = ["media_kind"]),
        Index(value = ["series_title", "season_number", "episode_number"]),
        Index(value = ["sort_title"])
    ]
)
data class LocalMediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "library_id") val libraryId: Long,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val title: String,
    @ColumnInfo(name = "sort_title") val sortTitle: String,
    @ColumnInfo(name = "media_kind") val mediaKind: LocalMediaKind = LocalMediaKind.UNKNOWN,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long? = null,
    @ColumnInfo(name = "date_modified_ms") val dateModifiedMs: Long? = null,
    @ColumnInfo(name = "poster_uri") val posterUri: String? = null,
    @ColumnInfo(name = "backdrop_uri") val backdropUri: String? = null,
    val description: String? = null,
    val genre: String? = null,
    @ColumnInfo(name = "release_year") val releaseYear: Int? = null,
    @ColumnInfo(name = "series_title") val seriesTitle: String? = null,
    @ColumnInfo(name = "season_number") val seasonNumber: Int? = null,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int? = null,
    @ColumnInfo(name = "added_at_ms") val addedAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_scanned_at_ms") val lastScannedAtMs: Long? = null
)

@Entity(
    tableName = "local_media_channels",
    foreignKeys = [
        ForeignKey(
            entity = LocalMediaLibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["library_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["library_id"]),
        Index(value = ["enabled", "sort_order"])
    ]
)
data class LocalMediaChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "library_id") val libraryId: Long? = null,
    val enabled: Boolean = true,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "local_media_programs",
    foreignKeys = [
        ForeignKey(
            entity = LocalMediaChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocalMediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["channel_id", "start_time_ms"]),
        Index(value = ["media_item_id"])
    ]
)
data class LocalMediaProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "channel_id") val channelId: Long,
    @ColumnInfo(name = "media_item_id") val mediaItemId: Long,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "start_time_ms") val startTimeMs: Long,
    @ColumnInfo(name = "end_time_ms") val endTimeMs: Long,
    @ColumnInfo(name = "media_duration_ms") val mediaDurationMs: Long? = null,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String? = null
)
