package com.afterglowtv.domain.repository

import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.LocalMediaBrowseResult
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.model.LocalMediaScanResult
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.model.SmbShareConfig
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {
    fun observeLibraries(): Flow<List<LocalMediaLibrary>>
    fun observeItems(libraryId: Long? = null): Flow<List<LocalMediaItem>>
    fun observeItemsPage(
        limit: Int,
        mediaKinds: Set<LocalMediaKind>? = null,
        libraryIds: Set<Long>? = null
    ): Flow<List<LocalMediaItem>>

    fun observeItemCount(
        mediaKinds: Set<LocalMediaKind>? = null,
        libraryIds: Set<Long>? = null
    ): Flow<Int>

    suspend fun addLibrary(rootUri: String, displayName: String?): Result<LocalMediaScanResult>
    suspend fun addSmbLibrary(config: SmbShareConfig): Result<LocalMediaScanResult>
    suspend fun browseLibrary(libraryId: Long, path: String = ""): Result<LocalMediaBrowseResult>
    suspend fun rescanLibrary(libraryId: Long): Result<LocalMediaScanResult>
    suspend fun deleteLibrary(libraryId: Long): Result<Unit>
}
