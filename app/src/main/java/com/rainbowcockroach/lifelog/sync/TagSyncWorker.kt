package com.rainbowcockroach.lifelog.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rainbowcockroach.lifelog.LifeLogApp

/**
 * Refreshes the local tag cache from `GET /tags`.
 *
 * Scheduled with NetworkType.CONNECTED. Failure → retry (handled by WorkManager backoff).
 * The cache survives the app process death; this just keeps it fresh.
 */
class TagSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as LifeLogApp).container
        return try {
            container.tagRepository.refreshFromServer()
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
