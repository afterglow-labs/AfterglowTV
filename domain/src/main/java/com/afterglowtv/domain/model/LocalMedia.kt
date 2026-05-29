package com.afterglowtv.domain.model

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class LocalMediaKind {
    MOVIE,
    EPISODE,
    EXTRA,
    UNKNOWN
}

enum class LocalMediaLibrarySourceType {
    DOCUMENT_TREE,
    SMB
}

data class LocalMediaLibrary(
    val id: Long = 0,
    val name: String,
    val rootUri: String,
    val sourceType: LocalMediaLibrarySourceType = LocalMediaLibrarySourceType.DOCUMENT_TREE,
    val displayName: String? = null,
    val enabled: Boolean = true,
    val itemCount: Int = 0,
    val addedAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val lastScannedAtMs: Long? = null
)

data class LocalMediaItem(
    val id: Long = 0,
    val libraryId: Long,
    val uri: String,
    val displayName: String,
    val title: String,
    val sortTitle: String = title,
    val mediaKind: LocalMediaKind = LocalMediaKind.UNKNOWN,
    val mimeType: String? = null,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val dateModifiedMs: Long? = null,
    val posterUri: String? = null,
    val backdropUri: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val releaseYear: Int? = null,
    val seriesTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val addedAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val lastScannedAtMs: Long? = null
)

data class LocalMediaChannel(
    val id: Long = 0,
    val name: String,
    val libraryId: Long? = null,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

data class LocalMediaProgram(
    val id: Long = 0,
    val channelId: Long,
    val mediaItemId: Long,
    val title: String,
    val description: String? = null,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val mediaDurationMs: Long? = null,
    val artworkUri: String? = null
) {
    init {
        require(endTimeMs > startTimeMs) { "LocalMediaProgram endTimeMs must be after startTimeMs" }
        mediaDurationMs?.let { require(it >= 0L) { "mediaDurationMs must be non-negative" } }
    }
}

data class LocalMediaPlaybackStart(
    val mediaItemId: Long,
    val startPositionMs: Long,
    val title: String
)

data class LocalMediaFolderEntry(
    val name: String,
    val path: String
)

data class LocalMediaBrowseResult(
    val library: LocalMediaLibrary,
    val path: String = "",
    val folders: List<LocalMediaFolderEntry> = emptyList(),
    val items: List<LocalMediaItem> = emptyList()
)

data class LocalMediaScanResult(
    val libraryId: Long,
    val scannedCount: Int,
    val importedCount: Int,
    val skippedCount: Int
)

data class SmbShareConfig(
    val host: String,
    val shareName: String,
    val path: String = "",
    val displayName: String? = null,
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val port: Int = SmbShareUri.DEFAULT_PORT
)

data class SmbShareReference(
    val host: String,
    val port: Int = SmbShareUri.DEFAULT_PORT,
    val shareName: String,
    val path: String = ""
)

data class SmbResolvedMedia(
    val host: String,
    val port: Int = SmbShareUri.DEFAULT_PORT,
    val shareName: String,
    val path: String,
    val username: String = "",
    val password: String = "",
    val domain: String = ""
)

interface SmbMediaSourceResolver {
    fun resolve(uri: String): SmbResolvedMedia?
}

object NoopSmbMediaSourceResolver : SmbMediaSourceResolver {
    override fun resolve(uri: String): SmbResolvedMedia? = null
}

object SmbShareUri {
    const val DEFAULT_PORT = 445

    fun fromConfig(config: SmbShareConfig): SmbShareReference =
        SmbShareReference(
            host = config.host.trim(),
            port = config.port,
            shareName = config.shareName.trim(),
            path = normalizePath(config.path)
        ).also(::validate)

    fun parse(input: String): SmbShareReference? {
        val normalizedInput = input.trim().takeIf { it.isNotBlank() } ?: return null
        val uriText = when {
            normalizedInput.startsWith("\\\\") -> {
                val unixPath = normalizedInput.replace('\\', '/').trimStart('/')
                "smb://$unixPath"
            }
            normalizedInput.startsWith("//") -> "smb:$normalizedInput"
            normalizedInput.startsWith("smb://", ignoreCase = true) -> normalizedInput
            else -> return null
        }
        return runCatching {
            val uri = URI(uriText.replace(" ", "%20"))
            val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
            val port = uri.port.takeIf { it > 0 } ?: DEFAULT_PORT
            val pathSegments = uri.rawPath
                ?.trim('/')
                ?.split('/')
                ?.filter { it.isNotBlank() }
                ?.map(::decodeSegment)
                .orEmpty()
            val shareName = pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            SmbShareReference(
                host = host,
                port = port,
                shareName = shareName,
                path = normalizePath(pathSegments.drop(1).joinToString("/"))
            ).also(::validate)
        }.getOrNull()
    }

    fun buildRoot(reference: SmbShareReference): String = build(
        host = reference.host,
        port = reference.port,
        shareName = reference.shareName,
        path = reference.path
    )

    fun build(
        host: String,
        port: Int = DEFAULT_PORT,
        shareName: String,
        path: String = ""
    ): String {
        val reference = SmbShareReference(
            host = host.trim(),
            port = port,
            shareName = shareName.trim(),
            path = normalizePath(path)
        ).also(::validate)
        val hostAndPort = if (reference.port == DEFAULT_PORT) {
            reference.host
        } else {
            "${reference.host}:${reference.port}"
        }
        val encodedPath = listOf(reference.shareName)
            .plus(reference.path.split('/').filter { it.isNotBlank() })
            .joinToString("/") { encodeSegment(it) }
        return "smb://$hostAndPort/$encodedPath"
    }

    fun displayName(reference: SmbShareReference): String =
        listOf(reference.host, reference.shareName, reference.path)
            .filter { it.isNotBlank() }
            .joinToString("/")

    fun normalizePath(path: String): String =
        path.trim()
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() }
            .joinToString("/")

    private fun validate(reference: SmbShareReference) {
        require(reference.host.isNotBlank()) { "SMB host is required." }
        require(reference.shareName.isNotBlank()) { "SMB share name is required." }
        require(reference.port in 1..65_535) { "SMB port must be between 1 and 65535." }
    }

    private fun encodeSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun decodeSegment(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}

object LocalMediaPlaybackResolver {
    private const val END_GUARD_MS = 1_000L

    fun resolvePseudoLivePlayback(
        program: LocalMediaProgram,
        nowMs: Long = System.currentTimeMillis()
    ): LocalMediaPlaybackStart? {
        if (nowMs < program.startTimeMs || nowMs >= program.endTimeMs) return null
        val elapsedMs = (nowMs - program.startTimeMs).coerceAtLeast(0L)
        val boundedStartMs = program.mediaDurationMs
            ?.takeIf { it > 0L }
            ?.let { duration -> elapsedMs.coerceAtMost((duration - END_GUARD_MS).coerceAtLeast(0L)) }
            ?: elapsedMs
        return LocalMediaPlaybackStart(
            mediaItemId = program.mediaItemId,
            startPositionMs = boundedStartMs,
            title = program.title
        )
    }
}

object LocalMediaScheduler {
    private const val DEFAULT_PROGRAM_DURATION_MS = 30 * 60_000L

    fun buildSequentialPrograms(
        channelId: Long,
        mediaItems: List<LocalMediaItem>,
        windowStartMs: Long,
        idSeed: Long = 0L,
        fallbackDurationMs: Long = DEFAULT_PROGRAM_DURATION_MS
    ): List<LocalMediaProgram> {
        require(fallbackDurationMs > 0L) { "fallbackDurationMs must be positive" }
        var cursor = windowStartMs
        return mediaItems.mapIndexed { index, item ->
            val duration = item.durationMs?.takeIf { it > 0L } ?: fallbackDurationMs
            LocalMediaProgram(
                id = idSeed + index,
                channelId = channelId,
                mediaItemId = item.id,
                title = item.title,
                description = item.description,
                startTimeMs = cursor,
                endTimeMs = cursor + duration,
                mediaDurationMs = item.durationMs,
                artworkUri = item.posterUri ?: item.backdropUri
            ).also { cursor = it.endTimeMs }
        }
    }
}
