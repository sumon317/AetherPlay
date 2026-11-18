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
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var sharedDataSourceFactory: DefaultDataSource.Factory
    private lateinit var audioSessionListener: Player.Listener

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
        sharedDataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        audioSessionListener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                equalizerEngine.attachToAudioSession(audioSessionId)
            }
        }

        val initialPlayer = createExoPlayer(DecoderMode.HARDWARE_FIRST).apply {
            addListener(audioSessionListener)
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

        player = initialPlayer
        mediaSession = MediaSession.Builder(this, initialPlayer)
            .setSessionActivity(pendingIntent)
            .build()

        serviceScope.launch {
            val savedEq = app.container.mediaRepository.getEqualizerSettings()
            equalizerEngine.loadPersistedSettings(savedEq)
            if (initialPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && initialPlayer.audioSessionId > 0) {
                equalizerEngine.attachToAudioSession(initialPlayer.audioSessionId)
            }

            val initialPrefs = app.container.settingsRepository.preferencesFlow.first()
            if (initialPrefs.decoderMode != DecoderMode.HARDWARE_FIRST) {
                rebuildPlayerForDecoderMode(initialPrefs.decoderMode, equalizerEngine)
            }

            app.container.settingsRepository.preferencesFlow
                .map { it.decoderMode }
                .distinctUntilChanged()
                .drop(1)
                .collect { nextMode ->
                    rebuildPlayerForDecoderMode(nextMode, equalizerEngine)
                }
        }
    }

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(mode: DecoderMode): ExoPlayer {
        val extensionMode = when (mode) {
            DecoderMode.SOFTWARE_PREFERRED -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            DecoderMode.HARDWARE_FIRST, DecoderMode.AUTO -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        }

        val renderersFactory = NextRenderersFactory(this).apply {
            setExtensionRendererMode(extensionMode)
            setEnableDecoderFallback(true)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(sharedDataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun rebuildPlayerForDecoderMode(
        mode: DecoderMode,
        equalizerEngine: EqualizerEngine
    ) {
        val oldPlayer = player ?: return
        val session = mediaSession ?: return

        val count = oldPlayer.mediaItemCount
        val items = (0 until count).map { oldPlayer.getMediaItemAt(it) }
        val currentIdx = oldPlayer.currentMediaItemIndex.coerceAtLeast(0)
        val currentPos = oldPlayer.currentPosition.coerceAtLeast(0L)
        val playWhenReady = oldPlayer.playWhenReady
        val repeatMode = oldPlayer.repeatMode
        val shuffleEnabled = oldPlayer.shuffleModeEnabled
        val playbackParams = oldPlayer.playbackParameters
        val trackParams = oldPlayer.trackSelectionParameters

        val newPlayer = createExoPlayer(mode).apply {
            addListener(audioSessionListener)
            this.repeatMode = repeatMode
            this.shuffleModeEnabled = shuffleEnabled
            this.playbackParameters = playbackParams
            this.trackSelectionParameters = trackParams
            if (items.isNotEmpty()) {
                setMediaItems(items, currentIdx.coerceIn(0, items.lastIndex), currentPos)
                prepare()
                this.playWhenReady = playWhenReady
            }
        }

        session.player = newPlayer
        player = newPlayer
        oldPlayer.removeListener(audioSessionListener)
        oldPlayer.release()

        if (newPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && newPlayer.audioSessionId > 0) {
            equalizerEngine.attachToAudioSession(newPlayer.audioSessionId)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let { currentPlayer ->
            currentPlayer.playWhenReady = false
            currentPlayer.pause()
            currentPlayer.stop()
            currentPlayer.clearMediaItems()
        }
        (application as? AetherPlayApp)?.container?.equalizerEngine?.releaseEffects()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
