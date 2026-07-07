package com.rainbowcockroach.lifelog.data

import com.rainbowcockroach.lifelog.data.local.CachedTag
import com.rainbowcockroach.lifelog.data.local.CachedTagDao
import com.rainbowcockroach.lifelog.data.remote.ApiClient
import com.rainbowcockroach.lifelog.data.remote.CreateTagRequest

/**
 * Reads tags from the local Room cache (so the picker works offline) and refreshes the cache
 * from `GET /tags` when the network is up. Creating a new tag is online-only — see editor UI.
 */
class TagRepository(
    private val dao: CachedTagDao,
    private val api: ApiClient,
) {
    suspend fun search(query: String, type: String): List<CachedTag> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) dao.recents(type) else dao.search(type, trimmed)
    }

    suspend fun findById(id: Long): CachedTag? = dao.findById(id)

    suspend fun bumpLastUsed(ids: List<Long>) {
        val now = System.currentTimeMillis()
        ids.forEach { dao.bumpLastUsed(it, now) }
    }

    /** Pull-all-overwrite refresh. Tag list is small and rarely changes, so simpler than diffing. */
    suspend fun refreshFromServer() {
        val tags = api.fetchAllTags()
        val mapped = tags.map { dto ->
            val serverMs = dto.lastUsed?.let { parseIsoToMs(it) }
            val localMs = dao.findById(dto.id)?.lastUsedMs
            // Merge: take whichever is more recent. Keeps offline bumps that haven't been
            // reflected on the server yet, while also picking up usage from the web client.
            val merged = when {
                serverMs == null -> localMs
                localMs == null -> serverMs
                else -> maxOf(serverMs, localMs)
            }
            CachedTag(
                id = dto.id,
                name = dto.name,
                searchHint = dto.searchHint,
                type = if (dto.type.isBlank()) "tag" else dto.type,
                lastUsedMs = merged,
                backgroundColor = dto.config?.backgroundColor,
                textColor = dto.config?.textColor,
            )
        }
        dao.upsertAll(mapped)
    }

    private fun parseIsoToMs(iso: String): Long? = try {
        java.time.Instant.parse(iso).toEpochMilli()
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }

    /** Online-only. Inserts into the local cache so the picker shows it immediately. */
    suspend fun createOnServer(name: String, type: String): CachedTag {
        val dto = api.createTag(
            CreateTagRequest(name = name.trim(), searchHint = name.trim().lowercase(), type = type)
        )
        val cached = CachedTag(
            id = dto.id,
            name = dto.name.ifBlank { name.trim() },
            searchHint = dto.searchHint,
            type = if (dto.type.isBlank()) type else dto.type,
            lastUsedMs = System.currentTimeMillis(),
            backgroundColor = dto.config?.backgroundColor,
            textColor = dto.config?.textColor,
        )
        dao.upsert(cached)
        return cached
    }

    suspend fun cacheSize(): Int = dao.count()
}
