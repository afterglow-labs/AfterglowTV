package com.afterglowtv.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.afterglowtv.data.local.dao.LocalMediaItemDao
import com.afterglowtv.data.local.dao.LocalMediaLibraryDao
import com.afterglowtv.data.local.entity.LocalMediaItemEntity
import com.afterglowtv.data.local.entity.LocalMediaLibraryEntity
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.model.LocalMediaScanResult
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.repository.LocalMediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

class LocalMediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryDao: LocalMediaLibraryDao,
    private val itemDao: LocalMediaItemDao
) : LocalMediaRepository {
    override fun observeLibraries(): Flow<List<LocalMediaLibrary>> =
        libraryDao.observeLibraries().map { libraries -> libraries.map { it.toDomain() } }

    override fun observeItems(libraryId: Long?): Flow<List<LocalMediaItem>> =
        (libraryId?.let(itemDao::observeByLibrary) ?: itemDao.observeAll())
            .map { items -> items.map { it.toDomain() } }

    override suspend fun addLibrary(
        rootUri: String,
        displayName: String?
    ): Result<LocalMediaScanResult> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val existing = libraryDao.getLibraryByRootUri(rootUri)
            val name = displayName?.takeIf { it.isNotBlank() }
                ?: existing?.name
                ?: "Local Media"
            val entity = LocalMediaLibraryEntity(
                id = existing?.id ?: 0L,
                name = name,
                rootUri = rootUri,
                displayName = displayName?.takeIf { it.isNotBlank() } ?: existing?.displayName,
                enabled = true,
                itemCount = existing?.itemCount ?: 0,
                addedAtMs = existing?.addedAtMs ?: now,
                updatedAtMs = now,
                lastScannedAtMs = existing?.lastScannedAtMs
            )
            val insertedId = libraryDao.upsertLibrary(entity)
            val libraryId = existing?.id ?: insertedId
            scanLibrary(libraryDao.getLibrary(libraryId) ?: entity.copy(id = libraryId))
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> Result.error(error.message ?: "Failed to add local media library.", error) }
        )
    }

    override suspend fun rescanLibrary(libraryId: Long): Result<LocalMediaScanResult> = withContext(Dispatchers.IO) {
        runCatching {
            val library = libraryDao.getLibrary(libraryId)
                ?: throw IllegalArgumentException("Local media library was not found.")
            scanLibrary(library)
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> Result.error(error.message ?: "Failed to scan local media library.", error) }
        )
    }

    override suspend fun deleteLibrary(libraryId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            libraryDao.deleteLibrary(libraryId)
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error -> Result.error(error.message ?: "Failed to delete local media library.", error) }
        )
    }

    private suspend fun scanLibrary(library: LocalMediaLibraryEntity): LocalMediaScanResult {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(library.rootUri))
            ?: throw IllegalArgumentException("Local media folder is no longer accessible.")
        if (!root.exists() || !root.isDirectory) {
            throw IllegalArgumentException("Local media folder is not available.")
        }

        val now = System.currentTimeMillis()
        val scan = LocalMediaTreeScan(libraryId = library.id, scannedAtMs = now)
        walkTree(root, scan, depth = 0)
        itemDao.deleteByLibrary(library.id)
        if (scan.items.isNotEmpty()) {
            itemDao.upsertItems(scan.items)
        }
        libraryDao.updateScanState(
            libraryId = library.id,
            itemCount = scan.items.size,
            scannedAtMs = now,
            updatedAtMs = now
        )
        return LocalMediaScanResult(
            libraryId = library.id,
            scannedCount = scan.scannedCount,
            importedCount = scan.items.size,
            skippedCount = scan.skippedCount
        )
    }

    private fun walkTree(
        document: DocumentFile,
        scan: LocalMediaTreeScan,
        depth: Int
    ) {
        if (depth > MAX_SCAN_DEPTH) return
        val children = runCatching { document.listFiles().toList() }.getOrDefault(emptyList())
        for (child in children) {
            if (child.isDirectory) {
                walkTree(child, scan, depth + 1)
                continue
            }
            scan.scannedCount += 1
            val displayName = child.name.orEmpty()
            val mimeType = child.type
            if (!LocalMediaImport.isImportableVideo(displayName, mimeType)) {
                scan.skippedCount += 1
                continue
            }
            val metadata = LocalMediaImport.describeFile(displayName, mimeType)
            scan.items += LocalMediaItemEntity(
                libraryId = scan.libraryId,
                uri = child.uri.toString(),
                displayName = displayName.ifBlank { child.uri.lastPathSegment.orEmpty() },
                title = metadata.title,
                sortTitle = metadata.title.toSortTitle(),
                mediaKind = metadata.mediaKind,
                mimeType = mimeType,
                durationMs = readDurationMs(child.uri),
                sizeBytes = child.length().takeIf { it > 0L },
                dateModifiedMs = child.lastModified().takeIf { it > 0L },
                seriesTitle = metadata.seriesTitle,
                seasonNumber = metadata.seasonNumber,
                episodeNumber = metadata.episodeNumber,
                addedAtMs = scan.scannedAtMs,
                updatedAtMs = scan.scannedAtMs,
                lastScannedAtMs = scan.scannedAtMs
            )
        }
    }

    private fun readDurationMs(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun String.toSortTitle(): String {
        val trimmed = trim()
        val normalized = trimmed.lowercase(Locale.US)
        return when {
            normalized.startsWith("the ") -> trimmed.drop(4)
            normalized.startsWith("an ") -> trimmed.drop(3)
            normalized.startsWith("a ") -> trimmed.drop(2)
            else -> trimmed
        }
    }

    private data class LocalMediaTreeScan(
        val libraryId: Long,
        val scannedAtMs: Long,
        var scannedCount: Int = 0,
        var skippedCount: Int = 0,
        val items: MutableList<LocalMediaItemEntity> = mutableListOf()
    )

    private companion object {
        const val MAX_SCAN_DEPTH = 16
    }
}

private fun LocalMediaLibraryEntity.toDomain(): LocalMediaLibrary = LocalMediaLibrary(
    id = id,
    name = name,
    rootUri = rootUri,
    displayName = displayName,
    enabled = enabled,
    itemCount = itemCount,
    addedAtMs = addedAtMs,
    updatedAtMs = updatedAtMs,
    lastScannedAtMs = lastScannedAtMs
)

private fun LocalMediaItemEntity.toDomain(): LocalMediaItem = LocalMediaItem(
    id = id,
    libraryId = libraryId,
    uri = uri,
    displayName = displayName,
    title = title,
    sortTitle = sortTitle,
    mediaKind = mediaKind ?: LocalMediaKind.UNKNOWN,
    mimeType = mimeType,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    dateModifiedMs = dateModifiedMs,
    posterUri = posterUri,
    backdropUri = backdropUri,
    description = description,
    genre = genre,
    releaseYear = releaseYear,
    seriesTitle = seriesTitle,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    addedAtMs = addedAtMs,
    updatedAtMs = updatedAtMs,
    lastScannedAtMs = lastScannedAtMs
)
