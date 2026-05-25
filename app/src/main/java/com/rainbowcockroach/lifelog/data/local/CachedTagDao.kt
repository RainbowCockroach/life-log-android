package com.rainbowcockroach.lifelog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedTagDao {

    @Query(
        """
        SELECT * FROM cached_tags
        WHERE type = :type
          AND (LOWER(name) LIKE '%' || LOWER(:query) || '%'
               OR LOWER(searchHint) LIKE '%' || LOWER(:query) || '%')
        ORDER BY
          CASE WHEN LOWER(name) = LOWER(:query) THEN 0
               WHEN LOWER(name) LIKE LOWER(:query) || '%' THEN 1
               ELSE 2 END,
          lastUsedMs DESC,
          name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun search(type: String, query: String, limit: Int = 30): List<CachedTag>

    @Query(
        """
        SELECT * FROM cached_tags
        WHERE type = :type
        ORDER BY lastUsedMs DESC, name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun recents(type: String, limit: Int = 30): List<CachedTag>

    @Query("SELECT * FROM cached_tags WHERE id = :id")
    suspend fun findById(id: Long): CachedTag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tags: List<CachedTag>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: CachedTag)

    @Query("UPDATE cached_tags SET lastUsedMs = :ts WHERE id = :id")
    suspend fun bumpLastUsed(id: Long, ts: Long)

    @Query("SELECT COUNT(*) FROM cached_tags")
    suspend fun count(): Int
}
