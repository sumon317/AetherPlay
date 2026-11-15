package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.AppContainer
import com.example.data.local.AudioDoubleTapAction
import com.example.data.local.AudioSwipeAction
import com.example.data.local.DecoderMode
import com.example.data.local.SettingsRepository
import com.example.data.local.SubtitlePreferences
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setDecoderMode(mode: DecoderMode) = viewModelScope.launch { settingsRepository.setDecoderMode(mode) }
    fun setUseSurfaceView(useSurface: Boolean) = viewModelScope.launch { settingsRepository.setUseSurfaceView(useSurface) }
    fun setDefaultSeekSeconds(sec: Int) = viewModelScope.launch { settingsRepository.setDefaultSeekSeconds(sec) }
    fun setResumePlayback(enabled: Boolean) = viewModelScope.launch { settingsRepository.setResumePlayback(enabled) }
    fun setBackgroundVideoAudio(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBackgroundVideoAudio(enabled) }
    fun setAutoPip(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoPip(enabled) }
    fun setGestureBrightnessEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setGestureBrightnessEnabled(enabled) }
    fun setGestureVolumeEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setGestureVolumeEnabled(enabled) }
    fun setBrightnessSensitivity(value: Float) = viewModelScope.launch { settingsRepository.setBrightnessSensitivity(value) }
    fun setVolumeSensitivity(value: Float) = viewModelScope.launch { settingsRepository.setVolumeSensitivity(value) }
    fun setSeekSensitivity(value: Float) = viewModelScope.launch { settingsRepository.setSeekSensitivity(value) }
    fun setGestureHorizontalSeek(enabled: Boolean) = viewModelScope.launch { settingsRepository.setGestureHorizontalSeek(enabled) }
    fun setDoubleTapSeek(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDoubleTapSeek(enabled) }
    fun setLongPress2x(enabled: Boolean) = viewModelScope.launch { settingsRepository.setLongPress2xSpeed(enabled) }
    fun resetGestureDefaults() = viewModelScope.launch { settingsRepository.resetGestureDefaults() }
    fun setRotateAudioArtwork(rotate: Boolean) = viewModelScope.launch { settingsRepository.setRotateAudioArtwork(rotate) }
    fun setAudioSwipeDownAction(action: AudioSwipeAction) = viewModelScope.launch { settingsRepository.setAudioSwipeDownAction(action) }
    fun setAudioSwipeUpAction(action: AudioSwipeAction) = viewModelScope.launch { settingsRepository.setAudioSwipeUpAction(action) }
    fun setAudioDoubleTapAction(action: AudioDoubleTapAction) = viewModelScope.launch { settingsRepository.setAudioDoubleTapAction(action) }
    fun updateSubtitlePreferences(transform: (SubtitlePreferences) -> SubtitlePreferences) =
        viewModelScope.launch { settingsRepository.updateSubtitlePreferences(transform) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(settingsRepository = container.settingsRepository)
            }
        }
    }
}
