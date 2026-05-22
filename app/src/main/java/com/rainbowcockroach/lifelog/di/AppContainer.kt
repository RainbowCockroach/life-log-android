package com.rainbowcockroach.lifelog.di

import android.content.Context
import androidx.room.Room
import com.rainbowcockroach.lifelog.data.EntryRepository
import com.rainbowcockroach.lifelog.data.local.AppDatabase
import com.rainbowcockroach.lifelog.data.local.SettingsStore
import com.rainbowcockroach.lifelog.data.remote.ApiClient
import com.rainbowcockroach.lifelog.util.ImageStorage

/**
 * Manual DI container. Holds long-lived singletons.
 *
 * Accessed from anywhere via `(context.applicationContext as LifeLogApp).container`.
 * Workers get it the same way from their applicationContext.
 */
class AppContainer(context: Context) {
    val settings: SettingsStore = SettingsStore(context)

    val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "lifelog.db"
    ).build()

    val imageStorage: ImageStorage = ImageStorage(context)

    val apiClient: ApiClient = ApiClient(settings)

    val entryRepository: EntryRepository = EntryRepository(
        dao = database.pendingEntryDao(),
        apiClient = apiClient,
        imageStorage = imageStorage,
    )
}
