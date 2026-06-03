package com.afterglowtv.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.afterglowtv.domain.repository.LocalMediaRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class LocalMediaScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LocalMediaScanWorkerEntryPoint {
        fun localMediaRepository(): LocalMediaRepository
    }

    override suspend fun doWork(): Result {
        val libraryId = inputData.getLong(KEY_LIBRARY_ID, INVALID_LIBRARY_ID)
        if (libraryId == INVALID_LIBRARY_ID) return Result.failure()

        return try {
            val repository = EntryPointAccessors.fromApplication(
                applicationContext,
                LocalMediaScanWorkerEntryPoint::class.java
            ).localMediaRepository()

            when (val result = repository.rescanLibrary(libraryId)) {
                is com.afterglowtv.domain.model.Result.Success -> Result.success()
                is com.afterglowtv.domain.model.Result.Error -> {
                    Log.w(TAG, "Local media scan failed for library $libraryId: ${result.message}", result.exception)
                    if (shouldRetry(result.exception)) Result.retry() else Result.failure()
                }
                com.afterglowtv.domain.model.Result.Loading -> Result.retry()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Local media scan worker failed for library $libraryId", error)
            if (shouldRetry(error)) Result.retry() else Result.failure()
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
        private const val TAG = "LocalMediaScanWorker"
        private const val KEY_LIBRARY_ID = "library_id"
        private const val INVALID_LIBRARY_ID = -1L
        private const val UNIQUE_WORK_PREFIX = "local-media-scan-"

        fun enqueue(context: Context, libraryId: Long) {
            if (libraryId <= 0L) return
            val request = OneTimeWorkRequestBuilder<LocalMediaScanWorker>()
                .setInputData(
                    Data.Builder()
                        .putLong(KEY_LIBRARY_ID, libraryId)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(libraryId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, libraryId: Long) {
            if (libraryId <= 0L) return
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(libraryId))
        }

        private fun uniqueWorkName(libraryId: Long): String = "$UNIQUE_WORK_PREFIX$libraryId"
    }
}
