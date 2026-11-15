package com.example.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.AetherPlayApp
import com.example.MainActivity
import com.example.data.local.DecoderMode
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderManager
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode as NextDecoderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var decoderManager: DecoderManager? = null
    private var lastAutoFallbackMediaId: String? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)

        val nextDecoderManager = DecoderManager(
            initialVideoMode = NextDecoderMode.AUTO,
            initialAudioMode = NextDecoderMode.AUTO
        )
        decoderManager = nextDecoderManager

        val renderersFactory = NextRenderersFactory(this)
            .setDecoderManager(nextDecoderManager)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        nextDecoderManager.attach(exoPlayer)

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                equalizerEngine.attachToAudioSession(audioSessionId)
            }

            override fun onPlayerError(error: PlaybackException) {
                val isDecoderFailure = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
                    error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
                val currentMediaId = exoPlayer.currentMediaItem?.mediaId
                if (isDecoderFailure && currentMediaId != null && lastAutoFallbackMediaId != currentMediaId) {
                    lastAutoFallbackMediaId = currentMediaId
                    val resumePos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    nextDecoderManager.selectVideoDecoder(NextDecoderMode.FFMPEG)
                    nextDecoderManager.selectAudioDecoder(NextDecoderMode.FFMPEG)
                    exoPlayer.seekTo(resumePos)
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
        })

        serviceScope.launch {
            val savedEq = withContext(Dispatchers.IO) {
                app.container.mediaRepository.getEqualizerSettings()
            }
            equalizerEngine.loadPersistedSettings(savedEq)
            if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId > 0) {
                equalizerEngine.attachToAudioSession(exoPlayer.audioSessionId)
            }
        }

        serviceScope.launch {
            app.container.settingsRepository.preferencesFlow
                .map { it.decoderMode }
                .distinctUntilChanged()
                .collectLatest { mode ->
                    applyDecoderPreferences(nextDecoderManager, mode)
                }
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

    private fun applyDecoderPreferences(manager: DecoderManager, mode: DecoderMode) {
        when (mode) {
            DecoderMode.HARDWARE_FIRST -> {
                // Hardware MediaCodec first, with automatic fallback to FFmpeg for unsupported codecs (AC3/DTS/Hi10P/etc.)
                manager.selectVideoDecoder(NextDecoderMode.AUTO)
                manager.selectAudioDecoder(NextDecoderMode.AUTO)
            }
            DecoderMode.AUTO -> {
                manager.selectVideoDecoder(NextDecoderMode.AUTO)
                manager.selectAudioDecoder(NextDecoderMode.AUTO)
            }
            DecoderMode.SOFTWARE_PREFERRED -> {
                manager.selectVideoDecoder(NextDecoderMode.FFMPEG)
                manager.selectAudioDecoder(NextDecoderMode.FFMPEG)
            }
        }
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
        runCatching { decoderManager?.detach() }
        decoderManager = null
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
