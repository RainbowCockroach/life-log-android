package com.rainbowcockroach.lifelog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PendingEntry::class, CachedTag::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingEntryDao(): PendingEntryDao
    abstract fun cachedTagDao(): CachedTagDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_tags (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        searchHint TEXT NOT NULL,
                        type TEXT NOT NULL,
                        lastUsedMs INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE pending_entries ADD COLUMN locationId INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE pending_entries ADD COLUMN tagIdsJson TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }
    }
}
