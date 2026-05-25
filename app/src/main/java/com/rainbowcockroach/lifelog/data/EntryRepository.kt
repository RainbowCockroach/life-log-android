package com.rainbowcockroach.lifelog.data

import com.rainbowcockroach.lifelog.data.local.PendingEntry
import com.rainbowcockroach.lifelog.data.local.PendingEntryDao
import com.rainbowcockroach.lifelog.data.remote.ApiClient
import com.rainbowcockroach.lifelog.data.remote.CreateEntryRequest
import com.rainbowcockroach.lifelog.util.ImageStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The only place that knows how local queue rows turn into server entries.
 *
 * Save path:    enqueue() — write to Room with id = now(), return immediately. SyncWorker picks it up.
 * Sync path:    syncOne() — upload each local image, then POST /entries with the same id. Updates row status.
 */
class EntryRepository(
    private val dao: PendingEntryDao,
    private val apiClient: ApiClient,
    private val imageStorage: ImageStorage,
) {
    private val pathListSerializer = ListSerializer(String.serializer())
    private val longListSerializer = ListSerializer(Long.serializer())

    fun observePending(): Flow<List<PendingEntry>> = dao.observeAll()
    fun observeUnsyncedCount(): Flow<Int> = dao.observeUnsyncedCount()

    suspend fun enqueue(
        content: String,
        mediaLocalPaths: List<String>,
        locationId: Long,
        tagIds: List<Long> = emptyList(),
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        val id = createdAt
        dao.upsert(
            PendingEntry(
                id = id,
                content = content,
                createdAt = createdAt,
                mediaLocalPaths = Json.encodeToString(pathListSerializer, mediaLocalPaths),
                status = PendingEntry.STATUS_PENDING,
                locationId = locationId,
                tagIdsJson = Json.encodeToString(longListSerializer, tagIds),
            )
        )
        return id
    }

    suspend fun loadUnsynced(): List<PendingEntry> = dao.loadUnsynced()

    /** Clear the last error and mark a row PENDING so the next SyncWorker run will pick it up. */
    suspend fun resetForRetry(id: Long) {
        val row = dao.findById(id) ?: return
        dao.update(row.copy(status = PendingEntry.STATUS_PENDING, lastError = null))
    }

    /** Drop a queued entry and delete its locally-staged images. Use for poison rows. */
    suspend fun discard(id: Long) {
        val row = dao.findById(id) ?: return
        val paths = runCatching {
            Json.decodeFromString(pathListSerializer, row.mediaLocalPaths)
        }.getOrDefault(emptyList())
        paths.forEach { imageStorage.delete(it) }
        dao.delete(id)
    }

    /**
     * Sync a single queued entry. Throws on failure (so WorkManager can retry with backoff).
     * On success the row is deleted and its local image files are removed.
     */
    suspend fun syncOne(entry: PendingEntry) {
        dao.update(entry.copy(status = PendingEntry.STATUS_UPLOADING, attempts = entry.attempts + 1))

        val localPaths = Json.decodeFromString(pathListSerializer, entry.mediaLocalPaths)

        // Only upload files still referenced by a pending:// token in content. The user may have
        // deleted the markdown line after picking the image; those files are orphans here.
        val referenced = PENDING_TOKEN_REGEX.findAll(entry.content)
            .map { it.groupValues[1] }
            .toSet()
        val toUpload = if (referenced.isEmpty()) localPaths
                       else localPaths.filter { File(it).name in referenced }

        val uploadedServerPaths = mutableListOf<String>()
        var content = entry.content

        try {
            for (path in toUpload) {
                val file = File(path)
                if (!file.exists()) continue
                val resp = apiClient.uploadMedia(file)
                uploadedServerPaths += resp.path
                content = content.replace("pending://${file.name}", resp.filename)
            }

            val tagIds = runCatching {
                Json.decodeFromString(longListSerializer, entry.tagIdsJson)
            }.getOrDefault(emptyList())

            apiClient.createEntry(
                CreateEntryRequest(
                    id = entry.id,
                    content = content,
                    mediaPaths = uploadedServerPaths,
                    createdAt = formatIso(entry.createdAt),
                    locationId = entry.locationId,
                    tagIds = tagIds,
                )
            )

            localPaths.forEach { imageStorage.delete(it) }
            dao.delete(entry.id)
        } catch (t: Throwable) {
            dao.update(
                entry.copy(
                    status = PendingEntry.STATUS_FAILED,
                    lastError = t.message?.take(500),
                )
            )
            throw t
        }
    }

    private companion object {
        val PENDING_TOKEN_REGEX = Regex("""pending://([^\s)]+)""")
    }

    private fun formatIso(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(epochMs))
    }
}
