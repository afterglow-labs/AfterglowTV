package com.afterglowtv.domain.repository

import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.model.LocalMediaScanResult
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.model.SmbShareConfig
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {
    fun observeLibraries(): Flow<List<LocalMediaLibrary>>
    fun observeItems(libraryId: Long? = null): Flow<List<LocalMediaItem>>

    suspend fun addLibrary(rootUri: String, displayName: String?): Result<LocalMediaScanResult>
    suspend fun addSmbLibrary(config: SmbShareConfig): Result<LocalMediaScanResult>
    suspend fun rescanLibrary(libraryId: Long): Result<LocalMediaScanResult>
    suspend fun deleteLibrary(libraryId: Long): Result<Unit>
}
