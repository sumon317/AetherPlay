package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.MediaItemEntity
import com.example.data.local.SettingsRepository
import com.example.data.repository.MediaRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val codec: String?,
    val isSelected: Boolean,
    val isSupported: Boolean
)

data class PlaybackUiState(
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentUri: String? = null,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val isVideo: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val subtitlesEnabled: Boolean = true,
    val activeCues: List<String> = emptyList(),
    val externalSubtitleUri: String? = null,
    val externalCues: List<TimedSubtitleCue> = emptyList(),
    val formatBadge: String = "",
    val isHardwareAccelerated: Boolean = true,
    val sleepTimerRemainingMs: Long? = null,
    val abLoopStartMs: Long? = null,
    val abLoopEndMs: Long? = null,
    val queue: List<MediaItemEntity> = emptyList(),
    val currentQueueIndex: Int = -1,
    val errorMessage: String? = null
)

class PlayerControllerHolder(
    private val context: Context,
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var playQueueJob: Job? = null
    private var lastSavedPositionTick = 0L
    private var attachedController: MediaController? = null
    private var attachedListener: Player.Listener? = null

    fun connect() {
        val current = _controller.value
        if (current != null && current.isConnected) return
        val existingFuture = controllerFuture
        if (existingFuture != null && !existingFuture.isDone) return

        if (existingFuture != null) {
            runCatching { MediaController.releaseFuture(existingFuture) }
            controllerFuture = null
        }
        _controller.value = null

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    scope.launch {
                        if (_controller.value == controller) {
                            detachCurrentListener()
                            _controller.value = null
                            controllerFuture = null
                            positionJob?.cancel()
                            sleepTimerJob?.cancel()
                            _uiState.value = PlaybackUiState()
                        }
                    }
                }
            })
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val ctrl = future.get()
                    if (controllerFuture == future) {
                        _controller.value = ctrl
                        attachListener(ctrl)
                        syncFullState(ctrl)
                        startPositionTicker()
                    } else {
                        ctrl.release()
                    }
                }.onFailure {
                    if (controllerFuture == future) {
                        controllerFuture = null
                        _controller.value = null
                    }
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun detachCurrentListener() {
        val prevCtrl = attachedController
        val prevListener = attachedListener
        if (prevCtrl != null && prevListener != null) {
            runCatching { prevCtrl.removeListener(prevListener) }
        }
        attachedController = null
        attachedListener = null
    }

    private fun attachListener(ctrl: MediaController) {
        detachCurrentListener()
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying) persistCurrentProgress()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = ctrl.duration.coerceAtLeast(0L)
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncFullState(ctrl)
                scope.launch {
                    val uri = mediaItem?.mediaId ?: return@launch
                    val entity = mediaRepository.getMediaByUri(uri)
                    if (entity?.externalSubtitleUri != null) {
                        loadExternalCues(Uri.parse(entity.externalSubtitleUri))
                    } else {
                        _uiState.update { it.copy(externalCues = emptyList(), externalSubtitleUri = null) }
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTrackOptions(tracks)
            }

            override fun onCues(cueGroup: CueGroup) {
                val cueStrings = cueGroup.cues.mapNotNull { it.text?.toString()?.takeIf { s -> s.isNotBlank() } }
                _uiState.update { it.copy(activeCues = cueStrings) }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _uiState.update {
                    it.copy(
                        playbackSpeed = playbackParameters.speed,
                        playbackPitch = playbackParameters.pitch
                    )
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.update { it.copy(repeatMode = repeatMode) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                _uiState.update {
                    it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _uiState.update {
                    it.copy(errorMessage = error.localizedMessage ?: "Playback error (${error.errorCodeName})")
                }
            }
        }
        attachedController = ctrl
        attachedListener = listener
        ctrl.addListener(listener)
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val ctrl = _controller.value
                if (ctrl != null && ctrl.mediaItemCount > 0) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.coerceAtLeast(0L)
                    val buf = ctrl.bufferedPosition.coerceAtLeast(0L)

                    val abStart = _uiState.value.abLoopStartMs
                    val abEnd = _uiState.value.abLoopEndMs
                    if (abStart != null && abEnd != null && abEnd > abStart && pos >= abEnd) {
                        ctrl.seekTo(abStart)
                    }

                    _uiState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            bufferedPositionMs = buf
                        )
                    }

                    val now = System.currentTimeMillis()
                    if (ctrl.isPlaying && now - lastSavedPositionTick > 3000L) {
                        lastSavedPositionTick = now
                        persistCurrentProgress()
                    }
                }
                delay(200L)
            }
        }
    }

    private fun persistCurrentProgress() {
        val ctrl = _controller.value ?: return
        val uri = ctrl.currentMediaItem?.mediaId ?: return
        val pos = ctrl.currentPosition.coerceAtLeast(0L)
        scope.launch {
            mediaRepository.updatePlaybackProgress(uri, pos)
        }
    }

    fun playQueue(items: List<MediaItemEntity>, startIndex: Int = 0, resumeSavedPosition: Boolean = true) {
        if (items.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, items.lastIndex)
        val (windowedItems, mappedIndex) = windowQueueAroundIndex(items, safeIndex)
        playQueueJob?.cancel()
        playQueueJob = scope.launch {
            connect()
            var waitCount = 0
            while ((_controller.value == null || _controller.value?.isConnected != true) && waitCount < 40) {
                delay(50L)
                waitCount++
            }
            val ctrl = _controller.value?.takeIf { it.isConnected } ?: return@launch
            val prefs = settingsRepository.preferencesFlow.first()
            val shouldResume = resumeSavedPosition && prefs.resumePlayback

            val mediaItems = withContext(Dispatchers.IO) {
                windowedItems.map { entity -> buildMediaItem(entity) }
            }
            val targetItem = windowedItems[mappedIndex]
            val startPos = if (shouldResume && targetItem.lastPlayedPositionMs > 2000L &&
                (targetItem.durationMs <= 0L || targetItem.lastPlayedPositionMs < targetItem.durationMs - 5000L)
            ) {
                targetItem.lastPlayedPositionMs
            } else {
                0L
            }

            _uiState.update {
                it.copy(
                    queue = windowedItems,
                    currentQueueIndex = mappedIndex,
                    errorMessage = null,
                    abLoopStartMs = null,
                    abLoopEndMs = null,
                    externalSubtitleUri = targetItem.externalSubtitleUri
                )
            }

            if (targetItem.externalSubtitleUri != null) {
                loadExternalCues(Uri.parse(targetItem.externalSubtitleUri))
            } else {
                _uiState.update { it.copy(externalCues = emptyList()) }
            }

            ctrl.setMediaItems(mediaItems, mappedIndex, startPos)
            ctrl.prepare()
            ctrl.play()
            syncFullState(ctrl)
        }
    }

    fun playSingle(item: MediaItemEntity, resumeSavedPosition: Boolean = true) {
        playQueue(listOf(item), 0, resumeSavedPosition)
    }

    fun togglePlayPause() {
        val ctrl = _controller.value ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun stopAndClear() {
        persistCurrentProgress()
        sleepTimerJob?.cancel()
        _controller.value?.let { ctrl ->
            ctrl.stop()
            ctrl.clearMediaItems()
        }
        _uiState.value = PlaybackUiState()
    }

    fun seekTo(positionMs: Long) {
        val ctrl = _controller.value ?: return
        val clamped = positionMs.coerceIn(0L, ctrl.duration.coerceAtLeast(0L))
        ctrl.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
    }

    fun seekBy(deltaMs: Long) {
        val ctrl = _controller.value ?: return
        seekTo(ctrl.currentPosition + deltaMs)
    }

    fun skipToNext() {
        _controller.value?.let {
            if (it.hasNextMediaItem()) it.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        _controller.value?.let {
            if (it.currentPosition > 4000L || !it.hasPreviousMediaItem()) {
                it.seekTo(0L)
            } else {
                it.seekToPreviousMediaItem()
            }
        }
    }

    fun skipToQueueIndex(index: Int) {
        _controller.value?.let { ctrl ->
            if (index in 0 until ctrl.mediaItemCount) {
                ctrl.seekTo(index, 0L)
                ctrl.play()
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val ctrl = _controller.value ?: return
        val currentPitch = _uiState.value.playbackPitch
        ctrl.playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 4.0f), currentPitch)
    }

    fun setPlaybackPitch(pitch: Float) {
        val ctrl = _controller.value ?: return
        val currentSpeed = _uiState.value.playbackSpeed
        ctrl.playbackParameters = PlaybackParameters(currentSpeed, pitch.coerceIn(0.5f, 2.0f))
    }

    fun cycleRepeatMode() {
        val ctrl = _controller.value
        val currentMode = ctrl?.repeatMode ?: _uiState.value.repeatMode
        val nextMode = when (currentMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        if (ctrl != null) {
            ctrl.repeatMode = nextMode
        }
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    fun toggleShuffle() {
        val ctrl = _controller.value
        val nextShuffle = !(ctrl?.shuffleModeEnabled ?: _uiState.value.shuffleEnabled)
        if (ctrl != null) {
            ctrl.shuffleModeEnabled = nextShuffle
        }
        _uiState.update { it.copy(shuffleEnabled = nextShuffle) }
    }

    fun selectAudioTrack(option: TrackOption) {
        val ctrl = _controller.value ?: return
        val group = ctrl.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        val params = ctrl.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
        ctrl.trackSelectionParameters = params
    }

    fun selectSubtitleTrack(option: TrackOption?) {
        val ctrl = _controller.value ?: return
        if (option == null) {
            ctrl.trackSelectionParameters = ctrl.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _uiState.update { it.copy(subtitlesEnabled = false, activeCues = emptyList()) }
        } else {
            val group = ctrl.currentTracks.groups.getOrNull(option.groupIndex) ?: return
            ctrl.trackSelectionParameters = ctrl.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
                .build()
            _uiState.update { it.copy(subtitlesEnabled = true) }
        }
    }

    fun attachExternalSubtitle(subtitleUri: Uri) {
        val ctrl = _controller.value ?: return
        val currentUri = _uiState.value.currentUri ?: return
        scope.launch {
            mediaRepository.attachExternalSubtitle(currentUri, subtitleUri.toString())
            loadExternalCues(subtitleUri)
            val entity = mediaRepository.getMediaByUri(currentUri) ?: return@launch
            val pos = ctrl.currentPosition
            val wasPlaying = ctrl.isPlaying
            val updatedItem = withContext(Dispatchers.IO) { buildMediaItem(entity) }
            val idx = ctrl.currentMediaItemIndex
            if (idx >= 0) {
                ctrl.replaceMediaItem(idx, updatedItem)
                ctrl.seekTo(idx, pos)
                if (wasPlaying) ctrl.play()
            }
            _uiState.update { it.copy(subtitlesEnabled = true, externalSubtitleUri = subtitleUri.toString()) }
        }
    }

    private fun loadExternalCues(uri: Uri) {
        scope.launch {
            val cues = SubtitleParser.parseExternalSubtitleCues(context, uri)
            _uiState.update { it.copy(externalCues = cues) }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.update { it.copy(sleepTimerRemainingMs = null) }
            return
        }
        sleepTimerJob = scope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0L && isActive) {
                _uiState.update { it.copy(sleepTimerRemainingMs = remaining) }
                delay(1000L)
                remaining -= 1000L
            }
            _controller.value?.pause()
            _uiState.update { it.copy(sleepTimerRemainingMs = null) }
        }
    }

    fun toggleAbLoopPoint() {
        val pos = _controller.value?.currentPosition ?: return
        val currentStart = _uiState.value.abLoopStartMs
        val currentEnd = _uiState.value.abLoopEndMs
        when {
            currentStart == null -> _uiState.update { it.copy(abLoopStartMs = pos, abLoopEndMs = null) }
            currentEnd == null && pos > currentStart + 500L -> _uiState.update { it.copy(abLoopEndMs = pos) }
            else -> _uiState.update { it.copy(abLoopStartMs = null, abLoopEndMs = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun buildMediaItem(entity: MediaItemEntity): MediaItem {
        val extras = Bundle().apply {
            putBoolean("is_video", entity.isVideo)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(entity.title)
            .setArtist(entity.artist)
            .setAlbumTitle(entity.album)
            .setExtras(extras)
            .build()

        val builder = MediaItem.Builder()
            .setMediaId(entity.uri)
            .setUri(Uri.parse(entity.uri))
            .setMediaMetadata(metadata)

        if (!entity.externalSubtitleUri.isNullOrBlank()) {
            val subConfig = SubtitleParser.buildSubtitleConfiguration(
                context,
                Uri.parse(entity.externalSubtitleUri)
            )
            builder.setSubtitleConfigurations(listOf(subConfig))
        }
        return builder.build()
    }

    private fun syncFullState(ctrl: MediaController) {
        val item = ctrl.currentMediaItem
        val meta = item?.mediaMetadata
        val isVideo = meta?.extras?.getBoolean("is_video")
            ?: ctrl.currentTracks.groups.any { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }

        _uiState.update {
            it.copy(
                hasMedia = item != null,
                isPlaying = ctrl.isPlaying,
                currentUri = item?.mediaId,
                currentTitle = meta?.title?.toString() ?: "Untitled Media",
                currentArtist = meta?.artist?.toString() ?: "",
                isVideo = isVideo,
                currentPositionMs = ctrl.currentPosition.coerceAtLeast(0L),
                durationMs = ctrl.duration.coerceAtLeast(0L),
                bufferedPositionMs = ctrl.bufferedPosition.coerceAtLeast(0L),
                playbackSpeed = ctrl.playbackParameters.speed,
                playbackPitch = ctrl.playbackParameters.pitch,
                repeatMode = ctrl.repeatMode,
                shuffleEnabled = ctrl.shuffleModeEnabled,
                currentQueueIndex = ctrl.currentMediaItemIndex
            )
        }
        updateTrackOptions(ctrl.currentTracks)
    }

    @OptIn(UnstableApi::class)
    private fun updateTrackOptions(tracks: Tracks) {
        val audioList = mutableListOf<TrackOption>()
        val subList = mutableListOf<TrackOption>()
        var badgeParts = mutableListOf<String>()
        var isHw = true

        tracks.groups.forEachIndexed { groupIdx, group ->
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val selected = group.isTrackSelected(trackIdx)
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> {
                        if (selected) {
                            val codec = readableCodec(format.sampleMimeType, format.codecs)
                            val res = if (format.height > 0) "${format.height}p" else ""
                            val fps = if (format.frameRate > 0f) "${format.frameRate.toInt()}fps" else ""
                            badgeParts.addAll(listOf(codec, res, fps).filter { it.isNotBlank() })
                            isHw = format.sampleMimeType != "video/x-vnd.on2.vp8"
                        }
                    }
                    C.TRACK_TYPE_AUDIO -> {
                        val lang = format.language?.uppercase() ?: "UND"
                        val ch = if (format.channelCount > 0) "${format.channelCount}ch" else ""
                        val codec = readableCodec(format.sampleMimeType, format.codecs)
                        val label = format.label?.takeIf { it.isNotBlank() }
                            ?: listOf("Audio #${audioList.size + 1}", lang, codec, ch).filter { it.isNotBlank() }.joinToString(" • ")
                        audioList += TrackOption(
                            groupIndex = groupIdx,
                            trackIndex = trackIdx,
                            label = label,
                            language = format.language,
                            codec = codec,
                            isSelected = selected,
                            isSupported = group.isTrackSupported(trackIdx)
                        )
                        if (selected && badgeParts.isEmpty()) {
                            val rate = if (format.sampleRate > 0) "${format.sampleRate / 1000}kHz" else ""
                            badgeParts.addAll(listOf(codec, rate, ch).filter { it.isNotBlank() })
                        }
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val lang = format.language?.uppercase() ?: "SUB"
                        val label = format.label?.takeIf { it.isNotBlank() }
                            ?: "Track #${subList.size + 1} ($lang)"
                        subList += TrackOption(
                            groupIndex = groupIdx,
                            trackIndex = trackIdx,
                            label = label,
                            language = format.language,
                            codec = readableCodec(format.sampleMimeType, format.codecs),
                            isSelected = selected,
                            isSupported = group.isTrackSupported(trackIdx)
                        )
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subList,
                formatBadge = badgeParts.joinToString(" • "),
                isHardwareAccelerated = isHw
            )
        }
    }

    private fun readableCodec(mime: String?, codecs: String?): String {
        val m = (mime ?: codecs ?: "").lowercase()
        return when {
            m.contains("hevc") || m.contains("hvc") || m.contains("h265") -> "HEVC"
            m.contains("avc") || m.contains("h264") -> "H.264"
            m.contains("av01") || m.contains("av1") -> "AV1"
            m.contains("vp9") -> "VP9"
            m.contains("vp8") -> "VP8"
            m.contains("flac") -> "FLAC"
            m.contains("opus") -> "OPUS"
            m.contains("vorbis") -> "VORBIS"
            m.contains("mp4a") || m.contains("aac") -> "AAC"
            m.contains("mpeg") -> "MP3"
            m.contains("ac3") -> "AC3"
            m.contains("eac3") -> "EAC3"
            m.contains("dts") -> "DTS"
            m.contains("raw") || m.contains("pcm") -> "PCM"
            m.contains("subrip") -> "SRT"
            m.contains("vtt") -> "VTT"
            m.contains("ssa") -> "ASS"
            else -> mime?.substringAfter('/')?.uppercase() ?: ""
        }
    }

    fun release() {
        persistCurrentProgress()
        playQueueJob?.cancel()
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        detachCurrentListener()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        _controller.value = null
    }

    companion object {
        const val MAX_PLAYER_QUEUE_SIZE = 200

        internal fun windowQueueAroundIndex(
            items: List<MediaItemEntity>,
            targetIndex: Int,
            maxWindowSize: Int = MAX_PLAYER_QUEUE_SIZE
        ): Pair<List<MediaItemEntity>, Int> {
            if (items.isEmpty()) return emptyList<MediaItemEntity>() to 0
            val safeIndex = targetIndex.coerceIn(0, items.lastIndex)
            val clampedMax = maxWindowSize.coerceAtLeast(1)
            if (items.size <= clampedMax) return items to safeIndex

            val half = clampedMax / 2
            var start = (safeIndex - half).coerceAtLeast(0)
            var endExclusive = start + clampedMax
            if (endExclusive > items.size) {
                endExclusive = items.size
                start = (endExclusive - clampedMax).coerceAtLeast(0)
            }
            return items.subList(start, endExclusive) to (safeIndex - start)
        }
    }
}
