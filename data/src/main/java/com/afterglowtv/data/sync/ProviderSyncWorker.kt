package com.afterglowtv.data.sync

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
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
import com.afterglowtv.data.local.dao.ProviderDao
import com.afterglowtv.data.local.dao.ChannelDao
import com.afterglowtv.data.local.dao.XtreamIndexJobDao
import com.afterglowtv.data.local.dao.XtreamLiveOnboardingDao
import com.afterglowtv.domain.model.ProviderStatus
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.SyncState
import com.afterglowtv.domain.repository.SyncMetadataRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

internal suspend fun reconcileTargetedProviderStatus(
    providerDao: ProviderDao,
    channelDao: ChannelDao,
    syncManager: SyncManager,
    provider: com.afterglowtv.data.local.entity.ProviderEntity,
    result: com.afterglowtv.domain.model.Result<Unit>,
    currentTimeMillis: Long = System.currentTimeMillis()
) {
    when (result) {
        is com.afterglowtv.domain.model.Result.Success -> {
            val finalStatus = if (syncManager.currentSyncState(provider.id) is SyncState.Partial) {
                ProviderStatus.PARTIAL
            } else {
                ProviderStatus.ACTIVE
            }
            if (!hasUsableLiveCatalogForActivation(provider.id, provider.type, channelDao)) {
                providerDao.update(
                    provider.copy(
                        isActive = false,
                        status = ProviderStatus.PARTIAL,
                        lastSyncedAt = currentTimeMillis
                    )
                )
                return
            }
            providerDao.update(
                provider.copy(
                    isActive = true,
                    status = finalStatus,
                    lastSyncedAt = currentTimeMillis
                )
            )
        }
        is com.afterglowtv.domain.model.Result.Error -> {
            if (provider.status != ProviderStatus.PARTIAL) {
                providerDao.update(provider.copy(isActive = false, status = ProviderStatus.ERROR))
            }
        }
        is com.afterglowtv.domain.model.Result.Loading -> Unit
    }
}

internal suspend fun shouldTrackInitialLiveOnboarding(
    provider: com.afterglowtv.data.local.entity.ProviderEntity,
    onboardingDao: XtreamLiveOnboardingDao
): Boolean = provider.type == ProviderType.XTREAM_CODES &&
    onboardingDao.getIncompleteByProvider(provider.id) != null

internal fun shouldSkipBackgroundProviderSync(
    provider: com.afterglowtv.data.local.entity.ProviderEntity,
    requestedProviderId: Long
): Boolean = requestedProviderId == -1L && provider.type == ProviderType.M3U

class ProviderSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderSyncWorkerEntryPoint {
        fun providerDao(): ProviderDao
        fun channelDao(): ChannelDao
        fun syncManager(): SyncManager
        fun syncMetadataRepository(): SyncMetadataRepository
        fun xtreamIndexJobDao(): XtreamIndexJobDao
        fun xtreamLiveOnboardingDao(): XtreamLiveOnboardingDao
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ProviderSyncWorkerEntryPoint::class.java
            )
            val requestedProviderId = inputData.getLong(KEY_PROVIDER_ID, INVALID_PROVIDER_ID)
            val providers = if (requestedProviderId != INVALID_PROVIDER_ID) {
                entryPoint.providerDao().getById(requestedProviderId)?.let(::listOf).orEmpty()
            } else {
                entryPoint.providerDao().getAllSync()
            }
            if (providers.isEmpty()) {
                return Result.success()
            }

            var sawRetryableFailure = false
            providers.forEach { provider ->
                if (shouldSkipBackgroundProviderSync(provider, requestedProviderId)) {
                    return@forEach
                }
                val trackInitialLiveOnboarding = shouldTrackInitialLiveOnboarding(
                    provider = provider,
                    onboardingDao = entryPoint.xtreamLiveOnboardingDao()
                )
                val result = if (requestedProviderId == provider.id) {
                    entryPoint.syncManager().sync(
                        provider.id,
                        force = false,
                        trackInitialLiveOnboarding = trackInitialLiveOnboarding
                    )
                } else if (provider.type == ProviderType.XTREAM_CODES) {
                    syncXtreamProviderIfStale(entryPoint, provider)
                } else {
                    entryPoint.syncManager().sync(provider.id, force = false)
                }
                if (requestedProviderId == provider.id) {
                    reconcileTargetedProviderStatus(entryPoint, provider, result)
                }
                when (result) {
                    is com.afterglowtv.domain.model.Result.Error -> {
                        Log.w(TAG, "Provider sync worker failed for provider ${provider.id}: ${result.message}")
                        if (shouldRetry(result.exception)) {
                            sawRetryableFailure = true
                        }
                    }
                    else -> VodEnrichmentWorker.enqueueProvider(
                        context = applicationContext,
                        providerId = provider.id,
                        initialDelaySeconds = VOD_ENRICHMENT_AFTER_SYNC_DELAY_SECONDS
                    )
                }
            }

            if (sawRetryableFailure) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Provider sync worker failed", e)
            if (shouldRetry(e)) Result.retry() else Result.failure()
        }
    }

    private fun shouldRetry(error: Throwable?): Boolean {
        return when (error) {
            is java.io.IOException -> true
            is SQLiteException -> error.message.orEmpty().contains("locked", ignoreCase = true) ||
                error.message.orEmpty().contains("busy", ignoreCase = true)
            else -> false
        }
    }

    companion object {
        private const val TAG = "ProviderSyncWorker"
        private const val UNIQUE_WORK_NAME = "provider-sync-worker"
        private const val UNIQUE_LAUNCH_STALE_WORK_NAME = "provider-sync-launch-stale-check"
        private const val UNIQUE_PROVIDER_WORK_PREFIX = "provider-sync-provider-"
        private const val KEY_PROVIDER_ID = "provider_id"
        private const val INVALID_PROVIDER_ID = -1L
        private const val PERIODIC_INITIAL_DELAY_MINUTES = 15L
        private const val LAUNCH_STALE_CHECK_DELAY_MINUTES = 3L
        private const val VOD_ENRICHMENT_AFTER_SYNC_DELAY_SECONDS = 90L

        fun enqueuePeriodic(context: Context) {
            val request = createPeriodicRequest()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueLaunchStaleCheck(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_LAUNCH_STALE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                createLaunchStaleCheckRequest()
            )
        }

        fun enqueueProvider(context: Context, providerId: Long) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_PROVIDER_WORK_PREFIX + providerId,
                ExistingWorkPolicy.REPLACE,
                createProviderRequest(providerId)
            )
        }

        fun cancelStartupMaintenance(context: Context): List<Operation> {
            val workManager = WorkManager.getInstance(context)
            return listOf(
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME),
                workManager.cancelUniqueWork(UNIQUE_LAUNCH_STALE_WORK_NAME)
            )
        }

        internal fun createPeriodicRequest() =
            PeriodicWorkRequestBuilder<ProviderSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInitialDelay(PERIODIC_INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.MINUTES
                )
                .build()

        internal fun createLaunchStaleCheckRequest() =
            OneTimeWorkRequestBuilder<ProviderSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInitialDelay(LAUNCH_STALE_CHECK_DELAY_MINUTES, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

        internal fun createProviderRequest(providerId: Long) =
            OneTimeWorkRequestBuilder<ProviderSyncWorker>()
                .setInputData(workDataOf(KEY_PROVIDER_ID to providerId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
    }

    private suspend fun syncXtreamProviderIfStale(
        entryPoint: ProviderSyncWorkerEntryPoint,
        provider: com.afterglowtv.data.local.entity.ProviderEntity
    ): com.afterglowtv.domain.model.Result<Unit> {
        val now = System.currentTimeMillis()
        if (shouldTrackInitialLiveOnboarding(provider, entryPoint.xtreamLiveOnboardingDao())) {
            return entryPoint.syncManager().sync(
                provider.id,
                force = false,
                trackInitialLiveOnboarding = true
            )
        }
        val metadata = entryPoint.syncMetadataRepository().getMetadata(provider.id)
        val liveStale = ContentCachePolicy.shouldRefresh(
            metadata?.lastLiveSuccess ?: 0L,
            ContentCachePolicy.CATALOG_TTL_MILLIS,
            now
        )
        val epgStale = provider.epgSyncMode != ProviderEpgSyncMode.SKIP &&
            ContentCachePolicy.shouldRefresh(
                metadata?.lastEpgSuccess ?: 0L,
                ContentCachePolicy.EPG_TTL_MILLIS,
                now
            )
        val movieIndexDue = shouldRunIndexJob(entryPoint, provider.id, ContentType.MOVIE, now)
        val seriesIndexDue = shouldRunIndexJob(entryPoint, provider.id, ContentType.SERIES, now)

        if (!provider.isActive) {
            return com.afterglowtv.domain.model.Result.success(Unit)
        }

        if (liveStale) {
            when (val liveResult = entryPoint.syncManager().retrySection(
                provider.id,
                SyncRepairSection.LIVE,
                syncReason = XtreamLiveSyncReason.BACKGROUND_STALE
            )) {
                is com.afterglowtv.domain.model.Result.Error -> return liveResult
                else -> Unit
            }
        }
        if (epgStale) {
            when (val epgResult = entryPoint.syncManager().syncEpg(provider.id, force = false)) {
                is com.afterglowtv.domain.model.Result.Error -> return epgResult
                else -> Unit
            }
        }
        if (movieIndexDue) {
            entryPoint.syncManager().scheduleXtreamIndexSync(provider.id, ContentType.MOVIE)
        }
        if (seriesIndexDue) {
            entryPoint.syncManager().scheduleXtreamIndexSync(provider.id, ContentType.SERIES)
        }
        return com.afterglowtv.domain.model.Result.success(Unit)
    }

    private suspend fun shouldRunIndexJob(
        entryPoint: ProviderSyncWorkerEntryPoint,
        providerId: Long,
        section: ContentType,
        now: Long
    ): Boolean {
        val job = entryPoint.xtreamIndexJobDao().get(providerId, section.name) ?: return true
        if (job.state in setOf("QUEUED", "PARTIAL", "STALE", "FAILED_RETRYABLE")) return true
        return ContentCachePolicy.shouldRefresh(job.lastSuccessAt, ContentCachePolicy.CATALOG_TTL_MILLIS, now)
    }

    private suspend fun reconcileTargetedProviderStatus(
        entryPoint: ProviderSyncWorkerEntryPoint,
        provider: com.afterglowtv.data.local.entity.ProviderEntity,
        result: com.afterglowtv.domain.model.Result<Unit>
    ) {
        reconcileTargetedProviderStatus(
            providerDao = entryPoint.providerDao(),
            channelDao = entryPoint.channelDao(),
            syncManager = entryPoint.syncManager(),
            provider = provider,
            result = result
        )
    }
}
