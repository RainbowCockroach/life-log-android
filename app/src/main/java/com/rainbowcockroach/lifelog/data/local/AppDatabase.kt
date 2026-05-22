package com.rainbowcockroach.lifelog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PendingEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingEntryDao(): PendingEntryDao
}
