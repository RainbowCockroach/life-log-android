package com.rainbowcockroach.lifelog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEntryDao {

    @Query("SELECT * FROM pending_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingEntry>>

    @Query("SELECT COUNT(*) FROM pending_entries WHERE status != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM pending_entries WHERE status IN ('PENDING','FAILED') ORDER BY createdAt ASC")
    suspend fun loadUnsynced(): List<PendingEntry>

    @Query("SELECT * FROM pending_entries WHERE id = :id")
    suspend fun findById(id: Long): PendingEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PendingEntry)

    @Update
    suspend fun update(entry: PendingEntry)

    @Query("DELETE FROM pending_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
