package com.afterglowtv.domain.repository

import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.model.LocalMediaScanResult
import com.afterglowtv.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {
    fun observeLibraries(): Flow<List<LocalMediaLibrary>>
    fun observeItems(libraryId: Long? = null): Flow<List<LocalMediaItem>>

    suspend fun addLibrary(rootUri: String, displayName: String?): Result<LocalMediaScanResult>
    suspend fun addNetworkShare(
        rootUri: String,
        displayName: String?,
        username: String?,
        password: String?,
        domain: String?,
        guest: Boolean
    ): Result<LocalMediaScanResult>
    suspend fun rescanLibrary(libraryId: Long): Result<LocalMediaScanResult>
    suspend fun deleteLibrary(libraryId: Long): Result<Unit>
}
