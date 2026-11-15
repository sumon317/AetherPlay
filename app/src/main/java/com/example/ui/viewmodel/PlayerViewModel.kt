package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.session.MediaController
import com.example.AppContainer
import com.example.data.local.DecoderMode
import com.example.data.local.MediaItemEntity
import com.example.data.local.SettingsRepository
import com.example.data.local.SubtitlePreferences
import com.example.data.local.UserPreferences
import com.example.data.repository.MediaRepository
import com.example.player.PlaybackUiState
import com.example.player.PlayerControllerHolder
import com.example.player.SubtitleParser
import com.example.player.TrackOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AspectRatioMode(val label: String) {
    FIT("Best Fit"),
    FILL("Fill Screen"),
    CROP_16_9("16:9 Cinema"),
    CROP_4_3("4:3 Classic"),
    STRETCH("Stretch")
}

class PlayerViewModel(
    private val playerController: PlayerControllerHolder,
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val controller: StateFlow<MediaController?> = playerController.controller
    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState
    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMediaEntity: StateFlow<MediaItemEntity?> = playbackState
        .map { it.currentUri }
        .distinctUntilChanged()
        .flatMapLatest { uri ->
            if (uri == null) flowOf(null) else mediaRepository.observeMediaByUri(uri)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun resolveVisibleSubtitles(state: PlaybackUiState, subPrefs: SubtitlePreferences): String {
        if (!state.subtitlesEnabled) return ""
        if (state.externalCues.isNotEmpty() && subPrefs.syncDelayMs != 0L) {
            return SubtitleParser.activeCueTextAt(
                cues = state.externalCues,
                positionMs = state.currentPositionMs,
                syncDelayMs = subPrefs.syncDelayMs
            ).orEmpty()
        }
        if (state.activeCues.isNotEmpty()) {
            return state.activeCues.joinToString("\n")
        }
        if (state.externalCues.isNotEmpty()) {
            return SubtitleParser.activeCueTextAt(
                cues = state.externalCues,
                positionMs = state.currentPositionMs,
                syncDelayMs = subPrefs.syncDelayMs
            ).orEmpty()
        }
        return ""
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun stopAndClear() = playerController.stopAndClear()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = playerController.seekBy(deltaMs)
    fun skipNext() = playerController.skipToNext()
    fun skipPrevious() = playerController.skipToPrevious()
    fun skipToQueueIndex(index: Int) = playerController.skipToQueueIndex(index)
    fun setSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)
    fun setPitch(pitch: Float) = playerController.setPlaybackPitch(pitch)
    fun cycleRepeat() = playerController.cycleRepeatMode()
    fun toggleShuffle() = playerController.toggleShuffle()
    fun selectAudioTrack(option: TrackOption) = playerController.selectAudioTrack(option)
    fun selectSubtitleTrack(option: TrackOption?) = playerController.selectSubtitleTrack(option)
    fun attachExternalSubtitle(uri: Uri) = playerController.attachExternalSubtitle(uri)
    fun setSleepTimer(minutes: Int?) = playerController.setSleepTimer(minutes)
    fun toggleAbLoop() = playerController.toggleAbLoopPoint()
    fun clearError() = playerController.clearError()

    fun retryWithSoftwareDecoder() {
        viewModelScope.launch {
            settingsRepository.setDecoderMode(DecoderMode.SOFTWARE_PREFERRED)
            delay(150L)
            playerController.retryCurrentPlayback()
        }
    }

    fun toggleCurrentLike() {
        val current = currentMediaEntity.value ?: return
        viewModelScope.launch {
            mediaRepository.toggleLike(current.uri, !current.isLiked)
        }
    }

    fun updateSubtitlePreferences(transform: (SubtitlePreferences) -> SubtitlePreferences) {
        viewModelScope.launch {
            settingsRepository.updateSubtitlePreferences(transform)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlayerViewModel(
                    playerController = container.playerControllerHolder,
                    mediaRepository = container.mediaRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
