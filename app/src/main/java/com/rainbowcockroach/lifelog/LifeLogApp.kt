package com.rainbowcockroach.lifelog

import android.app.Application
import com.rainbowcockroach.lifelog.di.AppContainer
import com.rainbowcockroach.lifelog.sync.SyncScheduler

class LifeLogApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.scheduleTagSync(this)
    }
}
