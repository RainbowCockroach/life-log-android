package com.rainbowcockroach.lifelog.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rainbowcockroach.lifelog.LifeLogApp

/**
 * WorkManager worker that drains all pending entries to the server.
 *
 * Scheduled with NetworkType.CONNECTED so Android holds it until the device has internet again.
 * On any failure the worker returns Result.retry() and WorkManager retries with exponential
 * backoff (default 30s, doubling, capped at 5h).
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as LifeLogApp).container
        val repo = container.entryRepository

        val pending = repo.loadUnsynced()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false
        for (entry in pending) {
            try {
                repo.syncOne(entry)
            } catch (t: Throwable) {
                anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
