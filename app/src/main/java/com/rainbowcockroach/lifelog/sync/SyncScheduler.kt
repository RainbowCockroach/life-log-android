package com.rainbowcockroach.lifelog.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    const val UNIQUE_WORK_NAME = "sync_pending_entries"
    private const val TAG_SYNC_WORK_NAME = "sync_tags"

    /**
     * Enqueue a sync. Safe to call repeatedly — if a sync is already pending or running
     * we KEEP the existing one. If you want to force a fresh run (e.g. user tapped "Sync now"),
     * pass [replace] = true.
     */
    fun schedule(context: Context, replace: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Refresh the tag cache from the server. Cheap, safe to call on app start. */
    fun scheduleTagSync(context: Context, replace: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<TagSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TAG_SYNC_WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
