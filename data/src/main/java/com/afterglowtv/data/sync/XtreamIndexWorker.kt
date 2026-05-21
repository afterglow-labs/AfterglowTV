package com.afterglowtv.data.sync

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.afterglowtv.data.local.dao.ProviderDao
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.ProviderType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.util.concurrent.TimeUnit

internal enum class XtreamIndexWorkReadiness {
    READY,
    NO_WORK,
    DEFER_LOW_MEMORY
}

internal fun decideXtreamIndexWorkReadiness(
    hasProviders: Boolean,
    isLowOnMemory: Boolean
): XtreamIndexWorkReadiness = when {
    !hasProviders -> XtreamIndexWorkReadiness.NO_WORK
    isLowOnMemory -> XtreamIndexWorkReadiness.DEFER_LOW_MEMORY
    else -> XtreamIndexWorkReadiness.READY
}

class XtreamIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface XtreamIndexWorkerEntryPoint {
        fun providerDao(): ProviderDao
        fun syncManager(): SyncManager
    }

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE, false)
        val requestedProviderId = inputData.getLong(KEY_PROVIDER_ID, INVALID_PROVIDER_ID)
        val requestedSection = inputData.getString(KEY_SECTION)?.toContentTypeOrNull()

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                XtreamIndexWorkerEntryPoint::class.java
            )
            val providers = if (requestedProviderId > 0L) {
                entryPoint.providerDao().getById(requestedProviderId)?.let(::listOf).orEmpty()
            } else {
                entryPoint.providerDao().getAllSync()
                    .filter { provider -> provider.isActive && provider.type == ProviderType.XTREAM_CODES }
            }
            when (decideXtreamIndexWorkReadiness(providers.isNotEmpty(), applicationContext.isCurrentlyLowOnMemoryForSync())) {
                XtreamIndexWorkReadiness.NO_WORK -> return Result.success()
                XtreamIndexWorkReadiness.DEFER_LOW_MEMORY -> {
                    Log.w(TAG, "Deferring Xtream index work: device low on memory")
                    return Result.retry()
                }
                XtreamIndexWorkReadiness.READY -> Unit
            }

            var sawRetryableFailure = false
            providers
                .filter { provider -> provider.type == ProviderType.XTREAM_CODES }
                .forEach { provider ->
                    when (val result = entryPoint.syncManager().processQueuedXtreamIndexJobs(
                        providerId = provider.id,
                        section = requestedSection,
                        force = force,
                        maxCategoriesPerSection = CATEGORY_SLICE_SIZE
                    )) {
                        is com.afterglowtv.domain.model.Result.Error -> {
                            Log.w(TAG, "Xtream index worker failed for provider ${provider.id}: ${result.message}")
                            if (shouldRetry(result.exception)) {
                                sawRetryableFailure = true
                            }
                        }
                        else -> VodEnrichmentWorker.enqueueProvider(
                            context = applicationContext,
                            providerId = provider.id,
                            initialDelaySeconds = VOD_ENRICHMENT_AFTER_INDEX_DELAY_SECONDS
                        )
                    }
                }

            if (sawRetryableFailure) Result.retry() else Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Xtream index worker failed", error)
            if (shouldRetry(error)) Result.retry() else Result.failure()
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
        private const val TAG = "XtreamIndexWorker"
        private const val KEY_PROVIDER_ID = "provider_id"
        private const val KEY_SECTION = "section"
        private const val KEY_FORCE = "force"
        private const val INVALID_PROVIDER_ID = -1L
        private const val CATEGORY_SLICE_SIZE = 2
        private const val VOD_ENRICHMENT_AFTER_INDEX_DELAY_SECONDS = 45L
        private const val UNIQUE_WORK_PREFIX = "xtream-index-worker-"
        private const val UNIQUE_PERIODIC_WORK_NAME = "xtream-index-periodic-worker"
        private const val PERIODIC_INITIAL_DELAY_MINUTES = 15L
        private const val LAUNCH_STALE_CHECK_DELAY_MINUTES = 4L

        fun enqueue(
            context: Context,
            providerId: Long,
            section: String? = null,
            force: Boolean = false,
            initialDelaySeconds: Long = 0L
        ) {
            if (providerId <= 0L) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(providerId, section),
                if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,
                createProviderIndexRequest(providerId, section, force, initialDelaySeconds)
            )
        }

        fun enqueueLaunchStaleCheck(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "$UNIQUE_WORK_PREFIX-launch-stale-check",
                ExistingWorkPolicy.KEEP,
                createLaunchStaleCheckRequest()
            )
        }

        fun enqueuePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                createPeriodicRequest()
            )
        }

        fun cancelStartupMaintenance(context: Context): List<Operation> {
            val workManager = WorkManager.getInstance(context)
            return listOf(
                workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME),
                workManager.cancelUniqueWork("$UNIQUE_WORK_PREFIX-launch-stale-check")
            )
        }

        internal fun createProviderIndexRequest(
            providerId: Long,
            section: String?,
            force: Boolean,
            initialDelaySeconds: Long
        ) = OneTimeWorkRequestBuilder<XtreamIndexWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(KEY_PROVIDER_ID, providerId)
                    .putBoolean(KEY_FORCE, force)
                    .also { builder ->
                        section?.let { builder.putString(KEY_SECTION, it) }
                    }
                    .build()
            )
            .setConstraints(defaultConstraints())
            .setInitialDelay(initialDelaySeconds.coerceAtLeast(0L), TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        internal fun createLaunchStaleCheckRequest() =
            OneTimeWorkRequestBuilder<XtreamIndexWorker>()
                .setConstraints(defaultConstraints())
                .setInitialDelay(LAUNCH_STALE_CHECK_DELAY_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        internal fun createPeriodicRequest() =
            PeriodicWorkRequestBuilder<XtreamIndexWorker>(6, TimeUnit.HOURS)
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

        private fun uniqueWorkName(providerId: Long, section: String?): String =
            "$UNIQUE_WORK_PREFIX$providerId-${section.orEmpty()}"

        private fun String.toContentTypeOrNull(): ContentType? =
            runCatching { ContentType.valueOf(this) }.getOrNull()
    }
}
