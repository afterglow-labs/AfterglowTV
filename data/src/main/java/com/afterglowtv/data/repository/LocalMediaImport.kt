package com.afterglowtv.data.repository

import com.afterglowtv.domain.model.LocalMediaKind
import java.util.Locale

data class LocalMediaFileMetadata(
    val title: String,
    val mediaKind: LocalMediaKind,
    val seriesTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)

object LocalMediaImport {
    private val videoExtensions = setOf(
        "3gp",
        "avi",
        "flv",
        "m2ts",
        "m4v",
        "mkv",
        "mov",
        "mp4",
        "mpeg",
        "mpg",
        "ts",
        "webm",
        "wmv"
    )
    private val releaseNoiseTokens = setOf(
        "480p",
        "720p",
        "1080p",
        "2160p",
        "4k",
        "8bit",
        "10bit",
        "aac",
        "amzn",
        "bdrip",
        "bluray",
        "brip",
        "brrip",
        "dl",
        "dts",
        "dvdrip",
        "hdr",
        "hdrip",
        "hevc",
        "h264",
        "h265",
        "nf",
        "proper",
        "remux",
        "rip",
        "uhd",
        "web",
        "webdl",
        "webrip",
        "x264",
        "x265",
        "xvid",
        "yify"
    )
    private val seasonEpisodePattern = Regex("""(?i)^s(\d{1,2})e(\d{1,3})$""")

    fun isImportableVideo(displayName: String?, mimeType: String?): Boolean {
        val normalizedMimeType = mimeType?.trim()?.lowercase(Locale.US).orEmpty()
        if (normalizedMimeType.startsWith("video/")) return true
        val extension = displayName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.US)
            .orEmpty()
        return extension in videoExtensions
    }

    fun describeFile(displayName: String, mimeType: String?): LocalMediaFileMetadata {
        val baseName = displayName.substringBeforeLast('.', displayName)
        val tokens = baseName
            .replace(Regex("""[\[\]{}()]"""), " ")
            .replace(Regex("""[._\-]+"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

        val episodeToken = tokens.firstNotNullOfOrNull { token ->
            seasonEpisodePattern.matchEntire(token)?.let { match ->
                EpisodeToken(
                    raw = token.uppercase(Locale.US),
                    seasonNumber = match.groupValues[1].toInt(),
                    episodeNumber = match.groupValues[2].toInt()
                )
            }
        }
        val cleanedTokens = tokens
            .filterNot { token -> token.lowercase(Locale.US) in releaseNoiseTokens }
            .map { token ->
                val seasonEpisode = seasonEpisodePattern.matchEntire(token)
                if (seasonEpisode != null) {
                    token.uppercase(Locale.US)
                } else {
                    token.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
                    }
                }
            }
        val title = cleanedTokens.joinToString(" ").ifBlank {
            displayName.substringBeforeLast('.', displayName)
        }
        val seriesTitle = episodeToken?.let { token ->
            cleanedTokens.takeWhile { it != token.raw }.joinToString(" ").ifBlank { null }
        }
        return LocalMediaFileMetadata(
            title = title,
            mediaKind = if (episodeToken != null) LocalMediaKind.EPISODE else LocalMediaKind.MOVIE,
            seriesTitle = seriesTitle,
            seasonNumber = episodeToken?.seasonNumber,
            episodeNumber = episodeToken?.episodeNumber
        )
    }

    private data class EpisodeToken(
        val raw: String,
        val seasonNumber: Int,
        val episodeNumber: Int
    )
}
