package com.afterglowtv.data.sync

import android.util.Log
import com.afterglowtv.data.local.entity.MovieEntity
import com.afterglowtv.data.local.entity.SeriesEntity
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

data class VodMetadataSearchSeed(
    val query: String,
    val year: String? = null
)

data class CinemetaSearchMatch(
    val id: String,
    val type: String,
    val name: String,
    val releaseInfo: String?,
    val poster: String?,
    val background: String?
)

data class VodOnlineMetadata(
    val id: String,
    val name: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val duration: String?,
    val rating: Float,
    val year: String?,
    val tmdbId: Long?,
    val youtubeTrailer: String?
)

@Singleton
class VodOnlineMetadataClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    suspend fun findMovieMetadata(movie: MovieEntity): VodOnlineMetadata? {
        return findMetadata(
            type = CINEMETA_TYPE_MOVIE,
            seed = buildVodMetadataSearchSeed(
                rawTitle = movie.name,
                fallbackYear = movie.year ?: movie.releaseDate?.take(4),
                categoryName = movie.categoryName
            )
        )
    }

    suspend fun findSeriesMetadata(series: SeriesEntity): VodOnlineMetadata? {
        return findMetadata(
            type = CINEMETA_TYPE_SERIES,
            seed = buildVodMetadataSearchSeed(
                rawTitle = series.name,
                fallbackYear = series.releaseDate?.take(4),
                categoryName = series.categoryName
            )
        )
    }

    private suspend fun findMetadata(
        type: String,
        seed: VodMetadataSearchSeed
    ): VodOnlineMetadata? {
        if (seed.query.length < MIN_QUERY_LENGTH) return null

        val matches = fetchSearchMatches(type, seed.query)
        val best = selectBestCinemetaMatch(seed, matches) ?: return null
        return fetchMeta(type, best.id) ?: best.toPreviewMetadata()
    }

    private suspend fun fetchSearchMatches(type: String, query: String): List<CinemetaSearchMatch> {
        val url = "$CINEMETA_BASE_URL/catalog/$type/top/search=${query.pathEncode()}.json"
        val root = fetchJsonObject(url) ?: return emptyList()
        val metas = root["metas"] as? JsonArray ?: return emptyList()
        return metas.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            CinemetaSearchMatch(
                id = obj.string("id") ?: obj.string("imdb_id") ?: return@mapNotNull null,
                type = obj.string("type") ?: type,
                name = obj.string("name") ?: return@mapNotNull null,
                releaseInfo = obj.string("releaseInfo"),
                poster = obj.string("poster"),
                background = obj.string("background")
            )
        }
    }

    private suspend fun fetchMeta(type: String, id: String): VodOnlineMetadata? {
        val root = fetchJsonObject("$CINEMETA_BASE_URL/meta/$type/${id.pathEncode()}.json") ?: return null
        val meta = root["meta"] as? JsonObject ?: return null
        val name = meta.string("name") ?: return null
        val releaseDate = meta.string("released")?.take(10)
        val year = meta.string("year") ?: meta.string("releaseInfo")?.extractYear() ?: releaseDate?.take(4)
        return VodOnlineMetadata(
            id = meta.string("id") ?: meta.string("imdb_id") ?: id,
            name = name,
            posterUrl = meta.string("poster"),
            backdropUrl = meta.string("background"),
            plot = meta.string("description"),
            cast = meta.stringList("cast").joinToStringOrNull(),
            director = meta.stringList("director").joinToStringOrNull(),
            genre = (meta.stringList("genre") + meta.stringList("genres")).distinct().joinToStringOrNull(),
            releaseDate = releaseDate ?: year,
            duration = meta.string("runtime"),
            rating = meta.string("imdbRating")?.toFloatOrNull() ?: meta.float("imdbRating") ?: 0f,
            year = year,
            tmdbId = meta.long("moviedb_id"),
            youtubeTrailer = meta.firstTrailer()
        )
    }

    private suspend fun fetchJsonObject(url: String): JsonObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                json.parseToJsonElement(body).jsonObject
            }
        }.onFailure { error ->
            Log.d(TAG, "Online VOD metadata lookup failed for $url", error)
        }.getOrNull()
    }

    private fun CinemetaSearchMatch.toPreviewMetadata(): VodOnlineMetadata =
        VodOnlineMetadata(
            id = id,
            name = name,
            posterUrl = poster,
            backdropUrl = background,
            plot = null,
            cast = null,
            director = null,
            genre = null,
            releaseDate = releaseInfo,
            duration = null,
            rating = 0f,
            year = releaseInfo?.extractYear(),
            tmdbId = null,
            youtubeTrailer = null
        )

    private companion object {
        const val TAG = "VodOnlineMetadata"
    }
}

internal fun buildVodMetadataSearchSeed(
    rawTitle: String,
    fallbackYear: String?,
    categoryName: String?
): VodMetadataSearchSeed {
    val normalized = rawTitle
        .replace(EXTENSION_PATTERN, "")
        .replace(Regex("""[\[\]{}()]"""), " ")
        .replace(Regex("""[._]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    val year = YEAR_PATTERN.find(normalized)?.value ?: fallbackYear?.extractYear()
    val categoryTokens = categoryName.searchTokens()
    val tokens = normalized
        .split(' ')
        .map { it.trim(',', '-', ':', ';', '|') }
        .filter { token ->
            val compact = token.lowercase(Locale.US).replace(".", "")
            token.isNotBlank() &&
                token != year &&
                compact !in RELEASE_NOISE_TOKENS &&
                compact !in categoryTokens
        }

    val query = tokens
        .joinToString(" ")
        .replace(Regex("""\s+[-:]\s*$"""), "")
        .trim()
        .ifBlank { rawTitle.trim() }

    return VodMetadataSearchSeed(
        query = query,
        year = year
    )
}

internal fun selectBestCinemetaMatch(
    seed: VodMetadataSearchSeed,
    matches: List<CinemetaSearchMatch>
): CinemetaSearchMatch? {
    val query = seed.query.normalizedTitle()
    val queryTokens = query.searchTokens()
    if (queryTokens.isEmpty()) return null

    return matches
        .mapNotNull { match ->
            val candidate = match.name.normalizedTitle()
            val candidateTokens = candidate.searchTokens()
            val tokenScore = tokenOverlap(queryTokens, candidateTokens)
            val exact = query == candidate
            val score = buildList {
                add(tokenScore * 40f)
                if (exact) add(45f)
                if (!exact && (candidate.startsWith(query) || query.startsWith(candidate))) add(10f)
                add(yearMatchScore(seed.year, match.releaseInfo?.extractYear()))
            }.sum()

            if ((exact || tokenScore >= MIN_TOKEN_OVERLAP) && score >= MIN_MATCH_SCORE) {
                match to score
            } else {
                null
            }
        }
        .maxByOrNull { it.second }
        ?.first
}

internal fun mergeMovieOnlineMetadata(
    movie: MovieEntity,
    metadata: VodOnlineMetadata
): MovieEntity {
    val durationSeconds = movie.durationSeconds.takeIf { it > 0 } ?: metadata.duration.parseMinutesToSeconds()
    return movie.copy(
        posterUrl = movie.posterUrl.fillBlank(metadata.posterUrl),
        backdropUrl = movie.backdropUrl.fillBlank(metadata.backdropUrl),
        plot = movie.plot.fillBlank(metadata.plot),
        cast = movie.cast.fillBlank(metadata.cast),
        director = movie.director.fillBlank(metadata.director),
        genre = movie.genre.fillBlank(metadata.genre),
        releaseDate = movie.releaseDate.fillBlank(metadata.releaseDate),
        duration = movie.duration.fillBlank(metadata.duration),
        durationSeconds = durationSeconds ?: movie.durationSeconds,
        rating = movie.rating.takeIf { it > 0f } ?: metadata.rating.coerceIn(0f, 10f),
        year = movie.year.fillBlank(metadata.year ?: metadata.releaseDate?.extractYear()),
        tmdbId = movie.tmdbId ?: metadata.tmdbId,
        youtubeTrailer = movie.youtubeTrailer.fillBlank(metadata.youtubeTrailer),
        cacheState = VOD_CACHE_STATE_DETAIL_HYDRATED,
        detailHydratedAt = System.currentTimeMillis()
    )
}

internal fun mergeSeriesOnlineMetadata(
    series: SeriesEntity,
    metadata: VodOnlineMetadata
): SeriesEntity {
    return series.copy(
        posterUrl = series.posterUrl.fillBlank(metadata.posterUrl),
        backdropUrl = series.backdropUrl.fillBlank(metadata.backdropUrl),
        plot = series.plot.fillBlank(metadata.plot),
        cast = series.cast.fillBlank(metadata.cast),
        director = series.director.fillBlank(metadata.director),
        genre = series.genre.fillBlank(metadata.genre),
        releaseDate = series.releaseDate.fillBlank(metadata.releaseDate ?: metadata.year),
        rating = series.rating.takeIf { it > 0f } ?: metadata.rating.coerceIn(0f, 10f),
        tmdbId = series.tmdbId ?: metadata.tmdbId,
        youtubeTrailer = series.youtubeTrailer.fillBlank(metadata.youtubeTrailer),
        episodeRunTime = series.episodeRunTime.fillBlank(metadata.duration),
        cacheState = VOD_CACHE_STATE_DETAIL_HYDRATED,
        detailHydratedAt = System.currentTimeMillis()
    )
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.content
        ?.takeIf { it.isNotBlank() }

private fun JsonObject.float(key: String): Float? =
    (this[key] as? JsonPrimitive)?.floatOrNull

private fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.longOrNull

private fun JsonObject.stringList(key: String): List<String> {
    return when (val value = this[key]) {
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.content?.takeIf(String::isNotBlank) }
        is JsonPrimitive -> value.content
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        else -> emptyList()
    }
}

private fun JsonObject.firstTrailer(): String? {
    val trailerStreams = this["trailerStreams"] as? JsonArray
    trailerStreams
        ?.mapNotNull { (it as? JsonObject)?.string("ytId") }
        ?.firstOrNull()
        ?.let { return it }

    val trailers = this["trailers"] as? JsonArray
    return trailers
        ?.mapNotNull { (it as? JsonObject)?.string("source") }
        ?.firstOrNull()
}

private fun List<String>.joinToStringOrNull(): String? =
    takeIf { it.isNotEmpty() }?.joinToString(", ")

private fun String?.fillBlank(candidate: String?): String? =
    this?.takeIf { it.isNotBlank() } ?: candidate?.takeIf { it.isNotBlank() }

private fun String?.parseMinutesToSeconds(): Int? {
    val minutes = this?.let { Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.getOrNull(1) }
        ?.toIntOrNull()
    return minutes?.times(60)
}

private fun String.pathEncode(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

private fun String?.extractYear(): String? =
    this?.let { YEAR_PATTERN.find(it)?.value }

private fun String.normalizedTitle(): String =
    lowercase(Locale.US)
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

private fun String?.searchTokens(): Set<String> =
    this
        ?.normalizedTitle()
        ?.split(' ')
        ?.filter { it.length > 1 && it !in STOP_WORDS }
        ?.toSet()
        .orEmpty()

private fun tokenOverlap(left: Set<String>, right: Set<String>): Float {
    if (left.isEmpty() || right.isEmpty()) return 0f
    return left.intersect(right).size.toFloat() / left.union(right).size.toFloat()
}

private fun yearMatchScore(seedYear: String?, candidateYear: String?): Float {
    if (seedYear == null) return 0f
    val seed = seedYear.toIntOrNull() ?: return 0f
    val candidate = candidateYear?.toIntOrNull() ?: return 0f
    val distance = kotlin.math.abs(seed - candidate)
    return when {
        distance == 0 -> 20f
        distance == 1 -> 8f
        else -> -18f
    }
}

private const val CINEMETA_BASE_URL = "https://v3-cinemeta.strem.io"
private const val CINEMETA_TYPE_MOVIE = "movie"
private const val CINEMETA_TYPE_SERIES = "series"
private const val MIN_QUERY_LENGTH = 2
private const val MIN_TOKEN_OVERLAP = 0.45f
private const val MIN_MATCH_SCORE = 28f
internal const val VOD_CACHE_STATE_DETAIL_HYDRATED = "DETAIL_HYDRATED"

private val YEAR_PATTERN = Regex("""(?<!\d)((?:19|20)\d{2})(?!\d)""")
private val EXTENSION_PATTERN = Regex("""\.(mkv|mp4|avi|mov|m4v|ts|webm)$""", RegexOption.IGNORE_CASE)
private val RELEASE_NOISE_TOKENS = setOf(
    "240p",
    "360p",
    "480p",
    "540p",
    "576p",
    "720p",
    "1080p",
    "1440p",
    "2160p",
    "4k",
    "8k",
    "hdr",
    "hdr10",
    "dv",
    "sdr",
    "web",
    "webdl",
    "web-dl",
    "webrip",
    "bluray",
    "blu-ray",
    "brrip",
    "dvdrip",
    "hdtv",
    "x264",
    "x265",
    "h264",
    "h265",
    "hevc",
    "aac",
    "dts",
    "proper",
    "repack",
    "subbed",
    "dubbed",
    "multi"
)
private val STOP_WORDS = setOf("the", "a", "an", "and", "or", "of")
