package com.rainbowcockroach.lifelog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Queued diary entry waiting to be synced to the server.
 *
 * Lives only on the device. After successful sync we delete the row.
 *
 * `id` is the local timestamp at save time and doubles as the server's entry id once synced.
 * This keeps offline entries sorted by their real creation time on the server (the list view
 * sorts by `id DESC`).
 */
@Entity(tableName = "pending_entries")
data class PendingEntry(
    @PrimaryKey val id: Long,
    val content: String,
    val createdAt: Long,
    /** JSON array of absolute file paths in app internal storage. */
    val mediaLocalPaths: String,
    /** PENDING | UPLOADING | SYNCED | FAILED */
    val status: String,
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
