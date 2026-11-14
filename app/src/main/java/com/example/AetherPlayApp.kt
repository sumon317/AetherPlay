package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsRepository
import com.example.data.repository.MediaRepository
import com.example.player.EqualizerEngine
import com.example.player.PlayerControllerHolder

class AppContainer(application: Application) {
    private val database: AppDatabase by lazy { AppDatabase.getInstance(application) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(application)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(application, database.mediaDao())
    }

    val equalizerEngine: EqualizerEngine by lazy {
        EqualizerEngine()
    }

    val playerControllerHolder: PlayerControllerHolder by lazy {
        PlayerControllerHolder(application, mediaRepository, settingsRepository)
    }
}

class AetherPlayApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
