package com.rainbowcockroach.lifelog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Queued diary entry waiting to be synced to the server.
 *
 * Lives only on the device. After successful sync we either delete the row or mark it SYNCED
 * and keep it briefly for the user to confirm.
 *
 * Note: `localId` is a UUID — NOT used as the server's entry id. Server assigns `remoteId`.
 * We send `createdAt` so the entry keeps the time the user actually wrote it.
 */
@Entity(tableName = "pending_entries")
data class PendingEntry(
    @PrimaryKey val localId: String,
    val content: String,
    val createdAt: Long,
    /** JSON array of absolute file paths in app internal storage. */
    val mediaLocalPaths: String,
    /** PENDING | UPLOADING | SYNCED | FAILED */
    val status: String,
    val remoteId: Long? = null,
    val lastError: String? = null,
    val attempts: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    /** Required for new entries. Server tag id of the location. Nullable only for legacy rows. */
    val locationId: Long? = null,
    /** JSON array of server tag ids. Empty = no tags. */
    val tagIdsJson: String = "[]",
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_UPLOADING = "UPLOADING"
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_FAILED = "FAILED"
    }
}
