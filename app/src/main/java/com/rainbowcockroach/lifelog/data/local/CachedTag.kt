package com.rainbowcockroach.lifelog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of a server-side tag (used as either a regular tag or a location).
 *
 * Refreshed in bulk from `GET /tags` by [com.rainbowcockroach.lifelog.sync.TagSyncWorker].
 * `lastUsed` is bumped locally when the user picks a tag in the editor, so recents work
 * before the next sync.
 */
@Entity(tableName = "cached_tags")
data class CachedTag(
    @PrimaryKey val id: Long,
    val name: String,
    val searchHint: String,
    /** "tag" or "location" — mirrors server `Tag.type`. */
    val type: String,
    val lastUsedMs: Long? = null,
)
