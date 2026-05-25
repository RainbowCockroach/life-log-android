package com.rainbowcockroach.lifelog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PendingEntry::class, CachedTag::class],
    version = 3,
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

        // Switches the pending_entries PK from a UUID `localId` to a Long `id` that doubles as
        // the server's entry id (= local timestamp at save time). Drops the obsolete `remoteId`.
        // Existing rows are migrated by using `createdAt` as the new id; on the off chance two
        // rows share a millisecond, INSERT OR IGNORE drops the later duplicate (acceptable for an
        // MVP-stage write-only queue that rarely holds more than a handful of rows).
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE pending_entries_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        mediaLocalPaths TEXT NOT NULL,
                        status TEXT NOT NULL,
                        lastError TEXT,
                        attempts INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        locationId INTEGER,
                        tagIdsJson TEXT NOT NULL DEFAULT '[]'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO pending_entries_new
                        (id, content, createdAt, mediaLocalPaths, status, lastError, attempts, updatedAt, locationId, tagIdsJson)
                    SELECT createdAt, content, createdAt, mediaLocalPaths, status, lastError, attempts, updatedAt, locationId, tagIdsJson
                    FROM pending_entries
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE pending_entries")
                db.execSQL("ALTER TABLE pending_entries_new RENAME TO pending_entries")
            }
        }
    }
}
