package com.afterglowtv.data.sync

import android.content.Context
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.util.Log
import android.media.MediaMetadataRetriever
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.afterglowtv.data.local.dao.MovieDao
import com.afterglowtv.data.local.dao.ProviderDao
import com.afterglowtv.data.local.dao.SeriesDao
import com.afterglowtv.domain.repository.MovieRepository
import com.afterglowtv.domain.repository.SeriesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val MIN_CANDIDATES_PER_SECTION = 12
private const val MAX_CANDIDATES_PER_SECTION = 240

internal fun shouldAttemptVodFrameArtwork(posterUrl: String?, streamUrl: String?): Boolean {
    val normalizedStream = streamUrl.orEmpty().trim()
    return posterUrl.isNullOrBlank() &&
        (normalizedStream.startsWith("http://", ignoreCase = true) ||
            normalizedStream.startsWith("https://", ignoreCase = true))
}

internal fun normalizeVodEnrichmentCandidateLimit(value: Int): Int =
    value.coerceIn(MIN_CANDIDATES_PER_SECTION, MAX_CANDIDATES_PER_SECTION)

internal enum class VodEnrichmentReadiness {
    READY,
    NO_WORK,
    DEFER_LOW_MEMORY
}

internal fun decideVodEnrichmentReadiness(
    hasProviders: Boolean,
    isLowOnMemory: Boolean
): VodEnrichmentReadiness = when {
    !hasProviders -> VodEnrichmentReadiness.NO_WORK
    isLowOnMemory -> VodEnrichmentReadiness.DEFER_LOW_MEMORY
    else -> VodEnrichmentReadiness.READY
}

class VodEnrichmentWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VodEnrichmentWorkerEntryPoint {
        fun providerDao(): ProviderDao
        fun movieDao(): MovieDao
        fun seriesDao(): SeriesDao
        fun movieRepository(): MovieRepository
        fun seriesRepository(): SeriesRepository
        fun vodOnlineMetadataClient(): VodOnlineMetadataClient
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                VodEnrichmentWorkerEntryPoint::class.java
            )
            val requestedProviderId = inputData.getLong(KEY_PROVIDER_ID, INVALID_PROVIDER_ID)
            val adultOnly = inputData.getBoolean(KEY_ADULT_ONLY, false)
            val candidateLimit = normalizeVodEnrichmentCandidateLimit(
                inputData.getInt(KEY_CANDIDATE_LIMIT, CANDIDATES_PER_SECTION)
            )
            val providers = if (requestedProviderId > 0L) {
                entryPoint.providerDao().getById(requestedProviderId)?.let(::listOf).orEmpty()
            } else {
                entryPoint.providerDao().getAllSync().filter { it.isActive }
            }

            when (decideVodEnrichmentReadiness(providers.isNotEmpty(), applicationContext.isCurrentlyLowOnMemoryForSync())) {
                VodEnrichmentReadiness.NO_WORK -> return Result.success()
                VodEnrichmentReadiness.DEFER_LOW_MEMORY -> return Result.retry()
                VodEnrichmentReadiness.READY -> Unit
            }

            var sawRetryableFailure = false
            providers
                .forEach { provider ->
                    runCatching {
                        enrichProvider(
                            entryPoint = entryPoint,
                            providerId = provider.id,
                            candidateLimit = candidateLimit,
                            adultOnly = adultOnly
                        )
                    }.onFailure { error ->
                        Log.w(TAG, "VOD enrichment failed for provider ${provider.id}", error)
                        if (shouldRetry(error)) sawRetryableFailure = true
                    }
                }

            if (sawRetryableFailure) Result.retry() else Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "VOD enrichment worker failed", error)
            if (shouldRetry(error)) Result.retry() else Result.failure()
        }
    }

    private suspend fun enrichProvider(
        entryPoint: VodEnrichmentWorkerEntryPoint,
        providerId: Long,
        candidateLimit: Int,
        adultOnly: Boolean
    ) {
        val staleBefore = System.currentTimeMillis() - ENRICHMENT_RETRY_COOLDOWN_MILLIS
        val movieCandidates = if (adultOnly) {
            entryPoint.movieDao().getAdultVodEnrichmentCandidates(providerId, candidateLimit, staleBefore)
        } else {
            entryPoint.movieDao().getVodEnrichmentCandidates(providerId, candidateLimit, staleBefore)
        }
        movieCandidates
            .forEach { movie ->
                entryPoint.movieRepository().getMovieDetails(providerId, movie.id)
                var refreshedMovie = entryPoint.movieDao().getById(movie.id) ?: movie
                entryPoint.vodOnlineMetadataClient().findMovieMetadata(refreshedMovie)?.let { metadata ->
                    val enrichedMovie = mergeMovieOnlineMetadata(refreshedMovie, metadata)
                    entryPoint.movieDao().update(enrichedMovie)
                    refreshedMovie = enrichedMovie
                }
                if (shouldAttemptVodFrameArtwork(refreshedMovie.posterUrl, refreshedMovie.streamUrl)) {
                    VodFrameArtworkExtractor.extract(
                        context = applicationContext,
                        contentKey = "movie-${refreshedMovie.providerId}-${refreshedMovie.id}",
                        streamUrl = refreshedMovie.streamUrl
                    )?.let { posterUri ->
                        entryPoint.movieDao().update(refreshedMovie.copy(posterUrl = posterUri))
                    }
                }
                val latestMovie = entryPoint.movieDao().getById(movie.id) ?: refreshedMovie
                if (latestMovie.detailHydratedAt <= 0L || latestMovie.cacheState == CACHE_STATE_SUMMARY_ONLY) {
                    entryPoint.movieDao().update(
                        latestMovie.copy(
                            cacheState = VOD_CACHE_STATE_DETAIL_HYDRATED,
                            detailHydratedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

        val seriesCandidates = if (adultOnly) {
            entryPoint.seriesDao().getAdultVodEnrichmentCandidates(providerId, candidateLimit, staleBefore)
        } else {
            entryPoint.seriesDao().getVodEnrichmentCandidates(providerId, candidateLimit, staleBefore)
        }
        seriesCandidates
            .forEach { series ->
                entryPoint.seriesRepository().getSeriesDetails(providerId, series.id)
                var refreshedSeries = entryPoint.seriesDao().getById(series.id) ?: series
                entryPoint.vodOnlineMetadataClient().findSeriesMetadata(refreshedSeries)?.let { metadata ->
                    val enrichedSeries = mergeSeriesOnlineMetadata(refreshedSeries, metadata)
                    entryPoint.seriesDao().update(enrichedSeries)
                    refreshedSeries = enrichedSeries
                }
                if (refreshedSeries.detailHydratedAt <= 0L || refreshedSeries.cacheState == CACHE_STATE_SUMMARY_ONLY) {
                    entryPoint.seriesDao().update(
                        refreshedSeries.copy(
                            cacheState = VOD_CACHE_STATE_DETAIL_HYDRATED,
                            detailHydratedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
    }

    private fun shouldRetry(error: Throwable?): Boolean {
        return when (error) {
            is IOException -> true
            is SQLiteException -> error.message.orEmpty().contains("locked", ignoreCase = true) ||
                error.message.orEmpty().contains("busy", ignoreCase = true)
            else -> false
        }
    }

    companion object {
        private const val TAG = "VodEnrichmentWorker"
        private const val KEY_PROVIDER_ID = "provider_id"
        private const val KEY_ADULT_ONLY = "adult_only"
        private const val KEY_CANDIDATE_LIMIT = "candidate_limit"
        private const val INVALID_PROVIDER_ID = -1L
        private const val CANDIDATES_PER_SECTION = 12
        private const val CACHE_STATE_SUMMARY_ONLY = "SUMMARY_ONLY"
        private const val ENRICHMENT_RETRY_COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L
        private const val UNIQUE_WORK_NAME = "vod-enrichment-worker"
        private const val UNIQUE_LAUNCH_WORK_NAME = "vod-enrichment-launch-worker"
        private const val UNIQUE_PROVIDER_WORK_PREFIX = "vod-enrichment-provider-"
        private const val PERIODIC_INITIAL_DELAY_MINUTES = 20L
        private const val LAUNCH_INITIAL_DELAY_MINUTES = 6L

        fun enqueueProvider(
            context: Context,
            providerId: Long,
            initialDelaySeconds: Long = 0L,
            candidateLimit: Int = CANDIDATES_PER_SECTION,
            adultOnly: Boolean = false
        ) {
            if (providerId <= 0L) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_PROVIDER_WORK_PREFIX + providerId + if (adultOnly) "-adult" else "",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                createProviderRequest(providerId, initialDelaySeconds, candidateLimit, adultOnly)
            )
        }

        fun enqueuePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                createPeriodicRequest()
            )
        }

        fun enqueueLaunchScan(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_LAUNCH_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                createLaunchRequest()
            )
        }

        fun cancelStartupMaintenance(context: Context): List<Operation> {
            val workManager = WorkManager.getInstance(context)
            return listOf(
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME),
                workManager.cancelUniqueWork(UNIQUE_LAUNCH_WORK_NAME)
            )
        }

        internal fun createProviderRequest(
            providerId: Long,
            initialDelaySeconds: Long,
            candidateLimit: Int = CANDIDATES_PER_SECTION,
            adultOnly: Boolean = false
        ) =
            OneTimeWorkRequestBuilder<VodEnrichmentWorker>()
                .setInputData(
                    workDataOf(
                        KEY_PROVIDER_ID to providerId,
                        KEY_CANDIDATE_LIMIT to normalizeVodEnrichmentCandidateLimit(candidateLimit),
                        KEY_ADULT_ONLY to adultOnly
                    )
                )
                .setConstraints(defaultConstraints())
                .setInitialDelay(initialDelaySeconds.coerceAtLeast(0L), TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        internal fun createLaunchRequest() =
            OneTimeWorkRequestBuilder<VodEnrichmentWorker>()
                .setConstraints(defaultConstraints())
                .setInitialDelay(LAUNCH_INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        internal fun createPeriodicRequest() =
            PeriodicWorkRequestBuilder<VodEnrichmentWorker>(6, TimeUnit.HOURS)
                .setConstraints(defaultConstraints())
                .setInitialDelay(PERIODIC_INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        private fun defaultConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
}

private object VodFrameArtworkExtractor {
    private const val FRAME_TIMEOUT_MS = 4_000L
    private const val FRAME_TIME_US = 10_000_000L
    private const val MAX_WIDTH = 640
    private const val JPEG_QUALITY = 82

    suspend fun extract(
        context: Context,
        contentKey: String,
        streamUrl: String
    ): String? = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "vod-artwork").apply { mkdirs() }
        val output = File(outputDir, "${sha256(contentKey + streamUrl)}.jpg")
        if (output.exists() && output.length() > 0L) return@withContext output.toURI().toString()

        val bitmap = withTimeoutOrNull(FRAME_TIMEOUT_MS) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(streamUrl, emptyMap())
                retriever.getFrameAtTime(FRAME_TIME_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } finally {
                runCatching { retriever.release() }
            }
        } ?: return@withContext null

        val scaled = bitmap.scaleDown(MAX_WIDTH)
        runCatching {
            output.outputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
        }.onFailure {
            output.delete()
            return@withContext null
        }
        output.toURI().toString()
    }

    private fun Bitmap.scaleDown(maxWidth: Int): Bitmap {
        if (width <= maxWidth) return this
        val targetHeight = (height * (maxWidth.toFloat() / width)).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, maxWidth, targetHeight, true)
    }

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
