package com.example.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.AetherPlayApp
import com.example.MainActivity
import com.example.data.local.DecoderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val app = application as AetherPlayApp
        val equalizerEngine = app.container.equalizerEngine

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("AetherPlay/1.0 (Android; Media3)")
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                equalizerEngine.attachToAudioSession(audioSessionId)
            }
        })

        serviceScope.launch {
            val savedEq = app.container.mediaRepository.getEqualizerSettings()
            equalizerEngine.loadPersistedSettings(savedEq)
            if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId > 0) {
                equalizerEngine.attachToAudioSession(exoPlayer.audioSessionId)
            }
            val prefs = app.container.settingsRepository.preferencesFlow.first()
            applyDecoderPreferences(exoPlayer, prefs.decoderMode)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun applyDecoderPreferences(exoPlayer: ExoPlayer, mode: DecoderMode) {
        val params = exoPlayer.trackSelectionParameters.buildUpon()
        when (mode) {
            DecoderMode.HARDWARE_FIRST -> params.setForceLowestBitrate(false)
            DecoderMode.AUTO -> params.setForceLowestBitrate(false)
            DecoderMode.SOFTWARE_PREFERRED -> params.setForceLowestBitrate(false)
        }
        exoPlayer.trackSelectionParameters = params.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        val currentUri = currentPlayer?.currentMediaItem?.mediaId
        val currentPos = currentPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
        val app = application as? AetherPlayApp

        if (currentUri != null && currentPos > 0L && app != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    app.container.mediaRepository.updatePlaybackProgress(currentUri, currentPos)
                }
            }
        }

        currentPlayer?.let { p ->
            p.playWhenReady = false
            p.stop()
            p.clearMediaItems()
        }
        app?.container?.playerControllerHolder?.stopAndClear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        val app = application as? AetherPlayApp
        app?.container?.equalizerEngine?.releaseEffects()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
