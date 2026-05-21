package com.afterglowtv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.afterglowtv.data.local.entity.LocalMediaChannelEntity
import com.afterglowtv.data.local.entity.LocalMediaItemEntity
import com.afterglowtv.data.local.entity.LocalMediaLibraryEntity
import com.afterglowtv.data.local.entity.LocalMediaProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalMediaLibraryDao {
    @Query("SELECT * FROM local_media_libraries ORDER BY name COLLATE NOCASE ASC")
    fun observeLibraries(): Flow<List<LocalMediaLibraryEntity>>

    @Query("SELECT * FROM local_media_libraries WHERE id = :id LIMIT 1")
    suspend fun getLibrary(id: Long): LocalMediaLibraryEntity?

    @Query("SELECT * FROM local_media_libraries WHERE root_uri = :rootUri LIMIT 1")
    suspend fun getLibraryByRootUri(rootUri: String): LocalMediaLibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibrary(library: LocalMediaLibraryEntity): Long

    @Query(
        """
        UPDATE local_media_libraries
        SET item_count = :itemCount,
            last_scanned_at_ms = :scannedAtMs,
            updated_at_ms = :updatedAtMs
        WHERE id = :libraryId
        """
    )
    suspend fun updateScanState(
        libraryId: Long,
        itemCount: Int,
        scannedAtMs: Long,
        updatedAtMs: Long
    )

    @Query("DELETE FROM local_media_libraries WHERE id = :id")
    suspend fun deleteLibrary(id: Long)
}

@Dao
interface LocalMediaItemDao {
    @Query("SELECT * FROM local_media_items ORDER BY sort_title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<LocalMediaItemEntity>>

    @Query(
        """
        SELECT * FROM local_media_items
        WHERE library_id = :libraryId
        ORDER BY sort_title COLLATE NOCASE ASC
        """
    )
    fun observeByLibrary(libraryId: Long): Flow<List<LocalMediaItemEntity>>

    @Query("SELECT * FROM local_media_items WHERE library_id = :libraryId ORDER BY sort_title COLLATE NOCASE ASC")
    suspend fun getByLibrary(libraryId: Long): List<LocalMediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<LocalMediaItemEntity>)

    @Query("DELETE FROM local_media_items WHERE library_id = :libraryId")
    suspend fun deleteByLibrary(libraryId: Long)
}

@Dao
interface LocalMediaChannelDao {
    @Query("SELECT * FROM local_media_channels WHERE enabled = 1 ORDER BY sort_order ASC, name COLLATE NOCASE ASC")
    fun observeEnabledChannels(): Flow<List<LocalMediaChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: LocalMediaChannelEntity): Long
}

@Dao
interface LocalMediaProgramDao {
    @Query(
        """
        SELECT * FROM local_media_programs
        WHERE channel_id = :channelId
          AND start_time_ms < :windowEndMs
          AND end_time_ms > :windowStartMs
        ORDER BY start_time_ms ASC
        """
    )
    fun observeProgramsForChannel(
        channelId: Long,
        windowStartMs: Long,
        windowEndMs: Long
    ): Flow<List<LocalMediaProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrograms(programs: List<LocalMediaProgramEntity>)

    @Query("DELETE FROM local_media_programs WHERE channel_id = :channelId")
    suspend fun deleteByChannel(channelId: Long)
}
