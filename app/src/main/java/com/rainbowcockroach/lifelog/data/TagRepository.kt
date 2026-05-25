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
        val existing = mutableMapOf<Long, Long?>()
        // Preserve local lastUsed bumps that happened since the last sync.
        tags.forEach { dto ->
            val prior = dao.findById(dto.id)?.lastUsedMs
            existing[dto.id] = prior
        }
        val mapped = tags.map { dto ->
            CachedTag(
                id = dto.id,
                name = dto.name,
                searchHint = dto.searchHint,
                type = if (dto.type.isBlank()) "tag" else dto.type,
                lastUsedMs = existing[dto.id],
            )
        }
        dao.upsertAll(mapped)
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
        )
        dao.upsert(cached)
        return cached
    }

    suspend fun cacheSize(): Int = dao.count()
}
