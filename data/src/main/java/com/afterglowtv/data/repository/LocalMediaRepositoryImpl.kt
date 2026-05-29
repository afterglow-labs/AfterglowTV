package com.afterglowtv.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.afterglowtv.data.local.dao.LocalMediaLibraryDao
import com.afterglowtv.data.local.dao.LocalMediaItemDao
import com.afterglowtv.data.local.entity.LocalMediaItemEntity
import com.afterglowtv.data.local.entity.LocalMediaLibraryEntity
import com.afterglowtv.data.security.CredentialCrypto
import com.afterglowtv.domain.model.LocalMediaBrowseResult
import com.afterglowtv.domain.model.LocalMediaFolderEntry
import com.afterglowtv.domain.model.LocalMediaLibrarySourceType
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.LocalMediaLibrary
import com.afterglowtv.domain.model.LocalMediaScanResult
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.model.SmbMediaSourceResolver
import com.afterglowtv.domain.model.SmbResolvedMedia
import com.afterglowtv.domain.model.SmbShareConfig
import com.afterglowtv.domain.model.SmbShareReference
import com.afterglowtv.domain.model.SmbShareUri
import com.afterglowtv.domain.repository.LocalMediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.EnumSet
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryDao: LocalMediaLibraryDao,
    private val itemDao: LocalMediaItemDao,
    private val credentialCrypto: CredentialCrypto
) : LocalMediaRepository, SmbMediaSourceResolver {
    override fun observeLibraries(): Flow<List<LocalMediaLibrary>> =
        libraryDao.observeLibraries().map { libraries -> libraries.map { it.toDomain() } }

    override fun observeItems(libraryId: Long?): Flow<List<LocalMediaItem>> =
        (libraryId?.let(itemDao::observeByLibrary) ?: itemDao.observeAll())
            .map { items -> items.map { it.toDomain() } }

    override fun observeItemsPage(
        limit: Int,
        mediaKinds: Set<LocalMediaKind>?,
        libraryIds: Set<Long>?
    ): Flow<List<LocalMediaItem>> {
        val boundedLimit = limit.coerceAtLeast(1)
        val kinds = mediaKinds?.takeIf { it.isNotEmpty() }?.toList()
        val libraries = libraryIds?.toList()
        if (libraryIds != null && libraries.isNullOrEmpty()) return flowOf(emptyList())
        return when {
            kinds != null && libraries != null -> itemDao.observeByMediaKindsAndLibrariesPage(kinds, libraries, boundedLimit)
            kinds != null -> itemDao.observeByMediaKindsPage(kinds, boundedLimit)
            libraries != null -> itemDao.observeByLibrariesPage(libraries, boundedLimit)
            else -> itemDao.observeAllPage(boundedLimit)
        }
            .map { items -> items.map { it.toDomain() } }
    }

    override fun observeItemCount(
        mediaKinds: Set<LocalMediaKind>?,
        libraryIds: Set<Long>?
    ): Flow<Int> {
        val kinds = mediaKinds?.takeIf { it.isNotEmpty() }?.toList()
        val libraries = libraryIds?.toList()
        if (libraryIds != null && libraries.isNullOrEmpty()) return flowOf(0)
        return when {
            kinds != null && libraries != null -> itemDao.observeCountByMediaKindsAndLibraries(kinds, libraries)
            kinds != null -> itemDao.observeCountByMediaKinds(kinds)
            libraries != null -> itemDao.observeCountByLibraries(libraries)
            else -> itemDao.observeCount()
        }
    }

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
                sourceType = LocalMediaLibrarySourceType.DOCUMENT_TREE,
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
            onFailure = { error ->
                if (error is CancellationException) throw error
                Result.error(error.message ?: "Failed to add local media library.", error)
            }
        )
    }

    override suspend fun addSmbLibrary(config: SmbShareConfig): Result<LocalMediaScanResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val reference = SmbShareUri.fromConfig(config)
                val rootUri = SmbShareUri.buildRoot(reference)
                val now = System.currentTimeMillis()
                val existing = libraryDao.getLibraryByRootUri(rootUri)
                val name = config.displayName?.takeIf { it.isNotBlank() }
                    ?: existing?.name
                    ?: SmbShareUri.displayName(reference)
                val entity = LocalMediaLibraryEntity(
                    id = existing?.id ?: 0L,
                    name = name,
                    rootUri = rootUri,
                    sourceType = LocalMediaLibrarySourceType.SMB,
                    displayName = config.displayName?.takeIf { it.isNotBlank() } ?: existing?.displayName,
                    enabled = true,
                    itemCount = existing?.itemCount ?: 0,
                    addedAtMs = existing?.addedAtMs ?: now,
                    updatedAtMs = now,
                    lastScannedAtMs = existing?.lastScannedAtMs,
                    smbHost = reference.host,
                    smbPort = reference.port,
                    smbShare = reference.shareName,
                    smbPath = reference.path.takeIf { it.isNotBlank() },
                    smbDomain = config.domain.trim().takeIf { it.isNotBlank() },
                    smbUsername = config.username.trim().takeIf { it.isNotBlank() },
                    smbPassword = config.password.takeIf { it.isNotBlank() }?.let(credentialCrypto::encryptIfNeeded)
                        ?: existing?.smbPassword
                )
                val insertedId = libraryDao.upsertLibrary(entity)
                val libraryId = existing?.id ?: insertedId
                val savedLibrary = libraryDao.getLibrary(libraryId) ?: entity.copy(id = libraryId)
                validateSmbLibrary(savedLibrary)
                LocalMediaScanResult(
                    libraryId = libraryId,
                    scannedCount = 0,
                    importedCount = 0,
                    skippedCount = 0
                )
            }.fold(
                onSuccess = { Result.success(it) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    Result.error(error.message ?: "Failed to add network share.", error)
                }
            )
        }

    override suspend fun browseLibrary(
        libraryId: Long,
        path: String
    ): Result<LocalMediaBrowseResult> = withContext(Dispatchers.IO) {
        runCatching {
            val library = libraryDao.getLibrary(libraryId)
                ?: throw IllegalArgumentException("Local media library was not found.")
            when (library.sourceType) {
                LocalMediaLibrarySourceType.SMB -> browseSmbLibrary(library, path)
                LocalMediaLibrarySourceType.DOCUMENT_TREE -> browseDocumentLibrary(library, path)
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                Result.error(error.message ?: "Failed to browse local media library.", error)
            }
        )
    }

    override suspend fun rescanLibrary(libraryId: Long): Result<LocalMediaScanResult> = withContext(Dispatchers.IO) {
        runCatching {
            val library = libraryDao.getLibrary(libraryId)
                ?: throw IllegalArgumentException("Local media library was not found.")
            scanLibrary(library)
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                Result.error(error.message ?: "Failed to scan local media library.", error)
            }
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

    override fun resolve(uri: String): SmbResolvedMedia? {
        val itemReference = SmbShareUri.parse(uri) ?: return null
        val canonicalUri = SmbShareUri.buildRoot(itemReference)
        val library = libraryDao.getSmbLibrariesBlocking()
            .firstOrNull { candidate ->
                val root = candidate.rootUri.trimEnd('/')
                canonicalUri == root || canonicalUri.startsWith("$root/")
            }
            ?: return null
        return SmbResolvedMedia(
            host = itemReference.host,
            port = itemReference.port,
            shareName = itemReference.shareName,
            path = itemReference.path,
            username = library.smbUsername.orEmpty(),
            password = library.smbPassword.orEmpty().let(credentialCrypto::decryptIfNeeded),
            domain = library.smbDomain.orEmpty()
        )
    }

    private suspend fun validateSmbLibrary(library: LocalMediaLibraryEntity) {
        val reference = SmbShareUri.parse(library.rootUri)
            ?: throw IllegalArgumentException("Network share path is invalid.")
        withSmbShare(library, reference) { share ->
            if (reference.path.isNotBlank() && !share.folderExists(reference.path)) {
                throw IllegalArgumentException("Network share folder is not available.")
            }
            share.list(reference.path.ifBlank { "" })
        }
        val now = System.currentTimeMillis()
        libraryDao.updateScanState(
            libraryId = library.id,
            itemCount = library.itemCount,
            scannedAtMs = now,
            updatedAtMs = now
        )
    }

    private suspend fun browseSmbLibrary(
        library: LocalMediaLibraryEntity,
        path: String
    ): LocalMediaBrowseResult {
        val reference = SmbShareUri.parse(library.rootUri)
            ?: throw IllegalArgumentException("Network share path is invalid.")
        val browsePath = SmbShareUri.normalizePath(path)
        val sharePath = joinSmbPath(reference.path, browsePath)
        val folders = mutableListOf<LocalMediaFolderEntry>()
        val items = mutableListOf<LocalMediaItem>()
        withSmbShare(library, reference) { share ->
            if (sharePath.isNotBlank() && !share.folderExists(sharePath)) {
                throw IllegalArgumentException("Network share folder is not available.")
            }
            for (entry in share.list(sharePath.ifBlank { "" })) {
                currentCoroutineContext().ensureActive()
                val displayName = entry.fileName.orEmpty()
                if (displayName == "." || displayName == "..") continue
                val relativePath = joinSmbPath(browsePath, displayName)
                val fullPath = joinSmbPath(reference.path, relativePath)
                val isDirectory = (entry.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                if (isDirectory) {
                    folders += LocalMediaFolderEntry(
                        name = displayName,
                        path = relativePath
                    )
                    continue
                }
                if (!LocalMediaImport.isImportableVideo(displayName, mimeType = null)) continue
                val metadata = LocalMediaImport.describeFile(displayName, mimeType = null)
                val uri = SmbShareUri.build(
                    host = reference.host,
                    port = reference.port,
                    shareName = reference.shareName,
                    path = fullPath
                )
                items += LocalMediaItem(
                    id = uri.hashCode().toLong(),
                    libraryId = library.id,
                    uri = uri,
                    displayName = displayName,
                    title = metadata.title,
                    sortTitle = metadata.title.toSortTitle(),
                    mediaKind = metadata.mediaKind,
                    mimeType = null,
                    durationMs = null,
                    sizeBytes = entry.endOfFile.takeIf { it > 0L },
                    dateModifiedMs = entry.lastWriteTime?.toEpochMillis()?.takeIf { it > 0L },
                    seriesTitle = metadata.seriesTitle,
                    seasonNumber = metadata.seasonNumber,
                    episodeNumber = metadata.episodeNumber,
                    addedAtMs = library.addedAtMs,
                    updatedAtMs = System.currentTimeMillis(),
                    lastScannedAtMs = library.lastScannedAtMs
                )
            }
        }
        return LocalMediaBrowseResult(
            library = library.toDomain(),
            path = browsePath,
            folders = folders.sortedBy { it.name.lowercase(Locale.US) },
            items = items.sortedBy { it.sortTitle.lowercase(Locale.US) }
        )
    }

    private suspend fun browseDocumentLibrary(
        library: LocalMediaLibraryEntity,
        path: String
    ): LocalMediaBrowseResult {
        val items = itemDao.getByLibrary(library.id).map { it.toDomain() }
        return LocalMediaBrowseResult(
            library = library.toDomain(),
            path = SmbShareUri.normalizePath(path),
            folders = emptyList(),
            items = items
        )
    }

    private suspend fun scanLibrary(library: LocalMediaLibraryEntity): LocalMediaScanResult =
        when (library.sourceType) {
            LocalMediaLibrarySourceType.DOCUMENT_TREE -> scanDocumentLibrary(library)
            LocalMediaLibrarySourceType.SMB -> {
                validateSmbLibrary(library)
                LocalMediaScanResult(
                    libraryId = library.id,
                    scannedCount = 0,
                    importedCount = 0,
                    skippedCount = 0
                )
            }
        }

    private suspend fun scanDocumentLibrary(library: LocalMediaLibraryEntity): LocalMediaScanResult {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(library.rootUri))
            ?: throw IllegalArgumentException("Local media folder is no longer accessible.")
        if (!root.exists() || !root.isDirectory) {
            throw IllegalArgumentException("Local media folder is not available.")
        }

        val now = System.currentTimeMillis()
        val scan = LocalMediaTreeScan(libraryId = library.id, scannedAtMs = now)
        walkTree(root, scan, depth = 0)
        flushScanItems(scan)
        itemDao.deleteStaleByLibrary(library.id, now)
        libraryDao.updateScanState(
            libraryId = library.id,
            itemCount = scan.importedCount,
            scannedAtMs = now,
            updatedAtMs = now
        )
        return LocalMediaScanResult(
            libraryId = library.id,
            scannedCount = scan.scannedCount,
            importedCount = scan.importedCount,
            skippedCount = scan.skippedCount
        )
    }

    private suspend fun scanSmbLibrary(library: LocalMediaLibraryEntity): LocalMediaScanResult {
        val reference = SmbShareUri.parse(library.rootUri)
            ?: throw IllegalArgumentException("Network share path is invalid.")
        val now = System.currentTimeMillis()
        val scan = LocalMediaTreeScan(libraryId = library.id, scannedAtMs = now)
        withSmbShare(library, reference) { share ->
            if (reference.path.isNotBlank() && !share.folderExists(reference.path)) {
                throw IllegalArgumentException("Network share folder is not available.")
            }
            walkSmbTree(
                share = share,
                reference = reference,
                currentPath = reference.path,
                scan = scan,
                depth = 0
            )
        }
        flushScanItems(scan)
        itemDao.deleteStaleByLibrary(library.id, now)
        libraryDao.updateScanState(
            libraryId = library.id,
            itemCount = scan.importedCount,
            scannedAtMs = now,
            updatedAtMs = now
        )
        return LocalMediaScanResult(
            libraryId = library.id,
            scannedCount = scan.scannedCount,
            importedCount = scan.importedCount,
            skippedCount = scan.skippedCount
        )
    }

    private suspend fun walkTree(
        document: DocumentFile,
        scan: LocalMediaTreeScan,
        depth: Int
    ) {
        currentCoroutineContext().ensureActive()
        if (depth > MAX_SCAN_DEPTH) return
        val children = runCatching { document.listFiles().toList() }.getOrDefault(emptyList())
        for (child in children) {
            currentCoroutineContext().ensureActive()
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
            scan.addItem(LocalMediaItemEntity(
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
            ))
            flushScanItemsIfNeeded(scan)
        }
    }

    private suspend fun walkSmbTree(
        share: DiskShare,
        reference: SmbShareReference,
        currentPath: String,
        scan: LocalMediaTreeScan,
        depth: Int
    ) {
        currentCoroutineContext().ensureActive()
        if (depth > MAX_SCAN_DEPTH) return
        val entries = share.list(currentPath.ifBlank { "" })
        for (entry in entries) {
            currentCoroutineContext().ensureActive()
            val displayName = entry.fileName.orEmpty()
            if (displayName == "." || displayName == "..") continue
            val childPath = joinSmbPath(currentPath, displayName)
            val isDirectory = (entry.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
            if (isDirectory) {
                walkSmbTree(share, reference, childPath, scan, depth + 1)
                continue
            }
            scan.scannedCount += 1
            if (!LocalMediaImport.isImportableVideo(displayName, mimeType = null)) {
                scan.skippedCount += 1
                continue
            }
            val metadata = LocalMediaImport.describeFile(displayName, mimeType = null)
            scan.addItem(LocalMediaItemEntity(
                libraryId = scan.libraryId,
                uri = SmbShareUri.build(
                    host = reference.host,
                    port = reference.port,
                    shareName = reference.shareName,
                    path = childPath
                ),
                displayName = displayName,
                title = metadata.title,
                sortTitle = metadata.title.toSortTitle(),
                mediaKind = metadata.mediaKind,
                mimeType = null,
                durationMs = null,
                sizeBytes = entry.endOfFile.takeIf { it > 0L },
                dateModifiedMs = entry.lastWriteTime?.toEpochMillis()?.takeIf { it > 0L },
                seriesTitle = metadata.seriesTitle,
                seasonNumber = metadata.seasonNumber,
                episodeNumber = metadata.episodeNumber,
                addedAtMs = scan.scannedAtMs,
                updatedAtMs = scan.scannedAtMs,
                lastScannedAtMs = scan.scannedAtMs
            ))
            flushScanItemsIfNeeded(scan)
        }
    }

    private suspend fun <T> withSmbShare(
        library: LocalMediaLibraryEntity,
        reference: SmbShareReference,
        block: suspend (DiskShare) -> T
    ): T {
        val config = SmbConfig.builder()
            .withTimeout(SMB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .withSoTimeout(SMB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        return SMBClient(config).use { client ->
            client.connect(reference.host, reference.port).use { connection ->
                val username = library.smbUsername.orEmpty()
                val password = library.smbPassword.orEmpty().let(credentialCrypto::decryptIfNeeded)
                val domain = library.smbDomain.orEmpty().takeIf { it.isNotBlank() }
                val authentication = if (username.isBlank() && password.isBlank()) {
                    AuthenticationContext.guest()
                } else {
                    AuthenticationContext(username, password.toCharArray(), domain)
                }
                connection.authenticate(authentication).use { session ->
                    val share = session.connectShare(reference.shareName) as? DiskShare
                        ?: throw IllegalArgumentException("Network share is not a file share.")
                    try {
                        block(share)
                    } finally {
                        share.close()
                    }
                }
            }
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

    private suspend fun flushScanItemsIfNeeded(scan: LocalMediaTreeScan) {
        if (scan.pendingItems.size >= SCAN_SAVEPOINT_BATCH_SIZE) {
            flushScanItems(scan)
        }
    }

    private suspend fun flushScanItems(scan: LocalMediaTreeScan) {
        if (scan.pendingItems.isEmpty()) return
        currentCoroutineContext().ensureActive()
        val items = scan.pendingItems.toList()
        itemDao.upsertItems(items)
        scan.pendingItems.clear()
        scan.importedCount += items.size
        libraryDao.updateScanState(
            libraryId = scan.libraryId,
            itemCount = scan.importedCount,
            scannedAtMs = scan.scannedAtMs,
            updatedAtMs = System.currentTimeMillis()
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

    private data class LocalMediaTreeScan(
        val libraryId: Long,
        val scannedAtMs: Long,
        var scannedCount: Int = 0,
        var skippedCount: Int = 0,
        var importedCount: Int = 0,
        val pendingItems: MutableList<LocalMediaItemEntity> = mutableListOf()
    ) {
        fun addItem(item: LocalMediaItemEntity) {
            pendingItems += item
        }
    }

    private companion object {
        const val MAX_SCAN_DEPTH = 16
        const val SMB_TIMEOUT_SECONDS = 20L
        const val SCAN_SAVEPOINT_BATCH_SIZE = 200
    }
}

private fun LocalMediaLibraryEntity.toDomain(): LocalMediaLibrary = LocalMediaLibrary(
    id = id,
    name = name,
    rootUri = rootUri,
    sourceType = sourceType ?: LocalMediaLibrarySourceType.DOCUMENT_TREE,
    displayName = displayName,
    enabled = enabled,
    itemCount = itemCount,
    addedAtMs = addedAtMs,
    updatedAtMs = updatedAtMs,
    lastScannedAtMs = lastScannedAtMs
)

private fun joinSmbPath(parent: String, child: String): String =
    listOf(parent.trim('/'), child.trim('/'))
        .filter { it.isNotBlank() }
        .joinToString("/")

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
