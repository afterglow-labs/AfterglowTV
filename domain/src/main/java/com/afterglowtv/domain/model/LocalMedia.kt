package com.afterglowtv.domain.model

enum class LocalMediaKind {
    MOVIE,
    EPISODE,
    EXTRA,
    UNKNOWN
}

data class LocalMediaLibrary(
    val id: Long = 0,
    val name: String,
    val rootUri: String,
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

data class LocalMediaScanResult(
    val libraryId: Long,
    val scannedCount: Int,
    val importedCount: Int,
    val skippedCount: Int
)

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
