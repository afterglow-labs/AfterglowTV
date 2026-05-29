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
import java.io.File
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
            val normalizedRootUri = normalizeLocalMediaRoot(rootUri)
            val existing = libraryDao.getLibraryByRootUri(normalizedRootUri)
            val name = displayName?.takeIf { it.isNotBlank() }
                ?: existing?.name
                ?: deriveLibraryNameFromRoot(normalizedRootUri)
            val entity = LocalMediaLibraryEntity(
                id = existing?.id ?: 0L,
                name = name,
                rootUri = normalizedRootUri,
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

    override suspend fun addNetworkShare(
        rootUri: String,
        displayName: String?,
        username: String?,
        password: String?,
        domain: String?,
        guest: Boolean
    ): Result<LocalMediaScanResult> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val normalizedRootUri = normalizeLocalMediaRoot(rootUri)
            val share = parseNetworkShare(normalizedRootUri)
            val existing = libraryDao.getLibraryByRootUri(normalizedRootUri)
            val name = displayName?.takeIf { it.isNotBlank() }
                ?: existing?.name
                ?: share.share.ifBlank { deriveLibraryNameFromRoot(normalizedRootUri) }
            val entity = LocalMediaLibraryEntity(
                id = existing?.id ?: 0L,
                name = name,
                rootUri = normalizedRootUri,
                sourceType = "SMB",
                displayName = displayName?.takeIf { it.isNotBlank() } ?: existing?.displayName,
                enabled = true,
                itemCount = existing?.itemCount ?: 0,
                addedAtMs = existing?.addedAtMs ?: now,
                updatedAtMs = now,
                lastScannedAtMs = existing?.lastScannedAtMs,
                smbHost = share.host,
                smbPort = 445,
                smbShare = share.share,
                smbPath = share.path,
                smbDomain = domain?.takeIf { it.isNotBlank() },
                smbUsername = username?.takeIf { it.isNotBlank() && !guest },
                smbPassword = password?.takeIf { it.isNotBlank() && !guest }
            )
            val insertedId = libraryDao.upsertLibrary(entity)
            val libraryId = existing?.id ?: insertedId
            scanLibrary(libraryDao.getLibrary(libraryId) ?: entity.copy(id = libraryId))
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> Result.error(error.message ?: "Failed to add network share.", error) }
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
        val source = LocalMediaSource.of(context, library.rootUri)
        val now = System.currentTimeMillis()
        val scan = LocalMediaTreeScan(libraryId = library.id, scannedAtMs = now)

        when (source) {
            is LocalMediaSource.DocumentTree -> {
                val root = source.root ?: throw IllegalArgumentException("Local media folder is no longer accessible.")
                if (!root.exists() || !root.isDirectory) {
                    throw IllegalArgumentException("Local media folder is not available.")
                }
                walkDocumentTree(
                    document = root,
                    scan = scan,
                    depth = 0,
                    folders = emptyList()
                )
            }
            is LocalMediaSource.FileSystem -> {
                val root = source.root
                if (!root.exists() || !root.isDirectory) {
                    throw IllegalArgumentException("Local media folder is not available.")
                }
                walkFilesystem(
                    file = root,
                    scan = scan,
                    depth = 0,
                    folders = emptyList()
                )
            }
            is LocalMediaSource.Unsupported -> throw IllegalArgumentException(source.reason)
        }

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

    private fun walkDocumentTree(
        document: DocumentFile,
        scan: LocalMediaTreeScan,
        depth: Int,
        folders: List<String>
    ) {
        if (depth > MAX_SCAN_DEPTH) return
        val children = runCatching { document.listFiles().toList() }.getOrDefault(emptyList())
        for (child in children) {
            if (child.isDirectory) {
                val childFolder = child.name.orEmpty()
                walkDocumentTree(
                    document = child,
                    scan = scan,
                    depth = depth + 1,
                    folders = if (childFolder.isNotBlank()) folders + childFolder else folders
                )
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
                folderPath = folders.filter { it.isNotBlank() }.joinToString(" / "),
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

    private fun walkFilesystem(
        file: File,
        scan: LocalMediaTreeScan,
        depth: Int,
        folders: List<String>
    ) {
        if (depth > MAX_SCAN_DEPTH) return
        if (scan.scannedCount >= MAX_SCAN_FILES) {
            return
        }

        val children = runCatching { file.listFiles() ?: emptyArray() }.getOrDefault(emptyArray())
        for (child in children) {
            if (scan.scannedCount >= MAX_SCAN_FILES) {
                return
            }
            if (child.isDirectory) {
                walkFilesystem(
                    file = child,
                    scan = scan,
                    depth = depth + 1,
                    folders = folders + child.name
                )
                continue
            }
            scan.scannedCount += 1
            val displayName = child.name.orEmpty()
            if (!LocalMediaImport.isImportableVideo(displayName, null)) {
                scan.skippedCount += 1
                continue
            }
            val metadata = LocalMediaImport.describeFile(displayName, null)
            scan.items += LocalMediaItemEntity(
                libraryId = scan.libraryId,
                uri = child.toURI().toString(),
                displayName = displayName.ifBlank { child.name },
                title = metadata.title,
                sortTitle = metadata.title.toSortTitle(),
                folderPath = folders.filter { it.isNotBlank() }.joinToString(" / "),
                mediaKind = metadata.mediaKind,
                mimeType = null,
                durationMs = readDurationMs(child),
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

    private fun readDurationMs(file: File): Long? {
        return runCatching { readDurationMs(Uri.fromFile(file)) }.getOrNull()
    }

    private fun normalizeLocalMediaRoot(rawRootUri: String): String {
        val trimmed = rawRootUri.trim()
        if (trimmed.isBlank()) throw IllegalArgumentException("Local media folder path is blank.")
        if (trimmed.startsWith("content://", ignoreCase = true)) {
            return trimmed
        }

        val isSmbLikePath = trimmed.startsWith("\\\\") || trimmed.startsWith("//") || trimmed.startsWith("smb://", ignoreCase = true)
        val withoutSchema = if (trimmed.startsWith("smb://", ignoreCase = true)) {
            trimmed.removePrefix("smb://")
        } else {
            trimmed
        }

        var normalized = withoutSchema
            .replace("\\\\", "/")
            .replace("\\", "/")
            .trimEnd('/')
            .trimEnd('\\')

        if (isSmbLikePath) {
            normalized = "//${normalized.trimStart('/', '\\')}"
        }

        if (!isSmbLikePath && normalized.contains("//")) {
            normalized = normalized.replace(Regex("/{2,}"), "/")
        }

        return normalized
    }

    private fun deriveLibraryNameFromRoot(rootUri: String): String {
        val fromPath = if (rootUri.startsWith("content://", ignoreCase = true)) {
            Uri.parse(rootUri).lastPathSegment
        } else {
            rootUri.substringAfterLast('/', missingDelimiterValue = "").ifBlank { null }
        }
        return fromPath?.ifBlank { null } ?: "Local Media"
    }

    private fun parseNetworkShare(rootUri: String): NetworkShareParts {
        val normalized = normalizeLocalMediaRoot(rootUri)
        val sharePath = normalized.removePrefix("//").trim('/')
        val parts = sharePath.split('/').filter { it.isNotBlank() }
        require(parts.size >= 2) { "Network share must include a server and share name." }
        return NetworkShareParts(
            host = parts[0],
            share = parts[1],
            path = parts.drop(2).joinToString("/").ifBlank { null }
        )
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

    private sealed class LocalMediaSource {
        data class DocumentTree(val root: DocumentFile?) : LocalMediaSource()
        data class FileSystem(val root: File) : LocalMediaSource()

        data class Unsupported(val reason: String) : LocalMediaSource()

        companion object {
            fun of(context: Context, rootUri: String): LocalMediaSource {
                val trimmedRootUri = rootUri.trim()
                val parsed = runCatching { Uri.parse(trimmedRootUri) }.getOrNull()
                val filesystemRoot = when {
                    parsed?.scheme?.equals("file", ignoreCase = true) == true -> parsed.path.orEmpty()
                    parsed != null && parsed.scheme.equals("content", ignoreCase = true) -> trimmedRootUri
                    else -> trimmedRootUri
                }

                return if (parsed?.scheme.equals("content", ignoreCase = true)) {
                    LocalMediaSource.DocumentTree(
                        DocumentFile.fromTreeUri(
                            context,
                            requireNotNull(parsed)
                        )
                    )
                } else {
                    LocalMediaSource.FileSystem(File(filesystemRoot))
                }
            }
        }
    }

    private companion object {
        const val MAX_SCAN_DEPTH = 16
        const val MAX_SCAN_FILES = 40_000
    }
}

private data class LocalMediaTreeScan(
    val libraryId: Long,
    val scannedAtMs: Long,
    var scannedCount: Int = 0,
    var skippedCount: Int = 0,
    val items: MutableList<LocalMediaItemEntity> = mutableListOf()
)

private data class NetworkShareParts(
    val host: String,
    val share: String,
    val path: String?
)

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
    folderPath = folderPath,
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
